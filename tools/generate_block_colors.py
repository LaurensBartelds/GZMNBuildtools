#!/usr/bin/env python3
"""Generate src/main/resources/block-colors.tsv from a Minecraft client jar.

For every block state file the script resolves the block's first model (and its
parents) to textures, and averages them in linear light:

  side  - the texture you see on the side of the block (particle/side/all)
  top   - the texture on top (top/end/up/all), or side when there is none

Only averaged colours are written, no texture data. Tinted textures (grass,
leaves, vines, water) are multiplied by their default plains-biome tint.

Usage: python3 tools/generate_block_colors.py <client.jar> [output.tsv]
"""
import io
import json
import sys
import zipfile

import numpy as np
from PIL import Image

# Default (plains) tints applied to tintindex faces.
FOLIAGE = (0x77, 0xAB, 0x2F)
GRASS = (0x91, 0xBD, 0x59)
FIXED_TINTS = {
    "spruce_leaves": (0x61, 0x99, 0x61),
    "birch_leaves": (0x80, 0xA7, 0x55),
    "lily_pad": (0x20, 0x80, 0x30),
    "water": (0x3F, 0x76, 0xE4),
    "water_cauldron": (0x3F, 0x76, 0xE4),
}
GRASS_LIKE = ("grass", "fern", "sugar_cane")

SIDE_KEYS = ("side", "all", "texture", "particle", "pattern", "front", "wall", "cross", "plant")
TOP_KEYS = ("top", "end", "up", "all", "texture", "side", "particle")


def to_linear(c):
    c = c / 255.0
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def to_srgb(c):
    c = c * 12.92 if c <= 0.0031308 else 1.055 * c ** (1 / 2.4) - 0.055
    return max(0, min(255, round(c * 255)))


class Assets:
    def __init__(self, jar):
        self.zip = zipfile.ZipFile(jar)
        self.names = set(self.zip.namelist())
        self.models = {}
        self.textures = {}

    def json(self, path):
        return json.loads(self.zip.read(path))

    def model(self, ref):
        """Returns (textures, has_tint) for a model reference, merging parents."""
        name = ref.split(":")[-1]
        if name in self.models:
            return self.models[name]
        path = f"assets/minecraft/models/{name}.json"
        textures, tint = {}, False
        if path in self.names:
            data = self.json(path)
            if "parent" in data:
                parent_textures, tint = self.model(data["parent"])
                textures.update(parent_textures)
            textures.update(data.get("textures", {}))
            for element in data.get("elements", []):
                for face in element.get("faces", {}).values():
                    tint = tint or "tintindex" in face
        self.models[name] = (textures, tint)
        return self.models[name]

    def average(self, ref):
        """Alpha-weighted linear average colour of a texture, or None."""
        name = ref.split(":")[-1]
        if name in self.textures:
            return self.textures[name]
        path = f"assets/minecraft/textures/{name}.png"
        result = None
        if path in self.names:
            image = Image.open(io.BytesIO(self.zip.read(path))).convert("RGBA")
            pixels = np.asarray(image, dtype=np.float64).reshape(-1, 4) / 255.0
            rgb = np.where(pixels[:, :3] <= 0.04045, pixels[:, :3] / 12.92,
                           ((pixels[:, :3] + 0.055) / 1.055) ** 2.4)
            alpha = pixels[:, 3]
            weight = alpha.sum()
            if weight > 0:
                linear = (rgb * alpha[:, None]).sum(axis=0) / weight
                result = (list(linear), weight / len(alpha))
        self.textures[name] = result
        return result


def sprite(value):
    # 26.x models may give a texture as {"sprite": ..., "force_translucent": ...}.
    return value.get("sprite") if isinstance(value, dict) else value


def resolve(textures, key):
    value = sprite(textures.get(key))
    seen = set()
    while isinstance(value, str) and value.startswith("#") and value not in seen:
        seen.add(value)
        value = sprite(textures.get(value[1:]))
    return value


def first_model(state):
    if "variants" in state:
        variant = next(iter(state["variants"].values()))
        return (variant[0] if isinstance(variant, list) else variant)["model"]
    for part in state.get("multipart", []):
        apply = part["apply"]
        return (apply[0] if isinstance(apply, list) else apply)["model"]
    return None


def tint_for(block_id):
    if block_id in FIXED_TINTS:
        return FIXED_TINTS[block_id]
    if any(word in block_id for word in GRASS_LIKE):
        return GRASS
    return FOLIAGE


def colour(assets, textures, keys, tint):
    for key in keys:
        ref = resolve(textures, key)
        if ref:
            avg = assets.average(ref)
            if avg:
                linear, coverage = avg
                if tint:
                    linear = [c * to_linear(t) for c, t in zip(linear, tint)]
                return "%02X%02X%02X" % tuple(to_srgb(c) for c in linear), coverage
    return None, 0


def main():
    jar = sys.argv[1]
    out = sys.argv[2] if len(sys.argv) > 2 else "src/main/resources/block-colors.tsv"
    assets = Assets(jar)
    rows = []
    for path in sorted(n for n in assets.names
                       if n.startswith("assets/minecraft/blockstates/") and n.endswith(".json")):
        block_id = path.rsplit("/", 1)[1][:-5]
        model = first_model(assets.json(path))
        if not model:
            continue
        textures, tinted = assets.model(model)
        tint = tint_for(block_id) if tinted or block_id in FIXED_TINTS else None
        side, coverage = colour(assets, textures, SIDE_KEYS, tint)
        if side is None:
            continue
        top, _ = colour(assets, textures, TOP_KEYS, tint)
        flags = ("t" if tinted else "") + ("a" if coverage < 0.95 else "")
        rows.append(f"{block_id}\t{side}\t{top or side}\t{flags or '-'}")
    with open(out, "w") as f:
        f.write("# Average block colours derived from Minecraft client assets by\n")
        f.write("# tools/generate_block_colors.py. Columns: id, side RGB, top RGB, flags\n")
        f.write("# (t = biome-tinted, default tint applied; a = partly transparent).\n")
        f.write("\n".join(rows) + "\n")
    print(f"wrote {len(rows)} blocks to {out}")


if __name__ == "__main__":
    main()
