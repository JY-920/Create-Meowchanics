# 饰品开发版 accessories.3 部署记录

部署时间：2026-09-12 21:04（Asia/Shanghai）。

- 本地 develop；本次未提交、未推送。公开版本仍为 2.1.4，内部版本为 2.1.4-dev.accessories.3。
- 双端均已替换，仅修改本模组 JAR；未改动其他模组、存档或用户 KubeJS 脚本。
- 每个实例仅启用一个本模组包，旧 accessories.2 包移到 mods 外的 mod-backups 目录，已核对新旧包哈希，可恢复。

## Forge 1.20.1

- 新包：`D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.3-forge-1.20.1.jar`
- SHA-256：`09BF619A47509574AF8E9DE01CFC2723CE1D45D119650164BA8AC2560EE39F0D`
- 旧包：`D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mod-backups/create-meowchanics/20260912-210458-accessories.3/create-meowchanics-2.1.4-dev.accessories.2-forge-1.20.1.jar`

## NeoForge 1.21.1

- 新包：`D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.3-neoforge-1.21.1.jar`
- SHA-256：`A31817DA4F7BF8ACEA156B2CC78C42270A7EF43AA0B824B76571688BE96D83AA`
- 旧包：`D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mod-backups/create-meowchanics/20260912-210458-accessories.3/create-meowchanics-2.1.4-dev.accessories.2-neoforge-1.21.1.jar`

## 内容与验证

- 16 件饰品使用简化名称及独立 16×16 AI 草稿贴图；新增黄油猫饰品战利品黄油块。
- 工程、医疗、音乐猫套装及中间件双端注册，目前仅外观，不启用工作和战斗功能。
- 双端正式构建成功，各通过 23,120 项饰品核心检查；真实 Minecraft GameTest 各通过 2 项，涵盖 KubeJS 接口、新职业保存和黄油块机制。
- 艺术资源与新职业静态检查 283 项通过，打包检查及既有回归检查通过。KubeJS 仍为可选依赖，未引入 Photon/LDLib。
- 部署前确认游戏已退出，部署后校验活动包唯一性及 SHA-256。尚未进行完整整合包的图形客户端或多人联机验收。
