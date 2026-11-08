namespace Scripta.Launcher;

/// <summary>
/// One Scripta at a time. A second launch brings the first window forward, and <c>Scripta.exe --quit</c>
/// closes the running one, which is how Integra gets it out of the way before an update.
/// </summary>
internal static class SingleInstance
{
    private const string MutexName = @"Local\Scripta.SingleInstance";
    private const string ShowEventName = @"Local\Scripta.Show";
    private const string QuitEventName = @"Local\Scripta.Quit";
    private static readonly TimeSpan QuitWait = TimeSpan.FromSeconds(40);

    private static Mutex? _mutex;

    /// <summary>True when this process is the only Scripta; keeps the claim until the process ends.</summary>
    public static bool TryClaim()
    {
        _mutex = new Mutex(initiallyOwned: true, MutexName, out var first);
        return first;
    }

    public static void RequestShow() => Signal(ShowEventName);

    /// <summary>Asks a running Scripta to close and waits for it to go. True when none is left.</summary>
    public static bool RequestQuit()
    {
        if (!Mutex.TryOpenExisting(MutexName, out var running)) return true;
        running.Dispose();
        Signal(QuitEventName);

        var deadline = DateTime.UtcNow + QuitWait;
        while (DateTime.UtcNow < deadline)
        {
            if (!Mutex.TryOpenExisting(MutexName, out var still)) return true;
            still.Dispose();
            Thread.Sleep(250);
        }
        return false;
    }

    /// <summary>Starts watching for show and quit requests from other processes.</summary>
    public static void Listen(MainForm form)
    {
        var show = new EventWaitHandle(false, EventResetMode.AutoReset, ShowEventName);
        var quit = new EventWaitHandle(false, EventResetMode.AutoReset, QuitEventName);
        new Thread(() =>
        {
            while (true)
            {
                var which = WaitHandle.WaitAny(new WaitHandle[] { show, quit });
                if (!form.IsHandleCreated) continue;
                if (which == 0) form.BeginInvoke(new Action(form.ShowAndActivate));
                else form.BeginInvoke(new Action(form.Close));
            }
        })
        { IsBackground = true, Name = "scripta-single-instance" }.Start();
    }

    private static void Signal(string name)
    {
        if (EventWaitHandle.TryOpenExisting(name, out var handle))
        {
            using (handle) handle.Set();
        }
    }
}
