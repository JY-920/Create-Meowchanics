# 猫咪饰品重绘素材 v1

本套共 16 件：15 件现有饰品 + 黄油猫 Boss 掉落「黄油块」。使用内置 image_gen 分别生成，不是程序绘制的替代图。

- `16x16/`：准确的 16×16 RGBA PNG，仅保留作历史草稿；游戏资源现已使用[用户重绘素材](../accessories-redrawn-v1/README.md)。
- `previews/`：最近邻放大到 128×128 的单张预览。
- `originals/`：AI 生成原图，完整保留透明通道。
- `contact-sheet.png`：4×4 中文名称对照预览。
- `manifest.json`：每张图的生成提示词、原始来源路径及稳定文件名。

AI 原图不是原生 16×16 文件，16×16 版是最近邻缩小导出，未进行人工描点。它们是重绘草稿，不是最终美术定稿。请以 `16x16` 目录为基准逐像素重绘，保存为带透明背景的 PNG，保留文件名即可直接替换资源。

## 对照

| 名称 | PNG 文件名 |
| --- | --- |
| 铃铛 | cat_taunt_bell.png |
| 羊毛毡 | cat_silent_bell.png |
| 船锚 | cat_stability_anchor.png |
| 隔热垫 | cat_fire_charm.png |
| 弹簧 | cat_impact_core.png |
| 齿轮 | cat_followup_gear.png |
| 磁铁 | cat_loot_magnet.png |
| 羽毛 | cat_ace_feather.png |
| 引线 | cat_blast_fuse.png |
| 红绳 | cat_health_badge.png |
| 尖牙 | cat_attack_badge.png |
| 风车 | cat_speed_badge.png |
| 鳞片 | cat_stamina_badge.png |
| 眼镜 | cat_intelligence_badge.png |
| 四叶草 | cat_luck_badge.png |
| 黄油块 | cat_butter_cube.png |

游戏资源目标：两个加载器各自的 `src/main/resources/assets/laowu/textures/item/`。旧饰品的 ID 不变，即使文件名仍含 badge，也只是兼容旧存档与 KubeJS 的稳定标识；显示名称已不含「徽章」。

工程/医疗/音乐套装继续使用你提供的模型与贴图，不属于这套 AI 重绘素材。
