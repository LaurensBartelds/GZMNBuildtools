# Smear Tool Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `/gzsmear` (module `smear`): a bound FAWE brush that smears the leading surface of the brushed area along the stroke direction; negative amounts drag back — spec §5, Phase 2 feature 3.

**Architecture:** Pure engine `modules/smear/engine/SmearEngine.smear(BlockGrid, center, radius, dir, amount)` groups sphere cells into lines parallel to `dir`, takes each line's leading cell, and copies it `amount` blocks along `dir` (negative: backwards). `SmearBrush` (PreviewableBrush, holds the player for `direction=stroke`) applies the changes via the EditSession; `SmearCommand` parses `amount=`/`direction=` and binds via `FaweBrushBinder` with `Limits.Kind.RADIUS` (`smear.max-radius`, default 16).

**Tech Stack / Spec:** Java 25, Paper 26.2, FAWE 2.16.1, `mvn -B verify` = CI, JUnit 5 + Mockito. Spec: `docs/arceon-parity-spec.md` §5.

## Global Constraints
Same as previous plans: module `modules/smear`, no module deps; permission `gzmnbuildtools.smear` (op default, child of `gzmnbuildtools.*`); `modules.smear: true` + `smear.max-radius` in config.yml; help entry (BRUSHES, `Material.CLAY_BALL`); messages into lang en/nl sorted; tests: `SmearEngineTest` (pure) + `SmearCommandTest` (FakeMessages); README section + module row + permission bullet; no new deps.

## Tasks (done in the commits on this branch)
1. `SmearEngine` + `SmearEngineTest` (forward, negative drag-back, negative axis, vertical).
2. `SmearBrush` + `SmearCommand` + `SmearModule` + wiring + `SmearCommandTest`.
3. lang en/nl + README + CI green.
