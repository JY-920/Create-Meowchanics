# 2.3.1 本地部署与验证

2026-10-14 00:33:55，Forge 1.20.1 与 NeoForge 1.21.1 本地开发实例已部署正式 2.3.1。双端 gradle.properties 的 mod_version 均为 2.3.1、internal_build 置空；正常 build 生成的文件名、包内元数据和实际安装版本一致，不是重命名旧包。

部署前检查到先前运行的开发客户端已经退出；脚本再次检查进程后替换。原 2.3.0-dev.compat.2 已移入各实例的 mod-backups/create-meowchanics/20261014-003355-2.3.1-release-231，未修改玩家存档、配置、资源包或其他模组。每端启用的 modId=laowu JAR 均为一个，安装与构建 SHA-256 一致，详见 [部署记录](deployment-2.3.1.json)。

## 本轮修复与验证

- JEI 猫饼预览错误的根因为空材质字段经 ResourceLocation.tryParse 解析为有效的空资源路径，未触发原橘猫回退。双端仅在字段为空白时跳过解析，继续使用原版 red.png 橘猫默认材质；非空的已保存材质与熔炉成功烧制后的随机生成逻辑不变。
- Neo 修复前 jei-orange-red 的新增断言明确失败；修复后双端 jei-orange-green 各 16 项真实 GameTest 通过，覆盖无数据预览、已保存黑猫材质、实际配方结果、真实熔炉随机生成和满槽行为，以及既有完整还原和猫饼交互。
- Neo jei-orange-231 独立客户端启用 JEI、Curios、Fresh Animations 1.10.4、EMF／ETF；实际 JEI 配方与客户端同步探针通过，打开烧炼页面并保存 screenshots/jei-orange-pancake.png，已人工目视确认橘猫预览。测试夹具仅使用独立 build 目录。
- 双端 build/release-231-build.log 正常 build 成功；冻结饰品 API v3、词条 API v1、运行包发布隔离与各 23274 项饰品定义／状态检查通过。
- tests/standalone-recipes.mjs 双端各 60 张独立配方获取图检查通过；新 2.3.1 成品包的 Mixin／导航字节码回归 408 项通过。
- Forge build/release-231-production.log 记录实际正式 SRG 客户端在不安装 Create／JECharacters 时进入标题页并正常退出，验证本次成品包启动而不只验证开发命名环境。

## 范围与历史证据

玩家更新内容见 [2.3.1 更新日志](changelog-2.3.1.md)。本轮未重新执行所有动画和博古架探针；此前搜索／新鲜动画及面团功能的实测范围和限制保留在 [compat.1](deployment-search-fresh-compat.1.md) 与 [compat.2](deployment-standalone-dough-compat.2.md)，不将此前博古架混合探针失败算作通过。

本轮未提交、推送、合并 Git 分支或上传公开发布平台。
