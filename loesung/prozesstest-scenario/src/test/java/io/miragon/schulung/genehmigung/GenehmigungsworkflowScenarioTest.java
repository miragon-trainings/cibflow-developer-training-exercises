package io.miragon.schulung.genehmigung;

import static org.cibseven.community.scenario.Scenario.run;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.cibseven.bpm.engine.test.Deployment;
import org.cibseven.bpm.engine.variable.Variables;
import org.cibseven.community.process_test_coverage.junit5.platform7.ProcessEngineCoverageExtension;
import org.cibseven.community.scenario.ProcessScenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Ausblick in Kapitel 10, kein Teil einer Übung: der Prozesstest als Szenario mit CIB seven Platform Scenario.
 * Dieselben vier Fälle wie GenehmigungsworkflowTag1Test (loesung/prozesstest-java): genehmigt, abgelehnt,
 * Nachbesserung und Timer, am selben Modell ohne External Task (genehmigungsworkflow-tag1.bpmn), mit derselben Engine
 * im Speicher. Nachbesserung und Timer laufen hier bis „Antrag genehmigt“, weil der Runner jeden Antrag zu Ende
 * führt, GenehmigungsworkflowTag1Test hört bei beiden an „Antrag prüfen“ auf.
 *
 * Wozu: Der Test legt vorab fest, was an jedem Wartezustand passiert, hier an jeder Aufgabe, die der Antrag erreicht.
 * Dann läuft der Antrag von selbst bis zum Ende durch, danach fragt der Test, was er durchlaufen hat. Das Verhalten
 * steht einmal in der Methode mit @BeforeEach und gilt für alle Tests, jeder Test überschreibt nur, was bei ihm
 * anders ist.
 *
 * Anders als der klassische Test (GenehmigungsworkflowTag1Test): Dort fragt der Test nach jedem Schritt, wo die
 * Instanz wartet, schließt die Aufgabe ab und stößt die Speicherpunkte (asyncAfter) und den Timer selbst an.
 * Hier führt der Runner die Jobs selbst aus. Für den Timer stellt er die Uhr der Engine vor, bis er fällig ist.
 * Kommt ein Speicherpunkt dazu oder fällt einer weg, bleibt der Test, wie er ist.
 * Anders als JGiven (loesung/prozesstest-jgiven): JGiven macht den Test als Folge von Schritten in Angenommen, Wenn,
 * Dann lesbar und schreibt einen Bericht. Hier gibt es keine Schritte im Test: Er beschreibt, wie sich die Beteiligten
 * an jedem Wartezustand verhalten, den Weg dazwischen nimmt der Antrag nach dem Modell.
 *
 * ProcessScenario ist ein Interface der Bibliothek, im Test steht ein Mock von Mockito dafür.
 * when(...).thenReturn(...) legt das Verhalten an einem Wartezustand fest, verify(...) fragt nach dem Lauf, was der
 * Antrag begonnen (hasStarted) oder beendet (hasFinished) hat. Die Engine sucht sich der Runner selbst: Es gibt
 * genau eine, die ProcessEngineCoverageExtension baut sie aus camunda.cfg.xml.
 *
 * Die IDs stehen als Text, wie in GenehmigungsworkflowTag1Test und in der JGiven-Demo: Das Projekt erzeugt keine
 * Konstanten mit bpmn-to-code. Ändert sich eine ID im Modell, übersetzt der Test weiter und wird erst im Lauf rot.
 *
 * Starten im Ordner loesung/prozesstest-scenario mit ./mvnw test (Windows: .\mvnw.cmd test). Über dem Testbaum
 * schreibt der Runner jeden Lauf mit: welche Aufgabe er erledigt und wann er die Uhr vorstellt. Den Abdeckungsbericht
 * schreibt der Testlauf unter
 * target/process-test-coverage/io.miragon.schulung.genehmigung.GenehmigungsworkflowScenarioTest/report.html.
 */
@ExtendWith(ProcessEngineCoverageExtension.class)
@Deployment(resources = "genehmigungsworkflow-tag1.bpmn")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Genehmigungsworkflow als Szenario (Platform Scenario)")
class GenehmigungsworkflowScenarioTest {

    /** Der Antrag als Szenario: sein Verhalten an jedem Wartezustand und, nach dem Lauf, was er durchlaufen hat. */
    ProcessScenario antrag = mock(ProcessScenario.class);

    /**
     * Das Verhalten für alle Tests an den fünf Aufgaben, die die vier Szenarien erreichen: Die genehmigende Stelle
     * genehmigt, jede andere Aufgabe wird sofort erledigt. „Genehmigende Stelle benachrichtigen“ in der Rücknahme
     * erreicht kein Szenario, sie hat kein Verhalten.
     * Fehlt das Verhalten für einen Wartezustand, an dem der Antrag ankommt, wird der Test rot.
     */
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

    @Test
    @Order(2)
    @DisplayName("Abgelehnt: Ende bei „Antrag abgelehnt“, nie „Genehmigung verbuchen“")
    void abgelehnt() {
        // Nur das Verhalten an "Antrag prüfen" ist anders, alles andere gilt aus @BeforeEach
        when(antrag.waitsAtUserTask("Task_Pruefen"))
            .thenReturn(task -> task.complete(Variables.putValue("entscheidung", "abgelehnt")));

        run(antrag).startByKey("Process_Genehmigung", antragsdaten()).execute();

        verify(antrag).hasFinished("Task_Ablehnen");
        verify(antrag).hasFinished("End_Abgelehnt");
        verify(antrag, never()).hasStarted("Task_Verbuchen");
    }

    @Test
    @Order(3)
    @DisplayName("Nachbesserung: einmal zurück an die Antragsteller:in, dann genehmigt")
    void nachbesserungDannGenehmigt() {
        // Zwei Verhalten für "Antrag prüfen": Mockito gibt sie der Reihe nach heraus, beim ersten Mal nachbessern,
        // beim zweiten Mal genehmigen. Dazwischen erledigt die Antragsteller:in "Antrag nachbessern" (@BeforeEach).
        when(antrag.waitsAtUserTask("Task_Pruefen")).thenReturn(
            task -> task.complete(Variables.putValue("entscheidung", "nachbessern")),
            task -> task.complete(Variables.putValue("entscheidung", "genehmigt")));

        run(antrag).startByKey("Process_Genehmigung", antragsdaten()).execute();

        verify(antrag, times(2)).hasFinished("Task_Pruefen");
        verify(antrag).hasFinished("Task_Nachbessern");
        verify(antrag).hasFinished("End_Genehmigt");
    }

    @Test
    @Order(4)
    @DisplayName("Timer: Entscheidung erst nach dem Timer, genau eine Erinnerung, dann genehmigt")
    void timerErinnertGenauEinmal() {
        // Die genehmigende Stelle entscheidet erst nach dem Timer an "Antrag prüfen" (im Modell PT3M,
        // fachlich 3 Tage): defer verschiebt die Aktion um PT4M, der Runner stellt dafür die Uhr der Engine vor.
        when(antrag.waitsAtUserTask("Task_Pruefen")).thenReturn(task ->
            task.defer("PT4M", () -> task.complete(Variables.putValue("entscheidung", "genehmigt"))));

        run(antrag).startByKey("Process_Genehmigung", antragsdaten()).execute();

        // Der Timer unterbricht nicht: "Erinnerung senden" läuft genau einmal, "Antrag prüfen" wartet weiter
        verify(antrag, times(1)).hasFinished("Task_Erinnern");
        verify(antrag).hasFinished("End_Genehmigt");
    }

    /**
     * Wie das Startformular, dazu antragsteller. Im laufenden System legt das Start-Event die angemeldete Person ab,
     * im Test ist niemand angemeldet.
     */
    private static Map<String, Object> antragsdaten() {
        return Variables.putValue("betrag", 1200)
            .putValue("begruendung", "Dienstreise")
            .putValue("antragsteller", "anna");
    }
}
