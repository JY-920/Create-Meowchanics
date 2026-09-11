param([string[]]$Jars)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$javaBin = 'C:/Program Files/Zulu/zulu-17/bin'
$libraries = 'D:/PCL2/PCL/.minecraft/libraries'
$output = Join-Path $PSScriptRoot 'build/mixin-package'
New-Item -ItemType Directory -Path $output -Force | Out-Null
$classpath = @(
    $output,
    "$libraries/org/ow2/asm/asm/9.7.1/asm-9.7.1.jar",
    "$libraries/com/google/code/gson/gson/2.10/gson-2.10.jar"
) -join ';'
if (!$Jars) {
    $Jars = @(
        "$projectRoot/forge-1.20.1/build/libs/create-meowchanics-1.0.2-forge-1.20.1.jar",
        "$projectRoot/neoforge-1.21.1/build/libs/create-meowchanics-1.0.2-neoforge-1.21.1.jar"
    )
}
& "$javaBin/javac.exe" -encoding UTF-8 -cp $classpath -d $output "$PSScriptRoot/MixinPackageRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Mixin package regression compilation failed' }
& "$javaBin/java.exe" -cp $classpath MixinPackageRegression @Jars
if ($LASTEXITCODE -ne 0) { throw 'Mixin package regression failed' }
