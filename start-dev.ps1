$ErrorActionPreference = "Stop"

function Test-Port {
    param(
        [string]$TargetHost,
        [int]$Port
    )
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $iar = $client.BeginConnect($TargetHost, $Port, $null, $null)
        $ok = $iar.AsyncWaitHandle.WaitOne(1000, $false)
        if (-not $ok) {
            $client.Close()
            return $false
        }
        $client.EndConnect($iar)
        $client.Close()
        return $true
    } catch {
        return $false
    }
}

function Get-DbTarget {
    param([string]$JdbcUrl)

    if ([string]::IsNullOrWhiteSpace($JdbcUrl)) {
        $JdbcUrl = "jdbc:mysql://127.0.0.1:3307/yutu_travel?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false"
    }

    if ($JdbcUrl -match '^jdbc:mysql://(?<host>[^:/?]+)(:(?<port>\d+))?/') {
        return @{
            Host = $Matches['host']
            Port = if ($Matches['port']) { [int]$Matches['port'] } else { 3306 }
        }
    }

    return @{
        Host = '127.0.0.1'
        Port = 3307
    }
}

$dbTarget = Get-DbTarget -JdbcUrl $env:DB_URL

if (-not (Test-Port -TargetHost $dbTarget.Host -Port $dbTarget.Port)) {
    Write-Host "MySQL ($($dbTarget.Host):$($dbTarget.Port)) is not reachable. Start MySQL first."
    exit 1
}

if (-not (Test-Port -TargetHost "127.0.0.1" -Port 6379)) {
    Write-Host "Redis (127.0.0.1:6379) is not reachable. Start Redis first."
    exit 1
}

Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot\yutu-admin'; mvn spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot\yutu-web'; npm run dev"

Write-Host "Started backend and frontend. Frontend: http://localhost:5173 Backend: http://localhost:8080"
Write-Host "Database target: $($dbTarget.Host):$($dbTarget.Port)"