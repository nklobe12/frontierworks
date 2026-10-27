from gen.fw import Mod, processing_machine, recipe_kind
from mods._materials import METALS
from mods.machines import BYPRODUCT


def build():
    m = Mod('fw_orewasher', 'Ore Washer', 'Tier-two ore processing: the Ore Washer scrubs ores and raw ores with water for '
                                          'triple yields, leaving slag that a Centrifuge can reclaim.', tab='MACHINES')
    rl = m.rl
    k = recipe_kind(m, 'washing')
    processing_machine(m, 'ore_washer', k, 1, 3, 'advanced', 'wave', capacity=40000, max_in=400, tanks_in=1, tank_cap=16000,
                       desc='Ore + water = three dusts. Raw ore + water = two dusts.')
    m.shaped('ore_washer', rl('ore_washer'), ['PBP', 'MFM', 'CWC'], {'P': '#forge:plates/steel', 'B': 'minecraft:bucket', 'M': 'fw_parts:motor',
                                                                    'F': 'fw_machines:pulverizer', 'C': '#forge:ingots/bronze', 'W': 'fw_circuits:advanced_circuit'})
    m.item('slag', ('raw', '#5d5650'), desc='Washing residue. Centrifuge it for metal nuggets.')
    W = 'fw_orewasher:washing'
    for name in list(METALS.keys()) + ['iron', 'gold', 'copper']:
        dust = 'fw_metals:%s_dust' % name
        bp = BYPRODUCT.get(name)
        m.mrecipe(W, 'ore_' + name, [('#forge:ores/' + name, 1)], [(dust, 3), (bp[0], 1, bp[1] * 2) if bp else (rl('slag'), 1, 0.3), (rl('slag'), 1, 0.5)],
                  time=200, energy=40, fluid_in=('minecraft:water', 500))
        raw = ('minecraft:raw_' + name) if name in ('iron', 'gold', 'copper') else 'fw_metals:raw_' + name
        m.mrecipe(W, 'raw_' + name, [(raw, 1)], [(dust, 2), (bp[0], 1, bp[1]) if bp else (rl('slag'), 1, 0.2), (rl('slag'), 1, 0.4)],
                  time=160, energy=40, fluid_in=('minecraft:water', 250))
    m.mrecipe(W, 'gravel', [('minecraft:gravel', 1)], [('minecraft:flint', 1), ('minecraft:iron_nugget', 1, 0.3), (rl('slag'), 1, 0.2)],
              time=100, energy=20, fluid_in=('minecraft:water', 100))
    m.mrecipe(W, 'dirt_to_clay', [('minecraft:dirt', 1)], [('minecraft:clay_ball', 2), ('minecraft:sand', 1, 0.3)],
              time=100, energy=20, fluid_in=('minecraft:water', 250))
    m.mrecipe(W, 'meteoric_iron', [('fw_meteors:raw_meteoric_iron', 1)], [('fw_meteors:raw_meteoric_iron', 1), ('fw_metals:nickel_dust', 1, 0.5)],
              time=160, energy=40, fluid_in=('minecraft:water', 250))
    return m
