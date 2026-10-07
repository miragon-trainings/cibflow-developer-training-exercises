# Ausblick: Prozesstest als Szenario mit CIB seven Platform Scenario

Ein Ausblick in Kapitel 10, kein Teil einer Übung. Der Trainer zeigt hier eine andere Art, einen Prozesstest zu bauen: Der Test legt vorab fest, was an jedem Wartezustand passiert, lässt den Antrag dann von selbst bis zum Ende laufen und prüft danach, was er durchlaufen hat. Es sind dieselben vier Fälle wie in `GenehmigungsworkflowTag1Test` der Demo in [`loesung/prozesstest-java/`](../prozesstest-java/): genehmigt, abgelehnt, Nachbesserung und Timer, am selben Modell ohne External Task, mit derselben Engine im Speicher. Nachbesserung und Timer laufen hier bis „Antrag genehmigt“, weil der Runner jeden Antrag zu Ende führt, `GenehmigungsworkflowTag1Test` hört bei beiden an „Antrag prüfen“ auf. Wer mag, startet die Szenarien nach der Schulung selbst.

Ein eigenes Projekt, damit nur lädt, wer die Bibliothek aufruft. `prozesstest-java/`, `loesung/prozesstest-java/` und `loesung/prozesstest-jgiven/` bleiben, wie sie sind.

## Was CIB seven Platform Scenario ist

[CIB seven Platform Scenario](https://github.com/cibseven-community-hub/cibseven-platform-scenario) ist eine Community-Extension von CIB seven, geschrieben von Martin Schimak (plexiti), Lizenz Apache 2.0. Hier läuft der Runner, `org.cibseven.community.scenario:cibseven-platform-scenario-runner` 2.0.0 von Maven Central. Er ist gegen CIB seven 2.0.0 übersetzt und bringt keine eigene Engine und keine weiteren Bibliotheken mit, er nimmt die Engine des Projekts, hier CIB seven 2.2.0.

Der Runner kennt die Wartezustände eines Modells: Aufgaben, Nachrichten, Zwischenereignisse mit Timer und einige mehr. Für jeden legt der Test fest, was dort passiert, etwa „Aufgabe mit `entscheidung` = genehmigt abschließen“. Dann startet der Runner die Instanz und führt sie bis zum Ende. Speicherpunkte (`camunda:asyncAfter`) führt er selbst aus. Einen Timer wartet er nicht ab, er stellt die Uhr der Engine vor, bis der Timer fällig ist.

Das Verhalten steht in einem Objekt vom Typ `ProcessScenario`, einem Interface der Bibliothek. Im Test steht dafür ein Mock von [Mockito](https://site.mockito.org) (5.24.0, Lizenz MIT), so wie es die Bibliothek in ihrem README zeigt: `when(...).thenReturn(...)` legt das Verhalten fest, `verify(...)` fragt nach dem Lauf, was die Instanz begonnen und beendet hat.

Wie in `loesung/prozesstest-java/` läuft dazu [CIB seven Process Test Coverage](https://github.com/cibseven-community-hub/cibseven-process-test-coverage) für den Abdeckungsbericht.

## Starten

JDK 21 oder neuer. Maven braucht ihr nicht, das Projekt bringt den Maven Wrapper mit. Im Repo-Root:

```bash
# macOS, Linux, Git Bash
cd loesung/prozesstest-scenario
./mvnw test
```

```powershell
# Windows PowerShell
cd loesung\prozesstest-scenario
.\mvnw.cmd test
```

`./mvnw test` führt die vier Szenarien aus. Über dem Testbaum schreibt der Runner mit, was er in jedem Szenario tut. Der Timer-Fall sieht so aus, Datum und Uhrzeit des Laufs sind hier durch `<Start>` ersetzt, der Name der Aktion im Test durch `…`:

```
INFO * Starting scenario at <Start>
INFO | Completed startEvent         'Antrag gestellt' (StartEvent_Antrag @ Process_Genehmigung # 308)
INFO * Acting on userTask           'Antrag prüfen' (Task_Pruefen @ Process_Genehmigung # 308)
INFO | Fast-forwarding scenario to <Start + 3 Minuten>
INFO |-- Completed boundaryTimer      '3 Tage ohne Entscheidung' (Boundary_Timer @ Process_Genehmigung # 308)
INFO   * Acting on userTask           'Erinnerung senden' (Task_Erinnern @ Process_Genehmigung # 308)
INFO   | Completed userTask           'Erinnerung senden' (Task_Erinnern @ Process_Genehmigung # 308)
INFO   | Completed noneEndEvent       'Erinnert' (End_Erinnert @ Process_Genehmigung # 308)
INFO   | Fast-forwarding scenario to <Start + 4 Minuten>
INFO   |-- Executing deferred action on 'Antrag prüfen' (Task_Pruefen @ Process_Genehmigung # 308 : …)
INFO     | Completed userTask           'Antrag prüfen' (Task_Pruefen @ Process_Genehmigung # 308)
INFO     | Completed exclusiveGateway   'Entscheidung?' (Gateway_Entscheidung @ Process_Genehmigung # 308)
INFO     * Acting on userTask           'Genehmigung verbuchen' (Task_Verbuchen @ Process_Genehmigung # 308)
INFO     | Completed userTask           'Genehmigung verbuchen' (Task_Verbuchen @ Process_Genehmigung # 308)
INFO     | Completed noneEndEvent       'Antrag genehmigt' (End_Genehmigt @ Process_Genehmigung # 308)
INFO     * Finishing scenario at <Start + 4 Minuten>
```

- `Acting on`: Der Antrag wartet an einer Aufgabe, der Runner führt das Verhalten aus, das der Test dafür festgelegt hat.
- `Completed`: Ein Element ist beendet, mit Beschriftung und ID aus dem Modell. `# 308` ist die ID der Instanz.
- `Fast-forwarding`: Der Runner stellt die Uhr der Engine vor, zum Timer (PT3M, fachlich 3 Tage) und zur verschobenen Entscheidung an „Antrag prüfen“. Was danach passiert, ist eingerückt. Gewartet wird nicht.

Darunter steht der Testbaum:

```
── Genehmigungsworkflow als Szenario (Platform Scenario) - 1.343 s
   ├─ ✔ Genehmigt: Ende bei „Antrag genehmigt“, nie „Ablehnung mitteilen“ - 0.270 s
   ├─ ✔ Abgelehnt: Ende bei „Antrag abgelehnt“, nie „Genehmigung verbuchen“ - 0.050 s
   ├─ ✔ Nachbesserung: einmal zurück an die Antragsteller:in, dann genehmigt - 0.057 s
   └─ ✔ Timer: Entscheidung erst nach dem Timer, genau eine Erinnerung, dann genehmigt - 0.041 s

Results:

Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
```

Maven setzt vor die Zeilen ab dem Testbaum `[INFO]`, der Block hier lässt das weg. Unter Windows können Umlaute, „“, ✔ und ✘ als Fragezeichen erscheinen, dann vorher `chcp 65001` ausführen. Hilft das nicht, schaltet `.\mvnw.cmd test "-Dbaum.theme=ASCII"` den Baum auf `+--`, `[OK]` und `[XX]` um.

Den Abdeckungsbericht schreibt `./mvnw test`, wie in `loesung/prozesstest-java/`:

```
target/process-test-coverage/io.miragon.schulung.genehmigung.GenehmigungsworkflowScenarioTest/report.html
```

Die vier Szenarien durchlaufen 21 von 27 Elementen und Pfeilen, `77.78%`, so viel wie `GenehmigungsworkflowTag1Test`. Weiß bleibt die Rücknahme, für sie gibt es hier kein Szenario.

## Beim ersten Lauf

Der erste `./mvnw test` lädt die Bibliotheken. Lief `prozesstest-java/` schon einmal, liegt das meiste bereits unter `~/.m2/`, dann lädt der erste Lauf rund 12 MB dazu, vor allem Byte Buddy, das Mockito mitbringt. Ohne diesen Lauf sind es rund 49 MB, dazu Maven selbst mit rund 9 MB. Lief `prozesstest-java/` vorher, stehen beim ersten Lauf rund 30 Zeilen `Artifact ... is present in the local repository, but cached from a remote repository ID that is unavailable in current build context ...` über der Ausgabe. Sie sind harmlos, ab dem zweiten Lauf fehlen sie.

Danach geht es ohne Netz: `./mvnw -o test`. Hinter einem Proxy hilft dasselbe wie für `prozesstest-java/`, siehe [Hinter einem Proxy](../../prozesstest-java/README.md#hinter-einem-proxy).

## Wie ein Szenario aussieht

```java
@BeforeEach
void jedeAufgabeWirdSofortErledigt() {
    when(antrag.waitsAtUserTask("Task_Pruefen"))
        .thenReturn(task -> task.complete(Variables.putValue("entscheidung", "genehmigt")));
    when(antrag.waitsAtUserTask("Task_Verbuchen")).thenReturn(task -> task.complete());
    when(antrag.waitsAtUserTask("Task_Ablehnen")).thenReturn(task -> task.complete());
    when(antrag.waitsAtUserTask("Task_Nachbessern")).thenReturn(task -> task.complete());
    when(antrag.waitsAtUserTask("Task_Erinnern")).thenReturn(task -> task.complete());
}

@Test
@Order(1)
@DisplayName("Genehmigt: Ende bei „Antrag genehmigt“, nie „Ablehnung mitteilen“")
void genehmigt() {
    run(antrag).startByKey("Process_Genehmigung", antragsdaten()).execute();

    verify(antrag).hasFinished("Task_Verbuchen");
    verify(antrag).hasFinished("End_Genehmigt");
    verify(antrag, never()).hasStarted("Task_Ablehnen");
}
```

- `antrag` ist der Mock für `ProcessScenario`. Die Methode mit `@BeforeEach` legt das Verhalten an den fünf Aufgaben fest, die die vier Szenarien erreichen, einmal für alle Tests: Die genehmigende Stelle genehmigt, jede andere Aufgabe wird sofort erledigt. „Genehmigende Stelle benachrichtigen“ in der Rücknahme erreicht kein Szenario, sie hat kein Verhalten. Ein Test überschreibt nur, was bei ihm anders ist. In `abgelehnt()` ist das allein „Antrag prüfen“ mit `entscheidung` = abgelehnt.
- `run(antrag).startByKey(...).execute()` startet den Antrag mit `betrag`, `begruendung` und `antragsteller` wie das Startformular und führt ihn bis zum Ende. Die Speicherpunkte hinter „Antrag gestellt“, „Antrag prüfen“ und „Antrag nachbessern“ stößt der Runner selbst an, anders als `speicherpunktAnstossen` in `GenehmigungsworkflowTag1Test`.
- `verify(antrag).hasFinished(...)` prüft nach dem Lauf, dass der Antrag ein Element genau einmal beendet hat, hier „Genehmigung verbuchen“ und „Antrag genehmigt“. `never()` heißt nie, `times(2)` genau zweimal. Ein Verhalten aus `@BeforeEach`, das ein Szenario nie braucht, meldet der Mock nicht: Ohne die Zeile mit `Task_Verbuchen` bliebe `genehmigt()` grün, auch wenn „Genehmigung verbuchen“ aus dem Weg fällt.
- `nachbesserungDannGenehmigt()` gibt „Antrag prüfen“ zwei Verhalten: Mockito gibt sie der Reihe nach heraus, erst nachbessern, dann genehmigen.
- `timerErinnertGenauEinmal()` verschiebt die Entscheidung mit `task.defer("PT4M", () -> task.complete(...))` hinter den Timer. Der Runner stellt die Uhr vor, „Erinnerung senden“ läuft genau einmal, danach endet der Antrag bei „Antrag genehmigt“.

Kommt an einer Stelle ein Speicherpunkt dazu oder fällt einer weg, bleibt der Test, wie er ist. Ausprobiert am Speicherpunkt hinter „Antrag prüfen“: Ohne `camunda:asyncAfter="true"` an `Task_Pruefen` in der Modellkopie bleiben alle vier Szenarien grün. `GenehmigungsworkflowTag1Test` mit derselben Änderung wird dreimal rot, weil er den Speicherpunkt selbst anstößt. Das ist zugleich die Grenze: Ob genau dort ein Speicherpunkt sitzt, an wen eine Aufgabe geht, welche Variablen der Antrag trägt und wann genau der Timer fällig ist, prüfen die Szenarien nicht, das prüft weiter `GenehmigungsworkflowTag1Test`. Mit `PT1M` statt `PT3M` am Timer oder mit „Antrag nachbessern“ an der Gruppe `genehmiger` statt an `${antragsteller}` bleiben alle vier Szenarien grün.

Anders als JGiven in [`loesung/prozesstest-jgiven/`](../prozesstest-jgiven/) gibt es im Test keine Schritte in Angenommen, Wenn, Dann und keinen Bericht in Sätzen. Der Test beschreibt, wie sich die Beteiligten an jedem Wartezustand verhalten, den Weg dazwischen nimmt der Antrag nach dem Modell.

## Wird ein Szenario rot

Fehlt das Verhalten für einen Wartezustand, an dem der Antrag ankommt, bricht der Runner dort ab. Mit der ID `Task_Pruefen2` statt `Task_Pruefen` für „Antrag prüfen“ in der Modellkopie werden alle vier Szenarien rot. Die ID ändert ihr wie in Variante A der [Demo in `prozesstest-java/`](../../prozesstest-java/README.md#demo-kapitel-10-trainer), mit dem Modeler oder im Texteditor mit Suchen und Ersetzen, Option „Nur ganzes Wort“. Ändert ihr nur das Attribut `id`, lässt sich das Modell nicht mehr deployen („Could not parse BPMN process“). Mit allen Vorkommen geändert etwa:

```
GenehmigungsworkflowScenarioTest.genehmigt:86 Process Instance {Process_Genehmigung:1:3, 8} waits at an unexpected UserTask 'Task_Pruefen2'.
```

Darunter steht `Tests run: 4, Failures: 4, Errors: 0, Skipped: 0`.

Schlägt ein `verify` fehl, meldet Mockito den erwarteten Aufruf und listet darunter alles, was der Antrag stattdessen begonnen und beendet hat. Mit `PT5M` statt `PT3M` am Timer in der Modellkopie kommt die Entscheidung vor dem Timer, nur der Timer-Fall wird rot:

```
GenehmigungsworkflowScenarioTest.timerErinnertGenauEinmal:137
Argument(s) are different! Wanted:
processScenario.hasFinished(
    "Task_Erinnern"
);
```

Zurück geht es im Ordner `loesung/prozesstest-scenario/` mit `git restore .`, danach zeigt `git status` keine Änderung. Den Abdeckungsbericht unter `target/` setzt das nicht zurück: Den grünen Stand zeigt er erst wieder nach `./mvnw test`.

## Was drin ist

- `src/main/resources/genehmigungsworkflow-tag1.bpmn`: Kopie von `prozess/genehmigungsworkflow.bpmn`, dem fertigen Modell nach Übung 7, nur mit User Tasks. Dieselbe Kopie wie in `loesung/prozesstest-java/` und `loesung/prozesstest-jgiven/`, die CI prüft, dass sie byte-gleich bleibt. Ändert sich der Startstand, kopiert ihr sie im Repo-Root neu: `cp prozess/genehmigungsworkflow.bpmn loesung/prozesstest-scenario/src/main/resources/genehmigungsworkflow-tag1.bpmn`.
- `src/test/java/io/miragon/schulung/genehmigung/GenehmigungsworkflowScenarioTest.java`: die vier Szenarien.
- `src/test/resources/camunda.cfg.xml`: die Engine im Speicher, ohne Job Executor, mit den Listenern für den Abdeckungsbericht. Die Engine sucht sich der Runner selbst, es gibt genau eine.
- `src/test/resources/simplelogger.properties`: Die Engine schreibt nur Warnungen, der Runner schreibt seinen Lauf.
- `src/test/resources/mockito-extensions/`: Mockito baut den Mock ohne Java Agent. Mit der Voreinstellung stünden über dem Lauf Warnungen von Mockito und Java.
- `pom.xml`: Engine und Testbibliotheken wie in `loesung/prozesstest-java/`, ohne bpmn-to-code und ohne `cibseven-bpm-assert`, dazu der Runner und Mockito.
- `.mvn/jvm.config`: stellt Maven leiser, übrig bleiben der Lauf, der Testbaum und die Meldungen.

Die IDs stehen als Text wie in `GenehmigungsworkflowTag1Test` und in der JGiven-Demo, das Projekt erzeugt keine Konstanten mit bpmn-to-code. Eine geänderte ID fällt deshalb erst im Lauf auf, wie oben.
