"""Frontier Decor: industrial and homestead building blocks - steel plating, hazard stripes, grates, tiles, bricks, beams and crates."""
from gen.fw import Mod, column, stairs_slab_wall
from gen import tex

M = 'fw_decor'


def recolor(img, fn):
    px = img.load()
    for y in range(img.size[1]):
        for x in range(img.size[0]):
            r, g, b, a = px[x, y]
            px[x, y] = fn(x, y, (r, g, b, a))
    return img


def hazard(seed):
    img = tex.noise_fill('#e2b81e', seed, 0.06)
    dark = tex.rgb('#26262c')
    return recolor(img, lambda x, y, c: (dark[0], dark[1], dark[2], 255) if ((x + y) // 4) % 2 else c)


def checker(seed):
    img = tex.noise_fill('#e8e8e8', seed, 0.04)
    dark = tex.rgb('#2a2a30')
    return recolor(img, lambda x, y, c: (dark[0], dark[1], dark[2], 255) if ((x // 8) + (y // 8)) % 2 else c)


def grate(seed):
    img = tex.metal_block('#5f6670', seed, 'plain')
    return recolor(img, lambda x, y, c: (0, 0, 0, 0) if (x % 4 in (1, 2) and y % 4 in (1, 2)) else c)


def vent(seed):
    img = tex.metal_block('#6b737d', seed, 'riveted')
    return recolor(img, lambda x, y, c: (28, 28, 32, 255) if (2 < x < 13 and y % 3 == 0 and 1 < y < 14) else c)


def framed_planks(seed):
    img = tex.planks('#a0784a', seed)
    edge = tex.rgb('#4a3420')
    return recolor(img, lambda x, y, c: (edge[0], edge[1], edge[2], 255) if (x in (0, 15) or y in (0, 15)) else c)


def crate(seed):
    img = tex.planks('#b08a56', seed)
    edge = tex.rgb('#5a4028')
    return recolor(img, lambda x, y, c: (edge[0], edge[1], edge[2], 255) if (x in (0, 1, 14, 15) or y in (0, 1, 14, 15) or x == y or x == 15 - y) else c)


def beam_side(seed):
    img = tex.metal_block('#7b8591', seed, 'plated')
    dk = tex.rgb('#3e444c')
    return recolor(img, lambda x, y, c: (dk[0], dk[1], dk[2], 255) if x in (4, 11) else c)


def rust(seed):
    img = tex.metal_block('#8a5a3a', seed, 'riveted')
    return recolor(img, lambda x, y, c: tex.shade(c[:3], 0.8 + tex.rnd(seed, x, y) * 0.4) if tex.rnd(seed + 'r', x, y) > 0.6 else c)


BLOCKS = [
    # name, texture, props, has stairs/slab, wall, mine
    ('steel_plating', lambda: tex.metal_block('#7b8591', 'stpl', 'plated'), 'FWProps.metal(4.0f)', True, False, 'pickaxe'),
    ('riveted_steel', lambda: tex.metal_block('#737d88', 'rivs', 'riveted'), 'FWProps.metal(4.0f)', False, False, 'pickaxe'),
    ('rusted_plating', lambda: rust('rust'), 'FWProps.metal(3.0f)', True, False, 'pickaxe'),
    ('hazard_block', lambda: hazard('haz'), 'FWProps.metal(3.0f)', True, False, 'pickaxe'),
    ('vent_block', lambda: vent('vent'), 'FWProps.metal(3.0f)', False, False, 'pickaxe'),
    ('concrete_tiles', lambda: tex.tiles('#9a9a9c', 'ctile'), 'FWProps.stone(1.8f)', True, False, 'pickaxe'),
    ('dark_tiles', lambda: tex.tiles('#34353b', 'dtile'), 'FWProps.stone(1.8f)', True, False, 'pickaxe'),
    ('checkered_tiles', lambda: checker('chk'), 'FWProps.stone(1.8f)', True, False, 'pickaxe'),
    ('industrial_bricks', lambda: tex.bricks('#7a4a3a', 'ibrick', '#9a9a9a'), 'FWProps.stone(2.0f)', True, True, 'pickaxe'),
    ('frontier_bricks', lambda: tex.bricks('#b89a6a', 'fbrick', '#e6dcc8'), 'FWProps.stone(2.0f)', True, True, 'pickaxe'),
    ('polished_slate', lambda: tex.polished('#4c5560', 'pslate'), 'FWProps.stone(2.0f)', True, True, 'pickaxe'),
    ('chiseled_slate', lambda: tex.chiseled('#4c5560', 'cslate'), 'FWProps.stone(2.0f)', False, False, 'pickaxe'),
    ('framed_planks', lambda: framed_planks('fplank'), 'FWProps.wood()', True, False, 'axe'),
    ('cargo_crate', lambda: crate('crate'), 'FWProps.wood()', False, False, 'axe'),
]


def build():
    m = Mod(M, 'Frontier Decor', 'Building blocks for bases and homesteads: steel plating, rusted panels, hazard stripes, vents, '
                                 'grates, tiles, bricks, slate, steel beams and cargo crates, with stairs, slabs and walls.', tab='DECOR')
    rl = m.rl
    for name, tx, props, ssw, wall, mine in BLOCKS:
        m.block(name, java='new Block(%s)' % props, textures={'block/' + name: tx()}, mine=mine, tab='DECOR')
        if ssw:
            stairs_slab_wall(m, name, '%s:block/%s' % (M, name), wall=wall, props=props, mine=mine)
    m.block('steel_grate', java='new GlassBlock(FWProps.metal(3.0f).noOcclusion())', jtype='GlassBlock', tab='DECOR',
            models={'steel_grate': {'parent': 'minecraft:block/cube_all', 'render_type': 'minecraft:cutout', 'textures': {'all': '%s:block/steel_grate' % M}}},
            textures={'block/steel_grate': grate('grate')}, desc='See-through metal flooring')
    column(m, 'steel_beam', beam_side('beam'), tex.metal_block('#5f6670', 'beame', 'riveted'), props='FWProps.metal(4.0f)', tab='DECOR')
    # recipes
    S = '#forge:plates/steel'
    m.shaped('steel_plating', rl('steel_plating'), ['PP', 'PP'], {'P': S}, 4)
    m.shapeless('riveted_steel', rl('riveted_steel'), [rl('steel_plating'), '#forge:nuggets/iron'])
    m.shapeless('rusted_plating', rl('rusted_plating'), [rl('steel_plating'), 'minecraft:water_bucket'])
    m.shaped('hazard_block', rl('hazard_block'), ['YPB', 'PPP', 'BPY'], {'Y': 'minecraft:yellow_dye', 'B': 'minecraft:black_dye', 'P': rl('steel_plating')}, 6)
    m.shaped('vent_block', rl('vent_block'), ['PBP', 'B B', 'PBP'], {'P': rl('steel_plating'), 'B': 'minecraft:iron_bars'}, 4)
    m.shaped('steel_grate', rl('steel_grate'), ['BPB', 'PBP', 'BPB'], {'P': rl('steel_plating'), 'B': 'minecraft:iron_bars'}, 8)
    m.shaped('steel_beam', rl('steel_beam'), ['P', 'P', 'P'], {'P': rl('steel_plating')}, 3)
    m.shaped('concrete_tiles', rl('concrete_tiles'), ['CC', 'CC'], {'C': 'minecraft:light_gray_concrete'}, 4)
    m.shaped('dark_tiles', rl('dark_tiles'), ['CC', 'CC'], {'C': 'minecraft:black_concrete'}, 4)
    m.shaped('checkered_tiles', rl('checkered_tiles'), ['WB', 'BW'], {'W': 'minecraft:white_concrete', 'B': 'minecraft:black_concrete'}, 4)
    m.shaped('industrial_bricks', rl('industrial_bricks'), ['BG', 'GB'], {'B': 'minecraft:bricks', 'G': 'minecraft:gravel'}, 4)
    m.shaped('frontier_bricks', rl('frontier_bricks'), ['BS', 'SB'], {'B': 'minecraft:bricks', 'S': '#minecraft:sand'}, 4)
    m.shaped('polished_slate', rl('polished_slate'), ['SS', 'SS'], {'S': 'minecraft:polished_deepslate'}, 4)
    m.shaped('chiseled_slate', rl('chiseled_slate'), ['S', 'S'], {'S': rl('polished_slate_slab')})
    m.shaped('framed_planks', rl('framed_planks'), ['SPS', 'P P', 'SPS'], {'S': '#forge:rods/wooden', 'P': '#minecraft:planks'}, 4)
    m.shaped('cargo_crate', rl('cargo_crate'), ['PSP', 'S S', 'PSP'], {'S': '#forge:rods/wooden', 'P': '#minecraft:planks'}, 2)
    for name in ('steel_plating', 'riveted_steel', 'hazard_block', 'vent_block', 'steel_grate', 'steel_beam'):
        if name != 'steel_plating':
            m.recipe('stonecutting/%s' % name, {'type': 'minecraft:stonecutting', 'ingredient': {'item': rl('steel_plating')}, 'result': rl(name), 'count': 1})
    return m
