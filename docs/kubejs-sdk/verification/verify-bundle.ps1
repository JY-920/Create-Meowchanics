# Read-only integrity check. Compatible with Windows PowerShell 5.1 and PowerShell 7.
$ErrorActionPreference='Stop'
$bundleRoot=[IO.Path]::GetFullPath((Split-Path $PSScriptRoot -Parent))
$manifest=Get-Content -Raw -Encoding UTF8 -LiteralPath (Join-Path $bundleRoot 'manifest.json') | ConvertFrom-Json
$prefix=$bundleRoot.TrimEnd([char[]]@('/','\'))+[IO.Path]::DirectorySeparatorChar
$checked=0
foreach($entry in $manifest.files){
    $path=[IO.Path]::GetFullPath((Join-Path $bundleRoot $entry.path))
    if(!$path.StartsWith($prefix,[StringComparison]::OrdinalIgnoreCase)){throw ('Out-of-bundle path: '+$entry.path)}
    if(!(Test-Path -LiteralPath $path -PathType Leaf)){throw ('Missing: '+$entry.path)}
    if((Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash -ne $entry.sha256){throw ('Changed: '+$entry.path)}
    $checked++
}
$actual=@(Get-ChildItem -LiteralPath $bundleRoot -Recurse -File | Where-Object {$_.FullName -ne (Join-Path $bundleRoot 'manifest.json')})
if($actual.Count -ne $checked){throw 'Bundle has added/unlisted files; use a clean extraction or audit your changes'}
if($manifest.catalog_count -ne 36){throw 'Unexpected catalog count'}
Write-Output ('PASS: '+$checked+' files match the manifest; 36-item SDK. No game files were modified.')
