# 本地部署记录：accessories.11

- 部署时间：2026-09-14T11:24:43.9748760+08:00
- 分支：develop；公开版本仍为 2.1.4，内部版本为 2.1.4-dev.accessories.11。
- 本次仅更新饰品图标和显示名称，不修改物品 ID、效果、配方或存档数据；未提交、推送或发布正式版。

## 本次改动

- 将用户 2026-09-14 提供的 16 张 16×16 PNG 原样导入 Forge / NeoForge，未缩放或改色；映射与 SHA-256 见 [素材清单](../art/accessories-redrawn-v2/manifest.json)。
- 其中铃铛、船锚、隔热手套、磁铁、羽毛、黄油共 6 张与上一版不同，其余 10 张与用户此次提供的文件字节一致。
- 中文名称改为「隔热手套」「羽毛玩具」「齿轮玩具」「弹簧玩具」「船锚玩具」，同步英文名称。

## 校验

- Forge / NeoForge 完整构建成功，各通过 23,120 项饰品回归检查。
- `tests/accessory-art-and-careers.mjs`：329 项通过。
- `tests/accessory-wiring.mjs`：391 项通过。
- 包校验通过：双端各 16 个饰品定义、模型、16×16 贴图与中文名称对应正确，全部贴图 SHA-256 与素材清单一致；15 个合成配方和 1 个不可合成的 Boss 战利品符合预期。
- 动画资源兼容性校验通过。
- 与已部署 accessories.10 逐项比对 JAR：Forge 501 个、NeoForge 495 个 class 条目全部相同；差异仅为版本元数据、两份语言文件和上述 6 张 PNG。其他资源、物品模型、效果定义与配方未变化。
- 部署前与替换前均确认没有 Minecraft / 非 Gradle Java 进程；部署后扫描全部启用 JAR 的 `[[mods]]` 元数据，两个实例各只有一个 `modId=laowu` 包。
- 新包与构建产物 SHA-256 一致，旧包备份 SHA-256 与部署前一致。本次没有启动游戏做画面验收。

## Forge 1.20.1

- 新包：`D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.11-forge-1.20.1.jar`
- SHA-256：`42552597123490B191FE08E98E4026426FE70930B2E3620FA30F1F554A1BE4ED`
- 旧包备份：`D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mod-backups/create-meowchanics/20260914-112443-accessories.11/create-meowchanics-2.1.4-dev.accessories.10-forge-1.20.1.jar`
- 旧包 SHA-256：`18CD20591335220C1CBC10C95D56A8B956E0AA5F309D9D4404855DD4FB5D9DB8`

## NeoForge 1.21.1

- 新包：`D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.11-neoforge-1.21.1.jar`
- SHA-256：`F2D67AF1E7FB31F40BCC66DB05F68568BE3E16BCA4DFD117735D3BA1E355009D`
- 旧包备份：`D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mod-backups/create-meowchanics/20260914-112443-accessories.11/create-meowchanics-2.1.4-dev.accessories.10-neoforge-1.21.1.jar`
- 旧包 SHA-256：`A1B3A5ABB7387D393E642C46EA4DF4A4E91A15826AA6033E671073DE7F08CE39`

旧包均以移动方式保留，可在退出游戏后恢复；未改动存档、配置或其他模组。
