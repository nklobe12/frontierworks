from gen.fw import Mod, stairs_slab_wall, title
from gen import tex

STONES = {
    # name: colour, natural style, y range
    'marble': ('#e8e4dc', 'veined', (0, 80)),
    'limestone': ('#d8cfae', 'layered', (20, 120)),
    'slate': ('#4f5560', 'layered', (-32, 40)),
    'gabbro': ('#3a3f3a', 'mottled', (-64, 0)),
    'travertine': ('#d9b98e', 'mottled', (30, 120)),
}


def build():
    m = Mod('fw_stone', 'Stoneworks', 'Five new natural stones - marble, limestone, slate, gabbro and travertine - generating in '
                                      'large underground veins, each with polished, brick, chiseled, stair, slab and wall variants.', tab='DECOR')
    for name, (color, style, (lo, hi)) in STONES.items():
        rl = m.rl
        m.block(name, props='FWProps.stone(1.5f)', textures={'block/' + name: tex.stone(color, name, style)},
                tab='WORLD', tags=['forge:stone', 'minecraft:base_stone_overworld'], item_tags=['forge:stone', 'minecraft:stone_crafting_materials', 'minecraft:stone_tool_materials'])
        m.block('polished_' + name, props='FWProps.stone(1.5f)', textures={'block/polished_' + name: tex.polished(color, name + 'p')})
        m.block(name + '_bricks', props='FWProps.stone(1.5f)', textures={'block/%s_bricks' % name: tex.bricks(color, name + 'b')})
        m.block('chiseled_' + name, props='FWProps.stone(1.5f)', textures={'block/chiseled_' + name: tex.chiseled(color, name + 'c')})
        stairs_slab_wall(m, name, '%s:block/%s' % (m.modid, name))
        stairs_slab_wall(m, 'polished_' + name, '%s:block/polished_%s' % (m.modid, name))
        stairs_slab_wall(m, name + '_bricks', '%s:block/%s_bricks' % (m.modid, name))
        m.shaped('polished_' + name, rl('polished_' + name), ['##', '##'], {'#': rl(name)}, 4)
        m.shaped(name + '_bricks', rl(name + '_bricks'), ['##', '##'], {'#': rl('polished_' + name)}, 4)
        m.shaped('chiseled_' + name, rl('chiseled_' + name), ['#', '#'], {'#': rl(name + '_slab'.replace(name + '_slab', name + '_brick_slab'))})
        # world generation: big blobs replacing stone/deepslate
        m.ore_gen('blob_' + name, rl(name), None, size=56, count=2, min_y=lo, max_y=hi, shape='uniform',
                  extra_targets=[{'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:deepslate_ore_replaceables'},
                                  'state': {'Name': rl(name)}}])
    return m
