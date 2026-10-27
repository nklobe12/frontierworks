"""Treasure Hunting: treasure maps to every kind of Frontierworks structure, buried stashes and a treasure detector."""
from gen.fw import Mod
from gen import tex
from gen.structures import Struct, structure, loot_table, AIR, LAND

M = 'fw_treasure'
G = 5

# structure tags for the maps; optional entries so missing mods never break the tag
MAPS = {
    'explorers_map': ('Explorer\'s Map', '#d9c48f', 'RED_X', ['#fw_core:explorable']),
    'dungeon_map': ('Dungeon Map', '#6b6b78', 'RED_X', ['fw_dungeons:dungeon', 'fw_temples:sun_pyramid', 'fw_temples:jungle_ziggurat',
                                                        'fw_temples:frost_shrine', 'fw_bunkers:bunker']),
    'ruins_map': ('Ruins Map', '#a89a7c', 'RED_X', ['fw_ruins:ruined_house', 'fw_ruins:column_circle', 'fw_ruins:broken_arch', 'fw_ruins:sunken_cellar',
                                                    'fw_towers:wizard_tower', 'fw_towers:watchtower', 'fw_towers:desert_tower', 'fw_towers:ice_spire']),
    'tech_map': ('Salvage Map', '#5c8a5a', 'RED_X', ['fw_outposts:research_station', 'fw_outposts:relay_station', 'fw_outposts:supply_depot',
                                                     'fw_meteors:crater_medium', 'fw_meteors:crater_large']),
    'stash_map': ('Stash Map', '#c0392b', 'RED_X', ['fw_treasure:buried_stash']),
}


def stash(seed):
    s = Struct(5, G + 2, 5, seed)
    # an X of coarse dirt marks the spot; the chest is buried three blocks down
    for i in range(5):
        s.set(i, G, i, 'minecraft:coarse_dirt')
        s.set(4 - i, G, i, 'minecraft:coarse_dirt')
    s.set(2, G - 3, 2, 'minecraft:chest[facing=north,type=single,waterlogged=false]', {'LootTable': M + ':chests/stash'})
    s.set(2, G - 4, 2, 'minecraft:gravel')
    if seed % 2:
        s.set(0, G + 1, 0, 'minecraft:oak_fence[east=false,west=false,north=false,south=false,waterlogged=false]')
        s.set(0, G + 1, 4, 'minecraft:oak_fence[east=false,west=false,north=false,south=false,waterlogged=false]')
    return s


def build():
    m = Mod(M, 'Treasure Hunting', 'Treasure maps that lead to dungeons, ruins, salvage sites and buried stashes, a treasure detector that '
                                   'beeps its way to unopened chests, and new treasure in vanilla loot.', tab='ADVENTURE')
    rl = m.rl
    m.imp('net.minecraft.world.level.saveddata.maps.MapDecoration')
    for name, (lang, color, marker, structures) in MAPS.items():
        m.item(name, ('map', color), lang=lang, desc='Right-click to reveal the way to the nearest unexplored site',
               java='new com.frontierworks.treasure.TreasureMapItem(new Item.Properties().stacksTo(16), "fw_treasure:%s", MapDecoration.Type.%s)' % (name, marker),
               jtype='com.frontierworks.treasure.TreasureMapItem')
        m.tr('item.%s.%s.filled' % (M, name), lang + ' (revealed)')
        vals = [v if v.startswith('#fw_core') else {'id': v, 'required': False} for v in structures]
        m.data('tags/worldgen/structure/%s.json' % name, {'replace': False, 'values': vals})
    m.tr('msg.fw_treasure.nothing', 'The map shows nothing nearby... try somewhere else')
    m.shaped('explorers_map', rl('explorers_map'), ['EPE', 'PCP', 'EPE'], {'E': 'minecraft:emerald', 'P': 'minecraft:paper', 'C': 'minecraft:compass'}, 2)
    m.shaped('stash_map', rl('stash_map'), [' P ', 'PGP', ' P '], {'P': 'minecraft:paper', 'G': 'minecraft:gold_ingot'}, 2)
    m.item('treasure_detector', ('compass', '#d4af37'),
           java='new com.frontierworks.treasure.TreasureDetectorItem(new Item.Properties().durability(200))', jtype='com.frontierworks.treasure.TreasureDetectorItem',
           desc='Right-click: direction and distance to the nearest unopened loot chest within 48 blocks', tab='TOOLS')
    m.shaped('treasure_detector', rl('treasure_detector'), ['GRG', 'ICI', 'IBI'], {'G': 'minecraft:gold_ingot', 'R': 'minecraft:redstone', 'I': 'minecraft:iron_ingot',
                                                                                  'C': 'minecraft:compass', 'B': '#forge:storage_blocks/redstone'})
    for k, v in {'msg.fw_treasure.detector': 'Treasure %s, %s blocks away, %s', 'msg.fw_treasure.detector_none': 'No treasure in range',
                 'msg.fw_treasure.dir.ahead': 'ahead', 'msg.fw_treasure.dir.behind': 'behind', 'msg.fw_treasure.dir.left': 'to the left',
                 'msg.fw_treasure.dir.right': 'to the right', 'msg.fw_treasure.vert.above': 'above you', 'msg.fw_treasure.vert.below': 'below you',
                 'msg.fw_treasure.vert.level': 'at your level'}.items():
        m.tr(k, v)
    m.item('gold_doubloon', ('coin', '#f1c40f'), desc='Pirate gold. Worth an emerald to any villager.', tags=['forge:coins'])
    m.item('treasure_pouch', ('pouch', '#8e44ad'), desc='Right-click to open',
           java='new LootBagItem(new Item.Properties().stacksTo(16), new ResourceLocation("fw_treasure", "bags/pouch"), 5, false)', jtype='LootBagItem')
    m.shapeless('doubloons_to_gold', 'minecraft:gold_ingot', [rl('gold_doubloon')] * 4)
    loot_table(m, 'chests/stash', [(4, 7, [(rl('gold_doubloon'), 12, 3, 9), ('minecraft:gold_ingot', 8, 2, 6), ('minecraft:emerald', 8, 2, 6),
                                           ('minecraft:diamond', 4, 1, 3), (rl('treasure_pouch'), 6, 1, 2), ('minecraft:golden_apple', 3),
                                           ('minecraft:book', 3, 1, 1, 'enchant'), (rl('dungeon_map'), 2), (rl('stash_map'), 3)])])
    loot_table(m, 'bags/pouch', [(2, 3, [(rl('gold_doubloon'), 10, 2, 6), ('minecraft:emerald', 8, 1, 3), ('minecraft:gold_nugget', 8, 4, 12),
                                         ('minecraft:diamond', 2), ('minecraft:lapis_lazuli', 6, 2, 8), ('minecraft:amethyst_shard', 4, 1, 4),
                                         ('fw_gems:ruby', 2), ('fw_gems:sapphire', 2), ('fw_gems:topaz', 2)])])
    # treasure in vanilla loot
    m.inject_loot('vanilla_maps', ['minecraft:chests/shipwreck_map', 'minecraft:chests/village/village_cartographer', 'minecraft:chests/stronghold_library',
                                   'minecraft:chests/woodland_mansion', 'minecraft:chests/pillager_outpost'],
                  {'type': 'minecraft:chest', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [
                      {'type': 'minecraft:empty', 'weight': 3}] + [{'type': 'minecraft:item', 'name': rl(n), 'weight': 1} for n in MAPS]}]})
    m.inject_loot('vanilla_treasure', ['minecraft:chests/buried_treasure', 'minecraft:chests/shipwreck_treasure', 'minecraft:chests/simple_dungeon',
                                       'minecraft:chests/abandoned_mineshaft', 'minecraft:chests/desert_pyramid', 'minecraft:chests/jungle_temple',
                                       'minecraft:chests/ruined_portal'],
                  {'type': 'minecraft:chest', 'pools': [{'rolls': {'type': 'minecraft:uniform', 'min': 0, 'max': 2}, 'bonus_rolls': 0, 'entries': [
                      {'type': 'minecraft:item', 'name': rl('gold_doubloon'), 'weight': 6, 'functions': [
                          {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 5}, 'add': False}]},
                      {'type': 'minecraft:item', 'name': rl('treasure_pouch'), 'weight': 3},
                      {'type': 'minecraft:item', 'name': rl('stash_map'), 'weight': 2},
                      {'type': 'minecraft:item', 'name': rl('dungeon_map'), 'weight': 1}]}]})
    structure(m, 'buried_stash', [(stash(1), 1), (stash(2), 1)], LAND + ['#minecraft:is_beach'], spacing=20, separation=6, terrain='none', height=-G)
    return m
