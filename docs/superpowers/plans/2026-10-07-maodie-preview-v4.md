# Maodie preview v4 implementation plan

> **For agentic workers:** Use superpowers:executing-plans for this small preview-only adjustment.

**Goal:** A simple continuously bent rectangular torso, genuinely forward straight punches, and a default 0.5× preview.

**Architecture:** Keep the existing native Blockbench model generator and renderer. Emit a separate v4 art directory, preserving v3 and all Java runtime files. The mesh has four constant-width sides, each using its original body UV exactly once from bottom to top.

**Tech Stack:** Node, native Blockbench render/animation API, Playwright, ffmpeg.

**Spec:** User's 2026-10-07 request in this chat: simplify the bent body and UV; jab forward; try 0.5× speed.

## Global constraints

- Preview first; no runtime integration, deployment, commits, or pushes.
- Preserve existing v2/v3 assets and unrelated dirty worktree changes.
- 0.5× is playback for comparison, not a change to game attack intervals or source animation timings.

## Review focus

- Four sides must not duplicate body texture bands or invert end caps.
- Both fists in a combo must travel forward, not just the single jab.
- Native animated geometry must remain above the floor and inside the camera.
- Initial video playback must really be 0.5×, not only a selector label.
- Model and preview must refer to the same v4 asset.

### Task 1: Geometry and punch direction

Files: tools/build-maodie-fighter-art.mjs, tools/preview-maodie-fighter-art.mjs, tests/maodie-fighter-shape-direction.mjs.

- [x] Add and run regression checks against v3: constant four-corner rectangular sections, original four body UV strips without duplication, native wrist-to-shoulder trajectory less than 7° off actor forward at jab/combo impact.
- [x] Preserve the v3 generator; emit v4. Simplify torso to four sides with constant section dimensions and shared curved vertices; compensate straight-punch yaw/roll without removing the lunge.
- [x] Run art tests and native full import/contact/render validation; inspect front and side punch captures.

### Task 2: Half-speed preview

Files: tools/maodie-fighter-preview.html, tools/package-maodie-fighter-preview.mjs, tools/serve-maodie-fighter-preview.mjs, tests/maodie-fighter-preview.mjs.

- [x] Change browser regression expectations to initial 0.5× and actual video playbackRate 0.5; run against v3 and observe failure.
- [x] Emit v4 preview, set initial video playbackRate as well as image-sequence rate, retain default flow and all other clips, add front/side jab impact captures for direction inspection.
- [x] Regenerate native media, run browser/media tests and all Node regression suites, provide the v4 preview and editable model links. Browser open request queued; native visible application launch was policy-blocked, so do not claim it opened.

## Execution notes

Already in linked develop worktree. User requests direct iteration without additional confirmation; execute inline. No shared runtime interfaces; task 2 consumes task 1's v4 model, clips, and frames.
