// Cat trait API v1 / schema 1. Install ONLY the file for your loader in kubejs/server_scripts/.
// Example IDs are permanent save keys. Do not rename IDs after distributing a pack.
// Demonstration traits are NOT naturally generated or mutated until you opt in below.
const exampleTraitApi = Java.loadClass('cn.laowu.mod.api.CatTraitApi')
const exampleTraitsReady = exampleTraitApi.supportsApi(1) && exampleTraitApi.schemaVersion() === 1
const exampleTraitDefinitions = {
  swift_paws: {
    schema_version: 1, title: '轻盈步伐', rarity: 'good', max_level: 3,
    description: ['速度属性+5。', '速度属性+10。', '速度属性+15。'],
    stat_bonuses: { speed: [5, 10, 15] },
    natural: false, mutation: false, inheritable: true
  },
  healing_rhythm: {
    schema_version: 1, title: '自愈节律', rarity: 'excellent', max_level: 3,
    description: ['受伤时每4秒回复1点生命。', '受伤时每4秒回复2点生命。', '受伤时每4秒回复3点生命。'],
    natural: false, mutation: false, inheritable: true
  },
  relentless: {
    schema_version: 1, title: '越战越勇', rarity: 'good', max_level: 3,
    description: ['攻击额外造成1点伤害。', '攻击额外造成2点伤害。', '攻击额外造成3点伤害。'],
    natural: false, mutation: false, inheritable: true
  }
}
if (exampleTraitsReady) {
  CatTraitEvents.tick(event => {
    const ctx = event.context
    const trait = ctx.trait('examplepack:healing_rhythm')
    if (!trait || exampleTraitApi.health(ctx.cat) >= exampleTraitApi.maxHealth(ctx.cat)) return
    // Reserve the cooldown BEFORE the effect. 20 ticks = 1 second.
    if (trait.tryActivate('examplepack:healing', 80)) {
      if (exampleTraitApi.heal(ctx.cat, ctx.cat, trait.level)) {
        trait.increment('examplepack:heals', 1)
      }
    }
  })
  CatTraitEvents.beforeAttack(event => {
    const ctx = event.context
    const trait = ctx.trait('examplepack:relentless')
    if (trait) ctx.setDamage(Math.min(1000000, ctx.damage + trait.level))
  })
  CatTraitEvents.afterAttack(event => {
    const trait = event.context.trait('examplepack:relentless')
    if (trait) trait.increment('examplepack:hits', 1)
  })
}
// Only the data-event spelling differs between Forge 1.20.1 and NeoForge 1.21.1.
ServerEvents.highPriorityData(event => {
  if (!exampleTraitsReady) return
  Object.keys(exampleTraitDefinitions).forEach(name => {
    event.addJson('examplepack:cat_traits/' + name + '.json', exampleTraitDefinitions[name])
  })
})
