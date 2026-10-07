# Kapitel 12 · Übung 9: Worker und Tests, zum Nachschlagen

Tag 2, Entwickler-Track, Kapitel 12 „Worker und Tests“.

Die Schritte von Übung 9 stehen in der Übungsanleitung eurer Schulung: eigene Pfade im Prozesstest in Java schreiben, den fertigen Worker aus Übung 8 weiterlaufen lassen und ihn mit einem Element Template zum Baustein im Modeler machen. Hier schlagt ihr nach: den Prozesstest in Java Schritt für Schritt mit allen Aufrufen und Meldungen, was der Worker unter `worker/` tut, wie ein Prozesstest in C# aussehen kann, das Element Template und die Musterlösung. Was ihr aus Übung 8 braucht, steht im [Blatt zu Übung 8](kapitel-11-lokales-setup.md).

## Prozesstest in Java

<a name="5-prozesstest-in-java"></a>

Ordner `prozesstest-java/`, Datei `src/test/java/io/miragon/schulung/genehmigung/GenehmigungsworkflowTest.java`, Folie „Prozesstest in Java“, dazu aus Kapitel 10 die fünf Schritte eines Testfalls und „Was ein Prozesstest prüft“.

Für den Prozesstest in Java braucht ihr JDK 21 (`java -version`). Habt ihr im Ordner `prozesstest-java/` noch nie `./mvnw test` laufen lassen (Windows: `.\mvnw.cmd test`) oder seit dem letzten Lauf `git pull` gemacht, startet es gleich zu Beginn, mit Netz. Der erste Lauf lädt Maven und die Bibliotheken, rund 70 MB, nach einem `git pull` nur, was dazugekommen ist. Scheitert er mit `PKIX path building failed` oder `Could not transfer artifact`, steht unter [Meldungen aus dem Prozesstest in Java](#meldungen-aus-dem-prozesstest-in-java), was ihr tut.

Im Startstand meldet `./mvnw test` 1 bestandenen und 3 übersprungene Tests: `[WARNING] Tests run: 4, Failures: 0, Errors: 0, Skipped: 3`. Das `[WARNING]` kommt von den übersprungenen Tests und ist kein Fehler. Die Tests stehen auf `@Disabled`, damit `./mvnw test` von Anfang an sauber durchläuft. Ein übersprungener Test ist kein grüner Test.

Der Test startet die Engine selbst, im Speicher, und braucht weder Stack noch Worker. Der Happy Path ist fertig, ihr schreibt Ablehnung und Nachbesserung, wer schneller ist, auch den Timer. Befehle für Windows, die Ausgabe im Startstand und warum der Test die Speicherpunkte selbst anstößt, stehen in der [README des Projekts](../prozesstest-java/README.md).

```bash
cd ../prozesstest-java     # aus worker/, aus dem Repo-Root: cd prozesstest-java
./mvnw test                # Windows PowerShell: .\mvnw.cmd test
```

IDs schreibt ihr nicht als Text. Bei jedem Lauf erzeugt bpmn-to-code aus dem Modell die Klasse `ProcessGenehmigungProcessApi`, darin jede ID als Konstante: aus `Task_Ablehnen` wird `TASK_ABLEHNEN`, aus `End_Abgelehnt` wird `END_ABGELEHNT`. Alle sind schon importiert. Die Prüfungen erwarten die ID als Text, deshalb `TASK_ABLEHNEN.getValue()`. Nur `speicherpunktAnstossen` nimmt die Konstante selbst, etwa `speicherpunktAnstossen(antrag, TASK_PRUEFEN)`. Woher die Klasse kommt und wo sie liegt, steht in der README unter [IDs aus dem Modell](../prozesstest-java/README.md#ids-aus-dem-modell).

1. Lest den Happy Path `genehmigterAntragWirdVerbucht`. Seine Kommentare nummerieren die fünf Schritte eines Testfalls: Starten, Warten, Entscheiden, Verbuchen, Beenden. Jede Zeile mit `assertThat` ist eine Prüfung.
2. **Ablehnung**, `abgelehnterAntragWirdMitgeteilt`: starten, Speicherpunkt nach dem Start anstoßen, „Antrag prüfen“ mit `entscheidung` gleich `abgelehnt` abschließen, Speicherpunkt nach „Antrag prüfen“ anstoßen. Jetzt wartet die Instanz genau bei `TASK_ABLEHNEN`: „Ablehnung mitteilen“ ist eine Aufgabe. Schließt sie mit `complete(task())` ab. Erst dann prüfen: Instanz beendet, `TASK_ABLEHNEN` und `END_ABGELEHNT` durchlaufen, `TASK_VERBUCHEN` und `END_GENEHMIGT` nicht, `buchungsnummer` fehlt.
3. **Nachbesserung**, `nachbesserungFuehrtZurueckZurPruefung`: genauso, aber mit `nachbessern`. Jetzt wartet `TASK_NACHBESSERN`, zugewiesen an `anna`. Diese Aufgabe schließt ihr ohne Variablen ab. Danach stoßt ihr den Speicherpunkt nach „Antrag nachbessern“ an, `speicherpunktAnstossen(antrag, TASK_NACHBESSERN)`: Seit Übung 7 hängt dort ein easyForm, und der Baustein setzt einen Speicherpunkt hinter die Aufgabe. Erst dann wartet die Instanz wieder bei `TASK_PRUEFEN`, und die Aufgabe liegt wieder bei der Gruppe `genehmiger`.
4. **Timer**, `timerSendetErinnerung`, für alle, die schneller sind: Den Timer-Job `BOUNDARY_TIMER` holen, seine Fälligkeit prüfen (in drei Minuten, auf zehn Sekunden genau: „3 Tage ohne Entscheidung“ steht im Modell fürs Training auf `PT3M`) und ihn ausführen, statt zu warten. Danach wartet die Instanz an zwei Stellen, `isWaitingAtExactly(TASK_PRUEFEN.getValue(), TASK_ERINNERN.getValue())`: Der Timer unterbricht nicht, „Erinnerung senden“ kommt als zweite Aufgabe dazu. Schließt sie mit `complete(task(TASK_ERINNERN.getValue(), antrag))` ab. Danach wartet die Instanz wieder genau bei `TASK_PRUEFEN`, `TASK_ERINNERN` und `END_ERINNERT` sind durchlaufen.
5. Ist ein Test fertig, löscht ihr die Zeile `@Disabled(...)` über ihm und startet `./mvnw test`. Löscht sie erst, wenn der Test etwas prüft: Eine leere Methode ohne `@Disabled` läuft grün durch und belegt nichts.

**Die Aufrufe**, alle schon importiert. Die Kommentare in jeder TODO-Methode sagen, welche ihr braucht.

| Aufruf | Was er tut |
|---|---|
| `TASK_PRUEFEN` und `TASK_PRUEFEN.getValue()` | die ID `Task_Pruefen` aus dem Modell, als Konstante und als Text. Mit `Elements.*` sind alle IDs des Modells importiert |
| `ProcessInstance antrag = antragStarten();` | Hilfsmethode am Ende der Klasse: startet `Process_Genehmigung` (Konstante `PROCESS_ID`) wie das Startformular, `antragsteller` ist `anna` (Konstante `ANTRAGSTELLER`) |
| `speicherpunktAnstossen(antrag, START_EVENT_ANTRAG)` | Hilfsmethode: prüft, dass die Instanz am Speicherpunkt hinter diesem Element steht, und führt ihn aus. Nach dem Start mit `START_EVENT_ANTRAG`, nach „Antrag prüfen“ mit `TASK_PRUEFEN` (erst dann entscheidet das Gateway), nach „Antrag nachbessern“ mit `TASK_NACHBESSERN`, jeweils ohne `.getValue()` |
| `assertThat(antrag).isWaitingAtExactly(TASK_PRUEFEN.getValue())` | Wartezustand: Die Instanz wartet genau dort und nirgends sonst. Mit zwei IDs prüft `isWaitingAtExactly(TASK_PRUEFEN.getValue(), TASK_ERINNERN.getValue())` beide Stellen zugleich |
| `complete(task(), withVariables("entscheidung", "abgelehnt"))` | schließt die offene Aufgabe ab wie die genehmigende Stelle, `complete(task())` ohne Variablen. `task()` geht nur, solange genau eine Aufgabe offen ist. Nach dem Timer `complete(task(TASK_ERINNERN.getValue(), antrag))` |
| `assertThat(antrag).task().isAssignedTo(ANTRAGSTELLER)` | prüft, wem die offene Aufgabe gehört, `hasCandidateGroup("genehmiger")` die Gruppe |
| `assertThat(antrag).isEnded().hasPassed(...).hasNotPassed(...)` | Pfad: beendet, durchlaufen, nicht durchlaufen, je mit einer oder mehreren IDs, jede mit `.getValue()` |
| `assertThat(antrag).variables().doesNotContainKey("buchungsnummer")` | Variablen: eine, die fehlen muss. `containsEntry("entscheidung", "abgelehnt")` prüft einen Wert |
| `Job timer = job(BOUNDARY_TIMER.getValue(), antrag);` und `execute(timer);` | holt den Timer-Job und führt ihn aus, `timer.getDuedate()` ist seine Fälligkeit. Das Datum prüft ihr mit `Assertions.assertThat(...)`, wie im Kommentar |

Nehmt immer die Konstanten mit den IDs aus dem Modell, etwa `TASK_ABLEHNEN.getValue()`, nie die Beschriftung „Ablehnung mitteilen“ und nie die ID als Text. Ändert sich dann eine ID im Modell, meldet schon das Übersetzen jede Stelle im Test.

Fertig seid ihr, wenn der Baum viermal ✔ zeigt und darunter `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0` steht, ohne Timer `Skipped: 1`. Maven setzt `[INFO]` davor, mit übersprungenen Tests `[WARNING]`, beides ist kein Fehler.

Nach jedem Lauf zeigt `target/process-test-coverage/io.miragon.schulung.genehmigung.GenehmigungsworkflowTest/report.html` im Browser das Modell, darin grün, was eure Tests durchlaufen haben, und die Abdeckung: im Startstand 9 von 27, mit Ablehnung und Nachbesserung 16 von 27, mit dem Timer 21 von 27, siehe [Abdeckung im Modell](../prozesstest-java/README.md#abdeckung-im-modell).

Wird ein Test rot, steht unter dem Baum im Block „Results“ je Test eine Zeile mit Klasse, Methode, Zeilennummer und Meldung. Was die häufigen Meldungen bedeuten, steht unter [Meldungen aus dem Prozesstest in Java](#meldungen-aus-dem-prozesstest-in-java).

### Bonus: Fehlerpfad in Java

Für alle, die schneller fertig sind. Ihr testet die Variante mit dem fachlichen Fehler, `verbuchen-fehlerpfad.bpmn`. Sie liegt schon in `src/main/resources/`, als Kopie von `prozess/varianten/verbuchen-fehlerpfad.bpmn`, Process ID `Process_VerbuchenFehlerpfad`. Was die Variante tut, steht unter [Fachlicher Fehler](#fachlicher-fehler). Den Worker braucht ihr dafür nicht, der Test spielt ihn selbst.

1. Legt neben `GenehmigungsworkflowTest.java` die Klasse `FehlerpfadTest.java` an, mit denselben Annotationen, aber `@Deployment(resources = "verbuchen-fehlerpfad.bpmn")`. Die IDs der Variante stehen in einer eigenen Klasse, die bpmn-to-code aus `verbuchen-fehlerpfad.bpmn` erzeugt: `ProcessVerbuchenFehlerpfadProcessApi` im Paket `io.miragon.schulung.genehmigung.api.fehlerpfad`, mit Process ID, IDs, Topic und Fehlercode.
2. **Starten:** `runtimeService().startProcessInstanceByKey(PROCESS_ID.getValue(), withVariables("antragsteller", "anna", "betrag", 60000, "begruendung", "Neue Serverhardware"))`. Die Variante hat keinen Speicherpunkt, die Instanz wartet sofort bei `TASK_VERBUCHEN`.
3. **Holen wie der Worker:** `List<LockedExternalTask> tasks = fetchAndLock(GENEHMIGUNG_VERBUCHEN, "prozesstest", 1);` Meldet der Test später `IndexOutOfBounds` mit `Index 0 out of bounds for length 0` bei `tasks.get(0)`, hat `fetchAndLock` nichts geholt. Prüft das Topic: `GENEHMIGUNG_VERBUCHEN` aus `ServiceTasks` steht für `genehmigung-verbuchen`. Die Musterlösung prüft deshalb vorher mit `hasSize(1)`.
4. **Ablehnen wie der Worker:** `externalTaskService().handleBpmnError(tasks.get(0).getId(), "prozesstest", BUCHUNG_ABGELEHNT.getCode(), "Budget der Kostenstelle reicht nicht")`. `BUCHUNG_ABGELEHNT` aus `Errors` trägt den errorCode des Modells. Antworten darf nur, wer den Task gesperrt hat, deshalb zuerst `fetchAndLock`.
5. **Prüfen:** Die Instanz wartet genau bei `TASK_BUCHUNG_KLAEREN` mit der Kandidatengruppe `genehmiger`, `errorCode` ist `BUCHUNG_ABGELEHNT.getCode()`, `errorMessage` euer Grund, `TASK_GENEHMIGUNG_MITTEILEN` ist nicht durchlaufen.
6. **Gegenprobe** als zweiter Test: mit `betrag` 1200 starten, den geholten Task mit `complete(tasks.get(0), withVariables("buchungsnummer", "B-2026-0001"))` abschließen. Dann ist die Instanz beendet, `TASK_GENEHMIGUNG_MITTEILEN` und `END_GENEHMIGT` sind durchlaufen, `TASK_BUCHUNG_KLAEREN` nicht, und `genehmigungMitgeteilt` ist `true`.

Neu zu importieren sind `fetchAndLock` und `externalTaskService`, statisch aus `BpmnAwareTests` wie die anderen, dazu `java.util.List` und `org.cibseven.bpm.engine.externaltask.LockedExternalTask`. Die Konstanten importiert ihr statisch aus `io.miragon.schulung.genehmigung.api.fehlerpfad.ProcessVerbuchenFehlerpfadProcessApi`: `Elements.*`, `PROCESS_ID`, `ServiceTasks.GENEHMIGUNG_VERBUCHEN` und `Errors.BUCHUNG_ABGELEHNT`. Kopiert ihr die Imports aus `GenehmigungsworkflowTest.java`, lasst die drei Zeilen mit `api.ProcessGenehmigungProcessApi` weg. Sonst kennt der Test zwei `TASK_VERBUCHEN`, und Java meldet `Referenz zu TASK_VERBUCHEN ist mehrdeutig`.

Stimmt der errorCode nicht, fängt kein Error-Boundary den Fehler, und die Engine beendet die Instanz still. Der Test meldet dann `to be unfinished, but found that it already finished!`, genau wie euer Modell im laufenden System eine abgelehnte Buchung ohne Vorfall beendet.

### Meldungen aus dem Prozesstest in Java

Nur lesen, wenn es rot wird. Unter dem Baum steht im Block „Results“ je rotem Test eine Zeile: Klasse, Methode, Zeilennummer und die Meldung.

| Was ihr seht | Woran es liegt, was ihr tut |
|---|---|
| `↷` vor eurem Test, `Skipped` zählt ihn noch | `@Disabled` steht noch über der Methode. |
| `to be waiting at exactly [Task_Pruefen], but it is actually waiting at [StartEvent_Antrag]` | Der Speicherpunkt nach dem Start fehlt: `speicherpunktAnstossen(antrag, START_EVENT_ANTRAG)`. Allgemein nennt `actually waiting at [...]` die Stelle, an der die Instanz wirklich wartet. |
| `to be waiting at exactly [Task_Pruefen], but it is actually waiting at [Task_Nachbessern]` | Der Speicherpunkt nach „Antrag nachbessern“ fehlt: `speicherpunktAnstossen(antrag, TASK_NACHBESSERN)`. |
| `to be waiting at exactly [Task_Pruefen], but it is actually waiting at [Task_Pruefen, Task_Erinnern]` | Nach dem Timer sind zwei Aufgaben offen. Prüft beide IDs: `isWaitingAtExactly(TASK_PRUEFEN.getValue(), TASK_ERINNERN.getValue())`. |
| `Query return 2 results instead of max 1` | `task()` ohne ID bei zwei offenen Aufgaben. Nehmt `task(TASK_ERINNERN.getValue(), antrag)`. |
| `by less than 10000ms but difference was 259020004ms` bei der Fälligkeit des Timers | Ihr erwartet drei Tage. „3 Tage ohne Entscheidung“ steht im Modell fürs Training auf `PT3M`, erwartet sind drei Minuten: `Duration.ofMinutes(3)`. |
| `to be ended, but it is not!` | Nach dem Abschließen von „Antrag prüfen“ fehlt `speicherpunktAnstossen(antrag, TASK_PRUEFEN)`, das Gateway hat noch nicht entschieden. Oder auf dem Pfad ist noch eine Aufgabe offen, etwa „Ablehnung mitteilen“ ohne `complete(task())`. Den Zusatz `Please make sure you have set the history service ...` ignoriert ihr, die History ist eingeschaltet. |
| `ENGINE-02004 No outgoing sequence flow for the element with id 'Gateway_Entscheidung' could be selected for continuing the process.` | Kein Pfeil passt zur `entscheidung`. Die Werte heißen genau `genehmigt`, `abgelehnt` und `nachbessern`, kleingeschrieben. |
| `Illegal call of execute(job = 'null') - must not be null!` | Diesen Job gibt es gerade nicht, etwa `speicherpunktAnstossen(antrag, TASK_PRUEFEN)`, bevor ihr die Aufgabe abgeschlossen habt. |
| `Cannot invoke "org.cibseven.bpm.engine.runtime.Job.getDuedate()" because "timer" is null` | Den Timer gibt es erst, wenn die Instanz bei „Antrag prüfen“ wartet. Stoßt vorher den Speicherpunkt nach dem Start an. |
| `Illegal call of complete(task = 'null'` | Es wartet keine Aufgabe. Mit Variablen endet die Meldung auf `both must not be null!`. Prüft vorher mit `isWaitingAtExactly`, wo die Instanz steht. |
| `Call a process instance assertion first - e.g. assertThat(processInstance)... !` | `task()` weiß nicht, welche Instanz gemeint ist. Ruft vorher `assertThat(antrag)` auf, `speicherpunktAnstossen` tut das auch. Ohne `assertThat` nimmt `task()` die Instanz aus dem letzten `assertThat`, notfalls aus dem vorigen Test. Diese Meldung seht ihr deshalb nur, wenn der Test allein läuft, sonst kommt meist `Illegal call of complete(task = 'null'`. |
| `to have passed activities [Ablehnung mitteilen] at least once, but actually we found that it passed [StartEvent_Antrag, Task_Pruefen, Gateway_Entscheidung, Task_Ablehnen, End_Abgelehnt]` | Beschriftung statt ID. Nehmt die Konstante, etwa `TASK_ABLEHNEN.getValue()`. Die Liste dahinter zeigt die IDs des Pfads, den die Instanz genommen hat. |
| `to be unfinished, but found that it already finished!` | Die Instanz ist schon zu Ende, der Test fragt aber nach einem Wartezustand. Nach `complete(task())` an „Ablehnung mitteilen“ ist die Instanz zu Ende, dann prüft ihr `isEnded()`. |
| `COMPILATION ERROR` mit Datei und `[Zeile,Spalte]`, etwa `';' erwartet` (englisch: `';' expected`) | Java-Syntax an dieser Stelle: Semikolon, Klammer oder Anführungszeichen fehlt. |
| `Symbol nicht gefunden`, darunter `Symbol: Variable TASK_PRUEFEN` (englisch: `cannot find symbol`, `symbol: variable TASK_PRUEFEN`) | Diese Konstante gibt es nicht: ein Tippfehler, oder die ID heißt im Modell anders, etwa `Task_Pruefen2`, dann heißt die Konstante `TASK_PRUEFEN_2`. Hat jemand eine ID im Modell geändert, nennt Maven jede Zeile, die noch die alte nutzt. Alle Namen stehen unter `target/generated-test-sources/bpmn-to-code/` in `ProcessGenehmigungProcessApi.java`, Abschnitt `Elements`. |
| `Inkompatible Typen: java.lang.String kann nicht in io.miragon.bpmn.runtime.ElementId konvertiert werden` (englisch: `incompatible types: java.lang.String cannot be converted to io.miragon.bpmn.runtime.ElementId`) | Die ID als Text, etwa `speicherpunktAnstossen(antrag, "Task_Pruefen")`. Die Hilfsmethode nimmt die Konstante: `speicherpunktAnstossen(antrag, TASK_PRUEFEN)`. |
| `kann nicht auf die angegebenen Typen angewendet werden` oder `Methode für task(io.miragon.bpmn.runtime.ElementId,...) nicht geeignet`, darunter `ElementId kann nicht in java.lang.String konvertiert werden` (englisch: `cannot be applied to given types`, `no suitable method found for task(...)`, `ElementId cannot be converted to java.lang.String`) | `.getValue()` fehlt. Prüfungen, `task(...)` und `job(...)` erwarten die ID als Text: `isWaitingAtExactly(TASK_PRUEFEN.getValue())`. |
| Eure IDE markiert `TASK_PRUEFEN` oder `ProcessGenehmigungProcessApi` rot, `./mvnw test` läuft aber | Die Klasse entsteht erst beim Lauf. Startet `./mvnw test` einmal und ladet danach das Maven-Projekt in der IDE neu. |
| `Methode für assertThat(java.util.Date) nicht geeignet` (englisch: `no suitable method found for assertThat(java.util.Date)`), darunter fünf Methoden aus `BpmnAwareTests` | Das importierte `assertThat` kennt nur Objekte der Engine wie Instanz, Aufgabe und Job. Für Datum und Listen schreibt ihr `Assertions.assertThat(...)`. |
| `Fatal error compiling: error: release version 21 not supported` | Maven läuft mit einem älteren JDK. `java -version` muss 21 oder neuer zeigen, sonst setzt ihr `JAVA_HOME` auf das JDK 21. |
| `The JAVA_HOME environment variable is not defined correctly` | Maven findet kein JDK. JDK 21 installieren oder `JAVA_HOME` auf sein Verzeichnis setzen, dann ein neues Terminal öffnen. |
| Der erste Lauf scheitert mit `PKIX path building failed` oder `Could not transfer artifact` | Ihr sitzt hinter einem Proxy, oder eure IT prüft verschlüsselte Verbindungen. Tragt den Proxy in `~/.m2/settings.xml` ein oder startet den ersten Lauf außerhalb des Behördennetzes, etwa über einen Hotspot, siehe [README des Projekts](../prozesstest-java/README.md#hinter-einem-proxy). Klappt es am Schulungstag nicht, schreibt ihr den Java-Teil zu zweit am Rechner eurer Nachbarn. |

## Der fertige Worker

Unter `worker/` liegt der fertige External Task Worker in C#. Ihr startet ihn ab Übung 8 im Ordner `worker/` mit `dotnet run --project src/GenehmigungWorker` und beendet ihn mit Strg+C, dann meldet er `Worker beendet.` Was in welcher Datei steht, zeigt [Aufbau von Worker und Tests](#aufbau-von-worker-und-tests).

Je Task holt die Schleife in `Program.cs` den Task, ruft den Handler und meldet das Ergebnis. Der Handler liest `antragsteller`, `betrag` und `begruendung` und gibt `buchungsnummer` zurück, mit der Engine spricht er nicht. `betrag` kommt aus dem easyForm als Text, etwa `"1234.5"`, immer mit Punkt, per REST als Zahl. Der Handler liest beides mit `CultureInfo.InvariantCulture`. Verbucht wird in der `BuchungssystemSimulation` hinter `IBuchungssystem`, statt eines echten Fachsystems.

Das Log, vor jeder Zeile steht die Uhrzeit:

```
Worker genehmigung-worker-1 holt Tasks vom Topic genehmigung-verbuchen bei http://localhost:8080. Beenden mit Strg+C.
Buchungen der Simulation: .../worker/src/GenehmigungWorker/bin/Debug/net10.0/buchungen.json
Task ed191d5d-... geholt: Business Key (keiner), Prozessinstanz ed10b8c4-..., Retries (noch keine)
Verbucht: B-2026-0001 für anna, 1.200,00 Euro, "Fachtagung Prozessautomatisierung" (Schlüssel ed10b8c4-...)
Task ed191d5d-... erledigt: buchungsnummer = B-2026-0001
```

- **Idempotenz:** Schlüssel jeder Buchung ist der Business Key, ohne ihn die Prozessinstanz-ID. Das Startformular setzt keinen Business Key, im Formular-Lauf ist der Schlüssel deshalb die Prozessinstanz-ID. Die Simulation merkt sich Schlüssel und Nummer in `buchungen.json` neben der DLL und speichert, bevor sie die Nummer zurückgibt. Kommt derselbe Schlüssel noch einmal, etwa weil der Worker vor `complete` abgestürzt ist und der Lock abläuft, loggt sie `Schlüssel ... ist schon verbucht als B-2026-0001, keine zweite Buchung.`, und der Task endet mit derselben Nummer. Das gilt auch nach einem Neustart des Workers.
- **Nummern:** fortlaufend je Kalenderjahr, `B-2026-0001`, `B-2026-0002` und so weiter. Löscht ihr `buchungen.json`, beginnen sie wieder bei 0001.
- **Technischer Fehler:** Scheitert der Handler, etwa an einer fehlenden Variablen, meldet die Schleife `failure`, beim ersten Mal mit 3 verbleibenden Versuchen, danach herunterzählend, dazwischen fünf Minuten Pause. Das Log zeigt etwa `Task ... fehlgeschlagen: The given key 'betrag' was not present in the dictionary. Noch 3 Versuche, der nächste in fünf Minuten.` Bei 0 legt die Engine einen Vorfall an. Was dann hilft, steht in den [Stolpersteinen zu Übung 8](kapitel-11-lokales-setup.md#typische-stolpersteine).
- **Fachlicher Fehler:** Über 50.000 Euro lehnt die Simulation ab, und die Schleife meldet `bpmnError` mit `BUCHUNG_ABGELEHNT`, siehe [Fachlicher Fehler](#fachlicher-fehler).

### Fachlicher Fehler

Nicht jeder Fehler ist technisch. Lehnt das Fachsystem eine Buchung ab, etwa weil das Budget der Kostenstelle nicht reicht, ändert ein zweiter Versuch nichts daran. Mit `failure` würde der Worker alle fünf Minuten dasselbe Nein abholen, bis die Engine einen Vorfall anlegt. Stattdessen meldet er einen BPMN-Fehler, und das Modell führt die Instanz auf einen eigenen Pfad.

Diesen Pfad hat nur die Variante `prozess/varianten/verbuchen-fehlerpfad.bpmn` aus Kapitel 04 und 11: der Ausschnitt ab „Genehmigung erteilt“. Am Service Task „Genehmigung verbuchen“ hängt das Error-Boundary „Buchung abgelehnt“ für den errorCode `BUCHUNG_ABGELEHNT`. Es legt Code und Grund in den Variablen `errorCode` und `errorMessage` ab und führt zu „Buchung klären“ für die Gruppe `genehmiger`. Die Variante hat eine eigene Process ID, `Process_VerbuchenFehlerpfad`, aber dasselbe Topic `genehmigung-verbuchen`. Der Worker bedient deshalb beide Modelle, ohne dass ihr `appsettings.json` ändert.

So probiert ihr es aus, mit laufendem Worker:

1. **Variante deployen**, im Ordner `worker/`. Mit einem Pfad dahinter spielt `deploy` diese Datei ein statt `prozess/genehmigungsworkflow.bpmn`, euer Modell bleibt, wie es ist:
   ```bash
   dotnet run --project src/GenehmigungWorker -- deploy prozess/varianten/verbuchen-fehlerpfad.bpmn
   ```
   Den Pfad schreibt ihr ab dem Repo-Root, `deploy` sucht die Datei vom aktuellen Ordner aus nach oben. Beim ersten Mal meldet `deploy` `Neue Version: Process_VerbuchenFehlerpfad, Version 1`, danach „Modell unverändert“.
2. **Variante per REST starten.** Sie hat weder Startformular noch Initiator, deshalb gebt ihr `antragsteller`, `betrag` und `begruendung` selbst mit. Am bequemsten mit der http-Datei `http/genehmigungsworkflow.http`: B2 startet die Variante, B3 bis B5 prüfen das Ergebnis, B1 deployt sie wie Schritt 1. Oder im Terminal:
   ```bash
   # bash, zsh, Git Bash
   curl -u anna:anna -H "Content-Type: application/json" \
     -d '{"variables":{"antragsteller":{"value":"anna","type":"String"},"betrag":{"value":60000,"type":"Long"},"begruendung":{"value":"Neue Serverhardware","type":"String"}}}' \
     http://localhost:8080/engine-rest/process-definition/key/Process_VerbuchenFehlerpfad/start
   ```
   ```powershell
   # PowerShell
   $anmeldung = "Basic " + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("anna:anna"))
   $antrag = '{"variables":{"antragsteller":{"value":"anna","type":"String"},"betrag":{"value":60000,"type":"Long"},"begruendung":{"value":"Neue Serverhardware","type":"String"}}}'
   Invoke-RestMethod -Method Post -Headers @{ Authorization = $anmeldung } -ContentType "application/json" -Body $antrag -Uri http://localhost:8080/engine-rest/process-definition/key/Process_VerbuchenFehlerpfad/start
   ```
3. **Prüfen.** Nach wenigen Sekunden zeigt das Log `Task ... fachlich abgelehnt: Budget der Kostenstelle reicht nicht: 60.000,00 Euro beantragt, 50.000,00 Euro frei. bpmnError BUCHUNG_ABGELEHNT gemeldet.` Meldet euch als `gerda` an: In „Aufgaben bearbeiten“ wartet im Filter „Aufgaben meiner Gruppen“ die Aufgabe „Buchung klären“ aus „Genehmigung verbuchen mit Fehlerpfad“. Sie hat kein Formular, nach „Mir zuweisen“ zeigt CIB flow sie als „Leere Aufgabe“ mit „Abschließen“. Den Grund seht ihr im Cockpit: Öffnet die Prozessliste unter http://localhost:7083/client/#/seven/auth/processes/list, wählt „Genehmigung verbuchen mit Fehlerpfad“ und öffnet die Instanz über das Augen-Symbol. Im Reiter „Variablen“ stehen `errorCode` mit `BUCHUNG_ABGELEHNT` und `errorMessage` mit dem Grund. In der http-Datei zeigen B3 die Aufgabe und B4 die Variablen.
4. **Gegenprobe.** Startet die Variante noch einmal, diesmal mit `betrag` 1200 statt 60000. Jetzt verbucht der Worker, und die Instanz endet bei „Antrag genehmigt“. B5 meldet `COMPLETED`, B4 zeigt `buchungsnummer` und `genehmigungMitgeteilt`.

Warum die Variante? Mit 60.000 Euro aus dem Startformular kommt auch euer Modell bis zu „Genehmigung verbuchen“, aber dort fängt kein Error-Boundary `BUCHUNG_ABGELEHNT`. Dann beendet die Engine die Instanz still am Service Task: Im Cockpit steht sie als abgeschlossen, ohne „Antrag genehmigt“ und ohne `buchungsnummer`. Es gibt keinen Vorfall, und den Grund findet ihr nur im Log der Engine: `docker compose logs flow-cibseven-spring` im Ordner `stack/` zeigt `ENGINE-02001 ... but no catching boundary event was defined. Execution is ended`. Ein `bpmnError` braucht also ein Boundary im Modell, das seinen Code fängt.

### Prozesstest in C#

`worker/tests/GenehmigungWorker.Tests/GenehmigungsworkflowTests.cs` zeigt, wie ein Prozesstest in C# aussehen kann. Für C# gibt es keine Engine im Speicher wie in Java, der Test spricht deshalb per REST mit eurer lokalen Engine. Dafür prüft er genau das Modell, das dort bereitgestellt ist, mit dem echten Handler. Er läuft in fünf Schritten: Antrag starten, bei „Antrag prüfen“ warten, mit `genehmigt` entscheiden, den External Task holen und den Handler mit einem Fake statt des Fachsystems verbuchen lassen, dann prüfen, dass die Instanz beendet ist und `buchungsnummer` trägt. Die Gegenprobe prüft, dass ein abgelehnter Antrag keinen External Task bekommt.

Er braucht, was auch der Worker braucht: laufenden Stack, den Umbau aus Übung 8 deployt, Zugangsdaten. Den Worker stoppt ihr vorher mit Strg+C, sonst holt er dem Test den Task weg. Im Ordner `worker/`:

```bash
dotnet test
```

Erwartet: 2 Tests bestanden. Jeder Test startet eine eigene Instanz mit Business Key `prozesstest-...` und löscht am Ende, was davon noch offen ist. Habt ihr den Worker eben erst gestoppt, kann der Test rund 30 Sekunden brauchen: Die letzte Long-Polling-Anfrage des Workers bleibt in der Engine noch bis zu zehn Sekunden offen und kann den Task des Tests holen. Dann gehört er für 30 Sekunden dem gestoppten Worker, der Test-Helfer fragt deshalb bis zu 45 Sekunden lang nach.

| Was ihr seht | Woran es liegt, was ihr tut |
|---|---|
| `Kein External Task auf Topic ... auch nicht nach 45 Sekunden` | Euer Worker läuft noch und hat den Task schon verbucht. Stoppt ihn. Oder der Umbau aus Übung 8 ist nicht deployt, „Genehmigung verbuchen“ liegt dann als Aufgabe in der Aufgabenliste. Oder die Instanz steht gar nicht am Service Task, dann stimmen Entscheidung oder Modell nicht. |
| `lieferte 404 NotFound` beim Start, mit der Frage, ob das Modell bereitgestellt ist | Modell nicht bereitgestellt (Projekt importieren und den Umbau deployen wie in Übung 8), oder `ProzessKey` passt nicht zur Process ID des Modells. |
| `Die Engine unter http://localhost:8080/ antwortet nicht` | Der Stack läuft nicht oder startet noch. Im Ordner `stack/`: `docker compose up -d`, dann warten, bis `[init] Fertig.` in der letzten Zeile von `docker compose logs init` steht. |
| `Zugangsdaten für die Engine fehlen` oder `lieferte 401 Unauthorized` | Der Test liest dieselbe Konfiguration wie der Worker, auch dieselben User Secrets. `dotnet user-secrets list --project src/GenehmigungWorker` zeigt, was gesetzt ist, die Befehle stehen unter [Hinweise zu Übung 8](kapitel-11-lokales-setup.md#hinweise). |

Unit-Tests ohne Engine, einen Prozesstest zum fachlichen Fehler und Architekturtests hat die hexagonale Fassung unter `loesung/worker-hexagonal/`, siehe [Schichten oder hexagonal](#schichten-oder-hexagonal).

## Euer Worker als Baustein

In Übung 8 habt ihr „Genehmigung verbuchen“ von Hand auf External gestellt und das Topic eingetippt. Ein Element Template macht euren Worker zu einem Baustein: Im Modeler steht er dann im Katalog wie die Bausteine von CIB flow, einsetzen kann ihn auch, wer im Team nicht entwickelt. Das Topic steht fest im Template statt von Hand im Modell, vertippen kann sich niemand mehr. Das Mapping bleibt im Handler: Euer Worker liest `antragsteller`, `betrag` und `begruendung` selbst und schreibt `buchungsnummer` zurück, das Template braucht dafür keine Ein- und Ausgaben.

Das geht im lokalen Stack, angemeldet als `demo`, an eurem Projekt aus Übung 8. Eine Vorlage gilt dort sofort für alle Konten, und dieselbe ID lässt sich nur einmal hochladen.

**Datei entwerfen**

1. Legt im Repo-Root den Ordner `element-template/` an und darin die Datei `genehmigung-verbuchen.json` mit diesem Gerüst:
   ```json
   {
     "$schema": "https://unpkg.com/@camunda/element-templates-json-schema/resources/schema.json",
     "name": "TODO",
     "id": "TODO",
     "description": "Verbucht einen genehmigten Antrag im Fachsystem. Den Task holt der GenehmigungWorker vom Topic genehmigung-verbuchen.",
     "appliesTo": ["bpmn:ServiceTask"],
     "groups": [
       { "id": "worker", "label": "Worker" }
     ],
     "properties": [
       {
         "label": "TODO",
         "type": "Hidden",
         "value": "TODO",
         "editable": false,
         "binding": { "type": "property", "name": "camunda:type" }
       },
       {
         "label": "Topic",
         "type": "String",
         "value": "TODO",
         "editable": false,
         "group": "worker",
         "description": "Der Worker liest antragsteller, betrag und begruendung selbst aus dem Prozess und schreibt buchungsnummer zurück.",
         "binding": { "type": "property", "name": "camunda:topic" }
       },
       {
         "label": "Name",
         "type": "Hidden",
         "value": "Genehmigung verbuchen",
         "binding": { "type": "property", "name": "name" }
       }
     ]
   }
   ```
   Die Zeile `$schema` braucht CIB flow nicht. VS Code prüft damit die Datei beim Schreiben und schlägt Felder vor.
2. Ersetzt jedes `TODO`. Aus diesen Stellen liest der Katalog, wo und wie euer Baustein erscheint:

| Stelle | Was ihr eintragt | Was der Katalog daraus macht |
|---|---|---|
| `name` | `<Abschnitt> - <Baustein> (Extern) (<Version>)`, die Klammern wie bei den Bausteinen von CIB flow | Der Teil vor dem ersten Bindestrich wird der Abschnitt, der Teil danach bis zur ersten Klammer der Eintrag. Nehmt im Abschnitt also keinen Bindestrich, etwa `Genehmigungsworkflow`: Aus `mm-genehmigung - …` würde der Abschnitt „mm“. Ohne Bindestrich im `name` landet der Baustein unter „Sonstiges“. |
| `id` | Buchstaben, Ziffern, Bindestriche und Punkte, am Ende die Version, etwa `-1.0.0` | Das Stück nach dem letzten Bindestrich steht als Version im Katalog, etwa „Version 1.0.0“. |
| `label` und `value` des ersten Property | `"label": "Implementation Type"` und `"value": "external"` | Nur mit genau diesem Label steht der Baustein als „Extern“ im Katalog. Heißt das Label anders, steht er als „Intern“ da, obwohl er einen External Task setzt. |
| `value` des Property „Topic“ | euer Topic, zeichengleich mit `Topic` in `appsettings.json` | Nach dem Anwenden steht es grau und nicht änderbar in der Gruppe „Worker“. |

**Hochladen**

3. Öffnet über das Logo „CIB flow“ die Kachel „Prozess modellieren“.
4. Klickt unten links auf „Manage templates“ (Zahnrad). Die Seite „Vorlagenverwaltung“ listet die Vorlagen von CIB flow, rund 500.
5. Klickt auf „+ Vorlage hinzufügen“. Im Dialog „Element-Vorlage erstellen“ ist unter „Import Mode“ „Single Template“ gewählt.
6. Klickt auf „Datei auswählen“ und wählt eure Datei `element-template/genehmigung-verbuchen.json`.
7. Klickt auf den grünen Haken „Diese Datei verwenden“. Meldung: „Datei erfolgreich verarbeitet! Überprüfen Sie die extrahierten Daten unten.“ „Vorlagen-ID“, „Name“, „Beschreibung“ und „Inhalt (JSON)“ sind ausgefüllt, „Aktiv“ ist an. Ändert hier nichts.
8. Klickt auf „Speichern“. Die Meldung „Vorlage '…' erfolgreich erstellt!“ nennt euren `name`, der Dialog schließt sich.

**Im Modell anwenden**

9. Öffnet erst jetzt euer Diagramm: Logo „CIB flow“, Kachel „Prozessmanagement“, euer Projekt, Maus über die Zeile des Diagramms, Stift „Artefakt bearbeiten“.
10. Klickt „Genehmigung verbuchen“ an. Der Panel-Kopf zeigt „SERVICE TASK“.
11. Klickt in der Gruppe „Vorlage“ rechts auf „+ Auswählen“. Der Katalog öffnet sich.
12. Tippt `verbuchen` ins Suchfeld „Nach Templates suchen“. Unter eurem Abschnitt steht euer Baustein, darunter grau „Extern“ und eure Version, etwa „Extern • Version 1.0.0“. Klickt ihn an.
13. Prüft das Panel: Der Kopf zeigt euren `name` in Großbuchstaben, bei schmalem Panel gekürzt, darunter „Genehmigung verbuchen“. Die Gruppe „Vorlage“ zeigt „Applied“, die Gruppe „Worker“ das Topic grau und nicht änderbar, darunter den Erklärtext. Die Gruppe „Implementation“ gibt es nicht mehr. Name und ID `Task_Verbuchen` unter „Allgemein“ bleiben.

**Herunterladen und deployen**

14. Klickt auf eine freie Stelle der Zeichenfläche, dann in der Leiste unten auf „BPMN-Diagramm herunterladen“ (Pfeil nach unten), wie in Übung 8. Ladet vor dem Speichern herunter.
15. Speichert mit der Diskette „Diagramm speichern“ und wartet auf die Meldung „Der Prozess wurde erfolgreich aktualisiert.“
16. Legt den Download als `prozess/genehmigungsworkflow.bpmn` ab, wie in Übung 8. Im Ordner `worker/`, mit dem Namen eurer Datei statt `Collaboration_078xn5b`:
    ```bash
    # bash, zsh, Git Bash
    mv ~/Downloads/Collaboration_078xn5b.bpmn ../prozess/genehmigungsworkflow.bpmn
    ```
    ```powershell
    # PowerShell
    Move-Item -Force $HOME\Downloads\Collaboration_078xn5b.bpmn ..\prozess\genehmigungsworkflow.bpmn
    ```
17. Prüft, dass das Template im Modell steht:
    ```bash
    # bash, zsh, Git Bash
    grep 'id="Task_Verbuchen"' ../prozess/genehmigungsworkflow.bpmn
    ```
    ```powershell
    # PowerShell
    Select-String -Path ..\prozess\genehmigungsworkflow.bpmn -Pattern 'id="Task_Verbuchen"'
    ```
    Neu ist nur `camunda:modelerTemplate` mit eurer `id`. Typ und Topic stehen da wie seit Übung 8. Mit der Musterlösung sieht die Zeile so aus:
    ```
        <bpmn:serviceTask id="Task_Verbuchen" name="Genehmigung verbuchen" camunda:modelerTemplate="genehmigung-verbuchen-1.0.0" camunda:type="external" camunda:topic="genehmigung-verbuchen">
    ```
18. Spielt das Modell ein:
    ```bash
    dotnet run --project src/GenehmigungWorker -- deploy
    ```
    `deploy` meldet `Neue Version:` mit eurer Process ID und der nächsten Versionsnummer. Für die Engine ändert sich nichts, sie führt denselben External Task aus wie vorher. `camunda:modelerTemplate` braucht nur der Modeler.
19. Lasst den Worker laufen oder startet ihn, stellt als `anna` einen Antrag und schließt als `gerda` „Antrag prüfen“ mit „Genehmigt“ ab, wie in Übung 8. Der Worker holt den Task wie vorher, am Code ändert ihr nichts. Das Log zeigt den geholten Task, die Buchung und zuletzt `Task ... erledigt: buchungsnummer = B-2026-...`.

Den Weg über den Katalog probiert ihr an einem neuen Service Task in einem anderen Diagramm: „Vorlage“, „+ Auswählen“, und ohne Suche steht euer Baustein im Abschnitt aus eurem `name`. Nach dem Klick heißt der Task „Genehmigung verbuchen“ und trägt Typ und Topic eures Workers.

**Hinweise**

- **Kein Feld `version`.** Mit `"version": 1` schreibt der Modeler zusätzlich `camunda:modelerTemplateVersion="1"` ins Modell. Ändert ihr die Zahl später in derselben Vorlage, zeigt jedes Modell mit der alten Zahl in der Gruppe „Vorlage“ „Not found“. Eine neue Fassung bekommt deshalb eine neue `id` und einen neuen `name`, etwa mit `1.1.0`. Dann stehen beide im Katalog, und vorhandene Modelle bleiben bei der alten.
- **Kein `entriesVisible`.** Mit `"entriesVisible": true` zeigt das Panel wieder alle Gruppen, auch „Implementation“. Dort lässt sich das Topic trotz `"editable": false` ändern, und die Vorlage bleibt dabei „Applied“.
- **Dieselbe `id` nur einmal hochladen.** Beim zweiten Mal meldet der Dialog rot „Constraint violated: …“ und bleibt offen, angelegt wird nichts. Eine geänderte Fassung derselben `id` speichert ihr in der Vorlagenverwaltung über den Stift „Bearbeiten“ an eurer Zeile, der Dialog dort heißt „Edit Element Template“.
- **Löschen geht in der Oberfläche nicht.** Die Vorlagenverwaltung hat dafür keinen Knopf. Das Auge an der Zeile blendet die Vorlage nur aus: Danach findet der Katalog sie nicht mehr, und Modelle mit dieser Vorlage zeigen in der Gruppe „Vorlage“ „Not found“. Ein deploytes Modell läuft weiter, Typ und Topic stehen ja im XML.
- **Am User Task gibt es euren Baustein nicht.** Der Katalog zeigt nur Vorlagen, deren `appliesTo` zum Element passt, an einem User Task findet die Suche `verbuchen` also „Keine Vorlagen“. Zeigt der Panel-Kopf in Schritt 10 „USER TASK“, macht ihr ihn zuerst über den Schraubenschlüssel „Element ändern“ zum „Service Task“.
- **Ungültiges JSON.** Nach dem grünen Haken steht rot „Ungültige JSON-Datei. Bitte überprüfen Sie das Dateiformat und versuchen Sie es erneut.“, und „Speichern“ bleibt grau. VS Code zeigt euch die Stelle in der Datei.
- **HTML statt BPMN.** Ist die Datei unter `prozess/` nur rund 500 Byte groß und `deploy` meldet `ENGINE-09003 Could not parse 'genehmigungsworkflow.bpmn'`, habt ihr direkt nach dem Speichern eine HTML-Datei heruntergeladen. Verschiebt im Modeler ein Element ein Stück und ladet noch einmal herunter.

Die Musterlösung liegt unter `loesung/element-template/genehmigung-verbuchen.json`. Euer Template vergleicht ihr im Repo-Root, bash und PowerShell gleich, mit `git diff --no-index element-template loesung/element-template`.

## Musterlösung

`loesung/prozesstest-java/` enthält die Musterlösung des Prozesstests in Java als vollständiges Projekt, aufgebaut wie der Startstand `prozesstest-java/`: Maven Wrapper, `pom.xml`, die Kopien der Modelle und alle Tests. Ihr baut und testet es direkt in diesem Ordner, ohne etwas zu kopieren. Die README des Java-Projekts liegt nur im Startstand. Den Worker in C# gibt es nur einmal, fertig unter `worker/`.

| Datei | Was die Musterlösung macht |
|---|---|
| `loesung/prozesstest-java/src/test/java/io/miragon/schulung/genehmigung/GenehmigungsworkflowTest.java` | alle vier Testfälle: Happy Path, Ablehnung, Nachbesserung, Timer |
| `loesung/prozesstest-java/src/test/java/io/miragon/schulung/genehmigung/FehlerpfadTest.java` | neu, Bonus: Prozesstest gegen die Variante, `bpmnError` mit `BUCHUNG_ABGELEHNT` führt zu „Buchung klären“, Gegenprobe mit `complete` endet bei „Antrag genehmigt“ |
| `loesung/prozesstest-java/src/test/java/io/miragon/schulung/genehmigung/GenehmigungsworkflowTag1Test.java` und `loesung/prozesstest-java/src/main/resources/genehmigungsworkflow-tag1.bpmn` | neu, nur für die Demo in Kapitel 10: dieselben vier Testfälle am fertigen Modell nach Übung 7, ohne External Task |
| `loesung/genehmigungsworkflow-entwickler.bpmn` | Lösung von Übung 8: das Modell mit „Genehmigung verbuchen“ als External Task, Quelle der Modellkopien in beiden Java-Projekten |
| `loesung/element-template/genehmigung-verbuchen.json` | das Element Template für „Genehmigung verbuchen“, Abschnitt „Genehmigungsworkflow“ im Katalog, „Extern“, Version 1.0.0, `camunda:type` und Topic `genehmigung-verbuchen` fest. Die GitHub Action prüft, dass das Topic zum Modell passt |

Unter `loesung/worker-hexagonal/` liegt der Worker aus `worker/` nach Ports und Adaptern geschnitten, siehe [Schichten oder hexagonal](#schichten-oder-hexagonal).

**Vergleichen:** In VS Code beide Dateien im Explorer markieren, Rechtsklick, „Ausgewählte vergleichen“. Oder im Terminal im Repo-Root, bash und PowerShell gleich:

```bash
git diff --no-index prozesstest-java/src/test/java loesung/prozesstest-java/src/test/java
```

**Bauen und testen:** Die Musterlösung ist ein eigenes Projekt. Ihr wechselt in ihren Ordner und nehmt denselben Befehl wie in `prozesstest-java/`, vom Repo-Root aus:

```bash
cd loesung/prozesstest-java
./mvnw test          # Windows PowerShell: .\mvnw.cmd test
```

`./mvnw test` meldet 10 bestandene Tests: vier am Modell mit External Task, vier der Demo am Modell ohne External Task, zwei des Fehlerpfads. Genau das prüft auch die GitHub Action des Repos bei jedem Push.

**Einzelne Dateien übernehmen:** Wollt ihr eine Datei der Musterlösung in eurem Stand haben, kopiert ihr sie im Repo-Root, etwa `cp loesung/prozesstest-java/src/test/java/io/miragon/schulung/genehmigung/FehlerpfadTest.java prozesstest-java/src/test/java/io/miragon/schulung/genehmigung/` (PowerShell: `Copy-Item` mit denselben Pfaden, `\` statt `/`). Das überschreibt eure Fassung dieser Datei, sichert oder committet sie vorher. Die drei Java-Dateien stehen jede für sich. `FehlerpfadTest.java` braucht nur die Kopie der Variante, die schon im Startstand liegt. `GenehmigungsworkflowTag1Test.java` braucht `genehmigungsworkflow-tag1.bpmn`, die Datei liegt nur in der Musterlösung. Die Klassen mit den IDs erzeugt der Startstand selbst, seine `pom.xml` ist dieselbe.

**Zurück zum Startstand:** Im Repo-Root mit `git restore prozesstest-java` und `git clean -fd prozesstest-java`, in bash und PowerShell gleich. Das verwirft alle eure Änderungen in diesem Ordner und löscht Dateien, die dort neu dazugekommen sind, auch eure eigenen. Ohne `git clean` bliebe eine übernommene `FehlerpfadTest.java` liegen und liefe weiter mit. Die Build-Ausgaben unter `target/` lässt `git clean` stehen, Git ignoriert sie. Die Musterlösung unter `loesung/` bleibt dabei, wie sie ist, ebenso `prozess/`: Dort liegt euer umgebautes Modell aus Übung 8.

## Zum Nachschlagen

### Aufbau von Worker und Tests

```
worker/
├── GenehmigungWorker.sln                    # Solution mit Worker und Prozesstest, hier laufen die dotnet-Befehle
├── src/GenehmigungWorker/
│   ├── Program.cs                           # Worker-Schleife mit complete, failure und bpmnError, mit "deploy" das Deployment
│   ├── ExternalTaskClient.cs                # fetchAndLock, complete, failure, bpmnError und der Record ExternalTask
│   ├── Deploy.cs                            # spielt prozess/genehmigungsworkflow.bpmn ein, mit Pfad eine andere Datei
│   ├── Einstellungen.cs                     # liest appsettings.json, User Secrets und Umgebung
│   ├── Handlers/
│   │   └── GenehmigungVerbuchenHandler.cs   # lesen, verbuchen, Ergebnis zurückgeben
│   ├── Fachsystem/
│   │   ├── IBuchungssystem.cs               # die eine Stelle nach außen
│   │   ├── BuchungssystemSimulation.cs      # simulierte Buchung statt echtem Fachsystem, idempotent, mit Budget
│   │   └── BuchungAbgelehntException.cs     # die fachliche Ablehnung mit Grund
│   └── appsettings.json                     # EngineUrl, ProzessKey, Topic, WorkerId
└── tests/GenehmigungWorker.Tests/
    ├── GenehmigungsworkflowTests.cs         # Prozesstest in C# per REST gegen eure lokale Engine, zum Ansehen
    ├── BuchungssystemFake.cs                # Fake statt Fachsystem
    └── EngineHelper.cs                      # Test-Helfer für den Prozesstest
prozesstest-java/
├── pom.xml                                  # Engine im Speicher, cibseven-bpm-junit5, cibseven-bpm-assert, bpmn-to-code, Process Test Coverage
├── mvnw, mvnw.cmd                           # Maven Wrapper, lädt Maven beim ersten Lauf
├── src/main/resources/
│   ├── genehmigungsworkflow.bpmn            # Kopie der Entwickler-Fassung, byte-gleich
│   └── verbuchen-fehlerpfad.bpmn            # Kopie der Variante für den Bonus, byte-gleich
├── src/test/java/io/miragon/schulung/genehmigung/
│   └── GenehmigungsworkflowTest.java        # Prozesstest mit der Engine im Speicher
├── target/generated-test-sources/bpmn-to-code/   # entsteht beim Lauf: Klassen mit den IDs der Modelle, nicht im Repo
└── target/process-test-coverage/            # entsteht beim Lauf: je Testklasse ein Abdeckungsbericht, report.html
```

Die Musterlösung in Java unter `loesung/prozesstest-java/` ist genauso aufgebaut wie `prozesstest-java/`.

### Schichten oder hexagonal

Euer Worker unter `worker/` ist in Schichten geschnitten: Die Schleife in `Program.cs` holt den Task, der Handler liest die Variablen und ruft das Fachsystem hinter `IBuchungssystem`. `loesung/worker-hexagonal/` macht genau dasselbe nach Ports und Adaptern: Die Fachlogik steht in einem eigenen Projekt, `GenehmigungWorker.Domaene`, das die Engine nicht kennt, und Architekturtests prüfen das bei jedem Testlauf. Was anders ist, warum und wo dieser Schnitt an Grenzen stößt, steht in [loesung/worker-hexagonal/README.md](../loesung/worker-hexagonal/README.md). Umbauen müsst ihr nichts, die Fassung ist zum Lesen und Vergleichen da.

### Tests

| Ordner | Befehl | Braucht | Erwartet |
|---|---|---|---|
| `worker/` | `dotnet test` | laufenden Stack, deployten Umbau, Zugangsdaten, gestoppten Worker | 2 bestanden |
| `prozesstest-java/` | `./mvnw test` (PowerShell: `.\mvnw.cmd test`) | nichts davon | im Startstand 1 bestanden, 3 übersprungen |
| `loesung/prozesstest-java/` | `./mvnw test` | nichts davon | 10 bestanden |
| `loesung/worker-hexagonal/` | `dotnet test --filter "Kategorie!=Prozesstest"` | nichts davon | 14 bestanden |
| `loesung/worker-hexagonal/` | `dotnet test` | wie `worker/` | 17 bestanden |

Die Prozesstests in C# tragen `[Trait("Kategorie", "Prozesstest")]` und laufen per REST gegen die lokale Engine. Jeder Test startet eine eigene Instanz mit Business Key `prozesstest-...`, holt den External Task mit Filter auf diesen Business Key unter der Worker-ID `prozesstest` und löscht am Ende, was von seinen Instanzen noch offen ist. Läuft die Engine nicht, fehlt das Modell oder stimmen die Zugangsdaten nicht, nennt die Fehlermeldung des Tests die Ursache und den nächsten Schritt.

Der Prozesstest in Java braucht weder Stack noch Zugangsdaten noch einen gestoppten Worker: `cibseven-bpm-junit5` startet für die Testklasse eine Engine mit H2 im Speicher und deployt für jeden Testfall die Kopie des Modells aus `src/main/resources/`. Einen Job Executor hat diese Engine nicht, Speicherpunkte und Timer stößt der Test selbst an.

Der Test-Helfer `EngineHelper.cs` (im Test `_engine`) kapselt die REST-Aufrufe, je Methode ein REST-Endpunkt, etwa `StartAsync`, `GetTaskAsync`, `CompleteTaskAsync`, `FetchAndLockAsync`, `CompleteAsync`, `GetHistoryAsync`, `GetVariableAsync` und für die Gegenprobe `GetExternalTasksAsync`. Die lesenden Methoden warten auf den Zustand, statt nur einmal zu fragen: `GetTaskAsync` fragt bis zu zehn Sekunden lang nach, bis die Aufgabe da ist. `GetHistoryAsync`, `GetVariableAsync` und `GetExternalTasksAsync` warten vorher, bis an der Instanz kein Speicherpunkt mehr aussteht. Das braucht euer Modell: Der easyForm-Baustein setzt nach dem Start-Event, nach „Antrag prüfen“ und nach „Antrag nachbessern“ je einen Speicherpunkt, die Engine antwortet dort schon, und den Rest führt ihr Job Executor kurz danach im Hintergrund aus. Kommt der Zustand nicht, nennt die Fehlermeldung, was erwartet war und wo die Instanz steht. `FetchAndLockAsync` fragt bis zu 45 Sekunden lang nach, statt nach einem leeren fetchAndLock sofort aufzugeben: Die letzte Long-Polling-Anfrage eines eben gestoppten Workers bleibt in der Engine bis zu zehn Sekunden offen und kann den Task des Tests noch für 30 Sekunden sperren.

### Entscheidungen, wo die Folien offen sind

- `Einstellungen.cs` liest die Konfiguration und baut den `HttpClient` mit `EngineUrl` als `BaseAddress` und Basic Auth. Worker, Deployment und Prozesstest nutzen sie gemeinsam.
- `lockDuration` (30 s), `maxTasks` (5) und `asyncResponseTimeout` (10 s) stehen wie auf der Folie im Code von `FetchAndLockAsync`, nicht in `appsettings.json`.
- `ExternalTask.Variables` ist ein `Dictionary<string, object>` mit ausgepackten Werten: Text als `string`, ganze Zahlen als `long`, andere Zahlen als `double`, Wahrheitswerte als `bool`. Variablen ohne Wert (`null`) lässt `ToTask` weg, der Handler scheitert dann laut an einer fehlenden Variable.
- `betrag` liest der Handler als Text mit `decimal.Parse` und als Zahl mit `Convert.ToDecimal`, beides mit `CultureInfo.InvariantCulture`. Das easyForm speichert auch ein Feld vom Typ „Zahl“ als Text.
- `CompleteAsync` schickt die Werte typisiert: `string` als String, `int` als Integer, `long` als Long, `decimal` und `double` als Double, `bool` als Boolean.
- `IBuchungssystem` und die Simulation liegen unter `Fachsystem/`, getrennt von den Handlern.
- Die Schleife in `Program.cs` schreibt je eine Log-Zeile für geholt, erledigt, fehlgeschlagen und fachlich abgelehnt.
- Die Simulation speichert ihre Buchungen in `buchungen.json` neben der DLL (`worker/src/GenehmigungWorker/bin/Debug/net10.0/`). Den Pfad gibt `Program.cs` im Konstruktor mit und loggt ihn beim Start. So findet der Worker die Datei, egal wo ihr ihn startet, und sie landet nie im Repo. Die hexagonale Fassung hat ihre eigene Datei.
- Die Simulation lehnt jede Buchung über 50.000 Euro ab (`BudgetJeBuchung`) und speichert sie nicht. Die Ablehnung ist eine eigene Exception unter `Fachsystem/`, `BuchungAbgelehntException`. Der Handler reicht sie durch, erst die Schleife macht daraus ein `bpmnError`. Die Beträge in Meldung und Log stehen immer im deutschen Format, egal wie der Rechner eingestellt ist.
- `deploy` mit Pfad spielt eine andere Datei ein, etwa eine Variante unter `prozess/varianten/`. Deployment und Ressource heißen dann wie die Datei, nicht wie der `ProzessKey`.
- Der Prozesstest in C# nimmt Prozess-Key und Topic aus der Konfiguration (`_engine.ProzessKey`, `_engine.Topic`).
- Der Prozesstest in Java liest Process ID, IDs, Topic und Fehlercode aus Klassen, die bpmn-to-code von Miragon bei jedem Lauf aus den Modellkopien erzeugt, etwa `TASK_PRUEFEN.getValue()` statt `"Task_Pruefen"`. Variablennamen wie `entscheidung` bleiben Text, das Modell legt sie nicht als Ein- oder Ausgabe fest. Die Demo in Kapitel 10 (`GenehmigungsworkflowTag1Test`) schreibt die IDs als Text: Dort macht eine geänderte ID den Test erst beim Lauf rot, mit den Konstanten fällt sie schon beim Übersetzen auf.
- Die Tests im Startstand in Java sind mit `@Disabled` markiert statt rot. So läuft `./mvnw test` von Anfang an sauber durch, und ihr seht, welche Tests noch fehlen.
- Der Prozesstest in Java testet die Kopie der Entwickler-Fassung in `prozesstest-java/src/main/resources/`, nicht euer Modell in der Engine. Die GitHub Action hält die Kopie byte-gleich zu `loesung/genehmigungsworkflow-entwickler.bpmn`, ebenso die Kopie der Variante und die Kopien unter `loesung/prozesstest-java/`.
- Die Musterlösung in Java ist ein vollständiges, baubares Projekt neben dem Startstand, kein Satz einzelner Dateien. `.github/scripts/loesung-abgleich.sh` prüft, auch in der GitHub Action: Die Lösung enthält jede Datei des Startstands und weicht nur in den Übungsdateien ab. Die Liste der Übungsdateien steht im Skript.
- Die Demo in Kapitel 10 läuft in der Musterlösung, `loesung/prozesstest-java/`, mit der Klasse `GenehmigungsworkflowTag1Test` am Modell ohne External Task. Der Trainer ändert vorübergehend nur `loesung/prozesstest-java/src/main/resources/genehmigungsworkflow-tag1.bpmn` und setzt mit `git restore loesung/prozesstest-java` zurück. Der Startstand `prozesstest-java/` bleibt für die Übung unberührt, siehe [README des Projekts](../prozesstest-java/README.md#demo-kapitel-10-trainer).
- Im Java-Bonus holt der Test den External Task selbst mit `fetchAndLock` und antwortet mit `handleBpmnError` oder `complete`, wie der Worker. Einen Handler oder Fake aus C# braucht er nicht.
