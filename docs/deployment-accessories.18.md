# accessories.18：联机编辑器修复与 NeoForge 本机测试服

2026-09-14 19:03 部署，公开版本仍为 2.1.4，内部构建 2.1.4-dev.accessories.18。

## 原因与修改

Forge 1.20.1 的 ServerboundContainerButtonClickPacket 使用 writeByte/readByte 编解码按钮编号，可表示范围 -128～127。当前有 82 个词条，“严选耄耋”的添加编号为 136，“鸿运当头”为 156；经过真实网络编解码后变负数。词条的 Shift 操作额外加 1000，属性上限 Shift 增加使用 130～135，也超出范围。集成服务器的本地通道可以不经过字节序列化，因此单人表现可能正常。

双端词条/属性调整棒改用仅发往服务端的 CatEditorActionPacket，容器编号和动作编号均使用 VarInt。服务器检查当前菜单类型、菜单编号、可达且存活的目标、非旁观状态；菜单拒绝越界动作。保留四词条容量、互斥关系、黑名单、等级上限、普通及 Shift 操作语义，兼容掉落猫饼。未修改医疗、飞行、仓管界面或渲染状态。

Forge 通信版本 24 → 25，NeoForge 17 → 18，客户端和服务端必须同时更新。

## 验证

- Forge / NeoForge build 各通过 23,120 项饰品测试。
- 双端独立 GameTest（无 KubeJS 变体）各 17 项全部通过。
- 新增 EditorNetworkingProbe，经过真实 ByteBuf/StreamCodec 编解码后验证全部 82 个词条的添加、升级、删除、Shift 快速操作，四词条限制、互斥规则与属性 Shift 调整；检查错误容器、越界编号、远距离、已删除目标和旁观者请求，覆盖实体猫与掉落猫饼。
- 飞行 wiring 133 项、医疗 wiring 49 项、饰品 wiring 391 项通过。
- 双端 JAR 校验通过，确认包含新通信包，不包含 GameTest 探针。
- 两台生产服务端均出现 Done，实际 Minecraft 状态握手及本机 RCON list 成功。尚未代替用户实际登录复现仓管 UI。

## 部署

部署前确认无游戏客户端进程，Forge 测试服无人在线，使用 RCON stop 正常保存并退出后换包。未改动客户端存档、配置或其他模组。按 modId=laowu 检查唯一启用包并校验 SHA-256。

- Forge 客户端与测试服：15F657D519EA43F9F95C8156FD8D050A2CB410717AA7AEE70A603AB6CFBA2F7F
- NeoForge 客户端与测试服：1BFAA4E68D080652D54B49F8E7DB0D4482E3B1E466CA5413C3355135C5EF3F50

两个客户端实例和 Forge 测试服的旧 accessories.17 包均保存在各自目录下：
`mod-backups/create-meowchanics/20260914-190333-accessories.18/`。
NeoForge 测试服为新建目录，无旧包覆盖。

## 本机服务器

| 客户端实例 | 地址 | 加载器 | Create | 本次启动 PID |
| --- | --- | --- | --- | --- |
| 1.20.1-Forge模组开发 | 127.0.0.1:25565 | Forge 47.4.22 | 6.0.8 | 23244 |
| 1.21.1-NeoForge模组开发 | 127.0.0.1:25566 | NeoForge 21.1.248 | 6.0.10 | 20756 |

PID 仅供本次启动记录，后续请查看各服 running-server.json。

各服位于对应客户端实例下的 local-multiplayer-server，独立超平坦创造存档，InstantRainbow 为 OP4。游戏端口和 RCON（25575/25576）只监听 127.0.0.1；online-mode=false 仅用于隔离本机测试，不添加防火墙规则或公网映射。NeoForge 复用已有的 EULA 接受配置，保留航空学、Sable、Create 扩展与咒术猫咪，服务端包均来自现有客户端并校验相同 SHA，排除纯客户端 UI/渲染模组。

每个目录提供 start-server.ps1 / server-command.ps1 / stop-server.ps1；普通日志在 logs/latest.log，后台输出在 console-logs。

NeoForge 启动日志存在 Sable 对 copycats:copycat_catwalk 的未知方块提示及 Copycats 混入警告，服务端仍完成启动；未为消除此提示修改其他模组。

仓管 UI 仍待用户在 NeoForge 客户端实际复现。Forge 与 NeoForge 不仅加载器不同，MC、Create 版本及模组组合也不同，因此即使只在 NeoForge 环境出现，也不能单凭这一对比断定是 NeoForge 本身的问题。
