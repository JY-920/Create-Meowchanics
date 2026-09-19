# 本地部署记录：accessories.15

- 状态：已部署；部署时间 2026-09-14T16:43:42.0106364+08:00。双端从 accessories.14 升级到 accessories.15。
- 分支 develop；公开版本 2.1.4，内部版本 2.1.4-dev.accessories.15；不提交、推送或公开发布。
- 根据游戏内反馈，上版仅修正中心位置，腹板加双支架仍形成一个悬在身体下方的框，没有包住身体。本次替换为完整上束环和中央单连接件，下方扳手抓握环保持原几何与位置。
- 上束环外框 X=-3–3、Y=13–21，包住实际躯干 X=-2–2、Y=14–20；内沿浅嵌 1/8 像素。全部结构仍居中 X=0/Z=1，并跟随躯干姿态、缩放。
- 两个四边环加一根短连接件，共 9 块几何；没有额外下垂矩形。旧背包兼容、飞行手感、属性与其他职业逻辑不变。

## 校验

- Forge / NeoForge 完整构建成功，各 23,120 项饰品回归通过。
- 双端独立客户端探针通过：真实 Create 左右手悬挂动作与 Mixin、9 块几何烘焙、各 132 个真实躯干截面轮廓采样点全部被上束环覆盖。
- 上环与躯干三轴中心一致；两环均为镂空，9 块几何整体连通；下环每块几何边界与 accessories.14 一致。各 450 项转向/姿态/体型缩放附着检查通过。
- 静态检查：职业 350、飞行 133、饰品 391、美术 329、炮台 735、曲柄 73 项通过。
- JAR 校验：16 张用户素材、原有模型/配方/动画、版本与可选 KubeJS 接口完整，测试类不进入发行包。
- 本次仅改变客户端挂环几何，未改动服务端逻辑，未重跑 GameTest。客户端探针不打开用户世界，自动退出；尚未做整合包内人工画面验收。
- 部署前及暂存核验后检查无游戏/未分类 Java 进程；没有强制结束用户程序。
- 按 JAR 内自身 [[mods]] 的 modId=laowu 核对，双端部署前后各只有一个启用包。新包/构建与旧包/备份 SHA-256 分别一致。
- 没有修改用户存档、配置或其他模组，也没有删除旧备份。

## forge-1.20.1

- 新包：D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.15-forge-1.20.1.jar
- 新包 SHA-256：65DE97E15E0DEC99499FC822CB541FE382676FD0A7A49D6871DB4F0787F6A158
- 旧包备份：D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mod-backups/create-meowchanics/20260914-164341-accessories.15/create-meowchanics-2.1.4-dev.accessories.14-forge-1.20.1.jar
- 旧包 SHA-256：A9D3160EDC3E71E5CF8493E65A4E559117673441CBDBA9F59A890328D60A78FE

## neoforge-1.21.1

- 新包：D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.15-neoforge-1.21.1.jar
- 新包 SHA-256：7808EB5FF9BAB7C82959BA97BB4BDFA492B929D727E34465060244AEB95B1459
- 旧包备份：D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mod-backups/create-meowchanics/20260914-164341-accessories.15/create-meowchanics-2.1.4-dev.accessories.14-neoforge-1.21.1.jar
- 旧包 SHA-256：1C67AFF14C18BDD533D0E71BAD84984D23935DD12BB12A75C2D9EC4282C310A2

旧包通过移动保留，可在退出游戏后恢复。
