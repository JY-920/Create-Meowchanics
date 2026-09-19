# 猫咪饰品 KubeJS 开发包 1.0.0

这是独立文档和示例包，**不是 Minecraft 模组，也不要把整个压缩包放进 mods**。不包含模组 JAR、测试世界或第三方模组。接口为 CatAccessoryEvents / CatAccessoryApi，不能直接粘贴 Curios 玩家饰品脚本。

## 先选择你的用途

| 用途 | 阅读 | 复制什么 |
| --- | --- | --- |
| 验证现有 36 件饰品的替换方案 | [安装与撤销](docs/01-installation.md) | replacement/forge 或 replacement/neoforge 下的 kubejs |
| 注册自己的新猫咪饰品 | [注册教程](docs/02-registration.md) → [效果编写](docs/03-effects.md) | examples/new-accessory 中对应加载器的 kubejs，仅作为独立演示 |
| 交给 AI 编写 | [AI_README.md](AI_README.md) | 把需求与本包一起交给 AI，不需要复制所有文件进游戏 |
| 查询具体接口 | [API](docs/API.md)、[数据格式](contracts/accessory.schema.json)、[36 件清单](catalog/accessories36.json) | 无 |
| 更新模组 | [兼容与迁移](docs/04-compatibility.md) | 保留原 kubejs，备份后测试；不要按本体版本号重命名物品 |

支持：Forge 1.20.1 / KubeJS 2001；NeoForge 1.21.1 / KubeJS 2101。已验证具体版本见 [验证记录](verification/RESULTS.md)。

36 件替换包沿用原 ID 与美术。其中 7 件事件脚本、29 件脚本数据定义调用引擎；**不是 36 件纯 JS 重写**，也不增加另一批物品。所有参数集中在 laowu36_catalog.js。护胸和金基咪的 50 耐久、15s 冷却仍由模组正确结算。

## 内容布局

```text
AI_README.md                 AI 接入顺序、约束、常见错误
docs/                        独立教程和 API，不依赖原工程目录
contracts/                   JSON Schema、冻结 v3 接口与能力说明
catalog/                     36 件 ID、数值、效果归属
replacement/forge/kubejs/     Forge 四文件替换包
replacement/neoforge/kubejs/ NeoForge 四文件替换包
examples/new-accessory/       从注册新物品到定义和事件的完整演示
verification/                已验证范围、离线完整性检查
manifest.json                所有交付文件的 SHA-256
```

解压后可用 PowerShell 执行 `./verification/verify-bundle.ps1` 检查完整性；它只读取文件，不安装、修改或删除游戏文件。目录名和注册 ID 均使用 ASCII，文档为 UTF-8，方便 AI 与工具解析。

本体的同系列更新保留 API v3 时无需改这些脚本。无法替任意未来 Minecraft / KubeJS 大版本或第三方改版承诺兼容；具体保护措施和边界见兼容文档。
