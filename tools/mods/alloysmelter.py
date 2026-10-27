from gen.fw import Mod, processing_machine, recipe_kind


def build():
    m = Mod('fw_alloysmelter', 'Alloy Smelter', 'An electric two-input furnace that fuses metals into alloys, '
                                                 'bakes silicon and makes glass products.', tab='MACHINES')
    k = recipe_kind(m, 'alloying')
    processing_machine(m, 'alloy_smelter', k, 2, 1, 'basic', 'flame', capacity=24000, max_in=240,
                       desc='Fuses two ingredients into one - alloys, silicon, hardened glass')
    m.shaped('alloy_smelter', m.rl('alloy_smelter'), ['PHP', 'FMF', 'CWC'], {'P': '#forge:plates/invar', 'H': 'fw_parts:heating_coil',
                                                                              'F': 'minecraft:furnace', 'M': 'fw_parts:machine_frame',
                                                                              'C': '#forge:ingots/copper', 'W': 'fw_circuits:basic_circuit'})
    A = 'fw_alloysmelter:alloying'
    m.mrecipe(A, 'silicon', [('#minecraft:sand', 2), ('fw_metals:coal_dust', 1)], [('fw_circuits:silicon', 1)], time=200, energy=30)
    m.mrecipe(A, 'hardened_glass', [('#forge:glass', 2), ('fw_metals:obsidian_dust', 1)], [('fw_glass:hardened_glass', 2)], time=160, energy=30)
    m.mrecipe(A, 'netherite', [('minecraft:netherite_scrap', 4), ('#forge:ingots/gold', 4)], [('minecraft:netherite_ingot', 1)], time=400, energy=60)
    m.mrecipe(A, 'redstone_alloy', [('#forge:ingots/iron', 1), ('#forge:dusts/redstone', 4)], [('fw_alloysmelter:redstone_alloy_ingot', 1)], time=200, energy=30)
    m.item('redstone_alloy_ingot', ('ingot', '#c0392b'), tags=['forge:ingots/redstone_alloy'], desc='Conductive alloy used in circuits and cables')
    return m
