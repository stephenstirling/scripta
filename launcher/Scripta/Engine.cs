using System.Diagnostics;
using System.Net.Http;

namespace Scripta.Launcher;

/// <summary>
/// The Scripta engine: the Java backend that does the PDF work, started hidden on a loopback port and
/// stopped when the app closes. The window talks to it over HTTP, so nothing is exposed beyond this PC.
/// </summary>
/// <remarks>
/// The backend watches this process (TAURI_PARENT_PID) and a shutdown file (SCRIPTA_SHUTDOWN_FILE), so it
/// exits by itself, gracefully, if the app is closed or crashes. The names are the backend's desktop-shell
/// contract and predate this launcher.
/// </remarks>
internal sealed class Engine : IDisposable
{
    private static readonly TimeSpan GracefulStop = TimeSpan.FromSeconds(20);

    private readonly string _logPath;
    private readonly string _shutdownFile;
    private Process? _process;
    private StreamWriter? _log;

    public Engine()
    {
        DataDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Scripta");
        Port = PortStore.Choose(DataDir);
        _logPath = Path.Combine(DataDir, "logs", "engine.log");
        _shutdownFile = Path.Combine(Path.GetTempPath(), $"scripta-stop-{Environment.ProcessId}");
    }

    public string DataDir { get; }
    public int Port { get; }
    public string LogPath => _logPath;
    public string Url => $"http://127.0.0.1:{Port}/";
    public bool HasExited => _process is null || _process.HasExited;

    public void Start()
    {
        var root = AppContext.BaseDirectory;
        var java = Path.Combine(root, "runtime", "bin", "javaw.exe");
        var jar = Path.Combine(root, "app", "scripta.jar");
        if (!File.Exists(java)) throw new FileNotFoundException($"The Java runtime is missing: {java}");
        if (!File.Exists(jar)) throw new FileNotFoundException($"The Scripta engine is missing: {jar}");

        Directory.CreateDirectory(Path.GetDirectoryName(_logPath)!);
        _log = new StreamWriter(new FileStream(_logPath, FileMode.Create, FileAccess.Write, FileShare.ReadWrite))
        {
            AutoFlush = true,
        };
        File.Delete(_shutdownFile);

        var start = new ProcessStartInfo(java)
        {
            WorkingDirectory = DataDir,
            UseShellExecute = false,
            CreateNoWindow = true,
            RedirectStandardOutput = true,
            RedirectStandardError = true,
        };
        foreach (var arg in new[]
        {
            "-Dfile.encoding=UTF-8",
            "-Djava.awt.headless=true",
            "-Dscripta.user-data=true",
            $"-Dscripta.tools-path={Path.Combine(root, "tools")}",
            "-DBROWSER_OPEN=false",
            "-DSCRIPTA_TAURI_MODE=true",
            "-Dserver.address=127.0.0.1",
            $"-Dserver.port={Port}",
            "-jar",
            jar,
        })
        {
            start.ArgumentList.Add(arg);
        }
        start.Environment["TAURI_PARENT_PID"] = Environment.ProcessId.ToString();
        start.Environment["SCRIPTA_SHUTDOWN_FILE"] = _shutdownFile;

        var process = new Process { StartInfo = start, EnableRaisingEvents = true };
        process.OutputDataReceived += (_, e) => Write(e.Data);
        process.ErrorDataReceived += (_, e) => Write(e.Data);
        process.Start();
        process.BeginOutputReadLine();
        process.BeginErrorReadLine();
        _process = process;
    }

    /// <summary>Waits for the engine's status endpoint to answer UP; false if it exits or the time runs out.</summary>
    public async Task<bool> WaitUntilReadyAsync(TimeSpan timeout, CancellationToken cancel = default)
    {
        using var http = new HttpClient { Timeout = TimeSpan.FromSeconds(3) };
        var deadline = DateTime.UtcNow + timeout;
        while (DateTime.UtcNow < deadline && !cancel.IsCancellationRequested)
        {
            if (HasExited) return false;
            try
            {
                var body = await http.GetStringAsync($"{Url}api/v1/info/status", cancel).ConfigureAwait(false);
                if (body.Contains("UP", StringComparison.Ordinal)) return true;
            }
            catch (Exception e) when (e is HttpRequestException or TaskCanceledException)
            {
                // Not listening yet.
            }
            await Task.Delay(500, cancel).ConfigureAwait(false);
        }
        return false;
    }

    /// <summary>Asks the engine to shut down, and ends it if it has not gone within a few seconds.</summary>
    public void Stop()
    {
        var process = _process;
        if (process is null || process.HasExited) return;
        try
        {
            File.WriteAllText(_shutdownFile, "stop");
            if (!process.WaitForExit(GracefulStop)) process.Kill(entireProcessTree: true);
        }
        catch (Exception e) when (e is IOException or InvalidOperationException or UnauthorizedAccessException)
        {
            try { process.Kill(entireProcessTree: true); } catch (InvalidOperationException) { }
        }
        finally
        {
            File.Delete(_shutdownFile);
        }
    }

    public string LogTail(int lines = 40)
    {
        try
        {
            using var stream = new FileStream(_logPath, FileMode.Open, FileAccess.Read, FileShare.ReadWrite);
            using var reader = new StreamReader(stream);
            return string.Join(Environment.NewLine, reader.ReadToEnd().Split('\n').TakeLast(lines));
        }
        catch (IOException)
        {
            return "";
        }
    }

    public void Dispose()
    {
        Stop();
        _log?.Dispose();
        _process?.Dispose();
    }

    private void Write(string? line)
    {
        if (line is null) return;
        lock (this) _log?.WriteLine(line);
    }
}
