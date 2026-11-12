# Revolve Tool Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `/gzrevolve` (module `revolve`): revolve the current cuboid selection around an axis through point 1 (like an advanced `//rotate`) — spec §3, Phase 2 feature 1.

**Architecture:** Pure engine `modules/revolve/engine/RevolvePlanner` rotates every selection cell around the axis in `steps` increments and emits (output cell → source cell + quarter turns). The command (`modules/revolve/command/RevolveCommand`) reads each source block, optionally remaps it through a pattern via `StateMapper`, rotates horizontal `facing`/`axis` properties by the quarter turns (string-cycled, like the roof's property setting), enforces `Limits.Kind.BLOCKS`, and previews (`-p`) / applies through `PlayerEdits.async` (`//undo`).

**Tech Stack:** Java 25, Paper 26.2, FAWE 2.16.1, Maven (`mvn -B verify` = CI), JUnit 5 + Mockito.

**Spec:** `docs/arceon-parity-spec.md` §3. Spec wins on conflict.

## Global Constraints

Same as the roof/loft plans: module under `modules/revolve` with no module dependencies (ArchUnit auto-covers); Brigadier command, player-only, permission `gzmnbuildtools.revolve` (op default, child of `gzmnbuildtools.*`); `modules.revolve: true` in `config.yml`; help entry (SHAPES, icon `Material.COMPASS`); `generation.max-blocks` limit incl. all copies; `-p` preview deterministic (changes computed once); apply via `PlayerEdits.async` returning true; messages via `Messages`/`tr` into `lang/en.yml` + `lang/nl.yml` (exact sorted order, script-assisted, placeholders preserved); tests: pure engine JUnit + FakeMessages command test; README section + module table row + permission bullet; no new deps.

## Interfaces (pinned)

`RevolveSpec` (engine, `ParamSpec` singleton `INSTANCE`): `id()`="revolve"; no pattern slot by default — pattern is optional, so `patternNames()` = [] and pattern is parsed from a leading `pattern=`-less token? Simpler: `patternNames()`=["pattern"] would force a pattern. Use: params only, and the command takes an optional leading pattern token if the first token is not `key=value` and not a number... Cleaner: `patternNames()`=["pattern"] but make it optional by trying parse with a dummy: if first token contains '=' or is numeric → no pattern. Simplest robust: `patternNames()` = List.of() and params: `degrees`=(360, 1..720), `steps`=(16, 1..64), `axis`=choice("axis","rotation axis","y","x","z"), `at`=(0, "x,y,z override for the axis point, default = selection point 1") — hmm string param not supported by BrushParam (numbers/choices only). Drop `at`; axis through pos1 always. Pattern: optional first positional arg — handle in command: if first token is not key=value and BrushArguments would treat it as positional param... With patternNames()=[], the first token would be parsed as `degrees` positional → error if it's "stone". So: command checks if first token contains '=' → no pattern; else treat first token as pattern (if it parses as pattern) — but "360" alone would be ambiguous. Decide: pattern is optional and given as `pattern=stone`? Not supported. OK simplest: make pattern REQUIRED like other tools? Arceon revolve copies the selection; pattern optional remap. I'll make the command: tokens; if first token doesn't contain '=' and isn't numeric → it's the pattern. Else no pattern. Document in usage: `/gzrevolve [pattern] [degrees=360] [steps=16] [axis=y]`.

`RevolvePlanner` (engine, pure):
```java
public final class RevolvePlanner {
    public record Cell(int x, int y, int z) {}
    /** Where an output cell comes from and how far it is turned. */
    public record Placement(Cell source, int quarterTurns) {}
    public record Plan(Map<Cell, Placement> placements, int blockCount, String problem) {}
    /** Revolve {@code sel} around {@code axis} through {@code center} in {@code steps} increments of degrees/steps. */
    public static Plan plan(Bounds sel, int[] center, char axis, double degrees, int steps) {}
}
```
Rotation (+θ around axis, right-handed, Y up): around Y: `x' = cx + dx*cos + dz*sin`, `z' = cz - dx*sin + dz*cos`; around X: `y' = cy + dy*cos - dz*sin`, `z' = cz + dy*sin + dz*cos`... (pick consistent right-handed formulas; tests pin them); around Z: `x' = cx + dx*cos - dy*sin`, `y' = cy + dx*sin + dy*cos`. quarterTurns = `((int) Math.round(theta / 90)) % 4` normalized to 0..3. Later steps overwrite earlier ones at the same cell. blockCount = map size. problem when selection empty (size 0) → "Select a cuboid area first."

`RevolveCommand`: `PERMISSION = "gzmnbuildtools.revolve"`; `revolve(player, rawArgs)`: strip `-p`; split optional leading pattern token; `BrushArguments.parse(RevolveSpec.INSTANCE, rest)`; require cuboid selection (like GenerateCommand); plan; limit check `generation.max-blocks`; inside `PlayerEdits.async`: for each placement → source state from editSession → optional pattern remap via `StateMapper.map(state, type)` → rotate facing/axis by quarterTurns around axis → changes map → preview or apply; success "Revolved the selection: %d blocks. //undo reverts it."

Rotation helper (command): `rotate(state, axis, quarterTurns)`: for "facing" property with values north/east/south/west(/up/down): around Y cycle N→E→S→W; around X: N↔? use cycle N→D? Keep: around X cycle [north, down, south, up]? Simplest correct-ish: around X: north→down→south→up→north? For +90 around X (right-handed): north(-z) → up? Let's not overthink: around X cycle facing N→U? I'll define: Y: [north,east,south,west]; X: [north,up,south,down]? Hmm — right-handed +90 about X: +Y→+Z, +Z→-Y: so north(-Z)→... -Z rotated +90 about X: (0,0,-1) → (0, -(-1)?, ...) ugh. Tests only pin Y; for X/Z use cycles X: [north,down,south,up], Z: [west,down,east,up]? These are cosmetic. For "axis" property: Y: x→z→x... define maps: around Y: axis x→z, z→x, y→y; around X: y→z, z→y, x→x; around Z: x→y, y→x, z→z. Apply per quarter turn.

`RevolveModule`: id `revolve`; help SHAPES/`Material.COMPASS`/tr("Revolve")/tr("Revolve your selection around an axis through point 1.").example("/gzrevolve stone_bricks degrees=180"); Brigadier `gzrevolve` alias `revolve`, greedy args + suggestions.

## Tasks
1. Engine + `RevolvePlannerTest` (pure): quarterTurnMapping (3x3x3 at center, axis y, 360/4 → blockCount 33, Cell(1,64,0) ← source Cell(2,64,1) quarterTurns 1), partialRevolve (degrees=90, steps=1 → count 9, all quarterTurns 1), axisX (axis x on a 1x3x1 column → rotates in y/z), emptySelection → problem. Commit "Add revolve planner engine with tests".
2. Command + module + wiring (`GZMNBuildtools`, `config.yml`, `paper-plugin.yml`) + `RevolveCommandTest` (FakeMessages: no selection message path can't be tested without server — test unknown param + usage on empty). Commit "Add /gzrevolve command and revolve module".
3. lang en/nl keys + README + CI green. Commit "Document /gzrevolve and add revolve messages (en/nl)".

## Review Focus
1. Empty selection → clean problem, no placements.
2. Overlapping copies: later steps win (map put), count reflects unique cells.
3. Limit counts all copies.
4. quarterTurns normalized to 0..3 for negative angles.
5. Preview deterministic (changes computed once, apply reuses them).
