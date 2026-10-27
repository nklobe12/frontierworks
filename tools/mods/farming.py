from gen.fw import Mod, custom_machine


def build():
    m = Mod('fw_farming', 'Farm Automation', 'The Crop Harvester tends a 9x9 field around itself: harvests and replants crops and '
                                             'nether wart, cuts sugar cane, picks melons and pumpkins, and applies fertilizer.', tab='LOGISTICS')
    custom_machine(m, 'crop_harvester', 'HarvesterBE', 'basic', 'plus', tab='LOGISTICS',
                   desc='Place at crop level in the middle of your field. Feed bone meal or fertilizer to grow faster.')
    m.shaped('crop_harvester', m.rl('crop_harvester'), ['PHP', 'MFM', 'PWP'], {'P': '#forge:plates/iron', 'H': 'minecraft:iron_hoe', 'M': 'fw_parts:motor',
                                                                               'F': 'fw_parts:machine_frame', 'W': 'fw_circuits:basic_circuit'})
    m.tr('gui.fw_farming.fert', '<- fertilizer')
    return m
