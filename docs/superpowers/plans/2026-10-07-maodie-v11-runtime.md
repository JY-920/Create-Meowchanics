# Maodie v11 runtime integration

User approved the v11 preview for game integration and requested direct implementation without another approval gate.

Goal: Forge 1.20.1 and NeoForge 1.21.1 use the accepted model and eight individual clips. Third stroke hisses ten ticks then summons directly, without aura, with distinct combat sounds.

Architecture: reuse RuntimeBlockbenchModel, including its free-format mirroring path. Cache exported animation tracks and binary-search linear samples. Server phase clock remains authoritative. Keep legacy GiantCatBoss save recovery but never restore new fighters.

- [x] Export production geometry, atlas and eight clips from art/maodie-fighter-v11; exact source fidelity and identical dual assets verified by tests/maodie-runtime-assets.mjs.
- [x] Replace handwritten fighter model with runtime asset and clock-driven poses; preserve reload, tint and invisible rendering.
- [x] Parallel combat task: six impacts at 2/4/7/10/12/15 ticks, append jab/block/slam phases, fast tracking, collision-safe movement and varied vanilla audio. Pure timing tests and dual GameTests green.
- [x] Parallel summon task: ten-tick third hiss, safe front spawn, no aura, wild only, permanent fighter. Pure state tests and dual GameTests green.
- [x] Build both platforms, run isolated game tests and actual client screenshot probes; review rendering and correct failures.
- [x] Check target processes, back up old JARs, deploy, verify SHA-256 and exactly one active laowu JAR per instance. No commits, pushes or user-save edits. Deployment record: docs/deployments/2026-10-07-fighter-v11.json (2.2.2-dev.fighter-v11.1).

Review focus: free-format handedness/UVs; linear unwrapped baked rotations; six hits versus vanilla immunity; old pending save migration; blocked/unloaded front space; no-Create dedicated server separation.

Existing dirty develop is retained because it contains the accepted art and previous requested features. Agents own combat/encounter files; main owns assets/model/sampler/build/deploy.

Verification (2026-10-07):

- Both loaders: 40/40 actual GameTests, build/fighter-v11-final-green.log. Added failing-then-passing regressions for attacks/pull/orbit during death and zero-resistance early-combo knockback.
- Both actual clients: build/fighter-v11-client-final.log reports FIGHTER CLIENT PASS. Forge with Create; NeoForge without Create. Entity creation events prove exactly ten ticks from third stroke to front spawn, independent of client observation latency.
- Nine action screenshots per port in build/maodie-client-v11-3/screenshots. Reviewed model/UVs and gameplay poses against accepted v11 art; the Forge guard capture is too close due initial camera synchronization, but subsequent full-body action captures and Neo guard are valid.
- Exact geometry/atlas/eight-clip export test passes. Review confirmed coordinate reflection/rotation order; fixed death-time attacks and early combo's additive vanilla knockback before deployment.
- Audio uses existing Minecraft sound events: weak/strong/knockback/sweep/no-damage attacks and shield block, with per-move pitch/volume; no new downloaded audio.
