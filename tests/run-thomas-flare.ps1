param([switch]$MissingResource)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$gameLibraries = 'D:/PCL2/PCL/.minecraft/libraries'
$dependencyClasspath = @(
    "$gameLibraries/org/joml/joml/1.10.5/joml-1.10.5.jar",
    "$gameLibraries/com/google/code/gson/gson/2.10/gson-2.10.jar",
    "$gameLibraries/com/mojang/logging/1.1.1/logging-1.1.1.jar",
    "$gameLibraries/org/slf4j/slf4j-api/2.0.1/slf4j-api-2.0.1.jar"
) -join ';'
foreach ($loader in @('forge-1.20.1', 'neoforge-1.21.1')) {
    $javaDir = if ($loader.StartsWith('forge-')) { 'C:/Program Files/Zulu/zulu-17/bin' } else { 'C:/Program Files/Zulu/zulu-21/bin' }
    $testOutput = Join-Path $projectRoot "tests/build/thomas-flare/$loader"
    New-Item -ItemType Directory -Path $testOutput -Force | Out-Null
    & "$javaDir/javac.exe" -encoding UTF-8 -cp $dependencyClasspath -d $testOutput "$projectRoot/tests/dance-stubs/net/minecraft/client/model/geom/ModelPart.java" "$projectRoot/$loader/src/main/java/cn/laowu/mod/client/CatStreetDanceAnimation.java" "$projectRoot/tests/CatStreetDanceRegression.java"
    if ($LASTEXITCODE -ne 0) { throw "$loader regression compilation failed" }
    $testClasspath = "$testOutput;$dependencyClasspath"
    if (!$MissingResource) { $testClasspath += ";$projectRoot/$loader/src/main/resources" }
    $testArgument = if ($MissingResource) { '--missing-resource' } else { "$projectRoot/docs/blockbench/cat_thomas_flare.bbmodel" }
    Write-Output $loader
    & "$javaDir/java.exe" -cp $testClasspath CatStreetDanceRegression $testArgument
    if ($LASTEXITCODE -ne 0) { throw "$loader regression failed" }
}
