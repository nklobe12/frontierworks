"""The Skylands: a dimension of floating islands above the clouds, reached through a Skylands Gate."""
from gen.fw import Mod, ore
from gen import tex
from mods._dims import dimension_type, dimension, biome, checkerboard, ore_in

M = 'fw_skylands'


def gate_tex(seed):
    img = tex.chiseled('#d8dce8', seed)
    px = img.load()
    for i in range(3, 13):
        px[i, 7] = (110, 190, 255, 255)
        px[7, i] = (110, 190, 255, 255)
    return img


def cloud_tex(seed):
    img = tex.noise_fill('#f4f6fa', seed, 0.04)
    px = img.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            px[x, y] = (r, g, b, 200)
    return img


def build():
    m = Mod(M, 'The Skylands', 'A sunny dimension of floating islands drifting over the void, reached through a Skylands Gate: flowery '
                               'meadows and forests in the sky, soft clouds that break your fall, skyshard ore, and the lair of the Sky Wyvern. '
                               'Fall off and you tumble back to the overworld.', tab='WORLD')
    rl = m.rl
    m.init('com.frontierworks.skylands.SkylandsEvents.init(bus)')
    m.imp('com.frontierworks.core.block.GateBlock')
    dimension_type(m, 'skylands', 0, 256, skylight=True, ceiling=False, ambient=0.0, effects='minecraft:overworld', natural=True, bed=True)
    m.block('cloud', java='new com.frontierworks.skylands.CloudBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.3f).sound(SoundType.WOOL).noOcclusion())',
            jtype='com.frontierworks.skylands.CloudBlock',
            models={'cloud': {'parent': 'minecraft:block/cube_all', 'render_type': 'minecraft:translucent', 'textures': {'all': '%s:block/cloud' % M}}},
            textures={'block/cloud': cloud_tex('cloud')}, mine='hoe', tab='WORLD', desc='Soft: you take no fall damage landing on it')
    m.block('skystone', java='new Block(FWProps.stone(1.5f))', textures={'block/skystone': tex.stone('#c8ccd8', 'skystone', 'mottled')}, tab='WORLD')
    ore(m, 'skyshard_ore', '#7ad0ff', base_tex='%s:block/skystone' % M, drop=rl('skyshard'), xp=(2, 5), hardness=3.0, style='gem', level='iron')
    m.item('skyshard', ('shard', '#7ad0ff'), desc='Light as air', tags=['forge:gems/skyshard'])
    m.block('skylands_gate', java='new GateBlock(FWProps.stone(5.0f, 1200.0f).lightLevel(s -> 10), "fw_skylands:skylands", "minecraft:overworld")', jtype='GateBlock',
            textures={'block/skylands_gate': gate_tex('sgate')}, tab='WORLD', level='iron',
            desc='Right-click to travel to the Skylands (and back). Builds a return gate on arrival.')
    m.shaped('skylands_gate', rl('skylands_gate'), ['FEF', 'EDE', 'FEF'], {'F': 'minecraft:feather', 'E': 'minecraft:ender_pearl', 'D': 'minecraft:diamond'})
    m.shaped('skylands_gate_upgrade', rl('skylands_gate'), ['QSQ', 'SES', 'QSQ'], {'Q': 'minecraft:quartz_block', 'S': rl('skyshard'), 'E': 'minecraft:ender_pearl'}, 2)
    m.shapeless('cloud', rl('cloud'), ['minecraft:white_wool', rl('skyshard')], 4)
    m.item('skyshard_wings', ('glider', '#7ad0ff') if 'glider' in tex.T else ('orb', '#7ad0ff'), desc='Carry it: slow falling while sneaking in the air',
           java='new com.frontierworks.skylands.SkyWingsItem(new Item.Properties().stacksTo(1))', jtype='com.frontierworks.skylands.SkyWingsItem', tab='TOOLS')
    m.shaped('skyshard_wings', rl('skyshard_wings'), ['SFS', 'FPF', 'SFS'], {'S': rl('skyshard'), 'F': 'minecraft:feather', 'P': 'minecraft:phantom_membrane'})
    m.field('public static final RegistryObject<net.minecraft.world.level.levelgen.feature.Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration>> CLOUD_FEATURE = '
            'REG.features().register("cloud", () -> new com.frontierworks.skylands.CloudFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));')
    m.configured('clouds', {'type': rl('cloud'), 'config': {}})
    m.placed('clouds', rl('clouds'), [{'type': 'minecraft:rarity_filter', 'chance': 3}, {'type': 'minecraft:in_square'},
                                      {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'min_inclusive': {'absolute': 40},
                                                                                    'max_inclusive': {'absolute': 200}}}, {'type': 'minecraft:biome'}])
    shard = ore_in(m, 'skyshard_ore', rl('skyshard_ore'), 'minecraft:stone', size=5, count=8, lo=0, hi=256)
    skystone = ore_in(m, 'skystone_blob', rl('skystone'), 'minecraft:stone', size=40, count=10, lo=0, hi=256)
    ores = ['minecraft:ore_coal_upper', 'minecraft:ore_iron_upper', 'minecraft:ore_gold', 'minecraft:ore_emerald', skystone, shard]
    spawns = {'creature': [('minecraft:sheep', 12, 2, 4), ('minecraft:chicken', 10, 2, 4), ('minecraft:rabbit', 6, 2, 3), ('fw_mobs:deer', 8, 2, 3)],
              'monster': [('minecraft:phantom', 5, 1, 2), ('minecraft:skeleton', 40, 1, 3), ('minecraft:zombie', 40, 1, 3), ('fw_mobs:scrap_drone', 20, 1, 2)],
              'ambient': [('minecraft:bat', 2, 1, 2)]}
    b1 = biome(m, 'sky_meadow', '#8ad0ff', '#c8e8ff', temp=0.7, downfall=0.6, precip=True, grass='#7ad060', foliage='#5ab048', spawns=spawns,
               features={'underground_ores': ores, 'vegetal_decoration': ['minecraft:flower_meadow', 'minecraft:patch_grass_plain', 'minecraft:trees_meadow',
                                                                         rl('clouds')]})
    b2 = biome(m, 'cloud_forest', '#a8dcff', '#e0f0ff', temp=0.6, downfall=0.8, precip=True, grass='#5ac070', foliage='#3aa060', spawns=spawns,
               features={'underground_ores': ores, 'vegetal_decoration': ['minecraft:flower_meadow', 'minecraft:patch_grass_plain',
                                                                         'minecraft:trees_flower_forest', rl('clouds')]})
    dimension(m, 'skylands', 'minecraft:floating_islands', checkerboard([b1, b2], 4), lang='The Skylands')
    return m
