# GZMNBuildtools

This is a WorldEdit/FastAsyncWorldEdit extension for type replacement and gradient generation.

**Status: Work in Progress**

This project is currently under development.
- The Type Replace feature is functional.
- The Gradient tool is currently not fully implemented.

## Requirements

- Paper 1.21+
- WorldEdit or FastAsyncWorldEdit (FAWE)

## Installation

1. Place the jar file in your server's plugins folder.
2. Ensure you have WorldEdit or FAWE installed.
3. Restart the server.

## Usage

### Type Replace

The Type Replace tool allows replacing entire families of blocks (stairs, slabs, walls, fences) while preserving their orientation and properties where possible.

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

**Note: This feature is not yet fully implemented.**

Command: `/gradient`

This command is intended to open a gradient UI or accept command syntax for generating gradients, but it is currently incomplete.

## Permissions

- `gzmnbuildtools.typereplace`: Grants access to the type replace command.
- `gzmnbuildtools.gradient`: Grants access to the gradient command.
- `gzmnbuildtools.*`: Grants access to all commands.

All permissions default to OP.
