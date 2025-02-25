#!/usr/bin/env pwsh
# changes-report.ps1
# Script to scan MicrOS codebase for the most recent commit and generate a markdown report

param(
    [Parameter(HelpMessage = "Number of commits to scan (default is 1, just the last commit)")]
    [int]$CommitCount = 1,
    
    [Parameter(HelpMessage = "Output markdown file path")]
    [string]$OutputFile = "changes-report.md",
    
    [Parameter(HelpMessage = "Git branch to scan (default is current branch)")]
    [string]$Branch = "",
    
    [Parameter(HelpMessage = "Optional author filter")]
    [string]$Author = ""
)

# Function to get friendly time format
function Get-FriendlyTimeFormat {
    param([DateTime]$dateTime)
    
    $now = Get-Date
    $timeSpan = $now - $dateTime
    
    if ($timeSpan.TotalDays -lt 1) {
        if ($timeSpan.TotalHours -lt 1) {
            return "$([math]::Floor($timeSpan.TotalMinutes)) minutes ago"
        }
        return "$([math]::Floor($timeSpan.TotalHours)) hours ago"
    }
    elseif ($timeSpan.TotalDays -lt 7) {
        return "$([math]::Floor($timeSpan.TotalDays)) days ago"
    }
    else {
        return $dateTime.ToString("yyyy-MM-dd")
    }
}

# Check if current directory is a git repository
if (-not (Test-Path ".git")) {
    Write-Host "Current directory is not a git repository. Please run this script from the root of your project." -ForegroundColor Red
    exit 1
}

# Additional git args for filtering
$gitArgs = @()
if ($Branch) {
    $gitArgs += $Branch
}
else {
    # Use current branch if none specified
    $gitArgs += "HEAD"
}

if ($Author) {
    $authorFilter = "--author=$Author"
}
else {
    $authorFilter = ""
}

# Get list of the latest N commits
$commitListCommand = "git log -n $CommitCount --pretty=format:`"%h|%an|%ad|%s|%ct`" --date=short $authorFilter $($gitArgs -join ' ')"
Write-Host "Executing: $commitListCommand" -ForegroundColor Gray
$commits = Invoke-Expression $commitListCommand

if (-not $commits) {
    Write-Host "No commits found." -ForegroundColor Yellow
    # Create an empty report
    $reportContent = @"
# MicrOS Changes Report

**Generated:** $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")

No commits found in the repository.
"@
    $reportContent | Out-File -FilePath $OutputFile -Encoding utf8
    Write-Host "Empty report created at $OutputFile" -ForegroundColor Green
    exit 0
}

# Process commits into organized structure
$commitData = @{}
$oldestCommitDate = $null
$newestCommitDate = $null

$commits -split "`n" | ForEach-Object {
    $parts = $_ -split "\|"
    if ($parts.Count -ge 5) {
        $hash = $parts[0]
        $author = $parts[1]
        $date = $parts[2]
        $message = $parts[3]
        $timestamp = [Int64]$parts[4]
        
        # Convert Unix timestamp to DateTime
        $commitDateTime = [DateTimeOffset]::FromUnixTimeSeconds($timestamp).DateTime

        # Track oldest and newest timestamps
        if ($null -eq $oldestCommitDate -or $commitDateTime -lt $oldestCommitDate) {
            $oldestCommitDate = $commitDateTime
        }
        if ($null -eq $newestCommitDate -or $commitDateTime -gt $newestCommitDate) {
            $newestCommitDate = $commitDateTime
        }
        
        if (-not $commitData.ContainsKey($date)) {
            $commitData[$date] = @()
        }
        
        $commitData[$date] += [PSCustomObject]@{
            Hash = $hash
            Author = $author
            Message = $message
            DateTime = $commitDateTime
        }
    }
}

# Get changed files for each commit
$fileChanges = @{}
$commits -split "`n" | ForEach-Object {
    $parts = $_ -split "\|"
    if ($parts.Count -ge 1) {
        $hash = $parts[0]
        $changedFiles = git show --name-status --oneline $hash | Select-Object -Skip 1
        $fileChanges[$hash] = @()
        
        $changedFiles | ForEach-Object {
            $change = $_
            if ($change -match '^([A-Z])\s+(.+)$') {
                $changeType = $Matches[1]
                $filePath = $Matches[2]
                
                # Determine change type
                $changeDesc = switch ($changeType) {
                    "A" { "Added" }
                    "M" { "Modified" }
                    "D" { "Deleted" }
                    "R" { "Renamed" }
                    default { $changeType }
                }
                
                $fileChanges[$hash] += [PSCustomObject]@{
                    Type = $changeDesc
                    Path = $filePath
                }
            }
        }
    }
}

# Group changes by type and component
function Get-FileComponent {
    param([string]$filePath)
    
    if ($filePath -match "^src/main/java/org/Finite/MicrOS/(.+?)/") {
        return $Matches[1]
    }
    elseif ($filePath -match "^filesystem/(.+?)/") {
        return "Filesystem - $($Matches[1])"
    }
    elseif ($filePath -match "^src/main/resources/(.+?)/") {
        return "Resources - $($Matches[1])"
    }
    elseif ($filePath -match "^docs/(.+?)/") {
        return "Documentation - $($Matches[1])"
    }
    else {
        # Extract directory or file extension
        if ($filePath -match "/") {
            $directory = ($filePath -split "/")[0]
            return $directory
        }
        else {
            $extension = [System.IO.Path]::GetExtension($filePath)
            if ($extension) {
                return "Project Files ($extension)"
            }
            return "Project Files"
        }
    }
}

# Format date range for report title
$reportDateRange = if ($CommitCount -eq 1) {
    "Latest Commit"
} else {
    "Latest $CommitCount Commits"
}

# Start building the markdown report
$reportContent = @"
# MicrOS Changes Report

**Generated:** $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")  
**Changes:** $reportDateRange

## Summary

"@

# Calculate summary statistics
$totalCommits = ($commits -split "`n").Count
$totalFilesChanged = 0
$fileChanges.Values | ForEach-Object {
    $totalFilesChanged += $_.Count
}

$componentChanges = @{}
$fileChanges.Values | ForEach-Object {
    $_ | ForEach-Object {
        $component = Get-FileComponent -filePath $_.Path
        if (-not $componentChanges.ContainsKey($component)) {
            $componentChanges[$component] = 0
        }
        $componentChanges[$component]++
    }
}

# Add summary to report
$reportContent += @"
- **Total Commits:** $totalCommits
- **Files Changed:** $totalFilesChanged
- **Components Modified:**
"@

$componentChanges.GetEnumerator() | Sort-Object -Property Value -Descending | ForEach-Object {
    $reportContent += "`n  - $($_.Key): $($_.Value) files"
}

# Add commit details
$reportContent += "`n`n## Commit Details`n"

$commitData.Keys | Sort-Object -Descending | ForEach-Object {
    $date = $_
    $friendlyDate = [DateTime]::ParseExact($date, "yyyy-MM-dd", $null)
    $dayName = $friendlyDate.ToString("dddd")
    $reportContent += "`n### $date ($dayName)`n`n"
    
    $commitData[$date] | Sort-Object -Property DateTime -Descending | ForEach-Object {
        $commit = $_
        $reportContent += "#### $($commit.Message)`n"
        $reportContent += "Commit: `[$($commit.Hash)`](commit/$($commit.Hash)) by $($commit.Author)`n`n"
        
        if ($fileChanges.ContainsKey($commit.Hash) -and $fileChanges[$commit.Hash].Count -gt 0) {
            $reportContent += "Changed files:`n"
            $fileChanges[$commit.Hash] | ForEach-Object {
                $fileChange = $_
                # Use a simpler link format that points directly to the file
                $reportContent += "- **$($fileChange.Type)**: [$($fileChange.Path)]($($fileChange.Path))`n"
            }
            $reportContent += "`n"
        }
    }
}

# Add section for detailed analysis of key components
$reportContent += @"

## Component Analysis

This section provides a more detailed view of changes in key system components.

"@

# Group files by component for detailed analysis
$componentFiles = @{}
$fileChanges.Values | ForEach-Object {
    $_ | ForEach-Object {
        $component = Get-FileComponent -filePath $_.Path
        if (-not $componentFiles.ContainsKey($component)) {
            $componentFiles[$component] = @()
        }
        
        # Only add the file if it hasn't been added yet
        $filePath = $_.Path
        if (-not ($componentFiles[$component] | Where-Object { $_.Path -eq $filePath })) {
            $componentFiles[$component] += $_
        }
    }
}

# Add component details
$componentFiles.Keys | Sort-Object | ForEach-Object {
    $component = $_
    $reportContent += "### $component`n`n"
    
    # Count by change type
    $addedCount = ($componentFiles[$component] | Where-Object { $_.Type -eq "Added" }).Count
    $modifiedCount = ($componentFiles[$component] | Where-Object { $_.Type -eq "Modified" }).Count
    $deletedCount = ($componentFiles[$component] | Where-Object { $_.Type -eq "Deleted" }).Count
    
    $reportContent += "**Summary:** $addedCount added, $modifiedCount modified, $deletedCount deleted`n`n"
    
    if ($componentFiles[$component].Count -gt 0) {
        $reportContent += "Files:`n"
        $componentFiles[$component] | Sort-Object -Property Path | ForEach-Object {
            $fileChange = $_
            # Use a simpler link format that points directly to the file
            $reportContent += "- **$($fileChange.Type)**: [$($fileChange.Path)]($($fileChange.Path))`n"
        }
        $reportContent += "`n"
    }
}

# Write report to file
$reportContent | Out-File -FilePath $OutputFile -Encoding utf8

Write-Host "Changes report generated successfully at $OutputFile" -ForegroundColor Green
Write-Host "Report covers the latest $CommitCount commit(s)" -ForegroundColor Cyan