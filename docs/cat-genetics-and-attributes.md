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

1. Combat Power (`战斗力`)
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

`attack` remains the stable serialized key for save compatibility, but its
player-facing name is Combat Power. It is an abstract stat that is converted by
the formula below and must not be confused with final melee damage.

### Founder generation

An ordinary, non-bred cat receives its profile once on the server. This covers
wild spawns, spawn eggs, cats from existing worlds when first tracked, and the
random kitten pancake created by Cat Dough filling. Existing worlds do not need
a bulk migration.

For each locus, the limit is rolled first so ordinary cats cover the whole
potential range instead of clustering in the highest colour bands:

```text
attribute limit = random integer in [0, 100]
current = random integer in [0, attribute limit]
```

Each of the six loci is rolled independently and exactly 100 remains the rare
perfect limit tier.

V1 profiles are migrated lazily and rewritten as V2 when the server next
touches the cat. Former current values from `24..60` are deterministically
expanded over `0..100`; their former growth room is preserved and the new limit
is capped at 100. The short-lived V2 format is also clamped from `0..150` back
to the intended `0..100`. Migration therefore widens old populations without rerolling
their relative ordering on every load.

### Breeding inheritance

A locus always carries its current value and Attribute Limit together. The
number of inherited loci is selected by breeding food. Every uninherited locus
receives a full fresh `0..100` limit roll. A successful numeric mutation adds a
second fresh roll and retains the candidate with the higher limit, using current
value as the tie breaker.

The Breeding Wand simulates Normal Breeding Food with an Advanced Box base
mutation chance, including both parents' Luck. Vanilla cat breeding uses the
same three-inherited-locus numeric contract without a machine mutation bonus.

The raw values in this compound remain the heritable genetic layer. Traits may
derive a temporary effective value from them, but never write that temporary
bonus or penalty back into `Current` or `Potential`. This prevents equipment,
time-of-day bonuses and negative traits from contaminating later inheritance.

### Effective attributes and gameplay formulas

Every consumer first calculates an effective value `E` from the raw current
value plus active trait/equipment bonuses and penalties. `E` may exceed the
heritable 0..100 range, but is never written back into genetics. The goggles
panel and gameplay conversion layer call the same function.

| Attribute | Runtime formula | E=0 | E=50 | E=100 |
| --- | --- | ---: | ---: | ---: |
| Health | `10 + 0.4E` maximum health | 10 | 30 | 50 |
| Combat Power | `2 + 0.08E` base attack damage | 2 | 6 | 10 |
| Stamina | `2 + 0.16E` armour | 2 | 10 | 18 |
| Stamina | `0.05E` armour toughness | 0 | 2.5 | 5 |
| Speed | `0.75 + 0.005E` movement multiplier | 0.75x | 1x | 1.25x |
| Speed | `24 - 0.12E` attack interval, nearest whole tick | 24 | 18 | 12 |
| Intelligence | `0.6 + 0.009E` training multiplier | 0.6x | 1.05x | 1.5x |
| Luck | `2% + 0.18%E` melee critical chance | 2% | 11% | 20% |

Attack interval is clamped to at least one tick when temporary effects push E
above 100. Critical chance is clamped to 100%; a successful critical hit uses
vanilla's 1.5x damage convention and emits the ordinary critical particles and
sound. Intelligence already exposes one shared training-multiplier entry point;
the future training mechanic must call it instead of reading raw NBT.

### Career-outfit combat

Every tamed cat wearing a career outfit participates when its owner attacks or
is attacked, or when the cat itself is attacked. An ordered-sitting cat, a cat
seated on a Create seat, and a cat pancake do not fight. Active combat retains
the 32-block owner leash and never selects a player or another cat as a target.

Melee career cats use the ordinary cat attack attribute, so Combat Power
controls damage and Speed controls the attack interval through the formulas
above. Fishing cats are ranged fighters and cast vanilla-style bobbers with a
curved line connected to the cat, using the same two attributes. A hook pulls
its victim toward the cat when the victim
implements vanilla's ranged-attacker interface or is below half maximum health;
otherwise it knocks the victim away.

Fishing-cat positioning is divided by effective Intelligence: `0..39` closes
to short range, `40..79` maintains a middle-distance firing band, and `80+`
maintains long range, circles the target and leads moving targets. A melee cat
with effective Intelligence `60+` also intercepts a fresh attacker of a nearby
same-owner fishing cat. These are effective-value checks, so traits may change
the chosen behaviour without modifying inherited values.

All career outfits supply `+10` maximum health, `+4` armour and `+2` armour
toughness. The Fishing Suit additionally supplies `+10` effective Luck. Fishing
loot uses the vanilla fishing loot table with `3E/100` loot luck (clamped to
`0..5`), so effective Luck 100 is equivalent to Luck of the Sea III and values
above the normal training ceiling can continue toward V.

The deterministic attribute modifiers use stable UUIDs and coexist additively
with career-outfit modifiers. They are refreshed only on a profile/trait change
and once per second for time, wetness, sitting, health, outfit and owner-health
transitions; those checks only read the cat and its direct owner reference and
perform no entity or world scan.

## Traits V1

Traits are stored independently from the texture genome and the six raw
attributes in a versioned `LaoWuCatTraits` compound. Living cats and cat
pancakes use the same representation:

```text
LaoWuCatTraits
├─ Version: 1
└─ Traits
   ├─ { Id: "laowu:night_owl", Level: 1 }
   └─ { Id: "laowu:heat_resistance", Level: 1 }
```

A profile contains zero to four unique traits. Newly generated traits always
start at level I; the later level-up system must be an explicit progression
mechanic and must not silently reroll existing cats. The initial founder count
distribution is 35% / 40% / 18% / 6% / 1% for zero through four traits. Rarity
selection weights are Defect 30, Common 35, Good 30 and Excellent 5. These
values are centralized so later balancing does not require a save migration.

Cat Dough filling always adds the level-I Defect trait `Doughy`, even when the
ordinary trait-count roll would have produced no trait. `Doughy` is explicitly
non-heritable. Ordinary spawning still uses the founder roll; offspring use the
shared trait-inheritance contract documented in the Breeding Box section.

### Implemented traits

| Trait | Rarity | Upgrade | Implemented effect |
| --- | --- | --- | --- |
| Thorn-Wreathed (`荆棘环绕`) | Excellent | I-VII | `25, 28, 30, 33, 35, 38, 40%` retaliation chance; retaliation equals the cat's current melee damage |
| Night Owl (`夜猫子`) | Common | I-VII | At night, Combat Power uses `3, 5, 7, 9, 11, 13, 15`; Speed uses `1, 2, 3, 4, 5, 6, 7`, reduced because one conditional trait grants two advantages |
| Heat Resistance (`耐热性`) | Good | No | Cancels fire- and lava-tagged damage and clears the burning state |
| Doughy (`面团团`) | Defect | No | All six effective current attributes -20; this trait is never inherited |

The first three complete single-attribute batches are implemented as follows.
Every value is added to the effective current attribute `E`, never to the saved
raw gene or Attribute Limit. All eighteen traits are upgradeable from I to VII.

| Attribute | Common trait | Good trait | Excellent trait |
| --- | --- | --- | --- |
| Combat Power | Brute Force (`力大砖飞`) | Heavy Hitter (`势大力沉`) | Might Over All (`一力破万法`) |
| Health | Tanky (`肉盾`) | Vigorous (`生生不息`) | Oceanic Vitality (`气血如海`) |
| Speed | Fleet-Footed (`飞毛腿`) | Gale Stride (`风驰电掣`) | Lightning Step (`追风逐电`) |
| Stamina | Steel Frame (`钢筋铁骨`) | Iron Body (`铜皮铁骨`) | Adamant Body (`金刚不坏`) |
| Intelligence | Quick-Witted (`灵光一闪`) | Brilliant Mind (`才思敏捷`) | Supreme Intellect (`慧极通天`) |
| Luck | Lucky Cat (`招财猫`) | Great Fortune (`鸿运当头`) | Chosen by Fate (`天命所归`) |

| Rarity | Level I..VII effective-attribute bonus |
| --- | --- |
| Common | `4, 6, 8, 10, 12, 14, 16` |
| Good | `15, 18, 20, 23, 25, 28, 30` |
| Excellent | `25, 28, 30, 33, 35, 38, 40` |

The Common curve is the fixed `4 + 2 * (level - 1)` baseline requested for
simple numeric traits. Good and Excellent remain inside their respective
15..30 and 25..40 budgets. Future traits with multiple simultaneous advantages
must use lower per-effect values rather than receiving this full single-stat
budget for every effect.

The first conditional batch follows that same total-budget rule:

| Trait | Rarity | Levels | Implemented effect |
| --- | --- | --- | --- |
| Night Owl (`夜猫子`) | Common | I-VII | At night, Combat Power `3..15` and Speed `1..7` |
| Fur in Force (`毛多势众`) | Good | I-VII | Stamina `12..30`; shearing yields `ceil(level/2)` extra fur; targeting range is reduced by `level` blocks |
| Bristling Rage (`挨打就炸毛`) | Good | I-VII | Accepted damage grants Combat Power `15..30` for 8 seconds; 16-second cooldown |
| Healing Purr (`呼噜疗愈`) | Excellent | I-VII | Health `17..35`; restores `ceil(level/2)` health every 4 seconds |
| Lu Bu Reborn (`吕布在世`) | Excellent | Fixed | Combat Power +20; maximum health doubles while at least three hostile mobs are within 8 blocks |
| BeeBee Gene (`BeeBee基`) | Good | I-VII | Honey-outfit work interval becomes `9..3` seconds |
| Blazing Form (`刚燃形态`) | Good | I-VII | In the fire outfit, Combat Power `3..21`; every 10 seconds has an `8..26%` chance to superheat adjacent burners for 5 seconds |
| Prosperous Litter (`猫丁兴旺`) | Good | I-VII | Each parent reduces Breeding Box time by 5 seconds per level; both parents stack, with a 20-second floor |

The second conditional and trade-off batch adds nine more traits:

| Trait | Rarity | Levels | Implemented effect |
| --- | --- | --- | --- |
| Angler's Fortune (`渔运亨通`) | Good | I-VII | While wearing the Fishing Suit, Luck `15..30`; the same effective Luck is supplied to the vanilla fishing loot table |
| Superheat Gene (`超燃基因`) | Excellent | Fixed | A seated Fire-Suit cat continuously keeps adjacent Blaze Burners superheated; mutually exclusive with Blazing Form |
| Protective Instinct (`护主心切`) | Good | I-VII | Combat Power `15..30` while the living owner is at or below half health |
| Wet-Fur Fury (`湿毛暴怒`) | Good | I-VII | While wet, Combat Power `18..36` and Speed `-6..-18` |
| Chonky Presence (`橘势膨胀`) | Good | I-VII | Health `18..42`, Stamina `12..30`, Speed `-8..-20` |
| Glass Claws (`玻璃爪`) | Good | I-VII | Combat Power `20..44`, Health `-10..-28` |
| Tail Held High (`尾巴翘上天`) | Common | I-VII | At full health, Speed `3..15` and Luck `1..7` |
| Loaf Thoughts (`香箱思考`) | Common | I-VII | While sitting or riding a seat, Intelligence `4..16` and Stamina `2..8` |
| Nine Lives (`九条命`) | Excellent | I-VII | A lethal final hit has a `7..25%` chance to be negated; 180-second saved cooldown |

The third conditional batch fills out the Defect pool and adds three active
utility traits:

| Trait | Rarity | Levels | Implemented effect |
| --- | --- | --- | --- |
| Water-Shy (`怕水`) | Defect | Fixed | While wet, Combat Power Attribute -15 and Speed Attribute -20; conflicts with Wet-Fur Fury |
| Daytime Drowsiness (`见光犯困`) | Defect | Fixed | In daytime skylit dimensions, Speed Attribute -15 and Intelligence Attribute -15 |
| Timid as a Mouse (`胆小如鼠`) | Defect | Fixed | With at least two hostile mobs within 8 blocks, Combat Power Attribute -15 and Speed Attribute +8; conflicts with Lu Bu Reborn |
| Punching Bag (`受气包`) | Good | I-VII | Stamina Attribute `15..30`; hostile mobs that acquire a target prefer an eligible carrier within 16 blocks |
| Mark of Cain (`该隐印记`) | Excellent | I-VII | Luck Attribute `15..30`; ordinary-damage avoidance starts at `3..9%`, gains 1% per 25 effective Luck, and is capped at 15%; conflicts with Nine Lives |
| Energy Recovery (`能量回收`) | Excellent | Fixed | At or below half health, consumes one Cat Pancake from the nine cat-inventory slots to heal 30% maximum health; 60-second saved cooldown |

Wet-Fur Fury, Chonky Presence and Glass Claws deliberately exceed the ordinary
single-stat positive budget because their disadvantages are always evaluated by
the same effective-attribute layer. A player can therefore build a specialised
cat without receiving the upside for free. Superheat Gene and Blazing Form
occupy one fire-career conflict slot, preventing contradictory burner rules on
the same cat. Water-Shy/Wet-Fur Fury, Timid/Lu Bu Reborn and Mark of Cain/Nine
Lives likewise share dedicated conflict slots.

The five-second Basic Box development timer is never increased to the normal
20-second floor. Searchable descriptions use one canonical numeric style such
as `生命+5` and never alternate between synonyms for addition.

All non-probability progression uses whole numbers. Percentage-point growth is
reserved for an effect that is itself a probability, such as Thorn-Wreathed.
Night Owl's displayed bonus and entity modifiers are removed during daytime
and while the cat is in pancake form. Combat Power and Speed are added to `E` before
the shared formulas above are evaluated; the trait no longer installs a second,
independent damage or movement modifier. Client panels and server effects share
the same explicit night interval (`13000..22999` ticks), so the displayed value
and the live modifier cannot disagree around dusk or dawn.

### Conflict and runtime rules

Each trait declares the exclusive effect slots it occupies. Appearance and
behaviour slots reserve the previously designed visual/AI channels. Each of the
six attributes also has a dedicated numeric-bonus slot, so a cat cannot stack a
Common, Good and Excellent bonus for the same attribute. Bonuses for different
attributes remain compatible, subject to the global four-trait limit. The
profile validator also rejects future combinations containing two appearance
traits, two behaviour traits, or a combined appearance-and-behaviour trait plus
either category.

Trait generation selects a rarity first and then selects uniformly from the
compatible traits in that rarity. Adding more definitions to one rarity
therefore does not silently increase that rarity's overall generation chance.

Generation and all damage decisions are server authoritative. Profiles are
persisted once, copied through pancake capture/restoration, and synchronized in
a compact tracking packet. The shared attribute layer refreshes at most once
per second. Heat Resistance, Thorn-Wreathed, Bristling Rage and Luck criticals
use Forge damage events; Nine Lives checks only accepted lethal damage and saves
its cooldown in the cat's persistent data. Fur uses Forge's shearable pipeline,
and career/breeding traits reuse their existing machine events. Angler's Fortune
feeds the existing fishing loot context, and both fire traits reuse the existing
burner work pass. Lu Bu Reborn and Timid share one bounded 8-block hostile query
per second when either is present. Punching Bag redirects only an existing
hostile target-change event and performs no manual tick polling. Transient
states are networked only when they change.

## Goggles presentation

Looking directly at a cat or a dropped cat-pancake item while wearing Engineer
Cat Goggles opens only the supplied pixel panel, vertically centred beside the
crosshair. Create's ordinary Engineer's Goggles deliberately do not unlock
this cat-specific panel. The complete panel is always 71x72 pixels so both
columns and their tier markers remain visible. There is no text tooltip or
separate floating item icon. It displays the six supplied 8x8 attribute icons
in the order above. Its 5x7 number glyphs overlap by one pixel so adjacent
digits form the connected style authored in the panel.

The `NOW` column displays current values and the `MAX` column simultaneously
displays all six Attribute Limits; neither living cats, inventory pancakes nor
dropped pancake entities require Shift. Both columns have their own six-pixel
tier marker. Each marker uses the value
from its own column and the supplied seven-colour atlas: black for abnormal
data, then yellow, green, cyan, brown, pink and magenta for `0..19`, `20..39`,
`40..59`, `60..79`, `80..99` and `100+` respectively.

The overlay reads already-synchronised client NBT. It does not query the server
each frame. A single compact packet is sent when a player starts tracking a cat
and whenever the debug fusion creates or updates one.

## Cat profile screen

The Cat Scanner is the sole ordinary entry point for this screen. Its held
model switches from the supplied inactive texture to the active texture while
the local crosshair is directly over a cat, and using it on that cat opens the
profile. This visual predicate is client-render-only and adds no entity tick or
server polling. While active, the complete current/limit attribute panel is
rendered as full-bright textured quads directly on the supplied model's top
screen; it reuses the same synced profile data and pixel atlases as the GUI.
Sneak-using an owned Transport cat with an empty hand is again
reserved for its logistics screen, whether or not it is currently on a Seat.

The V key opens a client-only `0..200%` volume slider for multi-cat hissing
sessions. It is saved as `hissing_pair_volume` in `laowu-client.toml`, updates
active tickable clips immediately, and does not alter logistics sounds or the
player's global Hostile Creatures volume.

Using the scanner opens the server-backed Cat Profile for a living cat,
including its living pancake state. Flight cats retain their specialised
inventory through sneak-interaction while holding an item.
The supplied empty 218x209 background is drawn independently from the supplied
component atlas: no demonstration stats, traits or items are baked into the
runtime screen.

The profile renders the live cat model, the shared six-attribute panel and the
same shared 72x27 trait-card renderer used by Breeding Boxes. Both current
values and Attribute Limits are always visible in this full profile view; the
current-number glyphs begin two visible pixels after their attribute icon. Four
accessory compatibility slots and nine general cat
inventory slots currently accept arbitrary items without applying effects. A
normal player inventory is appended below the authored cat panel. All thirteen
cat slots are immediately persisted in `LaoWuProfileItems`, including their
exact logical slot indices, and are copied naturally by the existing full cat
snapshot when the cat becomes a pancake.

Opening this screen installs a server-side, reference-counted movement lock on
the viewed cat. Autonomous and mod-driven horizontal movement pause until the
last viewer closes the screen, then the cat's previous `NoAI` state is restored.
A persisted recovery marker prevents an interrupted server session from leaving
the cat permanently frozen. The Attribute and Trait Adjustment menus reuse the
same lock when their target is a living cat; dropped pancake targets remain
unaffected.

The 218-pixel left main panel is the horizontal screen anchor. Create's
176-pixel player inventory is centred directly beneath that panel, while the
right-hand accessory and cat-inventory extension does not participate in the
centering calculation.

Concurrent menus for the same cat share one live server container instead of
loading independent NBT snapshots. The server cache is keyed by the cat UUID
and holds weak container values; the client always uses an isolated prediction
container populated by menu synchronisation. This separation is required
because Minecraft considers client and server entities with the same runtime
integer id equal. Stored items drop under the ordinary mob-loot
game rule when the cat dies. The bottom text field commits a literal custom cat
name on close through a range- and active-menu-validated server packet; clearing
the field removes the custom name. Text begins ten pixels inside the authored
nameplate, while the complete plate remains the focus
target; clicking anywhere outside it removes keyboard focus without closing the
profile.

## Breeding boxes

The three breeding boxes are persistent four-slot block entities. They never
scan the world or poll neighbouring inventories. Forge sided item capabilities
expose exactly one logical slot per face:

- Front: kitten-pancake output only.
- Visual front-left: adult father pancake input/output.
- Visual front-right: adult mother pancake input/output.
- Back, top and bottom: dedicated breeding-food input/output.

With both adult parents present, at least one breeding food available and an
empty output slot, the server advances a saved progress counter. During this
development iteration the Basic box takes five seconds for rapid testing;
Intermediate and Advanced boxes retain two and one real-time minutes.
Completion consumes one breeding food and writes one baby cat pancake into the
output slot; parents are retained for later cycles.

Ordinary Cat Food is exclusively a kitten-growth item and is rejected by every
breeding box. Boxes accept nine dedicated breeding foods:

| Food | Numeric inheritance |
| --- | --- |
| Breeding Cat Food | Three inherited loci and three fresh loci |
| Super Breeding Cat Food | Five inherited loci and one fresh locus |
| Mutation Cat Food | Two inherited loci, four fresh loci, and mutation chance +20% |
| One of six stat foods | The better targeted parent locus plus four randomly inherited loci |

Targeted food compares the selected Attribute Limit first, breaking ties with
current value. If both parent limits are at least 90 and differ by at most five,
the offspring receives `min(100, better limit + 1)` for that target. Its
inherited current value advances by the same one-point breakthrough, capped by
the new limit, so the improvement is also visible in the ordinary `NOW` panel.

Targeted food intentionally lets one high-value donor propagate that locus to a
second breeding parent. This makes establishing a single specialised 100-point
line approachable. It does not trivialise a six-perfect-limit cat: every food
still leaves at least one of the six loci uninherited, so the final missing
perfect locus must be rolled fresh and then consolidated in later generations.

The machine base chances are 10%, 20% and 30%. Mutation Cat Food adds 20% to
the displayed base value. Parent Luck then adjusts the
food-modified chance as follows:

```text
average luck = (father current Luck + mother current Luck) / 2
effective mutation = (box chance + food bonus) * (1 + average luck / 300)
```

The effective server value is synchronised into the pixel GUI and is used for
both uninherited numeric loci and appearance regions. Appearance regions are
otherwise selected independently from either parent's saved genome. The four

Trait inheritance uses a separate, deliberately visible rule:

1. A trait carried by both parents is guaranteed to appear on the offspring.
2. A trait carried by exactly one parent independently has a 50% inheritance
   chance.
3. `Doughy` is skipped in both cases and every inherited trait resets to level I.
4. If the displayed mutation check succeeds, one slot is reserved for one
   compatible trait absent from both parents; its rarity uses the ordinary
   Defect/Common/Good/Excellent weights.
5. Shared traits are resolved first, then one-parent traits in shuffled order,
   then mutation. The four-trait limit and every conflict slot are always
   enforced. Four traits shared by both parents leave no room for mutation.

This makes a selected trait player-controllable without another item: put the
same desired trait on both parents for guaranteed inheritance. A single donor
is a 50% propagation attempt; after obtaining a second carrier, breeding those
two carriers locks the trait into that lineage. Natural breeding uses the same
rules with a 5% trait-mutation chance. Breeding Boxes and the Breeding Wand use
their displayed effective mutation chance.

The four
trait rows render each saved trait with its rarity frame and a single vertically
centred title. Each complete frame, including its upper-left level badge, is
72x27 pixels. Empty rows are not rendered; summaries are intentionally omitted
because the 72x27 pixel card
cannot present a second Minecraft-font line cleanly. Hovering a row expands a
tooltip with rarity, current level, full effect and the next-level values. Fixed
positive traits use the level-VII badge, while a fixed Defect retains the black
level-I badge.

The runtime atlas stores a complete 72x27 card for every rarity/level pair. It
does not layer a loose 6x6 badge over an unrelated cropped frame: the supplied
right-side complete cards and complete level-replacement cards define the exact
one-pixel badge offset, checker pattern and transparent edge pixels. On the
breeding panel the card origins are `(10,57)` and `(87,57)`, with 28 pixels
between rows.

The development-only Trait Adjustment Wand opens a server-authoritative editor
for a living cat, a living pancake cat, or a dropped pancake item. The left pane
manages the target's four installed slots with minus/plus controls. The
scrollable right pane lists every registered trait, ordered by rarity, and lets
the tester add any compatible selection. Its search field matches translated
titles and every level's full description. Holding Shift removes an installed
trait immediately or raises an upgradeable trait to level VII. Invalid add
buttons are disabled client-side, while the server repeats the same conflict
and four-slot validation before changing saved data.

The breeding screen is a read-only automation monitor: it has no player
inventory and exposes no clickable machine slots. Parents, breeding food and the
result can only enter or leave through the sided item capabilities described
above or through the block's explicit hand interactions: normal use inserts
adult pancakes into father then mother or inserts breeding food; sneaking use
extracts child, food, mother and father in that priority order. Other held items
and an empty hand open the monitor. It renders both parent cats from their
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

## Behaviour-trait channels

Behaviour traits are split into narrow conflict channels instead of occupying
one global behaviour slot. A cat can therefore combine passive, environmental,
audio and gift traits, while incompatible target selectors cannot fight over
the same navigation or attack state.

- Hissing: `Good Cat` and `Select Elder` are mutually exclusive.
- Movement target: `Low-Level Code`, `Shedding`, `So Warm`, `Hunter Kimi`,
  `Sky Cat`, `Auto-Attach`, and `Three-Legged Cat` are mutually exclusive.
- Combat target: `Stitch`, `Edward`, `Food Guarding`, `Meddlesome`, `Filicide`,
  and `Hunter Kimi` are mutually exclusive.
- Reproduction: `Cuddles Only` blocks both natural breeding and Breeding Box
  parent insertion.
- Appearance: `Rolling Log` uses the appearance conflict channel.
- Fire career: `High-Explosive Fuel` shares the Fire-career channel with other
  exclusive Fire Suit traits.

The remaining rules compose: Anorexia blocks all hand/deployer food handling;
Ding-Dong Cat leaves snow; Cable Biter and TOM the Lumberjack respect
`mobGriefing`; Air-Raid Siren multiplies only its own hissing session; A Little
Ill zeroes both current attributes and limits before a death pancake is saved;
Xiaoting rolls its registry-backed smithing-template reward independently on
death and after a vanilla morning gift. Death rewards are spawned directly by
the authoritative server instead of being appended to the mutable living-drop
collection. Doraemon appends its own independent morning-gift roll; Cat King
playback is client-side, positional, and limited to the nearest three sessions.

World searches are never performed every tick. Minecarts are queried every ten
ticks, running belts every forty, nearby heat every eighty, containers every
eighty, and chewable redstone every sixty. Searches are staggered by entity id
and cache their selected entity or block position between passes. Player attack
auto-attachment and potion splashes are event-driven. Server-side persistent
tags hold timers and targets, while the Cat King audio manager is the only
client-only part.

## Multiplayer and performance rules

- The server is authoritative for first generation, inheritance and mutation.
- Generated profiles are saved; reconnecting never rerolls a cat.
- No per-tick world or entity scan is used by genetics or the overlay. Lu Bu
  cats alone perform their required bounded hostile query once per second.
- Cat pancakes copy and restore both genetics compounds.
- Network payloads contain only small versioned NBT compounds.
- Future custom materials should be registered by resource location in a data
  registry; saved cats must never depend on an array index or load order.

## Next integration points

- Add a data-driven material registry with weights, eligibility and mutation
  rules for materials such as obsidian, rainbow and andesite-alloy cats.
- Define training/growth sources that raise current values without exceeding
  the Attribute Limit.
- Route the future training/growth implementation through Intelligence's shared
  training multiplier.
