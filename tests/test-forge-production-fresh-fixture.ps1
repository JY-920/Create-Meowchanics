param(
    [Parameter(Mandatory=$true)][string]$JarPath,
    [ValidatePattern('^[a-z0-9-]+$')][string]$RunId=('fresh-fixture-'+[DateTime]::UtcNow.ToString('yyyyMMddHHmmss'))
)
$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$run=Join-Path $projectRoot ('forge-1.20.1/build/production-boot-'+$RunId)
& (Join-Path $PSScriptRoot 'run-forge-production-boot.ps1') -JarPath $JarPath -RunId $RunId -FreshAnimations -PrepareOnly -WithoutCreate -WithoutJECharacters
function Check([bool]$condition,[string]$message){if(!$condition){throw $message}}
function SameHash([string]$source,[string]$copy){
    Check ((Get-FileHash -LiteralPath $source).Hash -eq (Get-FileHash -LiteralPath $copy).Hash) ('Fixture copy changed bytes: '+$copy)
}
$pack='D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/resourcepacks/FreshAnimations_v1.10.4.zip'
SameHash $pack (Join-Path $run 'resourcepacks/FreshAnimations_v1.10.4.zip')
SameHash (Join-Path $projectRoot 'forge-1.20.1/build/accessory-gametest/no-kubejs-standalone-phase2/world/level.dat') (Join-Path $run 'saves/Standalone/level.dat')
SameHash ([IO.Path]::GetFullPath($JarPath)) (Join-Path $run ('mods/'+[IO.Path]::GetFileName($JarPath)))
SameHash (Join-Path $PSScriptRoot 'production/fresh-options.txt') (Join-Path $run 'options.txt')
$mods=@(Get-ChildItem -LiteralPath (Join-Path $run 'mods') -Filter '*.jar')
Check (@($mods | Where-Object Name -like '*jecharacters*').Count -eq 0) 'JECharacters exclusion was ignored'
Check (@($mods | Where-Object Name -like 'entity_model_features*').Count -eq 1) 'EMF fixture missing or duplicated'
Check (@($mods | Where-Object Name -like 'entity_texture_features*').Count -eq 1) 'ETF fixture missing or duplicated'
Check (@($mods | Where-Object Name -eq 'laowu-fresh-animation-probe-forge.jar').Count -eq 1) 'Separate Fresh probe missing'
Check (@($mods | Where-Object Name -eq 'laowu-release-boot-probe-forge.jar').Count -eq 1) 'Separate boot/AT probe missing'
Check (!(Test-Path -LiteralPath (Join-Path $run 'saves/Standalone/session.lock'))) 'Active source world lock must not be copied'
Check (!(Test-Path -LiteralPath (Join-Path $run 'logs'))) 'Prepare-only mode unexpectedly launched Minecraft'
Write-Output 'PASS: FRESH PRODUCTION FIXTURE - byte-identical inputs, isolated world/options, optional exclusions, no client launch'
