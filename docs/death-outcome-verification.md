# 死亡处理三选一：验证记录

本次构建保持正式版本 2.2.1；配置说明见 cat-death-outcome.md。只更新两个本地开发实例，旧公开压缩包不覆盖。

## 通过的检查

- 真实配置规格与界面数据：每端 90 项死亡配置检查，包含默认猫饼物品、三种取值、非法/缺失字段拒绝、全局锁定、伪造锁拒绝、服务端镜像及其他原有死亡扣属性用例。
- 两端独立客户端：三种选择、条件显示/隐藏、隐藏无效输入不阻止保存、切回恢复校验、保留草稿、全局锁定。未连接服务器时不发送初始化请求。
- NeoForge 无脚本原生死亡测试 18/18。
- 两端真实 KubeJS + 36 饰品替换示例 + 新饰品 SDK + 词条示例最终均报告 111/111，脚本错误 0。包含新增 6 个死亡结果场景及原有 12 个死亡/还原场景。
- 上述 111 总数含原有主动跳过的 ProtoChunk promotion 用例；本轮未重复验证该特定区块晋升场景。
- 两端正常 build：饰品 API v3、词条 API v1、每端 23252 项饰品定义检查与发布隔离通过。测试和文档不进入运行 JAR。

## 排错记录与边界

- Forge 首轮原生测试的 disabledCareerKittenPenalty、tamedWithoutCareer、careerDeathUnchanged 失败。日志同时记录快速连续修改测试配置时，Forge 文件自动保存/监听产生重复 TOML 键及异步重载；新模式在独立用例之间受到干扰。
- Forge 的死亡测试夹具改为在专用 GameTestServer 中使用真实配置规格的内存配置，避免压力切换用例之间的磁盘监听干扰；生产配置保存机制未改动。完整脚本回归中上述所有场景及新增场景均通过。
- 首轮 NeoForge 标题界面探针发现 WorldSettingsScreen 构造时无连接仍尝试请求服务端。已增加连接检查，随后两个客户端 UI 回归通过。
- 不把测试夹具调整当作“修复了 Forge 通用配置文件自动保存机制”，也不把跳过的区块晋升场景算作实际执行。
- 不修改用户存档、配置、脚本或其他模组；本次版本的网络协议号为 Forge 43 / NeoForge 36，联机需同时更新客户端与服务端。

## 日志

- tests/build/death-outcome-red.log：新增默认模式断言的修改前失败。
- tests/build/death-outcome-config.log：双端配置回归。
- 各端 build/death-outcome-native.log：首轮原生死亡测试。
- 各端 build/death-outcome-client2.log：最终客户端 UI 及已有音频/动作回归。
- 各端 build/death-outcome-sdk1.log：最终完整脚本回归。
- 各端 build/death-outcome-build.log：正常构建。
- 各端 build/accessory-gametest/examples36/run-death-outcome-sdk1/logs/kubejs/server.log：实际脚本日志。
