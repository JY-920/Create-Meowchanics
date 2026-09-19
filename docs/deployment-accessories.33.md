# accessories.33：心愿条件导入与六套组装配方

已部署：2026-09-17 19:35:18（北京时间），内部版本 2.1.4-dev.accessories.33，公开版本仍为 2.1.4。

## 本轮变更

- 猫咪过滤器右键心愿领养箱：重置所有旧条件后导入当前 NOW／MAX 范围；仅复制当前交易，不持续追踪刷新。
- NOW 专门匹配基础值，MAX 匹配属性上限；普通手动过滤器的当前有效值口径不变。持久化、菜单同步、编辑保存及说明均保留这一差别。
- 新增工程师、医疗、音乐、特工、潜水员、蟑螂六套机械动力序列组装配方，均以猫咪构件为底料，依用户材料顺序装配，1 次循环、100% 输出 1 件。
- 全部 15 个 incomplete 物品改为 Create 原生 SequencedAssemblyItem，使用原有 ID／贴图、原生单件堆叠及进度条。猫咪构件原有 80% 成功率与副产物不变。
- 未改动上轮心愿箱 GUI 坐标、用户存档、配置、其他模组，未添加饰品合成配方。
- 玩法与完整材料表见 [wish-filter-and-suit-assembly.md](wish-filter-and-suit-assembly.md)。

## 验证

- Forge Java 17 / NeoForge Java 21：compileJava、build 均成功。
- 两端实际 GameTest 含 KubeJS 各 63/63；不含 KubeJS 各 63/63，新增 3 项涵盖：
  - 重置旧条件、3000 组 NOW/MAX 判定、词条提升与基础值差异、物品与菜单数据保存。
  - 真实右键事件截获、旁观／距离／隔墙／旧编辑窗口保护。
  - 实际 Create 配方管理器查找、15 条组装链、31 个机械手步骤、阶段序列化和进度、正确最终产物。
- 两端实际客户端 GPU 测试通过；15 个半成品贴图和原生进度条均已实际渲染并逐端查看，导入 NOW 的“基础值”页签／摘要及重置验证通过。
- 图片保存在 art/assembly-previews/accessories33/forge.png、neo.png。猫手雷半成品沿用原有猫壳贴图，并非缺失贴图。
- 23 组 Node 回归全部通过，包括新增 335 项本轮接线／材料／资源检查。
- run-global-world-config.ps1 全部通过：原有过滤规则、布尔真值表、旧 NBT 与配置回归无变化。
- JAR 包核验通过：实际版本、15 条配方与资源一致、36 个饰品资产、无饰品制作配方、无测试类泄漏。新增客户端探针不随成品打包。

### 复核记录

首次新增测试曾有测试端假设错误：误把原有猫咪构件视为 100% 输出、Neo 的 rollResults 必须提供随机源、猫手雷半成品有意复用猫壳贴图。修正测试以匹配原有规则及真实 API 后通过，没有为使测试通过而修改这些原有规则或素材。

NeoForge 无 KubeJS 全量回归首次出现既有 agentbackstabandretreat 用例的 “Retreat actually moves, grenade actually breaks aggro” 间歇性失败；同版在含 KubeJS 全量及随后无 KubeJS 全量复测均通过。本轮不修改特工生产逻辑或弱化该断言。首次和复测日志分别保留在 build/accessories33-no-kubejs.log 与 build/accessories33-no-kubejs-recheck.log。

## 部署核验

- Forge SHA-256：CB360E58117EA5FE39BCDF29458EB735A9C8E1717B015623331C57766D20FA97
- NeoForge SHA-256：BA912C78CF9FE79C4F0A1D92ABC9161B111EB756FA7F07D8FE5B8217E2D7CC0D
- 两个本地客户端及各自 local-multiplayer-server 均已更新。
- 四个目标均按实际 mods 元数据核实，只有一个启用的 laowu JAR，新包与构建产物哈希一致。
- 原 accessories.32 包保存在各目标 mod-backups/create-meowchanics/20260917-193513-accessories.33/，备份哈希验证通过。
- 部署前确认游戏退出，未强制关闭；两台测试服保持关闭，端口无监听。
- 未提交、推送或公开发布。完整路径与备份记录见 deployment-accessories.33.json。
