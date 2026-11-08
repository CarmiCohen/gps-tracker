# Sends one debug command to a debug build and prints its one-line answer, or checks the screen for a text,
# without screenshots (SIMP-NOV8.100-S1). Only the A15 is used by default; the S21FE is in personal use.
# Usage:   powershell -File TESTS/device_check.ps1 -App monitor -Command PAIR -Id T
#          powershell -File TESTS/device_check.ps1 -App monitor -Command ADD_HOME -Lat 32.08 -Lng 34.78
#          powershell -File TESTS/device_check.ps1 -App monitor -Command CLEAR_HOME
#          powershell -File TESTS/device_check.ps1 -App sensor -Command SET_ID -Id T2
#          powershell -File TESTS/device_check.ps1 -App sensor -Command STATE
#          powershell -File TESTS/device_check.ps1 -ScreenText "System Idle"      (text-only uiautomator dump)
# Commands: sensor SET_ID, STATE; monitor PAIR, UNPAIR, ADD_HOME, CLEAR_HOME, STATE (see DebugCommand.kt).
# Exit code: 0 OK, 1 rejected, failed, no answer, or text not found.
param(
    [ValidateSet("monitor", "sensor")][string]$App = "monitor",
    [string]$Command,
    [string]$Id,
    [string]$Lat,
    [string]$Lng,
    [string]$ScreenText,
    [string]$Serial = "R58X40GV2AR",
    [int]$TimeoutSec = 10
)
$adb = @("-s", $Serial)

if ($ScreenText) {
    & adb @adb shell uiautomator dump /sdcard/ui.xml | Out-Null
    $xml = (& adb @adb shell cat /sdcard/ui.xml) -join ""
    & adb @adb shell rm /sdcard/ui.xml | Out-Null
    $found = $xml -match [regex]::Escape($ScreenText)
    if ($found) { "SCREEN OK '$ScreenText' found"; exit 0 } else { "SCREEN FAIL '$ScreenText' not found"; exit 1 }
}
if (-not $Command) { "Usage: -App monitor|sensor -Command <name> [-Id x] [-Lat y -Lng z], or -ScreenText <text>"; exit 1 }

$pkg = "com.jd.app.$App"
$epoch = "$(& adb @adb shell date +%s)".Trim()
if ($epoch -notmatch '^\d+$') { "DEVICE FAIL $Serial not reachable"; exit 1 }
$since = "$epoch.000"
$extras = @()
if ($Id) { $extras += @("--es", "id", $Id) }
if ($Lat) { $extras += @("--es", "lat", $Lat) }
if ($Lng) { $extras += @("--es", "lng", $Lng) }
& adb @adb shell am broadcast -n "$pkg/.DebugCommandReceiver" -a "com.jd.debug.$Command" @extras | Out-Null

$deadline = (Get-Date).AddSeconds($TimeoutSec)
do {
    Start-Sleep -Milliseconds 500
    $line = & adb @adb logcat -d -T "$since" -v brief "REV_DEBUG_CMD:V" "*:S" |
        Where-Object { $_ -match 'REV_DEBUG_CMD' } | Select-Object -Last 1
} while (-not $line -and (Get-Date) -lt $deadline)

if (-not $line) { "DEVICE FAIL no REV_DEBUG_CMD answer from $pkg within ${TimeoutSec}s (debug build installed?)"; exit 1 }
$answer = ($line -replace '^.*?REV_DEBUG_CMD\(\s*\d+\):\s*', '')
if ($answer -match '^OK ') { "DEVICE $answer"; exit 0 } else { "DEVICE FAIL $answer"; exit 1 }
