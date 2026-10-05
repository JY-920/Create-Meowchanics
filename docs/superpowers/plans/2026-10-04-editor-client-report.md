# Editor client regression report — 2026-10-04

Scope: only the Forge/NeoForge isolated CatEditorClientProbe fixtures; no production edits or deployment.

## Coverage

- Preserved four-facing real baked OBJ/item model checks (62 quads), GPU model screenshots, and capture helpers.
- Asserted 630×512 editor atlas and original 16×16 bottle asset.
- Populated both catalog levels and ordered row indices; checked two compact installed cards plus exactly one install row, and four installed cards with no fifth row.
- Captured shared scanner-style cards and a real hovered detailed tooltip.
- Captured all four rarity bottles; compared NOW/MAX GPU regions: EXCELLENT masks both, GOOD only NOW, COMMON/DEFECT neither; tier icons remain unchanged.
- Checked 16 temporary + 36 inventory slots within 320×240, active-slot switching, and 12 appearance choices.
- Verified material samples and vanilla draft selections update a detached real cat preview before commit, emit changed GPU pixels, and leave source genome/pose unchanged.
- Preserved real localized bottle title/level/ID/effect tooltip assertions; added pairwise differing runtime bottle color pixels.

## Execution

NeoForge first isolated client run passed all then-current assertions. Screenshot review exposed inventory-frame clipping at the bottom; reported to parent, who corrected production layout. The probe screenshot helper now draws custom choice backgrounds before their labels.

Final corrected-layout NeoForge rerun passed every assertion and exited successfully: BUILD SUCCESSFUL in 1m 8s. Log: neoforge-1.21.1/build/editor-v2-client.log. Screenshots: neoforge-1.21.1/build/cat-sixway-client-editor-v2/cat-editor-*.png.

Visual review confirmed the full hotbar now fits and the real clone is larger. Remaining visual concern reported to parent: appearance left-arrow pixels overlap the first Chinese title character, and the 0.65-scale material subtitle is hard to read. These are not assertion failures; title padding/size warrants adjustment.

Forge execution is owned by the parent; both port fixtures contain the same assertions with platform API adaptations.
