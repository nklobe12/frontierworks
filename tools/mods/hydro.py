from gen.fw import Mod, custom_model_machine
from gen import tex


def build():
    m = Mod('fw_hydro', 'Hydro Power', 'Water wheels and hydro turbines that turn rivers and waterfalls into energy - '
                                       'flowing water on each side adds power.', tab='MACHINES')
    rl = m.rl
    for name, per, tier in (('water_wheel', 6, 'steam'), ('hydro_turbine', 24, 'advanced')):
        textures = {'block/%s_side' % name: tex.machine_front(tier, 'wave', False, name), 'block/%s_top' % name: tex.casing(tier, m.modid + name, 'top')}
        custom_model_machine(m, name, 'new WaterWheelBE(BE_TYPE, pos, state, %d)' % per, 'WaterWheelBE', textures, top=name + '_top',
                             side=name + '_side', desc='%d FE/t per side touching flowing water' % per)
    m.shaped('water_wheel', rl('water_wheel'), ['SPS', 'PGP', 'SPS'], {'S': '#forge:rods/wooden', 'P': '#minecraft:planks', 'G': 'fw_parts:bronze_gear'})
    m.shaped('hydro_turbine', rl('hydro_turbine'), ['PFP', 'WMW', 'CGC'], {'P': '#forge:plates/steel', 'F': 'fw_parts:fan', 'W': rl('water_wheel'),
                                                                          'M': 'fw_parts:advanced_frame', 'C': 'fw_parts:motor', 'G': 'fw_circuits:advanced_circuit'})
    m.tr('gui.fw_hydro.tip', 'Place next to flowing water')
    return m
