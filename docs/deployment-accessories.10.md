# accessories.10 双端部署记录

部署时间：2026-09-14T10:47:31.7458600+08:00。内部版本 2.1.4-dev.accessories.10，公开版本仍为 2.1.4。

本次包含：
- 飞行员载人朝向立即跟随本地视角，每 tick 发送输入；仅平滑速度大小，不再保留旧运动方向。
- 飞行时长限制为 5–30 秒：min(30, 5 + 0.25 × 有效耐力)，保留耗尽滑翔与落地恢复，兼容旧版较大的体力消耗。
- 不再为脱离的玩家附加缓降状态，不移除其他来源的已有缓降。
- 载人时显示机腹支架和镂空挂环。
- 工程、医疗、音乐的未完成构件从创造栏隐藏，保留注册、素材、物品 ID 和存档数据。
- 工程与飞行员套装使用其他职业一致的简短说明、Ctrl 三节数值和 Shift 示例。

部署前重新确认没有游戏客户端或其他未确认 Java 进程。先将新包暂存并校验，再把旧 .9 移入备份并启用新 .10。两端按 JAR 内 modId=laowu 各确认一个启用版本，新包/构建产物、旧包/备份的 SHA-256 均一致。没有修改存档、配置或其他模组，没有提交、推送或发布新公开版本。

## forge-1.20.1

新包：`D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.10-forge-1.20.1.jar`

SHA-256：`18CD20591335220C1CBC10C95D56A8B956E0AA5F309D9D4404855DD4FB5D9DB8`

旧包备份：`D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mod-backups/create-meowchanics/20260914-104731-accessories.10/create-meowchanics-2.1.4-dev.accessories.9-forge-1.20.1.jar`

旧包 SHA-256：`BED63F4D0069DDDEEBD89D9DF7FA1B618848F842B6E9D9581F87518A65106494`

## neoforge-1.21.1

新包：`D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.10-neoforge-1.21.1.jar`

SHA-256：`A1B3A5ABB7387D393E642C46EA4DF4A4E91A15826AA6033E671073DE7F08CE39`

旧包备份：`D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mod-backups/create-meowchanics/20260914-104731-accessories.10/create-meowchanics-2.1.4-dev.accessories.9-neoforge-1.21.1.jar`

旧包 SHA-256：`E10F755FAC3149E33CF82D6936AEABEAB8E27F3CD16D71F1550ED45D59FF2B3C`

## 验证

- 双端完整 build 成功，各 23,120 项饰品检查通过。
- 双端分别在安装 KubeJS 与未安装 KubeJS/Rhino 的隔离运行环境通过全部 9 项 GameTest；覆盖原曲柄/炮台、真实载人、体力/滑翔/恢复、旧消耗迁移、缓降隔离、隐藏构件仍注册以及饰品脚本事件。
- 每端 32,432 个飞行与控距组合，包括急转/反向；修正测试中的目标方向归一化，避免原版三角函数查表造成长度误差。
- 双端独立客户端启动并正常退出，真实 Create 左右手悬挂姿态与 Mixin 应用成功；支架、支撑杆、挂环共七个方体成功烘焙，范围校验通过。不打开用户存档。
- 121 项飞行接线、735 项炮台接线、329 项素材/职业、346 项职业伤害、391 项饰品接线、73 项工程工作接线与 123 项激光轮盘接线检查通过。激光测试更新了旧的乘客断言，采用现行“只有炮台乘客可受令、飞行及其他乘客不可受令”的规则，未为此修改生产行为。
- 每端 3,844 项圈姿态与 8,790,481 曲柄姿态检查通过。
- 260 项成品 Mixin 检查通过；动画资源与 16 张用户重绘饰品校验通过，无测试类泄漏或新增强制 Photon/LDLib/KubeJS 依赖。

尚未进行完整整合包里的人工画面/飞行手感验收；客户端探针验证的是资源烘焙与真实姿态计算。

需要回退时先退出游戏，将 .10 移出 mods，再放回对应 .9 备份，不要同时启用两个版本。

详见[载人说明](cat-pilot-passenger-flight.md)和[开发更新日志](changelog-develop.md)。
