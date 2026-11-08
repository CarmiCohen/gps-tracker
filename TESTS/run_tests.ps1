# Runs Gradle test tasks and prints one summary line, plus one line per failed test (SIMP-NOV8.100-S1).
# The full Gradle output goes to build/run_tests.log, read only when something failed.
# Usage:   powershell -File TESTS/run_tests.ps1                                   (all unit tests, Completion step 1)
#          powershell -File TESTS/run_tests.ps1 :core-shared:test                 (one module while working, D7)
#          powershell -File TESTS/run_tests.ps1 ":app-monitor:testDebugUnitTest --tests *Geofence*"
# Exit code: 0 all passed, 1 a test or the build failed.
param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Tasks)
if (-not $Tasks) { $Tasks = @("test") }
$root = Split-Path -Parent $PSScriptRoot
$log = Join-Path $root "build/run_tests.log"
New-Item -ItemType Directory -Force -Path (Split-Path $log) | Out-Null
$start = Get-Date

$gradleArgs = @()
foreach ($t in $Tasks) { $gradleArgs += ($t -split ' ') }
Push-Location $root
try {
    & (Join-Path $root "gradlew.bat") @gradleArgs --warning-mode all --console=plain *> $log
    $exit = $LASTEXITCODE
} finally { Pop-Location }

$tests = 0; $failures = 0; $skipped = 0; $failed = @()
$results = Get-ChildItem -Path $root -Recurse -Filter "TEST-*.xml" -File |
    Where-Object { $_.FullName -match '[\\/]build[\\/]test-results[\\/]' }
$fresh = @($results | Where-Object { $_.LastWriteTime -ge $start })
# Up-to-date test tasks write no new results. Old result files may belong to deleted tests, so they are not counted.
$cached = ($fresh.Count -eq 0 -and $exit -eq 0 -and (Select-String -Path $log -Pattern 'UP-TO-DATE|FROM-CACHE' -Quiet))
$fresh |
    ForEach-Object {
        [xml]$x = Get-Content -Raw $_.FullName
        $s = $x.testsuite
        $tests += [int]$s.tests; $skipped += [int]$s.skipped
        $failures += [int]$s.failures + [int]$s.errors
        foreach ($c in $s.testcase) {
            if ($c.failure -or $c.error) { $failed += "  FAILED $($s.name).$($c.name)" }
        }
    }
$warnings = @(Select-String -Path $log -Pattern '^w: |deprecat' ).Count
$status = if ($exit -eq 0 -and $failures -eq 0) { "PASSED" } else { "FAILED" }
if ($cached) { $tests = "up-to-date" }
"TESTS $status tests=$tests failures=$failures skipped=$skipped cached=$cached warningLines=$warnings gradleExit=$exit tasks='$($Tasks -join ' ')' log=build/run_tests.log"
$failed
if ($exit -ne 0 -and $failures -eq 0) {
    Select-String -Path $log -Pattern '^e: |FAILURE: |What went wrong' -Context 0, 2 | Select-Object -First 5 | ForEach-Object { "  $($_.Line)" }
}
if ($status -eq "PASSED") { exit 0 } else { exit 1 }
