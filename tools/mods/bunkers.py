"""Bunkers: sealed concrete shelters buried under the plains, reached through a hatch on the surface."""
from gen.fw import Mod
from gen import tex
from gen.structures import Struct, structure, loot_table, AIR, FLAT_LAND
from mods._build import pick, lantern, ladder, bed, door, trapdoor, scatter

M = 'fw_bunkers'
G = 10  # template y of the surface


def bunker(seed, variant):
    s = Struct(15, G + 4, 15, seed)
    conc = pick(s, ('minecraft:gray_concrete', 8), ('minecraft:light_gray_concrete', 2), ('minecraft:cracked_stone_bricks', 1))
    s.room(0, 0, 0, 14, 5, 14, conc, floor='minecraft:smooth_stone', ceiling=conc)
    # interior walls: central hall x5..9, rooms either side split at z=7
    for z in range(1, 14):
        for y in range(1, 5):
            s.set(4, y, z, conc())
            s.set(10, y, z, conc())
    for x in list(range(1, 4)) + list(range(11, 14)):
        for y in range(1, 5):
            s.set(x, y, 7, conc())
    for z in (3, 11):
        door(s, 4, 1, z, 'east', iron=False, wood='spruce')
        door(s, 10, 1, z, 'west', iron=False, wood='spruce', hinge='right')
    # entrance shaft
    for y in range(6, G):
        s.walls(6, y, 6, 8, y, 8, conc)
        s.set(7, y, 7, AIR)
    s.set(7, 5, 7, AIR)
    for y in range(1, 5):
        s.set(7, y, 6, conc())
    ladder(s, 7, 1, G - 1, 7, 'south')
    s.fill(6, G, 6, 8, G, 8, 'minecraft:gray_concrete')
    trapdoor(s, 7, G, 7, 'south', 'top', 'minecraft:spruce_trapdoor')
    s.set(9, G + 1, 8, 'minecraft:iron_bars[east=false,west=false,north=false,south=false,waterlogged=false]')
    s.set(9, G + 2, 8, 'minecraft:iron_bars[east=false,west=false,north=false,south=false,waterlogged=false]')
    s.set(9, G + 3, 8, 'minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]')
    s.set(9, G, 8, 'minecraft:gray_concrete')
    # hall lights
    for z in (2, 5, 9, 12):
        lantern(s, 7, 4, z)
    # NW: bunks
    for x in (1, 3):
        bed(s, x, 1, 1, 'south', 'gray' if variant else 'green')
        bed(s, x, 1, 4, 'south', 'gray' if variant else 'green')
    s.set(2, 1, 6, 'minecraft:barrel[facing=up,open=false]', {'LootTable': M + ':chests/personal'})
    lantern(s, 2, 4, 3)
    # SW: storage
    for x in (1, 2, 3):
        for z in (12, 13):
            s.set(x, 1, z, 'minecraft:barrel[facing=up,open=false]', {'LootTable': M + ':chests/storage'} if s.rng.random() < 0.5 else None)
            if s.rng.random() < 0.6:
                s.set(x, 2, z, 'minecraft:barrel[facing=up,open=false]')
    s.chest(1, 1, 9, M + ':chests/storage', 'east')
    s.spawner(2, 1, 10, 'minecraft:zombie')
    lantern(s, 2, 4, 10)
    # NE: generator room
    s.set(13, 1, 1, 'fw_generators:combustion_generator')
    s.set(12, 1, 1, 'fw_batteries:energy_cell_basic')
    s.set(11, 1, 1, 'fw_cables:copper_cable')
    s.set(13, 1, 3, 'fw_tanks:tank_basic')
    s.set(13, 1, 5, 'minecraft:barrel[facing=up,open=false]', {'LootTable': M + ':chests/fuel'})
    s.set(11, 2, 6, 'minecraft:lever[face=wall,facing=north,powered=false]')
    lantern(s, 12, 4, 3)
    # SE: command room
    s.set(12, 1, 12, 'minecraft:cartography_table')
    s.set(11, 1, 12, 'minecraft:lectern[facing=north,has_book=false,powered=false]')
    s.chest(13, 1, 9, M + ':chests/command', 'west')
    s.set(13, 1, 12, 'minecraft:redstone_lamp[lit=false]')
    s.set(13, 2, 12, 'minecraft:lever[face=floor,facing=west,powered=false]')
    lantern(s, 12, 4, 10)
    scatter(s, 5, 1, 9, 13, 1, ['minecraft:cobweb'], 0.04)
    s.decay(remove=0.0, replace={'minecraft:gray_concrete': ('minecraft:cyan_terracotta', 0.04)})
    return s


def build():
    m = Mod(M, 'Bunkers', 'Sealed concrete shelters lie buried under the plains, marked only by a hatch and a rusty mast. Inside: bunks, '
                          'stores, a dead generator and a command room with the bunker\'s secrets.', tab='ADVENTURE')
    rl = m.rl
    m.item('ration_pack', ('food_bar', '#7a7a50'), desc='Long-life food. Fills you up fast.',
           java='new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(8).saturationMod(0.9f).fast().build()))')
    m.imp('net.minecraft.world.food.FoodProperties')
    m.item('bunker_keycard', ('card', '#c0392b'), desc='Right-click to swipe: unlocks the command cache',
           java='new LootBagItem(new Item.Properties().stacksTo(1), new ResourceLocation("fw_bunkers", "bags/command_cache"), 30, true)', jtype='LootBagItem')
    m.shapeless('ration_pack', rl('ration_pack'), ['minecraft:bread', 'minecraft:cooked_beef', 'minecraft:dried_kelp', 'minecraft:paper'], 2)
    food = [(rl('ration_pack'), 10, 1, 4), ('minecraft:bread', 6, 1, 4), ('minecraft:baked_potato', 5, 2, 5), ('minecraft:dried_kelp', 4, 2, 8)]
    loot_table(m, 'chests/personal', [(2, 4, food + [('minecraft:paper', 5, 1, 4), ('minecraft:compass', 2), ('minecraft:clock', 2), ('minecraft:emerald', 3, 1, 3),
                                                     ('minecraft:white_wool', 4, 1, 3)])])
    loot_table(m, 'chests/storage', [(4, 7, food + [('minecraft:torch', 6, 4, 16), ('minecraft:iron_ingot', 6, 2, 5), ('fw_alloys:steel_ingot', 5, 1, 4),
                                                    ('minecraft:arrow', 5, 8, 16), ('minecraft:tnt', 2, 1, 2), ('fw_rope:rope', 4, 4, 12),
                                                    ('minecraft:bucket', 3), ('fw_parts:iron_plate', 4, 2, 6)])])
    loot_table(m, 'chests/fuel', [(2, 4, [('minecraft:coal', 10, 4, 12), ('minecraft:charcoal', 6, 4, 10), ('fw_oil:bitumen', 5, 2, 6),
                                          ('fw_kiln:coal_coke', 5, 2, 8), ('fw_batteries:battery_basic', 3), ('fw_parts:copper_wire', 4, 2, 6)])])
    loot_table(m, 'chests/command', [(3, 5, [('minecraft:map', 5), ('minecraft:paper', 6, 2, 6), ('minecraft:redstone', 6, 4, 10), ('minecraft:ender_pearl', 3, 1, 2),
                                             ('fw_circuits:basic_circuit', 5, 1, 3), ('minecraft:diamond', 2, 1, 2), ('minecraft:golden_apple', 2)]),
                                     (1, 1, [(rl('bunker_keycard'), 1)])])
    loot_table(m, 'bags/command_cache', [(2, 3, [('minecraft:diamond', 6, 1, 3), ('fw_circuits:advanced_circuit', 6, 1, 3), ('minecraft:netherite_scrap', 2),
                                                 ('minecraft:crossbow', 4, 1, 1, 'enchant'), ('fw_batteries:battery_advanced', 4),
                                                 ('minecraft:experience_bottle', 6, 3, 8), ('minecraft:totem_of_undying', 1)])])
    structure(m, 'bunker', [(bunker(1, 0), 1), (bunker(2, 1), 1)], FLAT_LAND + ['#minecraft:is_taiga'], spacing=44, separation=16, terrain='none',
              height=-G)
    return m
