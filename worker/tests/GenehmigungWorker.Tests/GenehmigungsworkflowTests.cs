namespace GenehmigungWorker.Tests;

/// <summary>
/// Ein Beispiel zum Vorführen: So kann ein Prozesstest in C# aussehen. Für C# gibt es keine Engine im Speicher,
/// der Test spricht deshalb per REST mit eurer lokalen Engine und prüft Modell, Engine und den echten Handler
/// im Zusammenspiel. Statt des Fachsystems bekommt der Handler einen Fake.
/// Voraussetzung: Stack läuft, Modell mit dem Umbau aus Übung 8 ist bereitgestellt (dotnet run -- deploy),
/// der Worker ist gestoppt, sonst holt er dem Test den Task weg.
/// Starten im Ordner worker/: dotnet test
/// </summary>
[Trait("Kategorie", "Prozesstest")]
public class GenehmigungsworkflowTests : IDisposable
{
    // Test-Helfer: je Methode ein REST-Endpunkt, die lesenden Methoden warten auf den Zustand. Siehe EngineHelper.cs
    private readonly EngineHelper _engine = new();

    [Fact]
    public async Task Genehmigter_Antrag_wird_verbucht()
    {
        // Starten: wie das Startformular, mit eigenem Business Key je Lauf
        var instanz = await _engine.StartAsync(_engine.ProzessKey,
            new() { ["betrag"] = 1200m, ["begruendung"] = "Dienstreise" });

        // Warten: Die Instanz steht bei "Antrag prüfen"
        var aufgabe = await _engine.GetTaskAsync(instanz.Id);
        Assert.Equal("Task_Pruefen", aufgabe.TaskDefinitionKey);

        // Entscheiden: wie die genehmigende Stelle
        await _engine.CompleteTaskAsync(aufgabe.Id, new() { ["entscheidung"] = "genehmigt" });

        // Verbuchen: den External Task holen, den echten Handler rufen, complete senden
        var task = await _engine.FetchAndLockAsync(_engine.Topic, instanz.BusinessKey);
        var ergebnis = new GenehmigungVerbuchenHandler(new BuchungssystemFake()).Handle(task);
        await _engine.CompleteAsync(task, ergebnis);

        // Beenden: Die Instanz ist zu Ende, die Buchungsnummer steht in ihren Variablen
        Assert.Equal("COMPLETED", (await _engine.GetHistoryAsync(instanz.Id)).State);
        Assert.Equal("B-2026-0001", await _engine.GetVariableAsync(instanz.Id, "buchungsnummer"));
    }

    [Fact]
    public async Task Abgelehnter_Antrag_bekommt_keinen_External_Task()
    {
        // Gegenprobe: Mit abgelehnt nimmt die Instanz den anderen Weg am Gateway
        var instanz = await _engine.StartAsync(_engine.ProzessKey,
            new() { ["betrag"] = 1200m, ["begruendung"] = "Dienstreise" });
        var aufgabe = await _engine.GetTaskAsync(instanz.Id);
        Assert.Equal("Task_Pruefen", aufgabe.TaskDefinitionKey);
        await _engine.CompleteTaskAsync(aufgabe.Id, new() { ["entscheidung"] = "abgelehnt" });

        // "Ablehnung mitteilen" wartet als Aufgabe, einen External Task gibt es auf diesem Weg nie
        var mitteilen = await _engine.GetTaskAsync(instanz.Id);
        Assert.Equal("Task_Ablehnen", mitteilen.TaskDefinitionKey);
        Assert.Empty(await _engine.GetExternalTasksAsync(instanz.Id));
        Assert.Equal("ACTIVE", (await _engine.GetHistoryAsync(instanz.Id)).State);

        // Erst wenn auch diese Aufgabe abgeschlossen ist, endet die Instanz
        await _engine.CompleteTaskAsync(mitteilen.Id, new());
        Assert.Equal("COMPLETED", (await _engine.GetHistoryAsync(instanz.Id)).State);
        Assert.Null(await _engine.GetVariableAsync(instanz.Id, "buchungsnummer"));
    }

    public void Dispose() => _engine.Dispose();
}
