# 本地部署记录：accessories.14

- 状态：已部署；部署时间 2026-09-14T16:21:21.7992408+08:00。双端从 accessories.13 升级到 accessories.14。
- 分支 develop；公开版本 2.1.4，内部版本 2.1.4-dev.accessories.14；不提交、推送或公开发布。
- 仅修复飞行猫旧背包兼容入口与挂环居中，更新中英文套装描述；保留 accessories.13 的平衡与全部既有功能。
- 旧背包采用非空时正常打开的兼容方案，不自动删除物品，不限制已经打开的普通整理操作。取空并关闭后不再打开，物流猫不受影响。
- 机腹支架与挂环对齐真实长方形躯干 X=0/Z=1，跟随实时躯干姿态和缩放；腹板贴合 Y=20 腹部，挂环保持 Y=24–30 的原举手高度。

## 校验

- 双端完整构建成功，各 23,120 项饰品回归通过。
- Forge / NeoForge 有 KubeJS、无 KubeJS 共四组真实 GameTest，各 14 项全部通过；新增旧背包交互、所有 27 格物品/耐久保存、重载、主人权限、Shift 取回、取空关闭后的入口关闭、物流猫不回退测试。
- 双端独立客户端探针成功：真实 Create 左右手悬挂动作与 Mixin、七块支架挂环烘焙，各 450 项基于真实 ModelPart 的躯干居中/姿态/缩放检查。探针不打开用户世界，完成后自动退出；没有进行整合包内人工画面验收。
- 静态接线检查：职业 350、飞行 133、饰品 391、美术 329、炮台 735、曲柄 73 项全部通过。
- JAR 校验通过：原有 16 张用户贴图及动画、配方、可选 KubeJS 接口保持完整，测试类不泄漏进发行包。
- 部署前与暂存核验后确认无游戏或未分类 Java 进程；没有强制结束用户程序。
- 按 JAR 内自身 [[mods]] 的 modId=laowu 扫描，部署前后每个实例均仅一个启用版本。
- 新包与构建产物 SHA-256 一致，旧包与备份 SHA-256 一致；没有修改用户存档、配置或其他模组，没有删除旧备份。

## forge-1.20.1

- 新包：D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.14-forge-1.20.1.jar
- 新包 SHA-256：A9D3160EDC3E71E5CF8493E65A4E559117673441CBDBA9F59A890328D60A78FE
- 旧包备份：D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mod-backups/create-meowchanics/20260914-162121-accessories.14/create-meowchanics-2.1.4-dev.accessories.13-forge-1.20.1.jar
- 旧包 SHA-256：DA446E89A18C589A53D03AE0BE4B7F11FEBC9255E612A42D416850E71E8DCA1A

## neoforge-1.21.1

- 新包：D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.14-neoforge-1.21.1.jar
- 新包 SHA-256：1C67AFF14C18BDD533D0E71BAD84984D23935DD12BB12A75C2D9EC4282C310A2
- 旧包备份：D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mod-backups/create-meowchanics/20260914-162121-accessories.14/create-meowchanics-2.1.4-dev.accessories.13-neoforge-1.21.1.jar
- 旧包 SHA-256：3DD8C2EF050D60C187CD132F786A2F143AD813441295D3BD56A6F987B5EB0227

旧包通过移动保留，可在退出游戏后恢复。
