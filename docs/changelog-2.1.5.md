# 本地测试记录（原临时编号 2.1.5）：修复光影下表演猫描边不显示

历史归档：下文编号和路径仅保留作测试、备份记录，不作为当前版本号；当前更新以仓库 README 中的版本链接为准。

原因：2.1.4 将紫色轮廓合成放在 AFTER_PARTICLES，此处的输出会被 Iris 后续渲染覆盖。修正为与五条悟轮廓一致的 AFTER_LEVEL，且 NeoForge 使用单级纹理、保留原 framebuffer 绑定的 PerformanceTarget。

已在用户实际运行的 NeoForge 1.21.1 + Iris + ComplementaryReimagined r5.9 世界中复现并验证：相同猫咪、相同视角，修复前无描边，加载修复后的编译类后紫框出现。截图位于 validation/iris-before.png、validation/iris-after.png。未改动世界、猫咪状态或光影设置。

两个版本均构建成功。与 2.1.4 比对，仅 CatPerformanceOutline 类族及版本元数据改变。动画条件、颜色、材质、属性数值和其他资源保持不变。Forge 已编译与打包校验，本轮游戏内视觉验证在 NeoForge 进行。

用户退出游戏后，2.1.5 已永久安装至两个开发实例；2.1.4 保存在各实例 mod-backups/laowu-2.1.5-outline-shader-fix-20260911。现在可重新启动游戏。
