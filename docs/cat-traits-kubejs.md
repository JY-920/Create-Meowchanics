# 猫咪词条 KubeJS 开发指南（API v1 / schema 1）

适用：本次新增 CatTraitApi 的老吴学 2.2.0 构建，Forge 1.20.1、NeoForge 1.21.1。旧的同版本号 JAR 不一定包含本接口，务必检查 supportsApi(1)。
这是独立于猫咪饰品 API v1–v3 的新接口；不改动旧饰品脚本、物品 ID 或本体 82 个词条 ID。

## 先给 AI / 整合包作者看的规则

- 注册走服务端数据文件 `data/<namespace>/cat_traits/<path>.json`，不是 Curios，也不是 `StartupEvents.registry('cat_trait')`。
- 编写行为使用 `CatTraitEvents` 和 `Java.loadClass('cn.laowu.mod.api.CatTraitApi')`。只依赖本文公开 API；不要直接操作内部 NBT、反射内部 genetics 类、记住枚举序号。
- Forge 和 NeoForge 只在生成 JSON 的事件入口不同。不要同时安装两套文件；不要用 client_scripts 注册词条。
- 自定义与内置词条共享 4 个位置；自定义等级 1–7。显示名称不是存档 ID。ID 一旦发给玩家就不要重命名。
- 效果回调在服务端主线程执行；不要在线程池、EntityJoin、区块加载回调中扫描世界或强制加载区块。
- 每次事件内重新取得 `context.trait(id)`；不要把 handle/context 缓存到下一 tick、延迟任务或全局变量。
- 固定六维属性加成优先使用 JSON 的 stat_bonuses。动态状态用 handle；先占用冷却，再造成伤害/回血。
- 示例不会覆盖 82 个本体词条；现有内置效果继续执行。新事件可以追加内置词条效果，但本接口不提供“关闭某个内置词条全部原生逻辑”的总开关。
- 改更新不能只看 Java 签名：必须测试实际伤害、回血、冷却、存档和重载。

## 安装与第一次测试

仓库示例位置：`docs/examples/cat-traits-v1/<forge 或 neoforge>/kubejs/server_scripts/cat_traits.js`。
独立示例包内位置：`<forge 或 neoforge>/kubejs/server_scripts/cat_traits.js`。

1. 更新对应加载器的老吴学 JAR，安装整合包对应的 KubeJS。
2. 只复制自己加载器下的 cat_traits.js 到实例/服务器的 `kubejs/server_scripts/`，不要覆盖其他脚本。
3. 启动或运行 `/reload`，检查 `kubejs/logs/server.log` 与游戏日志没有本脚本报错。
4. 用词条调整棒选择“轻盈步伐”“自愈节律”“越战越勇”。它们会出现在词条面板、调整棒、过滤器以及生成黑名单中。
5. 轻盈步伐 I/II/III：速度 +5/+10/+15；自愈节律：受伤时每 4 秒回复 1/2/3 点生命；越战越勇：攻击事件额外 +1/+2/+3 点伤害。
6. 示例默认 natural=false、mutation=false，避免仅安装写法示例就改变自然生成。希望正式投放时，把需要的开关改为 true。

测试过的 KubeJS：Forge 2001.6.5-build.26；NeoForge 2101.7.2-build.377。
词条名称、描述和定义由服务端同步到客户端，不需要再编写一套客户端注册。KubeJS 本身的依赖/安装要求按整合包处理。
本次菜单协议有更新，联机客户端与服务端都应更新本模组。

## 最小注册示例

Forge 1.20.1，放在 server_scripts：

```js
const TraitApi = Java.loadClass('cn.laowu.mod.api.CatTraitApi')
const traitReady = TraitApi.supportsApi(1) && TraitApi.schemaVersion() === 1
ServerEvents.highPriorityData(event => {
  if (!traitReady) return
  event.addJson('mypack:cat_traits/light_step.json', {
    schema_version: 1,
    title: '轻步',
    description: ['速度属性+5。', '速度属性+10。', '速度属性+15。'],
    rarity: 'good',
    max_level: 3,
    natural: true,
    mutation: true,
    inheritable: true,
    stat_bonuses: {speed: [5, 10, 15]}
  })
})
```

对应存档 ID 是 `mypack:light_step`，不是带 cat_traits/ 或 .json 的资源文件路径。
NeoForge 1.21.1 将数据入口替换为：

```js
ServerEvents.generateData('after_mods', event => {
  // 使用同样的 definition 对象
  // event.json('mypack:cat_traits/light_step.json', definition)
})
```

也可不使用 KubeJS 注册：将同一 JSON 放进数据包的 data/mypack/cat_traits/light_step.json。
仅使用 stat_bonuses 的数据包词条无需 KubeJS 执行效果；脚本回调效果需要 KubeJS。

## JSON 字段

| 字段 | 含义 |
| --- | --- |
| schema_version | 必填，整数 1 |
| title | 必填，非空名称，最长 96 字符 |
| rarity | 必填，defect 缺陷 / common 普通 / good 优良 / excellent 卓越 |
| description | 可省略；字符串，或与 max_level 等长的逐级描述数组，每条最多 512 字符 |
| max_level | 默认 1，整数 1–7 |
| enabled | 默认 true；false 暂停该自定义词条的原生属性加成、handle 和自动遗传/生成，保留已有数据 |
| natural | 默认 false；是否进入自然/注入猫咪的候选池 |
| mutation | 默认 false；是否进入繁殖突变池 |
| inheritable | 默认 true；是否允许亲代遗传 |
| weight | 默认 1，整数 1–10000；同稀有度内部的相对权重 |
| stat_bonuses | 六维有效属性加成；整数或与 max_level 等长的整数数组，单项 -999999..999999 |
| slots | 可选，互斥通道数组 |
| conflicts | 可选，不能同时新获得的词条 ID 数组，最多 32 项；单向声明即互斥 |

六维 ID：health 生命、attack 战斗力、speed 速度、stamina 耐力、intelligence 智力、luck 幸运。
这些加成不修改基础属性或上限，也不绕过本体已有的有效属性裁剪/职业公式。

slots 可选值：

```text
appearance, behaviour, hissing_behaviour, movement_behaviour, combat_behaviour,
reproduction_behaviour, attack_bonus, health_bonus, speed_bonus, stamina_bonus,
intelligence_bonus, luck_bonus, fire_career, wet_condition, hostile_pressure, damage_avoidance
```

slots 只声明互斥；写 appearance 不会自动创建模型/动画。没有互斥需求可以不填。
最多 128 个自定义词条；每个规范化 JSON 最多 4096 UTF-8 字节；单个 ID 最多 128 字符。
未知字段、错误类型、无效数组、覆盖内置 ID 会拒绝并记录日志。不要依赖“拼错字段也默认生效”。
建议用自己的 namespace，避免与其他整合包脚本重名。

自然生成仍先按本体规则抽稀有度，再按 weight 在该档内部选词条。
普通遗传仍是双方共有必传、单方拥有独立 50% 概率，子代等级 I，遵守容量、互斥和黑名单；原有“面团”等例外不变。
脚本状态、冷却和动态属性不会自动遗传；只遗传定义允许的词条本身。

## 操作现有词条

以下代码需要放在已经拿到服务端 cat 的事件内，不是可直接在顶层执行的全局变量：

```js
// id 可为自定义 ID，也可为未改名的 laowu: 内置词条 ID。
const ok = TraitApi.setLevel(cat, 'mypack:light_step', 3)
const level = TraitApi.level(cat, 'mypack:light_step')
const stored = TraitApi.storedLevel(cat, 'mypack:light_step')
const ownedIds = TraitApi.ids(cat)
TraitApi.remove(cat, 'mypack:light_step')
```

- setLevel 的等级参数为 0–7，0 表示移除；超过该定义的最大级会夹到最大级。返回是否达到请求结果。
- 添加要遵守 4 词条、互斥、enabled 和生成黑名单。不能用此 API 硬塞第 5 条。
- 黑名单不删除存量词条，已有等级仍可调整。
- level 返回已加载定义下的有效等级，定义缺失时为 0；storedLevel 返回保留的存档等级。
- has 等同于 level > 0，不能用它判断效果是否启用；取得的 trait handle 为 null 才表示当前无法使用该词条的脚本效果。
- registeredIds 返回当前内置与已注册自定义 ID；ids(cat) 还会包含该猫保留的未加载词条。

猫饼：`pancakeIds(stack)`、`pancakeLevel(stack,id)`、`setPancakeLevel(stack,id,level)`。
必须传入真正的猫饼 ItemStack；写入必须在服务端主线程。修改的是传入的实际 stack，调用方负责将改过的 stack 放回原槽位并通知容器更新，不要只改一份临时副本。
面板、调整棒、过滤器均按 ID 处理，不应自行发送词条枚举序号。脚本重载后旧调整窗口会失效，关闭重开即可。

## 效果事件

统一入口 `CatTraitEvents.<事件>(event => { const ctx = event.context })`。
除 breed 外 ctx.cat 为拥有词条的猫，ctx.other / ctx.target 为另一方（可能为空），ctx.source 为伤害来源（可能为空）。

| 事件 | 时机与可修改内容 |
| --- | --- |
| added / removed / levelChanged | 每 10 tick 检查一次差异；ctx.traitId、oldLevel、newLevel、reason |
| tick | 每 20 tick 分散执行一次，适合周期效果 |
| fastTick | 每 10 tick 分散执行一次，慎用范围查询 |
| beforeAttack | 猫直接攻击或其投射物伤害结算前；可 setDamage / setAmount / cancel |
| afterAttack | 有正伤害后，在 tick 末观察；不能修改原攻击 |
| beforeHurt | 猫受伤结算前；可改伤害或取消 |
| afterHurt | 猫受到正伤害后，在 tick 末观察 |
| beforeHeal | 猫回血前，可 setAmount 或取消 |
| kill | 确认其他生物死亡后，攻击方猫的观察事件 |
| death | 确认该猫死亡后观察，不提供撤销死亡/复活接口 |
| breed | 生成子代词条后，修改子代词条；此时 ctx.cat 为 null |

added 的 reason 是 load（首次看到该猫）、changed（拥有情况改变）或 reload（定义重载造成的变化）。
除 death 外，回调派发时要求该猫仍存活；致命伤只交给 death 观察，不再向死猫派发可写状态的 afterHurt 等普通回调。
卸载再载入实体会重新收到 load，不要每次 added 都无条件发放一次性永久奖励；用持久状态去重。
短时间内多次调整可能合并为一次最终差异。removed 不能再取得被删除词条的 handle，要用 ctx.traitId / oldLevel。
冷却/周期以服务器 tick 计时；低 TPS 时现实中的秒数会变长。
before* 的伤害仍会经过原版护甲和后续模组处理；after* 使用加载器报告的伤害值，不能假定与 beforeDamage 恒等。
虚空等绕过无敌的伤害、雷管猫最终自爆流程不会被 beforeAttack/beforeHurt 当作普通可取消攻击。
原生反伤不作为再次主动攻击；helper 造成的效果不会递归触发词条/饰品脚本事件。

```js
CatTraitEvents.beforeAttack(event => {
  const ctx = event.context
  const trait = ctx.trait('mypack:light_step')
  if (!trait) return
  // 这只是追加行为的写法；不要与示例中的同一效果重复安装。
  ctx.setDamage(Math.min(1000000, ctx.damage + trait.level))
})
```

## 持久状态、冷却与动态属性

在当前事件内调用 `ctx.trait(id)`，或在其他主线程事件中 `TraitApi.trait(cat,id)`。
不存在、定义未加载或 enabled=false 时返回 null。

```js
const t = ctx.trait('mypack:light_step')
if (t) {
  t.increment('mypack:hits', 1)
  t.setText('mypack:phase', 'ready')
  if (t.tryActivate('mypack:heal', 200)) {
    TraitApi.heal(ctx.cat, ctx.cat, 2)
  }
  // 同一词条同一属性是覆盖设置，不会每次回调叠加。
  t.setStatBonus('speed', 8)
  // 不再需要时：t.clearStatBonuses()
}
```

公开方法：

- getId / id、getLevel / level、getMaxLevel / maxLevel。
- number(key)、setNumber(key,value)、increment(key,amount)：默认 0；有限数，绝对值不超过 1e12。
- text(key)、setText(key,value)：默认空字符串，最长 512 字符。
- cooldownRemaining(key)、startCooldown(key,ticks)、tryActivate(key,ticks)：最长 12096000 tick（20 TPS 下 7 天）；默认无冷却，tryActivate 先占用再返回 true，0 tick 表示不设冷却。
- setStatBonus(stat,amount)、clearStatBonuses()：六维有效属性，不修改基因。每项 -999999..999999。
- removeState(key)：删除同名数值、文本、冷却键，不删除属性加成。

键必须是 namespaced ID 风格，推荐 `mypack:用途`；各类数值/文本/冷却分区最多 64 个键。
状态以“猫 UUID 所属实体数据 + 词条 ID”隔离，保存/猫饼还原后保留；移除该猫的词条会清理对应状态。
setStatBonus 是持久值，不是仅当前 tick 有效：不要只删除计算它的回调却继续保留同 ID 的启用定义。如果想停止，要清理 bonus 或禁用/移除定义。
死亡事件中死猫状态只读；事件结束后不能再修改 context；跨 tick/换词条/重载后旧 handle 抛错，必须重新获取。
队列有保护上限，本体桥接异常按事件类型限频记录；KubeJS 自身的脚本错误仍由其日志系统报告。不要依赖事件队列处理无限任务，也不要只看 GameTest 通过数量而忽略脚本错误日志。

## 安全效果辅助方法

- health(cat)、maxHealth(cat)：读取生命数值。
- nearbyAllies(cat,radius)、nearbyEnemies(cat,radius)：半径 0–32，最多 32 个可见目标；只查询已加载区域，刚加入世界尚未 tick 的猫返回空列表。
- friendly(cat,target)：判断自身、主人/友方关系。
- heal(cat,target,amount)：仅自己/友方，距离不超过 32 格。
- damage(cat,target,amount)：仅可伤害目标，距离不超过 32 格且需视线；遵守无敌帧，不保证每次调用都掉血。
- addEffect(cat,target,effectId,ticks,amplifier,allies)：true 仅友方，false 仅可伤害目标；距离不超过 32 格，amplifier 为 0–255。

damage/heal 的 amount 为有限数，范围 0–1000000000；before* 的 setAmount/setDamage 范围为 0–1000000。不要用无限值代替“很大”。
用这些 helper 可避免词条效果再次触发自身及饰品的脚本回调。若绕过 helper 直接调用原版方法，需要自行承担映射、友伤和事件连锁风险。

## 繁殖事件

```js
CatTraitEvents.breed(event => {
  const ctx = event.context // 此时没有实体，ctx.cat === null
  if (ctx.parentLevel(0, 'mypack:light_step') > 0 &&
      ctx.parentLevel(1, 'mypack:light_step') > 0) {
    ctx.setChildLevel('mypack:light_step', 2)
  }
})
```

方法：childTraits()、childLevel(id)、parentLevel(0或1,id)、setChildLevel(id,level)。
0 移除；同样遵守容量、互斥和定义开关；不要把 childProfile 当作公开脚本入口。
实体与猫饼繁殖都经过同一个词条生成出口，修改不回写亲代。
不要在这个事件查“孩子的位置”或扫描附近生物，此时根本不一定有孩子实体。

## 重载、移除与更新兼容

- `/reload` 更新脚本与定义。热加载采用新旧表替换，服务器同步定义，旧猫按 ID 使用新数据。
- 已存在定义的 JSON 改坏时，保留上次有效定义并报错；第一次注册失败则不会凭空创建。
- 删除注册文件/脚本会暂停该词条效果，保留 ID、原等级和状态，面板显示“未加载词条”；重新安装相同 ID 后恢复。
- 暂时降低 max_level 不抹掉存档旧等级；恢复上限后可恢复原等级。主动用 API/调整棒改等级则是明确的新存档值。
- 修改互斥规则不清洗已有猫咪词条；只约束以后新增/繁殖。这是防止更新时丢失数据。
- 过滤器保留未加载 ID，并按猫身上是否存有该 ID 筛选，不因定义消失而偷偷放宽筛选条件。
- 不支持自定义贴图/动画的自动生成；需要资源包或额外客户端实现。词条当前使用本体对应稀有度卡片样式。
- 支持 API v1 的后续更新须保留旧方法与 schema 1。正常 build 已加入签名、事件和 82 个存档 ID 的兼容检查；行为仍需真实 KubeJS 回归。跨未来不兼容的大版本不能无条件保证。
- 本文和示例不放入运行 JAR，不向用户实例自动安装测试脚本，不覆盖旧饰品 SDK。

## 可复核的验证入口

项目内：
- `tests/CatTraitScriptRegression.java`：解析、同步、重载、遗传、互斥、存档保留、随机池。
- `tests/gametest/<forge 或 neo>/cn/laowu/mod/test/CatTraitScriptProbe.java`：真实 KubeJS，战斗/回血/取消/死亡、猫饼还原、过滤器/调整棒和真正的异步服务器资源重载。
- `compatibility/cat-trait-api-v1.json`：公开方法契约；独立包中为 `api-contract.json`。
- 可用 `-PtraitExamples -PtraitKubeProbeOnly` 与既有 `tests/accessory-gametest.init.gradle` 跑独立词条测试；不带 Only 并加入饰品示例标志可跑新旧脚本共存回归。
