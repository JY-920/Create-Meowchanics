# 饰品正式图标与音乐速度属性 — accessories.29

公开版本仍为 2.1.4。内部构建 2.1.4-dev.accessories.29。

## 本轮交付

- 19 张用户 PNG 原样导入双端，保留 16×16、透明度和原始字节；物品模型改为对应 laowu:item 图标。
- 共 36 件饰品，35 张正式图标。唯一仍占位的是蟑螂猫的蜕生卵鞘，没有其他漏图。
- 按外观简化中英名称，稳定物品 ID、配方及库存不变。“玩偶”对应猫咪玩偶，“小眼睛金基米”对应金基米（原不倒翁玩具），效果/互斥关系不变。
- 扩展音响（原节拍铃鼓）由目标移动速度 +10% 改为目标速度属性 +20。接入 CatAccessories.statBonus / 统一六维计算，因此影响既有移速、职业攻击间隔和其他速度公式。
- 临时速度按最强来源取值，不叠加；源猫移除或卸饰品后撤销，离开覆盖后按音乐余效最多保留 40 tick。面板通过 MusicSpeedBonus 同步，原始基因不变。
- 音乐套装自身速度 +10、饰品生命属性 −10，以及既有音乐范围/动画不变。
- 新机制 music_speed_bonus 为 0～300 整数；旧 music_movement_bonus 保留第三方数据包的百分比语义，但内置扩展音响不再使用。协议 Forge 35 / NeoForge 28，需要一起更新。

完整命名/源文件/效果/ID 表：[猫咪饰品-美术总表.md](猫咪饰品-美术总表.md)。19 张原图哈希：art/accessories-art-v3/manifest.json。

## 验证结果

- 两端 build 通过，既有饰品定义/载入/状态回归通过。
- 两端各 53/53 项真实 GameTest，在有 KubeJS 和无 KubeJS 两种环境均通过。
- 新/更新测试：两只音乐猫只加 20 速度；真实移动公式与攻击间隔读取该属性；原始基因和音乐猫自身不变；卸饰品/移除源猫/覆盖余效到期时撤销；同步数据清零。
- 两端实际客户端启动探针通过，35 张正式贴图与唯一鸡蛋占位图的烘焙模型资源 ID 正确。客户端真实属性计算路径读取/撤销 +20 速度，未写回基因；原有琵琶、医疗、音乐图标及圈、服装动作渲染测试继续通过。
- 21 组 Node 检查通过，其中新增 295 项图标原始哈希、命名、缺图清单和音乐属性接线检查。
- 成品 ZIP 校验通过：36 定义/模型、35 原图 + 1 占位、35 配方 + 1 Boss 饰品；所有 35 PNG 哈希一致；没有夹带测试代码或新增必装依赖。
- 既有全局配置、基因/筛选、轮盘等回归通过；git diff --check 通过（仅既有换行格式提醒）。
- 验证在隔离环境中运行，没有打开玩家存档；不代表覆盖全部第三方资源包和光影组合。

日志：各端 build/accessories29-gametest.log、accessories29-gametest-clean.log、accessories29-client.log；tests/build/accessories29-package-final.log、accessories29-global.log。

## 部署

完成时间：**2026-09-17 14:51:18（本机时间）**。

Forge、NeoForge 两个开发客户端及各自 local-multiplayer-server 均已从 .28 更新到 .29。四处按实际 [[mods]] modId=laowu 检查只有一个启用版本，安装哈希等于构建哈希：

- Forge：011CC578A403600EC9B872C34D36685F3C406ACB6ACD53959C2CCE2B850D49D8
- NeoForge：62A6995BA46FE34E66C65DDDD36F8AAED5C29A34E22E01445D46ED2F991CCC63

旧 .28 包分别备份在各实例下 `mod-backups/create-meowchanics/20260917-145112-accessories.29/`，可以恢复：

- Forge 旧包：9C8C366AE26E196BCF008126D9103CC9589BCD127533A0DD8E84E2895AC8AF79
- NeoForge 旧包：30DAAD307D8C9EA22522961F44FBE689DAA97DF3F756B4273F23EF1B3419B1BE

部署前后无 Java 游戏进程、25565/25566/25575/25576 无监听。两台常驻测试服保持关闭。未改玩家存档/配置/其他模组，未强制关游戏，未提交、推送或公开发布。

脚本：tests/build/deploy-art-accessories29.ps1。逐目标结果：deployment-accessories.29.json。
