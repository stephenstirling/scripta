using System.Diagnostics;
using Microsoft.Web.WebView2.Core;
using Microsoft.Web.WebView2.WinForms;

namespace Scripta.Launcher;

/// <summary>
/// The Scripta window: a status line while the engine starts, then the UI in a WebView2 control. The
/// window never navigates away from the engine's own address; links to other sites open in the browser.
/// </summary>
internal sealed class MainForm : Form
{
    private static readonly TimeSpan StartupTimeout = TimeSpan.FromMinutes(3);

    private readonly Engine _engine = new();
    private readonly Label _status = new()
    {
        Text = "Starting Scripta…",
        Dock = DockStyle.Fill,
        TextAlign = ContentAlignment.MiddleCenter,
        Font = new Font("Segoe UI", 14F),
    };
    private readonly WebView2 _web = new() { Dock = DockStyle.Fill };
    private bool _closing;

    public MainForm()
    {
        Text = "Scripta";
        Icon = Icon.ExtractAssociatedIcon(Application.ExecutablePath);
        MinimumSize = new Size(900, 600);
        Size = new Size(1360, 880);
        StartPosition = FormStartPosition.CenterScreen;
        Controls.Add(_web);
        Controls.Add(_status);
        // The web view starts underneath the status line rather than hidden: a control that was never
        // visible has no window handle for WebView2 to attach to.
        _status.BringToFront();

        Shown += async (_, _) => await StartAsync();
        FormClosing += OnFormClosing;
        SingleInstance.Listen(this);
    }

    /// <summary>Brings the window forward when a second copy of the app is started.</summary>
    public void ShowAndActivate()
    {
        if (WindowState == FormWindowState.Minimized) WindowState = FormWindowState.Normal;
        Activate();
    }

    private async Task StartAsync()
    {
        try
        {
            _engine.Start();
            var userData = Path.Combine(_engine.DataDir, "WebView2");
            var environment = await CoreWebView2Environment.CreateAsync(userDataFolder: userData);
            await _web.EnsureCoreWebView2Async(environment);
            ConfigureWebView(_web.CoreWebView2);

            if (!await _engine.WaitUntilReadyAsync(StartupTimeout))
            {
                Fail($"Scripta's engine did not start.\n\n{_engine.LogTail(12)}\n\nThe full log is at {_engine.LogPath}");
                return;
            }
            _web.CoreWebView2.Navigate(_engine.Url);
        }
        catch (WebView2RuntimeNotFoundException)
        {
            Fail("Scripta needs the Microsoft Edge WebView2 Runtime, which was not found on this PC.\n\n" +
                 "Install it from https://developer.microsoft.com/microsoft-edge/webview2/ and start Scripta again.");
        }
        catch (Exception e) when (e is IOException or InvalidOperationException or System.ComponentModel.Win32Exception)
        {
            Fail($"Scripta could not start: {e.Message}");
        }
    }

    private void ConfigureWebView(CoreWebView2 core)
    {
        core.Settings.AreDevToolsEnabled = false;
        core.Settings.IsStatusBarEnabled = false;
        core.Settings.IsZoomControlEnabled = true;

        core.NavigationCompleted += (_, _) => _status.Visible = false;
        core.NavigationStarting += (_, e) =>
        {
            if (IsEngineAddress(e.Uri) || e.Uri.StartsWith("about:", StringComparison.Ordinal)) return;
            e.Cancel = true;
            OpenInBrowser(e.Uri);
        };
        core.NewWindowRequested += (_, e) =>
        {
            e.Handled = true;
            OpenInBrowser(e.Uri);
        };
        core.DocumentTitleChanged += (_, _) =>
        {
            var title = core.DocumentTitle;
            Text = string.IsNullOrWhiteSpace(title) ? "Scripta" : title;
        };
    }

    private bool IsEngineAddress(string uri) =>
        Uri.TryCreate(uri, UriKind.Absolute, out var parsed)
        && parsed.Port == _engine.Port
        && (parsed.Host == "127.0.0.1" || parsed.Host == "localhost");

    private static void OpenInBrowser(string uri)
    {
        if (Uri.TryCreate(uri, UriKind.Absolute, out var parsed) && parsed.Scheme is "http" or "https")
        {
            Process.Start(new ProcessStartInfo(parsed.AbsoluteUri) { UseShellExecute = true });
        }
    }

    private void Fail(string message)
    {
        MessageBox.Show(this, message, "Scripta", MessageBoxButtons.OK, MessageBoxIcon.Error);
        Close();
    }

    private void OnFormClosing(object? sender, FormClosingEventArgs e)
    {
        if (_closing) return;
        _closing = true;
        Hide();
        _engine.Dispose();
    }
}
