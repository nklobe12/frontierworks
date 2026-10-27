"""The Depths: an enormous cave dimension of glowing fungus, dripstone and verdant grottos, reached through a Depths Gate."""
from gen.fw import Mod, ore, stairs_slab_wall
from gen import tex
from mods._dims import dimension_type, dimension, biome, checkerboard, ore_in

M = 'fw_depths'


def gate_tex(seed, glow):
    img = tex.chiseled('#2a2e3a', seed)
    px = img.load()
    g = tex.rgb(glow)
    for i in range(4, 12):
        for j in range(4, 12):
            if (i + j) % 3 == 0 or i in (4, 11) or j in (4, 11):
                px[i, j] = g + (255,)
    return img


def build():
    m = Mod(M, 'The Depths', 'A vast underground dimension reached through a Depths Gate: fungal hollows lit by glowcaps, dripstone '
                             'chasms and lush verdant grottos, an underground sea, new depthstone and the rare abyssal ore.',
            deps=['fw_caves', 'fw_mobs'], tab='WORLD')
    rl = m.rl
    m.init('com.frontierworks.depths.DepthsEvents.init()')
    dimension_type(m, 'depths', -64, 192, skylight=False, ceiling=True, ambient=0.08, effects='minecraft:overworld', monster_light=0)
    # blocks
    m.block('depthstone', java='new Block(FWProps.stone(2.0f, 8.0f))', textures={'block/depthstone': tex.stone('#3a3f4c', 'depthstone', 'layered')},
            tab='WORLD', loot='self', tags=['minecraft:base_stone_overworld', 'minecraft:stone_ore_replaceables'], item_tags=['forge:stone'])
    m.block('polished_depthstone', java='new Block(FWProps.stone(2.0f, 8.0f))', textures={'block/polished_depthstone': tex.polished('#3a3f4c', 'pdepth')}, tab='DECOR')
    m.block('depthstone_bricks', java='new Block(FWProps.stone(2.0f, 8.0f))', textures={'block/depthstone_bricks': tex.bricks('#3a3f4c', 'dbrick')}, tab='DECOR')
    stairs_slab_wall(m, 'depthstone_bricks', '%s:block/depthstone_bricks' % M, props='FWProps.stone(2.0f, 8.0f)')
    m.shaped('polished_depthstone', rl('polished_depthstone'), ['SS', 'SS'], {'S': rl('depthstone')}, 4)
    m.shaped('depthstone_bricks', rl('depthstone_bricks'), ['SS', 'SS'], {'S': rl('polished_depthstone')}, 4)
    ore(m, 'abyssal_ore', '#3ad8c8', base_tex='%s:block/depthstone' % M, drop=rl('abyssal_shard'), xp=(3, 7), hardness=4.5, style='gem', level='diamond')
    m.item('abyssal_shard', ('shard', '#3ad8c8'), desc='Pulses with deep energy', tags=['forge:gems/abyssal'])
    m.item('abyssal_lens', ('lens', '#3ad8c8'), desc='Hold in your off-hand in the Depths for night vision',
           java='new com.frontierworks.depths.AbyssalLensItem(new Item.Properties().stacksTo(1))', jtype='com.frontierworks.depths.AbyssalLensItem', tab='TOOLS')
    m.shaped('abyssal_lens', rl('abyssal_lens'), [' A ', 'AGA', ' A '], {'A': rl('abyssal_shard'), 'G': '#forge:glass_panes'})
    m.block('abyssal_block', java='new Block(FWProps.gemBlock(5.0f).lightLevel(s -> 6))', textures={'block/abyssal_block': tex.gem_block('#3ad8c8', 'abyblk')},
            tab='MATERIALS', level='iron', tags=['forge:storage_blocks/abyssal'], item_tags=['forge:storage_blocks/abyssal'])
    m.shaped('abyssal_block', rl('abyssal_block'), ['AAA', 'AAA', 'AAA'], {'A': rl('abyssal_shard')})
    m.shapeless('abyssal_shard_from_block', rl('abyssal_shard'), [rl('abyssal_block')], 9)
    m.block('depths_gate', java='new GateBlock(FWProps.stone(5.0f, 1200.0f).lightLevel(s -> 10), "fw_depths:depths", "minecraft:overworld")', jtype='GateBlock',
            textures={'block/depths_gate': gate_tex('dgate', '#3ad8c8')}, tab='WORLD', level='iron',
            desc='Right-click to travel to the Depths (and back). Builds a return gate on arrival.')
    m.imp('com.frontierworks.core.block.GateBlock')
    m.shaped('depths_gate', rl('depths_gate'), ['OEO', 'EDE', 'OEO'], {'O': 'minecraft:crying_obsidian', 'E': 'minecraft:ender_pearl', 'D': 'minecraft:diamond'})
    m.shaped('depths_gate_upgrade', rl('depths_gate'), ['OAO', 'ASA', 'OAO'], {'O': 'minecraft:obsidian', 'A': rl('abyssal_shard'), 'S': '#forge:stone'}, 2)
    m.tr('msg.fw_core.destination_unavailable', 'The way is closed')
    # worldgen
    depth_blob = ore_in(m, 'depthstone_blob', rl('depthstone'), 'minecraft:stone', size=48, count=14, lo=-64, hi=128)
    aby = ore_in(m, 'abyssal_ore', rl('abyssal_ore'), rl('depthstone'), size=4, count=6, lo=-64, hi=20)
    ores = ['minecraft:ore_coal_lower', 'minecraft:ore_iron_middle', 'minecraft:ore_copper_large', 'minecraft:ore_gold', 'minecraft:ore_redstone',
            'minecraft:ore_lapis', 'minecraft:ore_diamond', 'minecraft:ore_diamond_large', depth_blob, aby]
    springs = ['minecraft:spring_water', 'minecraft:spring_lava']
    mons = {'monster': [('minecraft:zombie', 60, 2, 4), ('minecraft:skeleton', 60, 2, 4), ('minecraft:spider', 40, 1, 3), ('minecraft:creeper', 40, 1, 2),
                        ('fw_mobs:rust_beetle', 50, 1, 3), ('fw_mobs:cave_crawler', 50, 2, 4), ('minecraft:cave_spider', 20, 1, 3)],
            'ambient': [('minecraft:bat', 20, 4, 8)], 'underground_water_creature': [('minecraft:glow_squid', 10, 2, 4)]}
    deco_fungal = ['fw_caves:glowcap_patch', 'fw_caves:emberstool_patch', 'fw_caves:luminous_moss_patch', 'fw_caves:cave_crystal_patch', 'minecraft:glow_lichen']
    deco_drip = ['minecraft:large_dripstone', 'minecraft:dripstone_cluster', 'minecraft:pointed_dripstone', 'minecraft:glow_lichen']
    veg_lush = ['minecraft:lush_caves_ceiling_vegetation', 'minecraft:cave_vines', 'minecraft:lush_caves_clay', 'minecraft:lush_caves_vegetation',
                'minecraft:rooted_azalea_tree', 'minecraft:spore_blossom', 'minecraft:classic_vines_cave_feature']
    b1 = biome(m, 'fungal_hollows', '#000000', '#0f2a2a', features={'underground_ores': ores, 'underground_decoration': deco_fungal,
                                                                   'fluid_springs': springs}, spawns=mons, water='#2ad8c0', particle=('minecraft:mycelium', 0.01))
    b2 = biome(m, 'dripstone_chasms', '#000000', '#2a221a', features={'underground_ores': ores, 'underground_decoration': deco_drip, 'fluid_springs': springs},
               spawns=mons)
    b3 = biome(m, 'verdant_grotto', '#000000', '#1a2a12', features={'underground_ores': ores, 'underground_decoration': ['minecraft:glow_lichen'],
                                                                   'fluid_springs': springs, 'vegetal_decoration': veg_lush}, spawns=mons,
               water='#3aa0d0', particle=('minecraft:spore_blossom_air', 0.006))
    dimension(m, 'depths', 'minecraft:caves', checkerboard([b1, b2, b3], 3), lang='The Depths')
    return m
