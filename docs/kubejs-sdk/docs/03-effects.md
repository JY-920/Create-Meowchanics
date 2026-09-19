# 效果编写与验证

## 选择实现层

已有机制（例如属性 +10、磁吸、减伤、职业弹药）优先用 effects；新的触发逻辑用 CatAccessoryEvents。弹幕模型、职业寻路、动作渲染需要模组原生支持，不能靠一个 JSON 自创客户端骨骼动画。

现有 36 件的效果归属在 catalog/accessories36.json 的 scripted 字段：非空的 7 件由 laowu36_effects.js 接管对应键，其余使用原生效果。保留属性成本，不保留被 JS 接管的同名 effect，避免双算。

## 最小事件

```js
CatAccessoryEvents.acceptedAttack(event => {
  const ctx = event.context
  const charm = ctx.accessory('kubejs:example_cat_charm')
  if (!charm || String(charm.script) !== 'examplepack:heal_on_hit_v1') return
  if (!charm.tryActivate('examplepack:heal', 200, 0)) return
  ctx.healSelf(2)
})
```

这是实际命中才检查冷却的例子，冷却在触发时开始；满血时治疗被上限截断，但仍算一次触发。200 tick 是 10 秒，chargeCost=0 表示不耗哈气。不要把它放到 beforeAttack 后再声称“只在真正命中时触发”。

## 常用改动

- beforeAttack：ctx.setDamage(ctx.damage * 1.2)；只作用于当前待结算攻击，还会经过护甲等流程。
- beforeHurt：减伤或 event.cancel()，先验证佩戴、职业、队伍和伤害类型。
- beforeAvoid：取消合法受击，适合免疫/闪避；不能绕过虚空和最终死亡保护。
- beforeHeal：ctx.setAmount(ctx.amount * 1.2)，不是直接重复调用 heal 导致叠算。
- acceptedHurt：按实际损失生命计算反伤，用 Api.reflectDamage 保留队伍与防递归检查。
- tick / fastTick：每 20 / 10 tick，尽量常数工作量，范围扫描用有限 helper。
- projectile：用 ctx.setProjectileDamage / scaleProjectileSpeed；取消也消耗正常攻击冷却。
- 新状态：handle.number/text/cooldown/tryActivate，键带自己的命名空间，不能持有过期 handle。

## 验证清单

1. startup 注册成功，两端没有未知物品；data 目录与加载器回调正确。
2. 猫能装备，取下清除效果，职业限制/互斥/重复装备有效。
3. 真实成功、被取消、被护盾吸收、目标死亡和原版无敌帧分别测试。
4. 主人/同队不误伤，反伤与追击不递归，辅助猫不因此获得主动攻击 AI。
5. 数值和单位正确，无原生＋脚本双倍；显示文案与实际参数一致。
6. 猫饼/猫袋、实体保存还原、切职业、换槽和 /reload 后状态正确。
7. 客户端提示与服务端一致；插件未安装时原生模组仍能启动。
8. 若修改底层或升级大版本，在对应真实 MC/KubeJS 环境重新跑，不能仅凭 JS 语法或 AI 阅读宣布通过。

示例不自动提供充能/合成配方/奖励掉落。要添加这些内容，应单独明确材料消耗与来源，不要凭调用 recharge 宣称消耗了资源。
