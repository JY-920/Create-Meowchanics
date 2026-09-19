param(
    [string]$CreateJar = 'C:/Users/16611/.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.20.1/6.0.8-291/428ec142ed283e98d4ea183771e0a7c8a9aeb02b/create-1.20.1-6.0.8-291-slim.jar'
)
$ErrorActionPreference = 'Stop'
$referenceRoot = Join-Path (Split-Path $PSScriptRoot -Parent) 'tests/build/engineer-reference'
New-Item -ItemType Directory -Path $referenceRoot -Force | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
$referenceArchive = [IO.Compression.ZipFile]::OpenRead($CreateJar)
try {
    $referencePaths = @(
        'models/block/hand_crank/handle.json', 'models/block/hand_crank/block.json',
        'models/block/seat.json', 'models/block/white_seat.json',
        'textures/block/axis.png', 'textures/block/axis_top.png',
        'textures/block/andesite_casing_short.png', 'textures/block/smooth_dark_log_top.png',
        'textures/block/seat/bottom.png', 'textures/block/seat/top_white.png', 'textures/block/seat/side_white.png'
    )
    foreach ($referencePath in $referencePaths) {
        $referenceEntry = $referenceArchive.GetEntry('assets/create/' + $referencePath)
        if ($null -eq $referenceEntry) { throw "Missing Create reference $referencePath" }
        $referenceTarget = Join-Path $referenceRoot $referencePath
        New-Item -ItemType Directory -Path (Split-Path $referenceTarget -Parent) -Force | Out-Null
        [IO.Compression.ZipFileExtensions]::ExtractToFile($referenceEntry, $referenceTarget, $true)
    }
} finally { $referenceArchive.Dispose() }
Write-Output "Extracted real Create crank and seat references to $referenceRoot"
