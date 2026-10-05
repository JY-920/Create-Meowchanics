# 猫机壳与哈基工作盆验证记录

日期：2026-09-19。基线：6126b85（此前内容已推送 origin/main）；本次新增功能留在 develop 工作区，未再次提交或推送。

## 自动验证

- Forge / NeoForge 正常 build 均通过，版本均为 2.2.1。每端 23252 项饰品逻辑检查，以及饰品 API v3、词条 API v1、动画资源和发布隔离验证通过。
- 实际 Minecraft + KubeJS 全套 GameTest：两端均报告 All 118 required tests passed。36 个替换饰品脚本、SDK 示例和词条示例均加载；两端 server.log 中没有 KubeJS ERROR。
- 本次新增 7 个 GameTest：注册及独立实体类型、三种包覆件和三轴的多轮带动力包覆/拆壳、物品/流体能力与保存恢复、正确工具和掉落、真实加热/过滤限制的黄铜搅拌、真实铁块压制及防重复产物。
- 现有 ProtoChunk promotion 用例仍沿用默认运行中的条件跳过；本次未改动其策略。
- 独立客户端：两端均输出 PASS: CAT MACHINES CLIENT；检查全部方块状态模型、实际连接纹理 UV、开/关轴孔边框、小齿轮侧面连纹、Flywheel 注册，并生成十视角 GPU 图片供人工检查。
- tests/run-global-world-config.ps1 通过；模型导出算法和资源验证、导出结果与原 PNG 字节校验通过。

日志：每个加载器 build/machines-full.log、build/machines-client-final.log、build/machines-build.log；公共配置日志 tests/build/machines-config.log。图片在各端 build/cat-machines-client/cat-machines-preview.png。

## 红—绿与审查

- 初始两个 GameTest 因缺少真实机壳、工作盆注册而失败，注册后通过。
- 客户端测试先抓到缺少 CTModel，再抓到缺少 Flywheel 可视化注册，修复后通过。
- 审查发现打开齿轮轴孔后错误连纹，以及小齿轮误用大齿轮 CT 构造函数。两个问题均经实际 UV 测试复现并修复；最终双端复测通过。
- 包覆测试最初要求方块替换的同一调用帧保留转速。独立方块实体替换后会按正常 tick 生命周期重新加入动力网络，故改为等待真实世界 tick 后检查同一有符号转速、类型和保存数据；没有手动注入转速或调用 tick 来制造通过。三种传动件、三个轴向、多次往返全部通过。
- 只读审查复核上述两处修复后，无剩余具体问题。

## 发布隔离与部署

- 两端运行 JAR 不包含探针、测试源码/世界、SDK、示例脚本、预览图或美术导出工具。
- 游戏进程检查只发现用户正在运行的其他实例，不替换该实例任何文件。
- 已部署到约定的 Forge / NeoForge 开发实例；各自仅有一个 modId=laowu 的启用 JAR，安装文件与构建文件 SHA-256 一致。
- 旧包可在各实例 mod-backups/create-meowchanics/20260919-192947-2.2.1-cat-machines/ 恢复。
- 详细哈希与路径见 deployment-cat-machines.json。之前 releases/ 中的公开压缩包未覆盖。

功能说明和无配方范围见 cat-machines.md。
