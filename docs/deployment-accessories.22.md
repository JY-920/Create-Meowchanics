# accessories.22：职业能力与反馈修复

状态：2026-09-15 20:38:44 已部署到 Forge / NeoForge 两个客户端及两个关闭的测试服。四处各只有一个启用的 laowu 包，均与已校验构建产物哈希一致。

公开版本 2.1.4；内部版本 2.1.4-dev.accessories.22。具体规则见 [能力说明](career-abilities-accessories.22.md)。

## 本轮实现

1. 修复医疗加号、音乐攻速符号的世界相机矩阵来源：Forge 与 NeoForge 分别使用正确的渲染阶段参数，不复用错误的 AFTER_LEVEL 姿态栈。
2. 医疗坐垫采用独立的、固定躯干支点的坐姿施法；地面动作修正躯干前后偏移。
3. 蟑螂猫临时群体三维属性加成，按用户最终要求改为每只友方的生命、耐力、战斗力属性各 +6；原始动作拆分为追击展翅冲刺、受伤只展翅。
4. 特工猫低生命低防御、高伤、背刺必暴击、智能侧绕与有队友时烟雾撤退。
5. 音乐猫自身 9 格唱片无需坐垫即可循环；修复职业同步顺序、客户端实体尚未到达时的丢包处理，以及唱片通道静音启动。
6. 潜水猫扳手载人、水下三维移动、耐力耗尽上浮、水陆速度差；飞行／水下骑乘采用前肢前伸后肢后伸动作，陆地骑乘不用此动作。

三个新职业全部进入正式配置与属性计算，旧职业配置、钓鱼平衡、原美术资源、9 格库存和未完成构件隐藏规则保留。新协议 Forge 29 / NeoForge 22，需要客户端与服务器同步更新。

## 验证记录

- 双端完整 build 通过。
- 最终每只 +6 版本重新完成双端 build、包验证和带 KubeJS 的各 27 项 GameTest。NeoForge 日志：abilities22-swarm6-final.log；Forge 最终日志：abilities22-swarm6-recheck.log。群体测试断言基础 50 时两只友方为 62、移走一只后为 56，离开／脱装仍正确恢复。
- Forge 首次整组复测出现词条编辑测试单次“Add THORNS”失败；增加测试初始实体索引、存活、可达和非旁观者状态断言后，完整重测 27 项通过，词条编辑覆盖 82 种词条。未修改词条编辑生产代码；首次失败的具体原因未确认，未将其隐瞒为从未失败或宣称修复了新的生产缺陷。
- 两端无 KubeJS 的最终 GameTest 各 27 项通过（abilities22-gametest4.log）；带 KubeJS 各 27 项通过（abilities22-kubejs1.log）。
- 实际服务端测试涵盖：12 次必定背刺、全部身体朝向、真实绕侧到背后、独自作战不撤退、有队友时实际投弹及仇恨清除；群体属性动态撤销与遗传不变；水下实际载人、输入身份校验、供气、Ctrl 下潜、耗尽上浮、读档孤立载体释放；音乐实时库存／顺序循环与新同步包序列化。
- 双端 GPU 测试分别通过 10 次职业姿态绘制、六种带相机偏移／旋转的图标投影与地形深度遮挡测试；探针实际调用渲染事件，覆盖 Forge 的错误终局矩阵及 NeoForge 的空 PoseStack。潜水猫与成年玩家的组合模型也加入绘制检查。输出位于两端 build/pilot-client-probe/。
- 双端配置回归通过，包括各 155,666 项职业重置／击退、145,320 项套装配置／实际属性转换等检查；本轮发现并修正 TraitContext 没有覆盖新职业的问题，现直接传入职业 ID，避免以后追加职业再次漏算。
- 全部 14 个源接线脚本通过，新增能力接线 43 项（含每只 +6 的双语说明一致性）；git diff --check 通过。
- 包验证通过，原三套模型／PNG 哈希未变，新增两个动画片段与关键类齐全，无测试代码混入生产包，无新增必需模组依赖。
- 没有在用户的正在运行的整合包内进行人工游玩验收，也未声称实际听音或所有第三方 Boss 的自定义仇恨逻辑均验证通过。首轮 GPU 探针曾漏测渲染事件矩阵，本轮已补齐；这次不再仅以纯 shader 单测证明整合包显示正常。

## 构建产物（已校验）

- Forge：forge-1.20.1/build/libs/create-meowchanics-2.1.4-dev.accessories.22-forge-1.20.1.jar
  - SHA-256：0135BFC06E7D17188F4AA42BE8F1FECF2B5E8456153C4EC7CB81C7174239F674
- NeoForge：neoforge-1.21.1/build/libs/create-meowchanics-2.1.4-dev.accessories.22-neoforge-1.21.1.jar
  - SHA-256：968891FE7DA6B41D637470AF48E0292EFD2B86714ECC5276A8697817EBE91E46

## 部署结果

此前因 NeoForge 开发客户端运行而暂停。本轮用户要求每只 +6 并部署，重新检查确认没有 Java 游戏进程，25565 / 25566 / 25575 / 25576 均未监听后执行。部署前完整预检四处目标身份、原包哈希及新包哈希，先暂存校验，再将旧包移入可恢复备份并安装新包。

安装位置：

- D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.22-forge-1.20.1.jar

- D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/local-multiplayer-server/mods/create-meowchanics-2.1.4-dev.accessories.22-forge-1.20.1.jar

- D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/mods/create-meowchanics-2.1.4-dev.accessories.22-neoforge-1.21.1.jar

- D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/local-multiplayer-server/mods/create-meowchanics-2.1.4-dev.accessories.22-neoforge-1.21.1.jar

四处原 accessories.21 统一移入各自根目录下的 mod-backups/create-meowchanics/20260915-203806-accessories.22/，原包 SHA-256 均再次核对通过，可恢复。没有删除文件、修改存档或配置，也没有修改其他模组。

- Forge 备份原包：3551ACB0748A377EEBEB87A6A952BC5FE1AFDF926D4E363C54EC7F8DA70998B7
- NeoForge 备份原包：A6510A2FA8CB2D307A558802384D0335B98D11182090FA8A68AA1E614D1A8CD5

最终按包内 [[mods]] 的 modId=laowu 检查唯一性，四处计数均为 1。两个本地多人测试服保持关闭，没有启动服务器、强制关闭游戏、提交、推送或公开发布。
