# 巨猫骑乘画面与状态回归修复

> **For agentic workers:** Use superpowers:executing-plans to implement the approved repair inline, with test-driven-development and a fresh final code review.

**Goal:** 修复实际骑乘不起伏、扳手后客户端侧躺残留，以及骑乘玩家躯干偏向。

**Architecture:** 保留服务器固定物理座位和职业行为。客户端待命只读取同步姿态，不把原版未同步的本地命令当成姿态事实；玩家躯干朝向与实际巨猫渲染朝向保持一致。起伏先用实际渲染链定位，不能通过放宽测试或凭空增加正弦摇晃掩盖问题。

**Tech Stack:** Java 17 / 21, Forge 1.20.1 / NeoForge 1.21.1, Mixin, 实际客户端 GPU 探针和 GameTest/KubeJS。

**Spec:** 本会话用户“确认修复”；此前三个问题与确认范围，以及根目录 AGENTS.md。

## Global Constraints

- 两端一致；公开版本保持 2.2.1；不修改激光笔、Boss 战斗、用户材质或职业套装设计。
- 不修改用户存档、配置、KubeJS 或其他模组；运行 JAR 不包含测试、SDK 和诊断代码。
- 不在运行中的实例替换包；备份旧包，验证 modId 数量和 SHA-256。
- 保留用户脏工作树；不自动提交、推送、合并。

## Review Focus

- 飞行员/潜水员巨大化后扳手交互、空手上骑再下马：客户端本地 sitting 命令不能复活已清除的待命姿态。
- 玩家从斜向姿态上骑、静止转视角、转弯与跨越 ±180°：躯干贴合猫而头仍可正常观察。
- 最终 GPU 渲染与生产包选择器必须覆盖，不仅断言 getRenderOffset 返回值。
- 第一/第三人称、局部尺寸变化、跳跃/落地、下马：起伏连续，物理坐标与碰撞不被画面偏移改变。
- 躺下/起身过渡与职业恢复不因修正被取消。

## Task 1: 复现与修复客户端待命事实

**Files:** dual client/GiantCatAnimation.java, client/GiantCatRenderer.java; tests/machines/{forge,neo}/cn/laowu/mod/client/GiantCatRiderBobProbe.java.

**Interfaces:** 保持 sample(Cat,float) / restWeight(Cat) 不变；不新建持久状态。

- [x] 写测试：Neo原版扳手交互导致本地 ordered=true（Forge明确重建该本地字段），同步 sitting=false 后连续60 tick采样 restWeight 必须为0。
- [x] Neo真实旧实现红灯；双端最终真实客户端及双职业网络流程绿灯。
- [x] 最小修复客户端读取同步 isInSittingPose；服务器命令仍归服务器控制。
- [x] 回归实际侧躺/起身/头部观察、职业保留与真实网络按键。

## Task 2: 最终骑乘画面、相机和朝向

**Files:** dual client/GiantCatRiderMotion.java, mixin/GiantCatRiderOffsetMixin.java / GiantCatRiderCameraMixin.java; required rider-facing integration only; Forge refmap; dual rider probes.

**Interfaces:** offset(Entity,float) 沿实际 body 祖先变换；CameraSway.offset(Entity,float,boolean) 保持独立相机状态与弱引用。

- [x] 用真实 dispatcher / PlayerRenderer 最终顶点和相机采样诊断起伏；分别输出 walk/run/jump 的实际范围，原测试的“有数值变化”不能充当最终画面验收。
- [x] 写朝向测试：玩家初始 yBodyRot 与坐骑差 35°，最终渲染躯干与巨猫一致；静止观察不导致左右漂移或强制转视角。
- [x] 确定失效环节后一次只修一个环节，保留固定物理座位、下马清理及障碍裁切。
- [x] 实际 GPU 连续帧、真实输入/网络、生产映射与相机验证通过。

## Task 3: 验证、审查和部署

- [x] 双端正常 build，饰品/词条冻结 API、资源兼容、发布隔离及最终JAR内容检查通过。
- [x] 双端最终客户端通过；完整实际KubeJS已运行并逐项记录Forge失败，隔离16项复测通过不等同全套通过。
- [x] 一次独立代码审查及同席delta复查，修库存预览问题与测量缺口，校正探针裁切后有效红绿验证。
- [x] 检查客户端退出、备份、部署、核对每端仅一个 laowu 与源/安装 SHA-256。
- [x] 记录原失败、修复依据、测试和实际部署；不提交/推送。

## Progress / rulings

- 执行依据：用户已确认直接修复并验证后部署；使用现有 develop linked worktree，不另建工作树。
- 不提交脏工作树：根 AGENTS.md 的授权范围优先于技能示例中的 commit 步骤；保留本计划和测试日志作为记录。
- 任务 1、2 共用同步姿态与动画采样；任务 3 消费最终包和已验证的运行类，没有新公共 API。
