from gen.fw import Mod, custom_machine


def build():
    m = Mod('fw_quarry', 'Quarry', 'An automatic Quarry that strips a 9x9 area (up to 41x41 with Range Upgrades) all the way '
                                   'to bedrock, replacing liquids with stone and sending everything to a chest on top.', tab='LOGISTICS')
    custom_machine(m, 'quarry', 'QuarryBE', 'advanced', 'pick', tab='LOGISTICS',
                   desc='Mines the area below it down to bedrock. Put a chest on top for the output.')
    m.shaped('quarry', m.rl('quarry'), ['PDP', 'MFM', 'CWC'], {'P': '#forge:plates/steel', 'D': 'minecraft:diamond_pickaxe', 'M': 'fw_parts:motor',
                                                               'F': 'fw_parts:advanced_frame', 'C': 'fw_parts:servo', 'W': 'fw_circuits:advanced_circuit'})
    m.tr('gui.fw_quarry.done', 'Finished!')
    m.tr('gui.fw_quarry.layer', 'Layer: Y=%s')
    return m
