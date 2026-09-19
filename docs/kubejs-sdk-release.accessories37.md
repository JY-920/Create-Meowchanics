# 猫咪饰品 SDK 1.0.0 / accessories.37 交付记录

时间：2026-09-17 23:01:51。独立 SDK 已封包；本体构建完成，尚未部署。

## 开发包

[最终压缩包](../releases/cat-accessory-kubejs-sdk-1.0.0.zip)，46739 字节。25 个文档/脚本/契约文件和一个 manifest，共 26 个文件；重新解压后完成 287 项检查和全部 SHA 核验。

SHA-256：`3FCBBDD7CF998703B27A67469CB6E017BA3601DC2BAD1E46C07F413B614E9F26`

包含 AI_README、注册/效果教程、完整 API、JSON Schema、36 件机器可读目录、Forge/NeoForge 四文件替换包、新物品注册和事件演示。36 件的边界为 7 件事件脚本 + 29 件声明式调用引擎，原有 ID 不变，不新增第二套物品。原生护胸/金基咪的 50 耐久、15 秒冷却保留。

兼容契约为 API v3 / schema 1，不绑定内部构建号；新增 supportsApi，保留旧入口。正常构建检查 84 个公开签名、15 个事件和全部旧数据键与物品 ID。脚本只有在兼容且注册完成后才接管原生定义，离线验证了 8 种正常/降级场景。跨 Minecraft/KubeJS 大版本或第三方改版不能提前保证，须实际验证。

## 实际测试

| 环境 | 全量结果 | 说明 |
| --- | --- | --- |
| Forge，四文件替换和完整注册演示 | 75/77 | 饰品与脚本案例通过；职业用例 smartArtillery、radiusHostilesAndWalls 未通过 |
| NeoForge，同上 | 77/77 | 全部通过 |
| Forge，无 KubeJS | 76/77 | smartArtillery 未通过，警戒用例通过；KubeJS 专属案例按设计跳过 |
| NeoForge，无 KubeJS | 77/77 | KubeJS 专属案例按设计跳过 |
| 双端普通 build | 通过 | 无需引入测试脚本，API 与 JAR 隔离验证执行 |
| 源码/脚本检查 | 26 组通过 | 另加单独运行的 SDK 287 项检查，不用离线检查冒充游戏测试 |

详细边界见 SDK 内 verification/RESULTS.md。日志保留在两端 build/accessories37-sdk-tracked.log；Forge 原生记录为 build/accessories37-native.log，NeoForge 原生亦同名。客户端渲染沿用 accessories.36 已验证结果；本次未声称重新进行了客户端截图验证。

新旧运行 JAR 逐条目对比：仅 CatAccessoryApi、版本元数据和 Forge manifest 变化；职业、饰品实现、美术资源的内容字节均未变。固定 tick 的职业测试仍存在无玩家测试场不稳定/实体跟踪等待问题，保留失败证据，未降低断言或改生产 AI 来掩盖结果。

## 产物清理

76 个旧构建已移入两个加载器各自的 `build/artifact-archive/20260917-225716-sdk37/`，可原样恢复；没有删除。两端 build/libs 各仅保留当前 accessories.37 正式运行包。具体源路径、归档路径与 SHA 见 [归档清单](build-output-archive.accessories37.json)。

运行 JAR 不含 SDK、示例 JS、测试探针、测试世界、缓存、临时文件。正式 API、KubeJS 可选桥接、原生定义、运行所需 bbmodel/动画/PNG 保留。未来普通构建和 Maven 发布继续执行隔离检查。

## 部署状态

未部署：最后检查仍有 Minecraft 客户端 PID 36928 运行；没有强制结束或在运行中覆盖 JAR。四个实例仍为经过核验的 accessories.36，每处只有一个 laowu 包，两台测试服务器保持关闭。没有修改玩家 kubejs、存档、配置或其他模组。

已构建的两个 accessories.37 JAR 均完成 SHA 和包内身份/资源核验。待玩家退出后再遵循备份与校验流程部署。详见 [机器可读交付记录](kubejs-sdk-release.accessories37.json)。
