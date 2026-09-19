// Both loaders: put in kubejs/server_scripts/ together with ONE definition source.
// Requires the companion startup item and its accessory JSON. /reload replaces event listeners.
const CatAccessoriesApi = Java.loadClass('cn.laowu.mod.api.CatAccessoryApi')

// A completely scripted mechanic: heal nearby allied cats on a hit, at most once per 10 seconds.
CatAccessoryEvents.afterAttack(event => {
  const ctx = event.context
  const charm = ctx.accessory('kubejs:storm_cat_charm')
  if (!charm || !charm.tryActivate('mypack:healing_wave', 200, 1)) return
  charm.increment('mypack:healing_waves', 1)
  CatAccessoriesApi.nearbyAllies(ctx.cat, 3).forEach(ally => {
    CatAccessoriesApi.heal(ctx.cat, ally, 2)
  })
})

// A conditional effective-stat bonus. Night = attack attribute +8; removed by day or unequipping.
// 'speed', 'health', 'stamina', 'intelligence', 'luck' work the same way.
CatAccessoryEvents.tick(event => {
  const ctx = event.context
  const charm = ctx.accessory('kubejs:storm_cat_charm')
  if (charm) charm.setStatBonus('attack', ctx.cat.level.isNight() ? 8 : 0)
})

// Other entry points (examples; not enabled here):
// CatAccessoryEvents.beforeHurt(event => {
//   const charm = event.context.accessory('kubejs:storm_cat_charm')
//   if (charm && charm.tryActivate('mypack:shield', 600, 1)) event.cancel()
// })
// CatAccessoryEvents.projectile(event => {
//   const ctx = event.context
//   if (ctx.accessory('kubejs:storm_cat_charm') && ctx.outfit === 'terminator') {
//     ctx.setProjectileDamage(ctx.projectileDamage + 2)
//     ctx.scaleProjectileSpeed(1.2)
//   }
// })
// CatAccessoryEvents.beforeExplosion(event => {
//   if (event.context.accessory('kubejs:storm_cat_charm')) event.context.setDamage(40)
// })
//
// Recharge only after YOUR script has actually consumed gas/material:
// const added = CatAccessoriesApi.accessory(cat, 'kubejs:storm_cat_charm').recharge(10)
// recharge() adds up to capacity and returns the actual amount; it does not create or consume fluid.
