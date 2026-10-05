# 巨猫项圈与弹性坐骑动画

## 使用

- 物品 ID：`laowu:cat_giant_collar`，名称“巨猫项圈”，史诗饰品，暂用原版马鞍图标。
- 给已驯服的成年猫装备到猫咪饰品栏即可变为巨猫；主人空手右键骑乘。
- WASD 移动，疾跑键加速，空格跳跃，Shift 下坐骑。上方或周围空间不足时不会强行骑乘。
- 卸下项圈恢复普通体型；若正在骑乘，会先安全解除骑乘。
- 沿用当前普通饰品的心愿领养箱奖励池规则，不新增制作配方。
- 不替换猫实体：原 UUID、主人、名字、基因、词条、背包和职业数据继续保留。巨猫形态暂不绘制普通猫的职业服装与背包外观；卸下项圈恢复。其他职业载具中的猫不强制变大。

## 模型、尺寸与花色

源文件归档在 `art/giant-cat-mount/source/`，原始文件不改写。可编辑工程为 `art/giant-cat-mount/giant_cat_mount.bbmodel`。

保留用户模型的 11 个方块、比例、UV 和骨骼层级，使用现有巨猫的 4 倍渲染尺寸；不是缩到背高 2 格。普通巨猫碰撞箱宽 2 格、高 3 格，骑乘根实体高 4.05 格覆盖玩家头部。外观弹性不改变碰撞箱，避免伸缩导致卡墙。

模型自动使用当前猫的原版花色或基因生成贴图；不统一替换成黄色贴图，不添加毛发材质。

## 动画

- idle：轻微呼吸及尾巴缓动。
- walk：四肢交替步态，身体轻微起伏。
- run：前后腿成组的弹跳奔跑，身体压缩/伸展。
- jump：服务端同步 3 tick 蓄势、腾空姿态、落地回弹。
- 转向与横移：身体轻微侧倾，头部反向补偿，尾巴延迟跟随。

站立、行走、疾跑按实际服务端移动速度平滑混合；空中不叠加地面步态。动画与物理位移分离，防止跳跃高度重复叠加。

## 维护

源曲线：`tools/export-giant-cat-mount.mjs`。执行 `node tools/export-giant-cat-mount.mjs` 会同时生成双端运行资源及 Blockbench 可编辑工程。BB 使用 60 Hz 采样关键帧，运行时使用同源平滑曲线；不要仅修改导出结果。

追加机制 `giant_mount: 1`；保留饰品 API v1–v3、schema 1 和旧 36 件饰品 ID。总原生饰品数变为 37，但冻结兼容清单与旧 36 件替换脚本不重写。

## 验证记录

- 四项 Node 资源/导出器/采样器检查通过。
- 双端真实客户端：实际主人右键、真实按键输入及网络包、行走/疾跑、跳跃蓄势、落地同步、Shift 下坐骑通过。
- 双端实际 GPU 渲染：原版橘猫与黑猫花色、放大轮廓、20 帧 idle/walk/run/jump/落地姿态、猫之间动画状态隔离通过。截图在各端 `build/cat-sixway-client/giant-cat-mount-preview.png`。
- 巨猫 GameTest 覆盖身份/数据保留、卸下恢复、非法输入、移动碰撞、起跳、存档恢复以及其他职业载具形态隔离。
- NeoForge 广泛 KubeJS 回归 148/148 通过（`build/giant-kube-neo-2.log`）。
- Forge 广泛回归存在旧 AI/飞行测试不稳定：一次 smartArtillery 失败，下一次该项通过但 passengerFlight 失败；单独组合复测 passengerFlight 通过，而 autoArtillery32 失败。没有把这些运行报告为全绿，也未据此修改与巨猫无关的 AI。保留原始日志 `build/giant-kube-forge*.log`、`build/giant-pilot-forge.log`。
- 最终聚焦饰品兼容与巨猫的真实 KubeJS 回归双端均 42/42 通过：`build/giant-compat-forge.log`、`build/giant-compat-neo.log`。
- 双端正常 build 通过，各 23,270 项纯逻辑检查；饰品 API v3、词条 API v1、发布隔离和运行 JAR 内容验证通过。日志 `build/giant-build-forge-2.log`、`build/giant-build-neo-2.log`。
- 自定义动画放在 `assets/laowu/cat_animation_clips/giant_cat_mount.json`，避免 GeckoLib 自动扫描 `animations/` 引发解析冲突；未放宽构建检查。

测试源码、测试世界、预览图、可编辑工程和 SDK 不进入运行 JAR；部署不改玩家存档、配置或 KubeJS 脚本。

## 最终部署

2026-09-27 02:07 双端已部署，公开版本保持 2.2.1，内部记录 `20260927-020725-2.2.1-giant-cat-mount`。旧包已移入各实例的 `mod-backups/create-meowchanics/`，可恢复。各实例仅一个启用的 laowu 包，安装包和构建包 SHA-256 一致；详见同目录 `deploy-giant-cat-mount-20260927.json`。

调整动画资源路径后的最终客户端日志为 `build/giant-client-forge-verified.log`、`build/giant-client-neo-verified.log`，骑乘、巨猫 GPU 渲染及原有机器/JEI 探针均通过。重用旧探针存档的中间运行曾超时，后续运行还遇到上次失败保存的已骑乘玩家；测试现增加旧载具解除逻辑，并以干净隔离世界完成最终验证。没有将中间失败运行当作成功。
