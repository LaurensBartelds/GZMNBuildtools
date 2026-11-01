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
the brushes in chat. It is a normal FAWE brush, so `//size`, `//mask`, `//mat`, scroll actions, `/none` and visualisation all
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
| `radius` | `4`; the brush radius (`//size` works too) |

Run `/gb` without arguments to open the brush menu. It shows the palette and has a button for every option. To base
a palette on a block, click it in your own inventory; shift-click blocks to build a palette in order, and the Steps
button fills in blocks between them by colour. Bind, Paint, Preview, Inspect and Reset are buttons too.

Other commands: `/gb status` shows your settings, `/gb paint [palette] [options]` paints the whole build you look at,
`/gb preview [palette] [options]` shows that paint as ghost blocks first,
`/gb info` inspects it (size, materials, direction, palette), `/gb suggest [block]` offers clickable palettes,
`/gb code <gzmn@...>` imports a web editor code, `/gb rescan` forgets the remembered build and `/gb reset` restores
the defaults. Settings changed with `/gb set` apply to the bound brush immediately.

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

### Previews

`/gb preview` and `/gzgen ... -p` show the result as client-side ghost blocks that only you can see. Click
**[Apply]** (or `/gzpreview apply`) to place exactly what you see, or **[Cancel]** (`/gzpreview cancel`). A preview
expires after `preview.seconds` (default 60) and is limited to `preview.max-blocks` (default 60000).

## Configuration

On first run the plugin writes editable files to `plugins/GZMNBuildtools/`:

- `config.yml`: General settings (e.g. `messages.verbose`).
- `presets.yml`: The built-in gradient presets. Add or edit entries here to create your own presets without
  recompiling.
- `block-families.yml`: The block families used by `/typereplace` (which stairs/slabs/walls/etc. each material has)
  and the material tabs shown in the Type Replace GUI (`categories:`).
- `messages.yml`: Message template defaults.

After editing these, run `/gzmnbuildtools reload` (alias `/gzmnbt reload`) to apply changes without restarting.

## Permissions

- `gzmnbuildtools.typereplace`: Grants access to the type replace command.
- `gzmnbuildtools.gradient`: Grants access to the gradient command.
- `gzmnbuildtools.admin`: Grants access to `/gzmnbuildtools reload`.
- `gzmnbuildtools.terrainbrush`: Grants access to `/terrainbrush` and using its brushes.
- `gzmnbuildtools.generate`: Grants access to `/gzgen`.
- `gzmnbuildtools.*`: Grants access to all commands.

All permissions default to OP.

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
