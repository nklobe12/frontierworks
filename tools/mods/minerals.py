from gen.fw import Mod, ore, title
from gen import tex
from mods._materials import MINERALS


def build():
    m = Mod('fw_minerals', 'Minerals', 'Sulfur, saltpeter, graphite, fluorite and apatite ores - the raw chemistry behind '
                                       'explosives, fertilizer, batteries and reactor parts.')
    shapes = {'sulfur': 'dust', 'saltpeter': 'dust', 'graphite': 'raw', 'fluorite': 'shard', 'apatite': 'gem'}
    for name, (color, size, count, lo, hi, cnt) in MINERALS.items():
        rl = m.rl
        ore(m, name + '_ore', color, 'stone', drop=rl(name), xp=(1, 4), counts=cnt, style='specks', level='stone',
            tags=['forge:ores/' + name, 'forge:ores'], item_tags=['forge:ores/' + name, 'forge:ores'])
        ore(m, 'deepslate_%s_ore' % name, color, 'deepslate', drop=rl(name), xp=(1, 4), counts=cnt, style='specks', level='stone',
            tags=['forge:ores/' + name, 'forge:ores'], item_tags=['forge:ores/' + name, 'forge:ores'], lang='Deepslate %s Ore' % title(name))
        tag = 'forge:gems/' + name if shapes[name] in ('gem', 'shard') else 'forge:dusts/' + name
        m.item(name, (shapes[name], color), tags=[tag])
        m.block(name + '_block', props='FWProps.stone(2.0f)', textures={'block/%s_block' % name: tex.raw_block(color, name + 'blk')},
                level='stone', tags=['forge:storage_blocks/' + name], item_tags=['forge:storage_blocks/' + name], lang='Block of ' + title(name))
        m.ore_gen('ore_' + name, rl(name + '_ore'), rl('deepslate_%s_ore' % name), size=size, count=count, min_y=lo, max_y=hi)
        m.shaped(name + '_block', rl(name + '_block'), ['###', '###', '###'], {'#': rl(name)})
        m.shapeless(name + '_from_block', rl(name), [rl(name + '_block')], 9)
    rl = m.rl
    m.tag('items', 'forge:dusts/sulfur', rl('sulfur'))
    m.tag('items', 'forge:dusts/saltpeter', rl('saltpeter'))
    m.shapeless('gunpowder', 'minecraft:gunpowder', [rl('saltpeter'), rl('saltpeter'), rl('sulfur'), 'fw_metals:coal_dust'], 4)
    m.shapeless('bone_meal_from_apatite', 'minecraft:bone_meal', [rl('apatite')], 4)
    m.smelt('graphite_from_coal_dust', 'fw_metals:coal_dust', rl('graphite'), 0.1, blast=True)
    m.item('graphite_rod', ('rod', '#3b3d42'), desc='Neutron moderator for fission reactors')
    m.shaped('graphite_rod', rl('graphite_rod'), ['G', 'G', 'G'], {'G': rl('graphite')}, 2)
    return m
