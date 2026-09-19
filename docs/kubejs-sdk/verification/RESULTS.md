# 验证证据与边界

## 基线

本包基于 accessories.36 已验证的 36 件替换机制（7 事件＋29 声明式）。该轮 Forge、NeoForge 的脚本模式各 76 项通过；无 KubeJS 原生模式也通过各自套件。双端真实客户端检查了全部 36 件中英文提示、贴图及护胸/金基咪耐久。

## 本 SDK 版本

- 本体：2.1.4-dev.accessories.37；API 仍为 3、schema 仍为 1，新增 supportsApi 兼容声明。
- Forge 1.20.1 / Forge 47.4.22 / Java 17 / KubeJS 2001.6.5-build.26。
- NeoForge 1.21.1 / NeoForge 21.1.219 / Java 21 / KubeJS 2101.7.2-build.377。
- 新增完整注册演示：真实 startup 注册物品、生成数据、有效速度 +10、命中治疗、200 tick 冷却、卸下撤销。
- 真实替换脚本＋新注册演示：NeoForge 全量 77/77 通过；Forge 全量 75/77，所有饰品/脚本案例通过，两个职业测试例外见下文。
- 原生无 KubeJS：NeoForge accessories.37 全量 77/77；Forge accessories.37 全量 76/77，唯一失败为同一个 smartArtillery 职业 AI 用例，警戒范围用例通过。此模式中的 KubeJS 专属测试按测试器设计跳过，不表示执行了脚本。
- 正常双端构建、公开接口、资源和发行隔离检查通过；SDK 中的八个替换文件与四个新物品演示文件已逐一核对真实测试目录 SHA-256 一致。
- 兼容门控：8 个离线场景，旧 API3、未来 API4 保留 v3、未知/不兼容 API 或 schema、缺文件、注册中断。
- 公开 API 检查：84 个冻结签名，所有旧事件/数据键/职业和保存用 36 件 ID。
- JAR 检查：SDK、脚本、探针、测试世界、缓存不打包；API 桥接和正式模型保留。

实际 Minecraft 测试没有用 Node 替代；新兼容包装曾暴露 Rhino 对块内函数声明的差异，已将公共 helper 移到顶层并重跑。离线门控测试只证明控制逻辑，不证明所有未来 KubeJS 实现或第三方模组组合。

## Forge 全量回归的已知例外

最终脚本模式运行中，smartArtillery 的撤退后重新部署未在固定 tick 达成；radiusHostilesAndWalls 等待无玩家测试场中的实体可查询状态超时。因此 Forge 的全量结果不是全绿，不能把“全部饰品案例通过”表述为“整个模组所有功能通过”。其他轮次还观察到飞行落地后自由移动使复乘测试的空间条件变化。

无 KubeJS 的 Forge 本轮也复现 smartArtillery 未在固定 tick 部署的失败，因此并非仅在替换脚本启用时出现。警戒测试在该原生模式通过，但脚本模式的实体跟踪等待超时原因尚未完全定位，保留为已知限制。

这些职业相关用例会跨区块运行，测试场已补充仅限 GameTestServer 的区块票据，并将复乘检查移回已清空的位置；没有降低半径/伤害/冷却断言，也没有改生产职业 AI。本轮生产 JAR 相对 accessories.36 的内容差异只有 CatAccessoryApi 新增 supportsApi、版本元数据及 Forge 的非确定性构建时间戳移除，其他职业/饰品实现与美术内容逐字节相同。以上例外仍需后续独立排查，不能据此断言实际玩家环境无故障。

## 开发工程复现

以下需完整源码工程，不是仅解压 SDK。分别进入 forge-1.20.1 或 neoforge-1.21.1：

```powershell
.\gradlew.bat build runGameTestServer --init-script ../tests/accessory-gametest.init.gradle -PaccessoryExamples36 -PaccessorySdkDemo -PaccessoryRunId=myfreshsdk --no-configuration-cache
```

Java 版本应对应；每次需要新世界时换 accessoryRunId，且不要并发启动同一路径的测试世界。测试脚本与探针不会进入模组 JAR。

SDK 使用者运行 verification/verify-bundle.ps1 只检查文件 SHA 和文件清单，不启动 Minecraft，不修改游戏。其成功不能表述为“已在你的整合包验证”。
