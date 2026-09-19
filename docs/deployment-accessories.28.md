# 音乐/医疗/特工表现修正 — accessories.28

公开版本仍为 2.1.4；内部构建 2.1.4-dev.accessories.28。包括上一轮未部署的 accessories.27 通用饰品。

## 本轮变更

- 琵琶：移除外观渲染层“至少拥有一个词条”的错误前置条件。音乐职业直接触发琵琶演奏，不需要琵琶行词条。原词条、幼猫、隐身和街舞切换逻辑保留。
- 特工烟雾：普通烟雾和绿色治疗烟雾均使用原版营火的 12 张 big_smoke 贴图。独立粒子采用营火式缓慢上升/横向漂动，不透明度 32%，最后 20 tick 平滑消散。初始烟团从 80/60 减至 32/24，持续治疗烟雾每次 4 团。没有替换原版粒子，没有新增 Mixin 或访问变换器；1.20.1 的原版营火构造函数不开放，因此两端用相同的局部粒子实现。
- 医疗：战斗治疗使用站立躯干和张开的低位四肢，不再折叠后腿；只有坐垫工作采用原坐姿及其独立动作。治疗规则、静止施法、范围群疗、不叠加和受治疗者自主行动不变。
- 特工：出拳、连续拳和旋转踢均改为站立骨架，后肢不再锁到原版坐姿。保留出招/收招混合、整身旋转和服装随骨骼运动。参考本机 jujutsuvfx 的 CatCombatPose/SorcererCatModel，未新增对该模组的依赖。
- 音乐增幅半径：min(24, 5 + 0.03 × 智力)，0/100 智力对应 5/8 格，原值为 8/12 格。与客户端范围同步和中英 Ctrl 详情一致；攻速强度和最强来源不叠加规则不变。
- 新增普通烟雾粒子类型，网络版本 Forge 34 / NeoForge 27，要求客户端与服务端一起更新。

## 验证

双端 build 通过，每端 23,252 项饰品基础回归通过。每端 52/52 项实际 GameTest 在带 KubeJS 和无 KubeJS 两种环境均通过；双端实际客户端 GPU 回归及新增琵琶测试通过。全局/存档配置、基因/轮盘等原回归、成品 ZIP 贴图和测试代码排除检查通过，git diff --check 通过（只有原有换行格式提醒）。

- 各端服务端日志：build/accessories28-gametest-verified.log（带 KubeJS）、build/accessories28-gametest-clean.log（无 KubeJS）。
- 各端实际客户端日志：build/accessories28-client-verified.log。
- tests/build/accessories28-package.log、accessories28-global.log、accessories28-pipa.log。
- 测试环境隔离，不打开用户存档；尚不代表覆盖全部第三方光影/资源包组合。

新增 tests/support-presentation-wiring.mjs；20 组 Node 静态检查通过。原琵琶轨道回归每端 961 姿势 / 399,776 顶点通过；双端实际客户端已验证 6 个琵琶渲染用例（无词条职业、街舞、停演、隐身、幼猫、原词条）。

已人工查看实际 GPU 输出的音乐琵琶、医疗站立/坐垫和特工攻击截图。截图和客户端日志位于各端 build/pilot-client-probe 与 build/accessories28-client-verified.log。

## 部署

已于 **2026-09-16 20:47:58（本机时间）** 部署。两个开发客户端和对应的两个 local-multiplayer-server 均由 .26 直接更新至 .28；上一轮 .27 的 10 件通用饰品包含在内。每处均按实际 [[mods]] modId=laowu 核对只有一个启用版本，新包 SHA-256 与构建一致。

- Forge：9C8C366AE26E196BCF008126D9103CC9589BCD127533A0DD8E84E2895AC8AF79
- NeoForge：30DAAD307D8C9EA22522961F44FBE689DAA97DF3F756B4273F23EF1B3419B1BE

四处旧 .26 包分别可从各实例下 `mod-backups/create-meowchanics/20260916-204753-accessories.28/` 恢复。旧包 SHA：Forge 0D610133C1F97D2885B4BF6ACED80AC76217AE75B8CAB2A2CC86389D9995246E；NeoForge 391EC85AC5F47C88A0E058A960D61672C284C792C52A9311DBB4E2AA2F876CF1。

部署前后无 Java 游戏进程、25565/25566/25575/25576 无监听。两台常驻测试服继续关闭；没有强制关游戏、热替换、修改存档/配置/其他模组或提交/推送/公开发布。

部署脚本：tests/build/deploy-support-accessories28.ps1。逐目标结果：deployment-accessories.28.json。
