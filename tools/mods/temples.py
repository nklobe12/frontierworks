"""Lost Temples: a stepped sun pyramid in the desert, a jungle ziggurat and a frost shrine, all trapped."""
import math

from gen.fw import Mod
from gen import tex
from gen.structures import Struct, structure, loot_table, stairs, AIR
from mods._build import pick, lantern, ladder, torch_wall, scatter

M = 'fw_temples'
PLATE = 'minecraft:stone_pressure_plate[powered=false]'


def tnt_trap(s, x, y, z):
    """Pressure plate at (x,y,z) with TNT hidden two blocks under it (the floor block between conducts power)."""
    s.set(x, y, z, PLATE)
    s.set(x, y - 2, z, 'minecraft:tnt[unstable=false]')


def sun_pyramid(seed):
    n = 21
    s = Struct(n, 13, n, seed)
    sand = pick(s, ('minecraft:cut_sandstone', 6), ('minecraft:sandstone', 3), ('minecraft:smooth_sandstone', 2))
    s.fill(0, 0, 0, n - 1, 0, n - 1, 'minecraft:sandstone')
    for i in range(10):
        s.fill(i, i + 1, i, n - 1 - i, i + 1, n - 1 - i, sand)
    # grand stair up the south face
    for i in range(10):
        for x in (9, 10, 11):
            s.set(x, i + 1, n - 1 - i, stairs('sandstone', 'north'))
    # inner chamber (x/z 6..14, y 2..5) and the north tunnel
    s.clear(6, 2, 6, 14, 5, 14)
    s.fill(6, 1, 6, 14, 1, 14, 'minecraft:smooth_sandstone')
    s.fill(9, 1, 6, 11, 1, 14, 'minecraft:chiseled_sandstone')
    s.clear(9, 2, 0, 11, 4, 5)
    for z in (1, 4):
        torch_wall(s, 9, 3, z, 'east')
        torch_wall(s, 11, 3, z, 'west')
    tnt_trap(s, 10, 2, 3)
    tnt_trap(s, 10, 2, 11)
    for x in (7, 13):
        for z in (7, 13):
            s.set(x, 2, z, 'minecraft:chiseled_sandstone')
            s.set(x, 3, z, 'minecraft:chiseled_sandstone')
            s.set(x, 4, z, 'minecraft:orange_glazed_terracotta')
    # altar at the south end
    s.fill(8, 2, 13, 12, 2, 14, 'minecraft:smooth_sandstone')
    s.set(10, 3, 14, 'minecraft:gold_block')
    s.chest(8, 3, 14, M + ':chests/pyramid', 'north')
    s.chest(12, 3, 14, M + ':chests/pyramid', 'north')
    s.chest(6, 2, 10, M + ':chests/temple', 'east')
    s.chest(14, 2, 10, M + ':chests/temple', 'west')
    s.spawner(10, 2, 8, 'minecraft:husk')
    s.set(6, 2, 6, 'minecraft:decorated_pot')
    s.set(14, 2, 6, 'minecraft:decorated_pot')
    # summit
    s.set(10, 11, 10, 'minecraft:chiseled_sandstone')
    s.set(10, 12, 10, 'minecraft:lantern[hanging=false,waterlogged=false]')
    return s


def jungle_ziggurat(seed):
    n = 17
    s = Struct(n, 13, n, seed)
    rock = pick(s, ('minecraft:mossy_cobblestone', 5), ('minecraft:mossy_stone_bricks', 4), ('minecraft:cobblestone', 3), ('minecraft:cracked_stone_bricks', 1))
    s.fill(0, 0, 0, n - 1, 0, n - 1, 'minecraft:cobblestone')
    for t in range(4):
        s.fill(t * 2, 1 + t * 2, t * 2, n - 1 - t * 2, 2 + t * 2, n - 1 - t * 2, rock)
    # ramps up all four sides (air cleared above every step)
    for t in range(8):
        y = 1 + t
        for c in (7, 8, 9):
            for (x, z, f) in ((c, n - 1 - t, 'north'), (c, t, 'south'), (t, c, 'east'), (n - 1 - t, c, 'west')):
                s.set(x, y, z, stairs('mossy_cobblestone', f))
                s.clear(x, y + 1, z, x, 12, z)
    # summit shrine (summit floor is y=8)
    top = 9
    for (x, z) in ((6, 6), (10, 6), (6, 10), (10, 10)):
        for y in range(top, top + 3):
            s.set(x, y, z, 'minecraft:mossy_stone_bricks')
    s.fill(6, top + 3, 6, 10, top + 3, 10, 'minecraft:mossy_stone_brick_slab[type=bottom,waterlogged=false]')
    s.chest(8, top, 6, M + ':chests/temple', 'south')
    lantern(s, 8, top + 2, 8)
    # the shrine floor hides a trapdoor down to a buried chamber (x/z 5..11, y 1..4)
    s.set(8, top - 1, 8, 'minecraft:spruce_trapdoor[facing=south,half=top,open=false,powered=false,waterlogged=false]')
    s.clear(5, 1, 5, 11, 4, 11)
    s.fill(5, 0, 5, 11, 0, 11, 'minecraft:mossy_stone_bricks')
    for y in range(1, top - 1):
        s.set(8, y, 7, 'minecraft:mossy_stone_bricks')
    ladder(s, 8, 1, top - 2, 8, 'south')
    s.chest(6, 1, 6, M + ':chests/ziggurat', 'south')
    s.chest(10, 1, 10, M + ':chests/temple', 'west')
    s.chest(10, 1, 6, M + ':chests/temple', 'west')
    s.spawner(6, 1, 10, 'minecraft:cave_spider')
    torch_wall(s, 5, 2, 8, 'east')
    torch_wall(s, 11, 2, 8, 'west')
    s.set(8, 1, 10, 'minecraft:chiseled_stone_bricks')
    return s


def frost_shrine(seed):
    s = Struct(15, 9, 15, seed)
    c = 7
    ice = pick(s, ('minecraft:packed_ice', 5), ('minecraft:blue_ice', 1))
    s.fill(0, 0, 0, 14, 0, 14, 'minecraft:snow_block')
    for x in range(15):
        for z in range(15):
            if (x - c) ** 2 + (z - c) ** 2 <= 9:
                s.set(x, 0, z, 'minecraft:polished_diorite')
    for i in range(8):
        a = i * math.pi / 4
        x, z = int(round(c + 6 * math.cos(a))), int(round(c + 6 * math.sin(a)))
        for y in range(1, 4 + (i % 2) * 2):
            s.set(x, y, z, ice)
        s.set(x, 4 + (i % 2) * 2, z, 'minecraft:sea_lantern')
    s.set(c, 1, c, 'minecraft:chiseled_quartz_block')
    s.set(c, 2, c, 'minecraft:lodestone')
    s.set(c, 3, c, 'minecraft:lantern[hanging=false,waterlogged=false]')
    s.chest(c, 1, c + 1, M + ':chests/frost', 'south')
    s.spawner(c, 1, c - 2, 'minecraft:stray')
    return s


def build():
    m = Mod(M, 'Lost Temples', 'Stepped sun pyramids in the deserts, moss-eaten ziggurats in the jungles and frost shrines in the snow. '
                               'Mind the pressure plates.', tab='ADVENTURE')
    rl = m.rl
    m.item('sun_idol', ('trophy', '#e3b23c'), desc='A golden idol from a sun pyramid. Villagers will trade a fortune for it.', stack=16)
    m.item('jade_idol', ('trophy', '#3caa6a'), desc='A jade idol from a jungle ziggurat. Villagers will trade a fortune for it.', stack=16)
    m.item('frost_idol', ('trophy', '#9ad8f5'), desc='An ice-cold idol from a frost shrine. Villagers will trade a fortune for it.', stack=16)
    for idol in ('sun_idol', 'jade_idol', 'frost_idol'):
        m.mrecipe('fw_machines:pulverizing', idol, [(rl(idol), 1)], [('minecraft:gold_ingot', 4), ('minecraft:emerald', 2), ('minecraft:diamond', 1, 0.25)], time=160)
    base = [('minecraft:gold_ingot', 8, 2, 6), ('minecraft:iron_ingot', 6, 2, 5), ('minecraft:bone', 8, 2, 6), ('minecraft:rotten_flesh', 6, 2, 6),
            ('minecraft:emerald', 5, 1, 4), ('minecraft:gunpowder', 5, 2, 5), ('minecraft:book', 3, 1, 1, 'enchant'), ('minecraft:golden_apple', 2),
            ('minecraft:diamond', 2, 1, 2)]
    loot_table(m, 'chests/temple', [(3, 6, base)])
    loot_table(m, 'chests/pyramid', [(3, 6, base + [('fw_gems:topaz', 6, 1, 4), ('minecraft:golden_horse_armor', 2)]), (1, 1, [(rl('sun_idol'), 1)])])
    loot_table(m, 'chests/ziggurat', [(3, 6, base + [('fw_gems:peridot', 6, 1, 4), ('minecraft:cocoa_beans', 4, 2, 6), ('minecraft:bamboo', 4, 2, 8)]),
                                      (1, 1, [(rl('jade_idol'), 1)])])
    loot_table(m, 'chests/frost', [(3, 6, base + [('fw_gems:sapphire', 6, 1, 4), ('minecraft:blue_ice', 5, 2, 6), ('minecraft:diamond_boots', 1, 1, 1, 'enchant')]),
                                   (1, 1, [(rl('frost_idol'), 1)])])
    structure(m, 'sun_pyramid', [(sun_pyramid(1), 1)], ['#forge:is_desert'], spacing=48, separation=18)
    structure(m, 'jungle_ziggurat', [(jungle_ziggurat(2), 1)], ['#minecraft:is_jungle'], spacing=40, separation=14)
    structure(m, 'frost_shrine', [(frost_shrine(3), 1)], ['minecraft:snowy_plains', 'minecraft:ice_spikes', 'minecraft:snowy_taiga', 'minecraft:grove',
                                                          'minecraft:snowy_slopes'], spacing=36, separation=12)
    return m
