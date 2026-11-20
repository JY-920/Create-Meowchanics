param([string[]]$Suite=@('Images'))
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
foreach($port in @('forge-1.20.1','neoforge-1.21.1')){
    $jdk=if($port -like 'forge-*'){'C:/Program Files/Zulu/zulu-17/bin'}else{'C:/Program Files/Zulu/zulu-21/bin'}
    $out=Join-Path $PSScriptRoot ('build/painting-core/'+$port)
    New-Item -ItemType Directory -Force -Path $out | Out-Null
    $sources=@(Get-ChildItem -LiteralPath (Join-Path $root ($port+'/src/main/java/cn/laowu/mod/painting')) -Filter '*.java' | Where-Object Name -in @('PaintingSpec.java','PaintingImages.java','PaintingAssetStore.java','PaintingChunkAssembler.java','PaintingWire.java','PaintingBuiltinCatalog.java') | ForEach-Object FullName)
    $importSource=Join-Path $root ($port+'/src/main/java/cn/laowu/mod/client/painting/PaintingImportFiles.java')
    if(Test-Path -LiteralPath $importSource){$sources+=$importSource}
    $trackerSource=Join-Path $root ($port+'/src/main/java/cn/laowu/mod/client/painting/PaintingUploadTracker.java')
    if(Test-Path -LiteralPath $trackerSource){$sources+=$trackerSource}
    foreach($extra in @('PaintingDiskCache','PaintingPreviewQueue','PaintingFrameUv')){
        $extraSource=Join-Path $root ($port+'/src/main/java/cn/laowu/mod/client/painting/'+$extra+'.java')
        if(Test-Path -LiteralPath $extraSource){$sources+=$extraSource}
    }
    foreach($name in $Suite){$sources+=Join-Path $PSScriptRoot ('painting/Painting'+$name+'Regression.java')}
    & "$jdk/javac.exe" -encoding UTF-8 -d $out @sources
    if($LASTEXITCODE -ne 0){throw "$port painting compile failed"}
    foreach($name in $Suite){
        & "$jdk/java.exe" '-Djava.awt.headless=true' -Xmx256m -cp $out ('Painting'+$name+'Regression')
        if($LASTEXITCODE -ne 0){throw "$port $name regression failed"}
    }
}
