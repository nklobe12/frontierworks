from gen.fw import Mod, processing_machine, recipe_kind
from gen import tex

WOODS = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry']


def build():
    m = Mod('fw_sawmill', 'Sawmill', 'An electric Sawmill that cuts logs into six planks plus sawdust, and planks into sticks. '
                                     'Sawdust becomes paper, particle board and fuel briquettes.', tab='MACHINES')
    rl = m.rl
    k = recipe_kind(m, 'sawing')
    processing_machine(m, 'sawmill', k, 1, 2, 'basic', 'saw', desc='Logs into 6 planks and sawdust')
    m.shaped('sawmill', rl('sawmill'), ['PBP', 'PMP', 'CWC'], {'P': '#forge:plates/iron', 'B': 'fw_parts:iron_gear', 'M': 'fw_parts:machine_frame',
                                                               'C': 'fw_parts:motor', 'W': 'fw_circuits:basic_circuit'})
    m.item('sawdust', ('dust', '#c9a46a'), tags=['forge:dusts/wood', 'forge:sawdust'])
    m.item('sawdust_briquette', ('bar', '#a8845a', None, '#c9a46a'), java='new FuelItem(new Item.Properties(), 1000)', jtype='FuelItem',
           desc='Compressed sawdust. Decent furnace fuel.')
    m.block('particle_board', props='FWProps.wood()', mine='axe', tab='DECOR',
            textures={'block/particle_board': tex.noise_fill('#c39a62', 'pboard', 0.25, speck=0.15, speck_color='#8a6a40')})
    m.shapeless('paper_from_sawdust', 'minecraft:paper', [rl('sawdust'), rl('sawdust'), rl('sawdust'), 'minecraft:water_bucket'], 3)
    m.shaped('particle_board', rl('particle_board'), ['SSS', 'SBS', 'SSS'], {'S': rl('sawdust'), 'B': 'minecraft:slime_ball'}, 4)
    m.mrecipe('fw_machines:compressing', 'briquette', [(rl('sawdust'), 4)], [(rl('sawdust_briquette'), 1)], time=80, energy=12)
    m.mrecipe('fw_machines:compressing', 'particle_board', [(rl('sawdust'), 4)], [(rl('particle_board'), 1)], time=120, energy=16)
    S = 'fw_sawmill:sawing'
    for w in WOODS:
        m.mrecipe(S, w + '_log', [('#minecraft:%s_logs' % w, 1)], [('minecraft:%s_planks' % w, 6), (rl('sawdust'), 1, 0.5)], time=100, energy=16)
    m.mrecipe(S, 'crimson', [('#minecraft:crimson_stems', 1)], [('minecraft:crimson_planks', 6), (rl('sawdust'), 1, 0.5)], time=100, energy=16)
    m.mrecipe(S, 'warped', [('#minecraft:warped_stems', 1)], [('minecraft:warped_planks', 6), (rl('sawdust'), 1, 0.5)], time=100, energy=16)
    m.mrecipe(S, 'bamboo', [('minecraft:bamboo_block', 1)], [('minecraft:bamboo_planks', 3), (rl('sawdust'), 1, 0.25)], time=80, energy=16)
    m.mrecipe(S, 'planks', [('#minecraft:planks', 1)], [('minecraft:stick', 3), (rl('sawdust'), 1, 0.25)], time=60, energy=12)
    m.mrecipe(S, 'sticks', [('minecraft:stick', 1)], [(rl('sawdust'), 1)], time=40, energy=8)
    return m
