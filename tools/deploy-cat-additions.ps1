param(
    [ValidatePattern('^[a-z0-9-]+$')][string]$Label='cat-additions',
    [ValidatePattern('^\d+\.\d+\.\d+$')][string]$Version='2.2.2',
    [ValidatePattern('^build(?:-[a-z0-9-]+)?$')][string]$BuildDirectory='build',
    [switch]$AllowFreshInstall
)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$targets=@(
    @{Port='forge-1.20.1'; Instance='D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发'},
    @{Port='neoforge-1.21.1'; Instance='D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发'}
)
function Read-LaowuJar([string]$path) {
    $archive=[IO.Compression.ZipFile]::OpenRead($path)
    try {
        foreach($entryName in @('META-INF/mods.toml','META-INF/neoforge.mods.toml')) {
            $entry=$archive.GetEntry($entryName)
            if(!$entry){continue}
            $reader=[IO.StreamReader]::new($entry.Open())
            try {$metadata=$reader.ReadToEnd()} finally {$reader.Dispose()}
            $modBlock=@([regex]::Matches($metadata,'(?ms)^\s*\[\[mods\]\]\s*(.*?)(?=^\s*\[|\z)') |
                Where-Object { $_.Groups[1].Value -match 'modId\s*=\s*"laowu"' })
            if(!$modBlock.Count){continue}
            $versionMatch=[regex]::Match($modBlock[0].Groups[1].Value,'(?m)^\s*version\s*=\s*"([^"]+)"')
            if(!$versionMatch.Success){throw "Missing laowu version in $path"}
            $jarVersion=$versionMatch.Groups[1].Value
            if(@($archive.Entries | Where-Object FullName -Match '(^|/)tests/|Probe.class$|kubejs-sdk|cat-machines-preview|\.zip$').Count) {
                throw "Test/SDK payload in runtime jar: $path"
            }
            return [pscustomobject]@{Path=[IO.Path]::GetFullPath($path); ModId='laowu'; Version=$jarVersion; Sha256=(Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash}
        }
        return $null
    } finally {$archive.Dispose()}
}
function Assert-ClientsStopped {
    foreach($process in (Get-CimInstance Win32_Process -Filter "Name = 'java.exe' OR Name = 'javaw.exe'")) {
        $match=[regex]::Match($process.CommandLine,'--gameDir\s+(?:"([^"]+)"|(\S+))')
        $window=(Get-Process -Id $process.ProcessId -ErrorAction SilentlyContinue).MainWindowTitle
        if($match.Success) {
            $directory=$match.Groups[1].Value+$match.Groups[2].Value
            if($directory -eq '.') {throw "Unresolved active game directory, PID $($process.ProcessId); wait for test client exit."}
            foreach($target in $targets) {
                if([IO.Path]::GetFullPath($directory).TrimEnd('\','/') -eq [IO.Path]::GetFullPath($target.Instance).TrimEnd('\','/')) {
                    throw "Target client still running, PID $($process.ProcessId)"
                }
            }
        } elseif($window -match 'Minecraft') {
            throw "Minecraft with unknown game directory, PID $($process.ProcessId)"
        }
    }
}
Assert-ClientsStopped
$prepared=@()
foreach($target in $targets) {
    $source=Join-Path $projectRoot ($target.Port+'/'+$BuildDirectory+'/libs/create-meowchanics-'+$Version+'-'+$target.Port+'.jar')
    $sourceInfo=Read-LaowuJar $source
    if(!$sourceInfo){throw "Missing modId=laowu in source $source"}
    if($sourceInfo.Version -ne $Version){throw "Expected source version $Version, found $($sourceInfo.Version)"}
    $mods=[IO.Path]::GetFullPath((Join-Path $target.Instance 'mods'))
    $old=@(Get-ChildItem -LiteralPath $mods -Filter '*.jar' -File | ForEach-Object {Read-LaowuJar $_.FullName} | Where-Object { $null -ne $_ })
    if($old.Count -gt 1 -or ($old.Count -eq 0 -and !$AllowFreshInstall)) {
        throw "Expected exactly one active laowu jar in $mods, found $($old.Count)"
    }
    $previous=if($old.Count -eq 1){$old[0]}else{$null}
    $prepared+=@{Target=$target; Source=$sourceInfo; Mods=$mods; Old=$previous}
}
$stamp=Get-Date -Format 'yyyyMMdd-HHmmss'
$stamp+='-'+$Version+'-'+$Label
$results=@()
foreach($item in $prepared) {
    Assert-ClientsStopped
    $backupRoot=[IO.Path]::GetFullPath((Join-Path $item.Target.Instance 'mod-backups/create-meowchanics'))
    $backupDirectory=[IO.Path]::GetFullPath((Join-Path $backupRoot $stamp))
    if(!$backupDirectory.StartsWith($backupRoot+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) {
        throw 'Backup path escaped intended directory'
    }
    $destination=Join-Path $item.Mods ([IO.Path]::GetFileName($item.Source.Path))
    if((Test-Path -LiteralPath $destination) -and (!$item.Old -or $destination -ne $item.Old.Path)) {
        throw "Unexpected destination file; refusing to overwrite $destination"
    }
    $backup=$null
    if($item.Old) {
        if([IO.Path]::GetDirectoryName($item.Old.Path) -ne $item.Mods){throw 'Old jar escaped mods directory'}
        New-Item -ItemType Directory -Path $backupDirectory -Force | Out-Null
        $backup=Join-Path $backupDirectory ([IO.Path]::GetFileName($item.Old.Path))
        Move-Item -LiteralPath $item.Old.Path -Destination $backup
    }
    try {
        Copy-Item -LiteralPath $item.Source.Path -Destination $destination
        $installed=@(Get-ChildItem -LiteralPath $item.Mods -Filter '*.jar' -File | ForEach-Object {Read-LaowuJar $_.FullName} | Where-Object { $null -ne $_ })
        if($installed.Count -ne 1 -or $installed[0].Version -ne $Version -or $installed[0].Sha256 -ne $item.Source.Sha256){throw 'Deployment version/hash/count mismatch'}
    } catch {
        # Recover only the explicitly named jar written above. Never touch other mods.
        if(Test-Path -LiteralPath $destination){Remove-Item -LiteralPath $destination}
        if($item.Old){Copy-Item -LiteralPath $backup -Destination $item.Old.Path}
        throw
    }
    $results+=[pscustomobject]@{Port=$item.Target.Port; Source=$item.Source.Path; Installed=$installed[0]; Backup=$backup; Previous=$item.Old; ActiveLaowuCount=1; Verified=$true}
}
[pscustomobject]@{Time=(Get-Date -Format 'yyyy-MM-dd HH:mm:ss');Version=$Version;Stamp=$stamp;Results=$results} | ConvertTo-Json -Depth 7
