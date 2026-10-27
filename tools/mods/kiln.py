from gen.fw import Mod, fuel_machine, recipe_kind
from gen import tex


def build():
    m = Mod('fw_kiln', 'Kilns', 'Fuel-fired early-game machines: the Alloy Kiln makes bronze, brass and steel before you have power, '
                                'and the Coke Oven turns coal into high-energy coal coke.', tab='MACHINES')
    ka = recipe_kind(m, 'kiln_alloying')
    co = recipe_kind(m, 'coking')
    fuel_machine(m, 'alloy_kiln', ka, 2, 1, 'steam', 'flame', desc='Burns fuel to smelt two metals into an alloy')
    fuel_machine(m, 'coke_oven', co, 1, 1, 'steam', 'flame', desc='Bakes coal into coal coke and logs into charcoal')
    rl = m.rl
    m.item('coal_coke', ('raw', '#4a4a50'), java='new FuelItem(new Item.Properties(), 3200)', jtype='FuelItem',
           tags=['forge:coal_coke'], desc='Burns twice as long as coal')
    m.item('coke_dust', ('dust', '#3a3a40'), tags=['forge:dusts/coal_coke', 'forge:dusts'])
    m.block('coal_coke_block', props='FWProps.stone(3.0f)', textures={'block/coal_coke_block': tex.raw_block('#3d3d44', 'cokeblock')},
            item_java='b -> new FuelItem.Block_(b, new Item.Properties(), 32000)', tags=['forge:storage_blocks/coal_coke'],
            item_tags=['forge:storage_blocks/coal_coke'], level='stone')
    m.item('fire_clay', ('raw', '#c9a27e'), desc='Mixed clay used for kiln bricks')
    m.item('kiln_brick', ('bar', '#a5573a', None, '#d07a52'))
    m.block('kiln_bricks', props='FWProps.stone(2.0f)', textures={'block/kiln_bricks': tex.bricks('#a5573a', 'kb', '#d8c8a8')}, tab='DECOR')
    m.shapeless('fire_clay', rl('fire_clay'), ['minecraft:clay_ball', 'minecraft:clay_ball', '#minecraft:sand', 'minecraft:gravel'], 4)
    m.smelt('kiln_brick', rl('fire_clay'), rl('kiln_brick'), 0.2)
    m.shaped('kiln_bricks', rl('kiln_bricks'), ['BB', 'BB'], {'B': rl('kiln_brick')})
    m.shaped('alloy_kiln', rl('alloy_kiln'), ['BBB', 'BFB', 'BBB'], {'B': rl('kiln_brick'), 'F': 'minecraft:furnace'})
    m.shaped('coke_oven', rl('coke_oven'), ['BCB', 'BFB', 'BBB'], {'B': rl('kiln_brick'), 'F': 'minecraft:furnace', 'C': 'minecraft:coal_block'})
    m.shaped('coal_coke_block', rl('coal_coke_block'), ['###', '###', '###'], {'#': rl('coal_coke')})
    m.shapeless('coal_coke_from_block', rl('coal_coke'), [rl('coal_coke_block')], 9)
    m.mrecipe('fw_kiln:coking', 'coke', [('minecraft:coal', 1)], [(rl('coal_coke'), 1)], time=600, energy=0)
    m.mrecipe('fw_kiln:coking', 'coke_block', [('minecraft:coal_block', 1)], [(rl('coal_coke_block'), 1)], time=4800, energy=0)
    m.mrecipe('fw_kiln:coking', 'charcoal', [('#minecraft:logs_that_burn', 1)], [('minecraft:charcoal', 2)], time=400, energy=0)
    m.mrecipe('fw_machines:pulverizing', 'coke_dust', [(rl('coal_coke'), 1)], [(rl('coke_dust'), 1)], time=100, energy=16)
    m.mrecipe('fw_alloysmelter:alloying', 'coke', [('minecraft:coal', 2), ('fw_metals:coal_dust', 1)], [(rl('coal_coke'), 2)], time=300, energy=20)
    return m
