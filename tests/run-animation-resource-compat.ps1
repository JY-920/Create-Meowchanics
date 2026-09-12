param(
    [string[]]$JarPath = @(),
    [switch]$ExpectConflict
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$projectRoot = Split-Path $PSScriptRoot -Parent
$versions = @('forge-1.20.1', 'neoforge-1.21.1') | ForEach-Object {
    $match = Select-String -LiteralPath "$projectRoot/$_/gradle.properties" -Pattern '^mod_version=(.+)$'
    $match.Matches.Groups[1].Value
}
if ($versions[0] -ne $versions[1]) { throw 'Loader versions differ' }
$version = $versions[0]
if ($JarPath.Count -eq 0) {
    $JarPath = @(
        "$projectRoot/forge-1.20.1/build/libs/create-meowchanics-$version-forge-1.20.1.jar",
        "$projectRoot/neoforge-1.21.1/build/libs/create-meowchanics-$version-neoforge-1.21.1.jar"
    )
}
function Read-ZipText($entry) {
    if ($null -eq $entry) { throw 'Required JAR entry missing' }
    $reader = [IO.StreamReader]::new($entry.Open())
    try { return $reader.ReadToEnd() } finally { $reader.Dispose() }
}
function Get-ZipHash($entry) {
    if ($null -eq $entry) { throw 'Required JAR entry missing' }
    $stream = $entry.Open()
    $sha = [Security.Cryptography.SHA256]::Create()
    try { return [BitConverter]::ToString($sha.ComputeHash($stream)).Replace('-', '') }
    finally { $stream.Dispose(); $sha.Dispose() }
}
foreach ($jar in $JarPath) {
    $resolvedJar = (Resolve-Path -LiteralPath $jar).Path
    $zip = [IO.Compression.ZipFile]::OpenRead($resolvedJar)
    try {
        # Mirrors GeckoLib 4's listResources("animations", *.json) discovery
        # and FileLoader's required root "animations" object; no GeckoLib runtime dependency.
        $conflicts = @($zip.Entries | Where-Object {
            $_.FullName -match '^assets/[^/]+/animations/.+\.json$'
        } | ForEach-Object {
            $json = (Read-ZipText $_) | ConvertFrom-Json
            if ($json.animations -isnot [pscustomobject]) { $_.FullName }
        })
        if ($ExpectConflict) {
            if ($conflicts.Count -eq 0) { throw 'Negative control did not reproduce the resource collision' }
            Write-Output "PASS negative control: $([IO.Path]::GetFileName($jar)): $($conflicts -join ', ')"
            continue
        }
        if ($conflicts.Count -gt 0) { throw "GeckoLib would reject: $($conflicts -join ', ')" }
        $isNeo = [IO.Path]::GetFileName($jar).Contains('-neoforge-')
        $loader = if ($isNeo) { 'neoforge-1.21.1' } else { 'forge-1.20.1' }
        $metadataPath = if ($isNeo) { 'META-INF/neoforge.mods.toml' } else { 'META-INF/mods.toml' }
        $metadata = Read-ZipText ($zip.GetEntry($metadataPath))
        if ($metadata -notmatch ('(?m)^version\s*=\s*"' + [regex]::Escape($version) + '"')) {
            throw "Internal version differs from gradle.properties: $jar"
        }
        if ([IO.Path]::GetFileName($jar) -notlike "create-meowchanics-$version-*.jar") {
            throw "Artifact name differs from mod version: $jar"
        }
        foreach ($pair in @(
            @('cat_thomas_flare', 'CatStreetDanceAnimation'),
            @('cat_pipa_performance', 'CatPipaAnimation')
        )) {
            $resource = "assets/laowu/cat_animation_clips/$($pair[0]).json"
            if ($null -ne $zip.GetEntry("assets/laowu/animations/$($pair[0]).json")) {
                throw "Stale auto-scanned clip remains: $jar"
            }
            $sourceHash = (Get-FileHash -LiteralPath "$projectRoot/$loader/src/main/resources/$resource" -Algorithm SHA256).Hash
            if ((Get-ZipHash ($zip.GetEntry($resource))) -ne $sourceHash) {
                throw "Packaged clip differs from source: $resource"
            }
            $classText = Read-ZipText ($zip.GetEntry("cn/laowu/mod/client/$($pair[1]).class"))
            if (!$classText.Contains("/$resource")) { throw "Compiled loader points at wrong resource: $($pair[1])" }
        }
        Write-Output "PASS: $([IO.Path]::GetFileName($jar)): GeckoLib scan, old-path removal, both clip hashes, compiled paths, version $version"
    } finally { $zip.Dispose() }
}
