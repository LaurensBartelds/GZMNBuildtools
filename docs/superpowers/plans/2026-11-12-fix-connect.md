# Fix-Connect Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `/gzfix [-p]` (module `fix-connect`): on the cuboid selection, set the `north/east/south/west` connection properties of every fence, wall, pane and iron bars to match adjacent connectable blocks (same family, or a solid full cube), preserving all other states — spec §10, Phase 3 feature 4.

**Architecture:** Pure engine `modules/fixconnect/engine/FixConnectPlanner` — `plan(Bounds, BlockOracle, Predicate<String> fullCube, ConnectionOracle current)` → `Plan(Map<Cell, boolean[4]> changes, int blockCount, String problem)`; `familyOf(String id)` public static (fence/wall/pane/iron_bars, fence gates excluded). `modules/fixconnect/command/FixConnectCommand` requires a `CuboidRegion`, reads blocks through the `EditSession` (`ExtentBlockSource.idOf`, `getMaterial().isSolid()`, boolean properties read/written by name so `waterlogged` etc. are preserved by only touching the four connection properties), enforces `Limits.Kind.BLOCKS` (`generation.max-blocks`), `-p` preview via `PreviewService`, applies via `PlayerEdits.async` (`//undo`).

**Tech Stack / Spec:** Java 25, Paper 26.2, FAWE 2.16.1, `mvn -B verify` = CI (no local JDK), JUnit 5 + Mockito. Spec: `docs/arceon-parity-spec.md` §10.

## Global Constraints
Same as roof/resize plans: module `modules/fixconnect`, no module dependencies; Brigadier command `gzfix` (alias `fix`), player-only, permission `gzmnbuildtools.fixconnect` (op default, child of `gzmnbuildtools.*`); `modules.fix-connect: true` in config.yml; help entry (BUILDS, `Material.OAK_FENCE`); messages via `Messages`/`tr` into lang en/nl via `/tmp/addlang.py`; tests: `FixConnectPlannerTest` (pure: fence next to solid connects, same-family connects, different family does not, fence gate skipped, no-change yields empty plan, out-of-selection neighbor still counts) + `FixConnectCommandTest` (FakeMessages: no selection error path is hard to mock — test usage on empty args and parse-free paths); README section + module row + permission bullet; no new deps.

## Tasks (all done in commits on branch vibe/arceon-parity-spec-88e6d4)
1. Plan + ledger, engine `FixConnectPlanner` + `FixConnectPlannerTest` — commit.
2. `FixConnectCommand` + `FixConnectModule` + wiring + `FixConnectCommandTest` — commit.
3. lang en/nl + README + push, CI green — commit.

## Review Focus
- Only the four connection booleans are written; `waterlogged` and friends survive because the command starts from the existing `BlockState` and uses `state.with(property, value)`.
- Neighbours outside the selection still count (a fence at the selection edge connects to a wall outside it).
