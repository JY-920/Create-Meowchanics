# Optional Create Implementation Plan

> Agent execution is inline in the current develop worktree, as authorized by the user.

Goal: support cat gameplay without Create while retaining existing behavior when Create is installed.
Architecture: one published mod, loader-only presence detection, isolated Create integration registration and client hooks. No standalone industrial replacement or new recipes.
Spec: ../specs/2026-10-05-optional-create-design.md

## Global constraints

- Forge 1.20.1 and NeoForge 1.21.1 both supported.
- Preserve laowu namespace, cat saved data, accessory API v1-v3/schema 1 and trait API v1/schema 1.
- Stable main stays at a4a56d1 during development. Changes proceed on develop.
- No user world edits or live-client JAR replacement. No new alternative recipes in this work.

## Task 1: Establish optional class-loading boundaries

Files: both ports' compat/create/CreateIntegration.java, CreateMixinPolicy.java, CreateProcessingEvents.java; mixin/CreateOptionalMixinPlugin.java; laowu.mixins.json; CommonEvents.java; LaoWuMod.java.

- [x] Use loader metadata to detect Create without referencing its classes.
- [x] Skip Create-targeted mixins and vanilla-target contraption mixin when absent, retaining core cat mixins.
- [x] Move unchanged stateful deployer recipe hook into a conditionally registered class, removing Create event signatures from CommonEvents.
- [x] Run plain-JVM policy checks without any Create dependency.
- [x] Verify actual installed-Create event registration and existing machine crafting on both loaders (6 GameTests per loader passed).

## Task 2: Split core registration and startup

Files: both LaoWuMod.java entry points; compat/create startup/registration adapters; client/ClientModEvents.java and other annotated subscribers; machine registration classes.

- [ ] Isolate industrial block/entity/menu/recipe and item factories; do not instantiate Create subclasses when absent.
- [ ] Retain all existing registrations and IDs when present; hide unavailable industrial content from creative tabs when absent.
- [ ] Move Create tooltips/stress, ponder, rendering and Curios Create renderer setup behind the integration boundary.
- [ ] Run isolated no-Create client/server startup to locate any remaining linkage errors.

## Task 3: Keep cat gameplay independent

Files: CommonEvents.java, CareerCatBehavior.java, genetics/CatBehaviorTraitEffects.java, core cat item/entity classes, client/CatProfileScreen.java and core previews.

- [ ] Guard Create-only jobs and interactions while retaining cat attributes, traits, accessories, bosses and combat.
- [ ] Remove Create GUI/widget dependencies from retained core screens or provide independent implementations.
- [ ] Verify cat spawn/load, profile editing, outfits, accessory effects and retained public APIs without Create.

## Task 4: Resource and dependency metadata

Files: both ports' recipes/tags; Forge mods.toml; NeoForge template neoforge.mods.toml; build/runtime test configuration.

- [ ] Gate Create recipe serializers, external ingredients, loot and required tag references when absent.
- [ ] Add no alternate recipes; keep unavailable industrial paths unavailable.
- [ ] Mark Create optional only after no-Create client/server and core gameplay checks pass.
- [ ] Verify with Create installed that recipe outputs, IDs and data remain compatible.

## Task 5: Final compatibility verification and delivery

- [ ] Run dual-loader normal builds, API/schema/isolation checks and relevant actual KubeJS compatibility regressions if behavior changes.
- [ ] Run both presence modes on each loader; report actual gaps instead of claiming optional support from metadata.
- [ ] Commit develop changes and verify remote develop; preserve stable main.
- [ ] Deploy a completed build after checking clients, backing up and matching hashes. Do not deploy an unfinished optional conversion.
