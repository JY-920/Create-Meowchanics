# Creature transmitter implementation

Spec: user supplied 生物转信器 model; count nearby filtered mobs and emit redstone, maximally reuse Create threshold-switch GUI, proceed without another approval.

## Scope / decisions
- Register creature_transmitter on Forge 1.20.1 and NeoForge 1.21.1 using supplied cube/texture.
- Count alive loaded Mob entities by feet distance in a sphere, radius 1–16 (default 8), every 10 ticks. No chunk loads, no wall occlusion, no players/items.
- Reuse CreatureFilterRules exactly; absent filter accepts all, installed empty filter accepts none. Held filter installs/replaces; sneak empty hand removes; breaking returns filter.
- Native hysteresis: upper threshold turns on 15, lower turns off; intermediate holds state; inversion. Default upper 1/lower 0; threshold domain 0–4096, upper > lower. Radius/filter/state persisted.
- Native THRESHOLD_SWITCH background/input sprites and ScrollInput/IconButton; replace unused measurement with radius and current count, no custom GUI texture. Server validates build permission, distance, loaded block and values.
- Do not add new age/taming filter types in this first version; preserve shipped filter semantics. No recipe invented without a requested recipe.

## Task 1 — server feature and assets
Write runtime GameTest against registry and reflective feature API; expected RED missing registration. Implement registration, BE, block interactions, packets, resource export. Verify GREEN count/threshold/filter/reload/security on both loaders.

## Task 2 — native GUI
Reuse native sprites/controls and render machine icon, count, radius and filter information. Add actual client probe for opening, editing, saving/reopening and screenshot. Verify dedicated server compatibility and client drawing.

## Task 3 — verification and deployment
Run normal dual builds (API/trait/release checks retained), targeted runtime tests and final independent review. Recheck processes before deployment, exact scoped backups and SHA checks. Do not commit dirty shared worktree.

## Review focus
Boundary threshold hysteresis, initial inversion, entity death and filter replacement, packet spoof/range validation, filter duplication on break, no client-only classes loaded by server, source UV mapping, complete native widgets without overlap.

## Progress
- Planning/discovery complete; existing linked develop worktree reused. Baseline creature-filter assets PASS.
- Ruling: first version uses existing category/entity conditions rather than speculative new attributes; existing GUI is already suitable for target selection.
- Task 1: complete. Initial 3 real GameTests failed with missing registration (outputs-creature-transmitter-red.log), then passed both loaders. Expanded 6 tests per loader passed: radius/death/hysteresis/inversion, walls/empty filter, bounds, NBT/packet authorization, species/drop, automatic ticks/survival installation/removal. Assets failed missing model before exporter and now pass exact supplied UV/PNG.
- Task 2: complete. Live client probes passed both loaders: native widgets, right-click opening, packet application, sync/save/reopen, compact bounds. Inspected both rendered screenshots. Native ScreenOpener queues screen opening; probe waits rather than assuming synchronous open. NeoForge offscreen capture bypasses vanilla blur only in test code, since blur redirects framebuffers.
- UI visual repair: move count into native footer above radius; use native torch transform. No new GUI artwork.
- Final read-only review: no critical/important defects. Reviewer verified automatic ticker/NBT and cleared contraption duplication concern using upstream removal order.
- Final: minor (deferred): outline is a cubic bounding box of spherical detection; corners are not counted. Actual spherical radius stated in UI. Replace with sphere outline later if desired.
- Final review boundary: external claim-mod compatibility not tested; standard vanilla permission/build/distance/chunk guards are enforced. Unrelated dirty changes are preserved. Build/deployment evidence follows separately.
- Task 3: complete. Normal dual build passed API v3, trait v1, release isolation and 23270 checks. Current resource regression suites passed. Deployment 20261004-202324-2.2.2-creature-transmitter verified one active laowu and exact SHA per instance; prior packages backed up. See docs/deployment-creature-transmitter-20261004.json. Existing develop worktree retained; no commit/push/merge.
