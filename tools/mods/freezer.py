from gen.fw import Mod, processing_machine, recipe_kind
from gen import tex


def build():
    m = Mod('fw_freezer', 'Cryogenics', 'The Cryo Freezer turns water into ice and lava into obsidian, and makes cryotheum - '
                                        'a super-coolant used by reactors and cooling coils.', tab='MACHINES')
    rl = m.rl
    k = recipe_kind(m, 'freezing')
    processing_machine(m, 'cryo_freezer', k, 1, 1, 'advanced', 'snow', capacity=40000, max_in=400, tanks_in=1, tank_cap=16000,
                       desc='Freezes fluids and items')
    m.item('cryotheum_dust', ('dust', '#7fe0ff'), tags=['forge:dusts/cryotheum'], tab='MATERIALS', desc='Absurdly cold. Reactor coolant.')
    m.block('cryotheum_block', props='FWProps.stone(1.0f).friction(0.98f)', textures={'block/cryotheum_block': tex.gem_block('#7fe0ff', 'cryo')},
            tab='DECOR', desc='Slippery as ice')
    m.shaped('cryo_freezer', rl('cryo_freezer'), ['PCP', 'IFI', 'CWC'], {'P': '#forge:plates/aluminum', 'C': 'fw_parts:cooling_coil', 'I': 'minecraft:packed_ice',
                                                                        'F': 'fw_parts:machine_frame', 'W': 'fw_circuits:advanced_circuit'})
    m.shaped('cryotheum_block', rl('cryotheum_block'), ['##', '##'], {'#': rl('cryotheum_dust')})
    F = 'fw_freezer:freezing'
    m.mrecipe(F, 'ice', [], [('minecraft:ice', 1)], time=60, energy=20, fluid_in=('minecraft:water', 1000))
    m.mrecipe(F, 'obsidian', [], [('minecraft:obsidian', 1)], time=120, energy=40, fluid_in=('minecraft:lava', 1000))
    m.mrecipe(F, 'packed_ice', [('minecraft:ice', 2)], [('minecraft:packed_ice', 1)], time=80, energy=20)
    m.mrecipe(F, 'cryotheum', [('minecraft:snowball', 2), ('#forge:dusts/redstone', 1)], [(rl('cryotheum_dust'), 2)], time=160, energy=60,
              fluid_in=('minecraft:water', 250))
    m.mrecipe(F, 'magma_to_basalt', [('minecraft:magma_block', 1)], [('minecraft:basalt', 1)], time=80, energy=20, fluid_in=('minecraft:water', 100))
    return m
