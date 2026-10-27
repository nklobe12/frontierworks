from gen.fw import Mod, processing_machine, recipe_kind


def build():
    m = Mod('fw_circuits', 'Circuits', 'Silicon, wafers and the four circuit tiers - basic, advanced, elite and quantum - '
                                       'plus the Circuit Assembler that builds them efficiently.', tab='MATERIALS')
    rl = m.rl
    k = recipe_kind(m, 'assembling')
    processing_machine(m, 'circuit_assembler', k, 3, 1, 'advanced', 'chip', capacity=40000, max_in=400,
                       desc='Assembles circuits and processors from components')
    items = [
        ('silicon', ('ingot', '#8a8f99'), ['forge:silicon'], 'Made in an Alloy Smelter from sand and coal dust'),
        ('silicon_wafer', ('plate', '#6b7590'), [], None),
        ('basic_circuit', ('circuit', '#2f7d32', None, '#d9844f'), ['forge:circuits/basic'], None),
        ('advanced_circuit', ('circuit', '#1d4fa3', None, '#f2c94c'), ['forge:circuits/advanced'], None),
        ('elite_circuit', ('circuit', '#7a1fa2', None, '#ecd97a'), ['forge:circuits/elite'], None),
        ('quantum_circuit', ('circuit', '#111827', None, '#4fd8ff'), ['forge:circuits/ultimate'], 'The pinnacle of Frontierworks electronics'),
        ('processor', ('chip_item', '#30363d'), [], None),
        ('solar_cell', ('sheet', '#1f3a6b'), [], 'Photovoltaic cell for solar panels'),
    ]
    for name, t, tags, desc in items:
        if t[0] == 'chip_item':
            t = ('tablet', '#2b3038', None, '#4fd8ff')
        m.item(name, t, tags=tags, desc=desc)
    m.shaped('circuit_assembler', rl('circuit_assembler'), ['PCP', 'AFA', 'PWP'], {'P': '#forge:plates/steel', 'C': 'minecraft:crafting_table',
                                                                                    'A': 'fw_parts:servo', 'F': 'fw_parts:machine_frame', 'W': rl('basic_circuit')})
    # hand recipes (more expensive than the assembler)
    m.shaped('basic_circuit', rl('basic_circuit'), ['WWW', 'RPR', 'WWW'], {'W': '#forge:wires/copper', 'R': '#forge:dusts/redstone', 'P': '#forge:plates/tin'})
    m.shaped('advanced_circuit', rl('advanced_circuit'), ['GRG', 'BSB', 'GRG'], {'G': '#forge:wires/gold', 'R': '#forge:dusts/redstone',
                                                                                 'B': rl('basic_circuit'), 'S': rl('silicon_wafer')})
    m.shaped('solar_cell', rl('solar_cell'), ['GGG', 'WWW', 'PCP'], {'G': '#forge:glass_panes', 'W': rl('silicon_wafer'), 'P': '#forge:plates/aluminum',
                                                                     'C': '#forge:wires/copper'})
    m.mrecipe('fw_machines:compressing', 'wafer', [(rl('silicon'), 1)], [(rl('silicon_wafer'), 2)], time=100, energy=20)
    m.shapeless('wafer_hand', rl('silicon_wafer'), ['fw_parts:engineers_hammer', rl('silicon')])
    A = 'fw_circuits:assembling'
    m.mrecipe(A, 'basic', [('#forge:wires/copper', 3), ('#forge:dusts/redstone', 2), ('#forge:plates/tin', 1)], [(rl('basic_circuit'), 2)], time=100, energy=40)
    m.mrecipe(A, 'advanced', [(rl('basic_circuit'), 2), ('#forge:wires/gold', 2), (rl('silicon_wafer'), 1)], [(rl('advanced_circuit'), 1)], time=160, energy=60)
    m.mrecipe(A, 'processor', [(rl('silicon_wafer'), 2), ('#forge:wires/gold', 2), ('#forge:gems/quartz', 1)], [(rl('processor'), 1)], time=160, energy=60)
    m.mrecipe(A, 'elite', [(rl('advanced_circuit'), 2), (rl('processor'), 1), ('fw_crystals:charged_energite', 1)], [(rl('elite_circuit'), 1)], time=240, energy=120)
    m.mrecipe(A, 'quantum', [(rl('elite_circuit'), 2), ('fw_moon:helium3_crystal', 2), ('fw_crystals:voidite_shard', 2)], [(rl('quantum_circuit'), 1)],
              time=400, energy=400)
    m.mrecipe(A, 'solar_cell', [(rl('silicon_wafer'), 3), ('#forge:glass_panes', 2), ('#forge:wires/copper', 2)], [(rl('solar_cell'), 2)], time=120, energy=40)
    return m
