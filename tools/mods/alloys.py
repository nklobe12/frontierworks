from gen.fw import Mod, title
from gen import tex
from mods._materials import ALLOYS

# alloy: (dust recipe [(dust tag, count)], output count, alloy-smelter inputs [(ingredient, count)])
RECIPES = {
    'bronze': ([('copper', 3), ('tin', 1)], 4, [('#forge:ingots/copper', 3), ('#forge:ingots/tin', 1)]),
    'brass': ([('copper', 3), ('zinc', 1)], 4, [('#forge:ingots/copper', 3), ('#forge:ingots/zinc', 1)]),
    'electrum': ([('gold', 1), ('silver', 1)], 2, [('#forge:ingots/gold', 1), ('#forge:ingots/silver', 1)]),
    'invar': ([('iron', 2), ('nickel', 1)], 3, [('#forge:ingots/iron', 2), ('#forge:ingots/nickel', 1)]),
    'constantan': ([('copper', 1), ('nickel', 1)], 2, [('#forge:ingots/copper', 1), ('#forge:ingots/nickel', 1)]),
    'duralumin': ([('aluminum', 3), ('copper', 1)], 4, [('#forge:ingots/aluminum', 3), ('#forge:ingots/copper', 1)]),
    'steel': (None, 1, [('#forge:ingots/iron', 1), ('#forge:coal_coke', 1)]),
}


def build():
    m = Mod('fw_alloys', 'Alloys', 'Bronze, brass, steel, electrum, invar, constantan and duralumin - the alloys every '
                                   'Frontierworks machine is built from. Mix dusts by hand early on, then use the Alloy Kiln or Alloy Smelter.')
    for name, color in ALLOYS.items():
        rl = m.rl
        m.item(name + '_ingot', ('ingot', color), tags=['forge:ingots/' + name, 'forge:ingots'])
        m.item(name + '_nugget', ('nugget', color), tags=['forge:nuggets/' + name, 'forge:nuggets'])
        m.item(name + '_dust', ('dust', color), tags=['forge:dusts/' + name, 'forge:dusts'])
        m.block(name + '_block', props='FWProps.metal(5.0f)', textures={'block/%s_block' % name: tex.metal_block(color, m.modid + name, 'riveted' if name == 'steel' else 'plain')},
                level='stone', tags=['forge:storage_blocks/' + name, 'forge:storage_blocks', 'minecraft:beacon_base_blocks'],
                item_tags=['forge:storage_blocks/' + name, 'forge:storage_blocks'], lang='Block of ' + title(name))
        ing, nug, blk, dust = rl(name + '_ingot'), rl(name + '_nugget'), rl(name + '_block'), rl(name + '_dust')
        m.shaped(name + '_block', blk, ['###', '###', '###'], {'#': '#forge:ingots/' + name})
        m.shapeless(name + '_ingot_from_block', ing, ['#forge:storage_blocks/' + name], 9)
        m.shaped(name + '_ingot_from_nuggets', ing, ['###', '###', '###'], {'#': '#forge:nuggets/' + name})
        m.shapeless(name + '_nugget', nug, ['#forge:ingots/' + name], 9)
        m.smelt(name + '_ingot_from_dust', '#forge:dusts/' + name, ing, 0.4, time=200)
        dusts, n, smelter = RECIPES[name]
        if dusts:
            ings = []
            for d, c in dusts:
                ings += ['#forge:dusts/' + d] * c
            m.shapeless(name + '_dust_mix', dust, ings, n)
        # alloy smelter + alloy kiln recipes
        m.mrecipe('fw_alloysmelter:alloying', 'alloying_' + name, smelter, [(ing, n)], time=200, energy=30)
        kiln_inputs = smelter if name != 'steel' else [('#forge:ingots/iron', 1), ('#forge:coal_coke', 2)]
        m.mrecipe('fw_kiln:kiln_alloying', 'kiln_' + name, kiln_inputs, [(ing, n)], time=400, energy=0)
    m.mrecipe('fw_alloysmelter:alloying', 'alloying_steel_from_dust', [('#forge:dusts/iron', 1), ('#forge:dusts/coal', 2)],
              [(m.rl('steel_ingot'), 1)], time=200, energy=30)
    m.tr('item.fw_alloys.steel_ingot.desc', 'Smelt iron with coal coke in an Alloy Kiln or Alloy Smelter.')
    return m
