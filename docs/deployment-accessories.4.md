# 工程猫开发版 accessories.4 部署记录

部署时间：2026-09-12 22:58:52（Asia/Shanghai）。

- 本地 develop；未提交、未推送。公开版本仍为 2.1.4，内部版本为 2.1.4-dev.accessories.4。
- 工程猫在坐垫上摇动正上方一格的侧装曲柄，侧身双爪推拉，动画跟随真实曲柄角度。
- 本次使用最新调整：两侧工具箱均保留，身体向后移动 1.5 个模型像素，双爪继续贴合握杆。
- 双端构建、动作回归和打包检查通过；Blockbench 检查 721 个时刻。尚未进行完整整合包的图形客户端验收。
- 部署前确认无 Java 游戏进程；仅替换本模组 JAR，未改动其他模组、存档、配置或 KubeJS 脚本。
- 根据 JAR 元数据确认每个实例仅有一个启用的 laowu 模组包；部署后新旧包 SHA-256 全部匹配。旧包移到 mods 外备份，可恢复。

## Forge 1.20.1

- 活动包：`D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.4-forge-1.20.1.jar`
- SHA-256：`1817B93E31CEF433CFEF2D58136EA90A56728166CB443BB447936400B4C728B5`
- 旧包备份：`D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mod-backups/create-meowchanics/20260912-225851-accessories.4/create-meowchanics-2.1.4-dev.accessories.3-forge-1.20.1.jar`
- 备份 SHA-256：`09BF619A47509574AF8E9DE01CFC2723CE1D45D119650164BA8AC2560EE39F0D`

## NeoForge 1.21.1

- 活动包：`D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.4-neoforge-1.21.1.jar`
- SHA-256：`8B6697E41A5447D57655CE84E6FD0DC6420233BE5C44FAFDC08606FD8B1F3FE2`
- 旧包备份：`D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mod-backups/create-meowchanics/20260912-225851-accessories.4/create-meowchanics-2.1.4-dev.accessories.3-neoforge-1.21.1.jar`
- 备份 SHA-256：`A31817DA4F7BF8ACEA156B2CC78C42270A7EF43AA0B824B76571688BE96D83AA`
