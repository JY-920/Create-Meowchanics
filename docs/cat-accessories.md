# 猫咪饰品（未发布：2.1.4-dev.accessories.26）

此功能只在 develop 开发。公开版本仍为 2.1.4，不把内部测试编号当作新正式版；见 [版本编号规则](versioning.md)。

复杂机制现已提供真正的 KubeJS 事件与状态操作，见 [事件脚本接口 v3](cat-accessory-scripting.md)。原数据格式 schema_version: 1 继续兼容。36 件完整替换示例见 [示例包](examples/cat-accessories-36/README.md)：7 件事件脚本，29 件脚本生成数据定义并复用引擎。

## 使用

用猫咪扫描器打开猫咪面板，右上四格为饰品槽，每格一件。Shift 点击饰品优先装备，其他物品仍进右下九格背包；取下立即撤销加成。所有已定义饰品统一显示 **[猫咪饰品]**，包括 KubeJS 注册的物品。

26 件内置饰品位于独立的「猫咪饰品」创造栏，图标为铃铛，不再混入「猫咪养成」。物品提示顺序为：名称 → 效果说明（含哈气值、职业条件）→ [猫咪饰品] → 其他既有信息；不会将饰品效果追加到物品 ID、NBT 或标签之后。第三方/KubeJS 物品保留其原有创造栏安排。

同名饰品只生效一件。铃铛与羊毛毡互斥。不同饰品的属性加成相加，同一种机制（概率、范围、免疫等）取最高值，不叠加成倍触发。职业专属饰品可以装备，但只有对应职业生效。

饰品不改变基因或训练上限、不参与遗传。转换猫饼、收纳与重新放出沿用完整实体装备存储；已驯养猫咪即使没有职业，死亡也变为猫饼并保留物品栏和饰品，默认随机一项基础属性扣20；可在世界设置中关闭或调整[死亡属性扣除](cat-death-penalty-config.md)。蟑螂卵鞘触发的分裂例外：以两只幼猫替代猫饼，原物品按规则掉落一次，不复制装备。未驯养且无职业的猫仍沿用原有物品掉落规则；死亡掉落继续遵守 doMobLoot。NeoForge 旧库存从无槽号的压缩列表迁移为明确槽号，旧物品保留。

## 当前 26 件（含 Boss 掉落）

原有 16 件如下；本次新增 10 件职业饰品、三件旧饰品的改动见 [完整职业效果表](career-accessories-v26.md) 和 [图标绘制清单](../art/career-accessories-v1/README.md)。

| 饰品 | 效果 | 物品 ID |
| --- | --- | --- |
| 铃铛 | 更容易吸引附近作战敌人的仇恨 | laowu:cat_taunt_bell |
| 羊毛毡 | 更不容易被优先攻击，不免除直接反击 | laowu:cat_silent_bell |
| 船锚玩具 | 免疫普通击退、钓鱼钩推退和本模组自爆推力；不是无敌 | laowu:cat_stability_anchor |
| 隔热手套 | 免疫燃烧、岩浆及火焰标签伤害 | laowu:cat_fire_charm |
| 弹簧玩具 | 远程职业弹幕恢复正常击退；钓鱼钩原有推退不改变 | laowu:cat_impact_core |
| 齿轮玩具 | 激光猫专属：战斗力 −20；命中后 25% 概率追加一次单体攻击，不连锁 | laowu:cat_followup_gear |
| 磁铁 | 吸引 3 格内掉落物，收入九格猫咪背包 | laowu:cat_loot_magnet |
| 羽毛玩具 | 飞行员：战斗力 −20；闪避率 = 速度属性 × 0.15%，上限 80% | laowu:cat_ace_feather |
| 自爆按钮 | 雷管猫：生命 −20；自爆伤害由 5 倍提高为 10 倍 | laowu:cat_blast_fuse |
| 生命挂饰 | 生命属性+10 | laowu:cat_health_badge |
| 力量挂饰 | 战斗力属性+10 | laowu:cat_attack_badge |
| 迅捷挂饰 | 速度属性+10 | laowu:cat_speed_badge |
| 耐力挂饰 | 耐力属性+10 | laowu:cat_stamina_badge |
| 智力挂饰 | 智力属性+10 | laowu:cat_intelligence_badge |
| 幸运挂饰 | 幸运属性+10 | laowu:cat_luck_badge |
| 黄油块 | 速度属性+10；水平移动时受到的生物攻击伤害降低25% | laowu:cat_butter_cube |

图标已替换为用户 2026-09-14 再次提供的 16 张 16×16 PNG，按原始文件导入双端，未缩放、改色或重采样；[当前重绘素材映射](../art/accessories-redrawn-v2/README.md)记录文件对应关系与校验值。当前 36 件饰品均无制作配方，来源为 Boss 或心愿领养箱；Boss 专属黄油块不进入心愿箱奖励池。改名不改变注册 ID。黄油块为史诗品质，黄油猫独立额外有 25% 概率掉落 1 件，无合成配方，不影响原有 1～3 件超级罐头/超级鱼干奖励。

### 机制边界

- 黄油块：水平速度超过 0.01 格/tick 才触发；坐下、乘坐、猫饼状态不触发。仅降低由生物造成的攻击，不降低虚空、强制处死、环境和反伤伤害；脱下立即撤销。相同减伤机制取最高值，不累计。
- 仇恨不是强制嘲讽。只调整已经在战斗的生物对当前目标及其附近友方猫咪的选择，搜索半径 8 格、要求视线，继续遵守主人和项圈团队规则。按距离平方除以权重比较，铃铛权重 4，普通 1，羊毛毡 0.25。没有其他合适目标时，羊毛毡不提供隐身或免战。
- 追击在 2 tick 后结算，按猫咪当前职业攻击伤害发动一次单体命中，可以暴击；不额外生成范围爆炸，不再次触发追击。每只猫每个攻击 tick 最多抽取一次；目标死亡、换维度、超出 32 格或失去视线则取消。反伤和雷管猫的最后自爆不触发。
- 羽毛玩具：速度 50/100/150 对应 7.5%/15%/22.5% 闪避；只判定生物攻击，不拦截虚空、强制处死和反伤。
- 吸取尊重拾取延迟、专属拾取 UUID 和遮挡；背包满时不吸取，不接触饰品槽。每 0.1 秒至多处理 32 个附近掉落物，最高吸附速度 0.65 格/tick。
- 属性挂饰参与有效属性、套装公式和属性面板；不直接改写基础属性或上限。无套装猫使用独立的低伤害公式 1 + 0.04 × 有效战斗力。
- 没有新增必装前置、Mixin 或 Photon/LDLib 依赖。首批内置饰品不消耗哈气；自定义饰品可配置哈气值，由脚本控制消耗和补充。暂不含饰品外观模型。

## KubeJS / 数据包接入（v1）

建议使用自己的命名空间和固定 ID。模组更新只更换模组 JAR，不覆盖玩家的 kubejs、数据包和脚本。

### 1. 注册物品

将 [物品注册示例](examples/cat-accessories/startup_scripts/cat_accessories.js) 放到客户端和服务器的 kubejs/startup_scripts/。新物品需要重启注册，定义效果则支持 /reload。[KubeJS 官方物品注册说明](https://kubejs.com/wiki/tutorials/item-registry)

### 2. 定义效果（三选一）

- 通用方式：将 [JSON 示例](examples/cat-accessories/data/kubejs/cat_accessories/storm_cat_charm.json) 放到 kubejs/data/kubejs/cat_accessories/storm_cat_charm.json；无需调用任何 Java 内部类。普通数据包用 data/kubejs/cat_accessories/storm_cat_charm.json。[KubeJS data 目录](https://kubejs.com/wiki/folder-structure/data)
- Forge 1.20.1：用 [server_scripts 示例](examples/cat-accessories/forge/server_scripts/cat_accessories.js)，通过 ServerEvents.highPriorityData 与 event.addJson 生成同一资源。接口依据 [KubeJS 2001 源码](https://github.com/kube-mods/kubejs/blob/2001/common/src/main/java/dev/latvian/mods/kubejs/script/data/DataPackEventJS.java)。
- NeoForge 1.21.1：用 [server_scripts 示例](examples/cat-accessories/neoforge/server_scripts/cat_accessories.js)，通过 ServerEvents.generateData('after_mods', ...) 与 event.json 生成同一资源。接口依据 [KubeJS 2101 生成阶段](https://github.com/kube-mods/kubejs/blob/2101/src/main/java/dev/latvian/mods/kubejs/script/data/GeneratedDataStage.java)及[资源生成器](https://github.com/kube-mods/kubejs/blob/2101/src/main/java/dev/latvian/mods/kubejs/generator/KubeResourceGenerator.java)。

同一个定义只选择一种来源，不要把两个加载器的 server_scripts 同时安装。服务端自动向客户端同步最终效果定义，客户端不需要额外手写饰品识别标签或重复添加标记描述。

### v1 格式

定义文件名决定饰品定义 ID；item 是已注册物品的 ID。两者可以不同。一个物品只能对应一个定义；修改内置饰品时覆盖其原定义文件。例：

~~~json
{
  "schema_version": 1,
  "item": "kubejs:storm_cat_charm",
  "enabled": true,
  "required_outfit": "any",
  "description": "整合包自定义饰品",
  "effects": {
    "speed": 12,
    "projectile_knockback": 1,
    "extra_strike_chance": 20
  }
}
~~~

可选项：enabled 默认 true；required_outfit 默认 any；description 为额外的一行说明；exclusive_group 为互斥组 ID，如 mypack:mobility。同组只允许装备一件。已有装备中出现重复或冲突时，按槽位顺序只生效第一件，不删物品。

| effects 字段 | 含义 / 有效范围 |
| --- | --- |
| health / attack / speed / stamina / intelligence / luck | 六项属性加成，-300～300 整数 |
| aggro_bias | 仇恨偏置，-16～16；正数更显眼，负数更低调 |
| knockback_resistance / fire_immune / projectile_knockback | 开关，0 或 1 |
| extra_strike_chance | 追击百分比，0～100；20 表示 20% |
| loot_magnet_radius | 吸取半径，0～16 格 |
| pilot_dodge_per_speed | 每点速度提供的闪避百分比，0～10；0.15 表示速度 100 时 15%，最终闪避最高 80% |
| moving_damage_reduction | 移动时生物攻击减伤百分比，0～80；25 表示减伤25% |
| self_destruct_multiplier | 雷管猫自爆倍率，0～100；不会降低其原本的 5 倍 |

required_outfit 支持：any、none、terminator、fishing、flight、fire、honey、transport、dynamite、engineering、medical、music、agent、diving、cockroach。音乐/医疗均已启用辅助战斗及坐垫工作；工程特殊弹药改由复合弹匣解锁。羽翼闪避与自爆倍率机制本身也限定对应职业。

accessories.26 新增可选机制：fishing_pull、honey_patch、enhanced_potions、engineering_special_ammo、healing_smoke、diving_cleanse、cockroach_split 为 0/1 开关；super_flame_multiplier 为喷火伤害倍率；medical_guard 为治疗中减伤百分比（0～80）；music_movement_bonus 为移动加速百分比（0～100）。内置定义的职业限制和数值见 [本轮完整表](career-accessories-v26.md)，旧 schema_version: 1 不变。

最多 256 个定义。非法字段、非法数值、重复物品 ID 会记入日志，不会执行任意代码或导致装备被清空。定义被删除或 enabled=false 时，既有装备仍保留、可以取出，只停用效果。恢复定义后重新生效。保留原 KubeJS 物品注册脚本；若删除整个 KubeJS 或注销物品本身，原版对未知物品的处理不在此数据格式保证范围内。

### 3. 超出内置机制的脚本

可以注册 effects: {} 的饰品，在 KubeJS 自己的战斗、交互或计时事件里处理新机制。使用稳定公开接口：

~~~js
const CatAccessoryApi = Java.loadClass('cn.laowu.mod.api.CatAccessoryApi')
// 在你自己的服务端事件里取得真正的 Cat 实体后：
if (CatAccessoryApi.isEquipped(cat, 'kubejs:storm_cat_charm')) {
  // 自定义行为；请自行限制频率、保护友方并避免伤害递归。
}
CatAccessoryApi.effectValue(cat, 'speed') // 此猫全部有效饰品的速度加成
CatAccessoryApi.equippedItems(cat)        // 四个饰品槽的物品副本，不能修改实时库存
CatAccessoryApi.schemaVersion()          // 1
~~~

公开包通过 kubejs.classfilter.txt 放行，不通过反射访问私有类；依据 [KubeJS 官方接口说明](https://github.com/kube-mods/kubejs#setting-class-filters)。数据协议与公开接口保留 v1；不同 Minecraft 大版本的 KubeJS 自身事件差异仍需采用对应示例。

isEquipped 表示饰品实际装备且定义未禁用；required_outfit 自动约束内置数据效果，自写事件还应自行检查职业和冷却。

## 验证与后续游戏测试

双端 build 自动运行 verifyCatAccessories：23120 项定义、去重、互斥、职业条件、外部 ID、概率、哈气值、冷却与边界检查。另有源码接线、配方、翻译、KubeJS 示例及打包检查；既有套装、繁育和配置回归继续运行。

另有隔离的真实 Minecraft + KubeJS 集成测试，检查回调、伤害、物品状态和 /reload。它不等同于完整整合包的可视化联机验收；游戏里仍需重点查看槽位点击、客户端面板以及多人战斗效果。

## accessories.29 音乐速度属性

内置扩展音响使用新机制 `music_speed_bonus: 20`（0～300 整数）：受音乐增幅的职业猫临时速度属性 +20，同类只取最高值。该属性进入统一六维计算，刷新实体移动属性并同步面板；不写入基础基因，来源卸下饰品/被移除即撤销，停止覆盖后沿用 40 tick 的音乐余效期限。

旧 `music_movement_bonus` 仍为第三方数据包保留百分比语义，但内置饰品不再使用。物品 ID `laowu:cat_rhythm_tambourine` 不变。协议 Forge 35 / NeoForge 28，双端需同时更新。
