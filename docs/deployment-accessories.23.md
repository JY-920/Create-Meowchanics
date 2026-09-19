# accessories.23 部署记录

2026-09-16 13:22:21 已部署完成。公开版本仍为 2.1.4，内部版本 2.1.4-dev.accessories.23。
规则与未实装的特工工作建议见 [职业说明](career-abilities-accessories.23.md)。

## 变更

- 蟑螂猫每只友方仅提供生命、战斗力各 +6，移除耐力加成。
- 9×9×9 耕地催熟取代尚未实装的掉落物销毁方案；每 5 秒判定，生命 100 时 50%，成功只施加一次骨粉效果。
- 潜水员近程范围喷水，成功命中洗去正面药水效果。
- 优化蟑螂冲刺、飞行员飞行与潜水员水下骑乘前肢；特工采用三种同步近战动作，不增加伤害次数。
- 13 套装的中英文默认介绍统一为战斗、工作、Ctrl、Shift；条件说明保留，公式移入详情。

## 验证

- 两端最终完整 build 与不带 KubeJS 的各 30 项真实 GameTest 通过：`<port>/build/abilities23-final-no-kubejs.log`。
- 带 KubeJS 的各 30 项 GameTest 通过：Forge `build/abilities23-gametest-final.log`；NeoForge `build/abilities23-gametest-recheck.log`。此后仅改进了客户端动作旋转、提示构建测试和按世界/UUID 隔离的动作缓存，最后的无 KubeJS 完整构建再次验证。
- 全部 15 组 Node 源码/资源检查通过，新增 362 项催熟/驱散/简化说明检查。
- 全量双端配置回归通过：`tests/build/abilities23-config.log`，既有钓鱼平衡不变。
- 双端各 14 张/姿态 GPU 绘制、400 帧伸肢间距、303 帧攻击姿态与结束回归，以及 721 个旋转四元数等价检查通过。三个特工动作、骑乘与蟑螂动作的实际 PNG 已人工检查。
- 双端各 39 个默认/Ctrl/Shift 实际本地化提示构建通过。日志 Forge `build/abilities23-client-final.log`；NeoForge `build/abilities23-client-tooltips.log`。
- 飞行员 4 组真实扳手接触、450 组身体挂环锚点，以及原有医疗/音乐图标透视、地形遮挡检查通过。
- 包检查通过：新增类、原美术哈希、资源路径、协议版本、无测试类泄漏；`git diff --check` 通过（只出现原有 CRLF 提示）。

测试过程修正：新增 GameTest 最初误把不合法作物可能被原版移除视为固定小麦；Forge 隔离地下测试场景还未完成光照传播，正常耕地上的作物会被原版移除。已加入真实光源、等待传播、场景前置断言，再跑完整测试通过。旋转踢的实际截图暴露了打包 JOML 1.10.5 的 ZYX 提取 X 分母符号问题，改用显式公式，并用旋转等价测试与重新截图核验。没有削弱生产机制来绕过失败。

## 部署与回滚

四个目标各仅启用一个实际 `modId=laowu` 包，安装包哈希与验证产物相同：

- Forge SHA-256：`36A06BB0EE530C2797638C2BDF4A99DC35591BAAD8792B01B40453D3F83C5327`
- NeoForge SHA-256：`AC859BB2624D891CC291CB332455E8CF05B171D56D4F21E7DD20768F16E4DBA3`
- 旧 .22 包已可恢复地移动到各目标的 `mod-backups/create-meowchanics/20260916-132216-accessories.23/`。
- 旧 Forge SHA-256：`0135BFC06E7D17188F4AA42BE8F1FECF2B5E8456153C4EC7CB81C7174239F674`
- 旧 NeoForge SHA-256：`968891FE7DA6B41D637470AF48E0292EFD2B86714ECC5276A8697817EBE91E46`

目标：

- `D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发`
- `D:/PCL2/PCL/.minecraft/versions/1.20.1-Forge模组开发/local-multiplayer-server`
- `D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发`
- `D:/PCL2/PCL/.minecraft/versions/1.21.1-NeoForge模组开发/local-multiplayer-server`

部署前后均确认无 Java 游戏/服务进程、无 25565/25566/25575/25576 监听；两台多人测试服保持关闭。未修改存档、实例配置或其他模组。未提交、推送或公开发布。

如需回滚，先退出客户端/服务器，再将此目录的新 .23 包移出 mods，并将相应备份 .22 包恢复。客户端与服务端需要同步回滚，因为网络协议为 Forge 30 / NeoForge 23。
