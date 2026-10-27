from gen.fw import Mod, machine, fluid


def build():
    m = Mod('fw_steam', 'Steam Power', 'Steam-age power: Boilers burn fuel to boil water, Steam Engines and the advanced Steam '
                                       'Turbine turn that steam into Forge Energy. Boilers heat up over time and push steam to neighbours.', tab='MACHINES')
    rl = m.rl
    fluid(m, 'steam', '#e8eef2', gas=True, bucket=False, temperature=400)
    m.imp('java.util.Map', 'java.util.LinkedHashMap')
    m.field('public static final Map<String, FluidGeneratorBE.Fuel> ENGINE_FUELS = new LinkedHashMap<>();')
    m.field('public static final Map<String, FluidGeneratorBE.Fuel> TURBINE_FUELS = new LinkedHashMap<>();')
    m.init('Hooks.init()')
    machine(m, 'boiler', 'new BoilerBE(BE_TYPE, pos, state, 40)', 'steam', 'flame', be_class='BoilerBE',
            desc='Burns fuel to turn water into up to 40 mB/t of steam')
    machine(m, 'high_pressure_boiler', 'new BoilerBE(BE_TYPE, pos, state, 120)', 'advanced', 'flame', be_class='BoilerBE',
            desc='Burns fuel to turn water into up to 120 mB/t of steam')
    machine(m, 'steam_engine', 'new FluidGeneratorBE(BE_TYPE, pos, state, Content.ENGINE_FUELS, 60000)', 'steam', 'gear',
            be_class='FluidGeneratorBE', desc='20 mB/t of steam into 40 FE/t')
    machine(m, 'steam_turbine', 'new FluidGeneratorBE(BE_TYPE, pos, state, Content.TURBINE_FUELS, 200000)', 'advanced', 'spin',
            be_class='FluidGeneratorBE', desc='60 mB/t of steam into 160 FE/t')
    m.shaped('boiler', rl('boiler'), ['PPP', 'PBP', 'BFB'], {'P': '#forge:plates/copper', 'B': 'minecraft:bricks', 'F': 'minecraft:furnace'})
    m.shaped('high_pressure_boiler', rl('high_pressure_boiler'), ['PPP', 'PBP', 'HFH'], {'P': '#forge:plates/steel', 'B': rl('boiler'),
                                                                                        'H': 'fw_parts:heating_coil', 'F': 'fw_parts:machine_frame'})
    m.shaped('steam_engine', rl('steam_engine'), ['PGP', 'GPG', 'CRC'], {'P': '#forge:plates/bronze', 'G': 'fw_parts:bronze_gear', 'C': '#forge:ingots/copper',
                                                                        'R': 'minecraft:piston'})
    m.shaped('steam_turbine', rl('steam_turbine'), ['PFP', 'FMF', 'CWC'], {'P': '#forge:plates/steel', 'F': 'fw_parts:fan', 'M': 'fw_parts:advanced_frame',
                                                                          'C': 'fw_parts:motor', 'W': 'fw_circuits:advanced_circuit'})
    m.tr('gui.fw_steam.heat', 'Heat: %s%%')
    m.tr('gui.fw_steam.steam_rate', 'Steam: %s mB/t')
    return m
