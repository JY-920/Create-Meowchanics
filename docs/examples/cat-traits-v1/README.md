# 词条脚本示例 v1

完整文档在仓库 `docs/cat-traits-kubejs.md`；独立压缩包中为 `README.md`。

只复制对应加载器下的 `kubejs/server_scripts/cat_traits.js`，不要同时安装 Forge 和 NeoForge 两份。
启动或 /reload 后，用词条调整棒添加：

- 轻盈步伐：速度属性 +5/+10/+15。
- 自愈节律：受伤时每 4 秒回复 1/2/3 点生命。
- 越战越勇：攻击事件额外 +1/+2/+3 点伤害。

三个 ID 分别是 examplepack:swift_paws、examplepack:healing_rhythm、examplepack:relentless。
默认不开自然生成和突变，允许遗传；需要正式投放再打开 natural / mutation。
自定义词条与本体共用 4 个位置，不会自动把本体词条替换掉。

示例不包含测试观察脚本、游戏世界、模组 JAR 或旧饰品 SDK；不会自动写入玩家实例。
移除示例并 /reload 后，已有猫咪保留这些 ID/等级但停止效果；重新安装相同 ID 可恢复。
