# 巨型橘猫 Boss 模型 · 参考重建 v1

## 出处

截图出自 Alan Becker《火柴人 VS 我的世界》第 40 集《苦力怕部族 / Creeper Clan》，截图水印为 BV163426vE3s，时间约 10:25。

- [B 站原视频，定位 10:25](https://www.bilibili.com/video/BV163426vE3s/?t=625)
- [Alan Becker 的 YouTube 原视频](https://www.youtube.com/watch?v=FcQlQAs5EDM)

项圈上方的红色线条是骑在猫背上的火柴人 Red，不是猫的装饰。本模型没有把 Red、额外铃铛、铠甲或发光眼加到猫身上。

这是依据用户截图及原视频公开缩略图的侧面、背面、跑动视角重新制作的模型，不是提取的官方模型，也不代表官方授权。被遮挡的正面、底面和精确尺寸采用一致风格补全；不能保证与未公开原模型逐像素一致。这里的八个动作是为后续 Boss 使用新做的基础动作，不是原视频动画文件。

## 直接使用

1. 用 Blockbench 打开 `giant_tabby_boss.bbmodel`。模型内嵌贴图，不依赖本机绝对路径。
2. 切换到动画模式，选择 `animation.giant_tabby.*` 播放。
3. 修改外观时以 `.bbmodel` 为主文件；`giant_tabby_boss.png` 是单独的像素贴图备份。

| 文件 | 用途 |
| --- | --- |
| giant_tabby_boss.bbmodel | 可编辑的主模型、骨骼及八个动作 |
| giant_tabby_boss.png | 128 × 128 像素 UV 贴图，最近邻采样 |
| giant_tabby_boss.glb | Blockbench 原生导出的带骨骼、贴图与动画的通用 3D 文件 |
| giant_tabby_boss.geo.json | Blockbench 原生导出的 Bedrock 几何结构，供后续适配 |
| giant_tabby_boss.animation.json | 与上述几何配套的动画数据 |
| previews/overview.png | 实际模型渲染总览，不是 AI 概念图 |
| previews/*.png | 三视图、背面及几个动作的预览帧 |
| manifest.json | 来源、坐标、尺寸、动作名称及主文件校验值 |
| blockbench-validation.json | 实际 Blockbench 导入结果 |
| animation-contact-validation.json | 实际 Blockbench 逐帧地面接触检查 |

## 结构与尺寸

- 23 个方块，20 个骨骼；四肢和足部可分别旋转，下颌可开合，尾巴分三段。
- 朝向为 `-Z`，向上为 `+Y`，地面为 `Y=0`。
- 按 16 模型单位 = 1 格，静态耳顶约 **3.81 格**，躯干宽 **2 格**，鼻尖到完全伸直的尾尖约 **7.69 格**。
- 以上为这版重建的可调整尺寸，不是原作官方尺度。后续可统一缩放，避免单独拉伸头或腿破坏比例。
- 红项圈随头、颈运动；没有把平时隐藏在内部的支撑结构做成大面积额外装饰。

## 基础动作

| 动作后缀 | 时长 | 播放方式 |
| --- | --- | --- |
| idle | 4.0 s | 循环：呼吸、轻微摆头、耳朵和尾巴活动 |
| walk | 1.6 s | 循环：四足行走 |
| run | 0.8 s | 循环：加快步态 |
| bite | 1.2 s | 单次：蓄力、张口前咬、回位 |
| paw_swipe | 1.5 s | 单次：抬起前爪、横扫、回位 |
| pounce | 1.8 s | 单次：下蹲、伸肢扑出、落地、回位 |
| hurt | 0.6 s | 单次：受击回弹 |
| death | 2.4 s | 保持尾帧：侧倒 |

循环动作首尾闭合，普通单次动作回到中立姿态。移动和扑击为原地动作：游戏中的水平位移、追踪、碰撞和伤害判定应由实体逻辑驱动，不能把动画当作攻击功能。

## 后续接入老吴学

**本次没有注册新 Boss，没有改黄油猫 Boss，没有设定血量、掉落、自然生成或召唤方法，也没有部署新 JAR。**

接入时以 `giant_tabby_boss` 为候选新资源 ID；它不是现有实体 ID。应先确定 Boss 行为方案、缩放和碰撞箱，再分别适配 Forge / NeoForge 的模型渲染、动作状态同步以及服务端判定。

不要把本包全部拷进 `src/main/resources`。模型源文件、检查报告、预览和说明应继续留在 `art/`。只有被正式加载的运行资源才进入两端资源目录；若继续采用本模组的自定义动画帧加载器，帧文件应放 `cat_animation_clips/`，不能把私有格式 JSON 放进 GeckoLib 会自动扫描的 `animations/`。

## 验证与复现

实际使用本机 Blockbench 5.1.6 验证：主模型导入成功；23 个方块、20 个骨骼、8 个动作和 128 × 128 贴图一致，贴图错误数为 0。Bedrock 几何/动画和 GLB 由 Blockbench 原生导出，GLB 内含贴图与八个动画。

生成器包含 6,638 次结构、UV、数值和动作边界断言；另以 30 fps 在 Blockbench 中采样 425 个动作帧检查模型不穿过平地。预览已人工查看多个视角与挥爪、咬击、扑击帧。这是模型层验证，**没有进行 Minecraft 游戏内 Boss 战斗验证**。

仓库内复现命令：

```powershell
node tools/build-giant-tabby-boss.mjs --write
# 打开 Blockbench，并启用本机已有的 MCP 插件后：
node tools/preview-giant-tabby-boss.mjs all
node tools/build-giant-tabby-boss.mjs
node tools/package-giant-tabby-boss.mjs
```

生成器的依赖目录可通过 `MEOW_ART_NODE_MODULES` 指定；Blockbench MCP 地址可通过 `BLOCKBENCH_MCP_URL` 指定。没有这些工具也能直接编辑主 `.bbmodel`；后续美术手动修改后，不要再次运行 `--write` 覆盖手工修改，除非已将改动合并回生成器或另存了副本。
