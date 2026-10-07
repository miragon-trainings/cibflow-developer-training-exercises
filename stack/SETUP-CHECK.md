# Setup-Check

Prüft in fünf Schritten, ob das Übungs-Repo auf einem Rechner läuft: Worker und Java-Projekte, CIB flow, Worker und der Prozesstest in C#. Jeder Schritt nennt, was ihr sehen müsst.

Ihr braucht Docker Desktop mit 8 GB Speicher, .NET SDK 10, JDK 21, Git und die Zugangsdaten für `harbor.cib.de`. Details stehen in der [README](README.md#voraussetzungen).

```bash
git clone https://github.com/miragon-trainings/cibflow-developer-training-exercises.git
cd cibflow-developer-training-exercises
```

Alle Schritte beginnen im Repo-Root. Unter Windows nehmt ihr `.\mvnw.cmd test` statt `./mvnw test`.

## 1. Bauen und testen, ohne Docker

```bash
cd worker
dotnet build                                   # erwartet: ohne Fehler und ohne Warnungen
cd ../prozesstest-java
./mvnw test                                    # erwartet: Tests run: 4, Skipped: 3
cd ../loesung/prozesstest-java
./mvnw test                                    # erwartet: Tests run: 10, Skipped: 0
cd ../..
```

Im Startstand `prozesstest-java/` sind drei Tests als übersprungen markiert, die schreiben die Teilnehmenden in Übung 9. Die Tests in Java brauchen weder Engine noch Zugangsdaten. Den Prozesstest in C# unter `worker/` startet ihr erst in Schritt 5: Er braucht die Engine aus Schritt 2 und die Zugangsdaten aus Schritt 4.

## 2. CIB flow starten

```bash
docker login harbor.cib.de
cd stack
docker compose up -d
docker compose logs init                       # wiederholen, bis "[init] Fertig." kommt
cd ..
```

Dann http://localhost:7083/client öffnen und als `demo` mit Passwort `demo` anmelden. Unter „Prozessmanagement“ über „Lokale Datei importieren“ die Datei `prozess/genehmigungsworkflow-projekt.zip` importieren, „Automatisch bereitstellen“ bleibt an.

## 3. Stack prüfen

```bash
cd stack
./smoke-test.sh                                # erwartet: "Alles grün.", Windows: in Git Bash "bash smoke-test.sh"
cd ..
```

Dabei darf kein Worker laufen. Der Smoke-Test spielt die Entwickler-Fassung `loesung/genehmigungsworkflow-entwickler.bpmn` ein, mit „Genehmigung verbuchen“ als External Task. Danach ist sie die neueste Version in der Engine.

## 4. Worker starten

```bash
cd worker
dotnet user-secrets set EngineBenutzer worker --project src/GenehmigungWorker
dotnet user-secrets set EnginePasswort worker --project src/GenehmigungWorker
dotnet run --project src/GenehmigungWorker     # muss ohne Fehler laufen, dann Strg+C
cd ..
```

Die Zugangsdaten gelten danach auch für den Prozesstest in C# und für die hexagonale Fassung unter `loesung/worker-hexagonal/`.

## 5. Prozesstest in C# gegen die Engine

```bash
cd worker
dotnet test                                    # erwartet: 2 bestanden
cd ..
```

Dabei darf kein Worker laufen, sonst holt er dem Test den Task weg. Voraussetzung ist Schritt 3: Ohne ihn liegt nur das Modell ohne External Task aus dem ZIP in der Engine, und `Genehmigter_Antrag_wird_verbucht` scheitert nach 45 Sekunden.

## Hinweise

- Hakt ein Schritt, helfen die [typischen Probleme](README.md#typische-probleme). Meldet sonst den Schritt und die Fehlermeldung.
- Aufräumen: im Ordner `stack/` mit `docker compose down`. Vor Übung 8 setzt ihr den Stack mit `docker compose down -v` zurück: Der Smoke-Test hat die Entwickler-Fassung eingespielt, und das Projekt ist schon importiert. Das löscht alle lokalen Projekte, Formulare und Instanzen.
