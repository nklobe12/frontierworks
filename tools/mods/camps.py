"""Camps: friendly explorer camps and dangerous bandit hideouts."""
from gen.fw import Mod
from gen import tex
from gen.structures import Struct, structure, loot_table, AIR, LAND, FLAT_LAND
from mods._build import tent, campfire, lantern, fence_line, scatter

M = 'fw_camps'


def log(s, x, y, z, axis='x', wood='oak'):
    s.set(x, y, z, 'minecraft:%s_log[axis=%s]' % (wood, axis))


def explorer_camp(seed):
    s = Struct(15, 5, 13, seed)
    tent(s, 1, 1, 5, 1, 'minecraft:white_wool')
    tent(s, 9, 2, 4, 1, 'minecraft:brown_wool')
    campfire(s, 7, 1, 9)
    log(s, 5, 1, 9, 'z')
    log(s, 9, 1, 9, 'z')
    log(s, 7, 1, 11, 'x')
    s.set(2, 1, 2, 'fw_sleepingbags:green_sleeping_bag')
    s.chest(4, 1, 2, M + ':chests/explorer', 'south')
    s.set(10, 1, 3, 'minecraft:barrel[facing=up,open=false]', {'LootTable': M + ':chests/explorer_food'})
    s.set(12, 1, 3, 'minecraft:cartography_table')
    s.set(13, 1, 8, 'minecraft:oak_fence[east=false,west=false,north=false,south=false,waterlogged=false]')
    s.set(13, 2, 8, 'minecraft:lantern[hanging=false,waterlogged=false]')
    s.set(1, 1, 9, 'minecraft:crafting_table')
    s.set(1, 1, 10, 'minecraft:composter[level=0]')
    scatter(s, 0, 0, 14, 12, 1, ['minecraft:grass', 'minecraft:fern'], 0.08)
    return s


def bandit_camp(seed):
    s = Struct(19, 6, 17, seed)
    fence_line(s, 0, 0, 18, 0, 1, 'minecraft:dark_oak_fence')
    fence_line(s, 0, 16, 18, 16, 1, 'minecraft:dark_oak_fence')
    fence_line(s, 0, 1, 0, 15, 1, 'minecraft:dark_oak_fence')
    fence_line(s, 18, 1, 18, 15, 1, 'minecraft:dark_oak_fence')
    for x in (8, 9, 10):
        s.set(x, 1, 16, AIR)
    tent(s, 1, 1, 6, 1, 'minecraft:black_wool')
    tent(s, 7, 1, 5, 1, 'minecraft:gray_wool')
    tent(s, 13, 1, 6, 1, 'minecraft:black_wool')
    campfire(s, 9, 1, 11)
    for (x, z, a) in ((7, 11, 'z'), (11, 11, 'z'), (9, 13, 'x')):
        log(s, x, 1, z, a, 'dark_oak')
    s.chest(2, 1, 2, M + ':chests/bandit', 'south')
    s.chest(16, 1, 2, M + ':chests/bandit', 'south')
    s.chest(9, 1, 2, M + ':chests/bandit_boss', 'south')
    s.set(3, 1, 13, 'minecraft:target')
    s.set(3, 2, 13, 'minecraft:carved_pumpkin[facing=north]')
    for (x, z) in ((1, 9), (17, 9)):
        s.set(x, 1, z, 'minecraft:dark_oak_fence[east=false,west=false,north=false,south=false,waterlogged=false]')
        s.set(x, 2, z, 'minecraft:dark_oak_fence[east=false,west=false,north=false,south=false,waterlogged=false]')
        s.set(x, 3, z, 'minecraft:lantern[hanging=false,waterlogged=false]')
    s.set(15, 1, 13, 'minecraft:barrel[facing=up,open=false]', {'LootTable': M + ':chests/bandit'})
    s.set(16, 1, 13, 'minecraft:barrel[facing=up,open=false]')
    s.set(15, 2, 13, 'minecraft:barrel[facing=up,open=false]')
    s.set(5, 1, 14, 'minecraft:grindstone[face=floor,facing=north]')
    s.set(13, 1, 14, 'minecraft:fletching_table')
    scatter(s, 1, 1, 17, 15, 1, ['minecraft:dead_bush', 'minecraft:grass'], 0.05)
    return s


def build():
    m = Mod(M, 'Camps', 'Explorer camps with tents, maps and supplies to share, and fenced bandit hideouts guarded by pillagers '
                        'with a boss chest full of stolen goods.', tab='ADVENTURE')
    rl = m.rl
    m.item('bandit_mask', ('pouch', '#2b2b2b'), desc='Proof that you raided a bandit camp. Bounty boards pay for these.', stack=64)
    m.item('trail_mix', ('round_food', '#a0703a'), desc='Quick to eat',
           java='new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationMod(0.6f).fast().build()))')
    m.imp('net.minecraft.world.food.FoodProperties')
    m.shapeless('trail_mix', rl('trail_mix'), ['minecraft:sweet_berries', 'minecraft:wheat_seeds', 'minecraft:cocoa_beans', 'minecraft:dried_kelp'], 3)
    loot_table(m, 'chests/explorer', [(3, 6, [('minecraft:map', 6), ('minecraft:compass', 4), ('minecraft:paper', 6, 2, 6), ('minecraft:torch', 8, 4, 12),
                                              ('fw_rope:rope', 5, 4, 12), ('fw_flares:flare', 4, 2, 6), ('minecraft:spyglass', 2), ('minecraft:iron_pickaxe', 2),
                                              ('minecraft:leather_boots', 3, 1, 1, 'enchant'), ('minecraft:emerald', 4, 1, 3)])])
    loot_table(m, 'chests/explorer_food', [(3, 6, [(rl('trail_mix'), 10, 2, 6), ('minecraft:bread', 8, 2, 4), ('minecraft:cooked_salmon', 6, 1, 4),
                                                   ('minecraft:apple', 6, 1, 4), ('minecraft:sweet_berries', 6, 3, 8)])])
    loot_table(m, 'chests/bandit', [(3, 6, [('minecraft:arrow', 10, 6, 16), ('minecraft:iron_ingot', 8, 1, 4), ('minecraft:gold_ingot', 6, 1, 4),
                                            ('minecraft:emerald', 7, 1, 5), ('minecraft:leather', 6, 2, 5), ('minecraft:crossbow', 3), ('minecraft:tnt', 2, 1, 2),
                                            (rl('bandit_mask'), 6, 1, 2), ('minecraft:bread', 6, 2, 5)])])
    loot_table(m, 'chests/bandit_boss', [(4, 7, [('minecraft:emerald', 10, 3, 9), ('minecraft:gold_ingot', 8, 2, 6), ('minecraft:diamond', 4, 1, 3),
                                                 ('minecraft:crossbow', 4, 1, 1, 'enchant'), ('minecraft:iron_axe', 3, 1, 1, 'enchant'),
                                                 ('minecraft:golden_apple', 3), ('minecraft:goat_horn', 2), (rl('bandit_mask'), 8, 2, 4)])])
    structure(m, 'explorer_camp', [(explorer_camp(1), 1), (explorer_camp(2), 1)], LAND, spacing=30, separation=10)
    bandits = {'monster': {'bounding_box': 'piece', 'spawns': [{'type': 'minecraft:pillager', 'weight': 3, 'minCount': 1, 'maxCount': 2},
                                                                {'type': 'minecraft:vindicator', 'weight': 1, 'minCount': 1, 'maxCount': 1}]}}
    structure(m, 'bandit_camp', [(bandit_camp(3), 1), (bandit_camp(4), 1)], FLAT_LAND + ['#minecraft:is_taiga', '#minecraft:is_badlands'], spacing=38,
              separation=14, spawn_overrides=bandits)
    return m
