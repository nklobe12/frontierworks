from gen.fw import Mod, processing_machine, recipe_kind, fluid, machine


def build():
    m = Mod('fw_petro', 'Petrochemicals', 'The Refinery splits crude oil into diesel and lubricant; the Diesel Generator burns '
                                          'diesel, biodiesel and rocket fuel for serious power.', tab='MACHINES')
    rl = m.rl
    k = recipe_kind(m, 'refining')
    processing_machine(m, 'refinery', k, 0, 1, 'advanced', 'drop', capacity=60000, max_in=600, tanks_in=1, tanks_out=2, tank_cap=16000,
                       desc='Crude oil in, diesel and lubricant out')
    fluid(m, 'diesel', '#e3c34a', density=850, viscosity=1200)
    fluid(m, 'lubricant', '#8a6a2a', density=900, viscosity=2500)
    fluid(m, 'biodiesel', '#b8d04a', density=880, viscosity=1300)
    m.imp('java.util.Map', 'java.util.LinkedHashMap')
    m.field('public static final Map<String, FluidGeneratorBE.Fuel> FUELS = new LinkedHashMap<>();')
    m.init('Hooks.registerFuels()')
    machine(m, 'diesel_generator', 'new FluidGeneratorBE(BE_TYPE, pos, state, Content.FUELS, 200000)', 'advanced', 'flame',
            be_class='FluidGeneratorBE', desc='Burns diesel (80 FE/t), biodiesel (60 FE/t) or rocket fuel (160 FE/t)')
    m.shaped('refinery', rl('refinery'), ['PTP', 'HFH', 'CWC'], {'P': '#forge:plates/steel', 'T': 'fw_tanks:tank_basic', 'H': 'fw_parts:heating_coil',
                                                                 'F': 'fw_parts:advanced_frame', 'C': 'fw_parts:motor', 'W': 'fw_circuits:advanced_circuit'})
    m.shaped('diesel_generator', rl('diesel_generator'), ['PTP', 'GFG', 'CWC'], {'P': '#forge:plates/steel', 'T': 'fw_tanks:tank_basic', 'G': 'fw_parts:steel_gear',
                                                                                 'F': 'fw_parts:machine_frame', 'C': '#forge:ingots/copper', 'W': 'fw_circuits:basic_circuit'})
    m.mrecipe('fw_petro:refining', 'crude_oil', [], [], time=60, energy=60, fluid_in=('fw_oil:crude_oil', 500), fluid_out=(rl('diesel'), 300),
              fluid_out2=(rl('lubricant'), 150))
    m.mrecipe('fw_petro:refining', 'seed_oil', [], [], time=80, energy=40, fluid_in=('fw_food:seed_oil', 500), fluid_out=(rl('biodiesel'), 400))
    return m
