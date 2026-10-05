# 自动激光标记器验证记录

日期：2026-10-03。基线为现有 dirty develop 工作区，HEAD `4b8b05d`，未重置或提交用户原有修改。公开版本保持 2.2.2；不覆盖 `releases/2.2.2`，不推送或发布。使用说明见 [自动激光标记器](../auto-cat-laser.md)。

## 本次实现

- 纸板块 + 猫锭应用成猫机壳，手持及真实动力机械手均适用；删除两种去皮木配方。
- 新增 `laowu:cat_auto_laser`，六向放置、底面接轴、4 SU/RPM、正反转及超载检查、25 tick 连续展开/收回、Create 扳手旋转/潜行拆除。
- 机器中心 24 格球形索敌，5 格球内实际乘坐 Create 坐垫的驯服激光猫原地攻击；机器与猫各自检查视线，范围与属性无关。伤害、射击间隔和饰品回调保持现有系统。
- 独立短期工作租约，不写猫 NBT，不改变普通战斗目标或导航；多机器共享单猫冷却、最近有效机器优先、拆机重放实例校验、无强制区块加载。
- 作者模型和两张 PNG 接入六向动画、转向和真实笔尖红色实心光束；物品模型显示底部传动杆。仅修改原模型被遮蔽的盖面和底轴口，PNG 字节不变。
- 工作弹按饰品回调后的真实速度计算寿命，专用同步标志保持工作弹速度，避免原版阻力导致慢弹无法到达约29格目标。普通弹保留原版阻力、16.5格/20tick 默认限制。

没有为新机器擅自增加生存配方，目前从创造物品栏取得。没有更改其它职业/Boss机制、玩家脚本、存档或配置。

## 命令和证据位置

每端在对应目录执行 Gradle，Forge 使用 Zulu17，NeoForge 使用 Zulu21。所有本次日志保存在仓库 `.superpowers/sdd/2026-10-03-auto-cat-laser/`；这不是运行 JAR 内容。

通用 GameTest 命令：

```powershell
./gradlew.bat runGameTestServer --init-script ../tests/accessory-gametest.init.gradle <参数> --no-configuration-cache
```

| 检查 | 参数/日志 | 结果 |
|---|---|---|
| 手持/机械手纸板应用、旧木材拒绝 | `-PcatMachineCraftingProbeOnly -PaccessoryNoKubeJS -PaccessoryRunId=auto-laser-casing-green`；`casing-*-green.log` | 双端5/5，初始RED已记录 |
| 真实电机六向/动力过渡/扳手拆除 | `-PcatAutoLaserProbeOnly -PaccessoryNoKubeJS -PaccessoryRunId=auto-laser-machine-green2`；`machine-*-green2.log` | 双端3/3，初始RED已记录 |
| 实际坐垫/远程命中/视线/租约/多机/KubeJS/NBT | `-PcatAutoLaserWorkProbeOnly -PaccessoryRunId=auto-laser-work-kube-green2`；`work-*-kube-green2.log` | 双端8/8；后续变速改为0.25，见合并验收 |
| 慢弹阻力回归 | `work-neo-drag-red.log` → `work-neo-drag-green.log` | 真实KubeJS将速度减至0.25后，约29格命中RED→GREEN，8/8 |
| 36饰品真实兼容 | `-PaccessoryCompatibilityProbeOnly -PaccessoryExamples36 -PaccessoryRunId=auto-laser-examples36-green`；`examples36-*.log` | 双端37/37 |
| 词条真实兼容 | `-PtraitKubeProbeOnly -PtraitExamples -PaccessoryRunId=auto-laser-traits-green`；`traits-*.log` | 双端7/7 |
| 原生完整回归，真实KubeJS | `-PaccessoryRunId=auto-laser-native-full-green`；`full-*-native-green.log` | Neo227/227；Forge225/227，旧用例失败见下 |
| 替换36饰品+词条完整回归 | `-PaccessoryExamples36 -PtraitExamples -PaccessoryRunId=auto-laser-full-green`；`full-*-green.log` | Neo227/227；Forge224/227，旧用例失败见下 |
| 本次功能合并最终验收 | `-PcatAutoLaserProbeOnly -PcatAutoLaserWorkProbeOnly -PcatMachineCraftingProbeOnly -PaccessoryExamples36 -PtraitExamples -PaccessoryRunId=auto-laser-acceptance-ready`；`acceptance-*-ready.log` | 双端16/16 |
| 准备流程修正后的完整回归 | `-PaccessoryExamples36 -PaccessorySdkDemo -PtraitExamples -PaccessoryRunId=auto-laser-final-full`；`final-full-*.log` | 双端227/227 |

客户端命令：

```powershell
./gradlew.bat runClient --init-script ../tests/cat-machines-client.init.gradle -PcatAutoLaserClient -PcatAutoLaserWorldClient -PcatMachinesRunId=auto-laser-world-green2 --no-configuration-cache
```

双端 `client-*-world-green2.log` 均有 `PASS: AUTO LASER WORLD` 和 `PASS: AUTO LASER CLIENT`、正常退出。实际注册BE渲染器和Create客户端同步包覆盖：六向笔尖/端点，新增墙阻挡，实体ID重用但UUID不匹配，清空/断电/移除，无底座视锥但有光束时仍可见，资源重载。另检查72个远距模型顶点案例、12个近距解析瞄准案例、物品GUI边界。

真实GPU图片分别位于：

- `forge-1.20.1/build/cat-sixway-client-auto-laser-world-green2/cat-auto-laser-sixway.png`
- `neoforge-1.21.1/build/cat-sixway-client-auto-laser-world-green2/cat-auto-laser-sixway.png`

NeoForge实际JEI客户端 `client-neo-recipe-green.log` 通过四机器配方、纸板机壳应用入口及猫工作盆催化剂检查；不是仅检查JSON。

Node资源检查：`tests/cat-auto-laser-assets.mjs`、`cat-sixway-assets.mjs`、`cat-machines-assets.mjs`、`cat-casing-scope.mjs` 全部通过；`accessory-scripting.mjs`63项、`trait-scripting.mjs`97项通过。静态脚本检查不代替上表实际KubeJS回归。

## 早轮Forge全量失败和最终复验

早轮Forge全量并未通过；不能把其根因认定为已经查明或把旧机制称为已修复。最后完整复验双端227/227通过，早轮失败日志仍保留。

第一次全量227项中失败3项：普通Boss平滑追逐、真实玩家索敌追逐、工程师炮台重新展开。第二次原生全量227项中上述3项通过，另失败2项：医疗分诊、死亡Boss取消坐砸（实际生命999、期望1000）。本次11项自动激光/工作测试及5项制作测试在两轮均通过。

独立37项饰品兼容复测中炮台和医疗通过。独立35项Boss复测中上述追逐/死亡测试通过，另一个旧 `liveAiSelectsSpecialAttacksWithoutMeleeAndHonorsRecoveries` 在450tick超时，34/35通过，见 `boss-forge-baseline.log`。失败案例有波动；未改其生产逻辑或削弱断言，没有以反复重跑全量来隐藏失败。

本地功能验收与全分支整合分别报告：按新增范围、脚本兼容、实际客户端、正常构建和正式启动检查本次本地包。最后完整套件已通过，早轮旧用例波动根因仍未完全查明。按照用户已确认的计划保留分支，不合并/推送/发布。

最后新增合并验收的首次Forge14/16，坐垫查询/多机用例失败；诊断轮同样14/16，失败改为动力展开/坐垫查询。坐垫诊断明确记录 `available=true, tracked=false, chunkTicking=false`：断言执行时区块尚未进入实体tick，不应当将此当作半径算法错误。新增 **仅测试源码** `CatAutoLaserTestArea`，等待整片场景实际实体tick就绪，100tick独立准备上限；之后仍用原有行为时间窗，释放仅本测试新增的强制票据。没有加入生产区块加载、增加攻击等待时间或放宽断言。修正后双端合并16/16通过，见 `acceptance-*-ready.log`；原诊断日志保留。之后双端完整回归各227/227通过。

## 构建/部署

双端正常 `./gradlew.bat build --no-configuration-cache` 已通过：冻结饰品APIv3/36ID、词条APIv1/82ID、23270项饰品定义/状态检查及发布隔离检查。正常包不包含探针、示例脚本、SDK、测试世界或美术源目录。

Neo最后完整回归后的无初始化脚本构建也通过，见 `final-build-neo.log`。ZIP清单独立确认每包恰好一份纸板应用配方、零旧木材配方、零测试/SDK污染，包含正式机器/工作/瞄准类。

Forge正式启动命令（独立探针不混入产品）：

```powershell
./gradlew.bat reobfReleaseBootProbeJar --init-script ../tests/production-boot.init.gradle --no-configuration-cache
pwsh -File tests/run-forge-production-boot.ps1 -JarPath '<仓库绝对路径>/forge-1.20.1/build/libs/create-meowchanics-2.2.2-forge-1.20.1.jar' -RunId auto-laser-green
```

`boot-probe-build-forge.log` 成功；`production-boot-forge.log` 有 `PASS: FORGE PRODUCTION JAR BOOT`，真正SRG映射客户端、完整已安装模组组合到达标题界面并正常退出。隔离目录 `forge-1.20.1/build/production-boot-auto-laser-green/`，不复制玩家账号、世界、配置或KubeJS。

部署执行 `pwsh -File tools/deploy-cat-additions.ps1 -Version 2.2.2 -Label auto-laser`，2026-10-03 17:53:38成功；两实例 `ActiveLaowuCount=1`、`Verified=true`，安装/构建哈希相同。部署前检查客户端已退出。记录见 [deployment-auto-laser-20261003.json](../deployment-auto-laser-20261003.json)。

- Forge SHA-256：`6FB0F1E30EF67821F28878E84D879E8F1392FFCE1C542CA4F2D5FED19C392057`
- NeoForge SHA-256：`C745BEBC5E1CF8313C36A25E8D46C2C0C0D150E07C5742371DD386BF3536197B`
- 两端备份在各实例 `mod-backups/create-meowchanics/20261003-175337-2.2.2-auto-laser/`。本次脚本执行前原启用包哈希已等于构建产物，故这里备份的是当时的相同包，并非声称可以凭此撤销本次功能；更早备份未删除。

未修改存档、配置、玩家脚本或其它模组；未关闭玩家客户端。测试/诊断日志保留在忽略目录和独立测试场景，不会影响正常运行JAR或公开发布目录。

## 执行裁决

完整裁决保留在本次ledger；交付时逐项向用户说明。重要取舍：保留用户dirty基线且不提交；仅不同端构建并行；真实电机而非注入无源转速；保留作者负尺寸/零高面和原PNG；修正盖面遮蔽与底轴口；极近目标位于实体笔尖内侧时仍标记/可攻击，但不画反向光束；不添加未要求的配方/GUI/PvP；不将失败的Forge全量报告为通过。没有延期的审查Minor；3项Important均已在一次修复流程中处理并用对应行为/客户端检查验证。
