# 本地部署记录：accessories.16

- 状态：已部署；部署时间 2026-09-14T17:07:48.5308344+08:00。双端从 accessories.15 升级到 accessories.16。
- 分支 develop；公开版本 2.1.4，内部版本 2.1.4-dev.accessories.16；不提交、推送或公开发布。
- 根据用户截图，移除下环与中央连接件，只保留包身单环。实际腹部底面 Y=20 到横杆内沿 Y=23 留 3 像素空间，在用户要求的 2–4 像素范围内。
- 横杆为 Y=23–24.125；背部与两侧仍包身并保持 X=0/Z=1 对齐，全结构从 9 块缩为 4 块。
- 相对悬挂高度 2.35 → 2.5 格；承载碰撞箱高度 3.15 → 3.3 格，起飞净空 3.2 → 3.35 格（提示约 3.4）。用于配合真实扳手开口，不修改原 Create 姿态、不拉伸玩家手臂。
- 旧背包兼容、5–30 秒飞行、速度/耐力/战斗参数不变。

## 校验

- Forge / NeoForge 完整构建成功，各 23,120 项饰品回归通过。
- 双端客户端各 132 项单环闭合轮廓检查、腹下三像素净空扫描、450 项旋转/姿态/缩放附着检查通过，确保没有下环、连接件或悬空几何。
- 从真实 Create 扳手 JSON 读取钳口，使用资源加载后的实际烘焙第三人称变换、原悬挂姿态和玩家模型，验证左右手 × 普通/纤细手臂共四种组合。钳口中心换算到猫模型空间 Y≈23.53–24.06，均落入横杆 Y=23–24.125 内，X/Z 同样命中；不是以手掌或整个扳手包围盒代替钳口检查。
- 双端无 KubeJS 隔离 GameTest 各 14 项全部通过，包含真实挂载、校准后的 2.5 格高度、碰撞箱包含猫、飞行/滑翔/落地、持久化与旧背包兼容。本次未重跑有 KubeJS 组合。
- Forge 首次完整回归中曲柄离座断言失败。隔离测试场景发现并清理 13 个残留非玩家实体；增加仅限该五格测试场景的启动清理和结束清理后，双端重跑全部通过。未改动生产曲柄逻辑，清理没有涉及用户存档。
- 静态检查：职业 350、飞行 133、饰品 391、美术 329、炮台 735、曲柄 73 项通过。
- JAR 校验确认 16 张用户图标、模型/配方/动画与可选 KubeJS 接口完整，测试类未进入发行包。
- 客户端探针不打开用户世界，完成后自动退出；以上为实际模型/变换检查，尚未进行整合包内人工画面验收。
- 部署前与暂存核验后确认无游戏或未分类 Java 进程，未强制结束任何用户程序。
- 按每个 JAR 自身 [[mods]] 的 modId=laowu 核对，双端部署前后各仅一个启用包；新包/构建、旧包/备份 SHA-256 分别一致。
- 没有修改用户存档、配置或其他模组，没有删除旧 JAR 备份。

## forge-1.20.1

- 新包：D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.16-forge-1.20.1.jar
- 新包 SHA-256：3902DC136E267E19D7D93D66EBE4B7DD4F86E575A543A1A29A29F5B568E86C4C
- 旧包备份：D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mod-backups/create-meowchanics/20260914-170748-accessories.16/create-meowchanics-2.1.4-dev.accessories.15-forge-1.20.1.jar
- 旧包 SHA-256：65DE97E15E0DEC99499FC822CB541FE382676FD0A7A49D6871DB4F0787F6A158

## neoforge-1.21.1

- 新包：D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.16-neoforge-1.21.1.jar
- 新包 SHA-256：F3F34A2482D3D4E3AB46FB1FC7E0D61AB1FB0DF871ECAAC482EF106D6BBE7023
- 旧包备份：D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mod-backups/create-meowchanics/20260914-170748-accessories.16/create-meowchanics-2.1.4-dev.accessories.15-neoforge-1.21.1.jar
- 旧包 SHA-256：7808EB5FF9BAB7C82959BA97BB4BDFA492B929D727E34465060244AEB95B1459

旧包通过移动保留，可在退出游戏后恢复。
