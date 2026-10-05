# 大猫猫 Boss 追逐转向修复（2.2.2）

日期：2026-10-03。仅修改 Boss 普通追逐控制；技能数值、阶段、毛色、模型资源、掉落和公开 KubeJS API 不变。累计玩家更新内容见 `changelog-2.2.2.md`。

## 根因与修复

1. 近身接近手动转向后，原版 MoveControl 又可以按路径点瞬间转向最多 90°。真实双端 AI 测试均记录到单 tick 90° 急拐。现在近身接近与路径跟随共用一个控制器，每 tick 最多转 10°，未对齐时先转身。
2. 原版身体控制器在停步时跟随头部观察，开始走路时又跳回移动方向。双端真实实体测试记录到一次 120° 身体跳转。普通状态的模型身体和鼻部伤害判定现在统一使用平滑实体朝向；翻滚的独立侧向朝向不受影响。
3. 方格路径的短折点造成额外左右修正。增加最多三节点／3.2 格的短距离前瞻；检查完整身体扫掠碰撞、脚底支撑、已加载区块和原版危险地形分类，才能略过节点。危险节点、门、台阶及未加载区域退回原寻路，不强行直线穿过。
4. 原版 MoveControl 可在停止水平移动前排入普通台阶跳跃，随后 JUMPING 状态保留旧方向。对齐判断移至原版请求跳跃之前；已对齐的移动及正常跳跃继续交给原版处理。

两项地形边界先在 NeoForge 真实导航／移动控制入口复现：路径直接略过节点至岩浆块上方，反向面对台阶时提前排入 JumpControl。对应 RED 证据：`neoforge-1.21.1/build/boss-pursuit-222-terrain-red.log`。Forge 同轮尝试在构建工具外部连通性预检中停滞，尚未执行测试，不计为 Forge RED 证据。

测试“转一圈”使用连续展开后的角度跨度，而非所有微小左右修正的绝对角度总和；同时保留每 tick 转角、身体转角、追逐目标方向偏差及真正咬伤断言。最初的绝对角度总量判据把约 233° 的累计路径修正误算为回转，已更正测试定义，未放宽 90°／120° 急转检测。

## 验证入口

完整服务器套件使用真实 KubeJS、36 饰品替换脚本、新物品注册示例和词条例程。新增五项 Boss 测试覆盖：斜向追逐、绕墙抵达、跨 ±180° 朝向、动态危险地面及普通台阶起跳对齐；原有真实玩家索敌、近身重新追赶、矮猫接触伤害、翻滚、坐砸、存档和掉落测试保留。

客户端通过实际客户端身体更新入口检查朝向，并渲染 Boss 九个阶段与中断恢复共 100 帧 GPU 图像。普通台阶新用例验证移动／跳跃控制的真实入口，没有夸大为所有自然上坡地形组合的验证。

复验（各加载器目录内）：

```powershell
.\gradlew.bat runGameTestServer --init-script ../tests/accessory-gametest.init.gradle -PaccessoryExamples36 -PaccessorySdkDemo -PtraitExamples -PaccessoryRunId=boss-pursuit-222-recheck --no-configuration-cache --console=plain
.\gradlew.bat runClient --init-script ../tests/cat-machines-client.init.gradle -PcatSixWayClient -PgiantVisualOnly -PcatMachinesRunId=boss-pursuit-222-recheck --no-configuration-cache --console=plain
.\gradlew.bat build --no-configuration-cache --console=plain
```

Forge Java 17，NeoForge Java 21。Forge 最终验证使用已缓存依赖 `--offline '-Dnet.minecraftforge.gradle.check.certs=false'`，仅跳过 ForgeGradle 对两个外部站点的预检；未改变 TLS 信任、JDK 安全设置或项目文件，未关闭模组测试、API 契约、动画及发布隔离检查。此前等待预检的本任务 Gradle wrapper／daemon 被终止，当时尚未启动游戏；没有关闭玩家客户端。

最终测试、构建、正式环境启动、JAR 哈希和部署结果记录在 `deployment-2.2.2.json`。运行包与测试探针、世界、脚本和 SDK 保持隔离。

## 最终结果

- Forge／NeoForge 完整服务器套件各 215／215 通过；两端客户端朝向、骑乘与 Boss 九阶段 100 帧渲染验证通过。
- 两端无测试初始化脚本的正常发布构建通过，包含 23270 项饰品校验、冻结 API、动画及发布隔离检查。
- Forge 真实 SRG 正式客户端使用已安装模组集合启动到标题界面，正常退出。
- 正式 2.2.2 已于 2026-10-03 09:50 部署，两实例各仅一份 `laowu` JAR；源文件、安装文件和双端压缩包内部 JAR 的 SHA-256 一致。
- 上一份 2.2.1 JAR 移至两实例的 `mod-backups/create-meowchanics/20261003-095001-2.2.2-boss-pursuit/`，可恢复；未提交或推送仓库。
