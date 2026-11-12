# Shadow Tool Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `/gzshadow <pattern> [dir=down] [dist=32] [-p]` (module `shadow`): projects the top-exposed blocks of a build (or selection) along a direction onto the first surface and paints them with the shadow block, never overwriting build blocks — spec §7, Phase 3 feature 1.

**Architecture:** Pure engine `modules/shadow/engine/ShadowPlanner` with `plan(Bounds, Set<Cell> build, SolidOracle solid, int dx, int dy, int dz, int dist, int minY, int maxY)` → `Plan(Map<Cell,Cell> targetToSource, int blockCount, String problem)`. Sources are the exposed face of the build: per column the topmost build cell for `down`, per perpendicular column the extreme cell along the direction otherwise. Each source walks up to `dist` steps; the first solid, non-build cell becomes the shadow target. `modules/shadow/engine/ShadowSpec` (ParamSpec: pattern slot + `dir` choice + `dist` int). `modules/shadow/command/ShadowCommand` mirrors `RoofCommand` mode handling (cuboid selection → build = non-air cells in it; else looked-at build via `BuildEditor.scan(..., TargetMode.STRUCTURAL, ...)`), `Limits.Kind.BLOCKS` (`generation.max-blocks`), `-p` preview via `PreviewService` + `PlayerEdits.ghost`, apply via `PlayerEdits.async` (`//undo`).

**Tech Stack / Spec:** Java 25, Paper 26.2, FAWE 2.16.1, `mvn -B verify` = CI (no local JDK), JUnit 5 + Mockito. Spec: `docs/arceon-parity-spec.md` §7.

## Global Constraints
Same as roof/resize plans: module `modules/shadow`, no module dependencies (ArchUnit auto-covers); Brigadier command `gzshadow` (alias `shadow`), player-only, permission `gzmnbuildtools.shadow` (op default, child of `gzmnbuildtools.*`); `modules.shadow: true` in config.yml; help entry (BUILDS, `Material.BLACK_CARPET`); limits; deterministic preview (compute `Map<BlockVector3, BlockState>` once, preview shows it, apply reuses it); messages via `Messages`/`tr` into lang en/nl (sorted, placeholders preserved) using `/tmp/addlang.py`; tests: `ShadowPlannerTest` (pure: down onto ground, never overwrites build, horizontal projection, dist limit, dedupe) + `ShadowCommandTest` (FakeMessages: usage on empty, parse error on bad dir); README section + module-table row + permission bullet; no new deps; no code comments beyond javadoc class headers matching sibling modules.

## Tasks (all done in commits on branch vibe/arceon-parity-spec-88e6d4)
1. Plan + ledger, engine `ShadowOptions`/`ShadowSpec`/`ShadowPlanner` + `ShadowPlannerTest` — commit.
2. `ShadowCommand` + `ShadowModule` + wiring (`GZMNBuildtools.allModules`, config.yml, paper-plugin.yml) + `ShadowCommandTest` — commit.
3. lang en/nl + README + push, CI green — commit.

## Review Focus
- Engine never targets a build cell; horizontal sources are the exposed face only; dist bound respected; y clamped to world min/max.
- Command: selection mode scans non-air cells inside the cuboid via `ExtentBlockSource`; build mode reuses `BuildScan.contains`; preview deterministic.
- Catalogue test: en.yml exactly equals generated catalogue; nl.yml covers all keys with same placeholders.
