# Dashes Spline Shape Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the `dashes` spline shape — `/gzspline build dashes <pattern> [dash=3] [gap=3] [width=1]` — a broken line along the curve, on for `dash` blocks of arc length then off for `gap` — spec §9, Phase 3 feature 3. Reuses permission `gzmnbuildtools.generate`, limits and `-p` preview of `/gzspline build`.

**Architecture:** `Splines.dashes(List<double[]> samples, int dash, int gap, int width, BlockSink sink)` in `shared/shapes/Splines.java`: walks the samples accumulating arc length, emits an across-line (same perpendicular stamping as `road`, refactored into `acrossAt(samples, i, width, visit)`) while `along % (dash+gap) < dash`, deduped. New enum constant `DASHES` in `SplineType` with params `dash` (1–64), `gap` (0–64), `width` (1–16) and a `case DASHES` in `build(...)` (part 0, like tube/wall). `SplineCommand`'s unknown-shape message gains `dashes` (en/nl catalogue update). No new module/permission.

**Tech Stack / Spec:** Java 25, Paper 26.2, FAWE 2.16.1, `mvn -B verify` = CI (no local JDK), JUnit 5. Spec: `docs/arceon-parity-spec.md` §9.

## Global Constraints
Same as the snow plan: shared/shapes is engine code with no Bukkit types; command help lists enum values automatically; the one catalogue string changed in both lang files via `/tmp/addlang.py`; tests in `SplinesTest` (dash/gap pattern on a straight line, width stamping, dedupe at corners); README table row + example; no new deps.

## Tasks (all done in commits on branch vibe/arceon-parity-spec-88e6d4)
1. Plan + ledger, `Splines.dashes` + `SplinesTest` cases — commit.
2. `SplineType.DASHES` + factory case + command message + lang en/nl — commit.
3. README + push, CI green — commit.

## Review Focus
- Arc length measured between consecutive samples, so dash/gap are in blocks along the curve, not sample counts; `gap=0` = solid line; overlapping stamps at sharp corners deduped via a placed-set.
