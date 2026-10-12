# 耄耋 Boss Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 双端实现三次抚摸博古架猫、绕架气场、巨大化、三招 Boss 战及安全还原原猫的可玩首版。

**Architecture:** 座位保存短期抚摸和召唤状态，独立挑战服务管理落点、变身事务与还原。现有 GiantCatBoss 仅在新增耄耋身份下启用专属战斗、持久化和渲染分支；客户端按服务器同步时间播放限量特效，不改变旧零食 Boss。

**Tech Stack:** Java 17 / Forge 1.20.1，Java 21 / NeoForge 1.21.1，原版实体数据同步与 NBT，既有 Blockbench 渲染器、GameTest、Node 回归和真实 KubeJS。

**Spec:** `docs/superpowers/specs/2026-10-06-maodie-boss-design.md`

## Global Constraints

- 有无机械动力均可使用，Forge 与 NeoForge 保持相同玩法；不改网易工程。
- 每次有效抚摸只触发现有半秒哈气，不增加躲头、拍尾、压耳动画、次数提示或其他表现。
- 蹲下空手主手计数：至少间隔 10 tick，8 秒未继续清零；第三次先播放 3 秒气场，再用 1 秒巨大化。
- 原猫进入挑战后的 5 分钟冷却写入持久数据；普通右键哈气不受影响。
- 四倍模型，实际宽 2 格、高 3 格；耄耋 300 HP，旧巨猫 800 HP 不变。
- 原猫完整养成数据保留；正常击败、命令杀死或连续 20 秒脱战安全还原，无安全位置则保留快照等待。
- 不破坏建筑、不强制切镜头、不加载未加载区块；首版无新战利品，不发旧 Boss 掉落或经验。
- 不改饰品 API v1–v3、schema 1、词条 API v1、82 个内置词条或已有物品 ID；测试不混入 main。
- 保留脏工作树其他改动，不自动提交、推送、合并或公开发布。完成必要验证后遵循 AGENTS.md 自动备份部署双端；运行中的 Java 客户端不能关闭或替换。

## Review Focus

1. 双手、快速连点、座位换猫：不能借用上一只猫的次数，不能多生 Boss（任务 1、2）。
2. 气场期间喂食、命名或驯服：最终提交才快照；驯服取消，不回滚正常交互（任务 2）。
3. 死亡与 /kill 在还原位置全堵时：不掉落、不丢备份，之后开放位置只还原一次（任务 2）。
4. 重载遇到相同 UUID：保留未知字段，不覆盖其他实体，不强行加载源区块（任务 2）。
5. 玩家晚到、架子侧向或召唤中卸载：粒子跟随同步进度，取消后无残留，旧巨猫渲染不变（任务 3）。

## 文件与职责

下列生产路径在 `forge-1.20.1/src/main/java/cn/laowu/mod/` 与 `neoforge-1.21.1/src/main/java/cn/laowu/mod/` 各有一份，接口一致，只适配平台签名。

| 文件 | 改动职责 |
|---|---|
| 新建 `encounter/MaodieSummonState.java` | 纯计数、超时、气场时钟；不操作世界 |
| 新建 `encounter/MaodieEncounter.java` | 检查源猫、安全落点、提交变身、结束与还原 |
| 修改 `entity/BoguShelfSeat.java` | 同步气场起始时间，调用服务，保留半秒哈气与原坐姿清理 |
| 修改 `CommonEvents.java` | 仅在博古架猫交互分支转发有效主手抚摸 |
| 修改 `entity/GiantCatBoss.java` | 耄耋身份、快照/结束状态、数值与生命周期钩子 |
| 新建 `entity/MaodieCombat.java` | 哈气阶段、固定方向命中几何、半血及招式参数 |
| 新建 `client/MaodieAuraEffects.java` | 客户端绕架粒子及末尾收束；客户端专用事件订阅 |
| 修改 `client/GiantCatBossRenderer.java`、`client/GiantCatBossAnimation.java` | 耄耋巨大化和哈气骨骼；旧召唤道具仅旧 Boss 可见 |
| `tests/gametest/{forge,neo}/cn/laowu/mod/test/Maodie{State,Encounter,Combat}Probe.java` | 新建隔离逻辑、生命周期、实战测试 |
| `tests/standalone-client/{forge,neo}/cn/laowu/mod/client/MaodieClientProbe.java` | 新建实际客户端演出与截图测试 |
| `tests/accessory-gametest.init.gradle`、`tests/standalone-client.init.gradle` | 增加独立探针选择，不改正常发布源码集 |
| `docs/maodie-boss-verification.md` | 实际命令、结果、截图和未覆盖限制；任务 5 新建 |

不改旧 `GiantCatTreatItem`、不新建召唤物品/奖励，不拆改整个既有 Boss AI。

## 验证命令约定

在仓库根目录执行；下例选择 Forge，Neo 替换 `$maodiePort` 与 JDK 为 `neoforge-1.21.1` / `C:/Program Files/Zulu/zulu-21`。所有新增 GameTest 使用新的 `maodie_probe`（32×12×32），各测试显式清理和铺设自己的区域，架子置于水平中心附近；现有 14 格宽 artillery 场地不能容纳完整落点搜索。

```powershell
$maodiePort = 'forge-1.20.1'
$env:JAVA_HOME = 'C:/Program Files/Zulu/zulu-17'
Push-Location $maodiePort
./gradlew.bat runGameTestServer --init-script ../tests/accessory-gametest.init.gradle --init-script ../tests/bogu-shelf-memory.init.gradle -PmaodieProbeOnly -PaccessoryNoKubeJS -PaccessoryRunId=maodie-task1-forge-20261006 --no-configuration-cache --max-workers=2
Pop-Location
```

预期：选定测试全部通过、失败数 0，进程成功退出。每轮/每端使用新 `accessoryRunId`，不可复用有残留实体的旧测试世界。无 Create 再追加 `-PwithoutCreate`。任务 1 增加 `maodieProbeOnly`（三类新探针）及 `maodieStateOnly`（仅状态探针），其后任务依次扩展文件，而不是引用尚不存在的测试类。

## Task 1：可验证的抚摸与气场时钟

**Files:** 新建双端 `encounter/MaodieSummonState.java`、`MaodieStateProbe.java`；修改 `tests/accessory-gametest.init.gradle`。

**Interfaces:**
- Produces: `MaodieSummonState.StrokeResult { IGNORED, ACCEPTED, READY }`；`StrokeResult stroke(UUID catId, long now)`，`int count()`，`boolean active()`，`long startTick()`，`void begin(long now)`，`boolean due(long now)`，`void rejectThirdStroke()`，`void reset()`。
- `stroke` 管理猫 UUID 和 10/160 tick 边界；READY 暂存第 3 次，未找到位置用 `rejectThirdStroke()` 回到 2 次。`begin` 锁定计数并记录 60 tick 时钟；重载不恢复未提交的气场/计数。

- [ ] 写失败测试 `debounceTimeoutAndOwnerChange`、`thirdStrokeStartsSixtyTickWindow`、`blockedThirdStrokeCanRetry`。用 GameTestHelper 的 `assertTrue` 断言：

```java
var s = new MaodieSummonState();
UUID a = UUID.randomUUID(), b = UUID.randomUUID();
h.assertTrue(s.stroke(a, 600) == ACCEPTED && s.count() == 1, "first");
h.assertTrue(s.stroke(a, 609) == IGNORED && s.count() == 1, "ten-tick debounce");
h.assertTrue(s.stroke(a, 610) == ACCEPTED && s.count() == 2, "second");
h.assertTrue(s.stroke(a, 620) == READY, "third");
s.rejectThirdStroke();
h.assertTrue(s.count() == 2 && s.stroke(a, 630) == READY, "retry opportunity");
s.begin(630);
h.assertTrue(s.active() && !s.due(689) && s.due(690), "sixty ticks");
h.assertTrue(s.stroke(a, 650) == IGNORED, "locked during aura");
s.reset(); s.stroke(a, 700); s.stroke(a, 860);
h.assertTrue(s.count() == 1, "160 ticks resets before accepting new stroke");
s.stroke(b, 870);
h.assertTrue(s.count() == 1, "new cat never inherits count");
```

- [ ] 运行验证命令，改用 `-PmaodieStateOnly`；确认失败来自尚未实现的接口/断言，而非依赖下载或测试框架错误。
- [ ] 实现上述接口和探针选择；在 init 的 `prepareAccessoryStructure` 结构映射追加 `maodie_probe: [32, 12, 32]`。状态只用服务端 gameTime，不引入静态跨世界缓存。
- [ ] 双端重复同命令，预期状态探针全部通过；记录新建文件及差异，不提交用户工作树。

## Task 2：召唤事务与原猫安全还原

**Files:** 新建双端 `MaodieEncounter.java`、`MaodieEncounterProbe.java`；修改双端 `BoguShelfSeat.java`、`CommonEvents.java`、`GiantCatBoss.java`。

**Interfaces:**
- Consumes: 任务 1 全部接口；既有 `BoguShelfSeat.cat()`、`anchor()`、`sanitizeSnapshot(CompoundTag)`、`hissBriefly()`。
- Produces: 服务静态入口 `void onStroke(BoguShelfSeat seat, Player player, InteractionHand hand)`，`void tickSeat(BoguShelfSeat seat)`，`Optional<Vec3> findBossSpawn(ServerLevel level, BlockPos origin, Direction facing)`，`boolean tickBoss(GiantCatBoss boss)`，`void requestEnd(GiantCatBoss boss)`，`boolean tryRestore(GiantCatBoss boss)`。`tickBoss` 返回 true 表示结束等待已接管、不得继续普通攻击。
- 座位新增 `boolean isSummoningMaodie()`、`long maodieAuraStartTick()`；同步 long 默认 -1，起始时间可见仅服务端确认后。服务与座位同一游戏刻不能重复提交。
- Boss 新增 `boolean isMaodie()`、`void configureMaodie(CompoundTag sourceCat, BlockPos origin)`、`CompoundTag sourceCatSnapshot()`（返回副本）、`BlockPos maodieOrigin()`、`boolean restorationPending()`、`void markRestorationPending()`、`int getSummonDurationTicks()`。新身份默认 false；旧 SUMMON 保持 60 tick，耄耋为 20 tick。

- [ ] 编写 `ordinaryAndOffhandNeverCount`、`thirdStartsAuraBeforeSpawn`、`auraCancellationPreservesCat`、`snapshotAtCommitPreservesLateChanges`、`blockedSpawnKeepsThirdChance`。通过真实平台 EntityInteract 事件，分别模拟普通右键、蹲下主手、持物、旁观及副手；断言每次有效动作哈气 10 tick，三次后 59 tick 原猫仍在架上且 Boss=0，第 60 tick Boss=1、原猫才移除。气场时拆架、驯服、离座或加载条件失效不得生成 Boss；提交时被堵仍保留猫。
- [ ] 编写 `defeatRestoresFullSnapshot`、`killCommandRestoresWithoutLoot`、`blockedRestorationRetriesOnce`、`savedPendingEncounterAndDuplicateUuid`、`oldBossKeepsLegacySemantics`。使用同一测试类私有 fixture 创建架/猫/玩家，`UUID sourceUuid` 和 `CompoundTag before` 指向原猫；测试断言代码须包含：

```java
h.assertTrue(boss.isMaodie() && boss.getMaxHealth() == 300, "new encounter values");
h.assertTrue(boss.sourceCatSnapshot().getUUID("UUID").equals(sourceUuid), "identity preserved");
// 还原后，对 saveWithoutId 的副本去掉位置/运动/临时姿势和预期冷却，再逐字段比较：
h.assertTrue(restored.getUUID().equals(sourceUuid), "same original cat");
h.assertTrue(restoredData.equals(expectedData), "genome, traits, outfit, accessory and opaque script NBT");
h.assertTrue(restoredCount == 1 && bossCount == 0 && lootCount == 0 && xpCount == 0, "single restore, no rewards");
```

测试夹具给猫写入名字、年龄、原坐下指令、六维、词条、套装、饰品和未知持久脚本字段；不只测原版皮肤。额外校验提交失败不移除原猫、气场期间命名/喂食不回退、5 分钟冷却、全部挡住后快照不消失。NBT 保存/重新加载验证身份、快照、pending 与计时；重复 UUID 猫不再生成，非猫 UUID 冲突不覆盖。
- [ ] 双端运行新探针，确认上述功能当前失败。检查两端映射源码中伤害致死、`/kill` 与 removal reason 的实际签名，再添加生命周期适配，不能猜测平台 override；用实际服务端 kill 命令验收该路径。
- [ ] 接入状态与服务。仅合规抚摸计数，普通半秒哈气保留；气场失败显示“腾出空间”。安全 Boss 落点按半径 4–8 格、原点 Y±2 搜索，检查完整 2×3 包围盒及相邻碰撞形状、世界边界、已加载区块、支撑与流体/危险；不读未加载区块。最终重新检查后保存完整实体 NBT，使用座位 sanitize 恢复原 Sitting，把冷却 gameTime+6000 写入快照的持久数据；成功 addFreshEntity 后才 discard 原猫。加入失败不得给仍存活的原猫写冷却。
- [ ] 实现 Boss 持久化与结束事务。使用保存的实体 ID 加载原猫，不丢未知字段；只清理已知临时查看/哈气/骑乘状态。正常致死前转 pending 阻断原死亡/奖励路径，命令杀死同样处理；pending 停止移动、伤害与血条，每 20 tick 尝试恢复。搜索当前 Boss 附近半径≤8 与架子原点的已加载安全位置，成功添加原猫或确认同 UUID 原猫已存在后才清除 Boss。遇同 UUID 非猫保持备份并等待，不替换实体。
- [ ] 接入无有效玩家 400 tick 结束计时；参与者为 32 格球形范围内存活的生存/冒险玩家，创造/旁观排除，巨大化期间不累计。正常区块卸载只保存，不触发还原；座位重载取消未提交气场。源猫在气场期间始终存在，不需跨区块强制加载。
- [ ] 双端有/无 Create 运行 `-PmaodieProbeOnly -PboguShelfProbeOnly -PgiantBossProbeOnly` 新 fixture，预期生命周期和所有旧 Bogu/巨猫探针失败数 0。此任务不对外部署未完成战斗/客户端版本。

## Task 3：绕架气场、巨大化与视觉验收

**Files:** 新建双端 `MaodieAuraEffects.java`、`MaodieClientProbe.java`；修改双端 Boss renderer/animation 和 `tests/standalone-client.init.gradle`。

**Interfaces:**
- Consumes: `BoguShelfSeat.isSummoningMaodie()`、`maodieAuraStartTick()`、`anchor()`；`GiantCatBoss.isMaodie()`、`getSummonDurationTicks()`、既有 phase/genome 同步。
- Produces: `MaodieAuraEffects` 客户端事件订阅，按已跟踪座位同步时间生成粒子；动画静态 `float maodieGrowthScale(float summonTicks)` 输出从 .64 到 4 的平滑比例，仅耄耋使用。客户端探针选择参数 `maodieClientOnly`，日志成功标记 `MAODIE CLIENT PASS`。

- [ ] 先写 `growthEndpointsAndLegacySummon` 数值断言：耄耋 0 tick=.64、10 tick 在(.64,4)、20 tick=4；旧 Boss SUMMON 和旧悬浮零食保持原值/显示路径。客户端探针依次布置四朝向架子，触发真实交互而非只生成静态粒子。
- [ ] 首次运行客户端，记录没有绕架气场/巨大化断言失败；只操作隔离 fixture，不关闭用户测试实例。
- [ ] 实现气场：位置由 anchor+水平朝向计算整座架子 3×3×1 外缘，淡金色粒子螺旋上升，60 tick 内疏→密、慢→快，末段收束到展示猫再散开。每座每 tick 最多 8 个粒子，64 格外不生成；无额外资源大光柱、无 HUD 次数、无镜头劫持。取消/卸载不再生成，已有粒子短寿命≤10 tick。
- [ ] 在 Boss renderer 接入巨大化和正确脚底对齐，保留原猫材质；耄耋不渲染旧召唤道具。20 tick 演出结束才出现“耄耋·不许摸”血条。普通巨猫的动画/体型/脚底契约不动。
- [ ] 两端 × 有/无 Create 实际启动客户端：在端目录运行 `./gradlew.bat runClient --init-script ../tests/standalone-client.init.gradle --init-script ../tests/bogu-shelf-memory.init.gradle -PmaodieClientOnly -PmaodieClientRunId=forge-create-20261006 --no-configuration-cache --max-workers=2`，无 Create 追加 `-PwithoutCreate`，每端每轮更换 ID。复制已有隔离基础世界到独立 Maodie fixture，不共享用户存档；init 新增 `maodieClientRunId` 属性，校验 `[a-zA-Z0-9_-]+` 后使用 `build/maodie-client-<runId>`，不改原 Bogu 测试目录。
- [ ] 检查 `MAODIE CLIENT PASS`，逐张打开并目视核对四朝向气场、30/59 tick 猫仍在架上、巨大化起/中/末、血条、取消、晚到玩家从当前进度观看、战后原猫。保存截图至端 `build/maodie-client-<runId>/screenshots/`，未实际截图检查不可写“视觉通过”。

## Task 4：耄耋三招战斗与强化

**Files:** 新建双端 `MaodieCombat.java`、`MaodieCombatProbe.java`；修改 Boss AI/伤害与动画；扩展客户端探针。

**Interfaces:**
- Consumes: `GiantCatBoss.isMaodie()` 与旧阶段 0–8、碰撞移动、动画采样；任务 2 `tickBoss` 的结束接管返回值。
- Produces: `MaodieCombat.HISS_WINDUP=9`、`HISS=10`、`HISS_RECOVERY=11`；`boolean inHissCone(Vec3 origin, float lockedYaw, Vec3 victim)`，`float damageForPhase(int phase)`，`int recoveryTicks()`；Boss `boolean isMaodieEnraged()` 同步并持久化，`float hissYaw()` 同步出招锁定方向。
- 参数：吸气 20 tick、哈气 20 tick、恢复≥20 tick；扇形半角 35°、范围 8 格，固定水平 yaw，一次施放每对象最多命中一次；生命≤150 首次强化。常态间隔 80–120 tick，强化 40–60 tick，但蓄力/恢复不缩短。

- [ ] 写失败测试 `hissTelegraphConeAndWall`、`noRepeatHitOrPetDamage`、`halfHealthEnragesOnce`、`noAiStopsEveryAttack`、`legacyDamageUnchanged`。真实服务端断言：预警 19 tick 不伤害，第 20 tick 方可进入哈气；正前 7 格可中，9 格/侧后/墙后不伤；同对象本次仅扣 4；普通猫和宠物猫均不伤；强化只播一次且重载不重播；NoAI/结束等待不伤害。
- [ ] 使用纯几何断言钉住角度边界；使用真实实体伤害断言近身 6、翻滚 8、跳砸 12、哈气 4，旧 Boss 保持近身 8、翻滚 12、跳砸 18 和 800 HP。
- [ ] 首次双端运行 Maodie + oldBoss 探针确认新战斗缺失的失败，再实现专属分支；哈气 LOS 使用原版射线判定，出招开始采样方向不随玩家瞬间转动。复用旧移动/碰撞和受击冷却，不做瞬移/穿墙。目标选择限有效玩家，普通巨猫选择/伤害逻辑不变。
- [ ] 用现有骨骼添加吸气/哈气/恢复动画和一次半血音效/粒子；三招轮换避免哈气挤掉跳砸，仍保留至少 1 秒反击窗口。双方游戏刻和客户端阶段同步一致。
- [ ] 两端有/无 Create 跑 Maodie +旧 Boss 服务器测试；客户端探针截图三招预警、命中/恢复、半血、脱战还原，目视核对。全部失败数 0 才进入打包。

## Task 5：完整回归、审查与安全部署

**Files:** 更新验收文档；仅验证后更新内部构建编号/部署记录，不修改 API 契约。建议 internal build `optional-create.10`，公开版仍 2.2.2。

**Interfaces:** Consumes: 前四任务所有探针及现有 build/deploy 契约。Produces: 两端已验证正式 JAR、真实验收记录、可恢复部署报告。

- [ ] 双端真实 KubeJS 回归：使用验证命令，去掉 `-PaccessoryNoKubeJS`，添加 `-PaccessoryCompatibilityProbeOnly -PtraitKubeProbeOnly -PmaodieProbeOnly -PboguShelfProbeOnly -PgiantBossProbeOnly`，全新 fixture。确保并列选择包含所有探针而不是意外只执行其中一类；必要时修正仅测试 init 的 include 聚合。预期 API/饰品/词条/新旧 Boss 与 Bogu 所有测试通过，无 Python/SDK 或用户 kubejs 写入。
- [ ] 执行现有顶层 Node 回归，先逐条读取脚本是否需要参数，仅运行无参 regression（排除 fixture/bundle 生成器）；保存脚本清单与退出码，不把跳过项记成通过。运行 `git diff --check`，双端正式 build（各端对应 JDK）：`./gradlew.bat build -Pinternal_build=optional-create.10 --no-configuration-cache --max-workers=2`。预期 `BUILD SUCCESSFUL`，原 API、动画和 release isolation 验证没有被禁用。
- [ ] 按已选择执行方式做独立只读最终审查，只涵盖本次新增/重叠改动，不能把旧 Bogu 审查当本次审查。修复 Important/Critical 时先补失败回归，再重跑相关双端测试；不自动使用新模型覆盖。
- [ ] 创建 `docs/maodie-boss-verification.md`：列出具体 run ID、测试数、真实 KubeJS 结果、四环境截图路径/目视结论、NBT/kill/无安全落点结果、旧 Boss 兼容、剩余限制；外部模组强行 discard 不保证恢复。任何必要测试或截图失败则保留开发源、不部署、不宣称完成。
- [ ] fresh 检查游戏进程；若任一目标实例运行，等待用户退出且不强关。然后运行 `./tools/deploy-cat-additions.ps1 -Label maodie-boss -Version 2.2.2-dev.optional-create.10`。预期两个实例各一包 `modId=laowu`，源/安装 SHA-256 相同，旧 .9 保留在 `mod-backups/create-meowchanics/<时间戳版本>/`，正式包无探针/世界/SDK。记录脚本实际部署报告，不移动其他模组/配置/存档。

## 自审与执行交接

- 设计覆盖：触发/时钟任务 1；事务/持久化/安全/结束任务 2；气场/巨大化任务 3；战斗/半血任务 4；双端回归/部署任务 5。
- 接口检查：客户端不引用尚不存在的通用服务方法，服务返回值与 Boss 接管顺序一致；新阶段只追加 9–11，不重编号旧 0–8。
- 风险检查：Review Focus 五类已落实为任务 1–3 的具名测试；补测不替代原版 GameTest/真实客户端验证。
- 本文件只记录计划，没有产品实现或完成声明。计划审阅后推荐当前会话直接执行（Native）：生命周期、Boss 与渲染共享接口较多，逐任务保持同一上下文更合适；末尾独立审查。
- 决策说明：首版半径/扇形/恢复等未在设计钉死的数值均在本计划明确，作为可调调试值；公开版本、旧玩法与用户工作树不做额外整合。
