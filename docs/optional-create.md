# 可选机械动力：develop 测试版

内部版本：`2.2.2-dev.optional-create.7`。这是 develop 测试阶段，不是新的公开发布版本。

同一个 JAR 支持两种运行环境：

- 安装机械动力：保留现有机器、配方、过滤器、猫咪工作能力、渲染及注册 ID。
- 不安装机械动力：保留猫咪、属性与词条、饰品、套装、Boss、战斗、骑乘、扫描器、材质和调试编辑功能，以及三档繁育箱、领养箱、心愿领养箱、猫咪收纳纸箱；工业机器、自动激光标记器、生物转信器、过滤器、加工链及机械动力工作能力不注册或不执行。

Forge 1.20.1 使用 Java 17；NeoForge 1.21.1 使用 Java 21。JEI、Curios、KubeJS 仍是可选兼容，不是新的强前置。

## 首版调整

- 飞行员猫和潜水员猫在无机械动力环境使用空手右键骑乘，并有独立的玩家姿态；安装机械动力时仍使用原来的扳手交互。
- 巨猫空手骑乘逻辑不变。
- 运输猫无机械动力时，蹲下空手右键打开原有 27 格背包的原版界面，不执行包裹配送。
- 工程猫战斗仍可用；无机械动力时暂用原版方块／物品显示炮架和弹药，不复制机械动力模型，也不改变命中、范围或伤害逻辑。
- 猫咪扫描器使用原版玩家物品栏背景；有机械动力时仍使用原背景。
- 旧配送猫加载到无机械动力环境时停止不可用的配送阶段，恢复重力与普通姿态，不清理待装载包裹、地址和猫咪存储的数据。
- 工业配方按加载器条件启用；缺失工业物品的战利品／标签不再阻止无机械动力启动。
- 饰品 API v1–v3／schema 1、词条 API v1／schema 1 和已存储的自定义词条 ID、等级、脚本状态保持兼容。

## 第二版：恢复猫咪生活设施

- 初级、中级、高级繁育箱使用原来的繁育时间、遗传逻辑、库存、观察界面和方块模型；高级箱无 Create 时不执行原生过滤槽与自动替换父母，有 Create 时仍使用原生交互与自动替换。
- 领养箱保留九格领养、奖励和计时；没有 Create 坐垫时不叠加附近职业猫的工作奖励。
- 心愿领养箱保留心愿刷新、锁定、NOW/MAX 条件、奖励和原界面。
- 猫咪收纳纸箱保留九只猫的活体收纳和释放；正打开猫咪档案、原版运输背包或 Create 包裹界面的猫不会被收走。
- 上述物品 ID 与库存、进度、名字等存档字段不变。完整放置的高级箱在无 Create 时保存原生过滤配置原始数据，重新安装 Create 可恢复；无 Create 时拆除箱子不能退还未注册的过滤器物品，请先在有 Create 的环境取出过滤器。
- 生活设施方块可正常掉落自身；创造物品栏主图标改为猫罐头。旧 Create 配方仍是条件启用，没有新增无 Create 配方。

## 第三版：独立能力与物品渲染

- 主创造物品栏图标改为猫锭，在有／无 Create 环境一致。
- 修复无 Create 时三档繁育箱物品渲染加载 Create 类导致的崩溃；覆盖创造栏、手持、掉落物等全部显示状态。
- 无 Create 时套装工业工作能力显示“暂无”，普通说明与 Ctrl 详情同步；不再展示无法使用的特工坐垫警戒半径。
- 飞行／潜水骑乘、音乐猫唱片播放、蟑螂猫作物催熟以及运输猫 27 格背包保留并归入“专属能力”；战斗、支援和套装属性加成不变。有 Create 时仍使用原工作能力说明与逻辑。

## 第四版：领养界面与护目镜

- 修复无 Create 时领养箱、心愿领养箱（以及共用背景的猫咪扫描器）玩家物品栏顶部的平直灰条：补全原版顶边和圆角，不移动物品槽位。有 Create 时仍使用其原生玩家物品栏背景。
- 新增 `laowu:giant_cat_boss_spawn_egg`，在主创造栏提供“大猫猫刷怪蛋”，生成原有大猫猫 Boss，不是骑乘载体；有／无 Create 都可用。
- 工程猫护目镜不再要求 Create 才注册。保留头部佩戴模型、右键穿戴、发射器穿戴以及准星指向猫咪／猫饼时的属性显示；Curios 安装后仍支持饰品头部槽。
- Curios 的单个玩家头部槽由本模组独立声明，不依赖 Create 提供；声明采用 SET 而非 ADD，不叠加重复槽位，也不替换其它饰品槽配置。
- 有 Create 时继续接入其护目镜佩戴判定、HUD 偏移配置和原 Curios 模型层；无 Create 时采用相同默认面板偏移和原版头部模型层。
- 没有新增无 Create 获取配方，原有护目镜锻造配方仍只在安装 Create 时启用。

## 第五版：独立生存配方与猫饼交互

- 新增 58 条无 Create 配方，覆盖 57 种养成物品；全部使用加载器的 `not(mod_loaded:create)` 条件，有 Create 时不加载、不改变原加工路线。
- 猫锭用一个铁锭与一份猫毛合成，不需要蜂蜜瓶。猫毛仍由猫咪玩法获得；一个猫锭制成两个猫板，两个猫锭竖排制成四个猫条，猫锭与燧石制成两份猫粉。原有猫齿轮、猫弹簧、工具和装备配方保留。
- 补齐三档繁育箱、领养箱、心愿领养箱、猫收纳纸箱、猫收纳袋、激光笔、工程护目镜、材质编辑棒、猫球、普通／繁育／变异／信息素猫粮、普通／黄金罐头、鱼干、六系属性猫粮／罐头、13 套套装、蘑菇点心与神秘蘑菇的原版材料获取路线。
- 信息素猫粮用普通猫粮与写有玩家名的命名牌合成，绑定当前命名牌的主人名，并完整返还命名牌。JEI 提供对应合成示例，说明文字会按 Create 是否安装切换。
- 铲子左击可无伤害压成猫饼，铲子右击拾取仍保留主人、外观、套装与属性。铁砧砸落和猫收纳袋收纳继续可用；有 Create 时仍兼容纸板剑与碾压机。
- 两种领养箱在有／无 Create 时都归入“猫咪养成”创造栏，不再混在主物品栏。

## 第六版：独立配方规整

本次只调整无 Create 的 26 条配方，两端内容一致；原机械动力配方不变。其余简单混合猫粮、食物和单材料转换继续使用无序合成，烹饪与绑定命名牌机制不变。

- 猫锭：一个铁锭、一份猫毛、一份糖，无序合成一个猫锭；仍不需要蜂蜜瓶。
- 工程猫护目镜：上排中间为线，下排依次为玻璃板、猫板、玻璃板，合成一个。
- 心愿领养箱：中心领养箱，上下各一张纸，左右各一颗绿宝石。
- 中级繁育箱：中心初级繁育箱，四角铁锭，上下左右猫毛。高级繁育箱：中心中级繁育箱、上方猫组件、左右钻石、四角纸。
- 猫组件：上下为猫齿轮与猫弹簧，左右各一铁锭、中心红石；比上一版多用一个铁锭。
- 猫袋：上方两角皮革、左右线、中心猫锭、下方中间皮革。猫球：中心黏液球，四周猫毛。
- 激光笔：从上到下为红色染色玻璃、猫锭、红石，不再额外使用普通铁锭。材质编辑棒：从上到下为紫水晶碎片、猫毛、木棍。
- 黄金罐头：一行金锭、猫罐头、金锭。黄金鱼干：中心鱼干、四周金锭。变异猫粮：中心毒马铃薯、周围八份繁育猫粮，产量仍为八份。

13 种套装统一采用十字配方：上下各一份猫毛、中心一个猫组件、左右各一份同样的职业材料。不再额外使用皮革、线或第二种职业材料；每次合成一件套装。

双端正常构建通过；四组有／无 Create 的实际 GameTest 各 10 项通过，覆盖 55 条普通合成、明确的新配方格子、错位与缺料拒绝、容器余物及加载条件。52 项新布局检查、两端 58 条配方的覆盖／获取图、原有 56 组脚本回归通过。NeoForge 测试夹具曾因窄配方裁剪后的余物读取越界而失败，已按实际输入尺寸修正并重跑通过；没有修改游戏逻辑。

已部署 `2.2.2-dev.optional-create.6` 到两个开发实例，部署前无游戏客户端运行，每端仅一个启用版本，安装包与构建包 SHA-256 一致。旧 `.5` 包保留在各自 `mod-backups/create-meowchanics/20261005-214628-124-optional-create.6/`，可恢复；未修改存档、配置、其它模组或 NetEase 工程。详细记录见 `deployment-optional-create.6.json`。本次未进行人工 JEI 画面验收。

| 套装 | 左右两份职业材料 |
| --- | --- |
| 终结者 | 铁块 |
| 渔夫 | 钓鱼竿 |
| 飞行员 | 幻翼膜 |
| 消防员 | 烈焰棒 |
| 蜜蜂 | 蜂蜜块 |
| 运输 | 箱子 |
| 爆破 | TNT |
| 工程师 | 工程猫护目镜 |
| 医生 | 白色羊毛 |
| 音乐家 | 音符盒 |
| 特工 | 遮光玻璃 |
| 潜水员 | 玻璃 |
| 蟑螂 | 面包 |

## 第七版：隐藏无用工业条目

- 无 Create 时从创造物品栏及其搜索栏隐藏猫弹壳、猫榴弹、哈气桶和哈基蜜桶；JEI 同时隐藏这四种物品及两种流体（含流动状态）。
- 安装 Create 时仍按原样显示并使用，不新增无 Create 弹药配方或独立猫炮功能；猫弹药箱和其它猫咪物品不在本次隐藏范围。
- 只调整展示，保留原有物品、方块、流体注册 ID、资源和存储数据，不清理旧存档中的物品或液体。

双端正式构建、58 组脚本回归通过；四组有／无 Create 的实际服务器测试各 11 项通过，四组实际客户端／JEI 检查通过。无 Create 隐藏检查和有 Create 保留检查均直接读取游戏创造栏、搜索栏及真实 JEI 过滤列表，不使用模拟列表。独立只读审查无可操作问题。

验证中曾遇到测试夹具过早手动渲染造成的相机未初始化异常，已仅在测试夹具增加相机初始化等待；有 Create 的并行服务器验证还曾因 JVM 原生内存不足中断，改为游戏测试进程 1.5 GB、Gradle 1 GB、最多 4 个处理器／2 个任务且串行重跑后通过，未修改用户内存设置。完整物品渲染探针仍有原有空纹理路径警告，本次没有改动或修复材质。

已部署 `2.2.2-dev.optional-create.7` 到两个开发实例，文件 SHA-256 与构建产物一致，每端仅一个启用版本。Forge 的 1,000 个、NeoForge 的 1,002 个猫模组资源／数据文件以及每端全部 342 条配方，与原部署的 .6 逐项一致。旧 .6 包保留在各自 `mod-backups/create-meowchanics/20261005-224929-224-optional-create.7/`，详细记录见 `deployment-optional-create.7.json`；存档、配置、其它模组、其它客户端与 NetEase 工程未修改。

## 暂未包含

Boss 战利品、超级食物、刷怪蛋以及属性／词条／繁育调试工具不新增普通生存配方，避免替代奖励路线或开放调试能力。猫毛仍通过猫咪交互获取；猫饼除猫咪交互外，新增下文猫面团烧制路线，不制作缺少猫咪数据的空白猫饼。其他工业遗留原料和液体仍无独立加工路线。

## 2026-10-13：独立猫面团与猫饼还原

- 未安装 Create 时，小麦与猫毛无序合成 1 个猫面团；原版熔炉烧制 200 tick（10 秒）产出 1 个随机花色猫饼，并生成正常的外观、六维属性及词条数据。两张新增配方不在有 Create 时加载，原搅拌／注液路线不变。
- 猫饼保持不可叠加；在真实烧制成功后才随机，配方预览不随机，满输出槽不会重新生成结果或额外消耗面团。
- 未安装 Create 时，手持猫饼蹲下右键方块还原猫咪，成功后才消耗一份（创造模式不消耗），受阻、越界或无权限时保留。主人、姓名、花色、属性、词条、套装、物品栏等沿用完整快照恢复；不改变普通右键投掷、铲子拍击／拾取、铁砧和猫收纳袋交互。
- 猫饼说明按 Create 是否安装切换；独立版为“蹲下右键方块还原猫咪”，有 Create 时保留鼓风机说明。

请先用独立测试存档。这里支持的是“无机械动力也能运行猫咪玩法”，不承诺把已经包含机械动力机器、坐垫实体、包裹或其它移除模组物品的旧存档无损转换。备份存档后再移除机械动力；不要把缺失物品的背包保存行为视为完整机器／物品迁移。

## 可重复验证

在对应加载器目录中运行正常 `gradlew.bat build --offline`，保留 API、资源和发布隔离检查。

最小客户端测试：

```powershell
# 先生成独立测试世界
.\gradlew.bat runGameTestServer --offline --init-script ../tests/accessory-gametest.init.gradle -PwithoutCreate -PstandaloneCatProbeOnly -PaccessoryNoKubeJS -PaccessoryRunId=standalone-phase2 --no-configuration-cache
.\gradlew.bat runClient --offline --init-script ../tests/standalone-client.init.gradle -PwithoutCreate --no-configuration-cache
# 保留 JEI / Curios 的无机械动力组合
.\gradlew.bat runClient --offline --init-script ../tests/standalone-client.init.gradle -PwithoutCreate -PstandaloneWithOptionalMods --no-configuration-cache
# 安装 Create 的客户端回归（同一夹具，不加 withoutCreate）
.\gradlew.bat runClient --offline --init-script ../tests/standalone-client.init.gradle --no-configuration-cache
```

真实服务器玩法与 KubeJS 词条回归：

```powershell
.\gradlew.bat runGameTestServer --offline --init-script ../tests/accessory-gametest.init.gradle -PwithoutCreate -PstandaloneCatProbeOnly -PtraitKubeProbeOnly -PtraitExamples -PaccessoryRunId=optional-create-all-absent --no-configuration-cache
.\gradlew.bat runGameTestServer --offline --init-script ../tests/accessory-gametest.init.gradle -PwithoutCreate -PstandaloneCatProbeOnly -PcatCareProbeOnly -PtraitKubeProbeOnly -PtraitExamples -PaccessoryRunId=cat-care-absent-final --no-configuration-cache
.\gradlew.bat runGameTestServer --offline --init-script ../tests/accessory-gametest.init.gradle -PcatCareProbeOnly -PcreateCatCareProbeOnly -PaccessoryCompatibilityProbeOnly -PtraitKubeProbeOnly -PtraitExamples -PaccessoryRunId=cat-care-present-final --no-configuration-cache
```

客户端夹具从 `build/accessory-gametest/no-kubejs-standalone-phase2/world` 复制测试世界，不能操作用户存档。测试源码、截图和世界不进入运行 JAR。

客户端回归应同时出现物品渲染、13 套套装说明和生活设施界面的三条 `PASS:`，且没有 `FAIL:`；不能仅以 Gradle 的成功退出判断通过。隔离测试玩家使用创造模式，截图前检查当前界面，避免测试存档中的溺水遮挡界面。

第四版回归另外要求刷怪蛋／护目镜模型／实际属性面板绘图的 `PASS:`。安装 Curios 时，还必须通过真实头部饰品槽插入、仅饰品槽佩戴时的属性面板绘图和正常玩家渲染流程；默认测试夹具只允许一个头部槽。测试使用独立世界和客户端目录，不修改用户饰品槽配置。

```powershell
# 独立 GUI 绘图边界回归：完整原版顶边、槽位对齐和 Create 分支
node ../tests/inventory-panel-regression.mjs
# 真正使用刷怪蛋生成 Boss、右键佩戴以及 Create 佩戴判定
.\gradlew.bat runGameTestServer --offline --init-script ../tests/accessory-gametest.init.gradle -PwithoutCreate -PstandaloneExtrasProbeOnly -PaccessoryNoKubeJS -PaccessoryRunId=extras-absent --no-configuration-cache
.\gradlew.bat runGameTestServer --offline --init-script ../tests/accessory-gametest.init.gradle -PstandaloneExtrasProbeOnly -PaccessoryNoKubeJS -PaccessoryRunId=extras-present --no-configuration-cache
```

## 第四版验证与部署记录（2026-10-05）

- 双端正常 build 通过，包含饰品／词条 API、动画资源及发布隔离检查；两端各 23,270 项饰品定义／状态回归通过。包内检查确认新的测试类未进入运行 JAR。
- 双端有／无 Create 的新增两项真实 GameTest 均通过：刷怪蛋实际生成原有 Boss，以及右键佩戴／Create 佩戴识别。
- 双端纯无 Create、有 Create、无 Create 但有 JEI／Curios 的客户端回归通过；后两种组合进一步测试真实 Curios 插入、空普通 HEAD、属性面板绘图及正常玩家渲染流程。两个领养界面截图已检查。
- Forge 有 Create 首次并行启动在早期窗口初始化阶段退出，尚未进入模组测试；随后单独重跑及最终完整回归均通过，未为此更改用户配置或其它模组。
- 部署前没有运行中的游戏客户端。两个开发实例均部署 `2.2.2-dev.optional-create.4`，按 JAR 内 `[[mods]]` 的 `modId=laowu` 检查各仅一个启用版本，目标文件与构建包 SHA-256 一致。
- 旧 `optional-create.3` 包移至各实例的 `mod-backups/create-meowchanics/20261005-181610-288-optional-create.4/`，可恢复；未修改用户存档、配置、其它模组或 NetEase 工程。
- Forge SHA-256：`98719544A6579ED36B718E9FD807B7636DBC8D91662B69D020BCA4279E03709D`。
- NeoForge SHA-256：`019EB9DDAE04868896BD941D6EFB3803248B90B1153C685DD712F2DDFE56FC60`。

## 第五版验证与部署记录（2026-10-05）

- 双端 58 条条件配方的数据回归与获取依赖图通过；真实 RecipeManager 检查全部 55 种普通合成、两种篝火烹饪、信息素猫粮输入／主人名／原样命名牌返还，没有合成冲突。
- 双端有／无 Create 的四组最终 GameTest 各 16 项通过，包含所有原版铲子、猫铲与可选纸板剑、普通剑负向测试、远程伤害、已取消攻击、铁砧、拾取和猫袋快照，并运行真实 KubeJS 词条事件／恢复／持久化回归。
- 代码审查发现并通过先失败后通过的测试修复两项边界：切换到铲子不能将箭等间接伤害误判为拍击，NeoForge 铲子交互不能绕过其他模组已取消的攻击。纸板剑历史兼容入口保留。
- 双端无 Create 且有 JEI／Curios 的真实客户端回归通过：JEI 显示 55 条普通合成、一个绑定猫粮示例和两条鱼干烹饪；特殊配方成功同步到客户端，独立合成说明、全部养成物品渲染和护理界面通过。测试仅使用隔离世界。
- 双端正常 build 通过，保留饰品／词条 API、资源、23,270 项饰品状态和发布隔离检查。部署前再次检查包内配方数、特殊序列化器、版本及测试文件隔离。
- 部署前没有运行中的游戏客户端。两个开发实例均部署 `2.2.2-dev.optional-create.5`，包内 `[[mods]]` 主条目确认各仅一个启用版本，部署文件和构建包 SHA-256 一致。
- 旧 `optional-create.4` 包移至各实例的 `mod-backups/create-meowchanics/20261005-194214-460-optional-create.5/`，可恢复；未修改存档、配置、其它模组或 NetEase 工程，未提交／推送分支或发布公开版本。
- Forge SHA-256：`73F8D25A7195647D65BB103A7E8DCD7E8704D8E1E72117C4A9A2051066A4663C`。
- NeoForge SHA-256：`B3D463F1DC3B725D8DC5E804DC7F66E9058C4A058C770AC483968CABB2DAB2F3`。

```powershell
node ../tests/standalone-recipes.mjs
# 添加 -PwithoutCreate 测试独立版本；省略则测试原机械动力版本。
.\gradlew.bat runGameTestServer --offline --init-script ../tests/accessory-gametest.init.gradle -PwithoutCreate -PstandaloneRecipesProbeOnly -PtraitKubeProbeOnly -PtraitExamples -PaccessoryRunId=recipes-complete-absent --no-configuration-cache
.\gradlew.bat runGameTestServer --offline --init-script ../tests/accessory-gametest.init.gradle -PstandaloneRecipesProbeOnly -PtraitKubeProbeOnly -PtraitExamples -PaccessoryRunId=recipes-complete-present --no-configuration-cache
.\gradlew.bat runClient --offline --init-script ../tests/standalone-client.init.gradle -PwithoutCreate -PstandaloneWithOptionalMods --no-configuration-cache
```
