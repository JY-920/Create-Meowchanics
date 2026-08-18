# Cat Genetics and Attributes

This document defines the first stable data contract for mixed cat textures and
six-dimensional cat attributes in Create: Meowchanics. The Forge 1.20.1
implementation is the reference implementation while this system is being
developed.

## Goals

- A cat can inherit different visible body regions from different materials.
- Adding dozens or hundreds of future materials must not require a new entity
  type or a complete pre-baked texture for every possible combination.
- Genetics remain deterministic after creation and safe for multiplayer saves.
- Cat pancakes preserve the same genome and attribute profile as the living cat.
- Rendering and inspection do not perform server tick scans.

## Texture genome V2

`LaoWuCatGenome` is a versioned compound in the cat's persistent Forge data and
in captured cat-pancake NBT. Each locus stores a material resource location.

| Locus | Map colour |
| --- | --- |
| Head primary | `#00FFFA` |
| Head secondary | `#62FF00` |
| Left eye | `#FF0000` |
| Right eye | `#1E009A` |
| Ears | `#335454` |
| Muzzle | `#FFC100` |
| Front body | `#FFF975` |
| Rear body | `#007E9A` |
| Front legs | `#EC00FF` |
| Hind legs | `#6F6F6F` |
| Tail | `#007308` |

The client composes a 64x32 texture only when a mixed genome is visible. The
result is cached by the ordered phenotype key in a 256-entry LRU cache. Uniform
genomes use the original material texture directly. The server stores only IDs
and never generates image data.

V1 saves are migrated by copying the former shared `eyes` locus into both eye
loci and ignoring the removed `body_middle` locus.

## Six-dimensional attributes V3

The six stable loci, in goggles display order, are:

1. Attack
2. Health
3. Speed
4. Stamina
5. Intelligence
6. Luck

Every locus stores two integers from 0 to 100:

- **Current value**: the cat's presently expressed attribute.
- **Attribute Limit** (`属性上限`): the maximum value the current attribute
  may reach in a future growth/training system.

The persistent compound is named `LaoWuCatAttributes`:

```text
LaoWuCatAttributes
├─ Version: 3
├─ Current: { attack, health, speed, stamina, intelligence, luck }
└─ Potential: { attack, health, speed, stamina, intelligence, luck }
```

### Founder generation

An ordinary, non-bred cat receives its profile once on the server. This covers
wild spawns, spawn eggs, cats from existing worlds when first tracked, and the
random kitten pancake created by Cat Dough filling. Existing worlds do not need
a bulk migration.

For each locus:

```text
current = random integer in [0, 100]
attribute limit = random integer in [current, 100]
```

Each of the six loci is rolled independently. The uniform `0..100` current
range deliberately spreads ordinary values evenly across the five 20-point
bands, while exactly 100 is the rare perfect tier. This produces much larger
differences both between cats and between the attributes of one cat.

V1 profiles are migrated lazily and rewritten as V2 when the server next
touches the cat. Former current values from `24..60` are deterministically
expanded over `0..100`; their former growth room is preserved and the new limit
is capped at 100. The short-lived V2 format is also clamped from `0..150` back
to the intended `0..100`. Migration therefore widens old populations without rerolling
their relative ordering on every load.

### Fusion inheritance

For every child, five of the six complete loci are inherited from randomly
selected parents and the remaining locus uses a fresh founder roll. A locus
always carries its current value and Attribute Limit together. At least one of
the five inherited loci comes from each parent.

The fusion debug wand and vanilla cat breeding both call this exact method, so
debug results and production breeding cannot drift into separate formats. The
20% mutation rule currently belongs only to texture-region inheritance; numeric
attributes use the five-inherited-plus-one-random rule requested for the
breeding-box iteration.

These numbers are descriptive only in V2. They do not yet modify vanilla
attack damage, maximum health, movement speed, AI or loot.

## Goggles presentation

Looking directly at a cat or a dropped cat-pancake item while wearing Engineer
Cat Goggles opens only the supplied pixel panel, vertically centred beside the
crosshair. Create's ordinary Engineer's Goggles deliberately do not unlock
this cat-specific panel. The ordinary panel is 65x72 pixels; the crouched panel is 71x72
pixels to make room for the second tier marker. There is no text tooltip or
separate floating item icon. It displays the six supplied 8x8 attribute icons
in the order above. Its 5x7 number glyphs overlap by one pixel so adjacent
digits form the connected style authored in the panel.

The `NOW` column always displays current values. The `MAX` column displays
the newly authored `???` glyphs by default and reveals the six Attribute Limits
while the player is sneaking. While crouched, both the current value and the
revealed limit have their own six-pixel tier marker. Each marker uses the value
from its own column and the supplied seven-colour atlas: black for abnormal
data, then yellow, green, cyan, brown, pink and magenta for `0..19`, `20..39`,
`40..59`, `60..79`, `80..99` and `100+` respectively.

The overlay reads already-synchronised client NBT. It does not query the server
each frame. A single compact packet is sent when a player starts tracking a cat
and whenever the debug fusion creates or updates one.

## Breeding boxes

The three breeding boxes are persistent four-slot block entities. They never
scan the world or poll neighbouring inventories. Forge sided item capabilities
expose exactly one logical slot per face:

- Front: kitten-pancake output only.
- Visual front-left: adult father pancake input/output.
- Visual front-right: adult mother pancake input/output.
- Back, top and bottom: Cat Food input/output.

With both adult parents present, at least one Cat Food available and an empty
output slot, the server advances a saved progress counter. Basic,
Intermediate and Advanced boxes take 3, 2 and 1 real-time minutes respectively.
Completion consumes one Cat Food and writes one baby cat pancake into the
output slot; parents are retained for later cycles.

The baby uses the global five-inherited-plus-one-random attribute rule above.
Appearance regions are selected independently from either parent's saved
genome. The four affix rows are currently rendered as aligned visual
placeholders but contain no affix logic yet. A region mutates to another registered cat material with a box-specific
chance of 5%, 7.5% or 10% respectively.

The breeding screen is a read-only automation monitor: it has no player
inventory and exposes no clickable machine slots. Parents, Cat Food and the
result can only enter or leave through the sided item capabilities described
above or through the block's explicit hand interactions: adult pancakes fill
father then mother, Cat Food inserts directly, an empty hand removes the child,
and sneaking removes mother then father. It renders both parent cats from their
actual pancake NBT, without duplicate pancake item icons. The compact stat grid displays
current values normally and switches to Attribute Limits with the supplied
blue number glyphs while Shift is held. Mutation rate and machine tier use the
pixel glyphs authored in the supplied sheet rather than Minecraft text.

Cat pancakes in this and all other container screens display the normal side
attribute panel to the left of the cursor when hovered, but only while Engineer
Cat Goggles are worn; the ordinary item tooltip remains in its vanilla position
on the right. The same goggles requirement applies to living cats and dropped
cat-pancake item entities. Missing legacy data is shown as abnormal `???` until
the server first initializes that pancake.

## Multiplayer and performance rules

- The server is authoritative for first generation, inheritance and mutation.
- Generated profiles are saved; reconnecting never rerolls a cat.
- No per-tick world or entity scan is used by genetics or the overlay.
- Cat pancakes copy and restore both genetics compounds.
- Network payloads contain only small versioned NBT compounds.
- Future custom materials should be registered by resource location in a data
  registry; saved cats must never depend on an array index or load order.

## Next integration points

- Add a data-driven material registry with weights, eligibility and mutation
  rules for materials such as obsidian, rainbow and andesite-alloy cats.
- Define training/growth sources that raise current values without exceeding
  the Attribute Limit.
- Decide which gameplay systems consume each attribute before attaching vanilla
  entity attribute modifiers.
