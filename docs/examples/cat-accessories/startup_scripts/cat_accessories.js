// Copy to kubejs/startup_scripts/cat_accessories.js on BOTH client and server. Restart to register new items.
StartupEvents.registry('item', event => {
  event.create('storm_cat_charm')
    .displayName('风暴猫符')
    .maxStackSize(1)
    .texture('minecraft:item/prismarine_shard')
})
