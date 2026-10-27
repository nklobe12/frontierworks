from gen.fw import Mod, custom_machine
from gen import tex


def build():
    m = Mod('fw_nuclear', 'Nuclear Power', 'A fission reactor: load uranium fuel rods, keep it fed with water, and manage the heat. '
                                           'More rods mean more efficiency - and more risk. Auto-SCRAM can save you from a meltdown.', tab='MACHINES')
    rl = m.rl
    custom_machine(m, 'fission_reactor', 'ReactorBE', 'nuclear', 'atom',
                   desc='1 rod: 150 FE/t. 4 rods: 1140 FE/t but 16x the heat. Needs water. Redstone signal = shutdown.')
    m.item('uranium_fuel_rod', ('rod', '#6ec54b'), java='new FuelRodItem(new Item.Properties(), 24000, true)', jtype='FuelRodItem', tab='MATERIALS')
    m.item('depleted_fuel_rod', ('rod', '#4a5a40'), java='new FuelRodItem(new Item.Properties(), 0, true)', jtype='FuelRodItem', tab='MATERIALS',
           desc='Reprocess it in a Centrifuge')
    m.item('reactor_plating', ('sheet', '#5f6b4a'), tab='MATERIALS')
    m.shaped('reactor_plating', rl('reactor_plating'), ['LPL', 'PGP', 'LPL'], {'L': '#forge:plates/lead', 'P': 'fw_parts:reinforced_plate', 'G': 'fw_minerals:graphite'}, 2)
    m.shaped('uranium_fuel_rod', rl('uranium_fuel_rod'), ['ZUZ', 'ZGZ', 'ZUZ'], {'Z': '#forge:plates/zinc', 'U': '#forge:ingots/uranium', 'G': 'fw_minerals:graphite_rod'})
    m.shaped('fission_reactor', rl('fission_reactor'), ['RCR', 'GFG', 'RTR'], {'R': rl('reactor_plating'), 'C': 'fw_circuits:elite_circuit',
                                                                              'G': 'fw_minerals:graphite_rod', 'F': 'fw_parts:advanced_frame', 'T': 'fw_tanks:tank_basic'})
    m.mrecipe('fw_centrifuge:centrifuging', 'depleted_rod', [(rl('depleted_fuel_rod'), 1)], [('fw_metals:uranium_dust', 1, 0.6), ('fw_metals:lead_dust', 2),
                                                                                             ('fw_parts:zinc_plate', 2), ('fw_minerals:graphite', 1, 0.5)], time=400, energy=80)
    for k, v in {'gui.fw_nuclear.toggle': 'Start', 'gui.fw_nuclear.scram': 'SCRAM', 'gui.fw_nuclear.scram_on': 'Auto-SCRAM', 'gui.fw_nuclear.scram_off': 'Manual',
                 'gui.fw_nuclear.heat': 'Heat: %s%%', 'tooltip.fw_nuclear.rod_life': 'Fuel left: %s s', 'tooltip.fw_nuclear.radioactive': 'Radioactive'}.items():
        m.tr(k, v)
    return m
