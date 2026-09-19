// Install on BOTH client and server. Restart required; keep this ID when updating.
StartupEvents.registry('item', event => {
  event.create('example_cat_charm')
    .displayName('示例猫符')
    .maxStackSize(1)
    .texture('minecraft:item/prismarine_shard')
})
