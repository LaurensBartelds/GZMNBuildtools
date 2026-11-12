# Resize Tool Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `/gzresize` (module `resize`): nearest-neighbour scaling of the cuboid selection, anchored at its minimum corner — spec §4, Phase 2 feature 2.

**Architecture:** Pure engine `modules/resize/engine/ResizePlanner` maps each output cell to its source cell (`plan(Bounds, sx, sy, sz)` → `Plan(Map<Cell,Cell> placements, int blockCount, Bounds out, String problem)`). `modules/resize/command/ResizeCommand` parses the scale, requires a cuboid selection, optional pattern remap via `StateMapper`, enforces `Limits.Kind.BLOCKS` (`generation.max-blocks`), `-p` preview via `PreviewService`, applies via `PlayerEdits.async` (`//undo`).

**Tech Stack / Spec:** Java 25, Paper 26.2, FAWE 2.16.1, `mvn -B verify` = CI, JUnit 5 + Mockito. Spec: `docs/arceon-parity-spec.md` §4.

## Global Constraints
Same as the roof/loft/revolve plans: module `modules/resize`, no module dependencies (ArchUnit auto-covers); Brigadier command, player-only, permission `gzmnbuildtools.resize` (op default, child of `gzmnbuildtools.*`); `modules.resize: true` in config.yml; help entry (BUILDS, `Material.ITEM_FRAME`); limits; deterministic preview; `PlayerEdits.async` apply; messages via `Messages`/`tr` into lang en/nl (sorted, placeholders preserved); tests: `ResizePlannerTest` (pure) + `ResizeCommandTest` (FakeMessages); README section + module row + permission bullet; no new deps.

## Tasks (all done in the commits on this branch)
1. Engine + `ResizePlannerTest` (double/half/non-uniform/empty) — commit.
2. Command + module + wiring + `ResizeCommandTest` — commit.
3. lang en/nl + README + CI green — commit.
