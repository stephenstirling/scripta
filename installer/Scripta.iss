; Scripta's Windows installer (Inno Setup 6). Built by .github/workflows/windows.yml:
;   ISCC.exe /DAppVersion=3.2.0 /DSourceDir=<folder holding Scripta.exe, app\, runtime\, tools\> installer\Scripta.iss
;
; Installs for the current user by default (%LOCALAPPDATA%\Programs\Scripta, no administrator prompt), so Integra
; can update it silently; the first page offers "all users" (Program Files) instead. Either way Scripta gets a
; Start menu entry, an optional desktop icon and an entry in Settings > Apps > Installed apps with Uninstall.
; Settings, sign-ins and logs live in %LOCALAPPDATA%\Scripta, which the launcher owns: uninstalling leaves them.
;
; Integra installs it with: /VERYSILENT /SUPPRESSMSGBOXES /NORESTART /CURRENTUSER

#ifndef AppVersion
  #error Pass /DAppVersion=x.y.z
#endif
#ifndef SourceDir
  #error Pass /DSourceDir=<packaged Scripta folder>
#endif

[Setup]
; Never change AppId: it is how Windows and later versions recognise an existing install.
AppId={{6F0C2E1D-8B47-4C3A-9E55-2D7A41B9C083}
AppName=Scripta
AppVersion={#AppVersion}
AppVerName=Scripta {#AppVersion}
AppPublisher=Stephen Stirling
AppPublisherURL=https://github.com/stephenstirling/scripta
AppSupportURL=https://github.com/stephenstirling/scripta/issues
AppUpdatesURL=https://github.com/stephenstirling/scripta/releases
VersionInfoVersion={#AppVersion}
VersionInfoProductName=Scripta
VersionInfoDescription=Scripta Setup

DefaultDirName={autopf}\Scripta
DisableProgramGroupPage=yes
DisableDirPage=auto
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog commandline
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
MinVersion=10.0.17763

UninstallDisplayName=Scripta
UninstallDisplayIcon={app}\Scripta.exe
SetupIconFile=..\launcher\Scripta\Scripta.ico
WizardStyle=modern
LicenseFile=..\LICENSE

OutputDir=..\release
OutputBaseFilename=Scripta-{#AppVersion}-setup-x64
; Several GB of bundled programs: lzma2 at "fast" with threads keeps the build inside the job's time limit,
; and solid compression still makes the download smaller than the old zip.
Compression=lzma2/fast
SolidCompression=yes
LZMAUseSeparateProcess=yes
LZMANumBlockThreads=4

; Scripta is closed by its own --quit (see [Code]) before files are replaced, which also stops the engine
; behind the window; Windows' Restart Manager can't see that the two belong together.
CloseApplications=no
RestartApplications=no

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[InstallDelete]
; An update replaces these folders whole, so files a newer version dropped (old jars, old tool versions) don't linger.
Type: filesandordirs; Name: "{app}\app"
Type: filesandordirs; Name: "{app}\runtime"
Type: filesandordirs; Name: "{app}\tools"

[Files]
Source: "{#SourceDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\Scripta"; Filename: "{app}\Scripta.exe"
Name: "{autodesktop}\Scripta"; Filename: "{app}\Scripta.exe"; Tasks: desktopicon

[Run]
Filename: "{app}\Scripta.exe"; Description: "{cm:LaunchProgram,Scripta}"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
; Anything the program folder gained after installing. %LOCALAPPDATA%\Scripta (the user's data) is not touched.
Type: filesandordirs; Name: "{app}"

[Code]
procedure QuitScripta(const Exe: String);
var
  Code: Integer;
begin
  if FileExists(Exe) then
    Exec(Exe, '--quit', '', SW_HIDE, ewWaitUntilTerminated, Code);
end;

function PrepareToInstall(var NeedsRestart: Boolean): String;
begin
  QuitScripta(ExpandConstant('{app}\Scripta.exe'));
  Result := '';
end;

function InitializeUninstall(): Boolean;
begin
  QuitScripta(ExpandConstant('{app}\Scripta.exe'));
  Result := True;
end;
