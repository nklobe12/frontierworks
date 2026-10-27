from gen.fw import Mod, custom_machine


def build():
    m = Mod('fw_fusion', 'Fusion Power', 'The end-game power source: ignite a fusion plasma with 2 million FE, then feed it '
                                         'deuterium and helium-3 from the Moon for 10,000 FE/t.', tab='MACHINES')
    rl = m.rl
    custom_machine(m, 'fusion_reactor', 'FusionBE', 'ultimate', 'atom',
                   desc='Ignite with 2M FE. Burns deuterium + helium-3 crystals for 10,000 FE/t.')
    m.item('plasma_containment_coil', ('wire', '#9b5de5', '#2a2438'), tab='MATERIALS')
    m.shaped('plasma_containment_coil', rl('plasma_containment_coil'), ['SES', 'ECE', 'SES'], {'S': 'fw_cables:superconductor_cable',
                                                                                               'E': 'fw_crystals:charged_energite', 'C': 'fw_parts:cooling_coil'})
    m.shaped('fusion_reactor', rl('fusion_reactor'), ['CQC', 'PFP', 'CRC'], {'C': rl('plasma_containment_coil'), 'Q': 'fw_circuits:quantum_circuit',
                                                                            'P': 'fw_mars:titanium_plate', 'F': 'fw_parts:ultimate_frame',
                                                                            'R': 'fw_nuclear:reactor_plating'})
    for k, v in {'gui.fw_fusion.ignite': 'Ignite', 'gui.fw_fusion.plasma': 'Plasma stable', 'gui.fw_fusion.cold': 'Reactor cold',
                 'gui.fw_fusion.needs': 'Needs 2M FE, D2, He-3', 'msg.fw_fusion.cannot_ignite': 'Ignition needs 2,000,000 FE, 100 mB deuterium and a helium-3 crystal'}.items():
        m.tr(k, v)
    return m
