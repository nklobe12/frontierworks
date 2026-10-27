from gen.fw import Mod, processing_machine, recipe_kind, fluid


def build():
    m = Mod('fw_chemistry', 'Chemistry', 'The Chemical Reactor combines solids and fluids: sulfuric acid, vulcanized rubber, '
                                         'gunpowder, fertilizer, plastic and rocket-grade chemicals.', tab='MACHINES')
    rl = m.rl
    k = recipe_kind(m, 'reacting')
    processing_machine(m, 'chemical_reactor', k, 2, 1, 'advanced', 'beaker', capacity=40000, max_in=400, tanks_in=1, tanks_out=1, tank_cap=16000,
                       desc='Two items plus a fluid in, an item and/or fluid out')
    fluid(m, 'sulfuric_acid', '#d8e04a', density=1800, viscosity=1500)
    m.item('vulcanized_rubber', ('bar', '#1f1f1f', None, '#333333'), tags=['forge:rubber/vulcanized'], tab='MATERIALS',
           desc='Heat-resistant rubber for advanced cables and seals')
    m.item('plastic_sheet', ('sheet', '#e8e8f0'), tags=['forge:plastic', 'forge:plates/plastic'], tab='MATERIALS')
    m.item('fertilizer', ('pellet', '#7a9a4a'), tab='MATERIALS', desc='Use on crops like bone meal. Works twice as well.')
    m.item('carbon_fiber', ('sheet', '#26282d'), tab='MATERIALS', tags=['forge:plates/carbon'], desc='Light, strong composite for gear and rockets')
    m.shaped('chemical_reactor', rl('chemical_reactor'), ['GTG', 'PFP', 'CWC'], {'G': '#forge:glass', 'T': 'fw_tanks:tank_basic', 'P': '#forge:plates/lead',
                                                                                 'F': 'fw_parts:machine_frame', 'C': 'fw_parts:motor', 'W': 'fw_circuits:advanced_circuit'})
    R = 'fw_chemistry:reacting'
    m.mrecipe(R, 'sulfuric_acid', [('#forge:dusts/sulfur', 2)], [], time=120, energy=40, fluid_in=('minecraft:water', 1000), fluid_out=(rl('sulfuric_acid'), 1000))
    m.mrecipe(R, 'vulcanized_rubber', [('#forge:rubber', 2), ('#forge:dusts/sulfur', 1)], [(rl('vulcanized_rubber'), 2)], time=160, energy=40)
    m.mrecipe(R, 'gunpowder', [('#forge:dusts/saltpeter', 2), ('#forge:dusts/sulfur', 1)], [('minecraft:gunpowder', 4)], time=100, energy=30)
    m.mrecipe(R, 'fertilizer', [('fw_minerals:apatite', 1), ('minecraft:bone_meal', 1)], [(rl('fertilizer'), 4)], time=100, energy=20,
              fluid_in=('minecraft:water', 250))
    m.mrecipe(R, 'plastic', [('fw_metals:coal_dust', 1)], [(rl('plastic_sheet'), 2)], time=160, energy=60, fluid_in=('fw_petro:lubricant', 250))
    m.mrecipe(R, 'carbon_fiber', [('fw_minerals:graphite', 2), (rl('plastic_sheet'), 1)], [(rl('carbon_fiber'), 1)], time=200, energy=80)
    m.mrecipe(R, 'acid_leach_gold', [('minecraft:gold_ore', 1)], [('fw_metals:gold_dust', 4)], time=200, energy=60, fluid_in=(rl('sulfuric_acid'), 250))
    m.mrecipe(R, 'acid_leach_uranium', [('#forge:ores/uranium', 1)], [('fw_metals:uranium_dust', 4)], time=240, energy=80, fluid_in=(rl('sulfuric_acid'), 250))
    m.mrecipe(R, 'battery_acid_cell', [('#forge:plates/lead', 2), ('#forge:plates/zinc', 1)], [('fw_batteries:battery_cell', 2)], time=160, energy=60,
              fluid_in=(rl('sulfuric_acid'), 250))
    m.mrecipe(R, 'concrete_powder', [('minecraft:sand', 4), ('minecraft:gravel', 4)], [('minecraft:white_concrete_powder', 8)], time=80, energy=20)
    return m
