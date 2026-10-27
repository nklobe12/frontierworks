from gen.fw import Mod, machine


def build():
    m = Mod('fw_generators', 'Generators', 'Your first power: the Combustion Generator burns any furnace fuel, the Reinforced '
                                           'Generator burns it hotter, and the Lava Generator drinks lava for steady energy.', tab='MACHINES')
    rl = m.rl
    machine(m, 'combustion_generator', 'new CombustionGeneratorBE(BE_TYPE, pos, state, 20, 1.0f, 40000)', 'basic', 'flame',
            be_class='CombustionGeneratorBE', desc='Burns furnace fuel for 20 FE/t')
    machine(m, 'reinforced_generator', 'new CombustionGeneratorBE(BE_TYPE, pos, state, 60, 0.8f, 120000)', 'advanced', 'flame',
            be_class='CombustionGeneratorBE', desc='Burns furnace fuel for 60 FE/t')
    m.imp('java.util.Map', 'java.util.LinkedHashMap')
    m.field('public static final Map<String, FluidGeneratorBE.Fuel> LAVA_FUELS = new LinkedHashMap<>();')
    m.init('Hooks.init()')
    machine(m, 'lava_generator', 'new FluidGeneratorBE(BE_TYPE, pos, state, Content.LAVA_FUELS, 100000)', 'steam', 'drop',
            be_class='FluidGeneratorBE', desc='Burns lava for 60 FE/t (20 mB per second)')
    m.shaped('combustion_generator', rl('combustion_generator'), ['PPP', 'PFP', 'CWC'], {'P': '#forge:plates/iron', 'F': 'minecraft:furnace',
                                                                                       'C': '#forge:ingots/copper', 'W': '#forge:dusts/redstone'})
    m.shaped('reinforced_generator', rl('reinforced_generator'), ['PHP', 'GFG', 'CWC'], {'P': '#forge:plates/steel', 'H': 'fw_parts:heating_coil',
                                                                                       'G': rl('combustion_generator'), 'F': 'fw_parts:machine_frame',
                                                                                       'C': '#forge:ingots/invar', 'W': 'fw_circuits:basic_circuit'})
    m.shaped('lava_generator', rl('lava_generator'), ['PBP', 'GFG', 'CWC'], {'P': '#forge:plates/bronze', 'B': 'minecraft:bucket', 'G': '#forge:glass',
                                                                           'F': rl('combustion_generator'), 'C': '#forge:ingots/copper', 'W': 'fw_circuits:basic_circuit'})
    return m
