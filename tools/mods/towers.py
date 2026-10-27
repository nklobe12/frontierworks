"""Towers: wizard towers, wooden watchtowers, sandstone desert towers and frozen ice spires."""
import math

from gen.fw import Mod
from gen import tex
from gen.structures import Struct, structure, loot_table, foundation, stairs, AIR
from mods._build import pick, ladder, lantern, round_tower, battlements, door, pane, fence_line, torch_wall

M = 'fw_towers'
FORESTS = ['#minecraft:is_forest', '#minecraft:is_taiga', '#forge:is_plains', 'minecraft:meadow', 'minecraft:windswept_hills',
           'minecraft:sunflower_plains', 'minecraft:cherry_grove']


def disc_layer(s, cx, cz, r, y, state, inside_only=False):
    for x in range(cx - r - 1, cx + r + 2):
        for z in range(cz - r - 1, cz + r + 2):
            d = math.sqrt((x - cx) ** 2 + (z - cz) ** 2)
            if d <= r + 0.4 and (not inside_only or d <= r - 0.6):
                s.set(x, y, z, state() if callable(state) else state)


def wizard_tower(seed):
    r, top = 4, 24
    size = 2 * r + 5
    c = size // 2
    s = Struct(size, top + 9, size, seed)
    wall = pick(s, ('minecraft:stone_bricks', 8), ('minecraft:mossy_stone_bricks', 3), ('minecraft:cracked_stone_bricks', 2))
    disc_layer(s, c, c, r + 1, 0, 'minecraft:cobblestone')
    round_tower(s, c, c, r, 1, top, wall)
    disc_layer(s, c, c, r, 0, 'minecraft:polished_andesite', inside_only=True)
    for fy in (6, 12, 18, top):
        disc_layer(s, c, c, r, fy, 'minecraft:spruce_planks', inside_only=True)
        lantern(s, c + 2, fy - 1, c)
    for wy in (3, 9, 15, 21):
        for (wx, wz, ax) in ((c - r, c, 'z'), (c + r, c, 'z'), (c, c + r, 'x')):
            if wy == 3 and wz == c + r:
                continue
            pane(s, wx, wy, wz, 'minecraft:purple_stained_glass_pane' if wy == 21 else 'minecraft:glass_pane', ax)
    # ladder up the north wall; it replaces the floor blocks it passes through
    ladder(s, c, 1, top, c - r + 1, 'south')
    door(s, c, 1, c + r, 'south')
    # floor contents
    s.set(c + 2, 1, c - 1, 'minecraft:crafting_table')
    s.set(c - 2, 1, c + 1, 'minecraft:cauldron')
    s.set(c - 2, 1, c - 1, 'minecraft:brewing_stand[has_bottle_0=false,has_bottle_1=false,has_bottle_2=false]')
    s.spawner(c, 7, c, 'minecraft:witch' if seed % 2 else 'minecraft:zombie')
    for z in range(c - 1, c + 2):
        s.set(c - 3, 13, z, 'minecraft:bookshelf')
        s.set(c - 3, 14, z, 'minecraft:bookshelf')
    s.set(c + 2, 13, c + 1, 'minecraft:enchanting_table' if seed % 3 == 0 else 'minecraft:lectern[facing=west,has_book=false,powered=false]')
    s.chest(c - 2, 13, c, M + ':chests/tower', 'east')
    s.spawner(c, 19, c, 'minecraft:skeleton')
    # conical roof with an attic for the wizard's chest
    for i in range(0, 8):
        rr = r + 1 - i * 0.75
        if rr < 0.6:
            s.set(c, top + 1 + i, c, 'minecraft:deepslate_tiles')
            break
        for x in range(c - r - 2, c + r + 3):
            for z in range(c - r - 2, c + r + 3):
                d = math.sqrt((x - c) ** 2 + (z - c) ** 2)
                if rr - 1.0 < d <= rr + 0.3:
                    s.set(x, top + 1 + i, z, 'minecraft:deepslate_tiles')
                elif d <= rr - 1.0:
                    s.set(x, top + 1 + i, z, AIR)
    s.chest(c + 2, top + 1, c, M + ':chests/wizard', 'west')
    lantern(s, c - 2, top + 1, c, hanging=False)
    return s


def watchtower(seed):
    s = Struct(11, 22, 11, seed)
    logs = 'minecraft:spruce_log[axis=y]'
    s.fill(3, 0, 3, 7, 0, 7, 'minecraft:cobblestone')
    for y in range(1, 15):
        for (x, z) in ((3, 3), (7, 3), (3, 7), (7, 7)):
            s.set(x, y, z, logs)
        for x in range(4, 7):
            for z in (3, 7):
                s.set(x, y, z, 'minecraft:spruce_planks' if y <= 4 or y % 4 == 0 else AIR)
        for z in range(4, 7):
            for x in (3, 7):
                s.set(x, y, z, 'minecraft:spruce_planks' if y <= 4 or y % 4 == 0 else AIR)
        for x in range(4, 7):
            for z in range(4, 7):
                s.set(x, y, z, AIR)
    for y in range(1, 15):
        s.set(5, y, 3, 'minecraft:spruce_planks')
    door(s, 5, 1, 7, 'south', wood='spruce')
    ladder(s, 5, 1, 14, 4, 'south')
    # lookout platform
    s.fill(1, 15, 1, 9, 15, 9, 'minecraft:spruce_planks')
    s.set(5, 15, 4, 'minecraft:spruce_trapdoor[facing=south,half=top,open=false,powered=false,waterlogged=false]')
    fence_line(s, 1, 1, 9, 1, 16, 'minecraft:spruce_fence')
    fence_line(s, 1, 9, 9, 9, 16, 'minecraft:spruce_fence')
    fence_line(s, 1, 2, 1, 8, 16, 'minecraft:spruce_fence')
    fence_line(s, 9, 2, 9, 8, 16, 'minecraft:spruce_fence')
    for (x, z) in ((1, 1), (9, 1), (1, 9), (9, 9)):
        for y in range(16, 19):
            s.set(x, y, z, logs)
    s.fill(0, 19, 0, 10, 19, 10, 'minecraft:spruce_slab[type=bottom,waterlogged=false]')
    s.fill(2, 20, 2, 8, 20, 8, 'minecraft:spruce_slab[type=bottom,waterlogged=false]')
    s.fill(4, 21, 4, 6, 21, 6, 'minecraft:spruce_slab[type=bottom,waterlogged=false]')
    s.chest(8, 16, 8, M + ':chests/watchtower', 'west')
    lantern(s, 5, 18, 5)
    s.set(2, 16, 8, 'minecraft:barrel[facing=up,open=false]', {'LootTable': M + ':chests/watchtower'})
    s.set(2, 16, 2, 'minecraft:cartography_table')
    return s


def desert_tower(seed):
    s = Struct(9, 26, 9, seed)
    w = pick(s, ('minecraft:cut_sandstone', 5), ('minecraft:sandstone', 3), ('minecraft:smooth_sandstone', 2))
    foundation(s, 0, 0, 8, 8, 0, 'minecraft:sandstone', depth=4)
    for y in range(1, 22):
        s.walls(1, y, 1, 7, y, 7, w)
        s.clear(2, y, 2, 6, y, 6)
    for fy in (6, 11, 16, 21):
        s.fill(2, fy, 2, 6, fy, 6, 'minecraft:smooth_sandstone')
        s.set(4, fy, 2, AIR)
        torch_wall(s, 6, fy - 2, 4, 'west')
    ladder(s, 4, 1, 22, 2, 'south')
    s.clear(4, 1, 7, 4, 2, 7)
    s.set(4, 3, 7, 'minecraft:chiseled_sandstone')
    for y in (8, 13, 18):
        s.set(1, y, 4, 'minecraft:orange_stained_glass_pane[north=true,south=true,east=false,west=false,waterlogged=false]')
        s.set(7, y, 4, 'minecraft:orange_stained_glass_pane[north=true,south=true,east=false,west=false,waterlogged=false]')
    s.fill(0, 22, 0, 8, 22, 8, 'minecraft:cut_sandstone')
    s.set(4, 22, 2, 'minecraft:ladder[facing=south,waterlogged=false]')
    battlements(s, 0, 0, 8, 8, 23, 'minecraft:cut_sandstone')
    s.spawner(4, 12, 5, 'minecraft:husk')
    s.chest(5, 7, 5, M + ':chests/tower', 'west')
    s.chest(6, 23, 6, M + ':chests/desert', 'west')
    s.set(2, 1, 5, 'minecraft:decorated_pot')
    s.set(6, 1, 5, 'minecraft:decorated_pot')
    return s


def ice_spire(seed):
    r, top = 3, 20
    size = 2 * r + 7
    c = size // 2
    s = Struct(size, top + 12, size, seed)
    wall = pick(s, ('minecraft:packed_ice', 6), ('minecraft:blue_ice', 1), ('minecraft:snow_block', 1))
    disc_layer(s, c, c, r + 2, 0, 'minecraft:snow_block')
    round_tower(s, c, c, r, 1, top, wall)
    disc_layer(s, c, c, r, 0, 'minecraft:packed_ice', inside_only=True)
    for fy in (5, 10, 15, top):
        disc_layer(s, c, c, r, fy, 'minecraft:blue_ice', inside_only=True)
    for wy in (8, 13, 18):
        pane(s, c - r, wy, c, 'minecraft:light_blue_stained_glass_pane', 'z')
        pane(s, c + r, wy, c, 'minecraft:light_blue_stained_glass_pane', 'z')
    ladder(s, c, 1, top, c - r + 1, 'south')
    s.clear(c, 1, c + r, c, 2, c + r)
    s.spawner(c, 6, c, 'minecraft:stray')
    s.spawner(c, 16, c, 'minecraft:stray')
    s.chest(c + 1, 11, c + 1, M + ':chests/tower', 'west')
    s.chest(c, top + 1, c + 1, M + ':chests/frost', 'north')
    for i in range(0, 11):
        rr = r + 1 - i * 0.45
        if rr < 0.5:
            break
        disc_layer(s, c, c, max(0, int(round(rr))), top + 1 + i, 'minecraft:packed_ice')
        if rr > 1.5:
            disc_layer(s, c, c, max(0, int(round(rr)) - 1), top + 1 + i, AIR, inside_only=True)
    s.set(c, top + 1, c + 1, 'minecraft:chest[facing=north,type=single,waterlogged=false]', {'LootTable': M + ':chests/frost'})
    lantern(s, c, top + 1, c - 1, hanging=False, soul=True)
    return s


def build():
    m = Mod(M, 'Towers', 'Lonely towers rise across the land: stone wizard towers with conical roofs, wooden forest watchtowers, '
                         'sandstone desert towers and frozen ice spires, each with guards on the way up and loot at the top.', tab='WORLD')
    rl = m.rl
    m.item('spyglass_lens', ('lens', '#9fd8ff'), desc='A finely ground lens salvaged from a watchtower')
    m.item('wizard_tome', ('scroll', '#7b3fc4'), desc='Right-click for a burst of experience and a random beneficial effect',
           java='new com.frontierworks.towers.TomeItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON))', jtype='com.frontierworks.towers.TomeItem')
    m.tr('msg.fw_towers.tome', 'The tome crumbles as its knowledge flows into you')
    m.shapeless('spyglass', 'minecraft:spyglass', [rl('spyglass_lens'), 'minecraft:copper_ingot', 'minecraft:copper_ingot'])
    base = [('minecraft:arrow', 8, 4, 12), ('minecraft:bread', 8, 1, 4), ('minecraft:iron_ingot', 8, 1, 4), ('minecraft:gold_ingot', 4, 1, 3),
            ('minecraft:string', 6, 2, 6), ('minecraft:coal', 6, 2, 8), ('minecraft:torch', 6, 4, 10), ('minecraft:emerald', 3, 1, 3)]
    loot_table(m, 'chests/tower', [(3, 6, base)])
    loot_table(m, 'chests/wizard', [(3, 5, [('minecraft:experience_bottle', 8, 2, 6), ('minecraft:lapis_lazuli', 8, 3, 10), ('minecraft:glowstone_dust', 6, 2, 8),
                                            ('minecraft:redstone', 6, 3, 10), ('minecraft:ender_pearl', 4, 1, 2), ('minecraft:amethyst_shard', 5, 2, 6),
                                            ('minecraft:potion', 3), ('minecraft:book', 5, 1, 1, 'enchant')]),
                                    (1, 1, [(rl('wizard_tome'), 4), ('minecraft:diamond', 2, 1, 2), ('fw_crystals:energite_shard', 3, 2, 5), ('minecraft:totem_of_undying', 1)])])
    loot_table(m, 'chests/watchtower', [(3, 6, base + [('minecraft:spyglass', 2), (rl('spyglass_lens'), 5, 1, 2), ('minecraft:map', 3),
                                                       ('minecraft:crossbow', 2, 1, 1, 'enchant'), ('minecraft:sweet_berries', 6, 2, 8)])])
    loot_table(m, 'chests/desert', [(3, 6, base + [('minecraft:gold_ingot', 6, 2, 5), ('minecraft:cactus', 4, 1, 4), ('minecraft:rabbit_foot', 3),
                                                   ('fw_gems:topaz', 4, 1, 3), ('minecraft:golden_apple', 2)]), (0, 1, [(rl('wizard_tome'), 1), ('minecraft:diamond', 2)])])
    loot_table(m, 'chests/frost', [(3, 6, base + [('minecraft:snowball', 8, 4, 16), ('minecraft:blue_ice', 5, 1, 4), ('minecraft:diamond', 2, 1, 2),
                                                  ('minecraft:iron_boots', 3, 1, 1, 'enchant'), ('fw_gems:sapphire', 4, 1, 3), ('fw_freezer:cryotheum_dust', 3, 1, 4)]),
                                   (0, 1, [(rl('wizard_tome'), 1)])])
    structure(m, 'wizard_tower', [(wizard_tower(1), 1), (wizard_tower(2), 1)], FORESTS, spacing=48, separation=18)
    structure(m, 'watchtower', [(watchtower(3), 1), (watchtower(4), 1)], ['#minecraft:is_taiga', '#minecraft:is_forest', 'minecraft:windswept_forest',
                                                                          'minecraft:old_growth_pine_taiga'], spacing=36, separation=12)
    structure(m, 'desert_tower', [(desert_tower(5), 1), (desert_tower(6), 1)], ['#forge:is_desert', '#minecraft:is_badlands'], spacing=40, separation=14)
    structure(m, 'ice_spire', [(ice_spire(7), 1), (ice_spire(8), 1)], ['minecraft:snowy_plains', 'minecraft:ice_spikes', 'minecraft:snowy_taiga',
                                                                       'minecraft:snowy_slopes', 'minecraft:frozen_peaks', 'minecraft:grove'], spacing=40, separation=14)
    return m
