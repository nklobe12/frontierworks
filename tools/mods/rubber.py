from gen.fw import Mod, wood_set, tree_feature, tree_placement


def build():
    m = Mod('fw_rubber', 'Rubber', 'Rubber trees grow in jungles, forests and swamps. Tap their logs with a Treetap (or use an '
                                   'Extractor) for latex, then smelt it into rubber for cables, belts and seals.', tab='WORLD')
    rl = m.rl
    log, leaves, sap = wood_set(m, 'rubber', '#5a4a3a', '#d8c690', '#c9a96e', '#4f8a3a', rl('rubber_tree'))
    tree_feature(m, 'rubber_tree', rl(log), rl(leaves), height=6, rand_a=3, radius=2)
    tree_placement(m, 'rubber_tree', rl('rubber_tree'), rl(sap), chance=10,
                   biomes={'type': 'forge:or', 'values': ['#minecraft:is_jungle', '#minecraft:is_forest', '#forge:is_swamp']})
    for n in (log, 'rubber_wood', 'stripped_rubber_log'):
        m.tag('blocks', 'fw_rubber:rubber_logs', rl(n))
        m.tag('items', 'fw_rubber:rubber_logs', rl(n))
    m.item('treetap', ('hook', '#a0723c'), java='new TreetapItem(new Item.Properties())', jtype='TreetapItem', tab='TOOLS',
           desc='Right-click a rubber log to collect latex')
    m.item('latex', ('orb', '#f2ecd8'), tags=['forge:latex'])
    m.item('rubber', ('bar', '#2d2d2d', None, '#4a4a4a'), tags=['forge:rubber'], tab='MATERIALS')
    m.item('rubber_sheet', ('sheet', '#333333'), tab='MATERIALS', tags=['forge:plates/rubber'])
    m.shaped('treetap', rl('treetap'), [' P ', 'PPP', 'P  '], {'P': '#minecraft:planks'})
    m.smelt('rubber', rl('latex'), rl('rubber'), 0.3)
    m.shapeless('rubber_sheet', rl('rubber_sheet'), ['fw_parts:engineers_hammer', rl('rubber'), rl('rubber')])
    m.mrecipe('fw_machines:compressing', 'rubber_sheet', [(rl('rubber'), 2)], [(rl('rubber_sheet'), 1)], time=120, energy=16)
    m.mrecipe('fw_machines:extracting', 'latex_from_sapling', [(rl(sap), 1)], [(rl('latex'), 1)], time=100, energy=16)
    m.mrecipe('fw_sawmill:sawing', 'rubber_log', [('#fw_rubber:rubber_logs', 1)], [(rl('rubber_planks'), 6), ('fw_sawmill:sawdust', 1, 0.5),
                                                                                     (rl('latex'), 1, 0.25)], time=100, energy=16)
    return m
