$ErrorActionPreference = "Stop"

$baseDir = "D:\MySQL\MySQL Server 8.0"
$mysqld = Join-Path $baseDir "bin\mysqld.exe"
$mysql = Join-Path $baseDir "bin\mysql.exe"

$runtimeRoot = "D:\codex-mysql-local"
$dataDir = Join-Path $runtimeRoot "data"
$logDir = Join-Path $runtimeRoot "logs"
$stdoutLog = Join-Path $logDir "mysqld-stdout.log"
$stderrLog = Join-Path $logDir "mysqld-stderr.log"
$errLog = Join-Path $logDir "mysqld.err"
$pidFile = Join-Path $runtimeRoot "mysqld.pid"
$projectRoot = Split-Path -Parent $PSScriptRoot
$sourceSqlFile = Join-Path $projectRoot "yutu-admin\src\main\resources\sql\init.sql"
$sqlFile = Join-Path $runtimeRoot "init.sql"

function Test-Port {
    param([int]$Port)

    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $iar = $client.BeginConnect("127.0.0.1", $Port, $null, $null)
        $ok = $iar.AsyncWaitHandle.WaitOne(1200, $false)
        if (-not $ok) {
            $client.Close()
            return $false
        }
        $client.EndConnect($iar) | Out-Null
        $client.Close()
        return $true
    } catch {
        return $false
    }
}

New-Item -ItemType Directory -Force -Path $runtimeRoot, $dataDir, $logDir | Out-Null

if (-not (Test-Path (Join-Path $dataDir "mysql"))) {
    Write-Host "Initializing local MySQL data directory..."
    & $mysqld "--initialize-insecure" "--basedir=$baseDir" "--datadir=$dataDir" "--console"
}

if (-not (Test-Port -Port 3307)) {
    if (Test-Path $stdoutLog) { Remove-Item $stdoutLog -Force }
    if (Test-Path $stderrLog) { Remove-Item $stderrLog -Force }

    $args = @(
        "--basedir=""$baseDir""",
        "--datadir=""$dataDir""",
        "--port=3307",
        "--bind-address=127.0.0.1",
        "--skip-log-bin",
        "--mysqlx=0",
        "--console",
        "--log-error=""$errLog""",
        "--pid-file=""$pidFile"""
    )

    $proc = Start-Process -FilePath $mysqld -ArgumentList $args -WorkingDirectory $runtimeRoot -RedirectStandardOutput $stdoutLog -RedirectStandardError $stderrLog -PassThru -WindowStyle Hidden
    Write-Host "Local MySQL PID: $($proc.Id)"
    Start-Sleep -Seconds 8
}

if (-not (Test-Port -Port 3307)) {
    Write-Host "MySQL failed to start."
    if (Test-Path $stdoutLog) {
        Write-Host "--- stdout ---"
        Get-Content $stdoutLog -Tail 80
    }
    if (Test-Path $stderrLog) {
        Write-Host "--- stderr ---"
        Get-Content $stderrLog -Tail 80
    }
    if (Test-Path $errLog) {
        Write-Host "--- err log ---"
        Get-Content $errLog -Tail 80
    }
    exit 1
}

Write-Host "Importing init.sql into local MySQL..."
Copy-Item -Force $sourceSqlFile $sqlFile
cmd /c "`"$mysql`" -h 127.0.0.1 -P 3307 -u root --default-character-set=utf8mb4 < `"$sqlFile`""
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host "Local MySQL is ready on 127.0.0.1:3307"
