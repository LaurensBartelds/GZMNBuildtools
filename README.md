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
your hand. It is a normal FAWE brush, so `//size`, `//mask`, `//mat`, scroll actions, `/none` and visualisation all
work. Parameters can be given in order or as `name=value`. Run `/tb` for the list or `/tb <type>` for the details of
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

The reshaping brushes (`raise` … `roughen`) move whole columns: the top four surface layers and any plants on top
travel with the surface, so grass stays on top and dirt stays below it.

Examples:

- `/tb smooth 8 3 iterations=4`
- `/tb overlay 6 #byslope[0:grass_block][35:coarse_dirt][55:stone] 2`
- `/tb spike 2 dripstone_block height=10 down=1`

The largest allowed radius is set by `terrain-brushes.max-radius` in `config.yml` (default 32).

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

Examples:

- `//pos1` and `//pos2` on two pillars, then `/gzgen arch stone_bricks 10 2`
- `/gzgen cable iron_chain sag=6` between two poles
- `/gzgen dome glass 12 hollow=1`

A single shape is limited to `generation.max-blocks` in `config.yml` (default 500000).

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
