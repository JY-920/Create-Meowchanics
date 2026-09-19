# 项目部署约定

用户于 2026-09-13 明确要求：以后完成改动后直接部署，不再等待单独的“部署”指令。

- 用户要求实现或修复功能时，完成 Forge / NeoForge 双端修改、构建和必要验证后，默认部署到下面两个本地测试实例。用户明确要求暂不部署时除外；纯咨询、审查和诊断不触发部署。
- Forge 实例：`D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发`。
- NeoForge 实例：`D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发`。
- 部署前检查游戏进程；若游戏正在运行，请用户退出后再替换，不强制结束游戏，也不在运行中替换 JAR。
- 先验证构建产物，再将本模组旧包移到实例下 `mod-backups/create-meowchanics/<时间戳-内部版本>/`。保留可恢复备份，不修改存档、配置或其他模组。
- 部署后核对新包与构建产物的 SHA-256，按包内 `modId=laowu` 确认每个实例仅有一个启用版本，并记录部署结果。
- 内部构建编号与公开发布版本分开；自动部署不表示自动提交、推送、合并分支或发布新公开版本。

# 猫咪饰品脚本兼容与发布隔离

- 猫咪饰品公开 API v1–v3、schema 1 和现有物品 ID 是兼容契约。正常更新只能追加，旧入口需要保留或委托兼容适配器；不得为绕过验证随意删除/改写 compatibility/cat-accessory-api-v3.json。
- supportsApi(3) 为真时，必须继续支持已交付的旧脚本。签名检查不是行为保证，修改事件时序/效果实现后须运行实际双端 KubeJS 回归。
- SDK 在 docs/kubejs-sdk 和独立 releases 包中，不放入 src/main/resources。测试源码不混入 main。
- 保留正式 api/compat 运行类、kubejs.plugins.txt、kubejs.classfilter.txt、原生饰品定义，以及实际加载的 .bbmodel/动画/图片；这些不是测试垃圾。
- 打包使用正常 build，保留 verifyCatAccessoryApiV3 与 verifyReleaseIsolation；发布前检查运行 JAR 不含示例、探针、测试世界、缓存或 SDK ZIP。不覆盖用户已有 kubejs。
- 词条公开 API v1、schema 1 与 82 个内置词条 ID 同样是追加式兼容契约，保留 compatibility/cat-trait-api-v1.json 和 verifyCatTraitApiV1；行为修改仍须实际双端 KubeJS 验证。自定义词条按 ID 存储，移除定义只能暂停效果，不可自动清洗存档中的 ID、等级或脚本状态。词条示例同样只放在 docs/examples 与独立 SDK 包，不自动覆盖玩家脚本。
