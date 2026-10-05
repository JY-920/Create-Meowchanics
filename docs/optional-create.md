# 可选机械动力：develop 首版

内部版本：`2.2.2-dev.optional-create.1`。这是 develop 测试阶段，不是新的公开发布版本。

同一个 JAR 支持两种运行环境：

- 安装机械动力：保留现有机器、配方、过滤器、猫咪工作能力、渲染及注册 ID。
- 不安装机械动力：保留猫咪、属性与词条、饰品、套装、Boss、战斗、骑乘、扫描器、材质和调试编辑功能；工业机器、自动激光标记器、生物转信器、过滤器、加工链及机械动力工作能力不注册或不执行。

Forge 1.20.1 使用 Java 17；NeoForge 1.21.1 使用 Java 21。JEI、Curios、KubeJS 仍是可选兼容，不是新的强前置。

## 首版调整

- 飞行员猫和潜水员猫在无机械动力环境使用空手右键骑乘，并有独立的玩家姿态；安装机械动力时仍使用原来的扳手交互。
- 巨猫空手骑乘逻辑不变。
- 运输猫无机械动力时，蹲下空手右键打开原有 27 格背包的原版界面，不执行包裹配送。
- 工程猫战斗仍可用；无机械动力时暂用原版方块／物品显示炮架和弹药，不复制机械动力模型，也不改变命中、范围或伤害逻辑。
- 猫咪扫描器使用原版玩家物品栏背景；有机械动力时仍使用原背景。
- 旧配送猫加载到无机械动力环境时停止不可用的配送阶段，恢复重力与普通姿态，不清理待装载包裹、地址和猫咪存储的数据。
- 工业配方按加载器条件启用；缺失工业物品的战利品／标签不再阻止无机械动力启动。
- 饰品 API v1–v3／schema 1、词条 API v1／schema 1 和已存储的自定义词条 ID、等级、脚本状态保持兼容。

## 暂未包含

没有设计无机械动力的新获取配方。依赖机械动力的旧配方会关闭，因此部分猫咪道具暂时需要创造模式、命令或整合包自定义配方获取。

请先用独立测试存档。这里支持的是“无机械动力也能运行猫咪玩法”，不承诺把已经包含机械动力机器、坐垫实体、包裹或其它移除模组物品的旧存档无损转换。备份存档后再移除机械动力；不要把缺失物品的背包保存行为视为完整机器／物品迁移。

## 可重复验证

在对应加载器目录中运行正常 `gradlew.bat build --offline`，保留 API、资源和发布隔离检查。

最小客户端测试：

```powershell
# 先生成独立测试世界
.\gradlew.bat runGameTestServer --offline --init-script ../tests/accessory-gametest.init.gradle -PwithoutCreate -PstandaloneCatProbeOnly -PaccessoryNoKubeJS -PaccessoryRunId=standalone-phase2 --no-configuration-cache
.\gradlew.bat runClient --offline --init-script ../tests/standalone-client.init.gradle -PwithoutCreate --no-configuration-cache
# 保留 JEI / Curios 的无机械动力组合
.\gradlew.bat runClient --offline --init-script ../tests/standalone-client.init.gradle -PwithoutCreate -PstandaloneWithOptionalMods --no-configuration-cache
```

真实服务器玩法与 KubeJS 词条回归：

```powershell
.\gradlew.bat runGameTestServer --offline --init-script ../tests/accessory-gametest.init.gradle -PwithoutCreate -PstandaloneCatProbeOnly -PtraitKubeProbeOnly -PtraitExamples -PaccessoryRunId=optional-create-all-absent --no-configuration-cache
```

客户端夹具从 `build/accessory-gametest/no-kubejs-standalone-phase2/world` 复制测试世界，不能操作用户存档。测试源码、截图和世界不进入运行 JAR。
