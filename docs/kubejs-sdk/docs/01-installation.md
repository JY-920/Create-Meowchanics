# 安装、切换与撤销

## 36 件替换方案

1. 备份实例的 kubejs 与存档；确认安装对应版本本模组和 KubeJS 自身前置。
2. Forge 复制 replacement/forge/kubejs；NeoForge 复制 replacement/neoforge/kubejs。**二选一，不合并整个 SDK。**
3. 最终 server_scripts 下需有四个同版本文件：laowu36_compat.js、laowu36_catalog.js、laowu36_effects.js、laowu36_data.js。compat priority 200，catalog priority 100，data/effects 默认优先级。
4. 启动或 /reload，在 kubejs 日志检查加载无错误。三个旧文件升级时要全部替换，并补上 compat；不要混用新旧文件。
5. 用原本物品测试：没有新物品注册、无需新配方、没有第二套 laowu: 饰品。

单机复制到当前实例；专服复制到服务器。仅这 36 件覆盖不注册客户端新物品；客户端仍需同版本本模组，定义由服务器同步。新物品演示则需要双方都装其 startup 注册和贴图。

兼容检查或完整事件注册未完成时，数据适配器不发出 36 条覆盖，保留模组原生定义，并在日志警告。注意这不是修复任意语法错误的魔法：看到错误应修正或卸下整套替换包并重启，不要继续堆叠另一套同 ID 脚本。

## 可选新物品演示

examples/new-accessory/forge 或 neoforge 下的 kubejs 是独立示例，注册 kubejs:example_cat_charm（示例猫符），速度 +10，实际命中后每 10 秒最多治疗自己 2 点。不加制作配方；测试时用 /give @s kubejs:example_cat_charm 获取，再放猫咪饰品槽。

新注册物品需要完整重启；仅 server_scripts 参数可 /reload。不要在已有存量物品时删除或重命名它的 startup 注册。此示例可与 36 件方案一起运行，但不是它的依赖。

## 撤销

36 件方案：把四个 laowu36_*.js 同时移出 server_scripts 后 /reload 或重启，原生定义恢复，旧物品和背包不删。不要只删除 effects 留下旧 data。

新物品演示：可先移走服务端效果，保留 startup 注册以避免存量物品丢失；完整卸载前由服主处理已有物品。任何曾写 setStatBonus 的自定义版本先执行迁移清理。本 SDK 新物品演示没有永久动态属性写入。

若别的脚本/数据包也覆写同一物品，先处理优先级冲突；本 SDK 不扫描或删除用户其他脚本。
