// SDK 1.0.0: requires the stable script API v3 contract. SERVER side only.
// Copying this file WITHOUT the matching data adapter is safe: the runtime marker prevents double effects.
const laowu36Api = typeof laowu36Compat === 'undefined' ? null : laowu36Compat.api

function laowu36Accessory(ctx, id) {
  if (typeof laowu36Compat === 'undefined' || !laowu36Compat.ready) return null
  const worn = ctx.accessory('laowu:' + id)
  return worn && String(worn.script) === 'laowu:examples36' ? worn : null
}
function laowu36Amount(id, effect) {
  return laowuExamples36.values(id)[effect]
}

function laowu36Combo(ctx, bone) {
  const now = ctx.gameTime
  const target = String(ctx.otherId)
  if (bone.text('examples36:target') !== target || now - bone.number('examples36:last_hit') >= 80) {
    bone.setText('examples36:target', target)
    bone.setNumber('examples36:stacks', 0)
    bone.setNumber('examples36:last_layer', -1000000000)
  }
  return now
}

if (typeof laowu36Compat !== 'undefined' && laowu36Compat.compatible) {
// FIRE CHARM and PILOT FEATHER: use the avoidance stage, not a late damage reduction.
// Void, forced death and the dynamite finale are rejected by the engine before this hook.
CatAccessoryEvents.beforeAvoid(event => {
  const ctx = event.context
  if (laowu36Accessory(ctx, 'cat_fire_charm') && ctx.fireDamage) {
    ctx.extinguish()
    event.cancel()
    return
  }
  if (!laowu36Accessory(ctx, 'cat_ace_feather') || String(ctx.outfit) !== 'flight') return
  if (!ctx.hasAttacker() || ctx.selfDamage || ctx.thornsDamage) return
  const chance = Math.max(0, Math.min(0.8, ctx.stat('speed') *
    laowu36Amount('cat_ace_feather', 'pilot_dodge_per_speed') / 100))
  if (ctx.random() < chance) event.cancel()
})
// fastTick is every TEN ticks, matching the native fire cleanup cadence.
CatAccessoryEvents.fastTick(event => {
  if (laowu36Accessory(event.context, 'cat_fire_charm')) event.context.extinguish()
})

// CHEW BONE: five layers, no new layer more often than every 10 ticks; 80-tick idle reset.
// Store state on the live accessory handle. Save/reload and slot changes never edit the cat's genes.
CatAccessoryEvents.equip(event => {
  if (String(event.context.stack.id) !== 'laowu:cat_chew_bone') return
  const bone = laowu36Accessory(event.context, 'cat_chew_bone')
  if (bone) {
    bone.setText('examples36:target', '')
    bone.setNumber('examples36:stacks', 0)
    bone.setNumber('examples36:last_hit', -1000000000)
    bone.setNumber('examples36:last_layer', -1000000000)
  }
})
CatAccessoryEvents.beforeAttack(event => {
  const ctx = event.context
  if (!ctx.other || !laowu36Api.canHarm(ctx.cat, ctx.other)) return
  const bone = laowu36Accessory(ctx, 'cat_chew_bone')
  if (bone) {
    laowu36Combo(ctx, bone)
    ctx.setDamage(ctx.damage * (1 + bone.number('examples36:stacks') *
      laowu36Amount('cat_chew_bone', 'damage_combo') / 100))
  }
  // PLUSH: strictly above 90% health, every eligible hit; no cooldown.
  if (laowu36Accessory(ctx, 'cat_mouse_plush') &&
      ctx.otherHealth > ctx.otherMaxHealth * 0.9) {
    ctx.setDamage(ctx.damage * (1 + laowu36Amount('cat_mouse_plush', 'opening_damage') / 100))
  }
})
CatAccessoryEvents.acceptedAttack(event => {
  const ctx = event.context
  const bone = laowu36Accessory(ctx, 'cat_chew_bone')
  if (!bone || !ctx.other) return
  const now = laowu36Combo(ctx, bone)
  if (now - bone.number('examples36:last_layer') >= 10) {
    bone.setNumber('examples36:stacks', Math.min(5, bone.number('examples36:stacks') + 1))
    bone.setNumber('examples36:last_layer', now)
  }
  bone.setNumber('examples36:last_hit', now)
})

// BUTTER: horizontal movement and attributed living attacks only; no riding/sitting/pancake exploit.
CatAccessoryEvents.beforeHurt(event => {
  const ctx = event.context
  if (!laowu36Accessory(ctx, 'cat_butter_cube')) return
  const cat = ctx.cat
  if (!ctx.hasLivingAttacker() || ctx.selfDamage || ctx.thornsDamage || ctx.bypassDamage ||
      ctx.passenger || ctx.sitting || laowu36Api.isPancake(cat) || laowu36Api.isFinishing(cat) ||
      ctx.horizontalSpeedSquared <= 0.0001) return
  ctx.setDamage(ctx.damage * (1 - Math.max(0, Math.min(80,
    laowu36Amount('cat_butter_cube', 'moving_damage_reduction'))) / 100))
})

// SCARF: modify healing once, after group healing has already selected the strongest source.
CatAccessoryEvents.beforeHeal(event => {
  const ctx = event.context
  if (laowu36Accessory(ctx, 'cat_warm_scarf')) {
    ctx.setAmount(ctx.amount * (1 + laowu36Amount('cat_warm_scarf', 'healing_received') / 100))
  }
})

// COLLAR: actual lost health, not incoming/pre-armor damage. The helper enforces the existing
// 3-block melee range, team protection, shared 20-tick cooldown and non-recursive thorns damage.
CatAccessoryEvents.acceptedHurt(event => {
  const ctx = event.context
  if (laowu36Accessory(ctx, 'cat_spiked_collar')) {
    laowu36Api.reflectDamage(ctx.cat, ctx.source,
      ctx.damage * laowu36Amount('cat_spiked_collar', 'melee_reflect') / 100, 20)
  }
})

// The data adapters replace definitions only after ALL registrations above succeeded.
laowu36Compat.ready = true
}
