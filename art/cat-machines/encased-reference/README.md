# Cat casing socket and split-panel art

**Superseded by user art (2026-09-26):** The active textures now come from
`../encased-user-20260926/`, copied byte-for-byte by `export-cat-machines.mjs`.
The generated drafts below remain rejected archives; matching runtime filenames
do not mean these drafts have been restored. The rest of this document is historical.

**DEFERRED (2026-09-26):** The user rejected these generated textures and
requested shipping only processor inventory shafts, socket UV alignment and
native belt casing behavior. Generated runtime PNGs and their packer/test are
archived in `deferred-20260926/`, outside runtime resources. Do not restore or
deploy them without a new request. The `.disabled` scripts retain their former
project-root-relative paths as historical references, not runnable entry points.

The artist's original casing and connected-casing PNGs remain unchanged.

- `gearbox.png` and `andesite_encased_cogwheel_side_connected.png` are reference assets extracted from the installed Create 1.20.1 6.0.8-291 JAR (`assets/create/textures/block/`). They are not copied into the runtime JAR. The latter supplies the exact alpha stencil and tile order for compatibility with Create's CT implementation.
- `cat-port-generated-source.png` and `cat-cog-generated-source.png` were produced with the built-in image generation tool on 2026-09-26, using the two native images as geometry references and the supplied `猫机壳 (1).png` as palette reference. No external artist source was overwritten.
- `node tools/pack-cat-encased-textures.mjs --write` performs nearest-center native-resolution conversion and connected-atlas packing. The generated preview's margins/slot layout are not treated as authoritative: the original Create alpha stencil is retained exactly. The source art is only used for the opaque color panels.
- Runtime assets: `cat_casing_shaft_opening.png` (16×16), `cat_encased_cogwheel_side.png` (16×16), `cat_encased_cogwheel_side_connected.png` (32×32), under each loader's `assets/laowu/textures/block/`.

## Generation briefs

Socket: recolor the 16×16 Create gearbox texture to the supplied honey-gold outer trim and pale cream wooden infill, preserving a centered dark shaft socket. Square, edge-to-edge, opaque, crisp pixel art; no shaft, gears, text, perspective, extra margins or lighting effects.

Split panels: recolor the native 32×32 four-tile connected cog casing sheet to honey-gold / cream wood, preserving horizontal transparent slots and left/right/both/neither connected border combinations. Crisp logical square pixels, no gears, shafts, text or scene background. Generated colors are packed to the native grid and native transparent mask by the build-resolution conversion above.
