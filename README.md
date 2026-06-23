# GZMNBuildtools

This is a WorldEdit/FastAsyncWorldEdit extension for type replacement and gradient generation.

## Requirements

- Paper 1.21+
- WorldEdit or FastAsyncWorldEdit (FAWE)

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

## Configuration

On first run the plugin writes editable files to `plugins/GZMNBuildtools/`:

- `config.yml`: General settings (e.g. `messages.verbose`).
- `presets.yml`: The built-in gradient presets. Add or edit entries here to create your own presets without
  recompiling.
- `messages.yml`: Message template defaults.

After editing these, run `/gzmnbuildtools reload` (alias `/gzmnbt reload`) to apply changes without restarting.

## Permissions

- `gzmnbuildtools.typereplace`: Grants access to the type replace command.
- `gzmnbuildtools.gradient`: Grants access to the gradient command.
- `gzmnbuildtools.admin`: Grants access to `/gzmnbuildtools reload`.
- `gzmnbuildtools.*`: Grants access to all commands.

All permissions default to OP.
