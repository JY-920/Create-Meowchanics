// Test-only observers. This file is never included in production jars or the SDK.
const traitProbeApi = Java.loadClass('cn.laowu.mod.api.CatTraitApi')
function traitProbeHandle(ctx) {
  if (!ctx.cat || !ctx.cat.tags.contains('trait_probe')) return null
  return ctx.trait('examplepack:relentless')
}
CatTraitEvents.added(event => {
  const ctx = event.context
  const trait = traitProbeHandle(ctx)
  if (trait) { trait.increment('probe:added', 1); trait.setText('probe:reason', String(ctx.reason)) }
})
CatTraitEvents.removed(event => {
  if (event.cat && event.cat.tags.contains('trait_probe')) event.cat.addTag('trait_probe_removed')
})
CatTraitEvents.levelChanged(event => {
  const trait = traitProbeHandle(event.context)
  if (trait) trait.increment('probe:level_changed', 1)
})
CatTraitEvents.tick(event => {
  const trait = traitProbeHandle(event.context)
  if (trait) trait.increment('probe:tick', 1)
})
CatTraitEvents.fastTick(event => {
  const trait = traitProbeHandle(event.context)
  if (trait) trait.increment('probe:fast_tick', 1)
})
CatTraitEvents.beforeAttack(event => {
  const trait = traitProbeHandle(event.context)
  if (trait) trait.increment('probe:before_attack', 1)
})
CatTraitEvents.afterAttack(event => {
  const ctx = event.context
  const trait = traitProbeHandle(ctx)
  if (!trait) return
  trait.increment('probe:after_attack', 1)
  if (ctx.cat.tags.contains('trait_probe_bonus') && trait.tryActivate('probe:bonus', 200)) {
    if (traitProbeApi.damage(ctx.cat, ctx.other, 100)) trait.increment('probe:bonus_accepted', 1)
  }
})
CatTraitEvents.beforeHurt(event => {
  const trait = traitProbeHandle(event.context)
  if (!trait) return
  trait.increment('probe:before_hurt', 1)
  if (event.cat.tags.contains('trait_probe_cancel')) event.context.cancel()
  else event.context.setDamage(Math.min(1, event.context.damage))
})
CatTraitEvents.afterHurt(event => {
  if (event.cat && event.cat.tags.contains('trait_probe') && traitProbeApi.health(event.cat) <= 0) {
    event.cat.addTag('trait_probe_dead_after_hurt')
  }
  const trait = traitProbeHandle(event.context)
  if (trait) trait.increment('probe:after_hurt', 1)
})
CatTraitEvents.beforeHeal(event => {
  const trait = traitProbeHandle(event.context)
  if (trait) { trait.increment('probe:before_heal', 1); event.context.setAmount(event.context.amount + .5) }
})
CatTraitEvents.death(event => {
  if (event.cat && event.cat.tags.contains('trait_probe')) event.cat.addTag('trait_probe_dead')
})
CatTraitEvents.kill(event => {
  const trait = traitProbeHandle(event.context)
  if (trait) trait.increment('probe:kill', 1)
})
CatTraitEvents.breed(event => {
  const ctx = event.context
  if (ctx.parentLevel(0, 'examplepack:relentless') >= 2 && ctx.parentLevel(1, 'examplepack:relentless') >= 2) {
    ctx.setChildLevel('examplepack:relentless', 2)
  }
})
