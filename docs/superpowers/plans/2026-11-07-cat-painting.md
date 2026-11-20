# 猫画编辑桌 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Forge 与 NeoForge 中提供可导入本地图片、自动联机同步并挂墙展示的猫画，以及 Create 风格的猫画编辑桌。

**Architecture:** 图像算法和有界传输组件使用独立 Java 类，双端保持同等行为；加载器适配负责物品数据、注册、网络和事件。图片由世界图片库持久化，物品与实体只存哈希及参数；Create 编辑桌通过核心接口接入事务服务，不使猫画实体依赖 Create。

**Tech Stack:** Java 17／21、Minecraft 1.20.1 Forge／1.21.1 NeoForge、Create 6.0.8／6.0.10、JDK ImageIO／BufferedImage、现有 ModNetwork、HangingEntity、NativeImage／DynamicTexture。

**Spec:** [已批准设计](../specs/2026-11-07-cat-painting-design.md)

## Global Constraints

- 双端交付；原有 ID、饰品 API v1–v3／schema 1、词条 API v1／schema 1 不变。未安装 Create 时已有猫画仍可显示、拆卸、重新放置，编辑桌不注册。
- 宽高各 1–8 格；像素模式每格 16×16 像素，原生最长边至多 2048；宽度保留／高度保留按设计居中裁剪或补浅米白，不拉伸。
- 原文件最多 16 MiB、最长边 8192、总像素最多 1600 万；处理后仅 PNG，最大 2048×2048、8 MiB；每块数据最多 24 KiB。
- 每玩家一个并发上传、每分钟至多四次印制提交、空闲会话 30 秒超时；默认世界图片库限 1 GiB；客户端磁盘缓存 256 MiB、GPU 缓存 64 MiB。
- 仅读取游戏目录 cat_paintings/imports 的直接 PNG/JPG/JPEG 文件；不跟随目录外链接，不上传原文件路径，不使用外部图片服务。
- 物品图标来自 C:/Users/16611/Downloads/painting.png；墙上画框使用 laowu:textures/block/cat_casing.png 风格。首版仅创造栏获取，不增加配方。
- 沿用当前 develop 工作树，保留所有已有未提交修改。不因技能中的常规 commit 步骤自动提交或推送；每个任务用检查记录替代提交门。公开版本仍为 2.3.2，测试包使用独立内部编号 painting.1。
- 禁止同一加载器并行执行会写同一 build/classes、build/resources 的 Gradle 作业；双端可并行。只有备份校验与进程检查通过才部署。

## Review Focus

1. 上传结束时菜单已关闭、槽内容已换或玩家离线：任务 4 的事务测试必须断言旧画保留、没有新物品生成。
2. 图片头小但像素尺寸巨大、截断或哈希伪造：任务 1／2 必须在完整解码或磁盘分配前拒绝；不存在越界写入或无限内存缓冲。
3. 跨世界同一哈希和取消后迟到回包：任务 6 必须隔离缓存和请求代次，旧回包不能覆盖当前世界的纹理。
4. 偶数宽高、部分墙体缺失或未加载区块：任务 3 必须验证完整占位，无效放置不扣物品、不加载额外区块。
5. 世界重启后新增的第二客户端看图：任务 8 必须在删除原图和清空第二客户端缓存后验证从服务器恢复，而非依赖上传者本地缓存。

## 文件边界和依赖

下列相对路径均在两个源码根分别创建：forge-1.20.1/src/main/java/cn/laowu/mod/ 与 neoforge-1.21.1/src/main/java/cn/laowu/mod/。纯 Java 文件也镜像到双端；不新建共享 Gradle 模块。测试脚本编译两个副本并运行同一断言，防止只修一个加载器。

- painting/PaintingSpec.java、PaintingImages.java：参数、图片头检查、裁剪采样和标准 PNG，不引用 Minecraft、客户端或 Create。
- painting/PaintingAssetStore.java、PaintingChunkAssembler.java、PaintingWire.java：世界资产 IO、会话缓冲、与加载器无关的有界消息编码。
- painting/CatPaintingData.java、PaintingEditorAccess.java、PaintingService.java、PaintingServerEvents.java、PaintingCommands.java：物品数据、事务接口、服务生命周期与管理占用信息。
- item/CatPaintingItem.java、entity/CatPaintingEntity.java：放置及挂墙实体。
- create/CatPaintingTableRegistration.java、CatPaintingTableBlock.java、CatPaintingTableBlockEntity.java、CatPaintingTableMenu.java：Create 可选注册、容器和槽位事务。
- network/PaintingPacket.java 与现有 network/ModNetwork.java：加载器协议封装与注册。
- client/painting/PaintingImportFiles.java、PaintingClientCache.java、PaintingTextures.java、CatPaintingRenderer.java、CatPaintingItemRenderer.java：本地目录、下载、缓存、核心渲染，不引用 Create。
- compat/create/client/CatPaintingTableScreen.java：Create 屏幕和控件，仅通过已存在的 Create 客户端注册路径加载。
- 修改双端 LaoWuMod.java、create/CatMachineBlocks.java、compat/create/CreateClientEvents.java 及现有客户端实体／物品渲染注册入口；只增加猫画所需条目，不重构原注册器。
- 双端资源目录增加 item/cat_painting 图标与模型、blockstates/cat_painting_table.json、models/block/cat_painting_table.json、models/item/cat_painting_table.json 和中英文语言键；表模型先引用 create:block/schematic_table，仅在 Create 下使用。

任务顺序：1 → 2 → 3 → 4 → 5 → 6 → 7 → 8。任务 3 可在任务 2 的接口固定后与存储细节并行；单人实现时保持此顺序。

## Task 1 图像算法和参数契约

**Files:** 双端 painting/PaintingSpec.java、painting/PaintingImages.java；tests/painting/PaintingImagesRegression.java；tests/run-painting-core.ps1。

**Interfaces:** `record PaintingSpec(int widthBlocks, int heightBlocks, Mode mode, Crop crop)`，内部枚举 `Mode { ORIGINAL, PIXEL }`、`Crop { KEEP_WIDTH, KEEP_HEIGHT }`；构造时验证 1–8。`PaintingImages.prepare(byte[] source, PaintingSpec spec): byte[]` 返回标准不透明 PNG；`validateUpload(byte[] png, PaintingSpec spec): byte[]` 解码前验证头，校验处理后尺寸并去除元数据返回标准 PNG。失败抛带错误码的 IOException 或 IllegalArgumentException，不部分返回。

- [ ] 写失败断言：`new PaintingSpec(0,1,PIXEL,KEEP_WIDTH)` 被拒绝；红／蓝两半 32×16 图生成 1×1 PIXEL 时结果为 16×16，KEEP_WIDTH 的上下第 0 行是底色，KEEP_HEIGHT 的上下第 0 行不是底色；2×1 PIXEL 得到 32×16；透明像素合成为 `0xFFF3E5C8`。
- [ ] 用真实 ImageIO 生成 PNG/JPG、损坏头、伪造超限头，运行 `./tests/run-painting-core.ps1 -Suite Images`，确认先因算法缺失失败，不把测试语法错误算 RED。
- [ ] 实现头检查、限长读取和居中等比变换。PIXEL 用最近邻采样；ORIGINAL 每格像素数取 `max(1,min(2048/max(w,h),ceil(max(sourceW/w,sourceH/h))))`，乘格数得到输出尺寸，平滑采样。先合成底色再输出 PNG；只读图片首帧，不接受动画格式伪装。
- [ ] 补正交断言：1×8／8×1／8×8、两个保留轴、16 MiB 和 1600 万像素边界、2048 上限、最终 8 MiB 上限；验证输出无元数据，源图像不修改。
- [ ] 同一脚本用 Java 17 和 21 分别编译两个副本，全部通过后记录接口，保持改动未提交。

## Task 2 世界资产库和有界消息

**Files:** 双端 painting/PaintingAssetStore.java、PaintingChunkAssembler.java、PaintingWire.java；tests/painting/PaintingStorageRegression.java、PaintingWireRegression.java；扩展 tests/run-painting-core.ps1。

**Interfaces:** `PaintingAssetStore(Path worldRoot,long quotaBytes)`；`put(byte[] canonicalPng, PaintingSpec spec, UUID owner): AssetInfo`、`read(String sha256): byte[]`、`usedBytes(): long`；`record AssetInfo(String hash,int pixelWidth,int pixelHeight,long bytes,UUID owner)`。`PaintingChunkAssembler(UUID session,int totalBytes,long nowMillis)`；`accept(int offset,byte[] bytes,long nowMillis): void`、`complete(): boolean`、`finish(): byte[]`、`expired(long nowMillis): boolean`。`PaintingWire.encode(Message): byte[]`、`decode(byte[]): Message`。

- [ ] 先写并跑 RED：同 PNG 两次 put 只计一次磁盘占用；重开 store 后 read 的 SHA-256 相同；`../x` 被拒绝；额度不足保留旧资产。
- [ ] 消息使用 sealed Message 的 Begin(menuId,revision,spec,totalBytes,hash)、Chunk(session,offset,bytes)、Commit(session)、Cancel(session)、Fetch(hash)、Manifest(session,hash,totalBytes)、Ack(session,nextOffset)、Result(session,status,hash,error)、DownloadChunk(session,offset,bytes) 类型。限制编码总消息不超过 24 KiB+1024 字节，区分 C2S 与 S2C 合法类型；禁止未知枚举、负长度、越界偏移和字符串无限分配。
- [ ] 写失败断言：24 KiB 正常分块完成；24 KiB+1 块拒绝；相同偏移相同字节可重发但不重复计数，内容冲突、乱序或越界块拒绝；30 秒空闲超时；不完整 finish 拒绝。
- [ ] 实现 worldRoot/laowu/cat_paintings 的哈希路径及原子 PNG 写入，内存索引后持久化；事务失败释放额度预留和临时文件。启动可从合法 PNG 重建索引，不自动删除看似未引用的旧图。
- [ ] 运行 `./tests/run-painting-core.ps1 -Suite Storage,Wire` 验证双端副本；追加磁盘写失败、损坏资产、伪造哈希和重启断言。不进入 Minecraft 启动即可独立验收。

## Task 3 核心猫画物品与挂墙实体

**Files:** 双端 painting/CatPaintingData.java、item/CatPaintingItem.java、entity/CatPaintingEntity.java、LaoWuMod.java；双端 assets/laowu/models/item/cat_painting.json、textures/item/cat_painting.png、lang/zh_cn.json、lang/en_us.json；tests/gametest/forge/cn/laowu/mod/test/CatPaintingProbe.java 和对应 neo 文件；tests/painting-gametest.init.gradle。

**Interfaces:** `record CatPaintingData(int schema,String hash,PaintingSpec spec,UUID author)`；`read(ItemStack): Optional<CatPaintingData>`、`write(ItemStack,CatPaintingData): void`、`createStack(CatPaintingData): ItemStack`。`CatPaintingEntity` 扩展 HangingEntity，提供 `getPaintingData()`／`setPaintingData(CatPaintingData)`，同步尺寸和资产编号。schema 固定 1，校验 64 位十六进制哈希，不存文件路径或图像字节。

- [ ] 写真实 GameTest RED：空白画不能挂墙；1×1 四向成功且生存扣一个；创造不扣；墙体部分缺失、未加载区块、越界和实体占位失败不扣。
- [ ] 注册核心物品和实体 laowu:cat_painting，最大堆叠数 1；把给定 painting.png 原样复制为物品资源，不重新生成图标。实现 Forge NBT 与 Neo CustomData 对等读写，完整数据缺失或损坏时作为空白处理并给出说明。
- [ ] 按原版画的偶数居中规则计算整幅碰撞、支撑和墙面法向；成功生成后才消费物品。依据实际版本 HangingEntity API 保留爆炸、创造、游戏规则掉落语义；掉落的数据不丢失且不重复。
- [ ] 加入保存重载、2×2、8×1、1×8、支撑移除、玩家权限拒绝、重复伤害只掉一次等断言。测试夹具可用任务 2 写入已知图片，不需要编辑桌。
- [ ] 双端运行 `gradlew runGameTestServer --init-script ../tests/painting-gametest.init.gradle --no-configuration-cache -PpaintingProbe=entity -PpaintingRunId=entity-green`；另加 `-PwithoutCreate` 再跑，断言核心实体没有隐式 Create 依赖。

## Task 4 服务器事务与网络适配

**Files:** 双端 painting/PaintingEditorAccess.java、PaintingService.java、PaintingServerEvents.java、PaintingCommands.java、network/PaintingPacket.java、network/ModNetwork.java；tests/gametest/{forge,neo}/cn/laowu/mod/test/CatPaintingTransactionProbe.java；扩展 painting-gametest.init.gradle。

**Interfaces:** 核心 `PaintingEditorAccess` 定义 `revision(): long`、`lock(UUID token,long expectedRevision): boolean`、`unlock(UUID token): void`、`commit(UUID token,long expectedRevision,CatPaintingData): boolean`。菜单实现该接口，core 不引用 Create 类。`PaintingService.begin(ServerPlayer,int menuId,long revision,PaintingSpec,int totalBytes,String hash): UUID`、`accept(ServerPlayer,UUID,int offset,byte[]): void`、`commit(ServerPlayer,UUID): void`、`cancel(ServerPlayer,UUID): void`、`request(ServerPlayer,String hash): void`，全部外部调用先在服务器线程验证，IO 在有界线程池完成。

- [ ] 先以真实测试菜单实现 PaintingEditorAccess 写 RED：上传成功只修改槽内一个画；关闭／改槽／离线／破坏后到达的完成回调不提交；失败重印保留原 hash。
- [ ] 以 player UUID+连接会话+menuId+槽 revision+随机 token 绑定事务，过期、无权限或距离不合法立即取消。后台完成时再次回到服务器线程复核全部条件；持久化成功不等于物品提交成功，不伪造替代物品。
- [ ] 每玩家 1 会话，四次／分钟，30 秒超时；服务端全局最多 8 上传会话。IO 用两线程有界队列，超载拒绝而非堆积。提供服务器配置对应配额和管理员权限 2 的 `/laowu paintings status` 占用查询；停止服务器时取消回调并收尾，不挂住 tick。
- [ ] 在既有 ModNetwork 追加独立猫画消息注册，不改变旧消息序号或接口；Forge 协议版本由当前 50 递增为 51，Neo 由 43 递增为 44，实施前若已有其他改动则重新读取后递增。不得使 dedicated server 解析客户端类。
- [ ] Fetch 必须由正在追踪画作实体、打开含该物品的菜单、或持有相关画作的玩家发起；菜单同步／实体追踪授予当前连接临时 hash 访问凭证，关闭或停止追踪后撤销。下载也有并发和速率上限，不扫描任意世界文件。
- [ ] 通过 `-PpaintingProbe=transaction` 跑两端真实测试；增加伪造 menuId、重复 Commit、失效凭证、8 MiB+1、洪泛及服务端关闭回调断言。以接收失败和状态不变证明拒绝，不只断言假回调被调用。

## Task 5 编辑桌方块与安全菜单

**Files:** 双端 create/CatPaintingTableRegistration.java、CatPaintingTableBlock.java、CatPaintingTableBlockEntity.java、CatPaintingTableMenu.java、create/CatMachineBlocks.java；双端编辑桌 blockstate 和 block/item model；tests/gametest/{forge,neo}/cn/laowu/mod/test/CatPaintingMenuProbe.java。

**Interfaces:** `CatPaintingTableBlockEntity` 持有一个画作槽、revision 与锁 token，保存槽位；`CatPaintingTableMenu` 扩展 AbstractContainerMenu 并实现任务 4 的 PaintingEditorAccess。服务端 stillValid 检查桌存在、距离及权限；客户端构造仅接收 blockPos 和正常槽位同步数据。

- [ ] 写 RED：非猫画拒绝，shift-click 只移动一个猫画，锁定时点击／shift-click／热键换槽均不能取走；确认成功原槽替换不增物；关闭／桌破坏取消任务但正常留存或掉落旧槽物品。
- [ ] 在可选 CatMachineBlocks.register 路径注册 laowu:cat_painting_table 的方块、物品、BE 和菜单，不从核心静态字段泄漏 Create 类型。模型 parent 使用 create:block/schematic_table，方块四向和体积对齐原蓝图桌，不改 Create 自身。
- [ ] 将槽 revision 与每次合法修改绑定，锁定会话中禁止漏斗／其他能力接口绕过菜单换物；不暴露自动化存取接口。server commit 复核锁、revision、物品类型后就地写数据。
- [ ] 跑 `-PpaintingProbe=menu`，检查存档重载、两玩家同时操作同一桌、桌破坏后迟到提交；Create 缺失时桌子和菜单不注册但核心画作测试仍通过。旧 schema／ID 不变。

## Task 6 本地导入和客户端画作显示

**Files:** 双端 client/painting/PaintingImportFiles.java、PaintingClientCache.java、PaintingTextures.java、CatPaintingRenderer.java、CatPaintingItemRenderer.java；客户端实体／物品注册入口；tests/painting/PaintingImportRegression.java、PaintingCacheRegression.java；tests/painting-client/{forge,neo}/cn/laowu/mod/client/CatPaintingClientProbe.java 与 tests/painting-client.init.gradle。

**Interfaces:** `PaintingImportFiles.list(Path gameDir): List<Path>`、`read(Path gameDir,Path selected): byte[]`；`PaintingClientCache.switchWorld(String namespace)`、`request(String hash)`、`accept(PaintingWire.Message)`、`close()`；`PaintingTextures.resolve(String hash): ResourceLocation` 返回已就绪纹理或占位纹理，不能阻塞渲染线程。

- [ ] 写 RED：目录仅列直接 png/jpg/jpeg、拒绝 ../ 和目录外符号链接、不读超限文件；同 hash 重复 request 不重复下载；switchWorld 后旧下载响应不得注册到新世界。
- [ ] 用任务 1 的算法和目录边界实现本地预览准备，读取／编码工作在后台，选择项改变使用代次丢弃旧结果。只有屏幕确认印制才发 Begin；预览不得上传任何字节。
- [ ] 实现按服务器地址+服务器下发持久世界随机 ID 隔离的磁盘缓存和哈希验证；256 MiB 磁盘／64 MiB GPU LRU，过大单图拒绝，DynamicTexture 注册／释放在渲染线程。断线和资源重载清理 GPU 状态，磁盘缓存可复用但仍需当前世界授权。
- [ ] 渲染器画出薄背板、四边框和正面图像；正侧背采用现有 cat_casing 纹理并平铺，尺寸包含画框。菜单预览复用同一绘制算法和色调；世界画面按光照分格绘制，避免整幅大画只取一个位置亮度。
- [ ] 单元脚本 Images/Import/Cache 通过；运行独立 `runClient --init-script ../tests/painting-client.init.gradle --no-configuration-cache -PpaintingClientRunId=core-render`，截图检查物品、四向实体、原生／像素清晰度、缺图占位、无 Create 显示和纹理释放。

## Task 7 Create 风格编辑界面

**Files:** 双端 compat/create/client/CatPaintingTableScreen.java、compat/create/CreateClientEvents.java 及任务 5 的注册类；双端语言键；tests/painting-client/{forge,neo}/cn/laowu/mod/client/CatPaintingScreenProbe.java。

**Interfaces:** `CatPaintingTableScreen` 扩展 `AbstractSimiContainerScreen<CatPaintingTableMenu>`，消费任务 6 的异步目录和预览接口，以及任务 4 的消息服务；不读取蓝图桌私有字段。

- [ ] 先写屏幕探针 RED：菜单可打开关闭，操作一个猫画槽、目录、刷新、文件选择、模式、宽、高、保留轴、确认和进度控件；没有待处理预览或槽为空时确认禁用。
- [ ] 复用 Create AllGuiTextures、IconButton、ScrollInput、Label 和玩家背包布局。先取得实际蓝图桌材质坐标再摆放，不遮盖蓝图原控件冒充新界面；控件 bounds 与预览／输入框无重叠，长文件名截断加悬浮全文。
- [ ] 选择图片或修改参数仅更新预览；确认后冻结本地提交快照并显示服务器 ACK 进度，成功／失败／取消信息明确。ESC 关闭时发 Cancel，不能只关可见控件而保留后台提交。
- [ ] 从给定图标和现有猫机壳素材复用资源，记录必要许可署名；不生成替代图标，不增加无作用的参考槽。
- [ ] 两端跑 `-PpaintingClientRunId=editor`，截图 1×1、横／竖比例、最大宽高、长文件名、无图片、上传中与错误状态；探针点击实际控件并校验选择值和待发送消息，不仅验证控件存在。

## Task 8 联机端到端和交付

**Files:** tests/painting-multiplayer.init.gradle、tests/painting-multiplayer/{forge,neo}/cn/laowu/mod/test/CatPaintingMultiplayerProbe.java、tests/run-painting-multiplayer.ps1；docs/deployment-cat-painting-painting.1.json；适用的功能说明和更新日志，不改已有发布历史。

**Interfaces:** 测试脚本 `-Loader forge|neo -RunId <unique>` 创建独立 server、clientA、clientB 目录；只启动和结束脚本自己的进程，不复制账号令牌、用户世界或覆盖用户文件。服务端和两个客户端只加载正常模组及隔离测试探针，测试支持本地离线 UUID。

- [ ] 先跑首次完整流：A 导入真实 PNG、客户端仅预览时服务器没有资产；A 确认后得到带 hash 的画；挂墙后 B 自动获取并显示同图。JPG、原生、像素各覆盖一次。
- [ ] 删除测试导入目录原图，停止并重启测试服务器，清空 B 的缓存再进入；验证 B 从世界图片库重新取得正确 hash，画面、尺寸和边框仍一致。验证同图两份物品只占用一个资产。
- [ ] 跑断网／关闭屏幕／拿走物品／桌破坏／超限图／伪造分块，断言失败不吞物、不复制画；多玩家下载不造成服务器 tick 明显阻塞。记录截图、错误拒绝和最终物品数。
- [ ] 两端执行正常 `gradlew build --no-configuration-cache '-Pinternal_build=painting.1'`；保留 API 冻结、饰品检查和 verifyReleaseIsolation。检查成品 JAR 没有测试类、测试世界、缓存、个人导入图片或 SDK。测试源集单独注册，不让它们进入 main。
- [ ] 对照已批准设计逐项自审并做独立代码审查；修正 Critical/Important 后重跑受影响测试，说明仍未覆盖的条件，不把命令退出 0 当作游戏探针成功。
- [ ] 检查实际开发客户端进程，运行时先请用户退出。通过 tools/deploy-cat-additions.ps1 的正常双端备份／哈希／单 laowu JAR 流程部署 `2.3.2-dev.painting.1`，保存部署记录；未获新请求不提交、推送、合并 main 或发布平台。

## 自审结果与执行门

- 设计的算法、原生／像素、裁剪留边、Create 复用、目录隐私、世界保存、联机授权、挂墙判定、无 Create、缓存／配额、完整回归和部署要求均有归属任务。
- 以上类型和方法是跨任务契约；加载器方法签名差异由所属任务适配，不通过给另一个任务添加隐式依赖来绕过。
- 每个任务执行 RED → GREEN → 回归检查，保留失败和成功证据。出现设计级变化先说明，不能因实现困难静默删减联机或事务要求。
- 用户已批准产品设计并要求直接开始；采用当前会话连续实施，不再增加流程确认。实现、测试与部署进度记录在本计划的执行账本中。
