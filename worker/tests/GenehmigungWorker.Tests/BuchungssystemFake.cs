namespace GenehmigungWorker.Tests;

/// <summary>
/// Ersetzt das Fachsystem im Prozesstest: Der Handler läuft echt, nur die Buchung ist gespielt.
/// Liefert immer B-2026-0001.
/// </summary>
public class BuchungssystemFake : IBuchungssystem
{
    public string Verbuchen(string schluessel, string antragsteller, decimal betrag, string begruendung)
        => "B-2026-0001";
}
