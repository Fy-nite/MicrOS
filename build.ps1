#!/usr/bin/env pwsh
# Build script for MicrOS and TextEditor app

# Function to build a Maven project
function Build-MavenProject {
    param(
        [string]$ProjectPath
    )
    
    Push-Location $ProjectPath
    try {
        mvn clean package
        if ($LASTEXITCODE -ne 0) {
            throw "Maven build failed"
        }
    }
    finally {
        Pop-Location
    }
}

# Ensure filesystem directory exists
if (-not (Test-Path "filesystem")) {
    New-Item -ItemType Directory -Path "filesystem"
}

if (-not (Test-Path "filesystem/apps")) {
    New-Item -ItemType Directory -Path "filesystem/apps"
}

# Build main MicrOS project
Write-Host "Building MicrOS..." -ForegroundColor Green
Build-MavenProject "."

# Build TextEditor app
Write-Host "Building TextEditor app..." -ForegroundColor Green
Build-MavenProject "apps/TextEditor.app"

# Copy the built jar to current directory
Copy-Item "target/MicrOS-1.0-SNAPSHOT-jar-with-dependencies.jar" "MicrOS.jar" -Force

# Initialize MicrOS
Write-Host "Initializing MicrOS..." -ForegroundColor Green
java -jar MicrOS.jar --init

# Copy TextEditor.app to filesystem/apps
Write-Host "Installing TextEditor app..." -ForegroundColor Green
if (Test-Path "filesystem/apps/TextEditor.app") {
    Remove-Item "filesystem/apps/TextEditor.app" -Recurse -Force
}
Copy-Item "apps/TextEditor.app" "filesystem/apps" -Recurse

# Create distribution zip
Write-Host "Creating distribution package..." -ForegroundColor Green
$zipName = "MicrOS-dist.zip"
if (Test-Path $zipName) {
    Remove-Item $zipName -Force
}

Compress-Archive -Path @("MicrOS.jar", "filesystem") -DestinationPath $zipName

Write-Host "Build completed successfully!" -ForegroundColor Green
Write-Host "Distribution package created: $zipName" -ForegroundColor Green