from gen.fw import Mod, processing_machine, recipe_kind
from gen import tex
from gen.structures import loot_table

DROPS = {
    'gravel': [('minecraft:flint', 0.25), ('minecraft:iron_nugget', 0.15), ('fw_metals:tin_nugget', 0.15), ('minecraft:gold_nugget', 0.06),
               ('fw_metals:lead_nugget', 0.08), ('fw_metals:zinc_nugget', 0.08), ('fw_gems:ruby', 0.015), ('fw_gems:sapphire', 0.015)],
    'sand': [('fw_metals:aluminum_nugget', 0.12), ('fw_metals:nickel_nugget', 0.06), ('minecraft:gold_nugget', 0.06), ('minecraft:cactus', 0.03),
             ('fw_gems:topaz', 0.015), ('fw_gems:peridot', 0.015), ('fw_minerals:saltpeter', 0.08)],
    'dirt': [('minecraft:wheat_seeds', 0.2), ('minecraft:beetroot_seeds', 0.06), ('minecraft:melon_seeds', 0.04), ('minecraft:pumpkin_seeds', 0.04),
             ('minecraft:potato', 0.05), ('minecraft:carrot', 0.05), ('minecraft:bone_meal', 0.1), ('fw_minerals:apatite', 0.06)],
    'soul_sand': [('minecraft:quartz', 0.2), ('minecraft:gold_nugget', 0.15), ('minecraft:nether_wart', 0.05), ('minecraft:ghast_tear', 0.01),
                  ('fw_minerals:sulfur', 0.12), ('fw_metals:silver_nugget', 0.06)],
}


def build():
    m = Mod('fw_sifting', 'Sifting', 'Early-game resource gathering: sift gravel, sand, dirt and soul sand by hand for nuggets, '
                                     'gems and seeds, then automate it with the powered Auto-Sifter.', tab='MACHINES')
    rl = m.rl
    m.block('sifter', java='new SifterBlock(FWProps.wood().noOcclusion())', jtype='SifterBlock', mine='axe',
            models={'sifter': {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': {
                'particle': '%s:block/sifter_side' % m.modid, 'side': '%s:block/sifter_side' % m.modid, 'mesh': '%s:block/sifter_mesh' % m.modid},
                'elements': [
                    {'from': [0, 0, 0], 'to': [16, 10, 16], 'faces': {
                        'north': {'texture': '#side', 'uv': [0, 6, 16, 16]}, 'south': {'texture': '#side', 'uv': [0, 6, 16, 16]},
                        'east': {'texture': '#side', 'uv': [0, 6, 16, 16]}, 'west': {'texture': '#side', 'uv': [0, 6, 16, 16]},
                        'down': {'texture': '#side'}, 'up': {'texture': '#mesh'}}}]}},
            textures={'block/sifter_side': tex.planks('#9a7048', 'sifter'), 'block/sifter_mesh': _mesh()},
            desc='Right-click with gravel, sand, dirt or soul sand to sift it')
    m.shaped('sifter', rl('sifter'), ['P P', 'SSS', 'P P'], {'P': '#minecraft:planks', 'S': 'minecraft:string'})
    k = recipe_kind(m, 'sifting')
    processing_machine(m, 'auto_sifter', k, 1, 4, 'basic', 'grid', desc='Sifts automatically with power - four output slots')
    m.shaped('auto_sifter', rl('auto_sifter'), ['PSP', 'PMP', 'CWC'], {'P': '#forge:plates/iron', 'S': rl('sifter'), 'M': 'fw_parts:machine_frame',
                                                                       'C': 'fw_parts:motor', 'W': 'fw_circuits:basic_circuit'})
    blocks = {'gravel': 'minecraft:gravel', 'sand': '#minecraft:sand', 'dirt': 'minecraft:dirt', 'soul_sand': 'minecraft:soul_sand'}
    for mat, drops in DROPS.items():
        # hand sifting loot table: each drop rolled independently
        pools = [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': it}],
                  'conditions': [{'condition': 'minecraft:random_chance', 'chance': ch}]} for it, ch in drops]
        m.data('loot_tables/sifting/%s.json' % mat, {'type': 'minecraft:chest', 'pools': pools})
        best = sorted(drops, key=lambda d: -d[1])[:4]
        m.mrecipe('fw_sifting:sifting', mat, [(blocks[mat], 1)], [(it, 1, min(1.0, ch * 1.5)) for it, ch in best], time=100, energy=20)
    return m


def _mesh():
    img = tex.planks('#9a7048', 'siftmesh')
    px = img.load()
    for y in range(2, 14):
        for x in range(2, 14):
            px[x, y] = (200, 200, 205, 255) if (x % 3 == 0 or y % 3 == 0) else (60, 50, 40, 255)
    return img
