from gen.fw import Mod, processing_machine, recipe_kind


def build():
    m = Mod('fw_press', 'Metal Press', 'A powered Metal Press that stamps ingots into plates, gears, rods and wires using '
                                       'reusable steel molds.', tab='MACHINES')
    rl = m.rl
    k = recipe_kind(m, 'pressing')
    processing_machine(m, 'metal_press', k, 2, 2, 'basic', 'press', capacity=30000, max_in=300,
                       desc='Insert a mold and metal ingots. The mold comes back out.')
    m.shaped('metal_press', rl('metal_press'), ['PIP', 'GMG', 'CWC'], {'P': '#forge:plates/steel', 'I': 'minecraft:piston', 'G': 'fw_parts:iron_gear',
                                                                       'M': 'fw_parts:machine_frame', 'C': 'fw_parts:motor', 'W': 'fw_circuits:basic_circuit'})
    m.item('blank_mold', ('plate', '#7b8591'), desc='Shape it into a mold on a crafting table')
    m.shaped('blank_mold', rl('blank_mold'), ['PP', 'PP'], {'P': '#forge:plates/steel'})
    molds = {'plate_mold': ['BH'], 'gear_mold': ['B', 'H'], 'rod_mold': ['B ', ' H'], 'wire_mold': ['H', 'B']}
    for name, pat in molds.items():
        m.item(name, ('card', '#7b8591'), tags=['fw_press:molds'], desc='Reusable press mold')
        m.shaped(name, rl(name), pat, {'B': rl('blank_mold'), 'H': 'fw_parts:engineers_hammer'})
    return m
