# 无机械动力猫面团与还原：2.3.0-dev.compat.2

2026-10-14 00:11:01 已备份并部署两个本地开发实例；各实例只有一个启用的 laowu JAR，构建与安装 SHA-256 一致。详细路径和备份见 [部署记录](deployment-standalone-dough-compat.2.json)。公开版本仍为 2.3.0，没有自动提交或推送。

## 本次玩法

- 无 Create 时，小麦＋猫毛无序合成 1 个猫面团；原版熔炉烧制 10 秒得到 1 个随机花色猫饼，带新生成的外观、六维属性和词条数据。
- 随机只发生在真正烧制成功后；配方预览稳定。猫饼不可叠加，满输出槽不重新随机、不继续扣面团。只支持此次原版熔炉路线，不新增烟熏炉／高炉或第三方机器配方。
- 无 Create 时，猫饼蹲下右键方块还原；成功才消耗（创造模式不消耗），使用既有完整快照恢复逻辑。正常蓄力投掷、铲子和铁砧交互不变。
- 无 Create 的说明为“蹲下右键方块还原猫咪”；有 Create 时保留原鼓风机说明，新配方和右键还原不启用。

## 验证证据

- Neo 先失败后通过：dough-red 有 3 个预期缺失失败；dough-green2 的新功能 4 项通过。Forge dough-green 4 项通过。
- 双端 dough-recipes-full 各 15 项真实 GameTest 通过，覆盖全部独立配方、原铲子／铁砧／收纳袋、新真实熔炉处理和阻塞输出、完整还原的主人／姓名／套装／词条／六维属性／物品栏及阻塞保留。
- 双端 dough-create 各 4 项通过，验证有 Create 时路线不改变。
- 客户端 dough-client-jei2（Neo，standaloneWithOptionalMods＋standaloneCareClientOnly）实际 JEI 的 56 张普通合成、命名猫粮、两张鱼干、新熔炉配方、客户端同步和猫饼说明全部通过；独立养成 GUI、物品渲染及 Curios 检查通过。
- 双端正常 build 通过，保留 API 冻结、动画资源、发布隔离和各 23274 项饰品检查；独立配方静态获取图 60 张通过，摆放回归 52 项通过，Mixin／导航字节码回归 408 项通过。
- Forge 正式 SRG 环境第一次 dough-srg-no-create-compat2 捕获 burn 方法映射缺失；从实际 SRG 类和映射核对后，补齐精确 named／m_266209_ 选择器。重新正常 build、dough-final-mapped 4 项以及 dough-srg-no-create-compat2b 正式客户端启动均通过。失败证据未删除。

## 如实保留的测试限制

第一次 dough-client-jei 混合客户端运行未携带 JEI，并同时加载了旧博古架／拳击猫等探针；其中 BoguShelfClientProbe 报出 “Client never received brief hissing pose”。该混合运行不作为 JEI 或博古架通过的证据，本轮不修改博古架玩法。之后将本次客户端探针独立到新的测试 sourceSet，并显式启用 JEI／Curios，完成上述实际验证。没有重新宣称博古架哈气视觉通过；若需要追查该旧探针，应另做单独验证。

独立审查未发现本次实现的阻塞问题。未扩展或重新验证共享快照解码的所有损坏旧存档输入，也不承诺数据包覆盖私有配方 ID 后仍维持原合成契约。玩家存档、配置、其他模组和资源包均未修改。
