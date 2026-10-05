# 猫机壳与哈基工作盆

适用：Forge 1.20.1 / NeoForge 1.21.1，当前开发版本 2.2.1。

## 使用

- `laowu:cat_casing`（猫机壳）：独立机壳方块，使用美术提供的原图及机械动力全向连接纹理。手持右键传动杆、齿轮或大齿轮可包覆；与安山机壳一样，不消耗手持机壳。潜行使用扳手拆除外壳，保留原传动件。
- `laowu:haji_basin`（哈基工作盆）：使用提供的猫猫工作盆模型，沿用机械动力工作盆的物品、流体、配方过滤、加热判定、搅拌、压块、输出口和扳手行为。容量与加工条件不额外增强。
- 两个物品位于本模组创造物品栏。按本轮约定暂不提供配方，可由整合包自行添加。
- 包覆状态 ID：`laowu:cat_encased_shaft`、`laowu:cat_encased_cogwheel`、`laowu:cat_encased_large_cogwheel`。不单独提供生存物品，破坏后掉落对应的原机械动力传动件。

## 实现边界

- 使用独立方块与方块实体 ID，不替换机械动力原注册，不修改其全局方块实体有效方块列表，也不增加 Mixin。
- 服务端通过原有 Create 接口识别机壳包覆和工作盆；NeoForge 单独注册工作盆的物品/流体能力。
- 客户端保留 Flywheel 转动部件及普通渲染后备路径，工作盆用 cutout 渲染透明像素，物品和流体沿用原工作盆渲染器。
- 本轮只包覆传动杆和大小齿轮，不扩展成风扇、动力泵等机器的换肤系统。

## 美术资源

原始工程保存在 `art/cat-machines/`。PNG 不改像素；连纹图实际为 128×128，符合 Create 的 8×8 图集布局。工作盆导出保留反向内壁、UV、旋转与输出口分组，默认状态不带输出口，另外导出四向输出口状态。

`node tools/export-cat-machines.mjs` 校验导出结果，`--write` 重新导出；`--source <目录>` 可使用外部美术目录。

## 独立验证入口

- `node tests/cat-machines-assets.mjs`：模型导出与资源一致性。
- 对应加载器目录执行 `gradlew.bat runGameTestServer --init-script ../tests/accessory-gametest.init.gradle --no-configuration-cache -PcatMachinesProbeOnly -PaccessoryNoKubeJS -PaccessoryRunId=machines-functional1`：真实游戏中的传动、加工、存档和自动化测试。
- `gradlew.bat runClient --init-script ../tests/cat-machines-client.init.gradle --no-configuration-cache`：独立隐藏客户端验证模型烘焙、连接纹理与 GPU 预览，完成后自行退出。以日志中的 `PASS: CAT MACHINES CLIENT` 为通过标志，而非仅判断 Gradle 退出码。
- 测试源码、客户端预览、原始美术工程和导出工具不进入运行 JAR，不读取玩家存档。
