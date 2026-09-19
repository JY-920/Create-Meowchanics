// priority: 100
// 36 existing items: no new item IDs, textures, recipes, or duplicated Wish rewards.
// This file is deliberately data-driven. Edit an effect amount here, not in Java.
// Seven entries use event scripts; their native effect is removed by register() to prevent double application.
const laowuExamples36 = {
  runtime: 'laowu:examples36',
  entries: [
    {
      "id": "cat_ace_feather",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_ace_feather",
        "required_outfit": "flight",
        "effects": {
          "attack": -20,
          "pilot_dodge_per_speed": 0.15
        }
      },
      "name": "羽毛玩具",
      "scripted": "pilot_dodge_per_speed",
      "scriptDescription": "每点速度属性提供0.15%闪避率，免除一次攻击伤害；最高80%。"
    },
    {
      "id": "cat_attack_badge",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_attack_badge",
        "effects": {
          "attack": 10
        }
      },
      "name": "力量挂饰",
      "scripted": ""
    },
    {
      "id": "cat_blast_fuse",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_blast_fuse",
        "required_outfit": "dynamite",
        "effects": {
          "health": -20,
          "self_destruct_multiplier": 10
        }
      },
      "name": "自爆按钮",
      "scripted": ""
    },
    {
      "id": "cat_blue_flame_nozzle",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_blue_flame_nozzle",
        "required_outfit": "fire",
        "effects": {
          "stamina": -20,
          "super_flame_multiplier": 1.5
        }
      },
      "name": "烈焰蛋糕",
      "scripted": ""
    },
    {
      "id": "cat_butter_cube",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_butter_cube",
        "effects": {
          "speed": 10,
          "moving_damage_reduction": 25
        }
      },
      "name": "黄油块",
      "scripted": "moving_damage_reduction",
      "scriptDescription": "移动时受到的生物攻击伤害降低25%"
    },
    {
      "id": "cat_catnip_pouch",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_catnip_pouch",
        "effects": {
          "critical_haste": 15
        }
      },
      "name": "猫薄荷饮",
      "scripted": ""
    },
    {
      "id": "cat_chew_bone",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_chew_bone",
        "effects": {
          "damage_combo": 4
        }
      },
      "name": "磨牙骨",
      "scripted": "damage_combo",
      "scriptDescription": "连续命中同一目标，每层伤害 +4%，最多 5 层；每 0.5 秒最多一层，换目标或 4 秒未命中清空。"
    },
    {
      "id": "cat_concentrated_pouch",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_concentrated_pouch",
        "required_outfit": "transport",
        "effects": {
          "speed": -30,
          "enhanced_potions": 1
        }
      },
      "name": "加急快递",
      "scripted": ""
    },
    {
      "id": "cat_cork_vest",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_cork_vest",
        "exclusive_group": "laowu:lifeguard",
        "effects": {
          "emergency_shield": 15
        }
      },
      "name": "软木护胸",
      "scripted": ""
    },
    {
      "id": "cat_fire_charm",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_fire_charm",
        "effects": {
          "fire_immune": 1
        }
      },
      "name": "隔热手套",
      "scripted": "fire_immune",
      "scriptDescription": "免疫燃烧和岩浆伤害。"
    },
    {
      "id": "cat_followup_gear",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_followup_gear",
        "effects": {
          "attack": -20,
          "extra_strike_chance": 25
        },
        "required_outfit": "terminator"
      },
      "name": "齿轮玩具",
      "scripted": ""
    },
    {
      "id": "cat_guard_bandage",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_guard_bandage",
        "required_outfit": "medical",
        "effects": {
          "intelligence": -10,
          "medical_guard": 20
        }
      },
      "name": "守护绷带",
      "scripted": ""
    },
    {
      "id": "cat_health_badge",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_health_badge",
        "effects": {
          "health": 10
        }
      },
      "name": "生命挂饰",
      "scripted": ""
    },
    {
      "id": "cat_honey_stamp",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_honey_stamp",
        "required_outfit": "honey",
        "effects": {
          "attack": -10,
          "honey_patch": 1
        }
      },
      "name": "蜂蜜饮",
      "scripted": ""
    },
    {
      "id": "cat_impact_core",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_impact_core",
        "effects": {
          "projectile_knockback": 1
        }
      },
      "name": "弹簧玩具",
      "scripted": ""
    },
    {
      "id": "cat_intelligence_badge",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_intelligence_badge",
        "effects": {
          "intelligence": 10
        }
      },
      "name": "智力挂饰",
      "scripted": ""
    },
    {
      "id": "cat_loot_magnet",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_loot_magnet",
        "effects": {
          "loot_magnet_radius": 3
        }
      },
      "name": "磁铁",
      "scripted": ""
    },
    {
      "id": "cat_luck_badge",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_luck_badge",
        "effects": {
          "luck": 10
        }
      },
      "name": "幸运挂饰",
      "scripted": ""
    },
    {
      "id": "cat_medic_smoke_canister",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_medic_smoke_canister",
        "required_outfit": "agent",
        "effects": {
          "attack": -10,
          "healing_smoke": 1
        }
      },
      "name": "香草冰淇淋",
      "scripted": ""
    },
    {
      "id": "cat_mixed_magazine",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_mixed_magazine",
        "required_outfit": "engineering",
        "effects": {
          "speed": -10,
          "engineering_special_ammo": 1
        }
      },
      "name": "工具箱",
      "scripted": ""
    },
    {
      "id": "cat_mouse_plush",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_mouse_plush",
        "effects": {
          "opening_damage": 35
        }
      },
      "name": "猫咪玩偶",
      "scripted": "opening_damage",
      "scriptDescription": "攻击血量高于90%的敌人，伤害+35%。"
    },
    {
      "id": "cat_old_food_bowl",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_old_food_bowl",
        "effects": {
          "rest_heal": 1
        }
      },
      "name": "旧饭碗",
      "scripted": ""
    },
    {
      "id": "cat_purifying_filter",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_purifying_filter",
        "required_outfit": "diving",
        "effects": {
          "attack": -10,
          "diving_cleanse": 1
        }
      },
      "name": "牛奶饮",
      "scripted": ""
    },
    {
      "id": "cat_rebirth_ootheca",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_rebirth_ootheca",
        "required_outfit": "cockroach",
        "effects": {
          "stamina": -20,
          "cockroach_split": 1
        }
      },
      "name": "爆珠奶茶",
      "scripted": ""
    },
    {
      "id": "cat_reel_hook",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_reel_hook",
        "required_outfit": "fishing",
        "effects": {
          "speed": 10,
          "fishing_pull": 1
        }
      },
      "name": "鱼钩",
      "scripted": ""
    },
    {
      "id": "cat_rhythm_tambourine",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_rhythm_tambourine",
        "required_outfit": "music",
        "effects": {
          "health": -10,
          "music_speed_bonus": 20
        }
      },
      "name": "扩展音响",
      "scripted": ""
    },
    {
      "id": "cat_roly_poly",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_roly_poly",
        "exclusive_group": "laowu:lifeguard",
        "effects": {
          "heavy_hit_cap": 30
        }
      },
      "name": "金基咪",
      "scripted": ""
    },
    {
      "id": "cat_silent_bell",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_silent_bell",
        "exclusive_group": "laowu:threat",
        "effects": {
          "aggro_bias": -3
        }
      },
      "name": "羊毛毡",
      "scripted": ""
    },
    {
      "id": "cat_sorting_pouch",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_sorting_pouch",
        "effects": {
          "sample_pickup": 1
        }
      },
      "name": "收纳袋",
      "scripted": ""
    },
    {
      "id": "cat_speed_badge",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_speed_badge",
        "effects": {
          "speed": 10
        }
      },
      "name": "迅捷挂饰",
      "scripted": ""
    },
    {
      "id": "cat_spiked_collar",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_spiked_collar",
        "effects": {
          "melee_reflect": 50
        }
      },
      "name": "钉刺项圈",
      "scripted": "melee_reflect",
      "scriptDescription": "近身受击后反弹实际伤害的50%。"
    },
    {
      "id": "cat_stability_anchor",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_stability_anchor",
        "effects": {
          "knockback_resistance": 1
        }
      },
      "name": "船锚玩具",
      "scripted": ""
    },
    {
      "id": "cat_stamina_badge",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_stamina_badge",
        "effects": {
          "stamina": 10
        }
      },
      "name": "耐力挂饰",
      "scripted": ""
    },
    {
      "id": "cat_taunt_bell",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_taunt_bell",
        "exclusive_group": "laowu:threat",
        "effects": {
          "aggro_bias": 3
        }
      },
      "name": "铃铛",
      "scripted": ""
    },
    {
      "id": "cat_tracking_tag",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_tracking_tag",
        "effects": {
          "owner_attack_bonus": 30
        }
      },
      "name": "追踪吊牌",
      "scripted": ""
    },
    {
      "id": "cat_warm_scarf",
      "definition": {
        "schema_version": 1,
        "item": "laowu:cat_warm_scarf",
        "effects": {
          "healing_received": 20
        }
      },
      "name": "暖绒围巾",
      "scripted": "healing_received",
      "scriptDescription": "受到的治疗量 +20%，包含医疗猫与治疗烟雾。"
    }
  ],
  values: function(id) {
    for (let i = 0; i < this.entries.length; i++) {
      if (this.entries[i].id === id) return this.entries[i].definition.effects
    }
    throw new Error('Unknown accessory example: ' + id)
  },
  register: function(write) {
    if (typeof laowu36Compat === 'undefined' || !laowu36Compat.ready) {
      console.warn('[laowu36] No overrides emitted: compatibility/effect registration incomplete; native definitions retained')
      return
    }
    this.entries.forEach(entry => {
      const definition = JSON.parse(JSON.stringify(entry.definition))
      definition.script = this.runtime
      if (entry.scripted) {
        delete definition.effects[entry.scripted]
        definition.description = entry.scriptDescription
      }
      write('laowu:cat_accessories/' + entry.id + '.json', definition)
    })
  }
}
