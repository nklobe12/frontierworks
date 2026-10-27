from gen.fw import Mod, custom_machine


def build():
    m = Mod('fw_pump', 'Electric Pump', 'An electric pump that drains lakes of lava, oil or any fluid below it (water is '
                                        'infinite), and pushes the fluid into tanks and ducts around it.', tab='LOGISTICS')
    custom_machine(m, 'electric_pump', 'PumpBE', 'basic', 'tank', tab='LOGISTICS', desc='Place above a fluid. Water is never used up.')
    m.shaped('electric_pump', m.rl('electric_pump'), ['PBP', 'MFM', 'PDP'], {'P': '#forge:plates/iron', 'B': 'minecraft:bucket', 'M': 'fw_parts:motor',
                                                                            'F': 'fw_parts:machine_frame', 'D': 'fw_fluidducts:fluid_duct'})
    m.tr('gui.fw_pump.tip', 'Pumps from below')
    return m
