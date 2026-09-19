# 模组打包隔离

本 SDK 独立放在 releases 下，不属于 src/main/resources。脚本、文档、测试探针、测试世界、截图、缓存和临时文件不进入本体 JAR。

工程有 release-isolation.gradle：资源/归档排除规则加最终 JAR 检查，随 build、assemble、check 与 Maven 发布执行。冻结的 API 验证也随 jar 执行。探针、*.js、脚本包等误入归档将导致验证失败，而不是当成正常发行品。

必须保留的内容：

- cn/laowu/mod/api 和兼容桥接类：用户脚本依赖的正式运行时 API。
- kubejs.plugins.txt、kubejs.classfilter.txt：可选 KubeJS 自动发现与访问控制，不是玩家脚本。
- data/laowu/cat_accessories：36 件原生定义，不装 KubeJS 时仍需使用。
- .bbmodel / cat_animation_clips / 正式 PNG：本模组会直接读取的游戏资产，不是无用测试材料。

清理采用隔离和可恢复归档，不删除用户现有存档、配置、kubejs 或美术源文件。历史测试记录留在测试/构建目录中，禁止把整个开发项目打成模组 JAR。sources JAR 属于源码发布，不是给玩家的运行包。

SDK 的 manifest.json 可验证交付文件；它不代替模组 JAR 的 modId 和 SHA 检查。
