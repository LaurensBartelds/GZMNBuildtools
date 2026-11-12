# GZMNBuildtools

A FastAsyncWorldEdit extension for Minecraft 26.2: terrain masks and patterns, terrain sculpting brushes, shape
generators, gradients and type replacement.

## Requirements

- Paper 26.2 (Minecraft 26.2)
- Java 25
- FastAsyncWorldEdit 2.15.3+ (recommended) or WorldEdit 7.4+

## Installation

1. Place the jar file in your server's plugins folder.
2. Ensure you have WorldEdit or FAWE installed.
3. Restart the server.

## Usage

Not sure where to start? Run `/gz` (aliases `/gzhelp`, `/gzmn`). It opens a menu of everything GZMNBuildtools can do
that you have permission for, grouped into brushes, terrain, shapes, builds and other tools. Click a feature to open
it, or to get a ready-to-edit example in chat. `/gz list` shows the same as a clickable chat list, and `/gz <word>`
searches it (for example `/gz terrain` or `/gz river`).

### Type Replace

The Type Replace tool allows replacing entire families of blocks (stairs, slabs, walls, fences) while preserving their
orientation and properties where possible.

Command: `/typereplace <from> <to>`

This command operates on your current WorldEdit selection.

Arguments:

- `from`: The source material name or group to replace (e.g., `stone`, `copper`).
- `to`: The target material family to replace with (e.g., `andesite`, `waxed_exposed_copper`).

Supported material groups include `all_copper`, `all_waxed_copper`, and `copper_all`.

Example:
`/typereplace oak spruce`
This will replace oak stairs with spruce stairs, oak slabs with spruce slabs, etc., inside the selection.

### Gradient

Command: `/gradient <blocks> <direction> [mode]`

Applies a gradient across your current WorldEdit selection.

- `blocks`: One or more block layers, e.g. `[stone]` or `[cobblestone,stone]`.
- `direction`: `up`, `down`, `x`, `z`, `radial` (or the long forms `VERTICAL_UP`, etc.).
- `mode` (optional): `LINEAR`, `SMOOTH`, `DISCRETE`, or `BLENDED`.

Example: `/gradient [stone][cobblestone,stone] up blended`

Subcommands: `save`, `use`, `list`, `delete`, `share`, and `preset`. Saved gradients can be kept private or shared
publicly. Presets are listed with `/gradient preset <id> <direction>`.

You can also use the gradient as a WorldEdit pattern: `#gradient[up][linear][stone,andesite,deepslate]`.

### Masks

These work anywhere FAWE accepts a mask (`//replace`, `//gmask`, `//mask`, brush masks, `//count`, ...). Arguments in
`[]` are optional where a default is shown. Combine them with FAWE's `&` (and), `,` (or) and `!` (not).

| Mask | Matches |
|------|---------|
| `#slope[min][max][radius=2]` | Solid blocks where the terrain slope is between `min` and `max` degrees |
| `#curvature[min][max][radius=3]` | Solid blocks by terrain curvature (positive = concave/valley, negative = convex/ridge) |
| `#concave[threshold=0.3][radius=3]` | Valleys, hollows and inner corners |
| `#convex[threshold=0.3][radius=3]` | Ridges, peaks and outer edges |
| `#depth[min][max]` | Solid blocks with `min`..`max` solid blocks directly above (0 = the surface block) |
| `#height[minY][maxY]` | Any block with Y in the range |
| `#sky` | Blocks with nothing solid above them |
| `#facing[up,north,...]` | Solid blocks exposed to air on any of the given faces |
| `#near[mask][radius]` | Blocks within `radius` of a block matching `mask` (e.g. `#near[water][4]` for shorelines) |
| `#checker[size=1]` | A 3D checkerboard |

Terrain masks look at the terrain around the column, so interior blocks of a slope also match. To affect only the
skin, combine them with a surface mask: `//replace #slope[35][90]&#facing[up] stone`.

### Patterns

| Pattern | Places |
|---------|--------|
| `#byslope[0:grass_block][35:stone][60:andesite][radius=2]` | A pattern per slope band (degrees) |
| `#bydepth[0:grass_block][1:dirt][4:stone]` | Layers by depth below the surface |
| `#byheight[60:sand][66:grass_block][160:snow_block][jitter=4][seed=1]` | Bands by Y level, with optional noisy edges |
| `#checker[size][pattern1][pattern2]...` | A 3D checkerboard cycling through the patterns |
| `#stripes[x\|y\|z\|xz\|-xz][width][pattern1][pattern2]...` | Repeating stripes along an axis or diagonal |

Every `pattern` slot accepts any FAWE pattern, including random mixes and other GZMN patterns, e.g.
`#bydepth[0:#byslope[0:grass_block][40:stone]][1:50%dirt,50%coarse_dirt][4:stone]`.

### Terrain brushes

`/terrainbrush <type> <radius> [pattern] [parameters]` (aliases `/tbrush`, `/tb`) binds a terrain brush to the item in
your hand. `/tb` on its own opens a menu: pick a brush, adjust its radius and parameters with left/right clicks
(shift for bigger steps), click blocks in your inventory to choose its patterns, then press Bind. `/tb list` lists
the brushes in chat. It is a normal FAWE brush, so `/size`, `//mask`, `/mat`, scroll actions, `/none` and visualisation all
work. Parameters can be given in order or as `name=value`. Run `/tb list` for the list or `/tb <type>` for the details of
one brush.

| Brush | Does | Parameters (defaults) |
|-------|------|-----------------------|
| `raise` | Raises terrain with a smooth falloff | `height=3`, `hardness=0` |
| `lower` | Lowers terrain with a smooth falloff | `depth=3`, `hardness=0` |
| `flatten` | Pulls terrain to the height of the clicked column | `strength=1`, `hardness=0.5` |
| `smooth` | Gaussian heightmap smoothing | `kernel=2`, `iterations=2`, `strength=1` |
| `terrace` | Cuts terrain into flat steps | `step=3`, `offset=0`, `hardness=0.4` |
| `roughen` | Adds natural noise to the terrain height | `amplitude=2`, `scale=6` |
| `overlay` | Paints the top layers of every upward-facing surface | pattern, `depth=1` |
| `blob` | Places an organic, noisy blob | pattern, `roughness=0.4`, `frequency=1` |
| `spike` | Places a tapering spike or stalactite | pattern, `height=12`, `sharpness=1.5`, `tilt=0`, `down=0` |
| `tree` | Plants a tree, or a forest with `count` > 1 | log and leaves patterns, `style=oak`, `height=10`, `thickness=1`, `leaves=3`, `count=1` |
| `rock` | Places boulders embedded in the ground | pattern, `flatness=0.35`, `roughness=0.35`, `sink=0.3`, `count=1` |

The reshaping brushes (`raise` … `roughen`) move whole columns: the top four surface layers and any plants on top
travel with the surface, so grass stays on top and dirt stays below it.

Examples:

- `/tb smooth 8 3 iterations=4`
- `/tb overlay 6 #byslope[0:grass_block][35:coarse_dirt][55:stone] 2`
- `/tb spike 2 dripstone_block height=10 down=1`
- `/tb tree 12 birch_log birch_leaves style=birch count=6` plants a birch grove with each click
- `/tb rock 4 #byslope[0:mossy_cobblestone][35:stone][60:andesite]`

Tree styles are `oak`, `spruce`, `birch`, `willow`, `dead` and `palm`. Logs follow the direction of trunks and branches,
and leaves are placed persistent so they never decay.

The largest allowed radius is set by `terrain-brushes.max-radius` in `config.yml` (default 32).

After binding a terrain brush, `/tb save <name>` keeps it; `/tb load <name>` binds it to your held item again,
`/tb loadouts` lists them and `/tb delete <name>` removes one. A player can keep up to `loadouts.max-per-player`
(default 30) loadouts per brush.

### Smart gradient brush

`/gradientbrush` (aliases `/gb`, `/gbrush`, `/gzmnbrush`) is a build-aware gradient brush. Bind it with
`/gb <palette> [options]` and click any build:

- **It fits the build you click.** The brush finds the connected structure you clicked and runs the gradient across
  it (bottom to top for walls and towers, along the length for floors and paths), so every stroke on the same build
  lines up, wherever you click.
- **It keeps shapes and states.** Stairs stay stairs, slabs stay slabs, walls and fences stay walls and fences, with
  their facing, half, shape and waterlogging. If a palette block has no stairs (say calcite), the closest matching
  material that does is used (quartz stairs).
- **It protects details.** Glass, lights, doors, containers, plants and natural terrain are never painted. With the
  default `target=clicked` only the material you clicked changes (including its mossy/cracked/chiseled forms);
  `target=main` repaints the build's main materials and leaves accents such as wood trim alone.
- **It generates palettes.** `auto` builds an aged palette from the build's own material (mossy and cracked at the
  base, lighter at the top). Colours come from the game's real block textures, and ramps stay in one material family.

Palettes:

| Palette | Means |
|---------|-------|
| `auto` | Aged ramp of the clicked build's main material |
| `aged:stone_bricks` | Mossy/cracked forms at the bottom, then lighter shades |
| `shades:oak_planks` | Darker to lighter blocks of the same hue |
| `sandstone..deepslate_tiles` | An even colour ramp between two blocks |
| `preset:<id>` | A gradient preset from `presets.yml` |
| `stone,andesite,#3a3a3a` | Explicit blocks; `#RRGGBB` picks the closest block |
| `[50%stone,50%andesite][deepslate]` | Weighted mixes per step |

Options (`key=value`, after the palette or with `/gb set`):

| Option | Values (default first) |
|--------|------------------------|
| `axis` | `auto`, `up`, `down`, `x`, `-x`, `z`, `-z`, `radial`, `inward` |
| `target` | `clicked`, `main`, `structural`, `all` |
| `dither` | `ordered` (even, grain-free), `noise`, `none` (clean bands) |
| `softness` | `0.6`; 0 = hard bands, 1 = fully blended |
| `steps` | `auto`; number of palette steps (fills in between explicit blocks) |
| `shapes` | `keep`, `full` |
| `range` | `build`, or fixed world coordinates such as `60..120` |
| `radius` | `4`; the brush radius (`/size` works too) |

Run `/gb` without arguments to open the brush menu. It shows the palette and has a button for every option. To base
a palette on a block, click it in your own inventory; shift-click blocks to build a palette in order, and the Steps
button fills in blocks between them by colour. Bind, Paint, Preview, Inspect and Reset are buttons too.

Your settings are saved, so the brush is set up the same way after a restart. Keep favourite setups as loadouts:
`/gb save <name>` stores the current brush, `/gb load <name>` brings it back, `/gb loadouts` lists them (click to
load) and `/gb delete <name>` removes one. The menu has a Loadouts page too.

Other commands: `/gb status` shows your settings, `/gb paint [palette] [options]` paints the whole build you look at,
`/gb preview [palette] [options]` shows that paint as ghost blocks first,
`/gb info` inspects it (size, materials, direction, palette), `/gb suggest [block]` offers clickable palettes,
`/gb code <gzmn@...>` imports a web editor code, `/gb rescan` forgets the remembered build and `/gb reset` restores
the defaults. Settings changed with `/gb set` apply to the bound brush immediately.

### Select a build

`/gzselect` (alias `/gsel`) makes your WorldEdit selection the bounding box of the build you are looking at, so
`//copy`, `//replace`, `//stack` and every other selection command work on "this build" without placing corners.
Particles outline what was selected.

- `/gzselect` or `/gzselect build`: the whole structure (building blocks, not the terrain it stands on)
- `/gzselect material`: only the connected blocks of the material you look at
- `/gzselect all`: everything connected, details included
- `/gzselect build 2`: the same, padded by 2 blocks on every side

### Roof

`/gzroof <pattern> [options]` (alias `/roof`) puts a roof on the walls of your current cuboid selection, or on the
build you are looking at when you have no selection (`/gzroof paint <pattern> ...` always uses the looked-at build,
`/gzroof preview <pattern> ...` shows it as ghost blocks first, and `-p` works anywhere too). Every roof can be
undone with `//undo` and counts towards `generation.max-blocks`.

- The roof sits on the top of the walls in each column; holes in the build get no roof, and the eaves extend
  `overhang` blocks past them.
- Slopes are made of the pattern's stairs (facing away from the ridge), the ridge is capped with top slabs, and
  `stack-down=1` fills below the roof down to the walls. If the pattern's material has no stairs or slabs, the
  closest matching material that does is used.

| Style | Shape |
|-------|-------|
| `gable` (default) | Ridge along the longer axis, triangular ends |
| `hip` | All four sides slope up to a shortened ridge |
| `pyramid` | Slopes meet in a point (square buildings) |
| `flat` | One layer of top slabs |

Options (`key=value`, or positionally after the pattern):

| Option | Default | Meaning |
|--------|---------|---------|
| `style` | `gable` | `gable`, `hip`, `pyramid` or `flat` |
| `height` | `0` | Ridge height; 0 = automatic (half the width) |
| `overhang` | `1` | Blocks the eaves extend past the walls |
| `axis` | `auto` | `auto`, `x` or `z`: direction of the ridge |
| `noise` | `0` | Random height jitter on the slopes (0–4) |
| `stack-down` | `0` | 1 = fill below the roof down to the walls |
| `level-ends` | `0` | 1 = flat level at the gable ends |

Example: `/gzroof stone_brick_stairs style=hip height=4 overhang=2 -p` previews a hip roof first.

### Loft

`/gzloft` (alias `/loft`) lofts a smooth organic surface through frames you place — cloth, terrain drapes, curved
roofs. Look at a block and run `/gzloft add [w] [h]` (or `/gzloft here` for the block you stand on) to add a point
with a frame size; `/gzloft frame <w> <h>` resizes the last point, and `remove`, `clear`, `list` and `show` manage
the points (particles draw the curve). Then build along it:

`/gzloft build <pattern> [options] [-p]`

- The curve passes smoothly through every point, and each point's frame (width × height) sets the elliptical
  cross-section there; frames blend between points with `smooth=1`.
- `outline=1` (default) places only the surface ring; `fill=1` makes it solid. `lowpoly=1` uses fewer samples and
  section segments, `stack-down=1` stacks every column down to the ground, `density=3` sets the curve sampling.
- Add `-p` to preview the loft as ghost blocks first. Builds count towards `generation.max-blocks` and are undone
  with `//undo`.

Example: `/gzloft add 6 2`, look elsewhere, `/gzloft add 2 8`, then `/gzloft build white_wool fill=1 -p`.

### Revolve

`/gzrevolve [pattern] [options]` (alias `/revolve`) revolves your current cuboid selection around an axis through
selection point 1 — an advanced `//rotate`. Each cell is copied at every step, with its facing and log axis turned
to match. An optional leading pattern remaps the material (keeping shapes via `StateMapper`).

- `degrees=360` (1–720): the total angle. `steps=16` (1–64): how many copies are placed around the axis.
- `axis=y` (or `x`, `z`): the rotation axis, always through `//pos1`.
- Add `-p` to preview first. Copies count towards `generation.max-blocks` and `//undo` reverts the whole revolve.

Example: `//pos1` and `//pos2` on a pillar, then `/gzrevolve stone_bricks degrees=360 steps=8 -p`.

### Resize

`/gzresize <scale|sx,sy,sz> [pattern] [-p]` (alias `/resize`) scales your current cuboid selection by
nearest-neighbour sampling, anchored at the minimum corner. `scale` is one number (0.25–4) or `sx,sy,sz` per axis;
an optional pattern remaps the material while keeping each block's shape. The result counts towards
`generation.max-blocks`, `-p` previews it, and `//undo` reverts it.

Example: `/gzresize 2` doubles the selection; `/gzresize 2,1,0.5 stone_bricks` flattens it and repaints it.

### Smear

`/gzsmear [amount=2] [direction=stroke]` (alias `/smear`) binds a brush to your held item that smears blocks: the
leading surface under the brush is dragged along the stroke direction. `amount` (1–16) sets how far it extends —
a negative amount drags it back. `direction` is `stroke` (your facing, default) or `north`/`east`/`south`/`west`/
`up`/`down`. The normal brush rules apply (`/size`, `//mask`, `/none`, `/gzhover` previews). The radius is capped by
`smear.max-radius` in `config.yml` (default 16).

Example: `/gzsmear amount=4 direction=east` smears the wall you brush four blocks east.

### Shadow

`/gzshadow <pattern> [dir=down] [dist=32] [-p]` (alias `/shadow`) casts a physical shadow of your selection or the
build you look at: every exposed face block is projected along `dir` (`down`, `north`, `east`, `south`, `west`) onto
the first solid surface within `dist` blocks, and that surface block is repainted with the shadow pattern. Build
blocks are never overwritten. `-p` previews the shadow as ghost blocks first.

Example: `/gzshadow black_concrete` darkens the ground under the build you look at; `/gzshadow deepslate dir=east
dist=16` casts an eastward shadow onto a wall.

### Material swap

`/gzswap <block> [options]` (alias `/swap`) binds a brush that swaps the material of the build you click, keeping its
shape: stone brick stairs become deepslate brick stairs with the same facing, and mossy or cracked blocks become the
new material's mossy or cracked form when it has one. Glass, doors, lights, other materials and terrain are left alone.

- `/gzswap paint <block>` swaps the whole build you look at; `/gzswap preview <block>` shows it first
- `from=<block>`: replace only that material, anywhere in the build (e.g. `/gzswap paint spruce_planks from=oak_planks`)
- `shapes=full` turns stairs and slabs into full blocks; `textures=plain` drops mossy/cracked forms
- `radius=4` sets the brush size

### Weathering

`/weathering [options]` (alias `/age`) binds a brush that ages the build you click: moss creeping up from the ground
in patches, cracks around the middle, corners and rims crumbling to slabs or wearing away (more near the top), and
vines hanging from the walls. Click again for more wear. `/weathering paint` ages a whole build and
`/weathering preview` shows it first.

| Option | Default | Meaning |
|--------|---------|---------|
| `strength` | 0.5 | Overall amount, 0 to 1 |
| `moss`, `cracks`, `decay`, `vines` | 1 | Each effect from 0 (off) to 2 (double) |
| `target` | `build` | `material` only ages the material you click |
| `radius` | 5 | Brush size |

Moss and cracks only appear on materials that have mossy or cracked forms (stone bricks, cobblestone, deepslate,
nether and blackstone bricks); wear and vines work on any building block.

### Pixel art

`/gzpixelart <image> [options]` (alias `/pixelart`) builds a picture from blocks where you are looking. Put images
(`.png`, `.jpg`, `.gif`, `.bmp`) in `plugins/GZMNBuildtools/images/`, or give a direct `https://` link.

- By default the picture stands as a wall facing you, on top of the block you look at; `floor` lays it on the ground
  stretching away from you.
- `width=64` sets the size in blocks; the height follows the image's proportions unless you set `height=`.
- `blocks=` picks the blocks: `all` (default), `wool`, `concrete`, `terracotta`, `flat` (those three), `stone`, `wood`;
  combine with commas.
- Colours are matched by eye (OKLab) with dithering to keep gradients; `dither=off` gives flat areas.
- `map` builds flat map art: north-up on the floor, matched to the colours blocks show on a map (128 wide by default).
- Transparent pixels are skipped. Add `-p` to preview. `//undo` reverts it.

Web downloads are limited for safety: they only go to public addresses (never localhost or private networks), never
follow redirects, and are capped in size (`pixel-art.*` in `config.yml`; `allow-urls: false` turns them off).

Text is built by the same module: `/gztext <block> <size> <text> [-p]` (alias `/text`) renders text with the bundled
5x7 font onto the wall you look at (letters, digits and punctuation; lowercase is uppercased), scaled `size` times
per font pixel (1–16). `/gzfont` (alias `/font`) shows or sets the font, `/gzfont -c` cycles it. The text size counts
towards the `image` limit (`pixel-art.max-size`), `-p` previews it, and `//undo` reverts it.

Example: `/gztext quartz_block 2 HELLO WORLD -p`.

### Terrain texturing

`/gztexture <preset>` (alias `/texture`) repaints the terrain in your WorldEdit selection with a ready-made look:
`alpine`, `desert`, `forest`, `badlands`, `tundra` or `beach`. Run `/gztexture` for the list.

- Each block is chosen by how steep the slope is, its height, how deep it is below the surface and a smooth noise
  value that forms natural patches. For example, alpine puts snow on high ground, stone on cliffs, gravel scree on
  steep slopes and grass with coarse dirt and moss patches elsewhere; badlands stripes its cliffs with terracotta bands.
- Only natural terrain (and blocks the presets themselves use) is repainted, so builds inside the selection stay as
  they are. Ground under trees is textured as well; leaves and logs don't count as ground.
- Grass tufts and flowers are removed where the ground under them turns into something they can't grow on.
- `-p` previews first; `seed=<n>` gives a different arrangement of the mixes and patches. `//undo` reverts it.
- `/gztexture brush <preset> [radius]` binds the same look as a brush for touching up by hand. Brush strokes and
  selection texturing line up seamlessly, and `//mask` limits the brush further.

Presets live in `plugins/GZMNBuildtools/terrain-presets.yml`, so you can change them or add your own. A preset is a
list of rules tried from top to bottom; the first rule that matches picks the block:

```yaml
presets:
  volcanic:
    description: Basalt flows with ash patches.
    rules:
      - when: { slope: "45..", depth: "0..3" }   # steep: 45 degrees or more, top 4 blocks
        place: [ "60%basalt", "40%blackstone" ]
      - when: { noise: "0.7.." }                  # patches covering about a third of the ground
        place: [ "70%gravel", "30%tuff" ]
      - when: { }                                 # everything else (just the top block)
        place: [ blackstone ]
      - when: { depth: "1..3" }
        place: [ basalt ]
```

Conditions are `depth` (0 = the top block, the default), `slope` (degrees), `height` (world y) and `noise` (0 to 1),
written as `a..b`, `a..`, `..b` or a single number. `bands: [...]` with `band-height: 2` stacks blocks by height
instead of mixing them. Run `/gzmnbuildtools reload` after editing; problems are reported in the console.

### Prefab scatter

`/gzscatter <prefabs> [radius] [options]` (alias `/scatter`) binds a brush that scatters prefabs naturally over the
ground. For example:

- `/gzscatter tree:oak,tree:birch,rock 12` for a woodland edge
- `/gzscatter bushes/ 8` for every schematic in the `bushes` folder
- `/gzscatter ruin1,ruin2,rock,rock 16 density=0.2` (repeat a name to make it more common)

Prefabs are your WorldEdit schematics (save them with `//copy` and `//schem save <name>`; your own folder is searched
first when FAWE keeps one per player) plus the built-in `tree:<style>` (`oak`, `spruce`, `birch`, `willow`, `dead`,
`palm`) and `rock`, which come out different every time. `/gzscatter list` shows everything you can use.

- Prefabs are kept apart (by their size, or `spacing=`), and `density=` (0.05 to 1, default 0.5) sets how full the
  area gets.
- Each prefab is set down on the lowest ground under its base, so no corner floats. If a schematic was saved with the
  ground under it (a grass or dirt layer), that layer is buried; `sink=` overrides how deep.
- It skips steep ground (`slope=`, default 35 degrees), water, roofs and other non-natural ground (`on=any` allows
  them), and spots that already have a tree or build on them.
- Each prefab gets a random quarter turn (`rotate=0` turns this off) and can be mirrored with `flip=1`; stairs, logs
  and other directional blocks turn with it.
- Prefabs only fill air and plants, so they never cut into terrain or builds; `replace=all` lets them.
- Every click places a new random layout, and with `/gzhover` on you see the next one before you click. `//undo`
  reverts a click.

### Shape generators

`/gzgen <shape> <pattern> [parameters]` (alias `/ggen`) builds a shape with any FAWE pattern. Parameters can be given
in order or as `name=value`; run `/gzgen` for the list or `/gzgen <shape>` for details. Every generation can be undone
with `//undo`.

| Shape | Placed at | Parameters (defaults) |
|-------|-----------|-----------------------|
| `arch` | From selection point 1 to point 2 | `height=8`, `thickness=1` |
| `cable` | From selection point 1 to point 2, sagging | `sag=4`, `thickness=1` |
| `torus` | Your position | `major=8`, `minor=2`, `hollow=0` |
| `cone` | Your position | `radius=6`, `height=12`, `hollow=0` |
| `dome` | Your position | `radius=8`, `hollow=0` |
| `spiral` | Your position | `radius=6`, `height=16`, `turns=2`, `thickness=1` |
| `tree` | Your position (log and leaves patterns) | `style=oak`, `height=10`, `thickness=1`, `leaves=3` |
| `rock` | The ground under you | `radius=4`, `flatness=0.35`, `roughness=0.35`, `sink=0.3` |

Examples:

- `//pos1` and `//pos2` on two pillars, then `/gzgen arch stone_bricks 10 2`
- `/gzgen cable iron_chain sag=6` between two poles
- `/gzgen dome glass 12 hollow=1`
- `/gzgen tree spruce_log spruce_leaves style=spruce height=16 -p` previews a spruce before planting it

A single shape is limited to `generation.max-blocks` in `config.yml` (default 500000). Add `-p` to any `/gzgen` command
to preview it first.

### Web editor

`/gb web` sends you a personal link to a browser editor for your gradient brush. Design the palette (auto, from a
block, a ramp, your own steps or a preset), pick the options and watch a live preview of a dithered tower. Then
press **Apply to my brush**: your bound brush uses it straight away. The link only works for you and expires after
`web-editor.session-minutes` (default 30). Without a link the page still works as a designer that gives you a `/gb`
command to paste.

The editor runs on `web-editor.port` (default 8080). When players reach the server through a domain or reverse
proxy, set `web-editor.public-url` so the links point there. Set `web-editor.enabled: false` to turn it off.

### Splines

`/gzspline` (alias `/gsp`) builds along a smooth curve through points you pick. Look at a block and run
`/gzspline add` (or `/gzspline here` for the block you stand on) to add points. Particles show the curve; `/gzspline
show` draws it again, and `undo`, `clear` and `list` manage the points. Then build along it:

| Shape | Parameters (defaults) |
|-------|-----------------------|
| `/gzspline build tube <pattern>` | `radius=1.5`, `hollow=0` (pipes, cables, tunnels) |
| `/gzspline build wall <pattern>` | `height=4`, `thickness=1` |
| `/gzspline build road <pattern>` | `width=3`, `clear=3` (air above), `fill=2` (support below) |
| `/gzspline build river <bed pattern>` | `width=7`, `depth=3`, `sink=1`, `valley=10`, `slope=1` |

The curve passes through every point without overshooting, and roads follow its height, so they climb hills, cut
through them with `clear` and bridge dips with `fill`. Add `-p` to preview. Builds count towards
`generation.max-blocks` and are undone with `//undo`.

Rivers are made for clicking along the ground. For example, `/gzspline build river gravel` or
`/gzspline build river 70%gravel,30%sand width=9`:

- The water sits `sink` blocks below your points, in a rounded channel `width` across and up to `depth` deep, on a bed
  of the pattern you give.
- A river always flows downhill, whichever end you started at, and its level never rises: where your points climb
  over a rise, it keeps its level and cuts through.
- Above the water the ground is cut away up to `valley` blocks high, with banks sloping `slope` blocks up per block
  out, so a river through a hill carves its own valley. Existing water (a lake or the sea at its mouth) is left alone.
- Where the ground falls away beside the river, it builds banks from the ground it dug through, so the water stays in.

### Previews

`/gb preview` and `/gzgen ... -p` show the result as client-side ghost blocks that only you can see. Click
**[Apply]** (or `/gzpreview apply`) to place exactly what you see, or **[Cancel]** (`/gzpreview cancel`). A preview
expires after `preview.seconds` (default 60) and is limited to `preview.max-blocks` (default 60000).

### Hover preview

`/gzhover` (alias `/hover`) toggles a live preview. While you hold the gradient, swap, weathering, texture or scatter
brush, ghost blocks show what a click would change wherever you look, and they follow your aim. A click places exactly
what was shown. The preview respects the brush's `/size` and `//mask` and your global mask, and changes nothing until
you click. The setting is remembered per player.

Server owners can tune it under `hover:` in `config.yml`. `interval-ticks` sets how often aim is checked (default 4)
and `range` how far you can look (default 64). Strokes changing more than `max-blocks` (default 4000) aren't previewed.
`enabled-by-default` turns it on for everyone who hasn't chosen.

## Modules

Every feature is a module that can be switched off under `modules:` in `config.yml`. A switched-off module doesn't
register its commands, listeners or (for the web editor) its network port. If a module fails to start, only that
module, and modules that depend on it, are skipped; the rest keep working. Changes need a restart.
`/gzmnbuildtools modules` shows what is running and why anything isn't.

| Module | Adds | Needs |
|--------|------|-------|
| `masks-and-patterns` | `#slope`, `#depth`, `#byslope`, `#stripes` and the other masks and patterns | |
| `gradient` | `/gradient`, saved gradients and the `#gradient` pattern | |
| `type-replace` | `/typereplace` and its menu | |
| `gradient-brush` | `/gradientbrush` (`/gb`) and its menus | |
| `terrain-brushes` | `/terrainbrush` (`/tb`), including tree and rock brushes | |
| `generators` | `/gzgen` | |
| `splines` | `/gzspline` | |
| `web-editor` | The browser editor and `/gb web` links | `gradient-brush` |
| `build-select` | `/gzselect` | |
| `material-swap` | `/gzswap` | |
| `weathering` | `/weathering` (`/age`) | |
| `pixel-art` | `/gzpixelart` | |
| `terrain-texture` | `/gztexture` and `terrain-presets.yml` | |
| `hover-preview` | `/gzhover` live brush previews | |
| `prefab-scatter` | `/gzscatter` | |
| `roof` | `/gzroof` | |
| `loft` | `/gzloft` | |
| `revolve` | `/gzrevolve` | |
| `resize` | `/gzresize` | |
| `smear` | `/gzsmear` | |
| `shadow` | `/gzshadow` | |

Previews (`/gzpreview`), reloading (`/gzmnbuildtools reload`) and the presets and block families are part of the core
and always available.

## Configuration

On first run the plugin writes editable files to `plugins/GZMNBuildtools/`:

- `config.yml`: General settings (e.g. `messages.verbose`).
- `presets.yml`: The built-in gradient presets. Add or edit entries here to create your own presets without
  recompiling.
- `block-families.yml`: The block families used by `/typereplace` (which stairs/slabs/walls/etc. each material has)
  and the material tabs shown in the Type Replace GUI (`categories:`).
- `messages.yml`: Message template defaults.
- `terrain-presets.yml`: The terrain looks used by `/gztexture`.
- `lang/en.yml`, `lang/nl.yml`: Message texts per language (see below).

After editing these, run `/gzmnbuildtools reload` (alias `/gzmnbt reload`) to apply changes without restarting.

### Languages

Players see GZMN messages in their game's language when there is a translation, and in English otherwise. English
and Dutch (`nl`) are included. To force one language for everyone, set `messages.language` (for example to `nl`) in
`config.yml`; the default `auto` follows each player.

Each file in `lang/` maps the English message, exactly as written in the code, to the text to show:

```yaml
"Radius %d is above your limit of %d.": "Straal %d is groter dan je limiet van %d."
```

To add a language, copy `lang/en.yml` to `lang/<code>.yml` (e.g. `de.yml`), translate the values and run
`/gzmnbuildtools reload`. Keep the placeholders (`%s`, `%d`, `%.2f`); number them (`%2$s`) to change their order. If
a translation's placeholders don't match, that message falls back to English. You can also edit `en.yml` to reword
messages. Edited files override the bundled text one message at a time, so plugin updates still bring in new messages.

For contributors: `lang/en.yml` is generated from the `messages.*(...)` calls in the code, and `MessageCatalogueTest`
fails when it is out of date (regenerate it with `mvn test -Dcatalogue.write=true`). The test also checks that every
bundled translation covers all messages, has no stale keys and uses the right placeholders.

### Limits per rank

The limits in `config.yml` (brush radius, `generation.max-blocks`, ...) apply to everyone. Under `limits:` you can give
ranks their own:

```yaml
limits:
  default:          # everyone
    radius: 12
    blocks: 100000
  ranks:
    builder:        # players with gzmnbuildtools.limits.builder
      radius: 32
      blocks: 500000
    architect:      # gzmnbuildtools.limits.architect
      radius: 64
      blocks: 2000000
      image: 512
```

| Limit | What it caps |
|-------|--------------|
| `radius` | Radius of every GZMN brush. Larger `/size` values are capped to it |
| `blocks` | Blocks one `/gzgen`, `/gzspline` or scatter click may place |
| `selection` | Blocks in a selection `/gztexture` works on |
| `image` | Longest side of a `/gzpixelart` picture |
| `loadouts` | Saved loadouts per brush |

A player gets the highest value among their ranks. Anything no rank sets falls back to the normal setting.
`gzmnbuildtools.limits.unlimited` lifts every limit (operators don't get it automatically). `/gzlimits` shows players
their own limits and where they come from; admins can check anyone with `/gzlimits <player>`. FAWE's own limits still
apply on top.

## Permissions

- `gzmnbuildtools.typereplace`: Grants access to the type replace command.
- `gzmnbuildtools.gradient`: Grants access to the gradient command.
- `gzmnbuildtools.admin`: Grants access to `/gzmnbuildtools reload`.
- `gzmnbuildtools.terrainbrush`: Grants access to `/terrainbrush` and using its brushes.
- `gzmnbuildtools.generate`: Grants access to `/gzgen` and `/gzspline`.
- `gzmnbuildtools.select`: Grants access to `/gzselect`.
- `gzmnbuildtools.swap`: Grants access to `/gzswap`.
- `gzmnbuildtools.weathering`: Grants access to `/weathering`.
- `gzmnbuildtools.pixelart`: Grants access to `/gzpixelart`.
- `gzmnbuildtools.texture`: Grants access to `/gztexture`.
- `gzmnbuildtools.hover`: Grants access to `/gzhover`.
- `gzmnbuildtools.scatter`: Grants access to `/gzscatter` and using its brush.
- `gzmnbuildtools.roof`: Grants access to `/gzroof`.
- `gzmnbuildtools.loft`: Grants access to `/gzloft`.
- `gzmnbuildtools.revolve`: Grants access to `/gzrevolve`.
- `gzmnbuildtools.resize`: Grants access to `/gzresize`.
- `gzmnbuildtools.smear`: Grants access to `/gzsmear` and using its brush.
- `gzmnbuildtools.shadow`: Grants access to `/gzshadow`.
- `gzmnbuildtools.limits.<rank>`: Gives the limits of a rank from `limits.ranks` (see above).
- `gzmnbuildtools.limits.unlimited`: Lifts all limits (not given to operators by default).
- `gzmnbuildtools.*`: Grants access to all commands.

All permissions default to OP.

## Architecture (for contributors)

The code is split like a Spring Modulith application, without Spring:

- `core`: bootstrap services (`PluginContext`: messages, presets, block families, previews, block knowledge and
  per-player data), the module system (`core.module`) and the always-on commands.
- `shared`: libraries any module may use: build awareness (`shared.build`: finding the connected build, shapes,
  protected details, the generic `BuildPainter`), palette and block colours, terrain maths, shape geometry, brush
  parameters, menus, previews and WorldEdit helpers (`BuildEditor` runs a build-aware edit at a click).
- `modules.<name>`: one package per feature. Its top-level package holds the `GzmnModule` class and the module's
  public API; sub-packages such as `command`, `menu`, `engine` and `fawe` are internal to the module.
- `api`, plus the model types it exposes (`gradient.model.GradientPreset`, `typereplace`): the public API for
  other plugins.

A module registers everything through its `ModuleContext`: commands, listeners, and services it `provide`s to modules
that list it in `dependencies()` and `require` it. For example, `web-editor` requires the gradient brush's
`SmartGradientService` and plugs its link generator into `/gb web`.

`ModuleBoundariesTest` (ArchUnit) enforces the rules at build time: the kernel never depends on a module, a module
only uses modules it declares, and only through their top-level package, and modules have no cycles. To add a
feature, create `modules/<name>/<Name>Module.java`, add it to `GZMNBuildtools.allModules()` and give it a switch in
`config.yml`.

A module adds itself to the `/gz` help hub with `context.help(HelpEntry.of(...))`. Wrap the title and description in
`tr(...)` so the message catalogue picks them up for translation. Entries only appear while the module is running
and for players who have the entry's permission.

Modules can also meet through shared interfaces instead of depending on each other. A brush that implements
`shared.worldedit.PreviewableBrush` gets hover previews from the `hover-preview` module without either knowing about
the other.

## Extending (for plugin developers)

GZMNBuildtools exposes a small API through Bukkit's `ServicesManager`, so other plugins can register gradient
presets or block families at runtime:

```java
RegisteredServiceProvider<GzmnBuildtoolsApi> rsp =
        getServer().getServicesManager().getRegistration(GzmnBuildtoolsApi.class);
if (rsp != null) {
    GzmnBuildtoolsApi api = rsp.getProvider();
    api.presets().register(myPreset);
    api.blockFamilies().register("my_material", myDefinition);
    api.messages().info(player, "Hello from my addon");
}
```

The API surface lives in the `nl.gzmn.gZMNBuildtools.api` package (`GzmnBuildtoolsApi`, `PresetRegistry`,
`BlockFamilyRegistry`, `Messages`).
