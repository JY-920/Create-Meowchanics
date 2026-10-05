# Six-way Cat Machines Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans. Main agent implements tasks; bounded read-only source/art audits may run in parallel.

**Goal:** Deliver and deploy four functional six-direction Create-style cat machines on both loaders.

**Architecture:** Preserve Create inventories and processing behaviours through inheritance. CatMachineOrientation maps local DOWN to the saved bottom direction. Narrow instance-guarded hooks adapt private upstream positional assumptions; native processing progress drives separated authored model parts.

**Tech Stack:** Java 17/21, Forge 1.20.1/Create 6.0.8-291, NeoForge 1.21.1/Create 6.0.10-281, JEI, Mixin, real GameTests and client probes, Node model exporter.

**Spec:** docs/superpowers/specs/2026-09-25-sixway-cat-machines-design.md

**Execution record:** See sibling `2026-09-25-sixway-cat-machines-progress.md` for actual task status, evidence and deviations; the checklist below preserves the original pre-implementation test plan. Final acceptance is recorded in `docs/sixway-cat-machines-verification.md`.

## Global Constraints

- Existing laowu:cat_depot and laowu:haji_basin IDs and inventories remain compatible; default bottom=down.
- Press supports depot and basin; mixer supports basin only; target centers are two blocks apart, with aligned bottom and free intermediate space.
- Retain original recipe matching/consumption, configured speed requirements, stress, and sequence progress.
- Scope mixins to cat instances; no global Create block/BE registry mutation.
- Assets originate at D:/Project_minecraft/哈基机器/哈基辊压, 哈基搅拌, 哈基置物台. Original files stay untouched.
- Preserve unrelated dirty changes; do not commit/push. Keep probes, art sources and SDK out of runtime jars.
- User waived further design/plan confirmations. Deployment follows AGENTS.md; never replace an in-use game jar.

## Review Focus

- Mid-cycle target removal/replacement and wrong-facing surfaces must not consume or duplicate materials (Task 3 GameTests).
- Switching or closing shaft ports must disconnect old kinetic edges and clear stale speed (Task 2 GameTests).
- Basin spout queues, sided capabilities and local heat position must remain correct on all six orientations (Task 3 GameTests).
- Legacy saves without bottom and ordinary Create machines must retain original upright behaviour (Tasks 1/3 regression).
- Server-only startup, Flywheel enabled and missing JEI must not load client-only classes or hide animated parts (Tasks 4/5/6).

### Task 1: Orientation contract and failing integration probes

**Files:** both loaders create/CatMachineOrientation.java; tests/gametest/{forge,neo}/cn/laowu/mod/test/CatSixWayProbe.java; tests/accessory-gametest.init.gradle.

**Interfaces:** `BOTTOM` DirectionProperty named bottom; `bottom(BlockState)`, `top(BlockState)`, `toWorld(Direction bottom, Vec3 vector)`, `toWorld(Direction bottom, Direction local)`, `toLocal(Direction bottom, Direction world)`, `rotatePosition(BlockPos origin, BlockPos localPosition, BlockState state)`, `rotateShape(VoxelShape, Direction)`.

- [ ] Add probe-only Gradle selector catSixWayProbeOnly and tests that inspect real registry/state and placement.
- [ ] RED: run isolated Forge GameTest; expect missing cat_press/cat_mixer and missing bottom property, not startup failure.
- [ ] Implement transforms and state defaults. Use literal mappings: DOWN identity; UP rotate X180; NORTH rotate X90; SOUTH rotate X-90; EAST rotate Z90; WEST rotate Z-90.
- [ ] Test transform inverses, six model orientations, existing upright inventory load, no player-world access.

### Task 2: Registration, placement, wrench and kinetic ports

**Files:** create/CatPressBlock.java, CatMixerBlock.java, CatProcessorPlacement.java, CatMachineBlocks.java; LaoWuMod.java; resources blockstates, item models, loot and mining tags.

**Interfaces:** `SHAFT_AXIS` EnumProperty<Direction.Axis>, `SHAFT_OPEN` BooleanProperty; `validTarget(LevelReader, BlockPos, BlockState, boolean mixer)`; registration CAT_PRESS/CAT_MIXER/PRESS_BE/MIXER_BE.

- [ ] RED: exercise real item placement, closed shaft speed=0, wrench pair opening, forbidden axis, change axis and sneak removal.
- [ ] Implement independent cat blocks with Create BE types; validate matching target at pos.relative(bottom,2), no blocked intermediate.
- [ ] Make kinetic equivalence depend on bottom, axis and open, so every connectivity change invalidates its network.
- [ ] GREEN: power through each permitted world axis using real creative motors; check stress and complete disconnect.

### Task 3: Directional processing and storage behaviour

**Files:** create/CatPressBlockEntity.java, CatMixerBlockEntity.java, HajiBasinBlock.java/HajiBasinBlockEntity.java, CatDepotBlock.java/CatDepotBlockEntity.java; narrow CatBasinOrientationMixin/CatDepotOrientationMixin/CatPressOrientationMixin and mixin configs.

**Interfaces:** Cat BE getBasin() returns only the aligned target; original PressingBehaviour supplies recipe handling. Basin FACING stays a local output direction, bottom controls world orientation.

- [ ] RED: feed one iron ingot to depot and nine ingots to basin in each orientation, assert plate/block products; copper+zinc with correctly placed heat must mix into exactly two brass.
- [ ] Adapt private position lookups and sided direction arguments only for cat instances. Preserve overflow buffers, filtering, sequence NBT and originals.
- [ ] Validate no heat/wrong filter/blocked gap/wrong orientation/removed target prevent consumption; queues resume after capacity returns.
- [ ] GREEN: run all six-way probes plus existing cat machine/depot regressions on both loaders.

### Task 4: Authoritative art, renderer transforms and process animation

**Files:** tools/export-cat-processors.mjs; tests/cat-processors-assets.mjs; client/CatProcessorRenderer.java, CatBasinRenderer.java, CatDepotRenderer.java, CatMachinesClient.java; authored art copy and generated resource JSON/PNG.

**Interfaces:** exported static shell by open axis, press_head/rod/sleeve partials, mixer_head; renderer applies the same bottom rotation around block center then native progress offsets.

- [ ] RED: export fixture checks winding, original PNG bytes, six states, selected opposing opening faces and isolated moving parts.
- [ ] Export bbmodel partitions; bake quarter turns and preserve inverted cubes/zero-thickness paddles. Use no new texture painting.
- [ ] Drive head travel using Create progress, rotation using real speed. Rotated bounding boxes include the full stroke. Basin/depot contents rotate with shells.
- [ ] GREEN: real client renders all orientations at idle/midstroke/endstroke; inspect screenshots and missing-texture/error logs, with Flywheel on.

### Task 5: JEI and user-facing integration

**Files:** both compat/jei/LaoWuJeiPlugin.java; language JSON; tests/machines/* client probes.

**Interfaces:** Create JEI category types from the exact compiled dependency; catalysts include relevant new machines and existing basin/depot.

- [ ] RED: query actual JEI runtime catalysts for press/mixing/packing, confirm missing entries before change.
- [ ] Add catalysts only to original categories; no recipe clones. Localize concise six-way/wrench information.
- [ ] GREEN: verify original recipe pages reachable from cat machines and no duplication.

### Task 6: Full verification, fresh review and safe deployment

**Files:** docs/sixway-cat-machines-verification.md; tools/deploy-cat-sixway.ps1 or existing safe deployment utility; docs/deployment-sixway-cat-machines.json.

- [ ] Run both normal Gradle builds, asset tests, dedicated server GameTests and client probes. Require explicit PASS markers and no Mixin failures.
- [ ] Fresh-context reviewer checks scoped production files and five Review Focus cases; address important findings with regression tests.
- [ ] Confirm runtime jars omit probe/source-art/SDK artifacts and API verification tasks pass.
- [ ] Check actual game processes/instance paths; if target instances are closed, back up exact modId=laowu jars, copy builds, verify SHA-256 and one enabled jar per instance. Do not touch other mods/configs/saves.
- [ ] Record all evidence and remaining limitations; deliver concise completion with real deployment status.
