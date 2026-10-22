param(
    [Parameter(Mandatory=$true)][string]$JarPath,
    [ValidatePattern('^[a-z0-9-]+$')][string]$RunId='green',
    [switch]$WithoutCreate,
    [switch]$WithoutJECharacters,
    [switch]$FreshAnimations,
    [switch]$PrepareOnly
)
$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$instance=[IO.Path]::GetFullPath((Join-Path $projectRoot '..'))
$minecraftRoot=[IO.Path]::GetFullPath((Join-Path $instance '../..'))
$libraryRoot=Join-Path $minecraftRoot 'libraries'
$versionName='1.20.1-Forge模组开发'
$version=Get-Content -LiteralPath (Join-Path $instance ($versionName+'.json')) -Raw | ConvertFrom-Json
$run=[IO.Path]::GetFullPath((Join-Path $projectRoot ('forge-1.20.1/build/production-boot-'+$RunId)))
if(Test-Path -LiteralPath $run){throw 'Choose a fresh RunId; never overwrite previous fixtures'}
$probe=Join-Path $projectRoot 'forge-1.20.1/build/release-boot-probe/laowu-release-boot-probe-forge.jar'
$product=[IO.Path]::GetFullPath($JarPath)
if(!(Test-Path -LiteralPath $probe)||!(Test-Path -LiteralPath $product)){throw 'Missing product or separate probe jar'}
$freshProbe=Join-Path $projectRoot 'forge-1.20.1/build/release-boot-probe/laowu-fresh-animation-probe-forge.jar'
$freshDependencies=Join-Path $PSScriptRoot 'build/fresh-animation-deps/forge'
$freshPack='D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/resourcepacks/FreshAnimations_v1.10.4.zip'
$freshWorld=Join-Path $projectRoot 'forge-1.20.1/build/accessory-gametest/no-kubejs-standalone-phase2/world'
if($FreshAnimations) {
    foreach($required in @($freshProbe,$freshPack,(Join-Path $freshWorld 'level.dat'))) {
        if(!(Test-Path -LiteralPath $required)){throw ('Missing Fresh fixture input: '+$required)}
    }
    $freshJars=@(Get-ChildItem -LiteralPath $freshDependencies -File -Filter '*.jar')
    if(@($freshJars | Where-Object Name -like 'entity_model_features*.jar').Count -ne 1 -or
       @($freshJars | Where-Object Name -like 'entity_texture_features*.jar').Count -ne 1){throw 'Expected exactly one Forge EMF and ETF fixture jar'}
}
$mods=New-Item -ItemType Directory -Path (Join-Path $run 'mods')
Get-ChildItem -LiteralPath (Join-Path $instance 'mods') -File -Filter '*.jar' |
    Where-Object { $_.Name -notmatch 'meowchanics' -and (!$WithoutCreate -or $_.Name -notmatch 'create-|createdieselgenerators|flywheel-|Ponder-') -and
        (!$WithoutJECharacters -or $_.Name -notmatch 'jecharacters') -and
        (!$FreshAnimations -or $_.Name -notmatch 'entity_model_features|entity_texture_features') } |
    Copy-Item -Destination $mods.FullName
Copy-Item -LiteralPath $product -Destination $mods.FullName
Copy-Item -LiteralPath $probe -Destination $mods.FullName
# Only the pre-existing isolated baseline world is copied in Fresh mode. Never user saves.
if($FreshAnimations) {
    Copy-Item -LiteralPath $freshProbe -Destination $mods.FullName
    $freshJars | Copy-Item -Destination $mods.FullName
    $packs=New-Item -ItemType Directory -Path (Join-Path $run 'resourcepacks')
    Copy-Item -LiteralPath $freshPack -Destination $packs.FullName
    $world=(New-Item -ItemType Directory -Path (Join-Path $run 'saves/Standalone')).FullName
    Get-ChildItem -LiteralPath $freshWorld -Force | Where-Object Name -ne 'session.lock' |
        Copy-Item -Destination $world -Recurse
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'production/fresh-options.txt') -Destination (Join-Path $run 'options.txt')
}
if($PrepareOnly){Write-Output ('PASS: PRODUCTION FIXTURE PREPARED - '+$run);return}
# Do not copy accounts, configs, existing options, or user KubeJS scripts.
$nativeDirectory=(New-Item -ItemType Directory -Path (Join-Path $run 'natives')).FullName
function Allowed($rules) {
    if(!$rules){return $true}
    $allowed=$false
    foreach($rule in $rules) {
        $matches=$true
        if($rule.os.name -and $rule.os.name -ne 'windows'){$matches=$false}
        if($rule.os.arch -and $rule.os.arch -ne 'amd64' -and $rule.os.arch -ne 'x86_64'){$matches=$false}
        if($rule.os.version -and [Environment]::OSVersion.Version.ToString() -notmatch $rule.os.version){$matches=$false}
        if($rule.features){$matches=$false}
        if($matches){$allowed=$rule.action -eq 'allow'}
    }
    return $allowed
}
$libraries=@(foreach($library in $version.libraries) {
    if(!(Allowed $library.rules)){continue}
    $path=Join-Path $libraryRoot $library.downloads.artifact.path
    if(!(Test-Path -LiteralPath $path)){throw ('Missing installed launcher library: '+$library.name)}
    [IO.Path]::GetFullPath($path)
})
$clientJar=Join-Path $instance ($versionName+'.jar')
$classpath=($libraries+@($clientJar))-join ';'
$replacements=@{
    classpath=$classpath;classpath_separator=';';library_directory=$libraryRoot
    natives_directory=$nativeDirectory;launcher_name='Codex isolated production test';launcher_version='1'
    version_name=$versionName;version_type='release';game_directory=$run
    assets_root=(Join-Path $minecraftRoot 'assets');assets_index_name=$version.assetIndex.id
    auth_player_name='ReleaseProbe';auth_uuid='a563bf11e3974ab088886c213faf193e';auth_access_token='0'
    clientid='';auth_xuid='';user_type='legacy'
}
function Expand([string]$value) {
    foreach($key in $replacements.Keys){$value=$value.Replace('${'+$key+'}',[string]$replacements[$key])}
    if($value -match '\$\{'){throw ('Unresolved launcher argument: '+$value)}
    return $value
}
function Arguments($values) {
    foreach($value in $values) {
        if($value -is [string]){Expand $value}
        elseif(Allowed $value.rules){foreach($part in $value.value){Expand $part}}
    }
}
$java='C:/Program Files/Zulu/zulu-21/bin/java.exe'
$probeJvm=if($FreshAnimations){@('-Dlaowu.fresh_probe=true')}else{@()}
$probeGame=if($FreshAnimations){@('--quickPlaySingleplayer','Standalone')}else{@()}
$launchArgs=@('-Xmx4G')+$probeJvm+@(Arguments $version.arguments.jvm)+@($version.mainClass)+@(Arguments $version.arguments.game)+@('--width','640','--height','480')+$probeGame
# ProcessStartInfo.ArgumentList preserves paths and does not expose or reuse account credentials.
$info=[Diagnostics.ProcessStartInfo]::new($java)
$info.WorkingDirectory=$run
$info.UseShellExecute=$false
$info.CreateNoWindow=$true
$info.RedirectStandardOutput=$true
$info.RedirectStandardError=$true
foreach($arg in $launchArgs){$info.ArgumentList.Add([string]$arg)}
$process=[Diagnostics.Process]::Start($info)
$stdout=$process.StandardOutput.ReadToEndAsync()
$stderr=$process.StandardError.ReadToEndAsync()
Write-Output ('Own isolated production client PID '+$process.Id+'; fixture '+$run)
$deadline=[DateTime]::UtcNow.AddMinutes(4)
while(!$process.WaitForExit(1000)) {
    if([DateTime]::UtcNow -gt $deadline){$process.Kill();throw 'Owned isolated production test timed out'}
}
$output=$stdout.GetAwaiter().GetResult()+$stderr.GetAwaiter().GetResult()
$exitCode=$process.ExitCode
$process.Dispose()
$expected=if($FreshAnimations){'PASS: FRESH ANIMATION CLIENT'}else{'PASS: FORGE PRODUCTION JAR BOOT'}
if($exitCode -ne 0 -or $output -notmatch $expected) {
    $output -split '\r?\n' | Select-String -Pattern 'DivingCapeLayerMixin|InvalidInjectionException|Caused by:|ERROR|FATAL|Exception' | Select-Object -Last 14 | ForEach-Object {$_.Line}
    throw ('Production boot failed, exit '+$exitCode+'; full logs in '+$run+'/logs/latest.log')
}
Write-Output ($expected+' - actual SRG client, Create '+$(if($WithoutCreate){'absent'}else{'installed'})+
    ', JECharacters '+$(if($WithoutJECharacters){'absent'}else{'installed'})+', '+
    $(if($FreshAnimations){'Fresh rendering preview'}else{'title screen'})+', clean shutdown')
