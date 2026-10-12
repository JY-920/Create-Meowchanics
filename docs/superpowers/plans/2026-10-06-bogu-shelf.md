# 博古架实现计划

Spec: ../specs/2026-10-06-bogu-shelf-design.md

## Global constraints
保持现有 develop 脏工作树；仅修改本任务，禁止自动提交/推送。源模型只复制不覆盖。有/无 Create 均注册核心博古架。网易客户端属于另一聊天，不操作。

## Task 1: 按键与精确模型导出
先新增回归验证未绑定默认、删除下降映射、模型注册与九格导出守恒，运行观察缺少实现失败。新增可重复 exporter，保留贴图字节与 UV，按九格裁剪面和碰撞包围盒。正常默认键改变不迁移 options.txt。
Expected: node tests/bogu-shelf-assets.mjs 从失败到通过；既有 Node 检查通过。

## Task 2: 核心多格方块与展示猫
先添加真实 GameTest，再实现 block、seat entity、注册、loot、木斧标签、语言和猫客户端缩放。座位持有真实猫并持久化，及时释放、不抢其他玩家宠物。只在座位猫分支短路移动行为，饰品/词条公开 tick 仍执行。
Interfaces: PART 0..8，底部定位 PART=1；FACING north 基准，列增量向 east；右侧大空位 source X=-12, Y=21, Z=0。
Expected: 两端隔离 GameTest 全通过，有/无 Create；实际客户端模型、默认按键与座位渲染检查通过。

## Task 3: 验证、审查与部署
正常双端 build、发布隔离检查，最终独立审查本任务变更，重要发现一次修复并回归。检查目标实例进程，旧包恢复性备份，部署 SHA-256 和单启用 laowu 校验。保持公开版本 2.2.2，内部 optional-create.8。
Expected: build 成功，部署日志记录两端 Verified=true；若目标客户端运行只报告等待退出。

## Review focus
多格跨区块、四向 UV、结构清理递归、原猫坐姿恢复、实体加载先后与座位空乘客、权限边界、破坏后安全位置、后台窗口 Ctrl、未覆盖已有玩家键位；不引入 Create 或测试依赖到核心/正式包。
