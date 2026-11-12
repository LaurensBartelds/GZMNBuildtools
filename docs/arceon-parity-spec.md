# Spec: Arceon parity — implementing the missing tools

Goal: make GZMNBuildtools a free, open-source replacement for **Arceon** (Arcaniax's
premium building-tools plugin), so builders can switch without losing capability.

Arceon's tool list (per its GitBook wiki): Arch, Color Replace, Resize, Rope, Shadow,
Smooth Snow, Smear, Roof, Spike, Road, Loft, Terragen, Revolve, Dashes, River, Text,
Type Replace, plus masks/patterns and brushes (boulder, crop, ...).

## Already covered

| Arceon feature | GZMNBuildtools |
|---|---|
| Type Replace | `/typereplace` |
| Color Replace | `/gradientbrush` (`/gb`), `/gzswap` |
| Arch | `/gzgen arch` |
| Rope | `/gzgen cable` (sagging) |
| Spike | `/tb spike` |
| Boulder | `/tb rock`, `/gzgen rock` |
| River | `/gzspline build river` |
| Road | `/gzspline build road` |
| Terragen | `/gztexture`, `/gzscatter`, terrain brushes (partial) |
| Masks & patterns | `#slope`, `#curvature`, `#depth`, `#height`, `#sky`, `#facing`, `#near`, `#checker`, `#byslope`, `#bydepth`, `#byheight`, `#stripes`, `#gradient` |
| Text (partial) | `/gzpixelart` (image-based) |

## Missing features to implement

Phases:

- **Phase 1 — headline tools:** 1. Roof, 2. Loft
- **Phase 2 — transforms & painting:** 3. Revolve, 4. Resize, 5. Smear, 6. Text
- **Phase 3 — finishing touches:** 7. Shadow, 8. Smooth Snow, 9. Dashes, 10. Fix-connect, 11. `//` aliases

## Implementation status (PR #5, branch `vibe/arceon-parity-spec-88e6d4`)

All 11 features implemented; CI green (`mvn -B verify`, 248 tests).

| # | Feature | Command | Module | Plan |
|---|---------|---------|--------|------|
| 1 | Roof | `/gzroof` | `roof` | `docs/superpowers/plans/2026-11-12-roof-tool.md` |
| 2 | Loft | `/gzloft` | `loft` | `docs/superpowers/plans/2026-11-12-loft-tool.md` |
| 3 | Revolve | `/gzrevolve` | `revolve` | `docs/superpowers/plans/2026-11-12-revolve-tool.md` |
| 4 | Resize | `/gzresize` | `resize` | `docs/superpowers/plans/2026-11-12-resize-tool.md` |
| 5 | Smear | `/gzsmear` | `smear` | `docs/superpowers/plans/2026-11-12-smear-tool.md` |
| 6 | Text | `/gztext`, `/gzfont` | `pixel-art` | `docs/superpowers/plans/2026-11-12-text-tool.md` |
| 7 | Shadow | `/gzshadow` | `shadow` | `docs/superpowers/plans/2026-11-12-shadow-tool.md` |
| 8 | Smooth Snow | `/tb snow` | `terrain-brushes` | `docs/superpowers/plans/2026-11-12-smooth-snow-brush.md` |
| 9 | Dashes | `/gzspline build dashes` | `splines` | `docs/superpowers/plans/2026-11-12-dashes-spline.md` |
| 10 | Fix-connect | `/gzfix` | `fix-connect` | `docs/superpowers/plans/2026-11-12-fix-connect.md` |
| 11 | `//` aliases | `//roof` `//loft` `//revolve` `//resize` `//shadow` `//smear` `//fc` | — (registered in FAWE's command manager via `shared/worldedit/WeAliases`) | `docs/superpowers/plans/2026-11-12-double-slash-aliases.md` |

SDD ledgers: `.superpowers/sdd/2026-11-12-*/progress.md`.

---

## 1. Roof tool (Phase 1)

Arceon: `//roof <width> <height>` on a convex/cuboid selection; flags `-n` (noise),
`-d` (stack down), `-b` (level at ends).

**Command:** `/gzroof <pattern> [style=gable] [height=auto] [overhang=1] [axis=auto]
[noise=0] [stack-down=0] [-p]` (alias `/roof`), plus `/gzroof paint <pattern> ...` and
`/gzroof preview <pattern> ...` operating on the build you look at (same pattern as
`/weathering paint` / `/gb paint`). With no selection and no `paint`/`preview`, use the
current WorldEdit selection.

**Input:** WorldEdit cuboid/convex selection, or the connected build found by
`shared/worldedit/BuildEditor.scan(...)` (as `WeatheringCommand` does).

**Algorithm (pure engine, `modules/roof/engine/RoofPlanner.java`):**
1. For each (x,z) column of the selection/build bounds, find the topmost build member
   (`BuildScan`) → rim height map. The roof sits on the rim ring.
2. `axis=auto` picks the longer horizontal axis of the bounds; ridge runs along it.
3. Styles: `gable` (ridge along the axis, slopes on the two short sides, triangular
   ends), `hip` (all four sides slope up to the ridge), `pyramid` (square bounds →
   point), `flat` (one slab layer on the rim).
4. Slope rows are placed as stairs (resolved from the pattern through
   `shared/build/VariantResolver` / `shared/worldedit/StateMapper`, so `stone_bricks`
   becomes `stone_brick_stairs` with correct `facing`), ridge capped with slabs
   (`layers`/`type=top` handled by StateMapper), `overhang` extends past the wall line.
5. `noise` jitters the ridge height per column (seeded, so `-p` previews match the
   applied result — see `GenerateCommand` seed handling). `stack-down` fills the
   interior below the roof down to the wall top with the pattern.
6. Result is a `Map<Position, BlockData>` change set; count is checked against
   `Limits.Kind.BLOCKS` before applying; `-p` shows it via `PreviewService`.

**Wiring:** module id `roof`, permission `gzmnbuildtools.roof`, help category
`HelpEntry.Category.BUILDS`, config `modules.roof: true`.

**Files:** `modules/roof/RoofModule.java`, `command/RoofCommand.java`,
`engine/RoofPlanner.java`, `engine/RoofOptions.java`; test `RoofPlannerTest`
(pure: given a rim map and options, assert the change set — gable/hip/pyramid/flat,
overhang, noise determinism, block counting).

## 2. Loft tool (Phase 1)

Arceon: loft tool with paper wand; `//loft point|frame|remove|clear`; flags `-p`
(low poly), `-o` (outline only), `-c` (smooth connect), `-d` (stack down to floor).

**Commands:** `/gzloft add [w] [h]`, `/gzloft here [w] [h]`, `/gzloft frame <w> <h>`
(set frame of last point), `/gzloft remove`, `/gzloft clear`, `/gzloft list`,
`/gzloft show`, then `/gzloft build <pattern> [options] [-p]`. Point management mirrors
`/gzspline` (particles while editing).

**Algorithm (`modules/loft/engine/LoftPlanner.java`, pure):**
1. Points + per-point frame (width w, height h) define stations along a Catmull-Rom
   curve (`shared/shapes/Splines.java`).
2. Each station gets an elliptical cross-section (w × h). Sample the curve at N
   stations; for each, place the section's surface blocks of `pattern` (outline only
   with `outline=1`, filled otherwise). `smooth=1` interpolates w/h between frames;
   `lowpoly=1` reduces N and section segments; `stack-down=1` extrudes each station
   down to the ground.
3. Change set → `Limits.Kind.BLOCKS` check → `-p` preview via `PreviewService` → apply
   through FAWE `EditSession` so `//undo` works.

**Wiring:** module id `loft`, permission `gzmnbuildtools.loft`, category `SHAPES`.

## 3. Revolve tool (Phase 2)

Arceon: Revolve-Tool on a cuboid selection — an advanced `//rotate`.

**Command:** `/gzrevolve [degrees=360] [steps=16] [axis=y] [pattern] [-p]` on the
current cuboid selection. Axis passes through selection point 1 (or `axis=x/y/z` with
`at=<x,y,z>`).

**Algorithm:** copy the selection around the axis in `steps` increments of
`degrees/steps`, rotating each block state (`facing`, log `axis`, etc.) with
`StateMapper`; optional `pattern` remaps materials through `VariantResolver`. Output
block count = selection size × steps, checked against `Limits.Kind.BLOCKS`.

Module `revolve`, permission `gzmnbuildtools.revolve`, category `SHAPES`.

## 4. Resize tool (Phase 2)

**Command:** `/gzresize <scale>` or `/gzresize <sx>,<sy>,<sz>` on the current selection
(`scale` 0.5–4). Nearest-neighbor resample of the selection into the resized box anchored
at point 1; directional states preserved via `StateMapper` for uniform scales, degrading
to plain blocks otherwise. Output count checked against `Limits.Kind.BLOCKS`; `-p`
preview; `//undo` reverts.

Module `resize`, permission `gzmnbuildtools.resize`, category `BUILDS`.

## 5. Smear tool (Phase 2)

Arceon: Smear tool drags a shape along a direction; negative amount drags back; works
with `//gsmask`.

**Command:** `/gzsmear [amount=2] [direction=stroke]` binds a FAWE brush
(`shared/worldedit/FaweBrushBinder`, `LimitedBrush`, `PreviewableBrush` like
`/weathering`). On each stroke, the blocks under the brush are sampled and extended
along the stroke direction by `amount` blocks; negative `amount` pulls back. The
brush's `//mask` limits which blocks are smeared.

Module `smear`, permission `gzmnbuildtools.smear`, category `BRUSHES`.
Engine `modules/smear/engine/SmearEngine.java` pure-testable (given a block grid and a
stroke vector, produce the smeared grid).

## 6. Text tool (Phase 2)

Arceon: `//font [font]`, `//text <block> <size> <text>`; unicode font option; quartz
block uses slabs for extra detail; `-c` cycles fonts.

**Commands (inside the existing `pixel-art` module):** `/gztext <block> <size> <text>`
and `/gzfont <font> [--cycle]` (alias `/text`, `/font`). Renders text with a bundled
5×7 bitmap font (plus `unicode`) onto the wall the player looks at, scaled by `size`;
when the block has slabs, half-height rows use slabs (two rows per block row).
Placement goes through the same safe placement path as `/gzpixelart` (`-p` preview,
`//undo`, `Limits.Kind.IMAGE` for the text bounds).

Module `pixel-art` (extend), permission `gzmnbuildtools.pixelart`, category `OTHER`.

## 7. Shadow tool (Phase 3)

Arceon: `//shadow <block>` creates a physical shadow of the build.

**Command:** `/gzshadow <block> [dir=down]` on the build you look at
(`BuildEditor.scan`). For each top-exposed build block, project along `dir` to the first
surface below and place the shadow block there (never overwriting build blocks).
`Limits.Kind.BLOCKS` + `-p` preview.

Module `shadow`, permission `gzmnbuildtools.shadow`, category `BUILDS`.

## 8. Smooth Snow (Phase 3)

Arceon: Smooth Snow tool.

**Implementation:** new terrain-brush type `snow` inside the existing `terrain-brushes`
module (`/tb snow <radius>`): averages `snow_layer` thickness (`layers` property 1–8)
with neighbors inside the radius and optionally places/removes snow on exposed ground.
No new module; permission `gzmnbuildtools.terrainbrush`.

## 9. Dashes (Phase 3)

Arceon: Dashes tool (dashed lines, e.g. road markings).

**Implementation:** new spline shape inside the existing `splines` module:
`/gzspline build dashes <pattern> [dash=3] [gap=3] [width=1]` — places `pattern` in
dashes of `dash` blocks with `gap` air between, following the curve on the ground.
No new module; permission `gzmnbuildtools.generate`.

## 10. Fix-connect (Phase 3)

Arceon: `//fc` instantly fix-connects disconnected walls/fences.

**Command:** `/gzfix` (optional alias `//fc`) on the current selection. For every
fence, wall, iron bars, glass pane, etc., set the `north/east/south/west` connection
properties to match adjacent connectable blocks of the same family; preserve
`waterlogged` and other states via `shared/worldedit/StateMapper` /
`shared/build/BlockRules`. Reports the changed count; `//undo` reverts.

Module `fix-connect`, permission `gzmnbuildtools.fixconnect`, category `BUILDS`.

## 11. `//` command aliases (Phase 3, needs a small spike)

Arceon commands are WorldEdit-style (`//roof`, `//river`, `//loft`, `//fc`, ...). To let
switching builders keep their muscle memory, register thin aliases for the new tools
with WorldEdit/FAWE's command system (`com.sk89q.worldedit.command.CommandManager` —
spike required to confirm the registration path on Paper 26.2 with FAWE; the brush
tools already integrate through FAWE's brush system). The `/gz*` commands stay
canonical; aliases just forward to the same command classes.

---

## Cross-cutting requirements (every feature)

1. **Module:** new package `modules/<id>` with `<Id>Module implements GzmnModule`
   (`core/module/GzmnModule.java`), registered in `GZMNBuildtools.allModules()`
   (`GZMNBuildtools.java:102-116`), `modules.<id>: true` in `src/main/resources/config.yml`.
   `ModuleBoundariesTest` (ArchUnit) enforces: kernel never depends on modules; modules
   only touch declared dependencies, and only via their top-level API package.
2. **Command:** Brigadier via `ModuleContext.commands(...)`, player-only with
   `.requires(... hasPermission(PERMISSION))`, short alias in `commands.register(...,
   description, List.of(aliases))` — see `WeatheringModule`.
3. **Permission:** `gzmnbuildtools.<id>` in `src/main/resources/paper-plugin.yml`
   (default op, child of `gzmnbuildtools.*`) + README permissions table.
4. **Help hub:** `context.help(HelpEntry.of(Category.X, icon, "/gz...", tr("..."),
   tr("...")).permission(...).runs())` — makes the feature appear in `/gz`, `/gz list`
   and `/gz <word>` search.
5. **Limits:** generation tools check `Limits.get(player, Limits.Kind.BLOCKS,
   config "generation.max-blocks")` (reference: `GenerateCommand.maxBlocks`); brushes use
   `Limits.Kind.RADIUS` via `FaweBrushBinder`. Rank limits keep working through the
   existing `limits:` config.
6. **Preview & undo:** `-p`/`preview` flag → deterministic (seeded) change map →
   `PreviewService.show(player, ghost, label, apply)`; apply writes through a FAWE
   `EditSession` so `//undo` reverts. Reference: `GenerateCommand` (lines ~94-191).
7. **i18n:** all player-facing strings via `tr(...)` / `Messages`; regenerate
   `lang/en.yml` with `mvn test -Dcatalogue.write=true`, add Dutch in `lang/nl.yml`;
   `MessageCatalogueTest` fails the build otherwise.
8. **Tests:** pure engine unit tests under `src/test/.../modules/<id>/engine/` (no
   Bukkit — see `shared/shapes` and `modules/*/engine` tests); command tests with
   MockBukkit (`MockBukkitIntegrationTest`). CI runs `mvn verify`
   (`.github/workflows/ci.yml`).
9. **Docs:** README section per feature, a row in the modules table, a row in the
   permissions table.

## Definition of done per feature

Command works in-game with permission + alias; `-p` preview matches the applied result;
`//undo` reverts; block/radius limits enforced and shown by `/gzlimits`; entry in `/gz`
hub; messages in `en` + `nl`; engine unit tests + command test; README updated;
`mvn verify` green.
