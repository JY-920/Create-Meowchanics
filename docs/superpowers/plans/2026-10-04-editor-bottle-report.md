# Trait bottle rendering report

## Changes

- Both loaders retain item ID and saved trait data unchanged. CatTraitTokenItem now supplies a lazy client renderer.
- Original 16×16 `assets-source/cat-editor/bottled-trait.png` copied byte-for-byte to both item textures. No bitmap recolouring or shape edits.
- Runtime renderer reads original pixels and emits 1-pixel-thick cuboids in all item contexts, sampling an original opaque white highlight as the neutral texel. Original pixel ARGB is supplied as vertex colour.
- Only the 19 saturated gold liquid/droplet pixels change colour. Pale glass, white highlights, cork, empty pixels and silhouette remain unchanged. Excellent retains authored gold; Good uses scanner frame blue 7C9DFB, Common brown B59370, Defect dark grey 575757, with original liquid shading retained.
- Item model uses builtin/entity plus explicit vanilla generated-item display transforms. Exporter token-model output matches it. No edits to unrelated exporter logic.
- Renderer exposes cache clearing and resource reload override. Parent wired clearCache into the actual existing reload listeners on both loaders.

## Verification

- TDD: standalone Java regression initially failed with `good must become blue` against identity recolouring; implemented colour policy, then passed.
- Both loader copies independently compiled with javac and passed `CatTraitTokenColoursRegression`.
- Regression checks four rarity behaviours, alpha retention, liquid shading, unchanged glass/cork and every source pixel against a hand-listed 19-pixel liquid mask.
- `node tools/export-cat-editor.mjs --check`: PASS, 11 assets per loader with exact original texture/UI/token bytes.
- `git diff --check`: no whitespace errors (existing unrelated line-ending warnings).
- Full Gradle build, rendered-client probes, game QA and deployment remain owned by parent/other assigned workers; not claimed complete here. No commits or deployment performed by this worker.
