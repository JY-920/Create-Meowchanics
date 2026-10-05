# 用户机壳贴图接入（2.2.1）

## 范围与来源

接续原先暂缓的第 4 项：使用用户 2026-09-26 提供的三张 PNG，
完善猫机壳包覆传动杆和齿轮的接口面、分隔面及连接纹理。
原图逐字节归档于 `art/cat-machines/encased-user-20260926/`；
文件映射、尺寸和 SHA-256 见该目录 README。
不改动像素、不缩放、不恢复此前被拒绝的生成贴图。

- 包轴接口、齿轮开放轴端：用户 gearbox。
- 小齿轮侧面：用户 side + connected，保持原生纵向/横向 CT 规则。
- 大齿轮侧面：用户 connected，保留 Create 原生模型 UV。
- 大齿轮物品模型 key 4 仍绑定真实齿轮材质，不误替换为接口。
- 上次完成的冲压机/搅拌器物品传动杆、接口 UV、原版传送带猫机壳包覆不变。

## 验证记录

- 测试先行：接入前 `tests/cat-casing-scope.mjs` 明确因缺少用户接口贴图失败；
  接入后原图字节一致性、模型绑定、六向/置物台资源与两项导出一致性检查全部通过。
- 独立只读复核确认三张图的尺寸和每个 alpha 值均与双端 Create 原图布局一致，
  所有包轴/齿轮变体与 CT 绑定正确，无待修复项。
- NeoForge 真实客户端：`build/user-casing-client-neo.log` 最终
  `PASS: CAT SIXWAY CLIENT + JEI`。实际 GPU 预览已查看。
- Forge 首轮：`build/user-casing-client-forge.log` 在原有搅拌器同步测试
  stage 1 超时，尚未进入本次贴图检查；不能将该轮 Gradle 的退出成功当作测试成功。
  保留旧隔离存档为 `forge-1.20.1/build/cat-sixway-client/saves/Sixway-before-user-casing-20260926`，
  重新从测试夹具创建存档复测；没有移动或修改玩家实例的存档。

本次只更新材质与客户端 CT 绑定，不改变饰品/词条 API 或游戏处理逻辑；
构建继续使用正常 build，保留兼容契约、动画和发布隔离检查。

## 最终结果与部署

- Forge 新隔离存档复测：`build/user-casing-client-forge-fresh.log` 最终
  `PASS: CAT SIXWAY CLIENT + JEI`，包括此前超时的搅拌器中断同步测试；
  未改动搅拌器生产代码或放宽断言。双端 GPU 预览已查看。
- 双端 `build/user-casing-release-{forge,neo}.log` 正常 build 成功：
  23252 项饰品检查、饰品 API v3、词条 API v1、动画资源与发布隔离检查均通过。
- 最终两个 JAR 内三张 PNG 与用户 Downloads 原图 SHA-256 一致；
  没有草稿来源、测试探针、旧 CatBeltDropsMixin 或 SDK 混入。
- 2026-09-26 23:11:33，确认目标客户端已停止后部署两个约定实例；
  旧包移至各实例 `mod-backups/create-meowchanics/20260926-231133-2.2.1-user-casing-art/`，可恢复。
  每个实例仅启用一个 laowu JAR，安装包与构建产物 SHA-256 一致。
- 部署明细：`docs/deployment-user-casing-art-2026-09-26.json`。
  未修改玩家存档、配置、其他模组或 KubeJS；未提交、推送或发布远端版本。
