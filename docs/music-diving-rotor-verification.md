# 网络音乐排查、潜水骑乘与飞行尾旋翼

日期：2026-09-20。Forge 1.20.1 / NeoForge 1.21.1；公开版本仍为 2.2.1。

## 实现与边界

- 飞行骑乘时保留正常四肢，尾巴根段向上，末段在水平面每 4 tick 旋转一圈。姿态混合采用四元数渐开锥面，避免半圈/整圈相位边界的突跳；保留零权重时原来的连接点微小偏差并平滑收拢，下骑恢复普通尾巴。
- 潜水耗尽后上浮，按实际流体高度限制上浮终点，保持水中状态；不再因每 tick 跨越水面检测边界而上下弹跳、重复显示陆地提示或错误返还耐力。真正上岸后仍沿用原陆地恢复规则，速度、耐力与呼吸公式未变。
- 原版披风额外跟随潜水骑手躯干的新旧姿态差值，保留原版摆动、皮肤、隐身和鞘翅规则，不重复叠加蹲伏偏移。未宣称兼容所有第三方披风替换渲染器。
- 音乐部分只新增隐私安全的失败日志，没有改变播放器语义。日志只含播放序号与异常类名链，不含异常消息、歌曲地址或凭据。

## 网络音乐调查结论

**原实例的无声故障未复现，根因未确认，不能记作已修复。**

- 检查了两个目标实例的现有日志；其中 NeoForge 的网络超时/域名解析错误来自版本检查器，不是已证实的音乐下载错误。没有找到足以归因到老吴学播放代码的 NetMusic 解码异常。
- 只读检查确认音乐猫的唱片被识别，保存数据不支持“未装唱片”或“猫在水下”这两种解释；原生唱片机也不是保存的暂停/红石停止状态。没有修改玩家存档、音量或音乐机配置。
- 使用用户指定的 `1439814454`，两个端均通过原生 NetMusic 解码器读取真实 PCM，并通过猫咪生产音源进入实际 SoundEngine；检查了静止表演和移动后退出表演。
- 这是测试时刻隔离环境的链路验证，并非对用户原实例故障的修复证明。若再次失败，客户端 `latest.log` 中新增的 `Music cat playback failed: stage=network-open` 可用于区分超时、连接和解码异常。

## 已完成验证

日志位于各子项目 `build/`，探针、音频夹具与测试存档均不进入正式 JAR。

| 范围 | 证据 |
| --- | --- |
| Forge 真实歌曲 | `music-external-forge.log`：外部 ID 的 PCM、SoundEngine、静止表演、移动退出 PASS；正常退出 |
| NeoForge 真实歌曲 | `music-external-neo.log`：同一外部 ID 全部 PASS；正常退出 |
| Forge 最终客户端 | `music-diving-client-final.log`：旋翼、潜水双手姿态、真实披风顶点、可见性/鞘翅、原职业与 GUI 检查通过 |
| NeoForge 最终客户端 | `diving-cape-green.log`：同上客户端检查通过 |
| 旋翼后视预览 | Forge `rotor-rear-preview.log`：检查通过；`pilot-client-probe/specialist-flight.png` 后视图已人工查看，根段直立、尾端水平，普通四肢保留 |
| 浮水专项 | NeoForge `diving-surface-final.log`：2/2，通过真实水池、四朝向后置骑手、墙体与耗尽上浮测试；连续 100 tick 保持水态、稳定高度，骑手眼睛露出水面；真正上岸后恢复耐力 |
| Forge 完整服务器回归 | `music-diving-regression.log`：129/129，通过真实 KubeJS 36 饰品替换、SDK 示例与词条例程环境 |
| NeoForge 完整服务器回归 | `music-diving-regression.log`：129/129，同一真实 KubeJS 示例环境全部通过 |
| Forge 正常发布构建 | `music-diving-build.log`：正常 `build` 成功；23252 项饰品规则、饰品 API v1–v3 / 36 ID、词条 API v1 / 82 ID、发布隔离均通过 |
| NeoForge 正常发布构建 | `music-diving-build.log`：正常 `build` 成功；同样的 23252 项规则、API 兼容与发布隔离通过 |
| 源码与诊断 | 41 项 NetMusic 兼容检查、81 项音乐职业检查、303 项美术/速度状态检查通过；实际 Java 诊断格式化器的双端异常链、敏感信息隔离、循环引用终止测试通过 |

失败先验：Forge `rotor-red.log` 捕获原尾根未朝上；`rotor-continuity-red.log` 捕获旧相位混合突跳；Neo `diving-cape-red.log` 捕获真实披风顶点仍留在旧躯干位置。上述断言在最终客户端中通过。只读代码复核未发现本轮新增 P0–P2；未扩大为对所有外部披风模组或全部网络歌曲的保证。

## 复验

详细 NetMusic 命令见 `netmusic-compatibility.md`；用户指定的真实歌使用可选 `-PnetMusicExternalId=1439814454`。

```powershell
./gradlew.bat runClient --init-script ../tests/pilot-client-probe.init.gradle --no-configuration-cache --offline --console=plain
./gradlew.bat runGameTestServer --init-script ../tests/accessory-gametest.init.gradle --no-configuration-cache --offline --console=plain -PaccessoryExamples36 -PaccessorySdkDemo -PtraitExamples -PaccessoryRunId=rider-nine-final
./gradlew.bat build --offline --console=plain
```

Forge 使用 Java 17，NeoForge 使用 Java 21。必须同时核对进程退出状态和探针 PASS/失败标记，不能仅凭客户端退出码判断。

## 部署

2026-09-20 19:41 已部署到约定的 Forge / NeoForge 两个开发实例，仍使用正式文件名 `create-meowchanics-2.2.1-<平台>.jar`。部署前确认两个目标实例均未运行；未关闭正在运行的其他整合包。

旧包保留于各实例 `mod-backups/create-meowchanics/20260920-194144-2.2.1-music-diving-rotor/`，可恢复。两个实例分别仅启用一个 `modId=laowu` 包，安装包与正式构建 SHA-256 相同；没有改动存档、配置、其他模组或玩家 KubeJS。

完整文件路径与新旧哈希见 `deployment-music-diving-rotor.json`。
