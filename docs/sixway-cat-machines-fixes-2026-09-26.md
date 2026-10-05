# 六向猫机器修正 — 2026-09-26

公开版本保持 2.2.1，范围仅为用户反馈的三项问题，不提交或推送。

## 行为与根因

1. 放置条件曾错误复用加工目标检查。现在允许独立放置，仅禁止紧邻盆/台工作面的那一格；所有六向一致，并兼容原版盆/台。加工仍要求目标距离两格、同向、中间无碰撞阻挡，搅拌器只处理盆。
2. Create 的置物台手动交互只接受世界 UP。猫置物台先检查实际工作面，再把已接受的点击方向映射为 UP，委托原版收放与库存逻辑，不重写物品处理。
3. 猫搅拌器物品模型继承了过大的默认 GUI 变换。现采用原版搅拌器 GUI 旋转 [30,225,0] 和缩放 [0.45,0.45,0.45]；猫模型偏低，所以使用 [0,2.25,0] 位移而非原版的负位移。只改物品栏展示，不改世界模型或贴图。

## 验证证据

- `build/sixway-fix-red26-forge.log`：新加独立放置、六向手动交互回归在修改前失败。
- `build/sixway-icon-red26-neo.log`：真实 GUI 渲染顶点超出 16px 物品格，不只检查 JSON 参数。
- `build/sixway-fix-green26b-forge.log`、`build/sixway-fix-green26b-neo.log`：最终各 11 项真实 GameTest 全部通过，包含六向手动放入/空手取回、错误面不收料、无目标独立放置、紧邻工作面禁止放置，以及原有加工/应力/出料回归。
- 静态资产检查：`node tests/cat-sixway-assets.mjs`、`node tests/cat-machines-assets.mjs`、`node tests/cat-depot-assets.mjs`、`node tools/export-cat-sixway.mjs --check` 均通过。
- 新增实际渲染顶点探针 `tests/machines/{forge,neo}/cn/laowu/mod/client/CatMixerIconProbe.java`，由正式测试客户端调用。测试代码不放进运行 JAR。
- 最终差异只读复核：放置、交互、GUI 变换未发现重要缺陷。

早期一次 Forge 套件还出现了已有的大范围测试场景出料/应力超时，保留原始日志及 NBT 诊断；后续两轮 Forge 与两轮 NeoForge 的这些既有测试均通过。没有据此宣称已查明该间歇问题根因，也没有放宽相关断言。

## 客户端、构建与部署

- `build/sixway-icon-green26-forge.log`、`build/sixway-icon-green26b-neo.log`：两端 GUI 顶点边界、机壳 CT、猫传送带、置物台以及领养箱九猫显示回归均 PASS。
- 两端 `build/cat-machines-client/cat-mixer-gui-comparison.png`：真实 GPU 截图，左为原版 Create 搅拌器，右为猫搅拌器。已检查，无溢出或裁切。
- `build/sixway-fix-release26-{forge,neo}.log`：正常 `gradlew build --no-configuration-cache` 全部成功。每端 23,252 项饰品检查、冻结的饰品/词条 API 检查及发布隔离检查全部通过。
- 2026-09-26 11:29:44 已部署至 Forge 1.20.1 与 NeoForge 1.21.1 实例。部署前无 Java 游戏进程；未强退游戏。每实例仅一个启用的 `modId=laowu`，均为 2.2.1，安装包 SHA-256 与构建产物一致。
- Forge SHA-256：`ED6D1D700FCBE49837557FBD6E8010D95FDBE417CCD0401E876509A549833643`。
- NeoForge SHA-256：`777BBD81B3BA254C1850CEDFD6760738D78F46A047015483E2072F3044AB92D8`。
- 原包已移入各实例 `mod-backups/create-meowchanics/20260926-112943-2.2.1-sixway-interaction-fixes/`，可恢复。完整路径和前后哈希见 `deployment-sixway-fixes-2026-09-26.json`。
- 未修改玩家存档、配置、其他模组或 KubeJS。未提交、推送或修改公开版本号。
