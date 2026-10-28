"""Kinetics: rotational power. Axles, gears, belts, wheels and windmills driving shaft-worked machines."""
import math

from PIL import Image, ImageDraw

from gen.ls import Mod, entity_block, element_model, recipe_kind, title, const
from gen import tex
from mods._materials import PART_METALS, METALS, VANILLA_METALS, ALLOYS, dust_item, raw_item

WOOD = '#9b6b3d'
WOOD_D = '#6a4526'
IRON = '#aab0b8'
IRON_D = '#5f656d'
COPPER = '#c27a44'
STONE = '#8d8b88'

FACING_ROT = {'up': {}, 'down': {'x': 180}, 'north': {'x': 90}, 'south': {'x': 90, 'y': 180}, 'east': {'x': 90, 'y': 90}, 'west': {'x': 90, 'y': 270}}
HORIZ_ROT = {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270}}
AXIS_ROT = {'y': {}, 'z': {'x': 90}, 'x': {'x': 90, 'y': 90}}


# ============================================================================ textures

def wood_grain(base, seed, vertical=True):
    img = tex.noise_fill(base, seed, 0.07)
    px = img.load()
    c = tex.rgb(base)
    for i in range(16):
        if tex.hsh(seed, 'g', i) % 4 == 0:
            for j in range(16):
                if tex.rnd(seed, i, j) < 0.8:
                    x, y = (i, j) if vertical else (j, i)
                    px[x, y] = tex.shade(c, 0.8)
    return img


def axle_side(seed):
    img = wood_grain(WOOD, seed)
    px = img.load()
    for y in (0, 1, 14, 15):
        for x in range(16):
            px[x, y] = tex.shade(tex.rgb(IRON), 1.0 if y in (0, 14) else 0.75)
    for x in range(16):
        px[x, 7] = tex.shade(tex.rgb(IRON_D), 1.0)
        px[x, 8] = tex.shade(tex.rgb(IRON), 0.9)
    return img


def axle_end(seed):
    img = tex.log_top(WOOD_D, '#c4935c', seed)
    px = img.load()
    for i in range(16):
        for j in (0, 15):
            px[i, j] = tex.shade(tex.rgb(IRON), 0.85)
            px[j, i] = tex.shade(tex.rgb(IRON), 0.85)
    return img


def gear_teeth(seed):
    img = tex.noise_fill(IRON_D, seed, 0.06)
    px = img.load()
    c = tex.rgb(IRON)
    for x in range(16):
        for y in range(16):
            if y < 3 or y > 12:
                px[x, y] = tex.shade(c, 1.05 if y < 3 else 0.7)
            elif x % 4 == 0:
                px[x, y] = tex.shade(c, 0.6)
    return img


def gear_hub(seed):
    img = tex.planks('#a9763f', seed)
    px = img.load()
    for i in range(16):
        for j in (0, 15):
            px[i, j] = tex.shade(tex.rgb(IRON_D), 1.0)
            px[j, i] = tex.shade(tex.rgb(IRON_D), 1.0)
    for (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
        px[x, y] = tex.shade(tex.rgb(IRON), 1.2)
    return img


def timber_casing(seed, port=True):
    """Oak frame with iron corner plates and a round shaft port."""
    img = tex.planks('#8c5f35', seed)
    px = img.load()
    fr = tex.rgb('#5a3b20')
    for i in range(16):
        for j in (0, 1, 14, 15):
            px[i, j] = tex.shade(fr, 1.1 if j in (0, 14) else 0.85)
            px[j, i] = tex.shade(fr, 1.1 if j in (0, 14) else 0.85)
    ir = tex.rgb(IRON)
    for (cx, cy) in ((0, 0), (12, 0), (0, 12), (12, 12)):
        for dx in range(4):
            for dy in range(4):
                if dx + dy < 5:
                    x = cx + (dx if cx == 0 else 3 - dx)
                    y = cy + (dy if cy == 0 else 3 - dy)
                    px[x, y] = tex.shade(ir, 1.15 - 0.05 * (dx + dy))
        px[cx + (1 if cx == 0 else 2), cy + (1 if cy == 0 else 2)] = tex.shade(ir, 0.5)
    if port:
        for x in range(16):
            for y in range(16):
                d = math.hypot(x - 7.5, y - 7.5)
                if d < 3.2:
                    px[x, y] = (26, 22, 20, 255)
                elif d < 4.4:
                    px[x, y] = tex.shade(ir, 0.95 if (x + y) % 3 else 0.7)
    return img


def iron_casing(seed, port=True, accent=None):
    img = tex.metal_block(IRON, seed, 'riveted')
    px = img.load()
    if accent:
        a = tex.rgb(accent)
        for x in range(2, 14):
            px[x, 2] = tex.shade(a, 1.0)
            px[x, 13] = tex.shade(a, 0.8)
    if port:
        for x in range(16):
            for y in range(16):
                d = math.hypot(x - 7.5, y - 7.5)
                if d < 3.0:
                    px[x, y] = (24, 24, 28, 255)
                elif d < 4.2:
                    px[x, y] = tex.shade(tex.rgb(IRON_D), 1.0)
    return img


def ratio_face(seed, out):
    img = iron_casing(seed, True, '#d4a640' if out else '#7a7f87')
    px = img.load()
    c = tex.rgb('#d4a640' if out else '#9aa0a8')
    # small gear teeth ring around the port
    for k in range(8):
        a = k * math.pi / 4
        x, y = int(round(7.5 + 5.6 * math.cos(a))), int(round(7.5 + 5.6 * math.sin(a)))
        px[x, y] = tex.shade(c, 1.2)
    return img


def coupling_tex(seed, on):
    img = tex.noise_fill(IRON_D, seed, 0.05)
    px = img.load()
    c = tex.rgb(IRON)
    lamp = (230, 60, 40, 255) if on else (90, 40, 36, 255)
    for x in range(16):
        px[x, 0] = px[x, 15] = tex.shade(c, 1.1)
        px[x, 1] = px[x, 14] = tex.shade(c, 0.8)
        for y in (6, 7, 8, 9):
            px[x, y] = lamp if y in (7, 8) else tex.shade(c, 0.6)
    for y in range(16):
        if y not in range(6, 10):
            px[0, y] = px[15, y] = tex.shade(c, 0.9)
    return img


def pulley_face(seed):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    w = tex.rgb('#a8743f')
    for x in range(16):
        for y in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            ang = math.atan2(y - 7.5, x - 7.5)
            if d < 8.2:
                spoke = abs(math.sin(ang * 3)) < 0.25
                px[x, y] = tex.shade(w, 1.0 + (tex.rnd(seed, x, y) - 0.5) * 0.15 if not spoke else 0.75)
            if 6.3 < d < 8.2:
                px[x, y] = tex.shade(tex.rgb(IRON), 0.9 + 0.1 * math.cos(ang * 2))
            if d < 2.4:
                px[x, y] = tex.shade(tex.rgb(IRON_D), 1.0)
    return img


def pulley_rim(seed):
    img = tex.noise_fill('#8a5c32', seed, 0.06)
    px = img.load()
    for x in range(16):
        for y in range(16):
            if y < 3 or y > 12:
                px[x, y] = tex.shade(tex.rgb(IRON), 1.0 if y in (0, 15) else 0.8)
            elif 6 <= y <= 9:
                px[x, y] = tex.shade(tex.rgb('#5a3b20'), 1.0)
    return img


def dial(seed, red_zone):
    img = tex.metal_block('#b9a26a', seed, 'plain')
    px = img.load()
    face = (236, 232, 218, 255)
    for x in range(16):
        for y in range(16):
            d = math.hypot(x - 7.5, y - 8.5)
            if d < 6.2:
                px[x, y] = face
            elif d < 7.0:
                px[x, y] = (60, 50, 34, 255)
    for k in range(9):
        a = math.radians(225 - k * 33.75)
        x, y = int(round(7.5 + 5.0 * math.cos(a))), int(round(8.5 - 5.0 * math.sin(a)))
        col = (200, 40, 32, 255) if red_zone and k >= 7 else (40, 40, 44, 255)
        if 0 <= x < 16 and 0 <= y < 16:
            px[x, y] = col
    if red_zone:
        for k in range(7, 9):
            a = math.radians(225 - k * 33.75)
            for r in (5.6, 6.0):
                x, y = int(round(7.5 + r * math.cos(a))), int(round(8.5 - r * math.sin(a)))
                if 0 <= x < 16 and 0 <= y < 16:
                    px[x, y] = (200, 40, 32, 255)
    px[7, 8] = px[8, 8] = (30, 30, 30, 255)
    return img


def gauge_end(seed):
    img = tex.metal_block('#b9a26a', seed, 'riveted')
    px = img.load()
    for x in range(16):
        for y in range(16):
            if math.hypot(x - 7.5, y - 7.5) < 2.6:
                px[x, y] = (30, 26, 22, 255)
    return img


def dark_iron(seed):
    img = tex.noise_fill('#4c5158', seed, 0.06)
    px = img.load()
    for x in range(16):
        px[x, 0] = tex.shade(tex.rgb('#7c838c'), 1.0)
        px[x, 15] = tex.shade(tex.rgb('#2f3237'), 1.0)
    return img


def handle_wood(seed):
    return tex.noise_fill('#7a4a28', seed, 0.1)


def paddle_board(seed):
    img = tex.planks('#a07142', seed)
    px = img.load()
    for y in range(16):
        px[0, y] = px[15, y] = tex.shade(tex.rgb(IRON_D), 1.0)
    return img


def vane_tex(seed):
    img = tex.noise_fill('#e8e0cc', seed, 0.05)
    px = img.load()
    for x in range(16):
        for y in range(16):
            if (x + y) % 2 == 0:
                r, g, b, a = px[x, y]
                px[x, y] = (tex.clamp(r * 0.96), tex.clamp(g * 0.96), tex.clamp(b * 0.95), 255)
    fr = tex.rgb('#7a5230')
    for i in range(16):
        for j in (0, 15):
            px[i, j] = tex.shade(fr, 1.0)
            px[j, i] = tex.shade(fr, 1.0)
        px[i, 7] = tex.shade(fr, 0.9)
        px[i, 8] = tex.shade(fr, 1.1)
    for i in range(1, 15):
        px[i, i] = tex.shade(tex.rgb('#cfc5ae'), 1.0)
    return img


def hub_side(seed):
    img = wood_grain('#7f552f', seed, vertical=False)
    px = img.load()
    for x in range(16):
        for y in (3, 4, 11, 12):
            px[x, y] = tex.shade(tex.rgb(IRON), 1.0 if y in (3, 11) else 0.75)
    return img


def hub_front(seed):
    img = timber_casing(seed, port=True)
    return img


def coil_side(seed, lit=False):
    img = tex.casing('basic', seed, 'side')
    px = img.load()
    cu = tex.rgb(COPPER)
    for y in range(3, 13):
        for x in range(3, 13):
            px[x, y] = tex.shade(cu, 1.15 if y % 2 else 0.8)
    for y in range(3, 13):
        px[2, y] = px[13, y] = tex.shade(tex.rgb(IRON_D), 1.0)
    return img


def motor_top(seed):
    img = tex.casing('basic', seed, 'top')
    px = img.load()
    for x in range(16):
        for y in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 3.3:
                px[x, y] = (26, 26, 30, 255)
            elif d < 4.5:
                px[x, y] = tex.shade(tex.rgb(COPPER), 1.0)
    return img


def dynamo_front(seed):
    img = tex.casing('basic', seed, 'side')
    px = img.load()
    for y in range(3, 13):
        for x in range(3, 13):
            px[x, y] = (40, 34, 30, 255) if y % 3 == 0 else tex.shade(tex.rgb(COPPER), 0.95 + 0.1 * ((x + y) % 2))
    return img


def mill_face(seed):
    img = tex.stone(STONE, seed, 'smooth')
    px = img.load()
    for x in range(16):
        for y in range(16):
            ang = math.atan2(y - 7.5, x - 7.5)
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 2 and (ang * 8 / math.pi) % 2 < 0.35:
                r, g, b, a = px[x, y]
                px[x, y] = (tex.clamp(r * 0.72), tex.clamp(g * 0.72), tex.clamp(b * 0.72), 255)
            if d < 1.6:
                px[x, y] = (40, 36, 32, 255)
    return img


def mill_side(seed):
    img = tex.stone(STONE, seed, 'layered')
    px = img.load()
    for x in range(16):
        px[x, 0] = tex.shade(tex.rgb(STONE), 1.15)
        px[x, 15] = tex.shade(tex.rgb(STONE), 0.65)
    for x in range(0, 16, 5):
        for y in range(16):
            r, g, b, a = px[x, y]
            px[x, y] = (tex.clamp(r * 0.85), tex.clamp(g * 0.85), tex.clamp(b * 0.85), 255)
    return img


def mill_base(seed):
    img = tex.bricks('#9a958e', seed + 'b')
    px = img.load()
    fr = tex.rgb('#6a4526')
    for x in range(16):
        px[x, 0] = px[x, 1] = tex.shade(fr, 1.0 if x % 2 else 0.85)
    return img


def press_bed(seed):
    img = tex.noise_fill('#5a5f66', seed, 0.05)
    px = img.load()
    for x in range(16):
        for y in range(16):
            if x % 4 == 0 or y % 4 == 0:
                px[x, y] = tex.shade(tex.rgb('#3c4046'), 1.0)
    for i in range(16):
        px[i, 0] = px[0, i] = tex.shade(tex.rgb(IRON), 1.1)
    return img


def press_frame(seed):
    img = tex.metal_block('#8f969e', seed, 'riveted')
    px = img.load()
    for y in range(16):
        for x in (6, 9):
            px[x, y] = tex.shade(tex.rgb('#5f656d'), 1.0)
    return img


def press_ram(seed):
    img = tex.noise_fill('#6f757d', seed, 0.04)
    px = img.load()
    for i in range(16):
        px[i, 0] = tex.shade(tex.rgb('#c9ced4'), 1.0)
        px[i, 15] = tex.shade(tex.rgb('#3a3e44'), 1.0)
    return img


def vat_side(seed):
    img = tex.planks('#c27a44', seed)
    px = img.load()
    band = tex.rgb('#5f656d')
    for x in range(16):
        for y in (1, 2, 12, 13):
            px[x, y] = tex.shade(band, 1.1 if y in (1, 12) else 0.85)
        if x % 8 == 4:
            px[x, 1] = px[x, 12] = tex.shade(tex.rgb(IRON), 1.3)
    return img


def vat_inner(seed):
    return tex.noise_fill('#7c4a28', seed, 0.06)


def vat_top(seed):
    img = vat_inner(seed + 't')
    px = img.load()
    c = tex.rgb('#c27a44')
    for i in range(16):
        for j in (0, 1, 14, 15):
            px[i, j] = tex.shade(c, 1.1 if j in (0, 15) else 0.9)
            px[j, i] = tex.shade(c, 1.1 if j in (0, 15) else 0.9)
    return img


def blower_side(seed):
    img = tex.metal_block('#9da4ac', seed, 'plain')
    px = img.load()
    for y in range(4, 12, 2):
        for x in range(3, 13):
            px[x, y] = (40, 42, 46, 255)
    return img


def blower_grille(seed):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    c = tex.rgb('#b4bac2')
    for x in range(16):
        for y in range(16):
            ring = x in (0, 15) or y in (0, 15)
            bar = (x % 4 == 1) and 1 < y < 14
            if ring or bar:
                px[x, y] = tex.shade(c, 1.0 if ring else 0.85)
    return img


def blade_tex(seed):
    img = tex.noise_fill('#c9ced4', seed, 0.05)
    px = img.load()
    for x in range(16):
        px[x, 0] = px[x, 15] = tex.shade(tex.rgb('#6f757d'), 1.0)
    return img


def drive_belt_item(seed):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    lt, dk = (150, 96, 54, 255), (92, 56, 30, 255)
    d.ellipse([1, 3, 14, 13], outline=dk, width=3)
    d.ellipse([2, 4, 13, 12], outline=lt, width=1)
    d.line([4, 3, 11, 3], fill=(172, 116, 70, 255))
    return img


def build_textures(m):
    s = m.modid
    t = {
        'axle': axle_side(s + 'axle'), 'axle_end': axle_end(s + 'axe'),
        'gear_teeth': gear_teeth(s + 'gt'), 'gear_hub': gear_hub(s + 'gh'),
        'timber_casing': timber_casing(s + 'tc'), 'timber_casing_plain': timber_casing(s + 'tcp', port=False),
        'gear_train_in': ratio_face(s + 'gti', False), 'gear_train_out': ratio_face(s + 'gto', True),
        'iron_casing': iron_casing(s + 'ic', False),
        'coupling': coupling_tex(s + 'cp', False), 'coupling_on': coupling_tex(s + 'cp', True),
        'pulley_face': pulley_face(s + 'pf'), 'pulley_rim': pulley_rim(s + 'pr'),
        'tachometer_dial': dial(s + 'td', False), 'dynamometer_dial': dial(s + 'dd', True), 'gauge_end': gauge_end(s + 'ge'),
        'dark_iron': dark_iron(s + 'di'), 'handle_wood': handle_wood(s + 'hw'),
        'paddle_board': paddle_board(s + 'pb'),
        'windmill_vane': vane_tex(s + 'wv'), 'windmill_hub_side': hub_side(s + 'whs'), 'windmill_hub_front': hub_front(s + 'whf'),
        'motor_side': coil_side(s + 'ms'), 'motor_top': motor_top(s + 'mt'), 'motor_bottom': tex.casing('basic', s + 'mb', 'bottom'),
        'dynamo_side': coil_side(s + 'ds'), 'dynamo_front': dynamo_front(s + 'df'), 'dynamo_top': tex.casing('basic', s + 'dt', 'top'),
        'millstone_face': mill_face(s + 'mf'), 'millstone_side': mill_side(s + 'msd'), 'mill_base': mill_base(s + 'mbs'),
        'press_bed': press_bed(s + 'pbd'), 'press_frame': press_frame(s + 'pfr'), 'press_ram': press_ram(s + 'prm'),
        'vat_side': vat_side(s + 'vs'), 'vat_inner': vat_inner(s + 'vi'), 'vat_top': vat_top(s + 'vt'),
        'blower_side': blower_side(s + 'bs'), 'blower_grille': blower_grille(s + 'bg'), 'blower_back': iron_casing(s + 'bb', True),
        'blade': blade_tex(s + 'bl'),
    }
    for k, v in t.items():
        m.texture('block/' + k, v)
    return t


# ============================================================================ models

def T(m, n):
    return '%s:block/%s' % (m.modid, n)


def faces(tex_ref, uv=None, skip=()):
    out = {}
    for d in ('north', 'south', 'east', 'west', 'up', 'down'):
        if d in skip:
            continue
        f = {'texture': tex_ref}
        if uv:
            f['uv'] = uv
        out[d] = f
    return out


def el(frm, to, fc, rot=None):
    e = {'from': list(frm), 'to': list(to), 'faces': fc}
    if rot is not None:
        e['rotation'] = {'origin': [8, 8, 8], 'axis': rot[0], 'angle': rot[1]}
    return e


def model(textures, elements, display=None, render='cutout'):
    mdl = {'parent': 'minecraft:block/block', 'textures': textures, 'elements': elements}
    if render:
        mdl['render_type'] = 'minecraft:' + render
    if display:
        mdl['display'] = display
    return mdl


def rod_faces(side, end):
    return {'north': {'texture': side, 'uv': [6, 0, 10, 16]}, 'south': {'texture': side, 'uv': [6, 0, 10, 16]},
            'east': {'texture': side, 'uv': [6, 0, 10, 16]}, 'west': {'texture': side, 'uv': [6, 0, 10, 16]},
            'up': {'texture': end, 'uv': [6, 6, 10, 10]}, 'down': {'texture': end, 'uv': [6, 6, 10, 10]}}


def axle_el(y0=0, y1=16, r=2):
    a, b = 8 - r, 8 + r
    return el([a, y0, a], [b, y1, b], {
        'north': {'texture': '#axle', 'uv': [a, 16 - y1, b, 16 - y0]}, 'south': {'texture': '#axle', 'uv': [a, 16 - y1, b, 16 - y0]},
        'east': {'texture': '#axle', 'uv': [a, 16 - y1, b, 16 - y0]}, 'west': {'texture': '#axle', 'uv': [a, 16 - y1, b, 16 - y0]},
        'up': {'texture': '#axle_end', 'uv': [a, a, b, b]}, 'down': {'texture': '#axle_end', 'uv': [a, a, b, b]}})


def build_models(m):
    t = lambda n: T(m, n)
    ax = {'axle': t('axle'), 'axle_end': t('axle_end'), 'particle': t('axle')}
    M = {}
    disp_small = {'gui': {'rotation': [30, 45, 0], 'scale': [0.625, 0.625, 0.625]}}
    M['axle_rotor'] = model(ax, [axle_el()], display=disp_small)
    M['axle'] = {'parent': 'minecraft:block/block', 'textures': {'particle': t('axle')}}
    # spur gear: axle, octagonal hub, eight teeth
    gtx = dict(ax, gear=t('gear_hub'), teeth=t('gear_teeth'), particle=t('gear_hub'))
    hub = lambda r=None: el([3, 6, 3], [13, 10, 13], faces('#gear'), r)
    teeth = lambda r=None: [el([0, 6.5, 7], [16, 9.5, 9], faces('#teeth', [0, 6, 16, 9]), r), el([7, 6.5, 0], [9, 9.5, 16], faces('#teeth', [0, 6, 16, 9]), r)]
    M['spur_gear_rotor'] = model(gtx, [axle_el(), hub(), hub(('y', 45))] + teeth() + teeth(('y', 45)))
    M['spur_gear'] = {'parent': 'minecraft:block/block', 'textures': {'particle': t('gear_hub')}}
    # pulley
    ptx = dict(ax, face=t('pulley_face'), rim=t('pulley_rim'), particle=t('pulley_face'))
    def disc(y0, y1, r, rot=None):
        a, b = 8 - r, 8 + r
        f = {'up': {'texture': '#face'}, 'down': {'texture': '#face'}}
        for d in ('north', 'south', 'east', 'west'):
            f[d] = {'texture': '#rim', 'uv': [a, 16 - y1, b, 16 - y0]}
        return el([a, y0, a], [b, y1, b], f, rot)
    M['pulley_rotor'] = model(ptx, [axle_el(), disc(3, 5, 7), disc(3, 5, 7, ('y', 45)), disc(11, 13, 7), disc(11, 13, 7, ('y', 45)),
                                    disc(5, 11, 5), disc(5, 11, 5, ('y', 45))])
    M['pulley'] = {'parent': 'minecraft:block/block', 'textures': {'particle': t('pulley_face')}}
    # coupling: static collar (axle drawn by the renderer)
    for on in (False, True):
        n = 'coupling' + ('_on' if on else '')
        ctx = dict(ax, collar=t(n), cend=t('iron_casing'), particle=t('coupling'))
        collar = el([4, 4, 4], [12, 12, 12], {'north': {'texture': '#collar'}, 'south': {'texture': '#collar'}, 'east': {'texture': '#collar'},
                                              'west': {'texture': '#collar'}, 'up': {'texture': '#cend'}, 'down': {'texture': '#cend'}})
        M[n] = model(ctx, [collar])
        if not on:
            M['coupling_item'] = model(ctx, [collar, axle_el()])
    # gauges
    for g in ('tachometer', 'dynamometer'):
        gtx2 = dict(ax, dial=t(g + '_dial'), gend=t('gauge_end'), particle=t(g + '_dial'))
        housing = el([3, 3, 3], [13, 13, 13], {'north': {'texture': '#dial', 'uv': [3, 3, 13, 13]}, 'south': {'texture': '#dial', 'uv': [3, 3, 13, 13]},
                                               'east': {'texture': '#dial', 'uv': [3, 3, 13, 13]}, 'west': {'texture': '#dial', 'uv': [3, 3, 13, 13]},
                                               'up': {'texture': '#gend', 'uv': [3, 3, 13, 13]}, 'down': {'texture': '#gend', 'uv': [3, 3, 13, 13]}})
        M[g] = model(gtx2, [housing])
        M[g + '_item'] = model(gtx2, [housing, axle_el()])
    # bevel junction and gear train cases; shaft stubs drawn by the renderer
    M['shaft_stub'] = model(ax, [axle_el(12, 16, 2.5)])
    M['bevel_junction'] = model({'case': t('timber_casing'), 'particle': t('timber_casing')}, [el([1, 1, 1], [15, 15, 15], faces('#case'))])
    M['bevel_junction_item'] = model(dict(ax, case=t('timber_casing'), particle=t('timber_casing')),
                                     [el([1, 1, 1], [15, 15, 15], faces('#case'))] + [axle_el(0, 1, 2.5), axle_el(15, 16, 2.5)])
    gt = {'case': t('timber_casing_plain'), 'in': t('gear_train_in'), 'out': t('gear_train_out'), 'particle': t('gear_train_out')}
    M['gear_train'] = model(gt, [el([1, 1, 1], [15, 15, 15], {'north': {'texture': '#case'}, 'south': {'texture': '#case'}, 'east': {'texture': '#case'},
                                                             'west': {'texture': '#case'}, 'up': {'texture': '#out'}, 'down': {'texture': '#in'}})])
    # hand wheel: mount (static) and wheel (renderer); model faces up
    hw = dict(ax, iron=t('dark_iron'), wood=t('handle_wood'), particle=t('dark_iron'))
    wheel = [axle_el(0, 7, 2),
             el([1, 5, 1], [15, 7, 3], faces('#iron')), el([1, 5, 13], [15, 7, 15], faces('#iron')),
             el([1, 5, 3], [3, 7, 13], faces('#iron')), el([13, 5, 3], [15, 7, 13], faces('#iron')),
             el([3, 5.5, 7], [13, 6.5, 9], faces('#iron')), el([7, 5.5, 3], [9, 6.5, 13], faces('#iron')),
             el([11, 7, 11], [13, 11, 13], faces('#wood'))]
    mount = [el([5, 0, 5], [11, 2, 11], faces('#iron'))]
    M['hand_wheel'] = model(hw, mount)
    M['hand_wheel_rotor'] = model(hw, wheel)
    M['hand_wheel_item'] = model(hw, mount + wheel)
    # paddle wheel: three blocks across; model along Y (the axle)
    pw = dict(ax, board=t('paddle_board'), spoke=t('handle_wood'), particle=t('paddle_board'))
    def spoke(rot):
        fx = {d: {'texture': '#spoke', 'uv': [0, 7, 16, 9]} for d in ('north', 'south', 'east', 'west', 'up', 'down')}
        return [el([-16, 7, 7], [32, 9, 9], fx, rot), el([7, 7, -16], [9, 9, 32], fx, rot)]
    def boards(rot):
        fb = {d: {'texture': '#board', 'uv': [0, 1, 10, 15]} for d in ('north', 'south', 'east', 'west')}
        fb['up'] = {'texture': '#board', 'uv': [0, 0, 10, 2]}
        fb['down'] = {'texture': '#board', 'uv': [0, 0, 10, 2]}
        return [el([22, 1, 7], [32, 15, 9], fb, rot), el([-16, 1, 7], [-6, 15, 9], fb, rot),
                el([7, 1, 22], [9, 15, 32], fb, rot), el([7, 1, -16], [9, 15, -6], fb, rot)]
    pw_els = [axle_el(), el([4, 2, 4], [12, 14, 12], faces('#spoke'))] + spoke(None) + spoke(('y', 45)) + boards(None) + boards(('y', 45))
    big = {'gui': {'rotation': [30, 45, 0], 'scale': [0.3, 0.3, 0.3]}, 'ground': {'scale': [0.2, 0.2, 0.2]},
           'fixed': {'scale': [0.3, 0.3, 0.3]}, 'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.2, 0.2, 0.2]},
           'firstperson_righthand': {'rotation': [0, 45, 0], 'scale': [0.25, 0.25, 0.25]}}
    M['paddle_wheel_rotor'] = model(pw, pw_els, display=big)
    M['paddle_wheel'] = {'parent': 'minecraft:block/block', 'textures': {'particle': t('paddle_board')}}
    # windmill hub (faces north) and the rotor head drawn in the sail plane
    wh = dict(ax, side=t('windmill_hub_side'), front=t('windmill_hub_front'), particle=t('windmill_hub_side'))
    M['windmill_hub'] = model(wh, [el([1, 1, 1], [15, 15, 15], {'north': {'texture': '#front'}, 'south': {'texture': '#front'},
                                                               'east': {'texture': '#side'}, 'west': {'texture': '#side'},
                                                               'up': {'texture': '#side'}, 'down': {'texture': '#side'}})])
    M['windmill_rotor'] = model(dict(ax, cap=t('windmill_hub_front'), spar=t('handle_wood')), [
        el([4, 4, 3], [12, 12, 9], faces('#cap')), el([6, 6, 9], [10, 10, 16], faces('#spar')),
        el([0, 7, 4], [16, 9, 8], faces('#spar')), el([7, 0, 4], [9, 16, 8], faces('#spar'))])
    # motor (faces up: shaft on top)
    mt = {'side': t('motor_side'), 'top': t('motor_top'), 'bottom': t('motor_bottom'), 'particle': t('motor_side')}
    M['kinetic_motor'] = model(mt, [el([2, 0, 2], [14, 13, 14], {'north': {'texture': '#side'}, 'south': {'texture': '#side'}, 'east': {'texture': '#side'},
                                                                 'west': {'texture': '#side'}, 'up': {'texture': '#top'}, 'down': {'texture': '#bottom'}}),
                                    el([1, 0, 1], [15, 2, 15], faces('#bottom'))])
    M['kinetic_motor_item'] = model(dict(mt, **ax), M['kinetic_motor']['elements'] + [axle_el(12, 16, 2.5)])
    # grist mill: stone base (static) and the runner stone (renderer)
    gm = dict(ax, base=t('mill_base'), face=t('millstone_face'), side=t('millstone_side'), particle=t('millstone_side'))
    base = el([0, 0, 0], [16, 7, 16], {'north': {'texture': '#base', 'uv': [0, 9, 16, 16]}, 'south': {'texture': '#base', 'uv': [0, 9, 16, 16]},
                                       'east': {'texture': '#base', 'uv': [0, 9, 16, 16]}, 'west': {'texture': '#base', 'uv': [0, 9, 16, 16]},
                                       'up': {'texture': '#face'}, 'down': {'texture': '#base'}})
    def runner(rot=None):
        return el([1, 7.2, 1], [15, 13, 15], {'north': {'texture': '#side', 'uv': [1, 3, 15, 9]}, 'south': {'texture': '#side', 'uv': [1, 3, 15, 9]},
                                              'east': {'texture': '#side', 'uv': [1, 3, 15, 9]}, 'west': {'texture': '#side', 'uv': [1, 3, 15, 9]},
                                              'up': {'texture': '#face'}, 'down': {'texture': '#face'}}, rot)
    stone = [runner(), runner(('y', 45)), axle_el(13, 16, 2)]
    M['grist_mill'] = model(gm, [base])
    M['grist_mill_stone'] = model(gm, stone)
    M['grist_mill_item'] = model(gm, [base] + stone)
    # stamping press: bed, corner pillars and head (static); ram (renderer)
    sp = dict(ax, bed=t('press_bed'), frame=t('press_frame'), ram=t('press_ram'), particle=t('press_frame'))
    frame = [el([0, 0, 0], [16, 4, 16], {'north': {'texture': '#frame', 'uv': [0, 12, 16, 16]}, 'south': {'texture': '#frame', 'uv': [0, 12, 16, 16]},
                                         'east': {'texture': '#frame', 'uv': [0, 12, 16, 16]}, 'west': {'texture': '#frame', 'uv': [0, 12, 16, 16]},
                                         'up': {'texture': '#bed'}, 'down': {'texture': '#frame'}})]
    for (x, z) in ((0, 0), (13, 0), (0, 13), (13, 13)):
        frame.append(el([x, 4, z], [x + 3, 13, z + 3], faces('#frame', [0, 0, 3, 9])))
    frame.append(el([0, 13, 0], [16, 16, 16], {'north': {'texture': '#frame', 'uv': [0, 0, 16, 3]}, 'south': {'texture': '#frame', 'uv': [0, 0, 16, 3]},
                                               'east': {'texture': '#frame', 'uv': [0, 0, 16, 3]}, 'west': {'texture': '#frame', 'uv': [0, 0, 16, 3]},
                                               'up': {'texture': '#frame'}, 'down': {'texture': '#bed'}}))
    ram = [el([3, 0, 3], [13, 3, 13], faces('#ram')), el([6.5, 3, 6.5], [9.5, 8, 9.5], faces('#frame', [6, 0, 9, 5]))]
    M['stamping_press'] = model(sp, frame)
    M['stamping_press_ram'] = model(sp, ram)
    shifted = [dict(e, **{'from': [e['from'][0], e['from'][1] + 5, e['from'][2]], 'to': [e['to'][0], e['to'][1] + 5, e['to'][2]]}) for e in ram]
    M['stamping_press_item'] = model(sp, frame + shifted)
    # mixing vat: open copper tub (static), paddle (renderer)
    mv = {'side': t('vat_side'), 'inner': t('vat_inner'), 'top': t('vat_top'), 'blade': t('blade'), 'particle': t('vat_side')}
    def wall(frm, to, outer):
        f = {}
        for d in ('north', 'south', 'east', 'west'):
            f[d] = {'texture': '#side' if d == outer else '#inner'}
        f['up'] = {'texture': '#top'}
        f['down'] = {'texture': '#side'}
        return el(frm, to, f)
    vat = [el([0, 0, 0], [16, 2, 16], {'north': {'texture': '#side', 'uv': [0, 14, 16, 16]}, 'south': {'texture': '#side', 'uv': [0, 14, 16, 16]},
                                       'east': {'texture': '#side', 'uv': [0, 14, 16, 16]}, 'west': {'texture': '#side', 'uv': [0, 14, 16, 16]},
                                       'up': {'texture': '#inner'}, 'down': {'texture': '#side'}}),
           wall([0, 2, 0], [16, 16, 2], 'north'), wall([0, 2, 14], [16, 16, 16], 'south'),
           wall([0, 2, 2], [2, 16, 14], 'west'), wall([14, 2, 2], [16, 16, 14], 'east')]
    paddle = [el([7.5, 3, 7.5], [8.5, 16, 8.5], faces('#blade')), el([3, 3, 7.5], [13, 8, 8.5], faces('#blade')),
              el([7.5, 3, 3], [8.5, 8, 13], faces('#blade'))]
    M['mixing_vat'] = model(mv, vat)
    M['mixing_vat_paddle'] = model(mv, paddle)
    # air blower (faces up: the stream leaves the top)
    ab = {'side': t('blower_side'), 'grille': t('blower_grille'), 'back': t('blower_back'), 'blade': t('blade'), 'particle': t('blower_side')}
    def shell(frm, to, outer):
        f = {}
        for d in ('north', 'south', 'east', 'west'):
            f[d] = {'texture': '#side'}
        f['up'] = {'texture': '#side', 'uv': [0, 0, 16, 2]}
        f['down'] = {'texture': '#side', 'uv': [0, 0, 16, 2]}
        return el(frm, to, f)
    housing = [shell([0, 0, 0], [16, 16, 2], 'north'), shell([0, 0, 14], [16, 16, 16], 'south'), shell([0, 0, 2], [2, 16, 14], 'west'),
               shell([14, 0, 2], [16, 16, 14], 'east'), el([2, 0, 2], [14, 2, 14], {'up': {'texture': '#back'}, 'down': {'texture': '#back'},
                                                                                  'north': {'texture': '#side'}, 'south': {'texture': '#side'},
                                                                                  'east': {'texture': '#side'}, 'west': {'texture': '#side'}}),
               el([2, 14.5, 2], [14, 15.5, 14], {'up': {'texture': '#grille', 'uv': [2, 2, 14, 14]}, 'down': {'texture': '#grille', 'uv': [2, 2, 14, 14]}})]
    blades = [el([6, 6, 6], [10, 10, 10], faces('#back')),
              el([2, 7.5, 6.5], [14, 8.5, 9.5], faces('#blade')), el([6.5, 7.5, 2], [9.5, 8.5, 14], faces('#blade')),
              el([2, 7.5, 6.5], [14, 8.5, 9.5], faces('#blade'), ('y', 45)), el([6.5, 7.5, 2], [9.5, 8.5, 14], faces('#blade'), ('y', 45)),
              el([7, 2, 7], [9, 6, 9], faces('#back'))]
    M['air_blower'] = model(ab, housing)
    M['air_blower_rotor'] = model(ab, blades)
    M['air_blower_item'] = model(ab, housing + blades)
    for k, v in M.items():
        m.asset('models/block/%s.json' % k, v)
    return M


def axis_state(m, model_name, extra=None):
    v = {}
    for a, r in AXIS_ROT.items():
        if extra:
            for ev, mn in extra.items():
                v['axis=%s,%s' % (a, ev)] = dict({'model': T(m, mn)}, **r)
        else:
            v['axis=%s' % a] = dict({'model': T(m, model_name)}, **r)
    return {'variants': v}


def facing_state(m, model_name):
    return {'variants': {'facing=%s' % f: dict({'model': T(m, model_name)}, **r) for f, r in FACING_ROT.items()}}


def horiz_state(m, model_name, lit=False):
    v = {}
    for f, r in HORIZ_ROT.items():
        if lit:
            v['facing=%s,lit=false' % f] = dict({'model': T(m, model_name)}, **r)
            v['facing=%s,lit=true' % f] = dict({'model': T(m, model_name + '_on')}, **r)
        else:
            v['facing=%s' % f] = dict({'model': T(m, model_name)}, **r)
    return {'variants': v}


# ============================================================================ mod

def build():
    m = Mod('ls_kinetics', 'Kinetics', 'Rotational power. Turn axles with hand wheels, paddle wheels in moving water, windmill sails high in '
                                      'the wind or electric motors; carry it with spur gears, bevel junctions, gear trains, couplings and '
                                      'belted pulleys; and drive a grist mill, a stamping press, a mixing vat and an air blower that '
                                      'rinses, roasts, sears and soul-steeps items in its stream. Every network balances torque against '
                                      'load and stalls when overloaded; kinetic dynamos and motors trade rotation with Forge Energy.',
            tab='INDUSTRY')
    m.init('KineticsSetup.init(bus)')
    m.client('com.lodestar.kinetics.client.KineticsClient.init(bus)')
    rl = m.rl
    build_textures(m)
    build_models(m)
    M = lambda n: 'ls_kinetics:block/' + n

    kinds = {n: recipe_kind(m, n) for n in ('grinding', 'stamping', 'mixing', 'heated_mixing', 'rinsing', 'steeping')}

    wood = 'LSProps.wood().noOcclusion()'
    metal = 'LSProps.metal(2.5f).noOcclusion()'

    def node(name, be, java, jtype, models_state, item_model, desc, lang=None, ctor='', mine='axe', level=None):
        entity_block(m, name, be, java, jtype=jtype, models={}, state=models_state, item_model=item_model, desc=desc, lang=lang,
                     ctor_args=ctor, mine=mine, level=level)

    # ---------------------------------------------------------------- transmission
    node('axle', 'AxleBE', 'new KineticAxisBlock(%s, BE_TYPE, 2.5f)' % wood, 'KineticAxisBlock', axis_state(m, 'axle'), 'block:axle_rotor',
         'Carries rotation along its length\nUse with an empty hand to read speed and load', ctor='AxleBE.Mode.AXLE')
    node('spur_gear', 'SpurGearBE', 'new KineticAxisBlock(%s, BE_TYPE, 7f)' % wood, 'KineticAxisBlock', axis_state(m, 'spur_gear'),
         'block:spur_gear_rotor', 'An axle with a gear on it\nMeshes with spur gears beside it on the same axis,\nwhich turn the other way')
    node('bevel_junction', 'BevelJunctionBE', 'new LSEntityBlock(LSProps.wood(), BE_TYPE).noMenu().noBuckets()', 'LSEntityBlock',
         {'variants': {'': {'model': T(m, 'bevel_junction')}}}, 'block:bevel_junction_item',
         'A shaft on every face: turns rotation round corners\nShafts leaving opposite faces turn opposite ways')
    node('gear_train', 'GearTrainBE', 'new FacingKineticBlock(%s, BE_TYPE, false, new double[]{1, 1, 1, 15, 15, 15})' % metal, 'FacingKineticBlock',
         facing_state(m, 'gear_train'), 'block:gear_train', 'Input on the grey back, output on the gold front\nUse: change the ratio (1:4 to 4:1)\n'
         'Sneak-use: reverse the output\nGearing up costs torque: power is kept', mine='pickaxe')
    node('coupling', 'AxleBE', 'new CouplingBlock(%s, BE_TYPE, 4f)' % metal, 'CouplingBlock',
         axis_state(m, None, {'powered=false': 'coupling', 'powered=true': 'coupling_on'}), 'block:coupling_item',
         'Joins the shafts on its two ends\nA redstone signal uncouples them', ctor='AxleBE.Mode.COUPLING', mine='pickaxe')
    node('pulley', 'PulleyBE', 'new KineticAxisBlock(%s, BE_TYPE, 7f)' % wood, 'KineticAxisBlock', axis_state(m, 'pulley'), 'block:pulley_rotor',
         'Belt it to another pulley on a parallel axle\nup to 24 blocks away with a drive belt')
    m.item('drive_belt', drive_belt_item(m.modid + 'belt'), java='new DriveBeltItem(new Item.Properties().stacksTo(16))', jtype='DriveBeltItem',
           desc='Use on a pulley, then on a second pulley\nThe two turn together at the same speed')
    node('tachometer', 'AxleBE', 'new KineticAxisBlock(%s, BE_TYPE, 5f)' % metal, 'KineticAxisBlock', axis_state(m, 'tachometer'), 'block:tachometer_item',
         'An axle with a speed dial (full scale 256 RPM)\nComparators read 1 per 16 RPM', ctor='AxleBE.Mode.TACHOMETER', mine='pickaxe')
    node('dynamometer', 'AxleBE', 'new KineticAxisBlock(%s, BE_TYPE, 5f)' % metal, 'KineticAxisBlock', axis_state(m, 'dynamometer'), 'block:dynamometer_item',
         'An axle with a load dial: how much of the\nnetwork\'s torque is in use\nComparators read the load (15 = overloaded)',
         ctor='AxleBE.Mode.DYNAMOMETER', mine='pickaxe')

    # ---------------------------------------------------------------- sources
    node('hand_wheel', 'HandWheelBE', 'new FacingKineticBlock(%s, BE_TYPE, true, new double[]{1, 0, 1, 15, 7, 15})' % wood, 'FacingKineticBlock',
         facing_state(m, 'hand_wheel'), 'block:hand_wheel_item', 'Mount it on a shaft and hold use to turn it\n24 RPM, 64 Nm. Sneak to turn it the other way')
    node('paddle_wheel', 'PaddleWheelBE', 'new KineticAxisBlock(%s, BE_TYPE, 6f, true)' % wood, 'KineticAxisBlock', axis_state(m, 'paddle_wheel'),
         'block:paddle_wheel_rotor', 'Three blocks across: moving water in the ring\naround the axle turns it, up to 16 RPM, 256 Nm\n'
         'Keep the ring clear of blocks')
    node('windmill_hub', 'WindmillHubBE', 'new LSEntityBlock(LSProps.wood(), BE_TYPE, LSEntityBlock.Facing.HORIZONTAL, false).noMenu().noBuckets()',
         'LSEntityBlock', horiz_state(m, 'windmill_hub'), 'block:windmill_hub',
         'Build at least 8 windmill vanes on the face in front,\nthen use the hub to raise the sail\nHigher, wetter and windier is stronger')
    m.block('windmill_vane', java='new Block(LSProps.wood().strength(0.8f))', tab='INDUSTRY', mine='axe',
            models={'windmill_vane': {'parent': 'minecraft:block/cube_all', 'textures': {'all': T(m, 'windmill_vane')}}},
            desc='Sailcloth on a frame. Lay vanes edge to edge\nin front of a windmill hub')
    node('kinetic_motor', 'MotorBE', 'new FacingKineticBlock(LSProps.machine().noOcclusion(), BE_TYPE, false, new double[]{1, 0, 1, 15, 13, 15})',
         'FacingKineticBlock', facing_state(m, 'kinetic_motor'), 'block:kinetic_motor_item',
         'Turns Forge Energy into rotation on its shaft\nUse: pick 16 to 256 RPM. Sneak-use: reverse\n512 Nm; draws power for the torque used\nRedstone stops it',
         mine='pickaxe', level='stone')

    # ---------------------------------------------------------------- machines
    spec = lambda n, layout, src: m.field('public static final MachineSpec %s_SPEC = new MachineSpec(KineticRecipes.%s(), %s, 0, 0, 1f, 1f);'
                                          % (const(n), layout, src))
    spec('grist_mill', 'millLayout', 'RecipeSource.machine(() -> Content.GRINDING)')
    spec('stamping_press', 'pressLayout', 'RecipeSource.machine(() -> Content.STAMPING)')
    spec('mixing_vat', 'vatLayout', 'KineticRecipes.vat()')
    for name, kind, desc, props in (
            ('grist_mill', 'MILL', 'Grinds grain, stone, bone and ores\nDrive it with a shaft from above or below\nLoads the network with 48 Nm', 'LSProps.stone(2.0f).noOcclusion()'),
            ('stamping_press', 'PRESS', 'Stamps ingots into plates\nDrive it with a shaft from above\nLoads the network with 64 Nm', 'LSProps.metal(3.0f).noOcclusion()'),
            ('mixing_vat', 'VAT', 'Mixes items and fluids; heated recipes need\nfire, lava or a campfire underneath\nDrive it with a shaft from above. 64 Nm',
             'LSProps.metal(2.5f).noOcclusion()')):
        java = 'new LSEntityBlock(%s, BE_TYPE, LSEntityBlock.Facing.HORIZONTAL, false)%s' % (props, '' if name == 'mixing_vat' else '.noBuckets()')
        entity_block(m, name, 'KineticMachineBE', java, jtype='LSEntityBlock', models={}, state=horiz_state(m, name),
                     item_model='block:%s_item' % name, desc=desc,
                     ctor_args='Content.%s_SPEC, KineticMachineBE.Kind.%s' % (const(name), kind), mine='pickaxe', level='stone' if name != 'grist_mill' else None)
    entity_block(m, 'air_blower', 'AirBlowerBE', 'new FacingKineticBlock(%s, BE_TYPE, false)' % metal, jtype='FacingKineticBlock', models={},
                 state=facing_state(m, 'air_blower'), item_model='block:air_blower_item',
                 desc='Blows air forward: 1 block per 8 RPM, up to 16\nPushes mobs and items; reverse it to pull\nItems carried through water, fire, lava or\nsoul fire are rinsed, roasted, seared or steeped\nShaft on any face but the front. 32 Nm',
                 mine='pickaxe', level='stone')
    dyn_models = {
        'kinetic_dynamo': {'parent': 'minecraft:block/orientable_with_bottom', 'textures': {
            'top': T(m, 'dynamo_top'), 'bottom': T(m, 'motor_bottom'), 'side': T(m, 'dynamo_side'), 'front': T(m, 'dynamo_front')}},
        'kinetic_dynamo_on': {'parent': 'minecraft:block/orientable_with_bottom', 'textures': {
            'top': T(m, 'dynamo_top'), 'bottom': T(m, 'motor_bottom'), 'side': T(m, 'dynamo_side'), 'front': T(m, 'dynamo_front')}}}
    entity_block(m, 'kinetic_dynamo', 'KineticDynamoBE', 'new MachineBlock(LSProps.machine(), BE_TYPE)', jtype='MachineBlock', models=dyn_models,
                 state=horiz_state(m, 'kinetic_dynamo', lit=True), desc='A shaft into its back makes Forge Energy:\nhalf its RPM in FE/t, for 128 Nm of load\n'
                 'Pushes energy into every neighbour', mine='pickaxe', level='stone')

    # ---------------------------------------------------------------- materials
    m.item('flour', ('dust', '#ece5d3'), tags=['forge:flour', 'forge:dusts/wheat'], tab='INDUSTRY')
    m.item('dough', ('ball', '#dcc08f'), tags=['forge:dough'], tab='INDUSTRY', desc='Bake it into bread')
    m.smelt('bread_from_dough', rl('dough'), 'minecraft:bread', xp=0.35, time=200, blast=False, smoke=True)
    m.tag('blocks', 'ls_kinetics:vat_heaters', 'minecraft:fire', 'minecraft:soul_fire', 'minecraft:lava', 'minecraft:magma_block',
          'minecraft:campfire', 'minecraft:soul_campfire')

    # ---------------------------------------------------------------- crafting
    S = m.shaped
    S('axle', rl('axle'), ['P', 'N', 'P'], {'P': '#minecraft:planks', 'N': 'minecraft:iron_nugget'}, count=4)
    S('spur_gear', rl('spur_gear'), [' P ', 'PAP', ' P '], {'P': '#minecraft:planks', 'A': rl('axle')}, count=2)
    S('bevel_junction', rl('bevel_junction'), ['LGL', 'G G', 'LGL'], {'L': '#minecraft:logs', 'G': rl('spur_gear')})
    S('gear_train', rl('gear_train'), ['IGI', 'A A', 'IGI'], {'I': '#forge:ingots/iron', 'G': '#forge:gears/iron', 'A': rl('axle')})
    S('coupling', rl('coupling'), [' R ', 'AIA'], {'R': 'minecraft:redstone', 'A': rl('axle'), 'I': '#forge:ingots/iron'})
    S('pulley', rl('pulley'), [' S ', 'SAS', ' S '], {'S': '#minecraft:wooden_slabs', 'A': rl('axle')}, count=2)
    S('drive_belt', rl('drive_belt'), ['LLL', 'K K', 'LLL'], {'L': 'minecraft:leather', 'K': 'minecraft:dried_kelp'}, count=2)
    S('tachometer', rl('tachometer'), [' C ', 'IAI'], {'C': 'minecraft:compass', 'I': '#forge:ingots/copper', 'A': rl('axle')})
    S('dynamometer', rl('dynamometer'), [' C ', 'IAI'], {'C': 'minecraft:clock', 'I': '#forge:ingots/copper', 'A': rl('axle')})
    S('hand_wheel', rl('hand_wheel'), [' S ', 'SAS', ' C '], {'S': '#forge:rods/wooden', 'A': rl('axle'), 'C': 'minecraft:cobblestone'})
    S('paddle_wheel', rl('paddle_wheel'), ['PSP', 'SAS', 'PSP'], {'P': '#minecraft:planks', 'S': '#minecraft:wooden_slabs', 'A': rl('axle')})
    S('windmill_hub', rl('windmill_hub'), ['LGL', 'GAG', 'LIL'], {'L': '#minecraft:logs', 'G': rl('spur_gear'), 'A': rl('axle'), 'I': '#forge:ingots/iron'})
    S('windmill_vane', rl('windmill_vane'), ['SW', 'SW', 'SW'], {'S': '#forge:rods/wooden', 'W': '#minecraft:wool'}, count=2)
    S('kinetic_motor', rl('kinetic_motor'), ['PWP', 'WAW', 'PCP'], {'P': '#forge:plates/iron', 'W': '#forge:wires/copper', 'A': rl('axle'),
                                                                   'C': 'ls_machina:redstone_coil'})
    S('kinetic_dynamo', rl('kinetic_dynamo'), ['PCP', 'WAW', 'PGP'], {'P': '#forge:plates/iron', 'C': 'ls_machina:redstone_coil',
                                                                     'W': '#forge:wires/copper', 'A': rl('axle'), 'G': '#forge:gears/copper'})
    S('grist_mill', rl('grist_mill'), [' A ', 'SGS', 'SSS'], {'A': rl('axle'), 'G': rl('spur_gear'), 'S': 'minecraft:smooth_stone'})
    S('stamping_press', rl('stamping_press'), ['IAI', 'PKP', 'SSS'], {'I': '#forge:ingots/iron', 'A': rl('axle'), 'P': '#forge:plates/iron',
                                                                     'K': 'minecraft:piston', 'S': 'minecraft:smooth_stone'})
    S('mixing_vat', rl('mixing_vat'), ['CAC', 'C C', 'CKC'], {'C': '#forge:ingots/copper', 'A': rl('axle'), 'K': 'minecraft:cauldron'})
    S('air_blower', rl('air_blower'), ['IBI', 'BGB', 'IAI'], {'I': '#forge:ingots/iron', 'B': 'minecraft:iron_bars', 'G': rl('spur_gear'), 'A': rl('axle')})

    # ---------------------------------------------------------------- machine recipes (time = ticks at 32 RPM)
    grind = lambda n, ing, outs, t=160: m.mrecipe('ls_kinetics:grinding', n, [(ing, 1)], outs, time=t, energy=0)
    grind('wheat', 'minecraft:wheat', [(rl('flour'), 1), (rl('flour'), 1, 0.25)], 100)
    grind('cobblestone', 'minecraft:cobblestone', [('minecraft:gravel', 1)])
    grind('gravel', 'minecraft:gravel', [('minecraft:sand', 1), ('minecraft:flint', 1, 0.1)])
    grind('sandstone', 'minecraft:sandstone', [('minecraft:sand', 2)], 120)
    grind('bone', 'minecraft:bone', [('minecraft:bone_meal', 5)], 100)
    grind('sugar_cane', 'minecraft:sugar_cane', [('minecraft:sugar', 2)], 80)
    grind('clay', 'minecraft:clay', [('minecraft:clay_ball', 4)], 80)
    grind('wool', '#minecraft:wool', [('minecraft:string', 3)], 120)
    grind('glowstone', 'minecraft:glowstone', [('minecraft:glowstone_dust', 4)], 120)
    grind('blaze_rod', 'minecraft:blaze_rod', [('minecraft:blaze_powder', 3)], 120)
    grind('coal', 'minecraft:coal', [('ls_veins:coal_dust', 1)], 120)
    grind('charcoal', 'minecraft:charcoal', [('ls_veins:coal_dust', 1)], 120)
    grind('obsidian', 'minecraft:obsidian', [('ls_veins:obsidian_dust', 2)], 400)
    for f, dye, n in (('dandelion', 'yellow', 3), ('poppy', 'red', 3), ('blue_orchid', 'light_blue', 3), ('allium', 'magenta', 3),
                      ('azure_bluet', 'light_gray', 3), ('red_tulip', 'red', 3), ('orange_tulip', 'orange', 3), ('white_tulip', 'light_gray', 3),
                      ('pink_tulip', 'pink', 3), ('oxeye_daisy', 'light_gray', 3), ('cornflower', 'blue', 3), ('lily_of_the_valley', 'white', 3),
                      ('wither_rose', 'black', 3), ('sunflower', 'yellow', 5), ('lilac', 'magenta', 5), ('rose_bush', 'red', 5), ('peony', 'pink', 5),
                      ('cactus', 'green', 2), ('cocoa_beans', 'brown', 2), ('ink_sac', 'black', 2), ('lapis_lazuli', 'blue', 2)):
        grind(f, 'minecraft:' + f, [('minecraft:%s_dye' % dye, n)], 80)
    for metal in list(VANILLA_METALS) + list(METALS):
        grind(metal + '_ore', '#forge:ores/' + metal, [(dust_item(metal), 1), (dust_item(metal), 1, 0.5)], 240)
        grind(metal + '_raw', '#forge:raw_materials/' + metal, [(dust_item(metal), 1, 1.1)], 200)

    stamp = lambda n, ing, cnt, out, t=120: m.mrecipe('ls_kinetics:stamping', n, [(ing, cnt)], [out], time=t, energy=0)
    for metal in PART_METALS:
        stamp(metal + '_plate', '#forge:ingots/' + metal, 1, ('ls_metalwork:%s_plate' % metal, 1))
    stamp('paper', 'minecraft:sugar_cane', 3, ('minecraft:paper', 4), 80)
    stamp('snow_block', 'minecraft:snowball', 4, ('minecraft:snow_block', 1), 40)
    stamp('dried_kelp_block', 'minecraft:dried_kelp', 9, ('minecraft:dried_kelp_block', 1), 60)

    mix = lambda n, ins, outs, t=160, fi=None, fi2=None, fo=None: m.mrecipe('ls_kinetics:mixing', n, ins, outs, time=t, energy=0,
                                                                           fluid_in=fi, fluid_in2=fi2, fluid_out=fo)
    for name, (color, recipe, out) in ALLOYS.items():
        if not recipe or name == 'cobalt_steel':
            continue
        mix(name + '_dust', [('#forge:dusts/' + a, k) for a, k in recipe.items()], [(dust_item(name), out)], 200)
    mix('dough', [(rl('flour'), 3)], [(rl('dough'), 2)], 100, fi=('minecraft:water', 250))
    mix('mud', [('minecraft:dirt', 1)], [('minecraft:mud', 1)], 60, fi=('minecraft:water', 250))
    mix('clay', [('#minecraft:sand', 1), ('minecraft:gravel', 1)], [('minecraft:clay', 1)], 200, fi=('minecraft:water', 500))
    mix('obsidian', [], [('minecraft:obsidian', 1)], 200, fi=('minecraft:lava', 1000), fi2=('minecraft:water', 1000))
    mix('slime_ball', [('minecraft:kelp', 2), ('minecraft:sugar', 1)], [('minecraft:slime_ball', 1)], 200, fi=('minecraft:water', 250))
    mix('fire_charge', [('minecraft:blaze_powder', 1), ('ls_veins:coal_dust', 1), ('minecraft:gunpowder', 1)], [('minecraft:fire_charge', 3)], 120)
    hmix = lambda n, ins, outs, t=200, fi=None, fo=None: m.mrecipe('ls_kinetics:heated_mixing', n, ins, outs, time=t, energy=0, fluid_in=fi, fluid_out=fo)
    for name, (color, recipe, out) in ALLOYS.items():
        if not recipe:
            continue
        hmix('molten_' + name, [('#forge:ingots/' + a, k) for a, k in recipe.items()], [], 240, fo=('ls_toolsmith:molten_' + name, 144 * out))
    hmix('steel', [('#forge:ingots/iron', 1), ('#forge:coal_coke', 1)], [('ls_metalwork:steel_ingot', 1)], 300)
    hmix('glass_bottles', [('#minecraft:sand', 1)], [('minecraft:glass_bottle', 3)], 160)
    hmix('magma_cream', [('minecraft:slime_ball', 1), ('minecraft:blaze_powder', 1)], [('minecraft:magma_cream', 1)], 160)
    hmix('lava', [('minecraft:cobblestone', 4), ('minecraft:netherrack', 1)], [], 400, fo=('minecraft:lava', 1000))

    rinse = lambda n, ing, outs, t=100: m.mrecipe('ls_kinetics:rinsing', n, [(ing, 1)], outs, time=t, energy=0)
    rinse('gravel', 'minecraft:gravel', [('minecraft:flint', 1, 0.3), ('minecraft:iron_nugget', 1, 0.12)])
    rinse('sand', 'minecraft:sand', [('minecraft:clay_ball', 1, 0.25), ('minecraft:gold_nugget', 1, 0.05)])
    rinse('red_sand', 'minecraft:red_sand', [('minecraft:gold_nugget', 1, 0.15), ('minecraft:dead_bush', 1, 0.05)])
    rinse('soul_sand', 'minecraft:soul_sand', [('minecraft:sand', 1), ('minecraft:quartz', 1, 0.1)])
    rinse('dirt', 'minecraft:dirt', [('minecraft:clay_ball', 1, 0.3), ('minecraft:wheat_seeds', 1, 0.1)])
    rinse('magma_block', 'minecraft:magma_block', [('minecraft:obsidian', 1)])
    rinse('wet_sponge', 'minecraft:sponge', [('minecraft:wet_sponge', 1)])
    rinse('copper_block', 'minecraft:copper_block', [('minecraft:exposed_copper', 1)])
    rinse('exposed_copper', 'minecraft:exposed_copper', [('minecraft:weathered_copper', 1)])
    rinse('weathered_copper', 'minecraft:weathered_copper', [('minecraft:oxidized_copper', 1)])
    for c in ('white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown',
              'green', 'red', 'black'):
        rinse(c + '_concrete', 'minecraft:%s_concrete_powder' % c, [('minecraft:%s_concrete' % c, 1)])
        if c != 'white':
            rinse(c + '_wool', 'minecraft:%s_wool' % c, [('minecraft:white_wool', 1)])
            rinse(c + '_terracotta', 'minecraft:%s_terracotta' % c, [('minecraft:terracotta', 1)])
    steep = lambda n, ing, outs, t=100: m.mrecipe('ls_kinetics:steeping', n, [(ing, 1)], outs, time=t, energy=0)
    steep('soul_sand', 'minecraft:sand', [('minecraft:soul_sand', 1)])
    steep('soul_soil', 'minecraft:dirt', [('minecraft:soul_soil', 1)])
    steep('blackstone', 'minecraft:cobblestone', [('minecraft:blackstone', 1)])
    steep('soul_torch', 'minecraft:torch', [('minecraft:soul_torch', 1)])
    steep('soul_lantern', 'minecraft:lantern', [('minecraft:soul_lantern', 1)])
    steep('soul_campfire', 'minecraft:campfire', [('minecraft:soul_campfire', 1)])
    steep('leather', 'minecraft:rotten_flesh', [('minecraft:leather', 1, 0.5)])
    steep('crimson_fungus', 'minecraft:red_mushroom', [('minecraft:crimson_fungus', 1)])
    steep('warped_fungus', 'minecraft:brown_mushroom', [('minecraft:warped_fungus', 1)])
    steep('wither_rose', 'minecraft:poppy', [('minecraft:wither_rose', 1)])
    steep('nether_wart', 'minecraft:beetroot_seeds', [('minecraft:nether_wart', 1, 0.2)])

    # ---------------------------------------------------------------- lang
    for k, v in {'grinding': 'Grist Mill', 'stamping': 'Stamping Press', 'mixing': 'Mixing Vat', 'heated_mixing': 'Heated Mixing Vat',
                 'rinsing': 'Air Blower: Rinsing', 'steeping': 'Air Blower: Soul Steeping'}.items():
        m.tr('lens.category.ls_kinetics.' + k, v)
    for k, v in {
        'msg.ls_kinetics.speed': '%s RPM', 'msg.ls_kinetics.load': 'load %s / %s Nm (%s%%)',
        'msg.ls_kinetics.overloaded': 'Overloaded: the machines need more torque than the sources give',
        'msg.ls_kinetics.jammed': 'Jammed: parts are turning against each other',
        'msg.ls_kinetics.uncoupled': 'Uncoupled by redstone',
        'msg.ls_kinetics.dynamometer': 'Load %s / %s Nm (%s%%) at %s RPM',
        'msg.ls_kinetics.gear_train': 'Gear train %s (output : input)', 'msg.ls_kinetics.gear_train_rev': 'Gear train %s, reversed',
        'msg.ls_kinetics.belt_first': 'Now use the belt on a second pulley', 'msg.ls_kinetics.belt_cancel': 'Belt put away',
        'msg.ls_kinetics.belt_lost': 'The first pulley is gone', 'msg.ls_kinetics.belt_done': 'Belt fitted across %s blocks',
        'msg.ls_kinetics.belt_same': 'Pick a different pulley', 'msg.ls_kinetics.belt_taken': 'That pulley already has a belt',
        'msg.ls_kinetics.belt_axis': 'Both pulleys need parallel axles', 'msg.ls_kinetics.belt_plane': 'The pulleys must line up side by side, not along the axle',
        'msg.ls_kinetics.belt_close': 'Too close: leave at least one block between them', 'msg.ls_kinetics.belt_far': 'Too far: at most 24 blocks',
        'msg.ls_kinetics.paddle': 'Paddle wheel: %s RPM from %s wet paddles', 'msg.ls_kinetics.paddle_still': 'No moving water around the paddles',
        'msg.ls_kinetics.paddle_blocked': 'Blocked: keep the ring around the axle clear',
        'msg.ls_kinetics.windmill': 'Sail of %s vanes, wind %s%%: %s RPM, %s Nm',
        'msg.ls_kinetics.windmill_idle': 'Lay vanes in front of the hub and use it to raise the sail',
        'msg.ls_kinetics.windmill_none': 'Put a windmill vane right in front of the hub first',
        'msg.ls_kinetics.windmill_small': 'A sail needs at least %s vanes', 'msg.ls_kinetics.windmill_big': 'A sail can have at most %2$s vanes',
        'msg.ls_kinetics.windmill_blocked': 'The sail is blocked: clear its circle',
        'msg.ls_kinetics.motor': 'Motor: %s RPM %s, %s FE stored, drawing %s FE/t',
        'msg.ls_kinetics.cw': 'forward', 'msg.ls_kinetics.ccw': 'reversed',
        'msg.ls_kinetics.blower': 'Air blower: %s RPM, stream %s blocks, %s',
        'air.ls_kinetics.none': 'plain air', 'air.ls_kinetics.rinse': 'rinsing', 'air.ls_kinetics.roast': 'roasting',
        'air.ls_kinetics.sear': 'searing', 'air.ls_kinetics.steep': 'soul steeping',
        'gui.ls_kinetics.rpm': '%s RPM', 'gui.ls_kinetics.no_drive': 'No shaft turning', 'gui.ls_kinetics.overloaded': 'Overloaded',
        'gui.ls_kinetics.jammed': 'Jammed', 'gui.ls_kinetics.heated': 'Heated', 'gui.ls_kinetics.dynamo_hint': 'Half the shaft RPM in FE/t',
        'tooltip.ls_kinetics.belt_from': 'From pulley at %s, %s, %s',
        'lens.ls_kinetics.at32': 'time at 32 RPM', 'lens.ls_kinetics.heat': 'Needs heat below', 'lens.ls_kinetics.stream': 'Air stream through',
    }.items():
        m.tr(k, v)

    # ---------------------------------------------------------------- codex
    m.codex_intro('Kinetics is power you can see: shafts turning at a speed in RPM and carrying torque in Nm. Sources (hand wheels, paddle '
                  'wheels, windmills, motors) decide the speed and how much torque the network can give; machines at the ends of its '
                  'shafts each need a fixed torque. If they need more than the sources give, the whole network stalls until you add '
                  'sources or remove machines. Use any part with an empty hand to read its speed and load.')
    m.codex_page('Shafts and gears', 'Axles carry rotation in a straight line. A spur gear is an axle with a gear on it: spur gears side by '
                                     'side on the same axis mesh and turn opposite ways. A bevel junction has a shaft on every face and turns '
                                     'rotation round corners; shafts leaving opposite faces counter-rotate. A coupling joins two shafts and '
                                     'parts them while it gets a redstone signal.\n\nPulleys carry rotation across a gap: use a drive belt on '
                                     'one pulley and then on another on a parallel axle, side by side up to 24 blocks away.',
                 items=[rl('axle'), rl('spur_gear'), rl('bevel_junction'), rl('coupling'), rl('pulley'), rl('drive_belt')])
    m.codex_page('Gearing and torque', 'A gear train changes speed: its gold front turns 1/4, 1/2, 1, 2 or 4 times as fast as its grey '
                                       'back (use it to change, sneak-use to reverse). Power is kept, not made: a machine geared up to twice '
                                       'the speed works twice as fast but loads the network with twice its torque. Gearing a slow, strong '
                                       'water wheel up is how you run fast machines.\n\nIf two sources try to turn the network opposite ways, '
                                       'or a loop of gears disagrees with itself, the network jams. The tachometer shows speed and the '
                                       'dynamometer shows how much of the torque is in use; comparators read both.',
                 items=[rl('gear_train'), rl('tachometer'), rl('dynamometer')])
    m.codex_page('Natural power', 'Hold use on a hand wheel to turn it: 24 RPM and 64 Nm while you keep at it. A paddle wheel is three '
                                  'blocks across; water flowing under it or falling past one side turns it, up to 16 RPM with 256 Nm. Keep '
                                  'the ring of eight blocks around the axle open.\n\nFor a windmill, lay at least 8 windmill vanes edge to edge '
                                  'on the face in front of a windmill hub, within 8 blocks of its centre, and use the hub to raise the sail. '
                                  'Bigger sails turn faster (up to 16 RPM) and give 24 Nm per vane at full wind. Wind grows with height, rain '
                                  'and thunder; vanes must see the sky and the swept circle must stay clear. Use the hub to reverse it; '
                                  'sneak-use to take the sail down.',
                 items=[rl('hand_wheel'), rl('paddle_wheel'), rl('windmill_hub'), rl('windmill_vane')])
    m.codex_page('Mill, press and vat', 'The grist mill grinds grain into flour, stone into gravel and sand, bones, flowers and ores; drive '
                                        'it from above or below. The stamping press stamps ingots into plates one for one, much cheaper than by '
                                        'hand. The mixing vat mixes up to four items with up to two fluids: alloy dusts, dough, mud, clay and '
                                        'obsidian. Put fire, lava, a magma block or a lit campfire under the vat for heated recipes: molten '
                                        'alloys straight from ingots, steel and more. Recipe times are given at 32 RPM; faster shafts work faster.',
                 items=[rl('grist_mill'), rl('stamping_press'), rl('mixing_vat'), rl('flour'), rl('dough')])
    m.codex_page('The air blower', 'The air blower drives a stream of air forward, one block for every 8 RPM up to 16 blocks, until a full '
                                   'block stops it. It pushes mobs and items along; turn it the other way to pull. Items that ride the stream '
                                   'after it passes through a block of water are rinsed (sifting gravel and sand, washing dyes out, weathering '
                                   'copper); through fire or a lit campfire they are roasted like in a smoker; through lava they are seared '
                                   'like in a furnace; through soul fire they are steeped in soul flame.',
                 items=[rl('air_blower')])
    m.codex_page('Energy', 'The kinetic dynamo turns a shaft in its back into Forge Energy: half its RPM in FE/t for 128 Nm of load. The '
                           'kinetic motor does the opposite: it turns its shaft at a chosen speed with up to 512 Nm, drawing energy for the '
                           'torque actually used (with a quarter lost), and stops on a redstone signal.',
                 items=[rl('kinetic_dynamo'), rl('kinetic_motor')])
    return m
