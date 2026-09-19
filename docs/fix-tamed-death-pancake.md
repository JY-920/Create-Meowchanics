# 已驯养无职业猫的死亡猫饼（2026-09-18）

## 规则

- 已驯养猫即使无职业，真正死亡时也生成一个可还原的猫饼；成年猫、幼猫都适用。职业猫原逻辑不变。
- 完整保留主人、名字、外观、项圈、年龄、词条、属性上限、9 格物品栏及 4 格饰品；随身物品只留在猫饼里，不再另掉一份。
- 沿用已有死亡代价：随机一项基础属性扣 20、最低 0，其他基础值及上限不变。“生个小病”等独立词条规则不变。
- 未驯养且无职业的猫不新增猫饼，仍按原有逻辑掉落物品；不改变已有职业猫资格。
- 遵守 doMobLoot；图腾等阻止死亡时不生成猫饼。蟑螂分裂成功仍用两只幼猫替代猫饼，原物品只落地一次。
- 保持包内和文件名正式版本 2.2.0，不增加 dev 后缀。

## 实现边界

双端 CommonEvents 死亡资格从“有职业”扩展为“已驯养或有职业”；CatProfileData 同步扩展库存保留资格，避免先把物品抛出再保存空猫饼。CatPancakeItem 仅更新方法说明，沿用现有捕获、扣属性和还原实现。没有新增物品、保存字段、配置项或 Mixin。

更新了当前饰品文档、脚本文档和 SDK 源文档的库存说明；未改脚本 API、已交付的 SDK ZIP、玩家脚本或公开版本号。先前 releases 下的双端 ZIP 保留为上一轮烟雾修复交付物，本轮以最新 build/libs 与开发实例中的包为准。

## 验证

- 独立无 KubeJS GameTest：Forge 6/6，NeoForge 6/6。
- 新增用例实际造成死亡并调用现有生产还原函数，覆盖成年家猫、幼年家猫、职业猫对照；检查主人、外观、名称、词条、属性扣减、原槽位物品与饰品、没有重复掉落；还覆盖野猫、doMobLoot=false 和原版图腾保护。
- 还原用例直接调用生产 restoreCat，不声称重新覆盖鼓风机风场检测。
- 双端真实 KubeJS + 36 饰品替换 + SDK 新饰品示例均为 92/92；包括实际蟑螂分裂、雷管最后自爆、物品栏、饰品回调及烟雾回归。旧有工程／警戒不稳定用例本轮通过，但未借此声称其根因已单独修复。
- 完整回归中的高风险 ProtoChunk 转换按原设计未启用并输出 SKIP；此项前轮独立暮色验证不算本次重跑。
- 28 组源码／资源检查以及既有 SDK 包 287 项检查通过。双端正常 build 保留公开 API v3、饰品与发布隔离验证；测试类不进入 main 或运行 JAR。

日志：两端 build/tamed-pancake-sdk1.log、build/tamed-pancake-build.log；原生专项为 Forge build/tamed-pancake-native1.log、NeoForge build/tamed-pancake-native2.log。

## 复跑

在对应子项目目录设置 Java 17（Forge）或 Java 21（NeoForge），使用新的 fixture ID：

```powershell
.\gradlew.bat runGameTestServer --init-script ..\tests\accessory-gametest.init.gradle '-PtamedDeathPancakeProbeOnly' '-PaccessoryNoKubeJS' '-PaccessoryRunId=death-native-new' --no-configuration-cache
.\gradlew.bat runGameTestServer --init-script ..\tests\accessory-gametest.init.gradle '-PaccessoryExamples36' '-PaccessorySdkDemo' '-PaccessoryRunId=death-sdk-new' --no-configuration-cache
.\gradlew.bat build --no-configuration-cache
```

构建归档：两端 build/artifact-archive/20260918-142212-before-tamed-pancake/ 保存上一轮正式构建。部署结果与成品校验见 deployment-tamed-pancake.json。
