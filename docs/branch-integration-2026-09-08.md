# Main / develop integration audit — 2026-09-08

Inputs: main `4fb399f`, develop `7ced652`. All three local worktrees were
clean before integration. This merge retains both histories rather than
overwriting either branch.

## Stable fixes checked

- `a490258`: both cat-ball textures already match the stable corrected blob
  `d6eadcc4511add3d2169e26e734208e80ab1f495`.
- `4fb3cf4`: develop's `7ced652` already restored vanilla Diamond tool tiers
  and Cat Ingot repairs. The custom Forge tier stays deleted.
- `4fb399f`: carry forward deletion of both bucket filling/emptying recipes
  on both loaders. These duplicate recipes were still present on develop.
- Resolve the NeoForge CommonEvents conflict with Create's
  `AllItems.CARDBOARD_SWORD.isIn` check and the stable canceled-attack handler.
  Keep develop's PlayerAttackMixin and all newer gameplay handlers.
  `CatPancakeBehavior.flatten` checks for an already flattened cat, so the
  two entry points cannot repeat the conversion effects.
- Preserve the stable Forge build version 1.0.2 instead of reverting to
  develop's older 1.0.1 metadata.

Compared with develop, the integrated implementation changes only the Forge
version, the four duplicate recipe removals, and the NeoForge cardboard-sword
event integration. No other develop implementation or asset is removed.
No Photon/LDLib dependency is introduced.

Validation: both loader builds succeeded. Packaged JARs contain the latest
spawn-preference packet and exclude the four duplicate fluid recipes, the
removed Forge custom tier, and the old Photon VFX assets/classes. No gameplay
session was run as part of this integration.

After validation, main and develop should point to this same merge commit.
Future stable-only fixes must be merged back into develop before new
development packages are built. A successful build checks compilation and
packaging; it does not replace multiplayer or in-game regression testing.
