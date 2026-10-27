from gen.fw import Mod, custom_model_machine
from gen import tex


def build():
    m = Mod('fw_wind', 'Wind Power', 'Wind turbines that love altitude and storms: build them high, give them open air, and '
                                     'they pay you back with free energy.', tab='MACHINES')
    rl = m.rl
    for name, mx, tier, col in (('wind_turbine', 24, 'basic', '#dfe5ea'), ('large_wind_turbine', 96, 'advanced', '#c7d1db')):
        textures = {'block/%s_side' % name: tex.casing(tier, m.modid + name, 'side'), 'block/%s_top' % name: tex.casing(tier, m.modid + name, 'top'),
                    'block/%s_front' % name: tex.machine_front(tier, 'spin', False, name), 'block/%s_front_on' % name: tex.machine_front(tier, 'spin', True, name)}
        custom_model_machine(m, name, 'new WindTurbineBE(BE_TYPE, pos, state, %d)' % mx, 'WindTurbineBE', textures, top=name + '_top',
                             side=name + '_side', front=name + '_front', front_on=name + '_front_on',
                             desc='Up to %d FE/t (more in storms). Higher is better; keep the space above clear.' % mx)
    m.item('turbine_blade', ('sheet', '#e6eaee'), tab='MATERIALS')
    m.shaped('turbine_blade', rl('turbine_blade'), ['  P', ' P ', 'P  '], {'P': '#forge:plates/aluminum'})
    m.shaped('wind_turbine', rl('wind_turbine'), [' B ', 'BMB', 'CGC'], {'B': rl('turbine_blade'), 'M': 'fw_parts:motor', 'C': '#forge:ingots/copper',
                                                                        'G': 'fw_parts:iron_gear'})
    m.shaped('large_wind_turbine', rl('large_wind_turbine'), ['BTB', 'TFT', 'CWC'], {'B': rl('turbine_blade'), 'T': rl('wind_turbine'),
                                                                                    'F': 'fw_parts:advanced_frame', 'C': 'fw_parts:steel_gear', 'W': 'fw_circuits:advanced_circuit'})
    m.tr('gui.fw_wind.tip', 'Higher = stronger wind')
    return m
