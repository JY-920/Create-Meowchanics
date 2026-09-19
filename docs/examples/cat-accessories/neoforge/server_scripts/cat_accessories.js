// NeoForge 1.21.1 / KubeJS 2101. Use this instead of the Forge server script.
ServerEvents.generateData('after_mods', event => {
  event.json('kubejs:cat_accessories/storm_cat_charm.json', {
  "schema_version": 1,
  "item": "kubejs:storm_cat_charm",
  "charge": { "capacity": 100, "initial": 20 },
  "effects": {
    "speed": 12,
    "projectile_knockback": 1,
    "extra_strike_chance": 20
  }
})
})
