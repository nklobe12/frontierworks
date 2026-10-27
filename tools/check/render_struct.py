#!/usr/bin/env python3
"""Renders structure templates (gen.structures.Struct) as isometric voxel images for eyeballing layouts.

Usage from Python: render(struct, path, cut=None)  - cut=(axis, value) hides blocks beyond a plane to see inside.
"""
import hashlib
import sys
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

KNOWN = {
    'grass_block': (95, 160, 60), 'dirt': (130, 90, 60), 'stone': (125, 125, 125), 'water': (50, 90, 220), 'chest': (200, 140, 40),
    'spawner': (30, 30, 50), 'ladder': (170, 130, 70), 'lantern': (255, 210, 90), 'jigsaw': (230, 40, 200), 'oak_leaves': (60, 130, 40),
    'birch_leaves': (110, 160, 70), 'tnt': (220, 40, 40), 'barrel': (150, 100, 50), 'cobweb': (235, 235, 235), 'glass_pane': (190, 220, 240),
}


def color(state):
    name = state.split('[')[0].split(':')[-1]
    for k, v in KNOWN.items():
        if k in name:
            return v
    h = hashlib.md5(name.encode()).digest()
    return (60 + h[0] % 170, 60 + h[1] % 170, 60 + h[2] % 170)


def render(s, path, cut=None, scale=6):
    sx, sy, sz = s.size
    W = (sx + sz) * scale * 2 + 20
    H = (sx + sz) * scale + sy * scale * 2 + 20
    img = Image.new('RGB', (W, H), (24, 26, 30))
    d = ImageDraw.Draw(img)
    ox = sz * scale * 2 + 10
    oy = sy * scale * 2 + 10
    blocks = [(p, st) for p, (st, _) in s.blocks.items() if not st.endswith(':air')]
    if cut:
        ax, v = cut
        i = 'xyz'.index(ax)
        blocks = [(p, st) for p, st in blocks if p[i] <= v]
    blocks.sort(key=lambda b: (b[0][0] + b[0][2], b[0][1]))
    for (x, y, z), st in blocks:
        c = color(st)
        px = ox + (x - z) * scale * 2
        py = oy + (x + z) * scale - y * scale * 2
        top = [(px, py), (px + 2 * scale, py + scale), (px, py + 2 * scale), (px - 2 * scale, py + scale)]
        left = [(px - 2 * scale, py + scale), (px, py + 2 * scale), (px, py + 4 * scale), (px - 2 * scale, py + 3 * scale)]
        right = [(px + 2 * scale, py + scale), (px, py + 2 * scale), (px, py + 4 * scale), (px + 2 * scale, py + 3 * scale)]
        d.polygon(top, fill=c)
        d.polygon(left, fill=tuple(int(v * 0.75) for v in c))
        d.polygon(right, fill=tuple(int(v * 0.55) for v in c))
    img.save(path)
    return path


def sheet(structs, path, cut=None, scale=4):
    tmp = []
    for i, s in enumerate(structs):
        p = Path(path).with_suffix('.%d.png' % i)
        render(s, p, cut, scale)
        tmp.append(Image.open(p))
    w = sum(t.width for t in tmp)
    h = max(t.height for t in tmp)
    out = Image.new('RGB', (w, h), (24, 26, 30))
    x = 0
    for t in tmp:
        out.paste(t, (x, 0))
        x += t.width
    out.save(path)
    for i in range(len(structs)):
        Path(path).with_suffix('.%d.png' % i).unlink()
    return path
