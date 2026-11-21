# 发布版本与内部开发版本

- `mod_version` 是对外版本号，目前为 **2.3.3**。只有用户指定新版本时才修改，双端必须一致。
- `internal_build` 是独立的内部构建编号，如 `compat.3`。有此编号时，文件名与模组列表均显示 **2.3.3-dev.compat.3**，不是下一个公开版本。
- main 保留已经确认的发布代码；新功能只在 develop 开发。合并前检查双端构建与回归测试，不使用覆盖式重置同步分支。
- develop 的本地开发默认编号写在双端 gradle.properties 中；CI 使用 `ci.<运行编号>.<重试编号>`。编号相同时表示同一开发阶段，不代表新公开发布。

2026-11-12 按小版本更新整理为 2.3.3，双端 internal_build 置空，默认 build 的文件名和包内版本均为 2.3.3。本版汇总 2.3.2 后的兼容修复及截至 painting.10 的猫画更新，见 [2.3.3 更新日志](changelog-2.3.3.md)。历史开发日志保留原版本号。版本命名或本地打包不等于上传平台、推送 Git 或创建发布标签。

2026-11-21 起，develop 的默认内部编号为 `brewing.1`，双端包内版本为 `2.3.3-dev.brewing.1`。新增配方见[哈气药水与原版酿造台](optional-create.md#哈气药水与原版酿造台)。

在对应加载器目录中构建：

```powershell
# 内部测试包：文件名和包内版本一致
.\gradlew.bat build '-Pinternal_build=compat.3'
# 明确构建正式发布包，忽略内部编号
.\gradlew.bat build '-Prelease_build=true'
.\gradlew.bat printModVersion
```

Forge 使用 Java 17，NeoForge 使用 Java 21。正式 changelog 按公开版本编写；开发记录放在单独的功能文档中。构建产物和本地历史备份不提交到源码 Git 仓库。
