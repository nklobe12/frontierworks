"""Procedural textures for gems and crystals (Lapidary and Resonance): rough stones, cut gems, crystal clusters,
druse linings, budding blocks, gem blocks and lamps. All drawn from scratch with PIL."""
import math

from PIL import Image, ImageDraw

from gen import tex

OPAL_FLECKS = [(255, 90, 120), (90, 220, 140), (90, 160, 255), (255, 210, 80), (200, 120, 255)]


def _ramp(color):
    return tex.ramp(color)


def _fleck(img, seed, special, mask=None, density=0.18):
    """Opal play-of-colour or onyx banding over the opaque pixels of an image."""
    if not special:
        return img
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a < 200 or (mask and not mask(x, y)):
                continue
            if special == 'opal' and tex.rnd(seed + 'op', x // 2, y // 2) < density:
                c = OPAL_FLECKS[tex.hsh(seed, x // 2, y // 2) % len(OPAL_FLECKS)]
                px[x, y] = (int(r * 0.4 + c[0] * 0.6), int(g * 0.4 + c[1] * 0.6), int(b * 0.4 + c[2] * 0.6), a)
            elif special == 'onyx' and (x + 2 * y) % 7 == 0:
                px[x, y] = (min(255, r + 70), min(255, g + 70), min(255, b + 74), a)
    return img


def _outline(img, color):
    """Adds a 1px dark outline around opaque pixels (item style)."""
    px = img.load()
    out = img.copy()
    opx = out.load()
    k = _ramp(color)['K']
    for y in range(16):
        for x in range(16):
            if px[x, y][3] > 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                xx, yy = x + dx, y + dy
                if 0 <= xx < 16 and 0 <= yy < 16 and px[xx, yy][3] > 0:
                    opx[x, y] = k
                    break
    return out


# ----------------------------------------------------------------------------- items

def rough_gem(color, seed, special=None):
    """An uncut stone: an irregular chunk with a few flat crystal faces, matte rind on one side."""
    r = _ramp(color)
    img = tex.blank()
    px = img.load()
    cx, cy = 7.5, 8.5
    pts = []
    n = 7
    for i in range(n):
        ang = 2 * math.pi * i / n + tex.rnd(seed, 'a', i) * 0.5
        rad = 4.2 + tex.rnd(seed, 'r', i) * 2.0
        pts.append((cx + math.cos(ang) * rad, cy + math.sin(ang) * rad * 0.85))
    mask = Image.new('L', (16, 16), 0)
    ImageDraw.Draw(mask).polygon(pts, fill=255)
    mpx = mask.load()
    for y in range(16):
        for x in range(16):
            if not mpx[x, y]:
                continue
            # facet shading: split into wedges around a off-centre apex
            ang = math.atan2(y - 6.5, x - 6.0)
            wedge = int(((ang + math.pi) / (2 * math.pi)) * 5) % 5
            shade = ['2', '3', '4', '3', '1'][wedge]
            if tex.rnd(seed, x, y) < 0.12:
                shade = str(max(1, min(5, int(shade) + (1 if tex.rnd(seed, 'u', x, y) < 0.5 else -1))))
            px[x, y] = r[shade]
    # matte rind (host rock) along the lower right
    for y in range(16):
        for x in range(16):
            if mpx[x, y] and (x - cx) * 0.6 + (y - cy) > 3.2:
                g = 96 + int(tex.rnd(seed, 'rk', x, y) * 30)
                px[x, y] = (g, g - 6, g - 12, 255)
    # sparkle
    for (x, y) in ((5, 6), (6, 5)):
        if mpx[x, y]:
            px[x, y] = r['W']
    _fleck(img, seed, special)
    return _outline(img, color)


def cut_gem(color, cut, seed, special=None):
    """Cut gem item: cabochon (smooth dome), step (rectangular emerald cut) or brilliant (round, faceted)."""
    r = _ramp(color)
    img = tex.blank()
    px = img.load()
    if cut == 'cabochon':
        cx, cy, rx, ry = 7.5, 8.0, 5.6, 4.6
        for y in range(16):
            for x in range(16):
                d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2
                if d > 1.0:
                    continue
                hl = math.hypot(x - (cx - 2), y - (cy - 1.6))
                if hl < 1.2:
                    c = r['W']
                elif hl < 2.4:
                    c = r['5']
                elif d > 0.78:
                    c = r['1']
                elif d > 0.5:
                    c = r['2']
                elif x + y > cx + cy + 1:
                    c = r['3']
                else:
                    c = r['4']
                px[x, y] = c
    elif cut == 'step':
        x0, y0, x1, y1 = 3, 4, 12, 12
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                # cut corners
                if min(x - x0, x1 - x) + min(y - y0, y1 - y) < 2:
                    continue
                ring = min(x - x0, x1 - x, y - y0, y1 - y)
                c = {0: r['1'], 1: r['2'], 2: r['3']}.get(ring, r['4'])
                # light from the top-left: upper/left steps brighter
                if ring < 3 and (y - y0 <= ring or x - x0 <= ring):
                    c = {0: r['3'], 1: r['4'], 2: r['5']}[ring]
                px[x, y] = c
        for x in range(6, 10):
            px[x, 7] = r['W']
    else:  # brilliant
        cx, cy, rad = 7.5, 7.5, 5.8
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - cx, y - cy)
                if d > rad:
                    continue
                ang = math.atan2(y - cy, x - cx)
                sector = int(((ang + math.pi) / (2 * math.pi)) * 8 + 0.5) % 8
                if d < 2.3:
                    c = r['5'] if (x + y) % 3 else r['W']
                elif d > rad - 1.1:
                    c = r['2'] if sector % 2 else r['1']
                else:
                    c = [r['4'], r['2'], r['5'], r['3'], r['4'], r['1'], r['3'], r['2']][sector]
                px[x, y] = c
        px[5, 5] = r['W']
        px[6, 4] = r['W']
    _fleck(img, seed, special, density=0.22)
    return _outline(img, color)


def grit(color, seed):
    img = tex.blank()
    px = img.load()
    r = _ramp(color)
    for i in range(46):
        x = 3 + int(tex.rnd(seed, 'x', i) * 10)
        y = 6 + int(tex.rnd(seed, 'y', i) * 7)
        if abs(x - 7.5) + abs(y - 10) * 1.3 > 7:
            continue
        px[x, y] = r[str(1 + tex.hsh(seed, 'c', i) % 5)]
    return _outline(img, color)


def setting(metal, seed):
    """A metal claw setting: a ring with four prongs."""
    r = _ramp(metal)
    img = tex.blank()
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 8.5)
            if 3.0 < d < 5.2:
                px[x, y] = r['4'] if y < 8 else r['2']
    for (x, y) in ((7, 2), (8, 2), (2, 8), (13, 8), (7, 14), (8, 14)):
        px[x, y] = r['5']
    for (x, y) in ((7, 3), (8, 3), (3, 8), (12, 8), (7, 13), (8, 13)):
        px[x, y] = r['3']
    return _outline(img, metal)


# ----------------------------------------------------------------------------- blocks

def druse(color, seed, special=None):
    """A crust of tiny glittering crystals (inner geode lining)."""
    c = tex.rgb(color)
    img = tex.noise_fill(tex.shade(c, 0.8), seed, 0.22)
    px = img.load()
    for i in range(30):
        x, y = tex.hsh(seed, 'gx', i) % 16, tex.hsh(seed, 'gy', i) % 16
        px[x, y] = tex.shade(c, 1.0, 0.25)
        if x + 1 < 16:
            px[x + 1, y] = tex.shade(c, 1.0, 0.08)
        if y + 1 < 16:
            px[x, y + 1] = tex.shade(c, 0.6)
    _fleck(img, seed, special, density=0.12)
    return img


def budding(color, seed, special=None):
    """Druse cracked open, with bright crystal points pushing through dark pits."""
    c = tex.rgb(color)
    img = druse(color, seed + 'b', special)
    px = img.load()
    dark = tex.shade(c, 0.32)
    for (sx, sy) in ((3, 3), (11, 3), (7, 8), (2, 12), (12, 11)):
        for dx in range(-2, 3):
            for dy in range(-2, 3):
                if abs(dx) + abs(dy) <= 2:
                    px[(sx + dx) % 16, (sy + dy) % 16] = dark
        for dx, dy, l in ((0, 0, 0.42), (1, 0, 0.22), (-1, 0, 0.12), (0, 1, 0.05), (0, -1, 0.3)):
            px[(sx + dx) % 16, (sy + dy) % 16] = tex.shade(c, 1.0, l, 1.1)
    return img


def cluster_stage(color, stage, seed, special=None):
    """Cross-model crystal growth: stage 0 small bud ... 3 full cluster."""
    r = _ramp(color)
    img = tex.blank()
    px = img.load()
    specs = {
        0: [(7, 4, 2)],
        1: [(5, 6, 2), (9, 8, 2)],
        2: [(4, 9, 2), (8, 12, 3), (11, 8, 2)],
        3: [(2, 9, 2), (5, 14, 3), (9, 15, 3), (12, 10, 2)],
    }[stage]
    for i, (x0, h, w) in enumerate(specs):
        lean = (tex.hsh(seed, stage, i) % 3) - 1
        for k in range(h):
            y = 15 - k
            t = k / max(1, h - 1)
            half = w if t < 0.7 else max(0, int(round(w * (1 - t) / 0.3)))
            shift = int(round(lean * t * 2))
            for dx in range(-half, half + 1):
                x = x0 + dx + shift
                if not 0 <= x < 16:
                    continue
                if dx == -half:
                    c = r['5'] if k < h - 1 else r['W']
                elif dx == half:
                    c = r['1']
                elif dx < 0:
                    c = r['4']
                else:
                    c = r['3'] if dx == 0 else r['2']
                px[x, y] = c
        tip_x = x0 + int(round(lean * 2))
        if 0 <= tip_x < 16:
            px[tip_x, 15 - h + 1] = r['W']
    _fleck(img, seed + str(stage), special, density=0.15)
    return img


def gem_block(color, seed, special=None):
    img = tex.gem_block(color, seed)
    return _fleck(img, seed, special, density=0.12)


def gem_lamp(color, lit, seed, special=None):
    """Faceted gem panes in a dark metal lattice; bright and saturated when lit."""
    c = tex.rgb(color)
    img = tex.blank((40, 40, 46, 255))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15) or x in (7, 8) or y in (7, 8):
                px[x, y] = (70, 70, 78, 255) if (x + y) % 2 else (54, 54, 60, 255)
                continue
            lx, ly = x % 8, y % 8
            d = abs(lx - 3.5) + abs(ly - 3.5)
            if lit:
                col = tex.shade(c, 1.05, 0.16 - d * 0.035, 1.25)
            else:
                col = tex.shade(c, 0.42 + 0.03 * (4 - d))
            px[x, y] = col
    if lit:
        for (x, y) in ((2, 2), (10, 2), (2, 10), (10, 10)):
            px[x, y] = (255, 255, 255, 255)
    return _fleck(img, seed, special, density=0.1)


def geode_crust(seed):
    """Outer geode shell: dark, rough, pitted."""
    img = tex.stone('#3d3b40', seed, 'rough')
    px = img.load()
    for i in range(12):
        x, y = tex.hsh(seed, 'p', i) % 16, tex.hsh(seed, 'q', i) % 16
        px[x, y] = (26, 25, 28, 255)
    return img


def geode_lining(seed):
    """Middle geode shell: pale, chalky, with faint crystal glints."""
    img = tex.stone('#d9d4c7', seed, 'mottled')
    px = img.load()
    for i in range(8):
        x, y = tex.hsh(seed, 'g', i) % 16, tex.hsh(seed, 'h', i) % 16
        px[x, y] = (250, 248, 240, 255)
    return img


def crystal_block(color, seed, special=None):
    """Large interlocking crystal faces (Resonance crystal blocks)."""
    c = tex.rgb(color)
    img = tex.blank()
    px = img.load()
    cells = [(tex.hsh(seed, 'cx', i) % 16, tex.hsh(seed, 'cy', i) % 16) for i in range(6)]
    for y in range(16):
        for x in range(16):
            best, second, idx = 99, 99, 0
            for i, (a, b) in enumerate(cells):
                dx = min(abs(x - a), 16 - abs(x - a))
                dy = min(abs(y - b), 16 - abs(y - b))
                d = math.hypot(dx, dy)
                if d < best:
                    best, second, idx = d, best, i
                elif d < second:
                    second = d
            if second - best < 0.9:
                col = tex.shade(c, 0.55)
            else:
                col = tex.shade(c, 0.85 + 0.07 * (idx % 4), 0.02 * (idx % 3))
                if (x + y + idx) % 9 == 0:
                    col = tex.shade(c, 1.0, 0.3)
            px[x, y] = col
    return _fleck(img, seed, special, density=0.1)
