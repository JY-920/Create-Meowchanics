# Forge 野生猫逃跑配置启动修复

2026-09-09 启动日志：WildCatPanicMixin 应用至 PanicGoal 时出现
`Non-private field cannot be aliased. Found f_25684_`。

- 根因：无 refmap 的 Forge 构建通过 Shadow aliases 兼容开发名和 SRG 名，但目标 mob 字段为 protected，Mixin 禁止对非私有目标字段使用这种别名。
- 同时修正 WildCatPanicMixin 和 WildCatAvoidMixin：在构造器返回时保存传入的 PathfinderMob 到独立 Unique 字段，不再依赖原版受保护字段名。
- 已核对原版 AvoidEntityGoal 的两个简化构造器都调用完整构造器，完整构造器注入覆盖三种构造路径。
- 保留野生猫逃跑配置的启停功能，不删除 AI，不影响宠物猫或其他生物。
- NeoForge 使用官方字段名，不带上述 aliases，不存在本次相同写法，因此无需改动。
- 本修复保留飞行员猫伤害提高 60% 等已完成改动。
