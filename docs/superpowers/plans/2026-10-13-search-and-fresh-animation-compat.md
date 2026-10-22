# 搜索与新鲜动画兼容修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** 修复第三方整合包创造栏启动崩溃，并恢复 Fresh Animations 猫咪眼睛和资源包动画。

**Architecture:** 搜索兼容仅在 JECharacters 的查询出口归一化本模组的重复 ItemStack，不再注入创造栏或读取滚动字段。猫咪渲染通过原版 CAT 模型层接入已加载的自定义实体模型，继续保留本模组专用姿态和渲染层；不强依赖 EMF/ETF。

**Tech Stack:** Forge 1.20.1、NeoForge 1.21.1、Mixin、原版模型层、EMF/ETF、Fresh Animations 1.10.4。

**Spec:** 用户本轮请求：修复搜索启动问题；兼容新鲜动画；眼睛不能消失；保留材质包提供的猫咪特殊动画。

## Global Constraints

- 在已有 develop 工作树实施，保留 main 与未提交的本地预览缓存。
- 不修改玩家存档、其他模组 JAR、材质包原文件或配置。
- 饰品 API v1–v3/schema 1、词条 API v1/schema 1 和已存物品 ID 不变。
- 测试模组、测试 AT 和测试资源包只放 build；正常 build 保留发布隔离验证。
- 未安装相关可选模组时仍能启动；运行中的目标客户端不得替换 JAR。
- 本轮不自动提交、推送、发布新公开版本。

## Review Focus

- 搜索索引中的非 ItemStack 对象和其他命名空间不被误删。
- 不同 NBT/组件的合法变体和原结果顺序保留。
- 创造栏滚动字段被其他模组改为 public 时仍可加载。
- EMF 未安装、资源包卸载和资源重载均回到正常猫模型。
- 资源包模型的眼睛、正常动画和本模组哈气/猫饼/套装不互相污染。

## Task 1 搜索出口兼容

**Files:** 双端 CatCreativeSearchResults、新 JECharactersSearchMixin、laowu.mixins.json；tests/production 和专用搜索回归。

**Interface:** 可选查询回调只读取查询结果，必要时返回新列表；原列表不被改写。

- [x] 使用真实已安装 FakeArray 和真实物品验证旧逻辑的多行重复结果。
- [x] 移除创造栏注入，以软目标在 JECharacters 查询出口处理本模组结果。
- [x] 验证完整数据变体、无匹配、外部物品、泛型对象和可选依赖缺失。
- [x] 正式 SRG 启动探针同时验证搜索结果与 public 滚动字段的加载。

## Task 2 新鲜动画模型入口

**Files:** 双端 HissingCatRenderer、HissingCatModel 和小型模型兼容辅助类；tests/fresh-animation-client 与隔离客户端配置。

**Interface:** 模型选择辅助类返回 HissingCatModel；接入有效资源包模型时使用 CAT 层，否则使用既有自定义层。

- [x] 真实客户端加载 EMF/ETF 与用户资源包，旧实现必须因未接入眼睛/动画部件失败并留截图。
- [x] 接入标准模型层，允许资源包自行管理耳朵和自定义骨骼，不硬编码新鲜动画资源。
- [x] 验证真实眼睛网格、随时间变化的资源包动画、正常/哈气切换和资源重载。
- [x] 检查无 EMF/无资源包时的既有模型，以及套装/猫饼/博古架路径。

## Integration

- [x] 双端普通 build、冻结 API、发布隔离和既有回归通过。
- [x] 搜索与渲染合并检查无相互改动或依赖。
- [x] 核对截图后备份并部署双端开发包，记录哈希和实际验证范围。

**完成记录：** [部署及验证](../../deployment-search-fresh-compat.1.md)。Forge 渲染使用真实 SRG 成品环境；新增混合眼睛和非活动演出词条回归。博古架做代码路径检查而非本轮独立视觉录像。保留 develop 工作树和全部未提交修改，不执行提交／推送；资源包兼容承诺限于记录中的实测组合。
