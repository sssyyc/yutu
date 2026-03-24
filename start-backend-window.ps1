$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$adminDir = Join-Path $projectRoot "yutu-admin"
$jarPath = Join-Path $adminDir "target\yutu-admin-1.0.0.jar"
$javaExe = "D:\Java\jdk\bin\java.exe"

if (-not (Test-Path $javaExe)) {
  throw "Java not found at $javaExe"
}

if (-not (Test-Path $jarPath)) {
  throw "Backend jar not found: $jarPath. Run 'mvn -DskipTests package' in yutu-admin first."
}

$env:ALIPAY_ENABLED = [Environment]::GetEnvironmentVariable("ALIPAY_ENABLED", "User")
$env:ALIPAY_APP_ID = [Environment]::GetEnvironmentVariable("ALIPAY_APP_ID", "User")
$env:ALIPAY_MERCHANT_PRIVATE_KEY = [Environment]::GetEnvironmentVariable("ALIPAY_MERCHANT_PRIVATE_KEY", "User")
$env:ALIPAY_PUBLIC_KEY = [Environment]::GetEnvironmentVariable("ALIPAY_PUBLIC_KEY", "User")

Set-Location $adminDir
Write-Host "Starting backend at http://127.0.0.1:8080 ..."
& $javaExe -jar $jarPath
