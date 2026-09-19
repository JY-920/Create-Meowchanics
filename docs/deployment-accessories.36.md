# accessories.36 — 36 件 KubeJS 替换示例与两件保护饰品耐久

日期：2026-09-17。公开版本仍为 2.1.4，本地内部版本为 2.1.4-dev.accessories.36；未提交、推送或发布。

## 交付

- [示例说明与 36 件索引](examples/cat-accessories-36/README.md)。
- [API v3](cat-accessory-scripting.md)：稳定上下文读取、成功扣血事件、早期闪避与治疗事件、真实装备耐久操作。
- [Forge 示例 ZIP](../build/accessory-examples36/cat-accessories36-kubejs-forge.zip)。
- [NeoForge 示例 ZIP](../build/accessory-examples36/cat-accessories36-kubejs-neoforge.zip)。

示例是 **7 件事件脚本 + 29 件由 KubeJS 定义数据并复用引擎**，不是把全部职业 AI / 弹幕 / 渲染重写成 JavaScript。现有 36 件 ID、贴图、职业门槛、互斥、稀有度和奖励来源保持不变；对应事件版原生键移除避免重复生效。KubeJS 仍非强制前置。

软木护胸和金基咪改为原版 **50 耐久**，每次成功触发精确扣 **1**，**15s / 300 tick** 冷却，互斥不变。未触发、完全取消不扣；第 50 次仍保护，护胸最后的六秒护盾不会被物品损坏提前清除。旧物品无需替换，新最大耐久自动适用，已有损耗与存档保存。

网络版本 Forge 40、NeoForge 33；客户端/服务端需要同版本。

## 验证

| 环境 | 结果 | 日志 |
| --- | --- | --- |
| Forge 1.20.1 + KubeJS 2001.6.5-build.26，实际装入三个替换脚本 | 76/76 通过 | forge-1.20.1/build/accessories36-examples-final.log |
| NeoForge 1.21.1 + KubeJS 2101.7.2-build.377，实际装入三个替换脚本 | 76/76 通过 | neoforge-1.21.1/build/accessories36-examples-complete.log |
| Forge，无 KubeJS，原生模式 | 76/76 通过 | forge-1.20.1/build/accessories36-no-kubejs-final.log |
| NeoForge，无 KubeJS，原生模式 | 76/76 通过 | neoforge-1.21.1/build/accessories36-no-kubejs.log |
| Forge / NeoForge 真实客户端 | 双端通过，截图已人工检查 | 两端 build/accessories36-client.log |
| Node 源码/示例检查 | 26 组通过，新增示例 344 项断言 | tests/*.mjs |
| 发行包 | 双端类/资源/配方/原始图片检查通过 | tests/verify-accessory-package.ps1 |
| 两个 ZIP | 各恰好三个运行脚本，与已测试源文件逐字一致 | build/accessory-examples36/ |

原生无 KubeJS 模式中的 KubeJS 专属测试会按现有测试器设计跳过；原生饰品效果和耐久测试仍执行。KubeJS 模式进行了真实 /reload、保存/还原和移除验证。

实际效果覆盖：36 件逐件职业与互斥及六维加减，火焰免疫/灭火、闪避、连击层数/间隔/重置、满血增伤无冷却、真实移动减伤与反伤、治疗倍率、暴击加速、休息回血、主人范围、拾取筛选/磁吸、嘲讽与低仇恨、近战/弹幕击退、10 倍自爆，以及钩子方向、蜂蜜地面、蓝火倍率、物流二级药水、工程三类弹药、医疗减伤、音乐速度属性、治疗烟雾、喷水驱散、死亡分裂继承。测试不是所有第三方整合包组合的保证。

耐久专测包含：被取消的伤害、未达门槛、重复吸收、真实 50 次触发及第 50 次保护、恰好第 299/300 tick 冷却边界、存档恢复、handle 对真实装备的修改、旧物品默认耐久。客户端逐件检查 36 件中英文提示，含真实渲染截图 accessory-text36.png 和部分损耗 33/50 文本。

### 测试中修正的问题

- NeoForge 服务端脚本禁止对 global 赋值：改用 KubeJS 共享服务端脚本词法作用域。
- 原版 Java 方法名不能直接照搬到 KubeJS 包装实体：新增并使用稳定的 ctx 接口。
- Forge 测试事件监听器内部类与已有测试同名：改为唯一类名，未改生产事件逻辑。
- 曾误并发启动同一个 Forge 隔离测试世界，产生目录锁；只停止了确认身份的失败测试进程，未关闭用户客户端。
- Forge 远距离炮击在无 KubeJS 时也失败：无玩家测试世界中补足炮击区域的实体运行区块后通过；仅修正测试场，不修改生产炮击逻辑。新 accessoryRunId 支持隔离的干净世界，不删除旧测试日志或用户存档。

## 部署

2026-09-17 21:36:44 已更新 Forge / NeoForge 两个客户端与两台本地测试服务器。四处均按包内 [[mods]] 的 modId=laowu 确认只有一个启用版本，安装包 SHA-256 与已核验构建一致，旧 accessories.35 均保留可恢复备份，没有删除旧包。

KubeJS 示例只进入隔离测试和交付 ZIP，不覆盖用户现有 kubejs；测试服务器继续保持关闭。没有修改用户存档、配置或其他模组。完整路径和时间见 [机器可读记录](deployment-accessories.36.json)。

- Forge SHA-256：`3B75508858FBD8D44CB31AB89007C19515333F249A5C08A8C31A3ED02FCDAD9C`
- NeoForge SHA-256：`0D11FB191280D5567D861ACCDD909896C388A0BB272799DBA8118BD5D8320763`
- 旧包备份目录（各实例下）：`mod-backups/create-meowchanics/20260917-213639-accessories.36/`
