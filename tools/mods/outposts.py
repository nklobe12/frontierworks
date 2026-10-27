"""Tech Outposts: abandoned research stations, radio relays and supply depots left by earlier engineers."""
from gen.fw import Mod
from gen import tex
from gen.structures import Struct, structure, loot_table, foundation, AIR, FLAT_LAND
from mods._build import pick, lantern, pane, door, ladder, fence_line, scatter, bars

M = 'fw_outposts'
BIOMES = FLAT_LAND + ['#minecraft:is_badlands', '#minecraft:is_savanna', '#minecraft:is_taiga']
STEEL = 'fw_alloys:steel_block'


def research_station(seed):
    s = Struct(15, 10, 11, seed)
    wall = pick(s, ('minecraft:light_gray_concrete', 6), ('minecraft:white_concrete', 2), ('minecraft:light_gray_concrete_powder', 1))
    s.fill(0, 0, 0, 14, 0, 10, 'minecraft:smooth_stone')
    s.room(0, 0, 0, 14, 5, 10, wall, floor='minecraft:polished_andesite', ceiling=STEEL)
    for (x, z) in ((0, 0), (14, 0), (0, 10), (14, 10)):
        for y in range(1, 5):
            s.set(x, y, z, 'minecraft:iron_block')
    for x in (3, 4, 10, 11):
        pane(s, x, 2, 0, axis='x')
        pane(s, x, 3, 0, axis='x')
        pane(s, x, 2, 10, axis='x')
    door(s, 7, 1, 0, 'north', wood='spruce')
    # labs
    s.fill(7, 1, 5, 7, 4, 9, wall)
    door(s, 7, 1, 6, 'east', wood='spruce')
    s.set(1, 1, 9, 'fw_machines:pulverizer')
    s.set(2, 1, 9, 'fw_machines:electric_furnace')
    s.set(3, 1, 9, 'fw_batteries:energy_cell_basic')
    s.set(1, 1, 8, 'minecraft:crafting_table')
    s.chest(5, 1, 9, M + ':chests/research', 'north')
    s.set(13, 1, 9, 'minecraft:brewing_stand[has_bottle_0=false,has_bottle_1=false,has_bottle_2=false]')
    s.set(12, 1, 9, 'minecraft:cauldron')
    s.chest(13, 1, 6, M + ':chests/lab', 'west')
    s.set(9, 1, 9, 'minecraft:cartography_table')
    s.set(1, 1, 1, 'minecraft:barrel[facing=up,open=false]', {'LootTable': M + ':chests/supplies'})
    s.set(13, 1, 1, 'minecraft:barrel[facing=up,open=false]', {'LootTable': M + ':chests/supplies'})
    for x in (3, 11):
        lantern(s, x, 4, 3)
        lantern(s, x, 4, 7)
    # roof: solar array + antenna
    for x in range(1, 14, 2):
        for z in (2, 4, 6, 8):
            if s.rng.random() < 0.8:
                s.set(x, 6, z, 'fw_solar:solar_panel_basic')
    for y in range(6, 9):
        s.set(13, y, 1, 'minecraft:iron_bars[east=false,west=false,north=false,south=false,waterlogged=false]')
    s.set(13, 9, 1, 'minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]')
    s.decay(remove=0.02, replace={'minecraft:light_gray_concrete': ('minecraft:cyan_terracotta', 0.08)}, min_y=1)
    return s


def relay_station(seed):
    s = Struct(9, 22, 9, seed)
    s.fill(0, 0, 0, 8, 0, 8, 'minecraft:gravel')
    s.room(1, 0, 1, 5, 4, 5, pick(s, ('minecraft:gray_concrete', 4), ('minecraft:cyan_terracotta', 1)), floor='minecraft:smooth_stone',
           ceiling='minecraft:smooth_stone_slab[type=double,waterlogged=false]')
    door(s, 3, 1, 5, 'south', wood='spruce')
    s.set(2, 1, 2, 'minecraft:lectern[facing=south,has_book=false,powered=false]')
    s.set(4, 1, 2, 'minecraft:redstone_lamp[lit=false]')
    s.set(4, 2, 2, 'minecraft:lever[face=floor,facing=south,powered=false]')
    s.chest(2, 1, 4, M + ':chests/relay', 'east')
    lantern(s, 3, 3, 3)
    # lattice mast
    for y in range(1, 20):
        for (x, z) in ((6, 6), (7, 6), (6, 7), (7, 7)):
            if (x + y + z) % 3 == 0 or y % 5 == 0:
                s.set(x, y, z, 'minecraft:iron_bars[east=true,west=true,north=true,south=true,waterlogged=false]')
        s.set(6, y, 6, 'minecraft:iron_bars[east=false,west=false,north=false,south=false,waterlogged=false]')
    for y in range(1, 21):
        s.set(7, y, 7, 'minecraft:iron_bars[east=false,west=false,north=false,south=false,waterlogged=false]')
    s.set(7, 21, 7, 'minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]')
    s.set(6, 20, 6, 'minecraft:redstone_torch[lit=true]')
    s.set(6, 19, 6, 'minecraft:iron_block')
    fence_line(s, 0, 0, 8, 0, 1, 'minecraft:iron_bars')
    return s


def supply_depot(seed):
    s = Struct(13, 6, 13, seed)
    s.fill(0, 0, 0, 12, 0, 12, pick(s, ('minecraft:gravel', 3), ('minecraft:coarse_dirt', 2), ('fw_oil:asphalt', 3)))
    fence_line(s, 0, 0, 12, 0, 1, 'minecraft:oak_fence')
    fence_line(s, 0, 12, 12, 12, 1, 'minecraft:oak_fence')
    fence_line(s, 0, 1, 0, 11, 1, 'minecraft:oak_fence')
    fence_line(s, 12, 1, 12, 11, 1, 'minecraft:oak_fence')
    s.set(6, 1, 12, AIR)
    s.set(5, 1, 12, AIR)
    for (x0, z0, h) in ((2, 2, 3), (2, 6, 2), (8, 2, 2), (9, 7, 3), (5, 8, 1)):
        for y in range(1, h + 1):
            for dx in range(2):
                for dz in range(2):
                    if s.rng.random() < 0.85 or y == 1:
                        s.set(x0 + dx, y, z0 + dz, 'fw_storage:wooden_crate' if s.rng.random() < 0.6 else 'minecraft:barrel[facing=up,open=false]')
    s.set(4, 1, 4, 'minecraft:barrel[facing=up,open=false]', {'LootTable': M + ':chests/supplies'})
    s.set(7, 1, 5, 'minecraft:barrel[facing=up,open=false]', {'LootTable': M + ':chests/supplies'})
    s.chest(10, 1, 10, M + ':chests/depot', 'north')
    # a little shelter
    for (x, z) in ((7, 9), (11, 9), (7, 11), (11, 11)):
        for y in range(1, 4):
            s.set(x, y, z, 'minecraft:oak_fence[east=false,west=false,north=false,south=false,waterlogged=false]')
    s.fill(7, 4, 9, 11, 4, 11, 'minecraft:spruce_slab[type=bottom,waterlogged=false]')
    lantern(s, 9, 3, 10)
    s.set(8, 1, 10, 'minecraft:crafting_table')
    s.set(8, 1, 11, 'minecraft:smoker[facing=east,lit=false]')
    scatter(s, 1, 1, 11, 11, 1, ['minecraft:grass', 'minecraft:dead_bush'], 0.06)
    return s


def build():
    m = Mod(M, 'Tech Outposts', 'Abandoned research stations, radio relays and supply depots left behind by earlier engineers. Salvage '
                                'their machines and decode their data drives for rare technology.', tab='ADVENTURE')
    rl = m.rl
    m.item('salvaged_circuit', ('circuit', '#5c8a5a'), desc='Scorched but repairable. Recycle or pulverise it for parts.')
    m.item('encrypted_data_drive', ('card', '#2f6fb5'), desc='Right-click to decode: experience and a chance of rare technology',
           java='new LootBagItem(new Item.Properties().stacksTo(16), new ResourceLocation("fw_outposts", "bags/data_drive"), 25, false)', jtype='LootBagItem')
    m.item('supply_crate', ('pouch', '#8a6a3f'), desc='Right-click to open', lang='Supply Crate',
           java='new LootBagItem(new Item.Properties().stacksTo(16), new ResourceLocation("fw_outposts", "bags/supply_crate"))', jtype='LootBagItem')
    m.mrecipe('fw_machines:pulverizing', 'salvaged_circuit', [(rl('salvaged_circuit'), 1)], [('fw_metals:copper_dust', 2), ('fw_metals:gold_dust', 1, 0.4),
                                                                                             ('fw_circuits:silicon', 1, 0.5)], time=120)
    m.shapeless('salvage_repair', 'fw_circuits:basic_circuit', [rl('salvaged_circuit'), rl('salvaged_circuit'), 'minecraft:redstone', 'fw_parts:copper_wire'])
    supplies = [('minecraft:bread', 8, 2, 5), ('minecraft:cooked_beef', 5, 1, 4), ('minecraft:torch', 8, 4, 16), ('minecraft:coal', 6, 4, 10),
                ('minecraft:iron_ingot', 5, 1, 4), ('fw_alloys:steel_ingot', 4, 1, 3), ('minecraft:rail', 4, 4, 12), ('fw_parts:iron_plate', 4, 1, 4),
                (rl('supply_crate'), 2)]
    tech = [(rl('salvaged_circuit'), 10, 1, 4), ('fw_parts:copper_wire', 8, 2, 8), ('fw_parts:motor', 4, 1, 2), ('fw_circuits:basic_circuit', 5, 1, 3),
            ('fw_circuits:silicon', 5, 1, 4), ('fw_parts:steel_gear', 4, 1, 3), ('fw_batteries:battery_basic', 3), ('minecraft:redstone', 8, 4, 12),
            ('fw_parts:machine_frame', 3), ('fw_chemistry:plastic_sheet', 4, 1, 4)]
    loot_table(m, 'chests/supplies', [(3, 6, supplies)])
    loot_table(m, 'chests/research', [(3, 6, tech), (1, 2, [(rl('encrypted_data_drive'), 6, 1, 2), ('fw_circuits:advanced_circuit', 3), ('fw_parts:servo', 3),
                                                             ('fw_batteries:battery_advanced', 2)])])
    loot_table(m, 'chests/lab', [(3, 5, [('minecraft:glass_bottle', 8, 2, 6), ('minecraft:redstone', 6, 2, 8), ('minecraft:glowstone_dust', 5, 2, 6),
                                         ('fw_minerals:sulfur', 6, 2, 6), ('fw_minerals:saltpeter', 5, 2, 6), ('minecraft:potion', 3), ('fw_crystals:energite_shard', 4, 1, 3)]),
                                 (1, 1, [(rl('encrypted_data_drive'), 5), ('fw_chemistry:carbon_fiber', 3, 1, 3)])])
    loot_table(m, 'chests/relay', [(3, 5, tech + [('minecraft:compass', 3), ('minecraft:map', 3), ('minecraft:copper_ingot', 6, 2, 8)]),
                                   (1, 1, [(rl('encrypted_data_drive'), 4)])])
    loot_table(m, 'chests/depot', [(4, 7, supplies + [('minecraft:tnt', 2, 1, 3), ('minecraft:minecart', 2), ('fw_rope:rope', 4, 4, 12)])])
    loot_table(m, 'bags/supply_crate', [(3, 5, supplies[:-1] + [('minecraft:arrow', 4, 4, 12), ('minecraft:golden_carrot', 3, 1, 3)])])
    loot_table(m, 'bags/data_drive', [(1, 2, [('fw_circuits:advanced_circuit', 8, 1, 2), ('fw_circuits:elite_circuit', 3), ('fw_circuits:processor', 4),
                                              ('fw_parts:advanced_frame', 3), ('fw_batteries:battery_advanced', 4), ('fw_crystals:charged_energite', 3, 1, 2),
                                              ('minecraft:diamond', 3, 1, 2), ('minecraft:experience_bottle', 6, 2, 5)])])
    structure(m, 'research_station', [(research_station(1), 1), (research_station(2), 1)], BIOMES, spacing=44, separation=16)
    structure(m, 'relay_station', [(relay_station(3), 1)], BIOMES + ['minecraft:windswept_hills', 'minecraft:meadow'], spacing=38, separation=12)
    structure(m, 'supply_depot', [(supply_depot(4), 1), (supply_depot(5), 1)], BIOMES, spacing=34, separation=12)
    return m
