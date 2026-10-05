# Cat deployment platform source assets

Imported byte-for-byte from `D:/Project_minecraft/待实现/猫战斗/哈基部署平台` and `哈基弹射部署平台`. Original PNG artwork must not be repainted. `model.bbmodel` is authoritative; the ejector's supplied `model.geo.json` is an old six-cube normal-platform export and does not contain the plate or rod.

Run `node tools/export-cat-deployment-platforms.mjs --write` to regenerate both loaders. Run without `--write` to verify byte-exact resource parity, and `node tests/cat-deployment-platform-assets.mjs` to check the signed inset geometry, quarter-turn face mapping and inventory/animated-part resources. `--import --write` refreshes original sources from the artist directories.

Both blockstates use horizontal `facing`; the unrotated baked model faces north. The base is 16 × 13 × 16 pixels. Five inset cubes have deliberately negative extents; the two side inserts rotate +90° around Y. The exporter preserves inward-facing surfaces and bakes quarter turns instead of emitting unsupported Java element rotations.

The ejector's base is static, while its inventory model includes all eight original cubes. Runtime partial models are `models/block/cat_ejecting_deployment_platform_plate.json` (12 × 2 × 12, from [2,12,2] to [14,14,14]) and `_rod.json` (3 × 2 × 3, from [6.5,10,6.5] to [9.5,12,9.5]). Register these partial models if rendering through Create's baked-model renderer.

Original source bbmodels remain here, including the authored animation keyframes. Runtime uses baked partials and `textures/block/cat_ejecting_deployment_platform.png`; no redundant Blockbench entity model is shipped.

Authored one-second looping clip, peak at 0.5 s:

- `bone2`, pivot [0,13,0]: local position Y 0 → +16 pixels; no rotation.
- `bone3`, pivot [0,11,0], child of `bone2`: local position Y 0 → -7 pixels and Y scale 1 → 8; X/Z scales remain 1.
- In Java block space the rod pivot is [8,11,8]. Apply parent lift before local translation/scale. At full extension the plate reaches Y28..30 and the rod reaches Y12..28. Preserve nested transforms rather than scaling the rod about the block origin.

For baked partials, let `cycle` run from 0 to 1. The plate translates upward by `cycle` blocks. The rod translates upward by `cycle - 7 * cycle / 16` blocks, then scales Y by `1 + 7 * cycle` around pivot [.5,11/16,.5]. Rotate about the block center to horizontal `facing`. Do not render the inventory model on top of the baked base or draw the moving parts twice.
