# PostToolUse hook (Documentation Rule D6): appends "HH:mm:ss  edited <path>" to chat_session.md
# after each file write, so the session log records changed files without model tokens.
# Writes only while chat_session.md exists (a working session), and never logs chat_session.md itself.
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$log = Join-Path $root 'chat_session.md'
if (-not (Test-Path $log)) { exit 0 }

$payload = [Console]::In.ReadToEnd() | ConvertFrom-Json
$path = $payload.tool_input.absolutePath
if (-not $path) { $path = $payload.tool_input.file_path }
if (-not $path) { exit 0 }

$full = [System.IO.Path]::GetFullPath($path)
if ($full -ieq [System.IO.Path]::GetFullPath($log)) { exit 0 }
$rel = if ($full.StartsWith($root, [StringComparison]::OrdinalIgnoreCase)) { $full.Substring($root.Length).TrimStart('\', '/') } else { $full }

Add-Content -Path $log -Value ("- {0}  edited {1}" -f (Get-Date -Format 'HH:mm:ss'), $rel.Replace('\', '/')) -Encoding utf8
