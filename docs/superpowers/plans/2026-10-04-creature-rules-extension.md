# Creature Rules and Transmitter Extension Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development. Steps use checkbox syntax for tracking.

**Goal:** Deliver the approved playable prototype of grouped creature attributes, analog transmitter output, indicator lights and persistent range display.

**Architecture:** CreatureFilterRules remains the shared predicate boundary for laser and transmitter. Extend saved filters with grouped conditions while retaining v1/v2 semantics; keep counting and redstone configuration on the machine. Reuse Create native widgets/background regions.

**Tech Stack:** Java 17 Forge 1.20.1, Java 21 NeoForge 1.21.1, Create 6, Gradle GameTests and native client probes.

**Spec:** Approved design in this document: each mob is evaluated individually; groups OR together, species AND group attributes; attributes aggregate using native ANY/ALL/NONE. First prototype includes baby/adult, tamed/untamed, health and health percentage. Default target is all mobs; an explicit group with no conditions matches its target, whereas legacy blank filters retain their previous match-none behavior. Maximum 8 groups and 64 total conditions. No UUID configuration.

## Global Constraints

- Dual loader parity; preserve old item IDs, filter v1/v2 behavior and existing accessory/trait APIs.
- Do not commit, push, publish or modify unrelated dirty files. Only deploy after dual normal build and verification; never replace running-client JARs.
- Use existing Create native controls, background regions and icons; no newly drawn pixel assets.
- Players are not selectable mobs. Unsupported attributes fail closed even when negated (e.g. cow is not an untamed pet).
- No cat trait behavior changes and no crash-performance fix in this task.

## Review Focus

- Legacy gates and empty filters survive edit/save/load and packet roundtrip.
- Inverted unsupported attributes must not accidentally match every mob.
- Duplicate group matches count once; no nearby-entity references retained in item NBT.
- Range display follows radius edits, expires at 180 seconds and disappears on removal/world change.
- All mode and filter-removal packets enforce interaction permissions and distance.

### Task 1: Grouped filters and native configuration GUI

**Files:** dual `item/CreatureFilterRules.java`, new `item/CreatureAttribute.java` if needed, `CreatureFilterMenu.java`, `client/CreatureFilterScreen.java` (extract a focused group editor screen if useful), `network/SetCreatureFilterPacket.java`, `network/ModNetwork.java`, lang resources; `tests/gametest/{forge,neo}/.../CreatureFilterProbe.java`, existing filter client probes.

**Interfaces:** Preserve `matches(Mob)`, old factories/accessors. Add grouped rules factory and immutable Group target/mode/conditions representation, with NBT v3 and bounded packet codec; expose these to native GUI. Machine code consumes only unchanged matches boundary.

- [x] Add failing GameTests for tamed low-health cat OR adult cow, exclusive species gating, overlapping groups, unsupported inverted condition, malformed bounded payload and legacy roundtrip. Run Forge focused filter probe and observe RED.
- [x] Implement bounded attribute/group evaluation, serialization and packet codec; migrate only explicit GUI changes, retain old semantics. Validate health finite ranges and entity IDs; do not reinterpret unknown data as match-all.
- [x] Add native GUI group selector/add/delete, target name/ID search, attribute/comparison/value controls and native add/invert buttons, group ANY/ALL/NONE. Keep name-tag summary and player inventory, do not close on selection. First target all mobs; changing group restores its controls. Editing conditions does not silently discard legacy rules.
- [x] Extend client probe to exercise two groups, search, values, save/reopen, compact screen bounds; capture screenshots for controller visual review.
- [x] Run dual focused server/client filter probes and report exact commands and results. Run normal dual build once at integration, not concurrent with another task's Gradle.

### Task 2: Transmitter output, lights and persistent sphere

**Files:** dual `create/CreatureTransmitterBlock.java`, `CreatureTransmitterBlockEntity.java`, `client/CreatureTransmitterScreen.java`, new focused client range renderer, packet/network registrations, `tools/export-creature-transmitter.mjs`, relevant client registrations and lang, transmitter server/client probes.

**Interfaces:** Preserve old configure overload; add mode enum THRESHOLD/ANALOG, default THRESHOLD in old saves. `getSignal()` provides actual 0..15 output. Grouped filters are consumed unchanged through matches.

- [x] Add failing tests: threshold low/high hysteresis, analog low0 high15 count1=>1; high150 count10=>1, midpoint floors, saturation and inversion15-normal; save/load; all-face output and indicator updates; permission checks.
- [x] Implement proportional `floor(15*(count-low)/(high-low))`, clamped. Preserve threshold semantics. Native two rows become lower/upper count endpoints with mode selector; show actual signal. Four side red indicators follow output without replacing user art or z-fighting.
- [x] Sneak rightclick toggles a client-local sphere wireframe, active for 180 seconds, removed on second toggle/block removal/world change. Screen temporary sphere shares renderer, persists after closing only when explicitly toggled. Match entity-position spherical counting radius. Move installed-filter removal to GUI filter icon using validated server packet; installing held filter remains normal rightclick.
- [x] Native client probe verifies toggle/expiry/removal, mode selection/network roundtrip and captures active/inactive machine/rendered UI.
- [x] Run dual transmitter probes plus existing filter/laser regressions, dual normal builds with API/release checks, then deployment and SHA verification when clients stopped.

## Execution record

Approved by user: “你尝试吧”; preceding request says no second confirmation. Use existing linked develop worktree. No commits: shared dirty checkout and AGENTS forbid treating deployment as commit authorization. Preserve task review snapshots instead of commit diffs.
