# accessories.27 构建与部署记录

核验日期：2026-09-16。

## 历史状态：.27 当轮未部署，现已随 .28 部署

更新：2026-09-16 20:47:58，全部通用饰品随 accessories.28 一并部署至四个目标，见 [最新部署记录](deployment-accessories.28.md)。下文保留 .27 当轮验证及暂停记录。

最后进程检查仍发现用户的 Minecraft NeoForge 1.21.1 单人游戏运行（PID 30944）。已询问退出开发实例；未得到退出确认，且进程尚未结束。没有关闭用户游戏、热替换 JAR、启动服务器或修改玩家存档/配置。

Forge、NeoForge 两个开发客户端及对应的 local-multiplayer-server 均仍是 accessories.26；按包内 [[mods]] modId=laowu 检查，每处只有一个启用包，旧包 SHA-256 与上轮记录一致。25565、25566、25575、25576 无监听；两台常驻测试服保持关闭。

## 已验证产物

- Forge：forge-1.20.1/build/libs/create-meowchanics-2.1.4-dev.accessories.27-forge-1.20.1.jar
  - SHA-256：E3D5E4271512205D09989AB57E206FBD44E7C444DDFE5F9BD5E68623E31F1902
- NeoForge：neoforge-1.21.1/build/libs/create-meowchanics-2.1.4-dev.accessories.27-neoforge-1.21.1.jar
  - SHA-256：8D8D03F180B85DB928A053935570937822AB087E50CEE7A588F6F484BD39EA32

每端 36 件饰品定义/模型，16 张用户正式图标、20 张已核对实际存在的原版占位图，35 个合成配方及 1 件 Boss 奖励。中英名称/说明齐全，无测试类或新的必需运行依赖混入产物。

## 验证结果

- 两端 build 及每端 23,252 项饰品定义/载入/概率/状态回归检查通过。
- 两端各 52 项真实 GameTest，在有 KubeJS 和无 KubeJS 环境均通过。
  - Forge 带 KubeJS：build/accessories27-nested-verified.log
  - NeoForge 带 KubeJS：build/accessories27-release-gametest.log
  - 两端无 KubeJS：各自 build/accessories27-final-clean.log
- 两端实际客户端探针通过：36 件已烘焙物品均有名称及有效贴图；既有医疗/音乐圈、图标、人物动画、实体边框和蜂蜜渍渲染回归通过。探针仅使用隔离环境，结束时自动退出。
  - 日志：两端 build/accessories27-client-verified.log
- 19 组 Node 静态校验、现有全局配置/基因/职业公式回归、成品 ZIP/贴图哈希检查通过。
- 补充测试涵盖取消伤害、击杀触发、辅助猫被动反伤、额外攻击受护盾保护，以及嵌套脚本伤害不丢失外层触发、不多叠连击。

首轮失败的测试问题已修正并重跑：模拟猫护甲/随机暴击/落地物品运动的干扰；Forge 新生成实体未进入区块 UUID 查询表就发令；重定位断言应比较距离而不是限定 X 方向。治疗速率测试隔离了邻近战斗实体，嵌套伤害测试显式控制 NeoForge Incoming 阶段的无敌帧。这些测试维护未改动生产炮台 AI。

## 退出游戏后的部署

已准备 tests/build/deploy-general-accessories27.ps1。重新检查进程和上述成品哈希后，以两端哈希作为 ForgeSha / NeoSha 参数执行。脚本要求旧包仍为已核验 .26，先创建可恢复备份，再更新四个目标，逐一校验 modId 唯一性及 SHA-256；两台服务器继续保持关闭。当前未执行此脚本，没有生成本轮部署备份。

实施说明：general-accessories.27.md。美术交付：猫咪饰品-美术总表.md。
