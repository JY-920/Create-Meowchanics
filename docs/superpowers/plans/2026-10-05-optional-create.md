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

- [x] Isolate industrial block/entity/menu/recipe and item factories; do not instantiate Create subclasses when absent.
- [x] Retain all existing registrations and IDs when present; hide unavailable industrial content from creative tabs when absent.
- [x] Move Create tooltips/stress, ponder, rendering and Curios Create renderer setup behind the integration boundary.
- [x] Run isolated no-Create client/server startup to locate any remaining linkage errors.

## Task 3: Keep cat gameplay independent

Files: CommonEvents.java, CareerCatBehavior.java, genetics/CatBehaviorTraitEffects.java, core cat item/entity classes, client/CatProfileScreen.java and core previews.

- [x] Guard Create-only jobs and interactions while retaining cat attributes, traits, accessories, bosses and combat.
- [x] Remove Create GUI/widget dependencies from retained core screens or provide independent implementations.
- [x] Verify cat spawn/load, profile editing, outfits, accessory effects and retained public APIs without Create.

## Task 4: Resource and dependency metadata

Files: both ports' recipes/tags; Forge mods.toml; NeoForge template neoforge.mods.toml; build/runtime test configuration.

- [x] Gate Create recipe serializers, external ingredients, loot and required tag references when absent.
- [x] Add no alternate recipes; keep unavailable industrial paths unavailable.
- [x] Mark Create optional with actual no-Create client/server and gameplay validation, not metadata alone.
- [x] Verify with Create installed that recipe outputs, IDs and data remain compatible.

## Task 5: Final compatibility verification and delivery

- [x] Run dual-loader normal builds, API/schema/isolation checks and actual KubeJS compatibility regressions.
- [x] Run both presence modes on each loader; report actual gaps instead of claiming optional support from metadata.
- [ ] Commit develop changes and verify remote develop; preserve stable main.
- [x] Deploy a completed build after checking clients, backing up and matching hashes. Do not deploy an unfinished optional conversion.

## Verification notes

- Normal build on both loaders passed: accessory API v3, trait API v1, animation/resources, release isolation and 23,270 accessory definition/state checks.
- Installed Create: 50 actual server tests per loader, including KubeJS accessories/traits, processing hook, crafting and support behavior. Existing real client/GPU visual probes passed on both loaders.
- No Create: real clients passed with profile, all outfit previews, creative contents, every registered item tooltip, scanner looking at vanilla chest and independent rider pose. JEI/Curios combination passed too.
- No Create: 14/14 actual server tests on each loader, including KubeJS trait examples, mount/capture/backpack behavior, engineering attacks, disabled industrial packets and saved logistics recovery. Final dual-loader clients also rendered the standalone engineering cannon.
- Node regression suite: 73/73.
- Review found scanner, industrial packet and transport gesture linkage holes; each was reproduced before its fix. Saved delivery motion releases gravity while retaining cargo data; the test includes native cat fall immunity and actual 220-block descent.
- Standalone recipes are deferred, and removing Create from an existing industrial world is not a lossless migration. See docs/optional-create.md.
- Local deployment completed at 2026-10-05 11:57:24 Asia/Shanghai. Both instances have one enabled laowu JAR, matching build SHA-256; previous packages are recoverable in mod-backups/create-meowchanics/20261005-115724-optional-create.1. See docs/deployment-optional-create.1.json.
