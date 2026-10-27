from gen.fw import Mod, ore, title
from gen import tex
from mods._materials import GEMS


def build():
    m = Mod('fw_gems', 'Gems', 'Ruby, sapphire, peridot, topaz and onyx: deep ores, gem blocks, and the gems used for '
                               'jewellery, relics and high-tier tools.')
    for name, (color, size, count, lo, hi, level) in GEMS.items():
        rl = m.rl
        ore(m, name + '_ore', color, 'stone', drop=rl(name), xp=(3, 7), hardness=3.5, style='gem', level=level,
            tags=['forge:ores/' + name, 'forge:ores'], item_tags=['forge:ores/' + name, 'forge:ores'])
        ore(m, 'deepslate_%s_ore' % name, color, 'deepslate', drop=rl(name), xp=(3, 7), hardness=3.5, style='gem', level=level,
            tags=['forge:ores/' + name, 'forge:ores'], item_tags=['forge:ores/' + name, 'forge:ores'], lang='Deepslate %s Ore' % title(name))
        m.item(name, ('gem', color), tags=['forge:gems/' + name, 'forge:gems'])
        m.block(name + '_block', props='FWProps.gemBlock(5.0f)', textures={'block/%s_block' % name: tex.gem_block(color, name)},
                level=level, tags=['forge:storage_blocks/' + name, 'minecraft:beacon_base_blocks'],
                item_tags=['forge:storage_blocks/' + name], lang='Block of ' + title(name))
        m.ore_gen('ore_' + name, rl(name + '_ore'), rl('deepslate_%s_ore' % name), size=size, count=count, min_y=lo, max_y=hi)
        m.shaped(name + '_block', rl(name + '_block'), ['###', '###', '###'], {'#': '#forge:gems/' + name})
        m.shapeless(name + '_from_block', rl(name), ['#forge:storage_blocks/' + name], 9)
        m.smelt(name + '_from_ore', '#forge:ores/' + name, rl(name), 1.0)
    return m
