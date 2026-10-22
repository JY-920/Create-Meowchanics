param([ValidateSet('forge','neo','both')][string]$Loader='both')
$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$minecraftRoot='D:/PCL2/PCL/.minecraft'
foreach($kind in $(if($Loader -eq 'both'){@('forge','neo')}else{@($Loader)})) {
    $forge=$kind -eq 'forge'
    $module=if($forge){'forge-1.20.1'}else{'neoforge-1.21.1'}
    $instance=if($forge){'1.20.1-Forge模组开发'}else{'1.21.1-NeoForge模组开发'}
    $javaBin=if($forge){'C:/Program Files/Zulu/zulu-17/bin'}else{'C:/Program Files/Zulu/zulu-21/bin'}
    # Unpatched named Minecraft avoids requiring a live FML registry bootstrap.
    $minecraft=if($forge){'C:/Users/16611/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/1.20.1-loom.mappings.1_20_1.layered+hash.40359-v2/minecraft-merged-1.20.1-loom.mappings.1_20_1.layered+hash.40359-v2.jar'}else{'C:/Users/16611/.gradle/caches/neoformruntime/intermediate_results/rename_8a3b8d60aaea5101c3bf8584c76f2325c2525f20_output.jar'}
    $version=Get-Content -LiteralPath "$minecraftRoot/versions/$instance/$instance.json" -Raw | ConvertFrom-Json
    $libraries=@(foreach($library in $version.libraries){
        $path=Join-Path "$minecraftRoot/libraries" $library.downloads.artifact.path
        if(Test-Path -LiteralPath $path){$path}
    })
    $jech=Get-ChildItem -LiteralPath "$minecraftRoot/versions/$instance/mods" -Filter '*jecharacters*.jar'
    if($jech.Count -ne 1){throw "Expected exactly one installed JECharacters jar for $kind"}
    $output=Join-Path $PSScriptRoot "build/creative-search-compatibility/$kind"
    New-Item -ItemType Directory -Path $output -Force | Out-Null
    $classpath=(@($output,$minecraft,"$projectRoot/$module/build/classes/java/main",$jech.FullName,
        "$PSScriptRoot/build/creative-search-deps/PinIn-1.6.0.jar",
        "$projectRoot/neoforge-1.21.1/build/moddev/artifacts/neoforge-21.1.219-client-extra-aka-minecraft-resources.jar")+$libraries)-join ';'
    $sources=@("$projectRoot/$module/src/main/java/cn/laowu/mod/client/CatCreativeSearchResults.java",
        "$PSScriptRoot/CreativeSearchCompatibilityRegression.java")
    $mixin="$projectRoot/$module/src/main/java/cn/laowu/mod/mixin/JECharactersSearchMixin.java"
    if(Test-Path -LiteralPath $mixin){$sources+=$mixin}
    & "$javaBin/javac.exe" -proc:none -encoding UTF-8 -cp $classpath -d $output @sources
    if($LASTEXITCODE -ne 0){throw "$kind search test compilation failed"}
    & "$javaBin/java.exe" "-Dlog4j.configurationFile=$PSScriptRoot/config-log4j2.xml" -cp $classpath CreativeSearchCompatibilityRegression $kind
    if($LASTEXITCODE -ne 0){throw "$kind search regression failed"}
}
