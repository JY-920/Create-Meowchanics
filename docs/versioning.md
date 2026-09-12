# 发布版本与内部开发版本

- `mod_version` 是对外发布版本号，目前为 **2.1.4**。只有决定正式发布时才修改，双端必须一致。
- `internal_build` 是独立的内部构建编号，如 `accessories.1`。有此编号时，文件名与模组列表均显示 **2.1.4-dev.accessories.1**，不是正式的 2.1.5。
- main 保留已经确认的发布代码；新功能只在 develop 开发。合并前检查双端构建与回归测试，不使用覆盖式重置同步分支。
- develop 的本地开发默认编号写在双端 gradle.properties 中；CI 使用 `ci.<运行编号>.<重试编号>`。编号相同时表示同一开发阶段，不代表新公开发布。

在对应加载器目录中构建：

```powershell
# 内部测试包：文件名和包内版本一致
.\gradlew.bat build '-Pinternal_build=accessories.1'
# 明确构建正式发布包，忽略内部编号
.\gradlew.bat build '-Prelease_build=true'
.\gradlew.bat printModVersion
```

Forge 使用 Java 17，NeoForge 使用 Java 21。正式 changelog 按公开版本编写；开发记录放在单独的功能文档中。构建产物和本地历史备份不提交到源码 Git 仓库。
