# 36 件猫咪饰品 KubeJS 替换示例

需要本模组 accessories.36 / API v3，以及对应 Minecraft 和加载器版本的 KubeJS 与前置。无需 KubeJS Curios。

备份原有 kubejs 后，将包内 kubejs 文件夹合并到游戏实例（专服为服务端）的根目录，重启。四个文件放在 server_scripts 中：
- laowu36_compat.js：稳定 API 兼容检查，只有整套就绪才启用覆盖。
- laowu36_catalog.js：全部 36 件原物品定义、数值、职业限制与互斥。
- laowu36_effects.js：7 件的事件触发示例。
- laowu36_data.js：当前加载器专用数据适配器，Forge 和 NeoForge 两版不能同时安装。

7 件为事件实现：隔热手套、羽毛玩具、黄油块、磨牙骨、猫咪玩偶、暖绒围巾、钉刺项圈。
其余 29 件是 KubeJS 数据定义调用模组机制；并非全部改成纯 JavaScript。职业弹幕、AI、特效、分裂与护盾结算仍复用引擎。事件版移除同名原生效果键，防止双倍生效；不新增物品或合成配方，不改 Boss 专属奖励池。

参数在 catalog 的 definition.effects 中统一修改。事件版 scriptDescription 是展示文字，需要随数值一起修改。普通修改可 /reload，检查 KubeJS 服务端日志四文件加载无 ERROR。
若已有脚本控制相同 ID，请先手动合并，勿装两套定义。

撤销时同时移走四个文件并 /reload 或重启，原生数据恢复，物品与存档保留。不要只删除 effects 文件，否则七种机制会缺失。

软木护胸 / 金基咪：50 耐久，每次成功触发扣 1，冷却 300 tick（20 TPS 为 15 秒），最后一次保护仍生效，二者互斥。原生逻辑已经扣耐久，不要在脚本重复扣。

API-v3.md 提供 ctx / handle / 事件接口。其仓库相对链接在压缩包外的完整项目文档中可用；本包内四个 JS 已包含所需的运行代码。
