#!/usr/bin/env pwsh
# Build script for MicrOS and TextEditor app

# Function to build a Maven project
function Build-MavenProject {
    param(
        [string]$ProjectPath
    )
    
    Push-Location $ProjectPath
    try {
        Write-Host "Starting Maven build..." -ForegroundColor Cyan
        mvn clean package
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Maven build failed" -ForegroundColor Red
            throw "Maven build failed"
        }
        Write-Host "Maven build completed successfully!" -ForegroundColor Green
    }
    finally {
        Pop-Location
    }
}

# Function to find compression tool
function Get-CompressionTool {
    if ($IsWindows) {
        $7zPath = "C:\Program Files\7-Zip\7z.exe"
        if (Test-Path $7zPath) {
            return $7zPath
        }
    }
    else {
        # Check for 7z on Linux
        $7zLinux = Get-Command "7z" -ErrorAction SilentlyContinue
        if ($7zLinux) {
            return $7zLinux.Source
        }
        
        # Check for tar and bzip2 on Linux
        $tar = Get-Command "tar" -ErrorAction SilentlyContinue
        $bzip2 = Get-Command "bzip2" -ErrorAction SilentlyContinue
        if ($tar -and $bzip2) {
            return "tar"
        }
    }
    return $null
}

# Check for compression tools
$compressionTool = Get-CompressionTool
if (-not $compressionTool) {
    Write-Host "No compression tools found. Please install 7-Zip (Windows) or tar/bzip2 (Linux)" -ForegroundColor Red
    Write-Host "Windows: https://7-zip.org/" -ForegroundColor Yellow
    Write-Host "Linux: sudo apt-get install p7zip-full tar bzip2" -ForegroundColor Yellow
    exit 1
}

# Build the Maven project
Write-Host "Building the Maven project..." -ForegroundColor Cyan
Build-MavenProject -ProjectPath "."

#Copy the jar to the root dir

#find the jar
$jarPath = Get-ChildItem -Path "target" -Filter "jar-with-dependencies.jar" -Recurse 
$jarPath = $jarPath.FullName

#copy the jar to the root dir
Copy-Item -Path $jarPath -Destination "MicrOS.jar"


# Create distribution archives
Write-Host "Creating distribution packages..." -ForegroundColor Cyan
$baseArchiveName = "MicrOS-dist"

# Create archives based on platform
if ($compressionTool -eq "tar") {
    # Use native tar/bzip2 on Linux
    Write-Host "Creating .tar.bz2 archive using tar..." -ForegroundColor Cyan
    & tar -cjf "$baseArchiveName.tar.bz2" "MicrOS.jar" "filesystem"
}
else {
    # Use 7-Zip on either platform
    Write-Host "Creating .tar.bz2 archive using 7z..." -ForegroundColor Cyan
    & $compressionTool a -ttar "$baseArchiveName.tar" "target/MicrOS.jar" "filesystem"
    & $compressionTool a -tbzip2 "$baseArchiveName.tar.bz2" "$baseArchiveName.tar"
    Remove-Item "$baseArchiveName.tar" -Force

    Write-Host "Creating .7z archive..." -ForegroundColor Cyan
    & $compressionTool a -t7z "$baseArchiveName.7z" "MicrOS.jar" "filesystem" -mx=9
}

Write-Host "Build completed successfully!" -ForegroundColor Green
Write-Host "Distribution packages created:" -ForegroundColor Green
Write-Host "- $baseArchiveName.tar.bz2" -ForegroundColor Green
if ($compressionTool -ne "tar") {
    Write-Host "- $baseArchiveName.7z (highest compression)" -ForegroundColor Green
}
