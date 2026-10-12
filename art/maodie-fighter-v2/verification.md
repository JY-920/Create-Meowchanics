# 2026-10-07 预览阶段验证

- `node tests/maodie-fighter-art.mjs`：通过。生成前缺失模型时失败；旧 UV 的鼻后盖透明采样也实际触发失败。修正后 21 个封闭立方体共 1052 个 UV 采样点全部不透明。
- `node tools/preview-maodie-fighter-art.mjs import`：在独立 Blockbench 5.2.1 实例真实导入，21 个立方体、20 个骨骼、8 个动画，64×32 原版橘猫纹理加载无错误，ZYX 旋转顺序。
- `node tools/preview-maodie-fighter-art.mjs verify`：120 Hz 原生网格世界坐标采样，共 544 帧；所有动作的最低脚底高于地面。报告为 `animation-contact-validation.json`。
- 原生相机逐帧截图用于 HTML 和 GIF，不是另造的模拟渲染器。检查了站姿正面、侧面、背面及七种动作关键帧；调整了上勾拳后偏和下砸手臂反折。
- `node tests/maodie-fighter-preview.mjs`：通过。逐张 PNG 检查非空渲染和取景范围；七个 GIF 保留动画及全部时间；Edge 无头浏览器实测八个动作按钮、护脸多视图、播放、暂停、速度选择和时间轴；无页面异常。
- 其余 61 个 Node 测试（排除夹具生成脚本）通过；`git diff --check` 返回 0，仅提示已有文件的换行符规范化。
- 当前仅交付美术预览资产。没有修改 Java 运行源码、构建编号、运行 JAR、存档或配置；因此未声称游戏内召唤/震屏/战斗行为已经修复，也未替换游戏包。
