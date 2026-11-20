param([switch]$Check)
$ErrorActionPreference='Stop'
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$source=Join-Path $repo 'assets-source/cat-painting/named'
foreach($port in @('forge-1.20.1','neoforge-1.21.1')){
 $jdk=if($port -like 'forge-*'){'C:/Program Files/Zulu/zulu-17/bin'}else{'C:/Program Files/Zulu/zulu-21/bin'}
 $classes=Join-Path $repo ('build/named-paintings/'+$port)
 New-Item -ItemType Directory -Force -Path $classes | Out-Null
 $domain=Join-Path $repo ($port+'/src/main/java/cn/laowu/mod/painting')
 $java=@('PaintingSpec','PaintingImages','PaintingAssetStore','PaintingBuiltinCatalog') | ForEach-Object {Join-Path $domain ($_+'.java')}
 & "$jdk/javac.exe" -encoding UTF-8 -d $classes @java (Join-Path $PSScriptRoot 'ExportNamedPaintings.java')
 if($LASTEXITCODE -ne 0){throw 'Named painting exporter failed to compile'}
 $dest=Join-Path $repo ($port+'/src/main/resources/assets/laowu/textures/painting/builtin')
 $mode=if($Check){'--check'}else{'--write'}
 & "$jdk/java.exe" '-Djava.awt.headless=true' -Xmx256m -cp $classes ExportNamedPaintings $source $dest $mode
 if($LASTEXITCODE -ne 0){throw "$port named photo verification failed"}
}
foreach($id in @('naiyou','xiaomi')){
 $forge=Get-FileHash -LiteralPath (Join-Path $repo ('forge-1.20.1/src/main/resources/assets/laowu/textures/painting/builtin/'+$id+'.png'))
 $neo=Get-FileHash -LiteralPath (Join-Path $repo ('neoforge-1.21.1/src/main/resources/assets/laowu/textures/painting/builtin/'+$id+'.png'))
 if($forge.Hash -ne $neo.Hash){throw "Named photo differs between ports: $id"}
}
