# 自定义猫咪动画与 GeckoLib 的资源目录隔离

2026-09-11 的反馈中，GeckoLib 4.4.9 加载 `laowu:animations/cat_thomas_flare.json` 时抛出 `Missing animations, expected to find a JsonObject`，中断资源加载；随后崩溃报告最外层显示 Sodium Dynamic Lights 空实例异常。不能仅据最外层异常认定所有优化模组均有独立冲突。

## 原因与约束

GeckoLib 会扫描各资源命名空间的 `animations/` 下的 JSON，要求根节点包含自己的 `animations` 对象。我们的帧采样数据使用 `frames`、`bone_order` 等字段，并不是该格式。

街舞与琵琶的资源、加载器和导出脚本统一改用：

- `assets/laowu/cat_animation_clips/cat_thomas_flare.json`
- `assets/laowu/cat_animation_clips/cat_pipa_performance.json`

不要在旧目录保留兼容副本，否则自动扫描仍会失败。只改文件名而仍留在 `animations/` 也无法解决问题。资源路径变化不涉及猫咪 NBT、词条编号或存档转换。

参考：[GeckoLib 1.20.1 资源扫描实现](https://github.com/bernie-g/geckolib/blob/1.20.1/Forge/src/main/java/software/bernie/geckolib/cache/GeckoLibCache.java)、[动画文件加载器](https://github.com/bernie-g/geckolib/blob/1.20.1/Forge/src/main/java/software/bernie/geckolib/loading/FileLoader.java)。

## 防回归

- 两端 `jar` / `check` 自动运行 `verifyCatAnimationResources`：检查打包输入中的自动扫描目录与私有动画资源。
- `tests/run-animation-resource-compat.ps1` 检查最终双端 JAR，防止增量输出残留旧路径，同时检查资源内容、加载器常量和版本一致性。
- 该脚本的 `-JarPath <旧包路径> -ExpectConflict` 模式用于反例测试。
- 两个 Blockbench 导出校验器，以及 `run-thomas-flare.ps1`、`run-pipa-performance.ps1`、`run-cat-collar.ps1` 继续验证动画数据和项圈。
- 资源扫描测试复现 GeckoLib 的目录发现与必需根对象规则，不是完整客户端联合启动测试；后续仍应使用反馈者的实际模组组合复测。
