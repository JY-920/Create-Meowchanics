# Cat Deployment Platforms Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans for core implementation. Independent resource export and API investigation use dispatching-parallel-agents.

**Goal:** Add both user-authored cat deployment machines with persistent hissing fuel and safe pancake restoration.
**Architecture:** Shared CatDeploymentBlockEntity owns4000mB tank, one pancake slot, optional target and deployment transaction. Block/item implement interactions and portable data; CatPancakeItem adds an additive restore wrapper. Ejector uses Create EntityLauncher; client renders baked moving parts and held pancake.
**Tech Stack:** Java17/21, Forge1.20.1 / NeoForge1.21.1, Create6, existing source exporters and GameTests.
**Spec:** docs/superpowers/specs/2026-10-03-cat-deployment-platforms.md

## Global Constraints

- 4000 mB hissing-only,250mB per successful deployment. Preserve full cat data.
- Preserve dirty workspace and API contracts. No commit/push. Both local instances deployed only when closed.
- Existing linked develop worktree retained; no new checkout.

## Review Focus

- Insufficient/wrong fluid never consumes input; simulation is non-mutating.
- Invalid/blocked/unloaded destination cannot destroy pancakes or force-load chunks.
- Save/break/place retains fuel and inventory exactly once; no duplicate drops.
- Target selection validates dimension/limits and cannot bypass server checks.
- Full cat snapshot/ownership and both client/server registration remain correct.

### Task1: Authored models
- [x] Export source BBModels and original PNGs under both IDs, distinguish newer ejector model from stale Geo.
- [x] Run node tests/cat-deployment-platform-assets.mjs and neighboring asset regression.

### Task2: Core deployment
Files: both ports create/CatDeploymentBlock.java, CatDeploymentBlockEntity.java, item/CatDeploymentBlockItem.java, item/CatPancakeItem.java, LaoWuMod.java.
Interfaces: tank, inventory; catStack(); tryDeploy(); setTarget(BlockPos):boolean; target():BlockPos; portableStack(); launchProgress(float).
- [x] Add GameTests for missing registration (RED), fuel validation/cost, transaction failure, persistence and complete cat restoration.
- [x] Implement core and additive restoration wrapper; registry, capabilities, creative tab, language, mining tags.
- [x] Run dual compile and focused GameTests (GREEN).

### Task3: Ejection and visuals
Files: both client/CatDeploymentRenderer.java, client registration; deployment block item targeting.
- [x] Test selection limits, deployment launch and emitted moving/item geometry.
- [x] Implement Create trajectory, selection workflow, plate/rod animation, pancake display.
- [x] Run dual client probes and inspect screenshots.

### Task4: Verify/deploy
- [x] Independent review, normal dual build including frozen APIs and release isolation.
- [x] Check clients stopped; tools/deploy-cat-additions.ps1 -Version2.2.2 -Labelcat-deployment; record hashes and backups.
