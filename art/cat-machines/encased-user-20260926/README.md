# User-authored cat casing textures — 2026-09-26

These PNGs are exact copies supplied by the user in Downloads. Do not resize,
repaint, regenerate, or substitute the rejected draft art in encased-reference.
The exporter copies PNG bytes unchanged to both loaders.

| Source | Runtime texture | Size | SHA-256 |
| --- | --- | --- | --- |
| gearbox.png | cat_casing_shaft_opening.png | 16×16 | 469480b88f4be67650c1ac561d73ec9f18729b2c8c47a7db43ea61d2906d1170 |
| andesite_encased_cogwheel_side.png | cat_encased_cogwheel_side.png | 16×16 | b836c3505c9887c592998af7db50168678fe93472e758bcbfbd03885d4b07055 |
| andesite_encased_cogwheel_side_connected.png | cat_encased_cogwheel_side_connected.png | 32×32 | 87adfd92195f6928c419d24ac91c0903a6057fddf72a1f580ed9aca86aa36890 |

The side PNG preserves Create's 12×4 transparent cog slot. The connected sheet
preserves its four native 16×16 tiles: both posts, right post only, left post
only, no posts. Small cogs use native vertical/horizontal CT behavior with these
new sprites; large cogs retain the native model UVs into the connected sheet.
The large-cog inventory model's key 4 remains Create's actual gear texture.
Press/mixer authored openings and their corrected UVs are separate and untouched.

Regenerate using tools/export-cat-machines.mjs --write, followed by
tools/export-cat-sixway.mjs --write (the latter owns directional basin resources).
