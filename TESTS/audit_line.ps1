# Prints the Completion step 5 audit line (SIMP-NOV8.100-S1), computed from the files, so no figure is estimated by hand.
# Usage:   powershell -NoProfile -ExecutionPolicy Bypass -File TESTS/audit_line.ps1 -Version Nov8.101-Tooling [-Testing PASSED]
# Figures: Rules = the SOT "Architectural Master Rules (N Rules)" header; SOT Count = 500 + Rules (the series since Oct28);
#          Open = gaps in "### 2. Open Gaps" of DOCS/issues.md by (High)/(Medium)/(Low); Ideas = SIMP-* lines, by (High)/(Medium), else Low;
#          Sub-items = numbered sub-items ("*   **N.M") in TESTS/TEST_PROCEDURE.md (from Nov8.101-Tooling; earlier values were hand counts);
#          QA = the verified-chapter count of the "**Summary:** N of M" line in TESTS/CHAPTER_TRIAGE.md.
param(
    [Parameter(Mandatory = $true)][string]$Version,
    [string]$Testing = "PASSED"
)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
function Read-Doc([string]$path) { Get-Content -Encoding UTF8 (Join-Path $root $path) }
function First-Number([string[]]$lines, [string]$pattern, [string]$what) {
    foreach ($l in $lines) { if ($l -match $pattern) { return [int]$Matches[1] } }
    throw "$what not found"
}

$rules = First-Number (Read-Doc "STATUS/SOT_MASTER_REQUIREMENTS.md") 'Architectural Master Rules \((\d+) Rules\)' "SOT rule-count header"
$qa = First-Number (Read-Doc "TESTS/CHAPTER_TRIAGE.md") '^\*\*Summary:\*\* (\d+) of \d+' "Triage summary line"
$subItems = @(Read-Doc "TESTS/TEST_PROCEDURE.md" | Where-Object { $_ -match '^\s*\*\s+\*\*\d+\.\d+' }).Count

$issues = Read-Doc "DOCS/issues.md"
function Get-Section([string[]]$lines, [string]$startPattern) {
    $out = @(); $inside = $false
    foreach ($l in $lines) {
        if ($inside -and ($l -match '^#{2,3} ' -or $l -eq '---')) { break }
        if ($inside) { $out += $l }
        if ($l -match $startPattern) { $inside = $true }
    }
    return $out
}
$gaps = Get-Section $issues '^### 2\. Open Gaps' | Where-Object { $_ -match '^- \*\*[A-Z]+-' }
$ideas = Get-Section $issues '^## .*Strategic Simplification Ideas' | Where-Object { $_ -match '^\*\s+\*\*SIMP-' }
function Count-By([object[]]$items, [string]$level) { @($items | Where-Object { $_ -match "\($level\)" }).Count }
$oH = Count-By $gaps 'High'; $oM = Count-By $gaps 'Medium'; $oL = Count-By $gaps 'Low'
$iH = Count-By $ideas 'High'; $iM = Count-By $ideas 'Medium'; $iL = @($ideas).Count - $iH - $iM
if ($oH + $oM + $oL -ne @($gaps).Count) { throw "An open gap has no (High)/(Medium)/(Low) severity" }

"- **${Version}: [SOT Count: $(500 + $rules) (Rules: $rules), Open: H:$oH, M:$oM, L:$oL, Ideas: H:$iH, M:$iM, L:$iL, Testing: $Testing (Sub-items: $subItems), QA: $qa]**"
