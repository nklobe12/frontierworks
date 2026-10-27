from gen.fw import Mod, custom_model_machine
from gen import tex

CELLS = [
    ('energy_cell_basic', 1_000_000, 1000, 'basic', '#c0392b'),
    ('energy_cell_advanced', 8_000_000, 4000, 'advanced', '#2980b9'),
    ('energy_cell_elite', 64_000_000, 16000, 'elite', '#8e44ad'),
    ('energy_cell_ultimate', 512_000_000, 65536, 'ultimate', '#16a085'),
]


def cell_face(tier, color, seed, lit):
    img = tex.casing(tier, seed, 'front')
    px = img.load()
    c = tex.rgb(color)
    for y in range(3, 13):
        for x in range(5, 11):
            on = (12 - y) < (8 if lit else 3)
            px[x, y] = (c[0], c[1], c[2], 255) if on else (30, 30, 36, 255)
        px[4, y] = px[11, y] = (60, 60, 66, 255)
    return img


def build():
    m = Mod('fw_batteries', 'Batteries', 'Energy cells from 1M to 512M FE that keep their charge when broken, plus portable '
                                         'batteries that power your gadgets and machines.', tab='MACHINES')
    rl = m.rl
    prev = None
    for name, cap, rate, tier, color in CELLS:
        textures = {'block/%s_side' % name: tex.casing(tier, m.modid + name, 'side'), 'block/%s_top' % name: tex.casing(tier, m.modid + name, 'top'),
                    'block/%s_front' % name: cell_face(tier, color, name, False), 'block/%s_front_on' % name: cell_face(tier, color, name, True)}
        custom_model_machine(m, name, 'new BatteryBE(BE_TYPE, pos, state, %d, %d)' % (cap, rate), 'BatteryBE', textures, top=name + '_top',
                             side=name + '_side', front=name + '_front', front_on=name + '_front_on',
                             desc='Stores %s FE. Outputs from the front, accepts on other sides. Keeps charge when broken.' % format(cap, ','))
        m.data('loot_tables/blocks/%s.json' % name, {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{
            'type': 'minecraft:item', 'name': rl(name), 'functions': [{'function': 'minecraft:copy_nbt', 'source': 'block_entity',
                                                                       'ops': [{'source': 'Energy', 'target': 'BlockEntityTag.Energy', 'op': 'replace'}]}]}]}]})
        prev = name
    items = [('battery_cell', ('battery', '#9aa0a8'), None), ('battery_basic', ('battery', '#c0392b'), 100_000),
             ('battery_advanced', ('battery', '#2980b9'), 1_000_000), ('battery_elite', ('battery', '#8e44ad'), 10_000_000)]
    for name, t, cap in items:
        if cap:
            m.item(name, t, java='new EnergyItem(new Item.Properties(), %d, %d, true)' % (cap, max(1000, cap // 100)), jtype='EnergyItem', tab='TOOLS',
                   desc='Portable energy. Put it in a machine battery slot or charge it in an energy cell.')
        else:
            m.item(name, t, tab='MATERIALS')
    m.shaped('battery_cell', rl('battery_cell'), [' W ', 'ZAL', 'ZAL'], {'W': '#forge:wires/copper', 'Z': '#forge:plates/zinc', 'A': '#forge:dusts/redstone',
                                                                         'L': '#forge:plates/lead'})
    m.shaped('battery_basic', rl('battery_basic'), [' W ', 'PCP', 'PCP'], {'W': '#forge:wires/copper', 'P': '#forge:plates/tin', 'C': rl('battery_cell')})
    m.shaped('battery_advanced', rl('battery_advanced'), ['GBG', 'BCB', 'GBG'], {'G': '#forge:wires/gold', 'B': rl('battery_basic'), 'C': 'fw_circuits:advanced_circuit'})
    m.shaped('battery_elite', rl('battery_elite'), ['EBE', 'BCB', 'EBE'], {'E': 'fw_crystals:charged_energite', 'B': rl('battery_advanced'),
                                                                           'C': 'fw_circuits:elite_circuit'})
    m.shaped('energy_cell_basic', rl('energy_cell_basic'), ['PBP', 'BFB', 'PWP'], {'P': '#forge:plates/lead', 'B': rl('battery_basic'),
                                                                                  'F': 'fw_parts:machine_frame', 'W': 'fw_circuits:basic_circuit'})
    m.shaped('energy_cell_advanced', rl('energy_cell_advanced'), ['PBP', 'CFC', 'PWP'], {'P': '#forge:plates/steel', 'B': rl('battery_advanced'),
                                                                                        'C': rl('energy_cell_basic'), 'F': 'fw_parts:advanced_frame',
                                                                                        'W': 'fw_circuits:advanced_circuit'})
    m.shaped('energy_cell_elite', rl('energy_cell_elite'), ['PBP', 'CFC', 'PWP'], {'P': '#forge:plates/platinum', 'B': rl('battery_elite'),
                                                                                  'C': rl('energy_cell_advanced'), 'F': 'fw_parts:elite_frame', 'W': 'fw_circuits:elite_circuit'})
    m.shaped('energy_cell_ultimate', rl('energy_cell_ultimate'), ['PBP', 'CFC', 'PWP'], {'P': 'fw_mars:titanium_plate', 'B': 'fw_crystals:charged_energite',
                                                                                        'C': rl('energy_cell_elite'), 'F': 'fw_parts:ultimate_frame',
                                                                                        'W': 'fw_circuits:quantum_circuit'})
    m.tr('gui.fw_batteries.io', 'Max I/O: %s FE/t')
    return m
