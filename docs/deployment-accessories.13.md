# 本地部署记录：accessories.13

- 部署时间：2026-09-14T16:01:31.9148616+08:00
- 分支 develop；公开版本 2.1.4，内部版本 2.1.4-dev.accessories.13；未提交、推送或公开发布。
- 双端从 accessories.11 升级至 accessories.13，包含先前 accessories.12 已完成但因客户端运行而暂缓安装的全部功能。
- 本次默认平衡见[说明](career-balance-accessories.13.md)：飞行额外 +30 生命/+7 护甲/+3 韧性；喷火、采蜜、雷管输出调整与物流 +10 护甲；钓鱼全部参数不改。
- 音乐战斗/工作、医疗驻点工作仅提出设计建议，未启用或实装。

## 校验

- 双端完整构建成功，各 23,120 项饰品回归通过。
- Forge / NeoForge 分别在有 KubeJS、无 KubeJS 环境运行真实 GameTest，四组各 13 项全部通过。
- 无 KubeJS 组新增实体断言：50 基础属性飞行猫实际获得 60 生命、17 护甲、5.5 韧性。
- 双端配置回归全部通过，各 137,231 项套装检查；包含 revision 0/1 升级、所有相关参数自定义组合、整组保留、全局覆盖、钓鱼默认/自定义数据不变、无关开关保留与不重复迁移。
- 静态检查：职业 350、载人飞行 121、饰品接线 391、饰品美术 329、炮台 735、曲柄 73 项通过。
- JAR 校验通过：原有 16 张用户饰品贴图、模型、配方和动画保留；辅助/三弹药/曲柄 Mixin 正确打包，无测试类或测试资源泄漏，没有新增必装前置。
- 部署前、暂存核验后均确认没有游戏/非 Gradle Java 进程；不结束用户程序。
- 按每个启用 JAR 内 [[mods]] 的 modId=laowu 扫描，部署前后每个实例均仅一个启用版本。
- 新包/构建产物 SHA-256 一致，旧包/备份 SHA-256 一致。没有修改存档、配置或其他模组。旧配置升级在下次正常加载世界时按安全迁移规则执行。
- 本次没有启动完整图形客户端做视觉验收；游戏机制验证使用隔离 GameTest。

## forge-1.20.1

- 新包：D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.13-forge-1.20.1.jar
- 新包 SHA-256：DA446E89A18C589A53D03AE0BE4B7F11FEBC9255E612A42D416850E71E8DCA1A
- 旧包备份：D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mod-backups/create-meowchanics/20260914-160131-accessories.13/create-meowchanics-2.1.4-dev.accessories.11-forge-1.20.1.jar
- 旧包 SHA-256：42552597123490B191FE08E98E4026426FE70930B2E3620FA30F1F554A1BE4ED

## neoforge-1.21.1

- 新包：D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.13-neoforge-1.21.1.jar
- 新包 SHA-256：3DD8C2EF050D60C187CD132F786A2F143AD813441295D3BD56A6F987B5EB0227
- 旧包备份：D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mod-backups/create-meowchanics/20260914-160131-accessories.13/create-meowchanics-2.1.4-dev.accessories.11-neoforge-1.21.1.jar
- 旧包 SHA-256：F2D67AF1E7FB31F40BCC66DB05F68568BE3E16BCA4DFD117735D3BA1E355009D

旧包通过移动保留，可在退出游戏后恢复；没有删除任何旧版本备份。
