# 特工猫坐垫警戒 — accessories.24

内部构建：2.1.4-dev.accessories.24；公开版本仍为 2.1.4。本次不改动特工战斗机制或其他职业平衡。

## 规则

- 驯服的特工猫坐在机械动力坐垫上时警戒。普通地面坐下不触发。
- 三维球形半径为 `min(64, max(0, 有效智力) × 0.64)` 格：0 智力为 0，50 为 32，100 及以上为 64。
- 使用有效智力（含既有属性加成），不再乘职业数值缩放系数，以保证 100 智力对应 64 格的硬上限。
- 每秒扫描已加载的敌对生物；包含上下方、障碍后的目标和隐身敌人，不强制加载区块。
- 怪物类别/Enemy 接口的生物视为敌对；其他中立生物正在攻击猫、主人或友方宠物时也可被标记。友方、玩家、普通被动动物不标记。
- 客户端显示原版红色发光轮廓，颜色为 #FF3030。只影响绘制，不给实体添加发光药水，不改服务器实体发光标志，不改计分板队伍。
- 离座、卸下套装后撤销；目标离开范围在下一次扫描时撤销。死亡、卸载、跨世界由源检查及短期标记过期兜底。
- 多只特工的警戒合并后同步，停用其中一只不会清除另一只仍覆盖的敌人。
- 玩家后来开始追踪实体时补发；客户端按世界、实体 UUID 和运行时 ID 共同校验，避免实体 ID 复用、换世界残留。
- 保留既有猫咪资料预览、打包、NoAI、激光命令等工作暂停规则。
- 默认介绍保留战斗/工作短说明及 Ctrl/Shift 提示；Ctrl 展示公式与触发条件，Shift 展示当前属性的警戒半径。

## 变更位置

两个加载器均新增 `CatAgentWatch`、`CatAgentWatchClient`、`AgentWatchPacket`；挂接职业 tick、世界 tick END、StartTracking 和既有客户端描边 Mixin。协议升级为 Forge 31 / NeoForge 24，客户端与服务器必须同步更新。

## 已完成验证

- 带 KubeJS 的双端完整构建及各 32 项真实 GameTest 通过：`<port>/build/agent24-gametest.log`。
- 不带 KubeJS 的双端完整构建及各 32 项 GameTest 通过：`<port>/build/agent24-final-no-kubejs.log`。
- 新增两项警戒场景：32/64 格边界与上限、动态智力、三维距离、隔墙/隐形、敌友筛选、数据包编解码、双猫覆盖、预览暂停、离座/卸装、源实体删除、原版队伍与发光标志不变。
- 全部 16 组 Node 源码/资源检查通过，其中新增警戒线路守卫 102 项。
- 全量配置回归通过：`tests/build/agent24-config.log`。
- 正式 JAR 新类与资源检查通过，测试代码未打入包。Forge 最终仅对世界 tick 中一行缩进修正后重新 build 通过：`forge-1.20.1/build/agent24-final-build.log`。
- Forge 客户端真实描边/颜色 Mixin、标记生命周期及原版效果保留测试通过；原版描边 shader/post chain 输出红色空心轮廓。日志：`forge-1.20.1/build/agent24-client-verified.log`，截图：`forge-1.20.1/build/pilot-client-probe/agent-watch-red.png`。
- NeoForge 客户端真实描边/颜色 Mixin、标记生命周期、原版效果保留和 GPU 检查全部通过：`neoforge-1.21.1/build/agent24-client-complete.log`。截图：`neoforge-1.21.1/build/pilot-client-probe/agent-watch-red.png`。Forge 为 1380 个红色边缘像素，NeoForge 为 1658 个。
- 既有医疗/音乐 GPU 图标、职业动画、39 组本地化提示、扳手挂环等客户端回归随同通过。`git diff --check` 通过。

隔离客户端探针不连接用户服务器、不打开用户存档，测试目录在各端 build/pilot-client-probe，完成后自动退出。期间修正了测试替身的维度/伤害类型注册、客户端原版发光 flag 的模拟、1.21 顶点格式和矩阵初始化；这些属于探针前置条件修复，未放宽生产警戒规则。1.21 原版线性模糊比 1.20 更柔和，GPU 检查按实际红色边缘色相、覆盖像素与可见对比度判断，不以两版相同峰值亮度为前提。

## 产物与部署状态

- Forge SHA-256：`F710E8085AF4F357E483C4FC91D80CCF3B5C002DBC7F7B4B95D071EBF22F93EB`
- NeoForge SHA-256：`2BBED5F176997538FBC731A885CE67CE4A4858EE2B67725E7BF3B5C121C24D8F`
- **2026-09-16 15:38:17 已部署**：Forge 与 NeoForge 两个开发客户端及各自的 `local-multiplayer-server`，共四个目标。
- 询问期间用户游戏已退出，部署前后确认无 Java 进程，无 25565/25566/25575/25576 监听；本次使用默认严格检查，未启用无关进程豁免。
- 四个目标各仅有一个实际 `[[mods]] modId=laowu` 包，安装包 SHA-256 与上述构建产物一致。
- 旧 accessories.23 包均已可恢复地移动到各目标 `mod-backups/create-meowchanics/20260916-153813-accessories.24/`。旧 Forge SHA-256 为 `36A06BB0EE530C2797638C2BDF4A99DC35591BAAD8792B01B40453D3F83C5327`，旧 NeoForge 为 `AC859BB2624D891CC291CB332455E8CF05B171D56D4F21E7DD20768F16E4DBA3`。
- 回滚需先退出对应客户端/服务器，再把 .24 移出 mods，恢复上述 .23 备份；客户端和服务端一同回滚。未改动存档、实例配置或其他模组。
- 备份/替换脚本：`tests/build/deploy-agent-watch-accessories24.ps1`。默认拒绝所有未退出的 Java 进程；只有用户明确同意时才可对核实过路径的无关实例设置豁免，仍拒绝任何命令行引用本次目标的进程。
- 两台本地多人测试服保持关闭。无提交、推送或公开发布。
