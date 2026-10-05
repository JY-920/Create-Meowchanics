# 骑乘动作与九猫饼展示验证

日期：2026-09-20。Forge 1.20.1 / NeoForge 1.21.1，公开版本保持 2.2.1。

## 本次功能

- 飞行员猫保留原版身体和四肢动作，仅尾巴末端绕连接点连续画圈；进入和退出骑乘沿用姿态混合。
- 潜水员前肢在原肩部高度向外伸展，肩部保持连接，减少与头盔重叠。
- 水下骑手位于猫后方，身体水平、双手前伸握持；八 tick 平滑切换水陆姿态。转向时头部、双手、身体与猫使用同一朝向。陆地不使用水下姿态。
- 后置骑手使用真实方块碰撞扫掠和终点检查；首次挂载与已经骑乘的状态分开，受阻后不能跨墙回退。检查区块已加载，超过八格的异常扫掠保留最后安全位置；不加载远处区块。水下速度、耐力和呼吸规则不变。
- 普通与心愿领养箱按输入槽 0–8 固定 3×3 展示最多九只完整猫饼。逐只保留外观，巨型猫饼按格子限制展示尺寸，移除一只不会移动其他猫。输出槽不展示；交易仍即时完成。
- 网络仅同步有上限的外观白名单，不携带完整猫实体、背包或六维属性；兼容旧单猫饼展示数据，明确空列表会清空旧预览。

## 验证证据

日志均在对应端的 `build/`，测试与预览不进入运行 JAR。

- Forge `rider-client-verified.log`、Neo `rider-client-verified.log`：真实模型和 Create 玩家姿态钩子通过；包括四肢保持、尾端连接/闭环、四方向及跨角度转向、头与双手世界坐标、400 帧前肢检查、水陆及下骑恢复。另有 14 项职业 GPU 预览及原有指示圈、饰品、GUI 检查。
- Forge `nine-client-verified.log`、Neo `nine-gpu-green-3.log`：两类箱体、四方向、普通与最大体型猫饼真实 GPU 渲染通过；逐格花色、替换、移除、清空，无格间重叠，不超出箱内范围。顶视与斜视预览已人工检查；低角度仍正常被箱壁遮挡。
- Neo `rider-nine-regression.log`：真实 KubeJS 36 饰品替换示例、SDK 示例和词条例程环境，128/128 必需测试通过。追加的最终扫掠上限由 `rider-safety-verified.log` 的真实水池、四方向、贴墙、跨墙与远距离场景通过。
- Forge `rider-nine-regression.log`：128 项中 127 项通过；本次新增/修改项均通过，旧 `logisticsPacing` 失败（casts=2，allies=2）。随后在同样的 KubeJS 示例配置下单独复测支持类，`rider-support-recheck.log` 中 5/5 通过，包括 `logisticsPacing`。整批失败未稳定复现，本轮未改物流玩法实现，不将初次全量结果记为全绿。
- 两端 `rider-nine-build.log`：无测试 init 脚本的正常 `build` 成功；各 23252 项饰品规则检查通过，饰品 API v1–v3/36 ID、词条 API v1/82 ID 和发布隔离检查通过。
- `git diff --check` 无空白错误；独立只读审查复核了转向补偿、落地位置和远距跨墙防护。

## 回归测试的失败证据

- `rider-poses-red.log`：旧飞行员伸肢姿态不符合正常四肢断言。
- `rider-poses-red4.log`：旧潜水骑手不在猫后方。
- `rider-shoulders-red.log`：过度横移肩部会脱离躯干。
- Neo `rider-heading-red.log`：旧玩家与载体转向不一致。
- `rider-wall-red3.log`：后置骑手进入墙体；`rider-distance-red.log`：已骑乘且距离超过四格时发生跨墙回退。
- Neo `nine-red-1.log`、`nine-gpu-red-1.log`、`nine-gpu-large-red-1.log`：旧单只同步、格间覆盖及巨型猫饼重叠分别被测试捕获。

渲染测试中旧烟雾断言仍期待 0.32 透明度，与此前已经实现的不透明烟雾不一致。本轮仅更新测试为检查接近不透明，未改生产烟雾效果。纯空气渲染 fixture 显式提供无碰撞和已加载范围；墙体安全由真实服务器测试验证，不由该 fixture 替代。

## 复验入口

```powershell
./gradlew.bat runClient --init-script ../tests/pilot-client-probe.init.gradle --no-configuration-cache --offline --console=plain
./gradlew.bat runClient --init-script ../tests/cat-machines-client.init.gradle --no-configuration-cache --offline --console=plain
./gradlew.bat runGameTestServer --init-script ../tests/accessory-gametest.init.gradle --no-configuration-cache --offline --console=plain -PaccessoryExamples36 -PaccessorySdkDemo -PtraitExamples -PaccessoryRunId=rider-nine-final
./gradlew.bat runGameTestServer --init-script ../tests/accessory-gametest.init.gradle --no-configuration-cache --offline --console=plain -PriderPlacementProbeOnly -PaccessoryNoKubeJS -PaccessoryRunId=rider-nine-final
./gradlew.bat build --offline --console=plain
```

Forge 使用 Java 17，NeoForge 使用 Java 21。客户端退出码必须与实际 PASS/失败标记共同检查。部署记录见 `deployment-rider-nine.json`；不修改用户存档、配置、其他模组或 KubeJS 文件。

部署完成时间为 2026-09-20 18:28。部署前 Forge 实例中没有启用的 laowu 包，因此为直接安装（未删除或移动历史禁用包）；NeoForge 旧包已移动到 `mod-backups/create-meowchanics/20260920-182811-2.2.1-rider-nine/`。两实例均只有一个启用 laowu 版本，安装文件与正式构建 SHA-256 一致。
