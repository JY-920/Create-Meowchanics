# 猫置物台、猫传送带与领养箱展示

适用 Forge 1.20.1 / NeoForge 1.21.1，版本仍为 2.2.1。

## 使用

- 猫置物台（`laowu:cat_depot`）：使用提供的模型和原图，支持机械动力置物台的物品存取、自动化、冲压、机械手和注液加工。
- 置物台碰撞体按原稿的 10 像素底座和 2 像素内缩台面对齐；原生物品渲染下移 1 像素，保留相对于台面的正常展示间距。
- 猫机壳包覆传送带：先用机械动力原版传送带连接两根传动杆，再手持 `laowu:cat_casing` 右键需要包覆的区段。保留原传送带滚动、动力和物品运输；其他区段与普通传送带外观不变。
- 包覆后的方块仍是 `create:belt`，以额外的 `LaoWuCatBelt` 外观标记保存，不替换注册或控制器。破坏和选取均返回原版传送带连接器。使用安山/黄铜机壳覆盖或用扳手拆除机壳，会清除猫纹。
- 不再提供独立猫传送带物品的正常获取入口。旧 `laowu:cat_belt` ID 仅为存档兼容保留，已从创造栏和 JEI 隐藏；旧物品仍可像原版连接器一样使用，但不自动加猫纹。猫置物台保留创造栏入口，本轮不新增配方。
- 普通领养箱与心愿领养箱按输入栏槽位排列为 3×3，最多同时显示 9 只完整猫饼，保留实际花色和体型差异。巨型猫饼按格子空间限制展示大小，不互相覆盖或伸出箱体。取出、消耗或更换输入后逐格同步；空格不显示，不额外生成猫实体。
- 心愿箱交易仍立即完成，符合条件的猫饼可能来不及显示；不额外停留或保留交易残影。奖励饰品展示不变。

## 资源与实现

- 置物台原稿：`art/cat-machines/哈基机器/哈基置物台/`；使用 `node tools/export-cat-depot.mjs` 校验，`--write` 导出。
- 用户确认按像素网格提取传送带截图：原图 `art/cat-machines/cat-belt-source.png`，64×64 结果 `cat-belt-casing.png`；可用 `tools/extract-cat-belt-texture.ps1` 重现。保留中心采样的原色，去除编辑器透明棋盘背景。
- 传送带保留 Create 原模型、原滚动渲染器与 Flywheel，仅按外观标记替换外壳网格贴图。两个轻量 Mixin 负责猫机壳交互及外观标记的存取/清除；不修改运输 tick 或原版掉落/选取逻辑。
- 领养箱网络包只携带白名单外观数据，不同步猫饼中的整个实体、背包和装备库存。菜单仍使用原有槽位同步，存档仍保留完整库存。

## 验证入口

- `node tests/cat-depot-assets.mjs`
- `gradlew.bat runGameTestServer --init-script ../tests/accessory-gametest.init.gradle --no-configuration-cache -PcatAdditionsProbeOnly -PaccessoryNoKubeJS -PaccessoryRunId=additions-check`
- `gradlew.bat runClient --init-script ../tests/cat-machines-client.init.gradle --no-configuration-cache`：须检查各项 PASS 及最终 `PASS: CAT MACHINES CLIENT`，不能仅依赖进程退出码。
- 正常 `build` 保留 API 兼容与发布隔离校验；所有测试、预览图片、原稿与导出脚本均在运行 JAR 之外。
