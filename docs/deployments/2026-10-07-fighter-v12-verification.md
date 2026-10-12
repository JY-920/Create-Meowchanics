# Fighter v12.1 verification

Scope: halve attack animation/effect speed, add 10–16 tick recovery, separate defensive block from pull animation, spawn near the shelf cat, track nearby players with the shelf cat's head, unify shelf hiss into one ten-tick visual/audio window.

## Evidence

- Pure timing and bounded gaze tests: PASS. Timing and new shelf tests were first exercised against old behavior (RED).
- Both loaders: 45/45 required GameTests passed in `build/fighter-v12-green.log` (without Create). Tests cover block not pulling, readable recovery, all four shelf orientations, blocked close spawn preserving the source cat, gaze, single hiss and reload cleanup.
- Both normal release builds: PASS, including accessory API v3, trait API v1 and release isolation checks (`build/fighter-v12-release-build.log`).
- `node tests/maodie-runtime-assets.mjs`: PASS, approved geometry/UV/atlas/eight source clips preserved across both loaders.
- Actual isolated clients: Forge with Create and NeoForge without Create passed `build/fighter-v12-client2.log`. Three native right-click packets generated exactly three hiss windows and three active one-shot sounds; all sounds stopped afterwards. No ambient audio session was created. Rendered geometry followed the same hiss clock. Third hiss created one unnamed permanent fighter exactly ten ticks later at (1.25, 100, 1.5), close to the displayed cat. Nine action timelines advanced; connected-player pull worked; death did not restore the cat.
- Screenshots manually inspected under each loader's `build/maodie-client-v12-2/screenshots`: shelf gaze/sitting, single hiss, and action poses. Filenames retain historical `fighter-v11-` prefix. First fixture run had premature/cropped shelf capture and Neo missed the first observation during loading; fixture now waits for stable player/camera and first visual cycle, and both strengthened reruns passed.
- Focused read-only review found no critical/important defect; suggested audio lifecycle coverage was added and passed in both actual clients.

## Deployment

See `2026-10-07-fighter-v12.json` for old-package backups and installed SHA-256. Both target clients were stopped. Exactly one modId `laowu` remains enabled per instance. No save/config/other-mod changes, commits or pushes.
