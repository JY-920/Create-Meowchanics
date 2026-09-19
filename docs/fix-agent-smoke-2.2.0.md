# 2.2.0 特工烟雾修复与正式包刷新

日期：2026-09-18。公开版本与包内版本均为 `2.2.0`，不再带内部后缀。以 `2.2.0-dev.agentsmoke.2` 为基线，仅修改本次烟雾问题及说明，保留暮色区块加载修复、全智力烟雾和非辅助队友条件。

## 已确认原因

用开启真实 AI 的铁傀儡接受猫咪攻击，确认其原版 HurtByTargetGoal 已实际运行。旧版虽然清空 Mob.target、受击来源与中立愤怒记录，但 TargetGoal 还保留独立的 targetMob 缓存；隐蔽期间 setter 被拦截却未停止该目标任务，隐蔽结束后重新锁定旧目标。

旧代码负向回归日志：`forge-1.20.1/build/smoke-golem-before.log`，失败断言为 “Expired smoke must not revive the cached revenge target”。不是仅对 NoAI 生物调用 setTarget 的模拟。

## 修改

- 撤退当 tick 直接释放烟雾并解除仇恨，不再生成投掷弹。旧 AgentSmokeBomb 注册和存档读取保留，渲染改为 NoopRenderer，旧存档内残留弹体也不显示烈焰弹图标。
- 当前正追击特工的生物会停止运行中的目标任务和移动／注视任务，清除目标缓存与导航；保留任务注册，不关闭 AI。
- 按猫咪身份清除报复来源、中立生物愤怒 UUID／时间，以及 Brain 的攻击、愤怒、受伤、注视与跟随记忆；不清除对其他猫的仇恨。
- 精确拦截 LivingEntity.canAttack(LivingEntity) 的生物选敌判断，使缓存目标、普通目标条件和原版 Brain 选敌遵守临时隐蔽。不是伤害免疫，不改变玩家控制。
- 保留目标事件拦截并补充最低优先级检查；“吸引仇恨”词条不能再把敌人重定向到隐蔽中的猫。
- 一次性清理与隐蔽期补充清理使用 32 格内已加载实体查询；隐蔽的选敌限制在同一世界不再受原来的 20 格观察距离限制。不请求新区块。
- 保持隐蔽 50 tick、撤退 60 tick、烟雾冷却 200 tick；无智力门槛，仍需 12 格内非辅助友方猫。结束后新的攻击可正常重新引起仇恨，不保留永久免战。
- 使用原营火粒子：普通烟雾初始 32 → 128，治疗烟雾初始 24 → 96、每 5 tick 补充 4 → 12；缩放 3 → 4，主体 alpha 0.32 → 1.0，最后 10 tick 淡出。纹理本身的柔边仍保留。
- 治疗烟雾仍为 3 格、持续 5 秒、2.5 HP/s、不可叠加；不改治疗数值和饰品 API。

## 验证

- 烟雾原生专项：Forge 4/4、NeoForge 4/4。覆盖真实铁傀儡报复、消散后旧仇恨不恢复、新攻击仍可重新激怒、其他目标仇恨保留、猪灵 Brain 记忆、词条重定向、远距离选敌及零智力当 tick 无弹体释放。
- 正式源码完整 KubeJS + 36 饰品替换 + SDK 新饰品示例：NeoForge 86/86；Forge 84/86。两端所有本次烟雾用例、治疗烟雾固定回复及脚本 API 相关用例通过。
- Forge 保留两个此前已有的未通过用例：smartArtillery（远距离追击后重新架炮）和 radiusHostilesAndWalls（测试实体跟踪序列超时）；未修改断言、延长超时、删除用例或将它们算作通过。
- 完整回归中的真实 ProtoChunk 转换用例按原设计未启用并输出 SKIP；其前轮真实暮色双端验证见 `docs/fix-chunkload.1.md`，本次不声称重跑暮色长途探索。
- 双端正常 build 成功，每端 23,252 项饰品检查；API v3 契约、动画资源、发布隔离检查通过。27 组源码／资源检查、284 项成品 Mixin 检查及原 SDK 287 项检查通过。
- 与 agentsmoke.2 成品逐 ZIP 条目核对，仅烟雾相关 6 个既有运行类、两份语言、Mixin 配置和版本元数据改变；新增 1 个选敌 Mixin 类，无条目删除。其他职业、饰品定义、配方及美术资源保持一致。
- 粒子数值和资源接线已核对；未自动开启图形客户端，不声称人工观察了光影／粒子设置下的最终遮挡效果。

日志：两端 `build/smoke220-sdk1.log`、`build/smoke220-release-build.log`；原生专项为 Forge `build/smoke-golem-after.log`、NeoForge `build/smoke-golem-after2.log`。

## 复跑

在对应子项目目录使用 Java 17（Forge）或 Java 21（NeoForge）：

```powershell
# 仅烟雾专项，无 KubeJS。每次使用新 fixture ID。
.\gradlew.bat runGameTestServer --init-script ..\tests\accessory-gametest.init.gradle '-PagentSmokeProbeOnly' '-PaccessoryNoKubeJS' '-PaccessoryRunId=smoke-new' --no-configuration-cache

# 真实脚本全量回归。
.\gradlew.bat runGameTestServer --init-script ..\tests\accessory-gametest.init.gradle '-PaccessoryExamples36' '-PaccessorySdkDemo' '-PaccessoryRunId=smoke-sdk-new' --no-configuration-cache

# 发布使用正常构建，不附加测试入口。
.\gradlew.bat build --no-configuration-cache
```

## 成品及部署

| 平台 | SHA-256 |
| --- | --- |
| Forge 1.20.1 | `1C362E2A148A9946102AFB9AACF867C00C669C5C8812A0DFD45B45C7AA693CD3` |
| NeoForge 1.21.1 | `37227D45320C699C32E7E2688436D6300DFC1DA6F01EBD75854DFEB944242216` |

13:14:55 已自动部署到两个开发客户端，真实 modId=laowu 各仅一个启用包；包内版本、安装后 SHA-256 与正常构建一致。旧 agentsmoke.2 放在各实例 `mod-backups/create-meowchanics/20260918-131454-2.2.0-smoke/`，可恢复；没有修改存档、配置、kubejs 或测试服。

旧公开 2.2.0 的 ZIP、展开目录及说明已复制到 `releases/release-backups/20260918-125444-2.2.0-before-smoke-refresh/`。新版同名本地发行 ZIP 替换的是本地交付物，没有上传发布平台或提交 Git。SDK 1.0.0 保持不变。
