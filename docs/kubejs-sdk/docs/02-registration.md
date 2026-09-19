# 注册物品和饰品定义

“物品注册”和“饰品效果定义”是两个步骤：存在物品不代表它能装猫咪饰品槽。

## 1. 物品注册（startup_scripts，双方安装，重启）

```js
StartupEvents.registry('item', event => {
  event.create('example_cat_charm')
    .displayName('示例猫符')
    .maxStackSize(1)
    .texture('minecraft:item/prismarine_shard')
})
```

这里实际 ID 是 kubejs:example_cat_charm。使用原版贴图无需另外附 PNG。完整可复制文件位于 [Forge 演示](../examples/new-accessory/forge/kubejs/startup_scripts/example_cat_charm.js)；实际目录是包根 examples/new-accessory/forge/kubejs（NeoForge 选另一目录）。

## 2. 数据定义（服务器）

逻辑资源 ID：kubejs:cat_accessories/example_cat_charm.json。schema 的必填项为 schema_version=1、item、effects；纯事件饰品也要 effects:{}。

```json
{
  "schema_version": 1,
  "item": "kubejs:example_cat_charm",
  "required_outfit": "any",
  "exclusive_group": "examplepack:healing_charms",
  "script": "examplepack:heal_on_hit_v1",
  "description": "实际命中后回复自身2点生命，冷却10秒。",
  "effects": {"speed": 10}
}
```

Forge：
```js
ServerEvents.highPriorityData(event => {
  event.addJson('kubejs:cat_accessories/example_cat_charm.json', definition)
})
```

NeoForge：
```js
ServerEvents.generateData('after_mods', event => {
  event.json('kubejs:cat_accessories/example_cat_charm.json', definition)
})
```

definition 是上面的 JSON 对象，不是一个未定义的魔法变量；完整演示文件已提供实际声明。也可用静态 kubejs/data/kubejs/cat_accessories/example_cat_charm.json，但不要同时保留同 ID 的生成器和静态定义。

## 字段

- enabled 默认 true；false 保留物品但禁用饰品定义。
- required_outfit 默认 any；职业不匹配不生效。详见 contracts/api-v3.json。
- exclusive_group 非空命名空间 ID；同组不允许同时有效。重复同一饰品不会叠加。
- description 是字符串，不是富文本数组。固定描述修改数值时也要同步修改。
- effects 是白名单键 → 数值，不支持随意加入 onTick、函数或 JavaScript 字符串。
- script 仅供事件辨认实现方案，不会加载 JS 或自动禁用原生键。
- charge 可选；容量、初始值和消耗由明确规则控制，详见 API，不意味着自动支持机械动力注液。

最大耐久是物品注册属性，不是 effects 字段。原生护胸/金基咪已经注册 50 耐久并在正确阶段扣除；不要再用事件扣第二次。自定义耐久物品可用 handle.damageDurability，返回 true 表示此次损坏。

## 添加贴图

使用自己的资源命名空间和 item 模型，按所选 KubeJS 版本的资源注册方式提供客户端资源；不要覆盖整个 assets/laowu。ID 作为存档契约长期保留，改显示名称或图片不需要改物品 ID。
