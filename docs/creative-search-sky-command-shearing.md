# 创造搜索、上天猫指令与长毛剪毛修复

## 创造搜索重复（首先在 NeoForge 发现）

物品注册/创造标签内猫袋、猫盒、信息素猫粮各只有一个入口，实例里也只有一份本模组 jar。

已用实例中 JECharacters 4.5.27 的 `FakeArray` 复现：同一对象的 3 / 3 / 4 行文本分别包含“猫”时，搜索返回 3 个猫袋、3 个猫盒、4 个信息素猫粮，去重后仅 3 个对象。该适配器把每行命中直接作为一条结果返回。

双端在原版创造搜索刷新完成后，仅对 `laowu` 物品按完整数据去重，保留首个结果的顺序并刷新可见格子。Forge 比较 Item + NBT；NeoForge 比较 Item + Data Components。不同颜色、主人、容器内容等合法变体仍独立显示。不会删除物品注册、修改玩家背包、改动 JECharacters 配置/jar，或引入对它的硬依赖。其他模组物品不处理。

实现：`CatCreativeSearchResults`、仅客户端加载的 `CatCreativeSearchMixin`。Forge 的方法名和私有滚动字段同时兼容开发映射及 SRG 成品映射。

## 上天猫接到激光笔指令后悬停

旧流程在有激光笔指令时跳过自主飞行 tick，但保留无重力状态，仍让猫的地面导航从空中寻路，因而可能一直悬停。

现在仅在有效指令期间，为拥有上天猫词条的猫临时切换 `FlyingPathNavigation` 与限速的三维移动控制。使用原版寻路避开碰撞，命令目标、持续时间、归属/团队校验不变，不直接穿墙或传送。无套装猫依旧用原攻击，职业猫继续由原职业 AI 计算攻击、冷却和远程走位。

飞行员套装收到“攻击”指令时沿用其已有俯冲控制；“前往地点”仍使用三维指令寻路，不能因为临时自动索敌就误切回地面导航。

指令完成、失效、过期、坐下或移除词条后恢复原导航与移动控制。原控制器只存于临时控制器对象内，不写入猫咪 NBT，也不建立强引用的实体全局缓存。限速 0.45 格/tick，竖直分量上限 0.28；不改变伤害或攻速公式。

### 2026-09-11：导航接口导致启动崩溃

NeoForge 的 `crash-2026-09-11_08.31.10-client.txt` 在原版实体初始化阶段报出 `IllegalClassLoadError`：普通接口 `cn.laowu.mod.mixin.CatNavigationAccessor` 位于 Mixin 专用包，禁止被游戏直接加载。接口即使没有列入 mixin 配置，也受整个包的保留规则限制。Forge 同样存在此隐患。

双端将这个接口移至普通包 `cn.laowu.mod.CatNavigationAccessor`，同步修改实现和调用引用，保留真正的 `CatNavigationMixin` 注入。导航逻辑、职业配置、词条、NBT 和存档格式均不改变。使用 clean build 防止旧包路径的 class 残留。

验证：

- `tests/run-mixin-package.ps1` 检查双端成品 jar：保留包内的顶层类型均为已注册 Mixin、旧接口路径已不存在、新接口实现与调用一致、Forge 的两个字段已正确重映射。共 248 项通过；对部署前旧 NeoForge jar 的反向验证能准确检出本次错误。
- NeoForge 执行 `gradlew runData --init-script ../tests/mixin-startup.init.gradle --no-configuration-cache`，完成真实 ModLauncher/Mixin 引导、猫实体相关变换和模组注册。测试配置及输出仅放在 build/mixin-startup，不启动玩家存档、不生成生产资源、不修改 EULA。这是隔离加载验证，不等同于完整整合包客户端/进世界实测。
- 原有配置、套装、飞行指令、剪毛、过滤和弹幕回归继续通过。

## 长毛护体的机械手剪毛产量

Forge 的剪毛接口已计算词条，NeoForge 移植漏掉了这一段，返回固定 1 个猫毛。NeoForge 现与 Forge 一致，玩家剪刀与 Create 机械手共用标准剪毛接口：

- 无长毛护体：1 个。
- I / II：2 个。
- III / IV：3 个。
- V / VI：4 个。
- VII：5 个。

即 `1 + ceil(词条等级 / 2)`。维持原套装剪下逻辑及单次交互边界，不额外生成第二批掉落物。

## 验证与待实机检查

- `tests/CreativeSearchProbe.java` 使用已安装 JECharacters 及其内嵌 PinIn 复现重复（可选测试依赖，仅提取至被忽略的 tests/build，不打包进模组）。
- `tests/run-global-world-config.ps1` 继续执行原配置与过滤器回归，并运行 `CatInteractionFixesRegression`：多方向/高度飞行收敛、速度边界、剪毛等级产量；NeoForge 额外用真实 ItemStack 组件验证去重、顺序、颜色/主人/内容保留和不改变原堆叠。
- 检查 Forge 成品 jar 中导航字段已映射为 `f_21344_` 和 `f_21342_`，搜索注入点兼容 `m_98630_` / `f_98508_`。
- 双端构建与离线回归不代替游戏实测。请检查创造栏搜索“猫”、上天猫从高处前往地面/高台及攻击敌人、职业猫的远程攻击，以及机械手对 I / VII 长毛护体猫剪毛。
