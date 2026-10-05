# Cat Editor Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans; independent assets and trait token tasks use dispatching-parallel-agents.

**Goal:** Deliver testable two-tab cat appearance/trait editor using provided assets.
**Architecture:** Server-authoritative block/menu locks nearby owned seated cat. Item token service performs lossless trait extraction/installation. Client assembles supplied atlas, live cat preview and buttons; existing genome/trait sync reused.
**Tech Stack:** Java17/21, Forge1.20.1,NeoForge1.21.1,Create6.
**Spec:** docs/superpowers/specs/2026-10-03-cat-editor-design.md

## Global Constraints

- 11 regions plus whole-body,1block per application;4trait slots,whole-level nonstackable token.
- Owned seated cat on horizontal adjacent Create seat; locked UUID and revalidation every action.
- Preserve dirty worktree/APIs/user scripts. No approval gates (user waived),no commit/push. Dual deployment only after clients stopped.

## Review Focus

- Ownership/seat departure/forged action indices cannot modify cats.
- Output full,trait conflicts/unknown custom ID/reload never consume or lose data.
- Menu closure/shift-click/disconnect return inputs exactly once; output readonly.
- UI sprite crop,slots/text fit and material preview uses actual cat texture.
- Existing API data and hooks remain compatible on both versions.

### Task1 Assets
- [x] Export cat_editor four-facing block/item from authored BBModel,copy originals/UI/token into both resources; asset parity test.

### Task2 Trait token
- [x] Add tests for full-level extraction/install/conflict/full/unknown definition cases then service+item (CatTraitTokenItem and CatEditorTraits),no registry edits by agent.
- [x] Contracts: token create(CatTraitType,int),traitId(ItemStack),level(ItemStack); service extract(Cat,ResourceLocation):ItemStack, install(Cat,ItemStack):boolean, validation failures do not mutate; servicecaller ensuresoutputempty/ownership. Item type LaoWuMod.CAT_TRAIT_TOKEN.

### Task3 Menu and rendering
- [x] Add CatEditorBlock,CatEditorMenu; server/client constructors(id,Inventory,Cat,BlockPos)/(id,Inventory,FriendlyByteBuf). Registry CAT_EDITOR/CAT_EDITOR_ITEM/CAT_EDITOR_MENU. Menu openingdata pos+uuid+entityId+catalogIDs.
- [x] Menu public entityId(),cat(Player),catalog(),traitLevel(int),regionName(int); click IDs100+region apply sample,1000+catalogIndex extract,20 install. Client sends integer-safe CatEditorActionPacket (vanilla button packet truncates); server validates.
- [x] CatEditorScreen uses authoredatlas, pagebuttons,11regions+whole,live preview,four-entry traitlist+full-description tooltip,3slots and normalplayerinventory.
- [x] Focused server tests RED/GREEN then actual clientscreen render/screenshot checks.

### Task4 Review and delivery
- [x] Independent review,dualserver/client checks,normal build withfrozenAPI/isolation; deploy2.2.2 and recordhashes.
