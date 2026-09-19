param([switch]$MissingResource)
$ErrorActionPreference='Stop'
$crankRoot=Split-Path $PSScriptRoot -Parent
$crankLibraries='D:/PCL2/PCL/.minecraft/libraries'
$crankClasspath=@(
    "$crankLibraries/org/joml/joml/1.10.5/joml-1.10.5.jar",
    "$crankLibraries/com/google/code/gson/gson/2.10/gson-2.10.jar",
    "$crankLibraries/com/mojang/logging/1.1.1/logging-1.1.1.jar",
    "$crankLibraries/org/slf4j/slf4j-api/2.0.1/slf4j-api-2.0.1.jar"
) -join ';'
foreach($crankPort in @('forge-1.20.1','neoforge-1.21.1')){
    $crankJava=if($crankPort.StartsWith('forge')){'C:/Program Files/Zulu/zulu-17/bin'}else{'C:/Program Files/Zulu/zulu-21/bin'}
    $crankOutput=Join-Path $crankRoot "tests/build/crank/$crankPort"
    New-Item -ItemType Directory -Path $crankOutput -Force | Out-Null
    & "$crankJava/javac.exe" -encoding UTF-8 -cp $crankClasspath -d $crankOutput "$crankRoot/$crankPort/src/main/java/cn/laowu/mod/client/CatCrankPose.java" "$crankRoot/tests/CatCrankRegression.java"
    if($LASTEXITCODE -ne 0){throw "$crankPort crank test compilation failed"}
    $crankRuntime="$crankOutput;$crankClasspath"
    $crankArgs=@()
    if($MissingResource){$crankArgs+= '--missing-resource'}else{$crankRuntime+=";$crankRoot/$crankPort/src/main/resources"}
    & "$crankJava/java.exe" -cp $crankRuntime CatCrankRegression @crankArgs
    if($LASTEXITCODE -ne 0){throw "$crankPort crank regression failed"}
}
