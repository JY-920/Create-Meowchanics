// Executed by the real KubeJS runtime, in a disposable GameTest world only.
const probeApi = Java.loadClass('cn.laowu.mod.api.CatAccessoryApi')
function probeCounter(ctx, key) {
  const accessory = ctx.accessory('kubejs:accessory_probe')
  if (accessory) accessory.increment('probe:' + key, 1)
}
CatAccessoryEvents.equip(event => probeCounter(event.context, 'equip'))
CatAccessoryEvents.unequip(event => {
  if (String(event.context.stack.id) === 'kubejs:accessory_probe')
    event.cat.addTag('probe_unequipped')
})
CatAccessoryEvents.tick(event => probeCounter(event.context, 'tick'))
CatAccessoryEvents.beforeAttack(event => {
  if (!event.context.accessory('kubejs:accessory_probe')) return
  probeCounter(event.context, 'before_attack')
  event.context.setDamage(12)
})
CatAccessoryEvents.afterAttack(event => {
  const ctx = event.context
  const acc = ctx.accessory('kubejs:accessory_probe')
  if (!acc) return
  acc.increment('probe:after_attack', 1)
  // Cannot recursively call these handlers, even when vanilla accepts this nested hit.
  if (acc.tryActivate('probe:bonus', 200, 1)) ctx.damageOther(1)
})
CatAccessoryEvents.beforeHurt(event => {
  if (!event.context.accessory('kubejs:accessory_probe')) return
  probeCounter(event.context, 'before_hurt')
  event.context.setDamage(1)
})
CatAccessoryEvents.afterHurt(event => probeCounter(event.context, 'after_hurt'))
CatAccessoryEvents.kill(event => probeCounter(event.context, 'kill'))
CatAccessoryEvents.projectile(event => {
  if (!event.context.accessory('kubejs:accessory_probe')) return
  probeCounter(event.context, 'projectile')
  if (event.context.accessory('kubejs:accessory_probe').text('probe:state') === 'cancel_shot') {
    event.context.cancel()
    return
  }
  event.context.setProjectileDamage(7)
  event.context.scaleProjectileSpeed(0.5)
})
CatAccessoryEvents.beforeExplosion(event => {
  if (!event.context.accessory('kubejs:accessory_probe')) return
  probeCounter(event.context, 'explosion')
  event.context.setDamage(13)
})
