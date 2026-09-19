# accessories.21：特工、潜水、蟑螂职业预注册

2026-09-15 11:23:24 已部署。公开版本仍为 2.1.4；内部构建 2.1.4-dev.accessories.21。

## 已完成与未完成的边界

- 双端追加 AGENT / DIVING / COCKROACH，持久化 ID 分别为 agent / diving / cockroach；既有 NONE 和十个职业的枚举编号不变。
- 使用用户提供的三套 BBModel、ZIP 内套装贴图、三个成品图标及三个未完成构件图标。PNG 逐字节保留，模型保留原结构、UV、旋转和动画数据；原猫身体仍用原生猫模型和猫自身皮肤，套装只渲染第二纹理层。
- 完成成品套装物品注册与创造栏显示、右键穿戴／剪刀卸下、猫饼保存／恢复、职业过滤、中英文名称和预览说明；接入 Create 猫饼穿脱显示配方。未完成构件只注册，沿用其他构件不进创造栏的规则。
- 潜水套装保留独立的倾斜身体根节点，按其原坐标适配站立／坐姿跟随和猫饼身体压缩；没有把它强行套用另两套的根节点。
- **仅外观和职业身份预览，不实现本次提出的战斗／工作建议，不给予额外职业数值。** 新职业不接入正式职业配置、不继承旧职业作战或工作能力。没有编造套装制造成本与序列组装配方。
- 全程复用猫自身 9 格普通物品栏，不增加 27 格容器、不清除任何历史物品；不改动既有十职业和钓鱼平衡。
- 通信版本升级为 Forge 28 / NeoForge 21，客户端和服务器应同步使用本构建。
- [推荐战斗与工作设计 v1](specialist-careers-design.md) 单独交付，明确标记为待确认草案。蟑螂包自带扑翼动画目前仅保存，没有自动循环播放。

## 验证

- Forge / NeoForge 完整 build 通过；正式 JAR 内套装、贴图、图标与 Create 穿脱配方全部检查通过。
- 双端有 / 无 KubeJS 四组真实服务端 GameTest 最终均为 24 项通过；新增测试覆盖稳定枚举、注册物品、实际穿脱与消耗、重复穿戴不消耗、没有继承职业能力／属性、9 格末槽物品保留、猫饼和实体存档重载、过滤器与真实配方管理器。
- Forge + KubeJS 首轮旧 medicalTriage 测试在坐下后的回血断言失败。该测试可能在当 tick 已收集治疗请求、尚未到服务器 END 结算时读取血量。测试改为先验证立即停止施法，允许当 tick 最多结算一次已提交的 0.25 秒治疗，再验证后续 20 tick 不新增治疗。只修正两端测试时序，未修改医疗机制；重跑两端带 KubeJS 测试通过。
- 双端配置回归通过，包括各 141,606 项套装检查、16,402 项职业重置／击退检查；原有全局／世界配置和自定义值保留。
- 全部 13 个静态检查脚本通过，其中新增职业／资源／预览安全接线 184 项。旧激光轮盘测试从只排除物流职业的过时文本检查更新为现有辅助职业和预览职业的完整排除条件。
- 双端真实 GPU 渲染：每端三套外观 × 站立／坐姿，共 12 次绘制，包含转头与身体根节点跟随；已逐一查看 6 张输出图片。已有医疗／音乐特效探针同时通过。本探针使用原生猫网格和生产套装渲染器，未代替完整整合包中的实际游玩验收。
- 打包检查确认 12 份模型／纹理文件与 [美术来源清单](../art/specialist-suits-v1/manifest.json) 哈希一致，6 个物品模型和6个穿脱配方齐全；无 GameTest 或客户端视觉探针泄漏到正式 JAR，没有新增必需模组依赖。保留既有动画资源哈希。

## 部署结果

部署前后没有 Java 游戏进程，25565 / 25566 / 25575 / 25576 均未监听。四处均按包内 modId=laowu 确认只启用一个版本，安装包与最终校验的构建产物 SHA-256 一致：

- Forge 客户端与测试服：3551ACB0748A377EEBEB87A6A952BC5FE1AFDF926D4E363C54EC7F8DA70998B7
- NeoForge 客户端与测试服：A6510A2FA8CB2D307A558802384D0335B98D11182090FA8A68AA1E614D1A8CD5

安装位置：

- D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.21-forge-1.20.1.jar
- D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/local-multiplayer-server/mods/create-meowchanics-2.1.4-dev.accessories.21-forge-1.20.1.jar
- D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.21-neoforge-1.21.1.jar
- D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/local-multiplayer-server/mods/create-meowchanics-2.1.4-dev.accessories.21-neoforge-1.21.1.jar

各实例旧 accessories.20 保存在对应根目录的 mod-backups/create-meowchanics/20260915-112317-accessories.21/，核对原包哈希后移入，可恢复：

- Forge 旧包：6B220293F3B7A55FD65AC7748736CDB63C2BEE79891A76FCA3254A102AF58C6A
- NeoForge 旧包：8B6FC9299ED57384C6702526A8F4A6767CC950840428194E46C7C4063B52078C

部署先完整预检四个目标、暂存校验新包，再替换并核对唯一性；最终构建导致 Forge 新包哈希更新，第一次预检在任何文件移动前安全停止，按最终验证产物更新预期哈希后重新部署成功。未修改用户存档、配置或其他模组，未提交、推送或公开发布。

两台测试服继续保持关闭，没有重启。此前未复现的仓管 UI 问题不在本次改动范围内，也没有宣称已修复。
