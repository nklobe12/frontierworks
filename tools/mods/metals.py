from gen.fw import Mod, ore, title
from gen import tex
from mods._materials import METALS, VANILLA_METALS


def build():
    m = Mod('fw_metals', 'Metals', 'Eight new ores - tin, lead, silver, nickel, zinc, aluminum, platinum and uranium - '
                                   'with raw ores, ingots, nuggets, dusts and storage blocks, plus dusts for vanilla metals.')
    for name, (color, gen, level, hardness) in METALS.items():
        rl = m.rl
        ore(m, name + '_ore', color, 'stone', drop=rl('raw_' + name), hardness=hardness, level=level,
            item_tags=['forge:ores/' + name, 'forge:ores'], tags=['forge:ores/' + name, 'forge:ores'], lang=title(name) + ' Ore')
        ore(m, 'deepslate_' + name + '_ore', color, 'deepslate', drop=rl('raw_' + name), hardness=hardness, level=level,
            item_tags=['forge:ores/' + name, 'forge:ores'], tags=['forge:ores/' + name, 'forge:ores'], lang='Deepslate ' + title(name) + ' Ore')
        m.item('raw_' + name, ('raw', color), tags=['forge:raw_materials/' + name, 'forge:raw_materials'])
        m.item(name + '_ingot', ('ingot', color), tags=['forge:ingots/' + name, 'forge:ingots'])
        m.item(name + '_nugget', ('nugget', color), tags=['forge:nuggets/' + name, 'forge:nuggets'])
        m.item(name + '_dust', ('dust', color), tags=['forge:dusts/' + name, 'forge:dusts'])
        m.block(name + '_block', props='FWProps.metal(5.0f)', textures={'block/%s_block' % name: tex.metal_block(color, m.modid + name)},
                level=level, tags=['forge:storage_blocks/' + name, 'forge:storage_blocks', 'minecraft:beacon_base_blocks'],
                item_tags=['forge:storage_blocks/' + name, 'forge:storage_blocks'], lang='Block of ' + title(name))
        m.block('raw_%s_block' % name, props='FWProps.stone(5.0f)', textures={'block/raw_%s_block' % name: tex.raw_block(color, m.modid + 'raw' + name)},
                level=level, tags=['forge:storage_blocks/raw_' + name], item_tags=['forge:storage_blocks/raw_' + name],
                lang='Block of Raw ' + title(name))

        size, count, lo, hi, shape = gen
        m.ore_gen('ore_' + name, rl(name + '_ore'), rl('deepslate_%s_ore' % name), size=size, count=count, min_y=lo, max_y=hi, shape=shape)

        ing, nug, blk, raw, rawb, dust = (rl(name + '_ingot'), rl(name + '_nugget'), rl(name + '_block'), rl('raw_' + name),
                                          rl('raw_%s_block' % name), rl(name + '_dust'))
        m.shaped(name + '_block', blk, ['###', '###', '###'], {'#': '#forge:ingots/' + name})
        m.shapeless(name + '_ingot_from_block', ing, ['#forge:storage_blocks/' + name], 9)
        m.shaped(name + '_ingot_from_nuggets', ing, ['###', '###', '###'], {'#': '#forge:nuggets/' + name})
        m.shapeless(name + '_nugget', nug, ['#forge:ingots/' + name], 9)
        m.shaped('raw_%s_block' % name, rawb, ['###', '###', '###'], {'#': raw})
        m.shapeless('raw_%s' % name, raw, [rawb], 9)
        m.smelt(name + '_ingot_from_raw', raw, ing, 0.7)
        m.smelt(name + '_ingot_from_ore', '#forge:ores/' + name, ing, 0.7)
        m.smelt(name + '_ingot_from_dust', '#forge:dusts/' + name, ing, 0.3, time=150)
        m.tag('items', 'forge:ingots', '#forge:ingots/' + name)
        m.tag('items', 'forge:nuggets', '#forge:nuggets/' + name)
        m.tag('items', 'forge:dusts', '#forge:dusts/' + name)

    for name, color in VANILLA_METALS.items():
        m.item(name + '_dust', ('dust', color), tags=['forge:dusts/' + name, 'forge:dusts'])
        m.smelt(name + '_ingot_from_dust', '#forge:dusts/' + name, 'minecraft:%s_ingot' % name, 0.3, time=150)
    m.item('coal_dust', ('dust', '#2b2b2e'), tags=['forge:dusts/coal', 'forge:dusts'])
    m.item('diamond_dust', ('dust', '#7ee6e0'), tags=['forge:dusts/diamond', 'forge:dusts'])
    m.item('obsidian_dust', ('dust', '#3a2a55'), tags=['forge:dusts/obsidian', 'forge:dusts'])
    m.item('ender_dust', ('dust', '#2a8f7e'), tags=['forge:dusts/ender_pearl', 'forge:dusts'])
    m.item('stone_dust', ('dust', '#8a8a8a'), tags=['forge:dusts/stone', 'forge:dusts'])
    m.smelt('diamond_from_dust', '#forge:dusts/diamond', 'minecraft:diamond', 1.0, blast=False)
    m.tr('item.fw_metals.uranium_ingot.desc', 'Radioactive fuel stock. Handle with care.')
    return m
