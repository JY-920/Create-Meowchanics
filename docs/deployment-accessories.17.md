# accessories.17 部署记录

- 完成时间：2026-09-14T18:33:01.0706833+08:00
- 内部构建：2.1.4-dev.accessories.17；公开版本仍为 2.1.4。
- 部署前确认无游戏或未分类 Java 进程；两端旧包均先备份，未改动存档、配置及其他模组。
- 安装后按包内 [[mods]] 的 modId=laowu 校验，每个实例仅启用一个版本；新包与构建产物 SHA-256 相同，备份与旧包 SHA-256 相同。

## 本次变更

- 飞行员支持 Ctrl 下降（控制设置可改键），Space 上升、潜行脱离；同时按升降抵消垂直控制，Ctrl 不会被起飞自动抬升覆盖。
- 满体力时长为 5 + 0.25 × 耐力 秒，航速为 4 + 0.06 × 速度 格/秒；保留至少 5 秒，去掉 30 秒、24 格/秒和最终向量 1.5 格/tick 上限。时长、已用 tick 和同步剩余秒数采用 long，兼容旧 int NBT。碰撞、区块、建筑高度和服务端输入校验保留。
- HUD 简化为“飞行 N秒”/“滑翔”，净空不足仅显示“空间不足”，其他资格失败不误报净空。
- 医疗猫搜索 16 格，靠近伤猫到 2.5 格后原地展开半径 4 格群疗；1 秒准备后，每个符合条件且无遮挡的友方职业患者每秒回复 1 生命。共享患者持久化时间戳保证多医疗猫不叠加。
- 医疗猫采用指定 forge-vfx 反转术式的低位张爪/呼吸动作、真实姿态绿色轮廓和五个上升十字，并显示地面治疗圈；删除旧连线粒子。仅施法者静止，受疗猫不冻结 AI 或动作。独立资源，无咒术模组运行依赖。
- 中英文套装说明更新，保留旧背包兼容、单环挂点和既有平衡。
- 网络协议：Forge 24、NeoForge 17，联机需服务端和客户端都更新。

## 验证

- 双端 build 通过，各 23,120 项饰品检查，动画资源/GeckoLib 扫描兼容检查及正式 JAR 校验通过。
- Forge / NeoForge × 有 / 无 KubeJS：四组各 15 项真实 GameTest 全通过，包括群疗、慢速不叠加、准备时间、患者 NBT 冷却、范围/墙体限制、施法停止、患者独立 AI 与视觉失效；原物流、炮台、存档和飞行兼容回归也通过。
- 飞行数学测试覆盖大于旧上限的属性、long 时长、Ctrl 在所有仰角下降、双键抵消、急转/滑翔等。
- 双端隔离客户端实际加载着色器并在 GPU 上对猫模型网格绘制绿色轮廓与十字：均检出 1,262 个绿色特效像素；原紫色演奏轮廓均检出 1,174 个紫色像素。已查看两端输出。此探针验证渲染组成，不等于完整整合包场景或多人手感验收。
- 客户端原挂环检查：各 132 项单环轮廓、450 项附着、4 项真实 Create 扳手开口接触，通过。
- 完整全局/存档配置回归通过，含每端 137,231 项套装参数、迁移与自定义保留检查。
- 静态接线：群疗 49、飞行 133、职业伤害 350、饰品 391、美术 329、炮台 735、曲柄 73；diff 空白检查通过。
- 测试代码及 MedicalVisualProbe 均未进入部署 JAR。未提交、推送或公开发布。

## 文件与校验

### forge-1.20.1

- 新包：`D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.17-forge-1.20.1.jar`
- SHA-256：`0816706941A025E9D169F134D2264FB4A348D4C7837D28E7C11D788A8430A4D0`
- 旧包备份：`D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mod-backups/create-meowchanics/20260914-183300-accessories.17/create-meowchanics-2.1.4-dev.accessories.16-forge-1.20.1.jar`
- 备份 SHA-256：`3902DC136E267E19D7D93D66EBE4B7DD4F86E575A543A1A29A29F5B568E86C4C`

### neoforge-1.21.1

- 新包：`D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.17-neoforge-1.21.1.jar`
- SHA-256：`5B0A82E881069C1C2A02A6CE1239FD66E30F2C7920BD463F598026A2EB3AC46A`
- 旧包备份：`D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mod-backups/create-meowchanics/20260914-183300-accessories.17/create-meowchanics-2.1.4-dev.accessories.16-neoforge-1.21.1.jar`
- 备份 SHA-256：`F3F34A2482D3D4E3AB46FB1FC7E0D61AB1FB0DF871ECAAC482EF106D6BBE7023`
