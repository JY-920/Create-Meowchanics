# 36 件猫咪饰品：KubeJS 可运行替换示例

适用模组 **2.1.4-dev.accessories.36 及兼容的 API v3 版本**。支持 Forge 1.20.1、NeoForge 1.21.1，不是 Curios API 的直接兼容层。

## 实现边界

这套示例覆盖现有全部 36 件，沿用原物品 ID、贴图、原版稀有度、职业限制与互斥关系。不会新增另一批 36 件、改变 Boss 专属奖励池或重新添加合成配方。

- **7 件用事件 JavaScript 实现主要触发逻辑**：隔热手套、羽毛玩具、黄油块、磨牙骨、猫咪玩偶、暖绒围巾、钉刺项圈。
- **其余 29 件由 KubeJS 生成完整饰品数据定义**，调用模组已有机制。弹幕、AI、团队规则、特效、死亡分裂与护盾结算仍由引擎执行，并非纯 JavaScript 重写。
- 事件版保留其属性加减，但移除相同的原生触发 effect，避免双倍闪避、伤害、减伤或治疗。定义带 `script: "laowu:examples36"`，事件只处理标记匹配的装备。
- 不安装示例或 KubeJS，36 件原生饰品仍正常使用。

完整、独立且适合 AI 阅读的最终 SDK 见 [开发包](../../kubejs-sdk/README.md)。

## 安装

先备份已有 kubejs 文件，确认模组为 accessories.36 或更新兼容版本。客户端/服务端按对应版本安装 KubeJS 及其自身前置；猫咪饰品脚本不需要 KubeJS Curios。

从对应压缩包解压至实例根目录，最终应只有这四个文件：

```text
kubejs/server_scripts/
  laowu36_compat.js    # API 契约和整套注册检查，priority 200
  laowu36_catalog.js   # 全部 36 件的数据、参数和说明，priority 100
  laowu36_effects.js   # 七种事件写法，API v3
  laowu36_data.js      # 仅选自己加载器的适配文件
```

手动复制时，先取 common/kubejs，再取 forge/kubejs 或 neoforge/kubejs。**不要同时安装两个版本的 laowu36_data.js。** Forge 使用 highPriorityData，NeoForge 使用 generateData('after_mods')。变量放在共享服务端脚本作用域，不给 NeoForge 禁止写入的 global 赋值。

已有其他脚本覆写同一 ID 时须先合并；不能用两个互相冲突的定义同时控制同一饰品。专用服务器把脚本放在服务端即可；效果定义同步给客户端，客户端仍须安装同版本本模组以提供物品和界面。

第一次建议完整重启；之后仅修改数据/事件可运行 /reload。检查 KubeJS 服务端日志中四个文件全部加载且无 ERROR。例子不注册新物品，所以普通参数修改不需要重做物品注册。

### 撤销

同时移走这四个示例文件并 /reload（也可重启）。内置数据自动恢复，原物品、背包、耐久与存档仍在。磨牙骨写入的命名空间状态不会影响原生版本，示例不写永久动态属性加成。**不要只删事件文件而保留替换数据**，否则被移除原生键的七种机制会缺失。

自动部署只更新本模组 JAR，不把示例擅自装进用户原有 kubejs。实际测试在项目 build 下的隔离世界进行。

## 改参数与编写新饰品

全部数值集中在 catalog 的 definition.effects。例如磨牙骨 damage_combo=4 表示每层 +4%，羽毛玩具 pilot_dodge_per_speed=0.15 表示每点速度 +0.15 个百分点；25% 传入 25，不是 0.25。事件版 scriptDescription 是展示文本，改了数值也要同步改描述。

写新饰品时可参考 catalog 中一项，改为自己的物品 ID，再按 [完整接口文档](../../cat-accessory-scripting.md) 注册物品和定义。普通属性/现有机制只需数据定义；自定义触发再监听 CatAccessoryEvents，并用自己的命名空间保存计数和冷却。不要将原版 Java 方法名称直接照搬到 KubeJS 包装实体上；示例使用跨版本稳定的 ctx 接口。

## 护胸与金基咪

两者原版最大耐久均为 **50**，一次成功触发扣 **1**，冷却 **15 秒（300 tick）**；正常触发 50 次后损坏。最后一次保护仍完整生效，护胸最后的护盾不会因为物品刚损坏而立即消失。未触发、完全取消的伤害、护盾吸收不额外扣耐久。二者继续互斥。旧存档中的物品也使用新最大耐久，既有损耗会保存。

这两种保护保留引擎结算，脚本不重复扣耐久；自定义事件饰品可参考 API 的 handle.damageDurability(amount)。

## 36 件索引

所有 ID 都有 `laowu:` 前缀。下表列出原始定义参数；事件版对应触发键在生成数据时被移走，由效果文件接管。

| 名称 | 物品 ID | 主要实现 | 参数 |
| --- | --- | --- | --- |
| 羽毛玩具 | `cat_ace_feather` | 事件脚本 | `attack=-20`，`pilot_dodge_per_speed=0.15` |
| 力量挂饰 | `cat_attack_badge` | 数据定义＋引擎 | `attack=10` |
| 自爆按钮 | `cat_blast_fuse` | 数据定义＋引擎 | `health=-20`，`self_destruct_multiplier=10` |
| 烈焰蛋糕 | `cat_blue_flame_nozzle` | 数据定义＋引擎 | `stamina=-20`，`super_flame_multiplier=1.5` |
| 黄油块 | `cat_butter_cube` | 事件脚本 | `speed=10`，`moving_damage_reduction=25` |
| 猫薄荷饮 | `cat_catnip_pouch` | 数据定义＋引擎 | `critical_haste=15` |
| 磨牙骨 | `cat_chew_bone` | 事件脚本 | `damage_combo=4` |
| 加急快递 | `cat_concentrated_pouch` | 数据定义＋引擎 | `speed=-30`，`enhanced_potions=1` |
| 软木护胸 | `cat_cork_vest` | 数据定义＋引擎 | `emergency_shield=15` |
| 隔热手套 | `cat_fire_charm` | 事件脚本 | `fire_immune=1` |
| 齿轮玩具 | `cat_followup_gear` | 数据定义＋引擎 | `attack=-20`，`extra_strike_chance=25` |
| 守护绷带 | `cat_guard_bandage` | 数据定义＋引擎 | `intelligence=-10`，`medical_guard=20` |
| 生命挂饰 | `cat_health_badge` | 数据定义＋引擎 | `health=10` |
| 蜂蜜饮 | `cat_honey_stamp` | 数据定义＋引擎 | `attack=-10`，`honey_patch=1` |
| 弹簧玩具 | `cat_impact_core` | 数据定义＋引擎 | `projectile_knockback=1` |
| 智力挂饰 | `cat_intelligence_badge` | 数据定义＋引擎 | `intelligence=10` |
| 磁铁 | `cat_loot_magnet` | 数据定义＋引擎 | `loot_magnet_radius=3` |
| 幸运挂饰 | `cat_luck_badge` | 数据定义＋引擎 | `luck=10` |
| 香草冰淇淋 | `cat_medic_smoke_canister` | 数据定义＋引擎 | `attack=-10`，`healing_smoke=1` |
| 工具箱 | `cat_mixed_magazine` | 数据定义＋引擎 | `speed=-10`，`engineering_special_ammo=1` |
| 猫咪玩偶 | `cat_mouse_plush` | 事件脚本 | `opening_damage=35` |
| 旧饭碗 | `cat_old_food_bowl` | 数据定义＋引擎 | `rest_heal=1` |
| 牛奶饮 | `cat_purifying_filter` | 数据定义＋引擎 | `attack=-10`，`diving_cleanse=1` |
| 爆珠奶茶 | `cat_rebirth_ootheca` | 数据定义＋引擎 | `stamina=-20`，`cockroach_split=1` |
| 鱼钩 | `cat_reel_hook` | 数据定义＋引擎 | `speed=10`，`fishing_pull=1` |
| 扩展音响 | `cat_rhythm_tambourine` | 数据定义＋引擎 | `health=-10`，`music_speed_bonus=20` |
| 金基咪 | `cat_roly_poly` | 数据定义＋引擎 | `heavy_hit_cap=30` |
| 羊毛毡 | `cat_silent_bell` | 数据定义＋引擎 | `aggro_bias=-3` |
| 收纳袋 | `cat_sorting_pouch` | 数据定义＋引擎 | `sample_pickup=1` |
| 迅捷挂饰 | `cat_speed_badge` | 数据定义＋引擎 | `speed=10` |
| 钉刺项圈 | `cat_spiked_collar` | 事件脚本 | `melee_reflect=50` |
| 船锚玩具 | `cat_stability_anchor` | 数据定义＋引擎 | `knockback_resistance=1` |
| 耐力挂饰 | `cat_stamina_badge` | 数据定义＋引擎 | `stamina=10` |
| 铃铛 | `cat_taunt_bell` | 数据定义＋引擎 | `aggro_bias=3` |
| 追踪吊牌 | `cat_tracking_tag` | 数据定义＋引擎 | `owner_attack_bonus=30` |
| 暖绒围巾 | `cat_warm_scarf` | 事件脚本 | `healing_received=20` |

## 验证与复现

测试以真实 Minecraft + KubeJS 加载此处四个脚本，不只是语法检查。catalog 测试逐件核对 36 件的有效职业、互斥与六维加减，并核对事件负责的原生键确实移除。其余 GameTest 验证真实伤害、治疗、反伤、仇恨、击退、掉落物拾取、弹幕、非叠加光环、分裂与存档恢复，以及实际 /reload。

在对应加载器项目目录运行（Forge Java 17，NeoForge Java 21）：

```powershell
.\gradlew.bat build runGameTestServer --init-script ../tests/accessory-gametest.init.gradle -PaccessoryExamples36 -PaccessoryRunId=myfresh36 --no-configuration-cache
```

每次需要干净世界时换一个 accessoryRunId，不会动用户游戏存档。不要同时启动同一个加载器、同一个测试世界。移除 accessoryExamples36 可测试原生定义；额外加 accessoryNoKubeJS 可验证未安装 KubeJS 的原生功能。

最终通过项数、加载器版本、构建 SHA 与已知测试过程见 [本次验证/部署记录](../../deployment-accessories.36.md)。自动测试不等于覆盖所有第三方模组组合；大量改动数值后应再测自己的整合包。
