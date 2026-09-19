# 饰品开发版部署记录

部署时间：2026-09-12 17:41（Asia/Shanghai）。

- 分支：本地 develop；本次未提交、未推送。
- 公开版本仍为 2.1.4；实际部署内部版本为 2.1.4-dev.accessories.2。
- 只替换 Create: Meowchanics 模组，没有安装、更新或移除其他模组，没有修改存档或用户 KubeJS 脚本。
- 每个实例 mods 中仅保留一个启用的本模组 JAR。原 2.1.4 JAR 移至实例内 mod-backups 目录，可恢复。

## 部署包

### forge-1.20.1

- 当前包：`D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.2-forge-1.20.1.jar`
- 原包备份：`D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mod-backups/create-meowchanics/20260912-174138-accessories.2/create-meowchanics-2.1.4-forge-1.20.1.jar`
- 当前包 SHA-256：`D11FBC660D78E49EE1A8D75DB6D403CB72284F99D1796A134B28322AE2C6A351`

### neoforge-1.21.1

- 当前包：`D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.2-neoforge-1.21.1.jar`
- 原包备份：`D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mod-backups/create-meowchanics/20260912-174138-accessories.2/create-meowchanics-2.1.4-neoforge-1.21.1.jar`
- 当前包 SHA-256：`61FB27A2B63D318FD5F34939043D0105E7C8B408F4D11060E5D4C60262E557B4`

## 验证结果

- Forge 与 NeoForge 正式打包任务均成功，各通过 23,114 项饰品定义、装配、概率及持久状态检查。
- 双端真实 Minecraft GameTest：安装 KubeJS 时，验证自定义物品、装卸回调、属性、冷却、哈气值、伤害回调及 /reload 不重复监听；均通过。
- 双端分别运行未安装 KubeJS/Rhino 的 GameTest，也通过。KubeJS 为可选依赖，不是强制前置。
- 打包检查通过：15 件内置饰品定义、模型及配方完整；KubeJS 插件入口存在，但没有打入 KubeJS/Rhino 或测试探针，未引入 Photon/LDLib 依赖。
- 保留街舞与琵琶动画资源兼容修复；既有配置、套装与繁育回归检查通过。
- 部署后重新核对新包与旧包备份 SHA-256，并确认不存在重复启用包。

尚未进行完整整合包的图形客户端及多人联机验收；重启游戏后重点检查饰品槽、属性显示及多人战斗。

接口、限制和可直接参考的机制脚本见 [KubeJS 接口说明](cat-accessory-scripting.md)。目前哈气值提供保存、消耗和补充接口；没有自动流体充能配方，脚本需要自行扣除充能材料。
