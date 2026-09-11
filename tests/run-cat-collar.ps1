$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$gameLibraries = 'D:/PCL2/PCL/.minecraft/libraries'
$commonLibraries = @(
    'org/joml/joml/1.10.5/joml-1.10.5.jar',
    'com/google/code/gson/gson/2.10/gson-2.10.jar',
    'com/mojang/logging/1.1.1/logging-1.1.1.jar',
    'org/slf4j/slf4j-api/2.0.1/slf4j-api-2.0.1.jar',
    'org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar',
    'com/google/guava/guava/32.1.2-jre/guava-32.1.2-jre.jar',
    'com/google/guava/failureaccess/1.0.1/failureaccess-1.0.1.jar',
    'it/unimi/dsi/fastutil/8.5.9/fastutil-8.5.9.jar',
    'io/netty/netty-common/4.1.97.Final/netty-common-4.1.97.Final.jar',
    'io/netty/netty-buffer/4.1.97.Final/netty-buffer-4.1.97.Final.jar',
    'io/netty/netty-codec/4.1.97.Final/netty-codec-4.1.97.Final.jar'
) | ForEach-Object { Join-Path $gameLibraries $_ }
foreach ($loader in @('forge-1.20.1', 'neoforge-1.21.1')) {
    $isForge = $loader.StartsWith('forge-')
    $javaDir = if ($isForge) { 'C:/Program Files/Zulu/zulu-17/bin' } else { 'C:/Program Files/Zulu/zulu-21/bin' }
    $minecraftJar = if ($isForge) {
        'C:/Users/16611/.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.20.1-47.4.22_mapped_official_1.20.1/forge-1.20.1-47.4.22_mapped_official_1.20.1.jar'
    } else {
        # Use NeoForm's named vanilla models so the headless test does not boot FML.
        'C:/Users/16611/.gradle/caches/neoformruntime/intermediate_results/rename_8a3b8d60aaea5101c3bf8584c76f2325c2525f20_output.jar'
    }
    $dataFixerVersion = if ($isForge) { '6.0.8' } else { '8.0.16' }
    $brigadierVersion = if ($isForge) { '1.1.8' } else { '1.3.10' }
    $authlibVersion = if ($isForge) { '4.0.43' } else { '6.0.54' }
    $distMarkerJar = if ($isForge) {
        "$gameLibraries/net/minecraftforge/mergetool/1.1.5/mergetool-1.1.5-api.jar"
    } else { "$gameLibraries/net/neoforged/mergetool/2.0.3/mergetool-2.0.3-api.jar" }
    $testOutput = Join-Path $projectRoot "tests/build/cat-collar/$loader"
    New-Item -ItemType Directory -Path $testOutput -Force | Out-Null
    $testClasspath = (@(
        $testOutput, "$projectRoot/$loader/build/classes/java/main",
        "$projectRoot/$loader/src/main/resources", $minecraftJar, $distMarkerJar,
        "$gameLibraries/com/mojang/datafixerupper/$dataFixerVersion/datafixerupper-$dataFixerVersion.jar",
        "$gameLibraries/com/mojang/authlib/$authlibVersion/authlib-$authlibVersion.jar",
        "$gameLibraries/com/mojang/brigadier/$brigadierVersion/brigadier-$brigadierVersion.jar"
    ) + $commonLibraries) -join ';'
    & "$javaDir/javac.exe" -encoding UTF-8 -cp $testClasspath -d $testOutput "$projectRoot/tests/CatCollarPoseRegression.java"
    if ($LASTEXITCODE -ne 0) { throw "$loader collar regression compilation failed" }
    Write-Output $loader
    & "$javaDir/java.exe" -cp $testClasspath cn.laowu.mod.client.CatCollarPoseRegression
    if ($LASTEXITCODE -ne 0) { throw "$loader collar regression failed" }
}
