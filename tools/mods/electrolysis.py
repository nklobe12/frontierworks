from gen.fw import Mod, processing_machine, recipe_kind, fluid


def build():
    m = Mod('fw_electrolysis', 'Electrolysis', 'The Electrolyzer splits water into hydrogen and oxygen, brine into chlorine and '
                                               'sodium, and heavy water into deuterium for fusion.', tab='MACHINES')
    rl = m.rl
    k = recipe_kind(m, 'electrolyzing')
    processing_machine(m, 'electrolyzer', k, 1, 1, 'advanced', 'bolt', capacity=60000, max_in=600, tanks_in=1, tanks_out=2, tank_cap=16000,
                       desc='Splits fluids into their elements with electricity')
    fluid(m, 'hydrogen', '#cfe8ff', gas=True, bucket=False, temperature=280)
    fluid(m, 'oxygen', '#ff9a9a', gas=True, bucket=False, temperature=280)
    fluid(m, 'chlorine', '#c8f07a', gas=True, bucket=False, temperature=280)
    fluid(m, 'deuterium', '#7ab8ff', gas=True, bucket=False, temperature=280)
    fluid(m, 'heavy_water', '#3a5fd0', density=1100)
    m.item('salt', ('dust', '#f4f4f4'), tags=['forge:dusts/salt', 'forge:salt'], tab='MATERIALS')
    m.item('sodium', ('ingot', '#d8d0c0'), tab='MATERIALS')
    m.shaped('electrolyzer', rl('electrolyzer'), ['GTG', 'WFW', 'CBC'], {'G': '#forge:glass', 'T': 'fw_tanks:tank_basic', 'W': '#forge:wires/electrum',
                                                                        'F': 'fw_parts:machine_frame', 'C': 'fw_circuits:advanced_circuit', 'B': 'fw_batteries:battery_basic'})
    E = 'fw_electrolysis:electrolyzing'
    m.mrecipe(E, 'water', [], [], time=100, energy=80, fluid_in=('minecraft:water', 500), fluid_out=(rl('hydrogen'), 500), fluid_out2=(rl('oxygen'), 250))
    m.mrecipe(E, 'brine', [(rl('salt'), 2)], [(rl('sodium'), 1)], time=160, energy=120, fluid_in=('minecraft:water', 250), fluid_out=(rl('chlorine'), 250))
    m.mrecipe(E, 'heavy_water', [], [], time=200, energy=200, fluid_in=(rl('heavy_water'), 500), fluid_out=(rl('deuterium'), 500), fluid_out2=(rl('oxygen'), 250))
    m.mrecipe('fw_centrifuge:centrifuging', 'heavy_water', [('minecraft:ice', 1)], [('minecraft:snowball', 1, 0.5)], time=200, energy=60,
              fluid_out=(rl('heavy_water'), 50))
    m.mrecipe('fw_freezer:freezing', 'salt', [], [(rl('salt'), 1)], time=200, energy=40, fluid_in=('minecraft:water', 1000))
    return m
