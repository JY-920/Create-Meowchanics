param(
    [Parameter(Mandatory=$true)][string]$JarPath,
    [ValidatePattern('^[a-z0-9-]+$')][string]$RunId='green'
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
$mods=New-Item -ItemType Directory -Path (Join-Path $run 'mods')
Get-ChildItem -LiteralPath (Join-Path $instance 'mods') -File -Filter '*.jar' |
    Where-Object Name -NotMatch 'meowchanics' | Copy-Item -Destination $mods.FullName
Copy-Item -LiteralPath $product -Destination $mods.FullName
Copy-Item -LiteralPath $probe -Destination $mods.FullName
# Do not copy saves, accounts, configs, options, or user KubeJS scripts.
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
$launchArgs=@('-Xmx4G')+@(Arguments $version.arguments.jvm)+@($version.mainClass)+@(Arguments $version.arguments.game)+@('--width','640','--height','480')
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
if($exitCode -ne 0 -or $output -notmatch 'PASS: FORGE PRODUCTION JAR BOOT') {
    $output -split '\r?\n' | Select-String -Pattern 'DivingCapeLayerMixin|InvalidInjectionException|Caused by:|ERROR|FATAL|Exception' | Select-Object -Last 14 | ForEach-Object {$_.Line}
    throw ('Production boot failed, exit '+$exitCode+'; full logs in '+$run+'/logs/latest.log')
}
Write-Output 'PASS: FORGE PRODUCTION JAR BOOT - actual SRG client, full installed mod set, title screen, clean shutdown'
