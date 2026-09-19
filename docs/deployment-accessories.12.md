# 本地部署状态：accessories.12

日期：2026-09-14。分支 develop；公开版本仍为 2.1.4，内部版本 2.1.4-dev.accessories.12。未提交、推送或发布正式版。

## 状态：已随 accessories.13 完成部署

本版当时检测到 PID 9660 的 Minecraft 客户端，暂缓安装，没有结束用户进程或在游戏运行中替换 JAR。2026-09-14 16:01 确认客户端已退出后，本版全部功能随 accessories.13 一同部署，两端从 accessories.11 升级至 accessories.13。实际安装包、旧包备份和哈希见[最新部署记录](deployment-accessories.13.md)。下方保留 accessories.12 的历史构建检查与部署前状态。

## 功能与验证

新增耐力 × 128 SU 曲柄输出、普通猫低伤害公式、三类炮弹与速度攻速、辅助职业/医疗 AI、物流 1 秒施放与同目标 5 秒冷却。详细规则及测试说明见[辅助职业与三种弹药](career-support-and-munitions.md)。

- Forge / NeoForge 完整构建成功，各 23,120 项饰品回归通过。
- 双端分别在安装 KubeJS 和不安装 KubeJS 两种隔离环境中，各 13 项真实 GameTest 全部通过。
- 完整全局/存档配置与套装参数回归通过，双端各 136,333 项套装检查；旧默认值迁移与自定义保留通过。
- 双端各 8,790,481 项原曲柄姿态回归通过。
- JAR 校验通过：新辅助、弹药、曲柄接口与 Mixin 类和注册存在；16 张用户贴图与素材清单 SHA-256 相同，原 ID、配方和动画保留，无测试类泄漏或新必装前置。
- 双端四份语言文件与资源同步完成；使用常规生产构建重新打包，不将测试启动参数或 KubeJS 测试数据带入成品。
- 构建使用 --offline；NeoForge 使用 --no-configuration-cache，避开既有动画资源验证任务与 Gradle 配置缓存不兼容的问题。
- 没有进行完整整合包图形客户端的医疗特效视觉验收。

## 已验证构建产物

| 平台 | 构建路径（相对仓库） | SHA-256 |
| --- | --- | --- |
| Forge | forge-1.20.1/build/libs/create-meowchanics-2.1.4-dev.accessories.12-forge-1.20.1.jar | 23CD6025269BD66B529F9B75F73C1E023FCCC949D33FAD173468061063989A93 |
| NeoForge | neoforge-1.21.1/build/libs/create-meowchanics-2.1.4-dev.accessories.12-neoforge-1.21.1.jar | AF054E4A67E0AC877533D3F7DE3BE36D5A578865079DABC22488159F0BA541DE |

## 待替换旧包（已按 [[mods]] modId=laowu 扫描）

- Forge：D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.11-forge-1.20.1.jar
  SHA-256：42552597123490B191FE08E98E4026426FE70930B2E3620FA30F1F554A1BE4ED
- NeoForge：D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.11-neoforge-1.21.1.jar
  SHA-256：F2D67AF1E7FB31F40BCC66DB05F68568BE3E16BCA4DFD117735D3BA1E355009D

上述为 accessories.12 构建时的部署前清单；后续已在 accessories.13 部署时完成进程检查、旧包备份、双端安装及唯一启用版本和哈希核验。
