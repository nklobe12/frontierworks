from gen.fw import Mod, custom_machine


def build():
    m = Mod('fw_beacons', 'Field Projectors', 'Powered beacons: a Field Projector bathes every player within 16 blocks in your choice '
                                              'of ten effects - speed, haste, regeneration, resistance, night vision and more - for FE.', tab='MACHINES')
    custom_machine(m, 'field_projector', 'FieldProjectorBE', 'elite', 'sun',
                   desc='Gives nearby players a chosen effect. Range Upgrades extend it; Speed Upgrades raise the level.')
    m.shaped('field_projector', m.rl('field_projector'), ['GBG', 'PFP', 'CWC'], {'G': 'fw_glass:hardened_glass', 'B': 'minecraft:beacon', 'P': '#forge:plates/platinum',
                                                                                 'F': 'fw_parts:elite_frame', 'C': 'fw_crystals:charged_energite', 'W': 'fw_circuits:elite_circuit'})
    for k, v in {'gui.fw_beacons.next': 'Next effect', 'gui.fw_beacons.range': 'Range: %s', 'gui.fw_beacons.cost': '%s FE / pulse'}.items():
        m.tr(k, v)
    return m
