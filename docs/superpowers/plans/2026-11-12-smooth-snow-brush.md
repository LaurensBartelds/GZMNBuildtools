# Smooth Snow Brush Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the `snow` terrain brush (`/tb snow <radius> [strength=1] [fill=0]`) to the existing `terrain-brushes` module: it averages `snow_layer` thickness (`layers` 1–8) over the brush disc and moves each column towards the average; `fill=1` also lays snow on exposed ground and clears snow that averages out to nothing — spec §8, Phase 3 feature 2.

**Architecture:** Pure engine `modules/terrainbrush/engine/SnowSmooth` — `plan(cx, cz, radius, ColumnTop top, ColumnSnow snow, strength, fill, minY, maxY)` → `Plan(Map<Cell,Integer> newLayers, Set<Cell> remove, int blockCount)`; column-based oracles keep it free of Bukkit/WorldEdit types. FAWE brush `modules/terrainbrush/fawe/SnowBrush` wires it to an `EditSession` (top solid via `ExtentTerrainView`, layers via the `layers` property of `snow_layer`, writes `BlockTypes.SNOW_LAYER` / air). Enum constant `SNOW` in `TerrainBrushType` (no pattern slots; params `strength`, `fill`), factory case in `TerrainBrushFactory`, menu icon case in `TerrainBrushMenu` (`Material.SNOW`). No new module, no new permission (`gzmnbuildtools.terrainbrush`).

**Tech Stack / Spec:** Java 25, Paper 26.2, FAWE 2.16.1, `mvn -B verify` = CI (no local JDK), JUnit 5 + Mockito. Spec: `docs/arceon-parity-spec.md` §8.

## Global Constraints
Brush param descriptions are plain strings (not `tr()` catalogue entries) — no lang changes needed. `TerrainBrushCommand.list/help` iterate the enum automatically; the menu switch is exhaustive so it must gain a `SNOW` case. Radius limit already enforced (`terrain-brushes.max-radius`). Tests: `SnowSmoothTest` (pure: averaging, spike reduction, removal at 0, fill on bare ground, world-edge skip). README terrain-brush list + commit per phase. No new deps.

## Tasks (all done in commits on branch vibe/arceon-parity-spec-88e6d4)
1. Plan + ledger, engine `SnowSmooth` + `SnowSmoothTest` — commit.
2. `SNOW` enum constant + `SnowBrush` + factory/menu cases — commit.
3. README + push, CI green — commit.

## Review Focus
- `layers` clamped 1..8 on write, 0 = remove; columns whose top is at maxY are skipped; average includes the center column; `fill` only adds where average >= 0.5.
- Brush reads through the EditSession so `//undo` and masks keep working; pattern argument unused (SNOW has no fill slot).
