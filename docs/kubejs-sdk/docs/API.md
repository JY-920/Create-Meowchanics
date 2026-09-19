# 猫咪饰品 KubeJS 事件接口 v3

适用：公开脚本 API v3、schema 1。已验证本体 accessories.37，兼容旧 accessories.36。支持 Forge 1.20.1 / NeoForge 1.21.1；更新不按内部构建号绑定，详见 [兼容约定](04-compatibility.md)。

## 安装与兼容

- 不装 KubeJS：内置饰品和数据包效果正常使用。
- 装对应版本 KubeJS 和它自己的前置后：自动出现 CatAccessoryEvents，无需安装 KubeJS Curios；猫咪使用自己的四格饰品槽。
- 模组的 KubeJS/Rhino 引用仅用于编译，未捆绑，也不是玩家强制前置。桥接类由 KubeJS 读取 kubejs.plugins.txt 后加载。
- 在 kubejs/startup_scripts 注册物品；在数据包或 kubejs/data 定义饰品；在 kubejs/server_scripts 写机制。新注册物品需重启，数据与事件脚本支持 /reload。
- /reload 由 KubeJS 替换监听器，不在每次加载时追加一套 Java 静态回调。玩家脚本和命名空间不放进模组 JAR，升级时保留原目录即可。
- 当前对接 KubeJS 2001.6.5-build.26 / 2101.7.2-build.377。跨 Minecraft/KubeJS 大版本仍可能需要适配原生接口；本页的公开接口尽量保持兼容。

物品与效果注册见 [注册教程](02-registration.md)；完整演示见 [Forge 新饰品](../examples/new-accessory/forge/kubejs/server_scripts/example_cat_charm.js)。对应文件位于包根 examples/new-accessory/；两个加载器适配器二选一。

## 36 件内置饰品的替换示例

见 [36 件安装说明](01-installation.md)。沿用现有物品与贴图，7 件事件脚本 + 29 件脚本数据定义调用原生机制；并非把寻路、职业弹幕和渲染全部改成 JavaScript。数据与事件须成套安装，避免原生与脚本重复生效。

## 事件

写法：CatAccessoryEvents.事件名(event => { const ctx = event.context; ... })。

| 事件 | 时机 | 是否可改或取消 |
| --- | --- | --- |
| equip | 饰品变为有效装备，包括实体载入后的首次检查、职业条件变化 | 否 |
| unequip | 饰品移除、禁用或不再满足职业条件 | 否 |
| tick | 佩戴期间每 20 tick 一次，各猫错开执行 | 否 |
| fastTick | 佩戴期间每 10 tick 一次，各猫错开执行 | 否 |
| beforeAvoid | 受击最早期，先于伤害减免；虚空、强制处死及自爆最后死亡不派发 | event.cancel；不要 setDamage |
| beforeHeal | 猫接受治疗、原生倍率处理后 | ctx.amount / setAmount，或 event.cancel |
| acceptedAttack | 实际损失生命结算后同步；真实成功的猫咪攻击 | 只读 |
| acceptedHurt | 猫实际损失生命结算后同步；包含致命一击 | 只读 |
| beforeAttack | 猫造成伤害，护甲计算前 | ctx.setDamage 或 event.cancel |
| afterAttack | 最终伤害结算后，服务器 tick 末执行 | 否 |
| beforeHurt | 猫受到伤害，护甲计算前 | ctx.setDamage 或 event.cancel |
| afterHurt | 最终伤害结算后，服务器 tick 末执行 | 否 |
| kill | 猫造成的死亡确认后，服务器 tick 末执行 | 否 |
| projectile | 职业弹幕瞄准后、生成到世界前；含物流包裹 | 改弹速/伤害或 event.cancel |
| beforeExplosion | 雷管猫最后自爆前 | 改自爆伤害或 event.cancel |

只为持有有效饰品的猫派发战斗/定时事件。内置去重、互斥、职业限制同样适用于脚本 handle。unequip 事件即使最后一件被取下也会发送。

equip/unequip 的 ctx.slot 为 0–3，ctx.stack 为变化物品的副本；ctx.reason 为 load 或 changed。不要用 equip 无条件送物品或回血，因为猫饼还原、区块重新加载也属于 load。需要一次性效果时，用饰品持久状态记录完成标记。

beforeAttack 修改的值还会经过暴击、护甲等正常流程；不是最终扣血值。afterAttack/afterHurt 统一延后到 tick 末，使 Forge 和 NeoForge 脚本都能观察结算后的生命值。不要在观察事件里尝试 cancel。

取消 projectile 会跳过本次弹幕，但仍使用正常攻击冷却。取消 beforeExplosion 只取消爆炸伤害/特效，不取消最后死亡和猫饼处理。虚空、强制处死以及雷管猫最后死亡不允许通过 beforeHurt 拦截。

## ctx：当前事件

- cat：猫实体；other：攻击目标、伤害来源生物或被击杀者，可能为 null。
- source：伤害来源，非伤害事件为 null。
- outfit：职业 ID；stat('speed') 等：读取有效属性。
- damage：当前事件伤害；setDamage(number)：在可修改的伤害事件中设置。
- projectile：真实弹幕实体；projectileDamage / setProjectileDamage(number)：四种攻击弹幕的伤害接口。物流包裹没有攻击伤害。
- scaleProjectileSpeed(number)：调整已瞄准方向上的速度，0–4 倍。
- accessory('命名空间:物品ID或定义ID')：取得该猫当前有效饰品的 handle，没有则返回 null。
- damageOther(number)、healSelf(number)：有队伍、距离及递归保护的操作。
- stack 返回副本，不能通过修改副本绕过背包保存。

事件对象只在同步回调期间有效。延迟动作应保存实体 UUID，在新的服务端事件中重新找实体和 handle，不要保留旧 event 或实体的永久全局引用。

### 跨版本稳定的上下文读法（v3）

KubeJS 对原版实体做了包装，不能假定 Mojang 的 `cat.level()`、`cat.clearFire()` 等 Java 方法在 JS 中仍原名可用。建议通过以下模组接口读取：

- `ctx.gameTime`：世界总 tick；`ctx.otherId`：对方 UUID 字符串，无对方为 ""。
- `ctx.otherHealth` / `ctx.otherMaxHealth`：对方生命，无生命实体为 0。
- `ctx.fireDamage`、`ctx.thornsDamage`、`ctx.bypassDamage`、`ctx.selfDamage`：伤害类型判断。
- `ctx.hasAttacker()` / `ctx.hasLivingAttacker()`：来源实体是否存在 / 是否为生物。
- `ctx.passenger`、`ctx.sitting`、`ctx.horizontalSpeedSquared`：猫咪运动状态。
- `ctx.random()`：猫自己的随机源，返回 [0, 1)；`ctx.extinguish()`：灭火。
- `ctx.amount` / `ctx.setAmount(value)`：与 damage 接口同源，供 beforeHeal 使用。

`acceptedAttack/acceptedHurt` 的数值是生命实际减少量，不含被护甲、吸收或护盾挡掉的部分；完全取消、完全吸收不会触发。辅助伤害不会递归触发。旧 after 事件仍保留原来的 tick 末观察语义；需要叠层或反伤按“确实扣血”判定时用 accepted 事件。beforeHeal 的 other 为猫自身，source 为 null。

## handle：某一格真实饰品

所有修改只能在服务器线程执行，移除物品后旧 handle 会失效。

| 方法 | 含义 |
| --- | --- |
| number(key)、setNumber(key,value)、increment(key,value) | 数值状态/计数 |
| text(key)、setText(key,value)、removeState(key) | 文字状态/清除指定状态和冷却 |
| cooldownRemaining(key) | 剩余冷却 tick |
| startCooldown(key,ticks) | 设置或清除冷却，0 为清除 |
| tryActivate(key,ticks,chargeCost) | 原子检查并预扣冷却与哈气值；失败不扣除 |
| script | 定义中的可选运行方案标记，缺省为空字符串 |
| maxDurability、durability | 物品原版最大耐久 / 剩余耐久；无耐久为 0 |
| damageDurability(amount) | 对真实装备精确扣耐久，不掷耐久附魔概率；返回 true 表示此次损坏，非“操作成功” |
| charge、chargeCapacity | 当前/最大哈气值 |
| consumeCharge(amount) | 足够时扣除，不足返回 false |
| recharge(amount) | 补充到上限，返回实际补充量，不消耗任何外部资源 |
| setStatBonus(stat,amount)、clearStatBonuses() | 动态六维属性加成，-300～300；参与面板及能力公式 |

key 要带命名空间，例如 mypack:combo；每个饰品每类最多 64 个状态键。数值必须有限，文字最多 512 字符。冷却用世界总运行时间计时，不受 /time set 改昼夜影响，最多 12,096,000 tick（20 TPS 时为 7 天）。卸下时冷却继续走，不会因为换槽重置。

动态属性加成随饰品保存，只在此饰品有效装备时生效；取下、禁用、职业不匹配时自动撤销，不改基础属性或遗传上限。脚本移除后，已经保存的动态加成不会猜测性删除；请先在迁移脚本中 clearStatBonuses()，或禁用该饰品定义。

### 耐久与可选运行标记

软木护胸、金基咪使用原版 50 耐久；每次成功触发扣 1，冷却 300 tick（正常 20 TPS 时 15 秒）。不成功的判定、被其他模组取消的伤害以及单纯消耗护盾不扣耐久。第 50 次仍完整生效，护胸最后的护盾在物品损坏后仍保留剩余 6 秒时效。卸下未损坏的护胸仍移除护盾。旧物品自动获得新的最大耐久，已有损耗正常保存。

这些内置耐久触发由引擎管理；脚本若仍保留 emergency_shield / heavy_hit_cap，不要再调用 damageDurability 重复扣除。自定义物品需要先注册为有耐久的物品，JSON 不会改变原版 Item 的最大耐久；Unbreakable 物品遵守原版不损坏语义。

JSON 可选增加 `"script": "mypack:variant"`，仅是同步给 handle 的标记，不会自动停用原生效果或执行代码。示例只有在标记匹配时执行事件，并在数据中移除该事件负责的原生 effect 键，保证只生效一次。schema_version 仍为 1，旧定义与旧 8 参数 Java 构造方式保留。

## 哈气值

在饰品 JSON 中可选增加：

~~~json
"charge": {
  "capacity": 100,
  "initial": 20
}
~~~

没有该字段就是不使用哈气值。initial 仅用于从未存过哈气值的新物品；0 值会明确保存，不会取下重戴就回满。耗尽时物品不损坏，统一描述显示「哈气值：当前/上限」。

需要消耗的效果由脚本显式调用 consumeCharge 或 tryActivate；不会自动扣除，也不会把其他被动效果一起禁用。补充哈气的来源和兑换比例由整合包决定：先实际消耗流体或材料，再 recharge。该方法不是一条自动生成的注液配方，不能只调用 recharge 就宣称消耗了哈气。

现有 36 件内置饰品不消耗哈气；本包示例猫符也不耗哈气。上面 charge 对象是自定义充能时可选使用的格式。护胸和金基咪消耗的是耐久，不是哈气。

## 通用工具

~~~js
const Api = Java.loadClass('cn.laowu.mod.api.CatAccessoryApi')
~~~

- 原有 schemaVersion、isEquipped、effectValue、equippedItems 保留；apiVersion() 为 3；supportsApi(1/2/3) 为 true，其他当前为 false，旧 accessories.36 没有该方法。
- stat(cat,'attack')、outfit(cat)：属性/职业读取。
- friendly(cat,target)、canHarm(cat,target)：按本模组队伍规则判断。
- nearbyAllies(cat,radius)、nearbyEnemies(cat,radius)：最多 32 格、32 个结果，要求视线。
- damage(cat,target,amount)：遵守同维度、32 格、视线、队伍和原版无敌帧；不会递归触发饰品脚本或追击。
- heal(cat,target,amount)：只能治疗同维度 32 格内存活的友方。
- addEffect(cat,target,'minecraft:regeneration',ticks,amplifier,allies)：按 allies 选择友方或敌方检查，0 为一级药水效果。
- isPancake(cat)、isFinishing(cat)：猫饼及自爆最终死亡状态。
- reflectDamage(cat,source,amount,cooldownTicks)：按实际伤害自行算数值后调用，保留近战 3 格、队伍保护、共享反伤冷却与禁止递归规则；返回是否伤害命中。
- accessory(cat,id)：在自己的其他 KubeJS 服务端事件中取得相同 handle。

可以继续使用 KubeJS/原版的实体操作实现新机制。绕过这些 helper 直接修改实体时，脚本作者需自行保证队伍、线程、冷却和安全性。框架不保证任意第三方脚本不会卡服；不要每 tick 全维度扫描实体。

## 数据保存与保护

- 哈气值、状态、动态加成保存在物品自身 LaoWuAccessory 数据中；Forge 使用物品 NBT，NeoForge 使用 CUSTOM_DATA，不修改其他模组数据。
- 猫本身、猫饼、收纳纸箱使用完整实体库存保存。职业猫及已驯养的无职业猫死亡时不先掉出库存，由死亡猫饼保留；未驯养且无职业的猫沿用物品掉落。分裂成功时仍不额外生成猫饼；遵守 doMobLoot。
- 当前定义删除/禁用不会删除物品。若整个 KubeJS 物品注册被删，原版未知物品处理不在本接口保证范围内。
- 回调引发的新伤害有递归保护，内置追击同样不会无限触发脚本。每 tick 的后置事件队列上限 2048，超额事件不继续排队。
- 新的复杂 AI 或客户端骨骼渲染仍可能需要扩展接口，此版不提供任意渲染注入。

## 验证

SDK 的离线验证见 [验证记录](../verification/RESULTS.md)。以下命令仅供持有完整模组开发工程的维护者使用，不可在解压 SDK 后直接运行。单元与兼容检查随双端 build 执行，真实测试使用隔离的 build/accessory-gametest 目录，不接触玩家存档：

~~~powershell
.\gradlew.bat runGameTestServer --init-script ../tests/accessory-gametest.init.gradle --no-configuration-cache
~~~

测试源码、脚本和临时资源不进入发行 JAR。集成测试包括自定义物品、装备/卸下、动态属性、伤害事件、递归保护、哈气值、实体保存和真实 /reload。
