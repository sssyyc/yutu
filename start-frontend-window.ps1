$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$webDir = Join-Path $projectRoot "yutu-web"
$nodeExe = "D:\node\node.exe"
$serverScript = Join-Path $webDir "scripts\serve-stable.cjs"

if (-not (Test-Path $nodeExe)) {
  throw "Node not found at $nodeExe"
}

if (-not (Test-Path $serverScript)) {
  throw "Stable frontend script not found: $serverScript"
}

Set-Location $webDir
Write-Host "Starting frontend at http://127.0.0.1:4174 ..."
& $nodeExe $serverScript
