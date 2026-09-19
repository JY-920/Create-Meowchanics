// Forge 1.20.1 / KubeJS 2001. Server data is synced to clients by Meowchanics.
ServerEvents.highPriorityData(event => {
  event.addJson('kubejs:cat_accessories/storm_cat_charm.json', {
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
