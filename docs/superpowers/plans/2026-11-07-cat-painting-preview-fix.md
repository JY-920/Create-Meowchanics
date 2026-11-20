# Cat Painting Preview and Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement these tasks. The user has asked to proceed directly.

**Goal:** Make the Cat Painting Table preview reliably and align its controls around the vanilla cartography parchment.

**Architecture:** Separate a bounded local raster preview from PNG production on confirmation. Leave wall asset storage and the network protocol unchanged; decode downloaded PNGs without LWJGL's temporary stack.

**Tech Stack:** Java 17/21, ImageIO, Minecraft GuiGraphics and Create's selection widgets.

**Spec:** Latest user request and existing `docs/cat-painting.md`.

## Global Constraints

Both loaders, unchanged save/protocol formats and Create optionality; build and verify before deploying, preserve existing working changes.

## Review Focus

- A normal large PNG must preview, print and redraw on the wall after a resource reload.
- A decode error must replace the spinner, and a newly selected good file must recover.
- Cropping/padding and block aspect ratio must agree between preview and print.
- Inventory slots and actions must align; the preview background must be the vanilla parchment.
- Selection changes must not upload; only confirmation may bind the image to the item.

## Task 1: Raster and decode

- [x] Reproduce large PNG failure on NeoForge with a seeded 768×384 fixture exceeding the old stack budget; verify the corrected fixture on both loaders.
- [x] Add `PaintingImages.preview(byte[], PaintingSpec)` regression coverage; assert original thumbnail bounds and pixel-mode parity with print.
- [x] Share the raster transform with `prepare`; decode final PNGs in `PaintingClientCache.decodeTexture` from a stream.

## Task 2: Screen and verification

- [x] In both `CatPaintingTableScreen` files align controls; share slot coordinates with the matching menus. Reuse Create widgets and the vanilla map parchment.
- [x] Register a transient texture directly from the raster. Keep the selected source snapshot for confirmation, show explicit failures, and ignore stale preview/print requests.
- [x] Pass seven core suites on each loader, the final isolated client probes and full builds. Inspect both client screenshots and deploy `2.3.2-dev.painting.2`; see `docs/deployment-cat-painting-painting.2.json`.
