# Maodie Fighter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans inline. No implementer agents; one final independent review.

**Goal:** Replace the Bogu giant challenge with a Butter-sized animated feline boxer.
**Architecture:** Independent Monster entity and articulated renderer. Shared source-cat restoration contract supports both new fighters and legacy giant backups. Client ground-wind effect reads the seat's synchronized clock.
**Tech Stack:** Forge 1.20.1 Java17 / NeoForge 1.21.1 Java21, articulated model parts and custom GLSL.
**Spec:** docs/superpowers/specs/2026-10-06-maodie-fighter-design.md

## Global Constraints

- 0.9×1.7 upright entity; visual scale1.5; no new giant attacks/model.
- User follow-up: custom GLSL wind, no vanilla particle spawning; longer standing torso/hind legs.
- Preserve source cat NBT, public accessory/trait API, no Create dependency.
- No automatic Git commit/push; preserve dirty baseline.
- Source-cat-safe restoration, player-only attacks, collision-safe movement.

## Review Focus

- Taming mid-aura preserves actual vanilla ordered-to-sit command.
- Peaceful despawn restores cat even when return floor is blocked.
- Reload during a punch must not duplicate an already applied hit.
- Pull and behind-movement must not pass through walls.
- Ground airflow cannot keep a global anchor alive after seat unload.

### Task 1: Fighter + encounter lifecycle

Files: both ports entity/MaodieFighter.java, entity/MaodieChallenge.java; encounter/MaodieEncounter.java; LaoWuMod.java; ModEntityEvents.java; entity/BoguShelfSeat.java.
Interfaces: MaodieChallenge sourceCatSnapshot()/maodieOrigin()/restorationPending()/markRestorationPending()/updateMaodieAbsence(boolean)/isMaodie()/isSummoning(); MaodieFighter beginAttack(byte)/getPhase()/getPhaseTicks(float).

- [x] Add real GameTests for fighter spawn, 300HP and Butter dimensions, summon20, punch impacts12/24, uppercut16, sweep22, wall-blocked hits, pull approaching without wall crossing, collision-safe behind arc, pending/Peaceful/kill recovery, NoAI and reload hit suppression.
- [x] Run RED; implement entity and shared lifecycle transaction.
- [x] Run GREEN with Bogu and genuine Giant regression.

### Task 2: Animated model + ground wind

Files: both ports client/MaodieFighterModel.java, MaodieFighterRenderer.java, MaodieAuraEffects.java, ClientModEvents.java; tests/standalone-client/*/MaodieFighterClientProbe.java.
Consumes: synchronized attack phase/start; seat aura start and anchor.
Produces: real render pose for guard/left/right/uppercut/sweep/pull/backstep, white-gray ground airflow.

- [x] Add client render assertions/screenshots with separate poses before implementation; run RED.
- [x] Implement upper/lower arms and fists; guard/strike/recovery interpolation.
- [x] Replace vertical gold halo with radius4 GLSL ground wind and local height sampling.
- [x] Run actual client screenshot verification in both ports and without Create.

### Task 3: Compatibility + delivery

- [x] Run Node suite and dual real KubeJS probes.
- [x] Run normal dual build with release isolation/API gates.
- [x] One fresh whole-change reviewer, one TDD fix pass if required.
- [x] Check actual game processes; deploy with backups/hashes if target instances stopped.
