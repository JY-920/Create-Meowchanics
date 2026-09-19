# 饰品开发版 accessories.7 部署记录

部署时间：2026-09-13 10:31:42 +08:00。公开版本仍为 2.1.4。

Forge 1.20.1 与 NeoForge 1.21.1 测试实例均由 accessories.4 更新至 2.1.4-dev.accessories.7。部署前确认游戏已退出；部署后按包内 modId 扫描，每端仅有一个启用的 laowu 模组包。新包和旧包备份均通过 SHA-256 校验。未修改存档、配置或其他模组，未提交或推送 Git。

本包包含：16 张用户重绘饰品贴图、六件属性挂饰命名、独立猫咪饰品创造栏、饰品提示排序，以及此前尚未部署的工程猫刚性身体起身与腿部平滑过渡。

## forge-1.20.1

新包：`D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.7-forge-1.20.1.jar`

SHA-256：`6205415CC9A0802F3F8C3E94620E8C44DB7EF8C6B896905FD7F33CF7602309BD`

旧包备份：`D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mod-backups/create-meowchanics/20260913-103141-accessories.7/create-meowchanics-2.1.4-dev.accessories.4-forge-1.20.1.jar`

旧包 SHA-256：`1817B93E31CEF433CFEF2D58136EA90A56728166CB443BB447936400B4C728B5`

## neoforge-1.21.1

新包：`D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.7-neoforge-1.21.1.jar`

SHA-256：`DA27985227AB24471112F91D57A869BDD8AD944989EFEE1E80A4A392949E5A08`

旧包备份：`D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mod-backups/create-meowchanics/20260913-103141-accessories.7/create-meowchanics-2.1.4-dev.accessories.4-neoforge-1.21.1.jar`

旧包 SHA-256：`8B6697E41A5447D57655CE84E6FD0DC6420233BE5C44FAFDC08606FD8B1F3FE2`

## 验证

- 双端正式打包流程和每端 23,120 项饰品回归检查通过。
- 每端 4 项隔离 Minecraft GameTest 通过，包括独立创造栏、普通/自定义/停用饰品提示顺序。
- 部署前重新验证包内 16 张贴图与用户原图校验值一致，名称和创造栏翻译完整。
- 两端部署文件与已验证构建产物一致。未启动完整图形客户端进行本次验收。

需要回退时先退出游戏，将本次新包移出 mods，再把对应备份的 accessories.4 JAR 放回；不要同时保留两个启用版本。
