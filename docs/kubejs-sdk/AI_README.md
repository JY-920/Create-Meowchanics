# AI 接入指南：先读此文件

目标：通过本模组的公开猫咪饰品 API 注册、配置、实现和验证脚本；不要编造 Curios / KubeJS 事件。

## 最短阅读顺序

1. docs/01-installation.md：目标加载器与四文件替换规则。
2. contracts/api-v3.json：可用事件、方法签名、单位、稳定 ID。
3. contracts/accessory.schema.json：JSON 必填项和数值范围。
4. docs/02-registration.md、docs/03-effects.md：完整演示。
5. docs/API.md：事件阶段、生命周期和安全操作。
6. catalog/accessories36.json：36 件真实参数；replacement 中是已运行过的代码。
7. verification/RESULTS.md：区分真实 MC 测试、静态验证和未来未验证环境。

## 生成代码前确认

- loader 只能选 forge-1.20.1 或 neoforge-1.21.1，不同时安装两套 data 适配器。
- 新饰品使用自己的命名空间；替换 36 件才覆盖 laowu: 原 ID。别重新注册已经存在的 laowu: 物品。
- 属性名只有 health / attack / speed / stamina / intelligence / luck。attack 是战斗力，不存在 strength。
- required_outfit 用合同列出的字符串 ID，不使用中文名或枚举序号。
- 25 表示 25%，冷却单位 tick；20 TPS 时 300 tick = 15s，服务器卡顿时不是 15 秒墙钟。
- durability / charge / 冷却是三种不同概念，不互相替代。

## 必须遵守

- 只依赖 cn.laowu.mod.api 的公开接口和 CatAccessoryEvents，不反射内部类，不读写 LaoWu* 内部 NBT。
- Java.loadClass 仅加载本模组公开 API；ctx.cat 等原版对象允许交给 helper，但不要假设映射方法 cat.level() / getHealth() 在两版 KubeJS 都可用。用 ctx.gameTime / otherHealth / hasLivingAttacker() 等稳定接口。
- 不给 server_scripts 的 global 赋值；使用文件唯一前缀和 priority 排序的共享脚本作用域。
- 不在 /reload 时无限追加 Java 全局监听器；用 CatAccessoryEvents 注册，KubeJS 负责替换。
- beforeAttack / beforeHurt 是护甲前；acceptedAttack / acceptedHurt 是确实扣血后的同步回调。完全取消不会触发 accepted。after* 是旧的 tick 末观察。
- event/ctx 只在同步回调有效；不放进定时器或长久全局引用。stack 是副本；持久化通过 ctx.accessory() 返回的真实 handle。
- 模组 helpers 保留队伍、距离、线程与防递归限制。不要用裸 hurt 绕过限制，也不要每 tick 扫全维度。
- 准备接管某个原生 effect 时，从生成 JSON 中移除对应 effect 键，并使用自己的 script 标记。只加 script 标记不会自动禁用原生效果。
- 36 件覆盖只有在兼容检查通过且所有事件注册完毕后发出。不要去掉 laowu36_compat.js 或把 ready 提前设 true。
- 不注册配方、改 Boss 奖励池、删除物品或改用户存档，除非另有明确要求。
- 本包的 startup 示例是可选的新物品演示；不能为了运行原 36 件而强制安装它。
- 卸载自定义物品注册前需要处理存量物品；永久动态 stat bonus 要显式迁移清理，不能承诺删脚本就自动清除。

## 兼容性约束

本体支持 apiVersion() 和从 accessories.37 起的 supportsApi(version)。正常更新保留 supportsApi(3)=true、schema=1、旧方法与事件即可继续用。旧 accessories.36 用 apiVersion()===3 兼容回退；不要把本体版本号写死在脚本中。

工程 build 已检查冻结的 84 个公开签名、事件、effect/outfit/stat ID 及原 36 件物品 ID。不允许为让 build 通过而随意修改冻结合同；破坏性修改需要保留兼容适配器并重跑旧脚本。签名检查不能证明行为完全相同，仍需要实际 MC 回归。

## 给用户交付时

列出修改文件、目标环境、复制目录、效果归属（数据/事件/引擎）、测试证据和未测范围。不要把 Node 通过写成游戏内验证；不要把“复用原生机制”说成“全部纯 JS”。任何未来大版本兼容只能在验证后宣布。
