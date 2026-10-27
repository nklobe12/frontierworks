from gen.fw import Mod, custom_model_machine
from gen import tex


def build():
    m = Mod('fw_geothermal', 'Geothermal Power', 'Geothermal generators that draw heat from lava pools and magma - '
                                                 '50% stronger in the Nether.', tab='MACHINES')
    rl = m.rl
    for name, per, tier in (('geothermal_generator', 16, 'steam'), ('magma_dynamo', 48, 'elite')):
        textures = {'block/%s_side' % name: tex.machine_front(tier, 'flame', True, name), 'block/%s_top' % name: tex.casing(tier, m.modid + name, 'top')}
        custom_model_machine(m, name, 'new GeothermalBE(BE_TYPE, pos, state, %d)' % per, 'GeothermalBE', textures, top=name + '_top',
                             side=name + '_side', desc='%d FE/t per adjacent lava source block' % per)
    m.shaped('geothermal_generator', rl('geothermal_generator'), ['PGP', 'IMI', 'CHC'], {'P': '#forge:plates/invar', 'G': '#forge:glass', 'I': '#forge:ingots/copper',
                                                                                       'M': 'fw_parts:machine_frame', 'C': 'fw_circuits:basic_circuit', 'H': 'fw_parts:heating_coil'})
    m.shaped('magma_dynamo', rl('magma_dynamo'), ['PHP', 'GMG', 'CWC'], {'P': '#forge:plates/platinum', 'H': 'fw_parts:heating_coil', 'G': rl('geothermal_generator'),
                                                                        'M': 'fw_parts:elite_frame', 'C': 'fw_parts:servo', 'W': 'fw_circuits:elite_circuit'})
    m.tr('gui.fw_geothermal.tip', 'Surround with lava')
    return m
