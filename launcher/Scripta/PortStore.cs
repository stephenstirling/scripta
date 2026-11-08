using System.Net;
using System.Net.Sockets;
using System.Text.Json;

namespace Scripta.Launcher;

/// <summary>
/// Remembers the loopback port the engine listens on. The UI's saved files and preferences live in the
/// browser storage of the page's origin, and the origin includes the port, so the port has to stay the
/// same from one launch to the next. DocuSign's redirect URI also names it.
/// </summary>
internal static class PortStore
{
    private const int PreferredPort = 41980;

    private sealed class Settings
    {
        public int Port { get; set; }
    }

    /// <summary>The saved port while it is free, otherwise the preferred one, otherwise any free port.</summary>
    public static int Choose(string dataDir)
    {
        var path = Path.Combine(dataDir, "launcher.json");
        var saved = Read(path)?.Port ?? 0;

        var port = new[] { saved, PreferredPort }.FirstOrDefault(p => p > 0 && IsFree(p));
        if (port == 0) port = AnyFreePort();

        if (port != saved) Write(path, new Settings { Port = port });
        return port;
    }

    private static bool IsFree(int port)
    {
        try
        {
            using var listener = new TcpListener(IPAddress.Loopback, port);
            listener.Start();
            return true;
        }
        catch (SocketException)
        {
            return false;
        }
    }

    private static int AnyFreePort()
    {
        using var listener = new TcpListener(IPAddress.Loopback, 0);
        listener.Start();
        return ((IPEndPoint)listener.LocalEndpoint).Port;
    }

    private static Settings? Read(string path)
    {
        try
        {
            return File.Exists(path) ? JsonSerializer.Deserialize<Settings>(File.ReadAllText(path)) : null;
        }
        catch (Exception e) when (e is IOException or JsonException or UnauthorizedAccessException)
        {
            return null;
        }
    }

    private static void Write(string path, Settings settings)
    {
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(path)!);
            File.WriteAllText(path, JsonSerializer.Serialize(settings));
        }
        catch (Exception e) when (e is IOException or UnauthorizedAccessException)
        {
            // Without a saved port the next launch just chooses again.
        }
    }
}
