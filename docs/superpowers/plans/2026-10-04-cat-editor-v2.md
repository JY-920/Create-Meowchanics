# Cat editor supplied-atlas redesign

> Execute inline with test-driven development and one final fresh review. User explicitly requested no second confirmation. Work in the existing develop worktree, do not commit/push unrelated work.

## Goal and design

Replace existing CatEditorScreen with the supplied two-page atlas, retain owned adjacent-cushion target locking and frozen trait API/schema. Both Forge 1.20.1 and NeoForge 1.21.1 ship version 2.2.2.

- Traits first tab: up to four compact ordered installed traits; shared scanner rarity cards, level badges and tooltips. One install row immediately after installed entries, never intervening gaps. Four independent token slots allow extraction into the matching empty slot or installation from the next row. Reject occupied output, duplicate/conflicting/disabled traits, stale menus and duplicate clicks without item duplication or penalties.
- Successful install only: EXCELLENT uses mutually exclusive rolls 0..59 NOW loss10..50,60..69 MAX loss10..20,70..79 NOW30..50+MAX10..20,80..99 no loss. GOOD 0..49 NOW10..50 else none. COMMON/DEFECT none. Choose a random stat independently for each applicable loss, floor at0 and retain NOW<=MAX. Extraction free. Hover enabled install button masks NOW+MAX for EXCELLENT, NOW for GOOD, neither lower rarity. Show scanner icons, pixel digits and tier blocks.
- Appearance second tab: whole+11 regions, vanilla coat cycling, one block sample slot per row, preview clone updated live. Reset cancels draft to opening appearance, confirm commits atomically. Consume one block only for a used changed sample upon successful confirmation, never during preview/reset. Server validates current samples at commit. Closing returns all temporary items exactly once.
- Inventory below both pages; supplied atlas drawn at pixel scale with safe compact layout at320x240. Preserve original artwork and bottle shape; four rarity colours affect bottle contents only.

## Tasks

1. Server: CatEditorTraits risk and non-mutating install eligibility; CatEditorMenu16 temp slots, row ordering, draft selection/reset/commit, authority checks. Add CatEditorV2Probe with probability branch boundaries, low-value clamps, free removal, blocked-install no loss, draft cancellation/atomic commit/sample swap and close/shift transfers. Run RED then GREEN via existing accessory-gametest init on both ports.
2. Assets/client: supplied atlas import, CatEditorScreen left tabs/cards/stats/preview; reuse CatTraitCardRenderer and CatStatsGoggleOverlay. Four bottle rarity appearances preserving saved ID. Update export-cat-editor.mjs and asset tests. Extend actual client probe for both pages, visible slots, mask states and preview-vs-source assertions,320x240 fitting and screenshots.
3. Verify dual game tests and real client rendering, normal build(API and isolation checks), fresh read-only review, fix concrete regressions, deploy only once clients stopped and verifySHA256/count. Record deploymentJSON. Keep user scripts, saves and other mods untouched.

## Review focus

Repeated/stale clicks must not reroll costs; hidden-page items remain recoverable; invalid/conflicting/missing custom traits never lose values; preview must not mutate real entity; a changed sample between preview/confirm is server-authoritative.
