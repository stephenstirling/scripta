namespace Scripta.Launcher;

internal static class Program
{
    /// <summary>
    /// Normal start opens the window. <c>--quit</c> closes a running Scripta. <c>--selftest &lt;file&gt;</c>
    /// starts the engine without a window, writes the outcome to the file and exits; the build uses it
    /// to prove a packaged copy starts.
    /// </summary>
    [STAThread]
    private static int Main(string[] args)
    {
        if (args.Contains("--quit")) return SingleInstance.RequestQuit() ? 0 : 1;

        var selfTest = Array.IndexOf(args, "--selftest");
        if (selfTest >= 0) return SelfTest(selfTest + 1 < args.Length ? args[selfTest + 1] : "selftest.txt");

        if (!SingleInstance.TryClaim())
        {
            SingleInstance.RequestShow();
            return 0;
        }

        ApplicationConfiguration.Initialize();
        Application.Run(new MainForm());
        return 0;
    }

    private static int SelfTest(string resultFile)
    {
        using var engine = new Engine();
        try
        {
            engine.Start();
            var ready = engine.WaitUntilReadyAsync(TimeSpan.FromMinutes(3)).GetAwaiter().GetResult();
            File.WriteAllText(resultFile, ready ? $"READY {engine.Port}" : $"FAILED{Environment.NewLine}{engine.LogTail()}");
            return ready ? 0 : 1;
        }
        catch (Exception e) when (e is IOException or System.ComponentModel.Win32Exception)
        {
            File.WriteAllText(resultFile, $"FAILED {e.Message}");
            return 1;
        }
    }
}
