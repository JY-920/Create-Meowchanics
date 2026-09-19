# 2.2.0 区块加载死锁修复

内部构建：`2.2.0-dev.chunkload.1`；公开版本仍为 `2.2.0`。原 2.2.0 发布包与饰品 KubeJS SDK 1.0.0 均保留，不覆盖。

## 原因与修复

用户提供的服务端线程转储指向：

```text
ChunkMap / LevelChunk.runPostLoad
  -> EntityJoinLevelEvent
  -> CommonEvents.initializeCatTraits
  -> CatAttributeData.ensure / set / CatAttributeEffects.refresh
  -> Cat.isInWaterOrRain / Level.isRainingAt
  -> Twilight Forest ASMHooks.cloud
  -> getBlockState / ServerChunkCache.getChunk
  -> 等待当前尚未完成 FULL 转换的同一个区块
```

服务器线程等待自己正在处理的区块，导致客户端仍能移动，但无法正常交互。旧 2.1.4 和原 2.2.0 均存在这条调用链。

Forge、NeoForge 的入场事件现在只将猫加入弱引用待初始化集合。恢复中断的属性界面锁、生成缺失词条/属性、刷新派生属性，整体移至该猫入场后的第一次正常实体 tick。不能只推迟最后一个 refresh，因为两个 ensure 内也可能间接刷新；也不使用可能在 managedBlock 内重入的 server.execute。

重复入场同样会重新排队，不依赖 tickCount 为 0 或 1；取消入场或已卸载的实体不会被集合强引用保留。不新增存档字段，不重抽已有属性/词条，不改变战斗公式、饰品效果或资源。

## 验证

- 旧代码基线：三个安全探针全部暴露入场期间的天气读取；真实区块转换用例未在旧代码上执行，以免故意锁死。
- Forge 1.20.1 + Forge 47.4.22 + 真实暮色森林 4.3.2508：4/4 专项用例通过。
- NeoForge 1.21.1 + NeoForge 21.1.219 + 真实暮色森林 4.8.3345：4/4 专项用例通过。
- 专项覆盖：新猫延迟初始化；旧猫属性、上限、词条、血量比例不变及 UI 锁恢复；已 tick 猫再次入场；带有猫 NBT 的真实 ProtoChunk 完成 FULL 转换并开始 tick。
- 两端均实际放置暮色雨云，验证晴天时原版天气查询被云层钩子改变，排除“仅加载模组、钩子未生效”的假阳性。
- 完整 KubeJS 回归：Forge 81/81、NeoForge 81/81，启用 36 饰品替换脚本及 SDK 新饰品示例。此轮未启用高风险区块转换项，其标记 SKIP；该项在上述暮色专项中单独实际执行。
- 双端正常 build 成功；每端 23,252 项饰品定义、状态和概率检查通过；公开 API v3 兼容快照、发布隔离和动画资源检查通过。
- 27 组 Node 源码/资源检查通过；双端成品资源检查通过；Mixin 包检查 276 项通过；原 SDK ZIP 解压后 287 项便携包检查通过。
- 与原 2.2.0 成品逐 ZIP 条目比较：两端运行代码都只改变 CommonEvents.class；其余变动仅为版本元数据。所有其他运行类、美术资源、配方及 API 字节不变；无新增/删除条目，未打入测试类、暮色依赖或 SDK。

验证使用独立 GameTest 世界，没有打开或修改用户存档。区块转换用例在测试主世界触发暮色的全局天气钩子，未声称完成用户原整合包/原存档的长时间暮色跑图复测。

## 复跑入口

根目录 `tests/accessory-gametest.init.gradle` 为可选测试入口，不属于生产构建依赖。

在对应子项目目录，用 Java 17（Forge）或 Java 21（NeoForge）运行：

```powershell
# SDK + 36 饰品行为回归；每次用新的 accessoryRunId 避免旧测试世界状态。
.\gradlew.bat runGameTestServer --init-script ..\tests\accessory-gametest.init.gradle '-PaccessoryExamples36' '-PaccessorySdkDemo' '-PaccessoryRunId=chunkload-sdk-new' --no-configuration-cache

# 真实暮色 + 新区块转换；仅在独立 GameTestServer 运行。
# Forge 使用下列依赖；NeoForge 改为 curse.maven:the-twilight-forest-227639:7797302。
.\gradlew.bat runGameTestServer --init-script ..\tests\accessory-gametest.init.gradle '-PaccessoryNoKubeJS' '-PcatLoadingProbeOnly' '-PcatLoadingPromotion' '-PcatLoadingTwilight=curse.maven:the-twilight-forest-227639:5468648' '-PaccessoryRunId=chunkload-twilight-new' --no-configuration-cache

# 正常构建不传测试 init script。
.\gradlew.bat build
```

对应构建日志位于两端 `build/chunkload-build.log`；实际运行日志位于 `build/chunkload-sdk1.log` 与 `build/chunkload-twilight3.log`；旧代码负向探针日志位于 Forge 的 `build/chunkload-baseline.log`。

## 成品校验

| 平台 | SHA-256 |
| --- | --- |
| Forge 1.20.1 | `199B3E6C5E070B48032DB0447178F485928EF35CC1BF25E204078BB3664906C5` |
| NeoForge 1.21.1 | `F4E26C52A56DD9F87DDE62B8C084F7CC7C61832BEE7B7669FB430671101DEE52` |

未修改另一个独立的 JEI / TConstruct 配方展示兼容异常；该异常不属于这条已确认的区块自等待调用链。
