# 职业专属饰品 / accessories.26

公开版本仍为 2.1.4。内部版本 2.1.4-dev.accessories.26。

## 本次实装

- [13 职业效果、数值与机制边界](career-accessories-v26.md)
- [10 张新增图标命名与绘制清单，及 3 张沿用图标](../art/career-accessories-v1/README.md)
- 警戒上限 128 格，智力系数保持每点 0.64 格；磁铁每 2 tick 吸取并提高牵引速度。
- 飞行员/潜水员恢复 accessories.22 前肢原始位置，蟑螂恢复原 Blockbench 轨道。6 tick 进入、8 tick 退出的平滑混合；同 UUID 的界面预览和世界实体不再互相覆盖动画缓存。
- 复用原三件饰品、注册十件新饰品并提供原版贴图占位。原 16 张用户绘制贴图保持字节一致。

## 验证结果

- 两端构建通过，每端 23,190 个饰品定义/加载/概率/状态基础检查。
- Forge 与 NeoForge 带 KubeJS 的隔离服务端各 **44/44** GameTest 通过；无 KubeJS 的隔离服务端各 **44/44** 通过。
- 新增游戏测试覆盖：磁铁在真实地面从 2.8 格吸取且不忽略拾取延迟；激光追加攻击不连锁；实际飞行员伤害闪避；鱼钩拉近；蜂蜜地面实体及新生物接触减速；蓝火实际 1.5 倍伤害；物流实际 II 级送达；真实炮台各弹种与伤害倍率；医疗实际伤害事件保护和音乐移速均不叠加且可撤销；固定速度的烟雾群疗；友军净化；真正死亡生成两只半属性幼猫且不复制物品。
- 新测试发现 NeoForge 分裂后仍额外掉猫饼，已修复并通过真实死亡路径验证。无脚本回归也暴露测试猫在入场时随机获得幼年词条造成的年龄污染；测试夹具清除词条后明确恢复成年，未改变游戏的年龄/词条规则。
- 两端隔离客户端通过：26 件实际烘焙物品模型和名称、原版烟雾粒子工厂的绿色颜色、三个旧版伸肢姿势连续进入/退出、同实体预览隔离、蜂蜜贴花实际注册渲染器绘制（各 44,724 个金色像素）。
- 原有治疗十字、音乐攻速图标、绿/紫范围圈 GPU 测试继续通过；范围圈跨四个视角，并检查 GUI 矩阵恢复。14 组职业模型姿势渲染、39 组本地化套装提示、真实扳手与挂环接触也通过。已查看旧版姿势与蜂蜜贴花截图。
- 18 组 Node 源码/资源检查通过；全局/存档配置、遗传/过滤/轮盘等回归通过；git diff --check 通过（仅既有换行格式提醒）。
- 包体检查：26 饰品定义/模型、16 原图 + 10 原版占位、25 常规配方 + 1 Boss 饰品；没有夹带测试代码或新增必装依赖。新粒子/蜂蜜实体独立注册，没有新增 Mixin。
- 以上是在隔离测试环境中的验证，不代表已经覆盖用户所有第三方光影组合；测试没有打开用户存档。

## 构建 SHA-256

- Forge：0D610133C1F97D2885B4BF6ACED80AC76217AE75B8CAB2A2CC86389D9995246E
- NeoForge：391EC85AC5F47C88A0E058A960D61672C284C792C52A9311DBB4E2AA2F876CF1

## 部署

完成时间：**2026-09-16 18:21:01（本机时间）**。

四处均安装 2.1.4-dev.accessories.26：Forge/NeoForge 开发客户端，以及两端各自的 local-multiplayer-server。每处按包内实际 [[mods]] / modId=laowu 确认仅一个启用包，新包 SHA-256 与上述构建一致。

旧 accessories.25 包均备份至各实例下：

`mod-backups/create-meowchanics/20260916-182057-accessories.26/`

备份 SHA：Forge 1128846AC9C2B6CC6E565F8CD75CCD33DFF5E19ABA8C16DB9624035482AC09C1；NeoForge 801FF90CA6CC3B003202AAA98C58E2C684B02ACF6466B260EBC88E0E7CCA72FC。

部署前后没有 Java 游戏进程或 25565 / 25566 / 25575 / 25576 监听。**两台测试服保持关闭**。未修改用户存档、配置、脚本或其他模组，未提交、推送或公开发布。

部署脚本：[deploy-career-accessories26.ps1](../tests/build/deploy-career-accessories26.ps1)。可从上述备份恢复旧包。

## 验证日志

每端 build 下的 accessories26-integration-final.log、accessories26-noscript-verified.log、accessories26-client6.log。Forge build 下另有 accessories26-config.log 与 accessories26-package.log。
