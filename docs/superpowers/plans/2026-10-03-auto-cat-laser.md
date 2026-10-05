# 自动激光标记器 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将猫机壳底材替换为纸板块，并交付六向动力激光标记器和坐垫激光猫工作能力，验证后部署两个本地实例。

**Architecture:** 独立 Create 动力方块负责展开、索敌与同步；`CatLaserWork` 负责坐垫参与、短期命令与单猫射击冷却，不使用普通战斗目标。仅工作弹延长飞行参数；客户端复用现有 Blockbench 与实心激光渲染，不新增必需依赖。

**Tech Stack:** Java 17 / Forge 1.20.1 / Create 6.0.8；Java 21 / NeoForge 1.21.1 / Create 6.0.10；Gradle、GameTest、真实 KubeJS、Node.js 资源检查。

**Spec:** `docs/superpowers/specs/2026-10-03-auto-cat-laser-design.md`，用户已确认。

## Global Constraints

- 追加 ID `laowu:cat_auto_laser`，显示名“自动激光标记器”；现有物品、方块及存档 ID 不替换。
- `create:cardboard_block` + `laowu:cat_ingot` → `laowu:cat_casing`；手动和机械手共用 `create:item_application`，去皮木入口删除。
- 仅模型底面接轴；六向安装；4 SU 系数；正负非零转速且未超载可工作，无最低转速。
- 展开／收回 25 tick；每 10 tick 扫描；目标为机器中心 24 格球内最近可见敌对实体；坐垫中心须在机器中心 5 格球内。
- 猫须驯服、存活、激光职业且实际乘坐 Create 坐垫；范围不受属性影响；伤害、攻速、饰品事件仍来自现有系统。
- 机器与猫均须无遮挡；不追击、不解除坐姿、不调用 `cat.setTarget`；多机共用单猫冷却。
- 普通战斗范围 16、普通弹飞行距离 16.5、普通弹寿命 20 tick 保持不变。工作弹距离至少 32 且不小于实际瞄准距离 +2，按速度 2.4 计算寿命并加 5 tick。
- 不新增新机器生存配方、PvP 权限、强制区块加载、GUI 或必需模组依赖。饰品 API v1–v3/schema 1、词条 API v1/schema 1 与 82 内置词条保持兼容。
- 运行 JAR 不包含测试、示例、SDK、测试世界或美术源目录；已发布 `releases/2.2.2` 不覆盖，不推送、不发布、不修改公开版本号。
- 保留现有 dirty worktree；构建串行，禁止其它任务同时改写同一端的 `build`。部署等待目标实例退出，仅备份／替换 `modId=laowu`，不修改世界、配置或玩家脚本。

## Review Focus

1. 同坐标拆机重放、区块重新加载：旧租约不能控制新方块实体；见 Task 3 `leasesExpireAndRejectReplacement`。
2. 多台机器与回调取消／变速：不叠加射速，取消弹不生成，脚本降速后的工作弹仍能到达范围内目标；见 Task 3、5。
3. 扫描之后新放墙、目标跨界或死亡：下一次发射前复检，不等待下个十 tick 扫描；见 Task 3 `shotRechecksBothSightLines`。
4. 六向旋转、跨 ±180° 瞄准：插值不能绕整圈，笔尖、光束和目标对齐；见 Task 4 `aimAndMuzzleAgreeInSixFrames`。
5. 实体 ID 重用、资源重载、底座离开视锥：无幽灵光束、无丢失模型或错误裁剪；见 Task 4 客户端探针。

## 文件与命令约定

以下 `PORT` 均明确指 `forge-1.20.1` 和 `neoforge-1.21.1` 两份；`J` = `PORT/src/main/java/cn/laowu/mod`，`R` = `PORT/src/main/resources`。GameTest 双份分别为 `tests/gametest/forge/cn/laowu/mod/test` 与 `tests/gametest/neo/cn/laowu/mod/test`。Forge 使用 `recipes`、`loot_tables`、`tags/blocks`；Neo 使用 `recipe`、`loot_table`、`tags/block`，结果物品字段分别为 `item`／`id`。

新增资源的精确路径为 `R/assets/laowu/blockstates/cat_auto_laser.json`、`models/block/cat_auto_laser.json`、`models/item/cat_auto_laser.json`、`models/entity/cat_auto_laser.geo.json`、`animations/cat_auto_laser.animation.json`、`textures/block/cat_auto_laser.png`、`textures/block/cat_auto_laser_off.png`（后六项也在 `R/assets/laowu` 下）。修改 `lang/zh_cn.json`、`lang/en_us.json`；追加 `R/data/minecraft/tags/blocks/mineable/pickaxe.json`（Neo 为 `tags/block`），新增 `R/data/laowu/loot_tables/blocks/cat_auto_laser.json`（Neo 为 `loot_table`）。

每端命令在该端目录执行；先设置对应 JDK，恢复原进程环境后再切换另一端：Forge `$env:JAVA_HOME='C:/Program Files/Zulu/zulu-17'`，Neo `$env:JAVA_HOME='C:/Program Files/Zulu/zulu-21'`。命令退出码非零即失败；必须检查 GameTest 结果与日志，不以 Gradle 启动成功代替测试成功。

执行前按 `using-git-worktrees` 检查当前 Git 状态与可用隔离区。不能使用仅含 HEAD、缺少当前未提交 2.2.2 内容的旧基线；不能通过全量提交、清理或重置用户改动来取得隔离。记录本任务触及文件的原始 diff；隔离无法保持基线时报告问题再选择执行位置。

## Task 1: 纸板猫机壳，真实应用流程

**Files:** Modify 双份 `CatMachineCraftingProbe.java`；Create `R/data/laowu/recipes/cat_casing_from_cardboard.json`（Neo 为 `recipe`）；Delete 两端旧 `cat_casing_from_logs.json`、`cat_casing_from_wood.json`。

**Interfaces:** 保留 `laowu:cat_casing`／`laowu:cat_ingot` 注册；只替换配方。手持交互仍通过 Create 的真实配方应用事件，无专用绕过分支。

- [ ] 写失败测试：将旧木头成功测试改为 `cardboardAcceptsCatIngotByHand`，主／副手、生存／创造分别断言产物为猫机壳，生存消耗 1、创造消耗 0。`strippedWoodNoLongerAcceptsCatIngot` 遍历现有全部去皮木，断言方块和猫锭数量不变。
- [ ] 写失败测试：`poweredDeployerAppliesCatIngotToCardboardAndDepot` 使用真实有动力机械手，分别对世界纸板块、置物台纸板块物品应用；断言单个产物、消耗 1。加入旧木料世界／物品负例，无重复应用配方。
- [ ] 跑红：两端 `./gradlew.bat runGameTestServer --init-script ../tests/accessory-gametest.init.gradle -PcatMachineCraftingProbeOnly -PaccessoryNoKubeJS -PaccessoryRunId=auto-laser-casing-red --no-configuration-cache`。预期纸板成功／去皮木拒绝断言失败，不能把无关基线失败认作该红灯。
- [ ] 增加唯一纸板 `item_application`，删除两种去皮木配方，不添加重复 deployer 配方。
- [ ] 用相同命令、新 `accessoryRunId=auto-laser-casing-green` 验证全部制作、采集和正常／潜行交互测试通过；记录本任务 diff。

## Task 2: 六向动力机器、连续展开状态

**Files:** Create 两端 `J/create/CatAutoLaserBlock.java`、`CatAutoLaserBlockEntity.java`；Modify `J/create/CatMachineBlocks.java`、`J/LaoWuMod.java`；Create 双份 `CatAutoLaserProbe.java`；Modify `tests/accessory-gametest.init.gradle` 增加 `catAutoLaserProbeOnly`（只包含本探针）；追加中英文语言、六向 blockstate、临时静态收回模型、item model、采集标签及掉落表。

**Interfaces:** `CatAutoLaserBlock extends KineticBlock implements IBE<CatAutoLaserBlockEntity>`，仅使用 `CatMachineOrientation.BOTTOM`；`getRotationAxis(BlockState): Direction.Axis` = bottom.axis，`hasShaftTowards(LevelReader,BlockPos,BlockState,Direction): boolean` 仅 bottom 为真。注册常量 `CAT_AUTO_LASER`、`CAT_AUTO_LASER_ITEM`、`AUTO_LASER_BE` 遵循各端 RegistryObject／DeferredHolder 类型。BE 暴露 `isWorking(): boolean`、`getExtension(float partialTick): float`，范围 [0,1]。

- [ ] 写 `bottomOnlyReceivesRealKineticsInSixDirections`：真实电机、传动杆连接每个 bottom，断言转速传入；其余五面不能驱动。`wrenchRotatesAndSneakDismantles` 验证点击面顺时针旋转与实际返还 1 个物品，不继承冲压机放置间隔约束。
- [ ] 写 `powerTransitionsAreContinuousAndOverstressStopsWork`：±16 RPM 可工作；0 或实际超载不可工作；25 tick 完成展开；中途反向过渡首帧连续；4 SU 被实际网络消耗。`registeredMachineHasDropsAndNoInventedRecipe` 验证创造入口与采集掉落，不存在自创配方。
- [ ] 跑红：两端 `./gradlew.bat runGameTestServer --init-script ../tests/accessory-gametest.init.gradle -PcatAutoLaserProbeOnly -PaccessoryNoKubeJS -PaccessoryRunId=auto-laser-machine-red --no-configuration-cache`。预期缺少新增类／注册或上述测试失败。
- [ ] 实现注册、Create 扳手／拆除及底面接口，BE 每 tick 从当前进度向动力决定的端点移动 1/25，停转立即令 `isWorking` 为 false；完整展开才准许索敌／发射。注册 `BlockStressValues.IMPACTS` 4.0，过载检查沿用 Create；加入采集与正式掉落。
- [ ] 两端改用 `auto-laser-machine-green` 跑绿；检查未改动其它猫机器放置、应力和扳手行为，记录 diff。

## Task 3: 标记、坐垫参与、远程弹与安全租约

**Files:** Create 两端 `J/create/CatAutoLaserTargets.java`、`J/CatLaserWork.java`；Modify `CatAutoLaserBlockEntity.java`、`J/CareerCatBehavior.java`、`J/entity/MechanicalLaserProjectile.java`；扩充双份 `CatAutoLaserProbe.java`。

**Interfaces:** `CatAutoLaserTargets.participants(ServerLevel,BlockPos): List<Cat>` 验证实际 Create `SeatEntity` 与对应 `SeatBlock`；`nearest(ServerLevel,BlockPos,List<Cat>): LivingEntity` 返回最近对至少一只参与猫合法的敌人，找不到返回 null；`validTarget(ServerLevel,BlockPos,Cat,LivingEntity): boolean` 复用现有敌对／队伍规则及 LOS。`CatLaserWork.offer(Cat,CatAutoLaserBlockEntity,LivingEntity): void`、`tick(Cat): void`、`stop(Cat): void`。BE 追加 `getTargetUuid(): UUID`（无目标 null）、`getTargetEntityId(): int`（无目标 -1）、`validWork(Cat,LivingEntity): boolean`。工作租约 WeakHashMap 以猫为键，保存弱引用机器实例、维度、位置、目标 UUID、到期世界 tick；有效期 12 tick，不写猫 NBT。

`MechanicalLaserProjectile.setWorkFlightLimits(double distance,int lifetimeTicks): void` 只由服务器工作射击调用；`getMaxTravelDistance(): double`、`getMaxLifetimeTicks(): int` 供行为测试。旧构造函数、`MAX_TRAVEL_DISTANCE` 与 `CatAccessoryProjectile` 接口保持不变；缺失／非有限／非正 NBT 参数回到普通默认值。

- [ ] 写 `fixedSphericalRangesAndHostileSelection`：目标 24 内／外，坐垫 5 内／外，含竖直与对角样例；坐垫偏远侧、目标偏近侧使猫到目标约 29 格。低／高属性结果相同；最近怪物优先、等距 UUID 稳定；友方宠物／玩家不攻击，中立仅在实际攻击参与猫／主人／友方时合法。
- [ ] 写 `seatedCatHitsFarTargetWithoutLeavingSeat`、`normalCombatKeepsOriginalRange`：工作弹真实命中约 29 格目标且猫仍坐着，普通弹在 16.5 格／20 tick 终止、普通 goal 不追射超 16 格。`invalidProjectileNbtUsesSafeDefaults` 检查 NaN、Infinity、负数、缺失与合法工作参数存取。
- [ ] 写 `shotRechecksBothSightLines`：机器被遮挡不标记；仅猫被遮挡仍可标记但不发射；扫描后新放墙、目标死亡／跨 24 边界，下一弹即禁止。实体飞行仍由方块碰撞阻止。
- [ ] 写 `leasesExpireAndRejectReplacement`：离座、拆坐垫、换职业、受检视、猫饼／玩家骑乘、断动力、超载、拆机重放同位置、猫／目标卸载、重读存档和世界切换，均不再新增弹；租约不留永久 NBT，不增加区块 tickets。`multipleMachinesShareOneCooldown`：不同 tick 遍历顺序不改变最近有效选机、等距 BlockPos 稳定、不重复发射，停用近机后可转用次近机。
- [ ] 跑红：Task 2 命令改用 `auto-laser-work-red`，预期新增工作行为断言失败。
- [ ] 实现 BE 十 tick 扫描与失效立即同步、已加载区块查询及稳定排序；LOS 使用 `COLLIDER/Fluid.NONE`，仅忽略机器自身方块的碰撞，不穿过其它块。被遮挡的最近敌人不阻止选取另一合法可见敌人。
- [ ] 实现 `CatLaserWork` 单猫工作冷却：tick 集中解析当下最近有效机器，不能由 offer 顺序决定本 tick 的第一枪；每枪重新验证所有条件，保持普通目标／坐姿。调用职业伤害与 `careerAttackIntervalTicks`，饰品 `projectile` 回调只调用一次；取消也消费一次攻击间隔，防止每 tick 重试刷回调。
- [ ] 实现工作弹参数：距离 `max(32, muzzle.distanceTo(aim)+2)`；先以 2.4 发射并运行饰品回调，再按实际有效速度计算足够寿命，正常速度寿命为 `ceil(distance/2.4)+5`；脚本速度为零／非有限时不加入世界。保存专用 NBT 飞行参数，不改普通默认值及正常伤害／友方保护；已发射弹不受机器停转撤回影响。
- [ ] 接入 `CareerCatBehavior.tick` 独立工作通道和换装 stop；两端以 `auto-laser-work-green` 跑绿，保留真实追击 AI 负例及落座断言，记录 diff。

## Task 4: 原模型展开、六向瞄准与红色光束

**Files:** Create `tools/export-cat-auto-laser.mjs`、`tests/cat-auto-laser-assets.mjs`；Create 两端 `J/client/CatAutoLaserPose.java`、`CatAutoLaserRenderer.java`、`CatAutoLaserItemRenderer.java`、`J/create/CatAutoLaserBlockItem.java`；Modify `J/client/CatMachinesClient.java`、`J/create/CatMachineBlocks.java`（新物品换用专用 BlockItem）；新增上述精确路径的两端正式模型／动画／纹理资源；Create 双份 `tests/machines/{forge,neo}/cn/laowu/mod/client/CatAutoLaserClientProbe.java`；Modify `tests/cat-machines-client.init.gradle` 增加 `catAutoLaserClient` 独立 fixture 与 `laowu.auto_laser_probe` 属性；BE 增加渲染边界和目标身份同步。

**Interfaces:** `CatAutoLaserPose.sample(float extension,float yaw,float pitch): Map<String,RuntimeBlockbenchModel.GroupTransform>` 以模型像素／弧度生成骨骼变换；`aim(BlockState,BlockPos,float extension,Vec3 target): Aim`；嵌套 `record Aim(float yaw,float pitch,Vec3 muzzle,Vec3 direction)`。`muzzle(BlockState,BlockPos,float extension,float yaw,float pitch): Vec3` 是图形与光束唯一出口计算；Aim 结果须满足 target-muzzle 与 direction 共线。客户端依据 BE 的 targetUUID 和 targetEntityId 同时匹配才渲染，不复用玩家指针的全局单标记状态。

- [ ] 写资源失败检查：源 `D:/Project_minecraft/待实现/自动激光/{model.bbmodel,model.geo.json,model.animation.json,texture.png,不亮.png}` 对应骨骼存在、父子层级完整、负尺寸转换后 UV／法向有效、零厚度面可见；两端输出一致，两张作者 PNG 原字节保留；应力口引用现有 `cat_casing_shaft_opening` 且面内凹 1 模型像素。`animation.model.new` 1.25 秒保留，不将 new2 整圈动作当追踪。
- [ ] 写 `aimAndMuzzleAgreeInSixFrames` 客户端数学断言：六 bottom、展开 0/.5/1、目标上下左右及 ±179°；出口计算与实际渲染骨架变换误差 <0.001 格，方向误差 <0.1°，偏航插值取最短弧。
- [ ] 跑红：`node tests/cat-auto-laser-assets.mjs`；两端 `./gradlew.bat runClient --init-script ../tests/cat-machines-client.init.gradle -PcatAutoLaserClient -PcatMachinesRunId=auto-laser-visual-red --no-configuration-cache`。预期缺失资源／未对齐／未注册渲染，不允许将打开主菜单算通过。
- [ ] 制作确定性 exporter：保留独立 `assets-source/cat-auto-laser` 艺术源（不打包），用现有 Bedrock loader 的真实坐标约定规范化输入，输出实际使用的骨骼模型、25 tick 动画与静态收回物品模型。先验证既有 loader 对负尺寸／零面的正确性；必要修改只允许追加兼容行为，并跑旧机型资源回归。
- [ ] 实现 pose 与 renderer：bone/bone3/bone7 升起、bone4/5 平移缩放，bone3 偏航、bb_main 俯仰；瞄准由局部逆变换后求解。正反动力时从当前 progress 连续采样；停机不亮纹理；轴口／转轴六向一致，开启 Flywheel 时也能显示。
- [ ] 实现红色 `CatLaserEffects.solidBeam`，使用正常深度测试，从上述 muzzle 到目标瞄准点；目标丢失立即停 beam。BE render bounds 包含展开模型及最长光束，实体 UUID 复核、区块卸载与资源重载不能残留旧目标；物品渲染为收回姿态，适配 GUI／手持／掉落尺度且不溢出格子。
- [ ] 以 `auto-laser-visual-green` 运行两端实际客户端探针，输出六向展开／收回、正反旋转、目标 tracking、近墙、仅 beam 在视锥、ID 重用／读档、图标截图与自动断言。执行者打开查看截图确认材质和光束笔尖衔接；资源检查通过不能替代真实画面验收。记录 diff。

## Task 5: 脚本行为、正式构建与安全部署

**Files:** 扩充双份 `CatAutoLaserProbe.java`（真实 KubeJS 分支）；必要时仅追加隔离 `tests/gametest/kubejs/server_scripts/probe.js`；Create `docs/testing/2026-10-03-auto-cat-laser-verification.md`。不把探针加入运行 resources。

**Interfaces:** 复用已有 `kubejs:accessory_probe`：`probe:projectile` 计数、`probe:state=cancel_shot` 取消、`setProjectileDamage(7)`、`scaleProjectileSpeed(0.5)`；原 API 契约清单不改。构建保留公开 `mod_version=2.2.2`；本地包仅在 build/libs 更新，部署 backup label `auto-laser` 区分本次构建，公开 releases 不覆盖。

- [ ] 写真实脚本红灯 `workShotsRespectRealKubeJsCallbacks`：一次工作射击计数 +1，伤害为 7，速度变为一半后仍可命中 29 格目标；取消不生成弹且不每 tick 重复回调。先对未接通的路径运行两端 Task 3 命令去掉 `-PaccessoryNoKubeJS`、RunId `auto-laser-kube-red`，确认行为断言失败；若已由 Task 3 实现通过，记录此前 Task 3 对回调的红灯，不制造无关故障。
- [ ] 最小修正工作发射与脚本时序，RunId `auto-laser-kube-green` 验证真实 Rhino/KubeJS 通过，无 JS ERROR；继续执行两端 `-PaccessoryCompatibilityProbeOnly -PaccessoryExamples36 -PaccessoryRunId=auto-laser-examples36-green` 与 `-PtraitKubeProbeOnly -PtraitExamples -PaccessoryRunId=auto-laser-traits-green` 的 GameTest 命令。
- [ ] 跑双端完整 GameTest（无 Only 过滤，新 RunId `auto-laser-full-green`，实际 KubeJS 开启），对失败先用 systematic-debugging 分离既有基线与本次回归，不削弱测试。两端执行 `./gradlew.bat build --no-configuration-cache`，确认 `verifyCatAccessoryApiV3`、`verifyCatTraitApiV1`、`verifyReleaseIsolation` 和资源检查都通过。
- [ ] Forge 执行 `./gradlew.bat reobfReleaseBootProbeJar --init-script ../tests/production-boot.init.gradle --no-configuration-cache`；仓库根执行 `pwsh -File tests/run-forge-production-boot.ps1 -JarPath 'forge-1.20.1/build/libs/create-meowchanics-2.2.2-forge-1.20.1.jar' -RunId auto-laser-green`。要求 `PASS: FORGE PRODUCTION JAR BOOT`、正常退出、正式映射无注入异常。检查两端 JAR 只有正式资源与运行类。
- [ ] 汇总本次验证报告：具体命令、测试数／结果、真实截图位置、构建 JAR SHA-256、基线问题与是否解决。执行前使用 verification-before-completion，最终 fresh reviewer 审查独立工作通道／兼容／模型变换，修复后重跑受影响验证。
- [ ] 仓库根执行 `pwsh -File tools/deploy-cat-additions.ps1 -Version 2.2.2 -Label auto-laser`。若检测目标实例运行，保留产物并等用户关闭，不强制结束；部署成功要求每端 ActiveLaowuCount=1、Verified=true、SHA-256 与构建一致、可恢复备份路径明确。报告仅称已完成确实跑过的检查。

## 自审与交付门槛

- [x] 对照规格：配方 Task 1；动力／六向／扳手 Task 2；固定范围／坐垫／弹体／租约／敌对与多机 Task 3；作者素材／动画／轴口／实心红光与裁剪 Task 4；脚本兼容、正常 build、正式 Forge 启动和部署 Task 5。
- [x] Review Focus 五项均有对应真实行为或客户端测试，不以源码字符串匹配替代。
- [x] 下一任务只依赖 Interfaces 明确列出的类／方法；不存在尚未定义的类型。Task 2 的静态临时模型随 Task 4 替换，不留多余资源。
- [ ] 每任务结束记录精确 diff 与验证结果；不自动全量 Git 提交，若另获用户提交授权，只暂存本任务文件／片段并保留无关改动。
- [ ] 实施前需用户审阅本计划并选执行方式；推荐 Native，由当前会话串行实现，末尾独立审查，避免共享构建目录并行污染。
