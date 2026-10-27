from gen.fw import Mod, fluid, ore
from gen import tex


def build():
    m = Mod('fw_oil', 'Oil', 'Crude oil lakes in deserts, badlands and plains, plus oil sand deposits. Refine crude into diesel, '
                             'lubricant and plastics, and pave roads with fast-walking asphalt.', tab='MATERIALS')
    rl = m.rl
    fluid(m, 'crude_oil', '#1c1a17', density=900, viscosity=3000)
    m.block('oil_sand', props='FWProps.sand(MapColor.TERRACOTTA_BROWN)', textures={'block/oil_sand': tex.noise_fill('#5a4a32', 'oilsand', 0.2, speck=0.12, speck_color='#1c1a17')},
            mine='shovel', loot=('ore', rl('bitumen'), 1, 2), tab='WORLD', desc='Oily sand that yields bitumen')
    m.item('bitumen', ('raw', '#25211c'), tags=['forge:bitumen'], desc='Sticky tar from oil sand. Makes asphalt.')
    m.block('asphalt', props='FWProps.stone(1.5f).speedFactor(1.35f)', textures={'block/asphalt': tex.noise_fill('#2b2b2e', 'asphalt', 0.18, speck=0.06, speck_color='#55555a')},
            tab='DECOR', desc='Smooth road surface - walk 35% faster')
    m.block('asphalt_lined', props='FWProps.stone(1.5f).speedFactor(1.35f)', tab='DECOR', textures={'block/asphalt_lined': (lambda: _lined())},
            desc='Road surface with a lane marking')
    m.shapeless('asphalt', rl('asphalt'), [rl('bitumen'), 'minecraft:gravel', 'minecraft:gravel', '#minecraft:sand'], 4)
    m.shapeless('asphalt_lined', rl('asphalt_lined'), [rl('asphalt'), 'minecraft:yellow_dye'])
    m.mrecipe('fw_machines:extracting', 'oil_from_bitumen', [(rl('bitumen'), 4)], [('minecraft:black_dye', 2)], time=120, energy=16)
    m.mrecipe('fw_centrifuge:centrifuging', 'oil_sand', [(rl('oil_sand'), 1)], [('minecraft:sand', 1), (rl('bitumen'), 1), (rl('bitumen'), 1, 0.5)],
              time=160, energy=30)

    lake = {'type': 'minecraft:lake', 'config': {
        'fluid': {'type': 'minecraft:simple_state_provider', 'state': {'Name': rl('crude_oil'), 'Properties': {'level': '0'}}},
        'barrier': {'type': 'minecraft:simple_state_provider', 'state': {'Name': 'minecraft:sandstone'}}}}
    m.configured('oil_lake', lake)
    m.placed('oil_lake', rl('oil_lake'), [{'type': 'minecraft:rarity_filter', 'chance': 24}, {'type': 'minecraft:in_square'},
                                          {'type': 'minecraft:heightmap', 'heightmap': 'WORLD_SURFACE_WG'}, {'type': 'minecraft:biome'}])
    m.add_feature(rl('oil_lake'), {'type': 'forge:or', 'values': ['#forge:is_desert', '#minecraft:is_badlands', '#forge:is_plains']}, 'lakes')
    m.configured('oil_lake_underground', lake)
    m.placed('oil_lake_underground', rl('oil_lake_underground'), [{'type': 'minecraft:rarity_filter', 'chance': 12}, {'type': 'minecraft:in_square'},
                                                                  {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'min_inclusive': {'absolute': -40}, 'max_inclusive': {'absolute': 40}}},
                                                                  {'type': 'minecraft:biome'}])
    m.add_feature(rl('oil_lake_underground'), '#minecraft:is_overworld', 'lakes')
    m.ore_gen('oil_sand', None, None, size=24, count=4, min_y=50, max_y=90, biomes={'type': 'forge:or', 'values': ['#forge:is_desert', '#minecraft:is_beach', '#minecraft:is_badlands']},
              extra_targets=[{'target': {'predicate_type': 'minecraft:block_match', 'block': 'minecraft:sand'}, 'state': {'Name': rl('oil_sand')}},
                             {'target': {'predicate_type': 'minecraft:block_match', 'block': 'minecraft:sandstone'}, 'state': {'Name': rl('oil_sand')}}])
    return m


def _lined():
    img = tex.noise_fill('#2b2b2e', 'asphaltl', 0.18, speck=0.06, speck_color='#55555a')
    px = img.load()
    for y in range(16):
        if y % 8 < 5:
            px[7, y] = (230, 200, 40, 255)
            px[8, y] = (230, 200, 40, 255)
    return img
