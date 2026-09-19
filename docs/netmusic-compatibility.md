# 音乐猫 × NetMusic 兼容

适配版本：老吴学 2.2.1；NetMusic 1.5.2；Forge 1.20.1 / NeoForge 1.21.1。

## 玩家用法

1. 使用 NetMusic 本身的功能录好一张网络唱片。
2. 将唱片放入音乐猫原有的 **9 格物品栏**。无需额外容器，也不需要坐垫。
3. 按物品栏槽位顺序循环播放；可以混放原版唱片和网络唱片。唱片不会被消耗。
4. 取出或替换当前唱片时切到下一首；清空、移除音乐套装或离开该世界时停止。
5. 音源跟随猫咪，使用“唱片 / 音符盒”音量；开始网络曲目时显示其曲名。
6. 实际播放时，猫咪头顶出现音符，每首选择琵琶行或街舞，仅在静止时表演；移动或上下位移时退出，停下后恢复。音乐和音符不中断，暂停游戏时随游戏暂停。不因此额外施加战斗增幅或禁止移动。
7. 不再显示头顶歌词框或曲名框，也不再请求歌词；开始网络歌曲时原有游戏 HUD 曲名提示保留。原版与网络唱片均有音符和静止表演。

NetMusic 是可选兼容，不安装时普通唱片功能不变。多人游戏的服务端和所有需要听网络歌曲的客户端均应安装对应版本 NetMusic，并使用本次同一版老吴学构建（播放包协议已经更新）。

## 限制与排错

- 空白唱片、无效地址、零或负数时长不进入播放列表。
- 顺序切歌使用唱片保存的时长，不修改 NetMusic 的歌曲数据。
- 使用 NetMusic 原有的解析器与解码器。实际外部歌曲仍受其地址有效期、服务商限制、授权和客户端网络影响；不绕过 VIP 或版权限制。
- 本地文件唱片的路径必须在**每个收听客户端**上存在；服务端不会分发文件。
- 晚加入收听范围从该曲开头播放，跟随服务器剩余时长切歌，不保证跨客户端采样级同步。这与原普通唱片的同步方式一致。
- 单个曲目失败后会显示 NetMusic 的播放错误提示，不会每秒重复下载；轮到下一曲或重新放入后可再试。
- 原版唱片、其他猫及普通唱片机的音源互不停止。
- URL 最大 4096 字符，曲名最大 256 UTF-16 单位（不会截断表情字符的代理对）。

## 实现边界（供维护者 / AI 阅读）

- 服务端：`compat/netmusic/NetMusicDiscCompat` 仅通过公开 `ItemMusicCD.getSongInfo` 读取元数据。Forge 的 NBT 与 NeoForge 的数据组件交由 NetMusic 自己处理，不做网络访问。
- `CatMusicSongs` 适配曲目；`CatMusicRecords` 继续维护原有 9 格循环；`MusicRecordPacket` 追加地址与标题。保留旧 Java 构造函数。
- 客户端：`CatMusicRecordClient.RecordSound` 是猫咪专属的位置音源；`NetMusicAudioBridge` 在两个后台线程中调用原生 `NetMusicAudioStream(URL)`。
- 队列有界，打开请求 30 秒超时。移除、切歌、换世界、失败和晚完成请求均有流释放路径；停止不在游戏主线程等待网络关闭。
- 不新增硬依赖、不打包第三方实现、不增加 Mixin；不修改职业战斗增幅、饰品或词条 API。
- 2.2.1 移除了歌词桥接器和头顶文本渲染器。`HissingCatModel` 在没有战斗表演且猫咪静止时使用唱片表演，运动检查同时覆盖服务端同步位移与本地水平速度。动作由曲目序号与猫咪 UUID 稳定选取，琵琶使用现有模型资源。
- 反射适配的是 NetMusic 1.5.2 的公开类名/字段；未来 NetMusic 若改 API，需重新验证，不能视作永久兼容承诺。

## 可复现验证

所有探针只在 `tests/netmusic/` 中；独立运行目录为各子项目 `build/netmusic-probe/`，不使用玩家存档或脚本。

```powershell
# 在对应 Forge / NeoForge 子项目执行，选择该端安装的 NetMusic JAR。
.\gradlew.bat runGameTestServer --init-script ../tests/netmusic-probe.init.gradle --no-configuration-cache "-PnetMusicJar=<NetMusic JAR 绝对路径>"
# 不传 netMusicJar，验证完全未安装 NetMusic 的原版唱片回退。
.\gradlew.bat runGameTestServer --init-script ../tests/netmusic-probe.init.gradle --no-configuration-cache
# 隐藏的隔离客户端：合成 MP3 测试音频，通过本机 HTTP 交给真实 NetMusic 解码为 PCM。
.\gradlew.bat runClient --init-script ../tests/netmusic-probe.init.gradle --no-configuration-cache -PnetMusicClient "-PnetMusicJar=<NetMusic JAR 绝对路径>"
```

首次运行客户端探针前，在仓库根目录生成音频夹具（依赖只装入 build，不进入模组）：

```powershell
npm install --prefix build/netmusic-test-tools --no-save --package-lock=false --ignore-scripts @breezystack/lamejs@1.2.7
node tests/generate-netmusic-fixture.mjs
```

客户端探针使用真实 SoundEngine、实际 NetMusic 解码器，验证 PCM、各音源独立、加载途中取消、晚完成流关闭、移动位置与停播；不访问外部歌曲平台，也不替代玩家对实际网络歌曲的试听。

源码检查：

```powershell
node tests/netmusic-compat-wiring.mjs
node tests/music-career-wiring.mjs
node tests/accessory-art-and-music-speed.mjs
```

正式构建使用正常 `build`，保留 API 兼容与发布隔离验证；探针、测试音频、测试世界及 NetMusic JAR 均不进入运行包。

参考：[NetMusic 官方源码](https://github.com/TartaricAcid/NetMusic)。实际接入接口同时按两个端已安装的 1.5.2 JAR 校验。
