// SDK 1.0.0; server-side only. No internal NBT or mapped Minecraft method names.
const exampleCatApi = Java.loadClass('cn.laowu.mod.api.CatAccessoryApi')
let exampleCatCompatible = false
try { exampleCatCompatible = exampleCatApi.supportsApi(3) }
catch (legacy) { exampleCatCompatible = exampleCatApi.apiVersion() === 3 }
exampleCatCompatible = exampleCatCompatible && exampleCatApi.schemaVersion() === 1
const exampleCatDefinition = {
  schema_version: 1,
  item: 'kubejs:example_cat_charm',
  required_outfit: 'any',
  exclusive_group: 'examplepack:healing_charms',
  script: 'examplepack:heal_on_hit_v1',
  description: '实际命中后回复自身2点生命，冷却10秒。',
  effects: {speed: 10}
}
let exampleCatReady = false
if (exampleCatCompatible) {
  CatAccessoryEvents.acceptedAttack(event => {
    if (!exampleCatReady) return
    const ctx = event.context
    const charm = ctx.accessory('kubejs:example_cat_charm')
    if (!charm || String(charm.script) !== 'examplepack:heal_on_hit_v1') return
    if (charm.tryActivate('examplepack:heal', 200, 0)) ctx.healSelf(2)
  })
  exampleCatReady = true
}
// Data callback runs during resource generation after server scripts have loaded.
ServerEvents.generateData('after_mods', event => {
  if (exampleCatReady) event.json('kubejs:cat_accessories/example_cat_charm.json', exampleCatDefinition)
})
