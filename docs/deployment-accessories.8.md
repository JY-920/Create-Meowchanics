# 工程猫炮台版 accessories.8 部署记录

部署时间：2026-09-13 13:08:49 +08:00。公开版本仍为 2.1.4。

双端测试实例已由 accessories.7 更新为 2.1.4-dev.accessories.8。本包包含工程猫 16 格原地炮台战斗、队伍色炮顶坐垫、齿轮弹丸、套装数值配置及饰品/KubeJS 接入，保留原有摇曲柄工作。

部署前确认游戏未运行，重新完成 JAR 资源及依赖校验。新包与构建产物、旧包与备份均核对 SHA-256；部署后按包内 modId 检查每端仅有一个启用的 laowu JAR。未修改存档、配置或其他模组，未提交、推送或发布公开版本，未启动图形客户端验收。

## Forge 1.20.1

新包：`D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.8-forge-1.20.1.jar`

SHA-256：`3AC7C7D3E46BBF401F535067D9BD75D786F4DB3508FC5F5DCE9C09CE6688D7FB`

旧包备份：`D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mod-backups/create-meowchanics/20260913-130848-accessories.8/create-meowchanics-2.1.4-dev.accessories.7-forge-1.20.1.jar`

旧包 SHA-256：`6205415CC9A0802F3F8C3E94620E8C44DB7EF8C6B896905FD7F33CF7602309BD`

## NeoForge 1.21.1

新包：`D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.8-neoforge-1.21.1.jar`

SHA-256：`62441644C0F317E4EC67C6B970BB65E8A5C7BF2D62F5A9D415DF95BE372A7995`

旧包备份：`D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mod-backups/create-meowchanics/20260913-130848-accessories.8/create-meowchanics-2.1.4-dev.accessories.7-neoforge-1.21.1.jar`

旧包 SHA-256：`DA27985227AB24471112F91D57A869BDD8AD944989EFEE1E80A4A392949E5A08`

## 后续约定与回退

已按用户要求将“完成双端验证后默认直接部署”的规则写入项目 AGENTS.md。游戏运行中仍须先退出；不自动推送远端。

回退时先退出游戏，将本次 .8 新包移出 mods，再把对应 .7 备份放回。不要同时启用两个版本。详细功能和测试结果见[工程猫炮台战斗](cat-engineering-artillery.md)。
