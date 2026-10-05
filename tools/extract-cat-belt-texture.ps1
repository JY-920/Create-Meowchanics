param(
    [string]$Source = (Join-Path $PSScriptRoot '../art/cat-machines/cat-belt-source.png'),
    [string]$Output = (Join-Path $PSScriptRoot '../art/cat-machines/cat-belt-casing.png')
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$inputBitmap = [Drawing.Bitmap]::new([IO.Path]::GetFullPath($Source))
$outputBitmap = [Drawing.Bitmap]::new(64,64,[Drawing.Imaging.PixelFormat]::Format32bppArgb)
try {
    if ($inputBitmap.Width -ne 648 -or $inputBitmap.Height -ne 573) {
        throw 'Expected the supplied 648x573 screenshot; refusing to sample unknown bounds.'
    }
    # Authored 64x64 atlas occupies x=63..593, y=23..553 in the supplied screenshot.
    # Sample cell centres, not averaged edges, to retain the original pixel-art palette.
    for ($y=0; $y -lt 64; $y++) {
        for ($x=0; $x -lt 64; $x++) {
            $sx=[int][Math]::Floor(63+($x+0.5)*531/64)
            $sy=[int][Math]::Floor(23+($y+0.5)*531/64)
            $counts=@{}
            for ($dy=-1; $dy -le 1; $dy++) {
                for ($dx=-1; $dx -le 1; $dx++) {
                    $argb=$inputBitmap.GetPixel($sx+$dx,$sy+$dy).ToArgb()
                    if (!$counts.ContainsKey($argb)) {$counts[$argb]=0}
                    $counts[$argb]++
                }
            }
            $argb=($counts.GetEnumerator() | Sort-Object Value -Descending | Select-Object -First 1).Key
            $colour=[Drawing.Color]::FromArgb([int]$argb)
            # Dark neutral blue/grey checkerboard is the editor's transparency background,
            # not the warm brown opaque pixels in the authored atlas.
            if ($colour.R -lt 40 -and $colour.G -lt 45 -and $colour.B -lt 50 -and $colour.B -ge $colour.R) {
                $colour=[Drawing.Color]::Transparent
            }
            $outputBitmap.SetPixel($x,$y,$colour)
        }
    }
    $outputBitmap.Save([IO.Path]::GetFullPath($Output),[Drawing.Imaging.ImageFormat]::Png)
    Write-Output "Extracted 64x64 cat belt atlas: $Output"
} finally {
    $inputBitmap.Dispose()
    $outputBitmap.Dispose()
}
