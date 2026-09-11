param(
    [string]$VanillaResources = "$PSScriptRoot/../neoforge-1.21.1/build/moddev/artifacts/neoforge-21.1.219-client-extra-aka-minecraft-resources.jar",
    [string]$ModResources = "$PSScriptRoot/../neoforge-1.21.1/src/main/resources",
    [string]$ModJar
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$vanilla = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $VanillaResources))
$packaged = if ($ModJar) { [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $ModJar)) } else { $null }
$cache = @{}

function Read-JsonEntry($archive, [string]$path) {
    $entry = $archive.GetEntry($path)
    if (!$entry) { return $null }
    $reader = [System.IO.StreamReader]::new($entry.Open())
    try { return ($reader.ReadToEnd() | ConvertFrom-Json) } finally { $reader.Dispose() }
}

function Test-Spec([string]$item, $spec, [string[]]$visited = @()) {
    foreach ($value in @($spec)) {
        $id = if ($value -is [string]) { $value } else { $value.id }
        if (!$id) { continue }
        if ($id.StartsWith('#')) {
            if (Test-Tag $item $id.Substring(1) $visited) { return $true }
        } elseif ($id -eq $item) { return $true }
    }
    return $false
}

function Test-Tag([string]$item, [string]$tag, [string[]]$visited) {
    $key = "$item|$tag"
    if ($cache.ContainsKey($key)) { return $cache[$key] }
    if ($visited -contains $tag) { throw "Tag reference cycle: $tag" }
    $namespace, $path = $tag.Split(':', 2)
    $resource = "data/$namespace/tags/item/$path.json"
    $base = Read-JsonEntry $vanilla $resource
    $extra = if ($packaged) { Read-JsonEntry $packaged $resource } else {
        $file = Join-Path $ModResources $resource
        if (Test-Path -LiteralPath $file) { Get-Content -LiteralPath $file -Raw | ConvertFrom-Json }
    }
    $values = @()
    if ($base) { $values += $base.values }
    if ($extra) {
        if ($extra.replace) { throw "Mod must not replace vanilla equipment tags: $resource" }
        $values += $extra.values
    }
    $result = Test-Spec $item $values (@($visited) + $tag)
    $cache[$key] = $result
    return $result
}

try {
    $pairs = [ordered]@{
        cat_helmet = 'diamond_helmet'; cat_chestplate = 'diamond_chestplate'
        cat_leggings = 'diamond_leggings'; cat_boots = 'diamond_boots'
        cat_sword = 'diamond_sword'; cat_pickaxe = 'diamond_pickaxe'
        cat_axe = 'diamond_axe'; cat_shovel = 'diamond_shovel'; cat_hoe = 'diamond_hoe'
    }
    $checks = 0
    foreach ($entry in $vanilla.Entries) {
        if ($entry.FullName -notmatch '^data/minecraft/enchantment/[^/]+\.json$') { continue }
        $enchantment = Read-JsonEntry $vanilla $entry.FullName
        foreach ($pair in $pairs.GetEnumerator()) {
            foreach ($field in @('supported_items', 'primary_items')) {
                $spec = $enchantment.$field
                if (!$spec) { $spec = $enchantment.supported_items }
                $actual = Test-Spec ("laowu:" + $pair.Key) $spec
                $expected = Test-Spec ("minecraft:" + $pair.Value) $spec
                if ($actual -ne $expected) {
                    throw "$($pair.Key) differs from $($pair.Value): $($entry.FullName) $field"
                }
                $checks++
            }
        }
    }
    if ($checks -lt 500) { throw "Insufficient enchantment coverage: $checks" }
    Write-Output "PASS: $checks supported/primary checks; nine cat equipment items match diamond equivalents."
} finally {
    $vanilla.Dispose()
    if ($packaged) { $packaged.Dispose() }
}
