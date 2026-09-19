# accessories.9 双端部署记录

部署时间：2026-09-14 10:03:44 +08:00。内部版本 2.1.4-dev.accessories.9，公开版本仍为 2.1.4。

本次包含：工程猫炮后坐垫与延长底板、中央出弹、32 格射程与主动索敌、高智力控距拆装；飞行员猫扳手悬挂载人、属性决定时长/速度、耗尽滑翔与地面恢复；双端套装说明与数值预览。

部署前用户确认两个开发实例已退出，随后再次检查无 Minecraft 客户端。按包内 modId=laowu 核对每端仅一个启用版本，旧 .8 移入备份，新包与构建产物及旧包与备份均通过 SHA-256 校验。未修改存档、配置和其他模组，未提交、推送或发布公开版本。

## forge-1.20.1

新包：`D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.9-forge-1.20.1.jar`

SHA-256：`BED63F4D0069DDDEEBD89D9DF7FA1B618848F842B6E9D9581F87518A65106494`

旧包备份：`D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mod-backups/create-meowchanics/20260914-100343-accessories.9/create-meowchanics-2.1.4-dev.accessories.8-forge-1.20.1.jar`

旧包 SHA-256：`3AC7C7D3E46BBF401F535067D9BD75D786F4DB3508FC5F5DCE9C09CE6688D7FB`

## neoforge-1.21.1

新包：`D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.9-neoforge-1.21.1.jar`

SHA-256：`E10F755FAC3149E33CF82D6936AEABEAB8E27F3CD16D71F1550ED45D59FF2B3C`

旧包备份：`D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mod-backups/create-meowchanics/20260914-100343-accessories.9/create-meowchanics-2.1.4-dev.accessories.8-neoforge-1.21.1.jar`

旧包 SHA-256：`62441644C0F317E4EC67C6B970BB65E8A5C7BF2D62F5A9D415DF95BE372A7995`

## 验证结果

- 双端完整 build 成功，各 23,120 项饰品检查通过。
- 两端均完成安装 KubeJS 与不安装 KubeJS/Rhino 的运行测试，每组 9 项通过。覆盖真实激光指令、炮台贴脸命中/追击/保存、智能退后/追近、32 格主动索敌、载人飞行与恢复、原曲柄工作、饰品与脚本事件。
- 独立图形客户端在不打开存档的情况下，验证真实 Create PlayerSkyhookRenderer 的 Mixin 应用以及左右手悬挂姿态计算；资源加载后正常退出。尚未进行完整整合包中的飞行手感和画面人工验收。
- 飞行/控距规则每端 31,807 个组合；743 项炮台接线/坐垫锚点检查、85 项飞行接线检查通过。
- 全局配置、职业参数、激光轮盘、已有饰品及各端 8,790,481 项原曲柄姿态回归通过。
- 260 项成品 Mixin 包检查通过；16 张用户重绘饰品、曲柄/街舞/琵琶动画资源核对通过，没有测试类或新的强制 Photon/LDLib/KubeJS 依赖。

测试设施另作了两项修正：Forge 源码运行显式加载生产中使用的 Mixin 配置；大场景下将曲柄探针保持在测试原点的持续更新区块，避免跨区块的方块实体停更误报。没有因此修改游戏内曲柄机制。

回退时先退出游戏，将 .9 移出 mods，再将对应 .8 备份放回；不要同时启用两个版本。

详见[工程炮台](cat-engineering-artillery.md)、[飞行员载人](cat-pilot-passenger-flight.md)和[开发更新日志](changelog-develop.md)。
