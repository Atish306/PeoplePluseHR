# PowerShell script to download portable Apache Maven 3.8.8 (Compatible with Java 8)
$ErrorActionPreference = "Stop"

$mavenVersion = "3.8.8"
$mavenZip = "apache-maven-$mavenVersion-bin.zip"
$mavenUrl = "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries/$mavenZip"
$targetDir = "$PSScriptRoot\tools\maven"

if (-not (Test-Path "$targetDir\bin\mvn.cmd")) {
    Write-Host "Downloading portable Apache Maven $mavenVersion..." -ForegroundColor Cyan
    New-Item -ItemType Directory -Force -Path "$PSScriptRoot\tools" | Out-Null
    $downloadPath = "$PSScriptRoot\tools\$mavenZip"
    
    Invoke-WebRequest -Uri $mavenUrl -OutFile $downloadPath
    Write-Host "Extracting Maven..." -ForegroundColor Cyan
    Expand-Archive -Path $downloadPath -DestinationPath "$PSScriptRoot\tools" -Force
    Rename-Item -Path "$PSScriptRoot\tools\apache-maven-$mavenVersion" -NewName "maven" -Force
    Remove-Item -Path $downloadPath -Force
    Write-Host "Maven installed to $targetDir" -ForegroundColor Green
} else {
    Write-Host "Portable Maven is already installed at $targetDir" -ForegroundColor Green
}

Write-Host "To start the application, run:" -ForegroundColor Yellow
Write-Host ".\tools\maven\bin\mvn spring-boot:run" -ForegroundColor White
