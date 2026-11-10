# Installs the release build of each app on a phone, for the device audit (Completion step 1, ISSUE-NOV8.13-O55).
# Debug builds are debuggable, so ART runs them in its interpreter: the Monitor's first frame took 726 ms there and
# about 0.45 s as a release build (Nov9.3). Startup frames are therefore audited on the release build.
# The release APK is unsigned; it is signed here with the debug key, so it installs over the debug app and keeps its data.
# Usage:   powershell -File TESTS/install_release.ps1                          (both apps on the A15, after assembleRelease)
#          powershell -File TESTS/install_release.ps1 -Apps monitor
# Restore the debug builds after the audit: gradlew :app-sensor:installDebug :app-monitor:installDebug
# Exit code: 0 all installed, 1 an APK, the signer, or the install failed.
param([string]$Serial = "R58X40GV2AR", [string[]]$Apps = @("sensor", "monitor"))
$root = Split-Path -Parent $PSScriptRoot
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
$signer = Get-ChildItem (Join-Path $sdk "build-tools") -Directory | Sort-Object { [version]($_.Name -replace '[^\d.].*$', '') } |
    Select-Object -Last 1 | ForEach-Object { Join-Path $_.FullName "apksigner.bat" }
$keystore = Join-Path $env:USERPROFILE ".android\debug.keystore"
$exit = 0
# A native tool's stderr reaches PowerShell as ErrorRecords whose text prints as "RemoteException" (ISSUE-NOV9.8-O70):
# turn every line into its plain message, and report the line that names the reason (e.g. INSTALL_FAILED_VERSION_DOWNGRADE).
function Lines { process { $t = if ($_ -is [System.Management.Automation.ErrorRecord]) { $_.Exception.Message } else { "$_" }; if ($t.Trim()) { $t.Trim() } } }
function Reason([string[]]$out) {
    $hit = $out | Where-Object { $_ -match 'INSTALL_FAILED|Failure|error|Exception' } | Select-Object -Last 1
    if ($hit) { $hit } elseif ($out) { $out[-1] } else { "(no output)" }
}
foreach ($app in $Apps) {
    $dir = Join-Path $root "app-$app\build\outputs\apk\release"
    $unsigned = Join-Path $dir "app-$app-release-unsigned.apk"
    $signed = Join-Path $dir "app-$app-release-debugkey.apk"
    if (-not (Test-Path $unsigned)) { "RELEASE $app FAILED no $unsigned (run assembleRelease first)"; $exit = 1; continue }
    $out = @(& $signer sign --ks $keystore --ks-pass pass:android --key-pass pass:android --ks-key-alias androiddebugkey --out $signed $unsigned 2>&1 | Lines)
    if ($LASTEXITCODE -ne 0) { "RELEASE $app FAILED signing: $(Reason $out)"; $exit = 1; continue }
    $out = @(adb -s $Serial install -r $signed 2>&1 | Lines)
    if ($LASTEXITCODE -eq 0 -and ($out -match '^Success')) { "RELEASE $app INSTALLED on $Serial" } else { "RELEASE $app FAILED $(Reason $out)"; $exit = 1 }
}
exit $exit
