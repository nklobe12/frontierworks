from gen.fw import Mod, processing_machine, recipe_kind


def build():
    m = Mod('fw_centrifuge', 'Centrifuge', 'The Centrifuge spins materials apart into their components: slag into metals, '
                                           'dirt into clay and sand, magma cream into slime and blaze powder, and moon dust into helium-3.', tab='MACHINES')
    rl = m.rl
    k = recipe_kind(m, 'centrifuging')
    processing_machine(m, 'centrifuge', k, 1, 4, 'advanced', 'spin', capacity=40000, max_in=400, tanks_out=1, tank_cap=8000,
                       desc='Separates mixtures into up to four products and a fluid')
    m.shaped('centrifuge', rl('centrifuge'), ['PGP', 'MFM', 'CWC'], {'P': '#forge:plates/steel', 'G': 'fw_parts:steel_gear', 'M': 'fw_parts:motor',
                                                                    'F': 'fw_parts:machine_frame', 'C': '#forge:ingots/invar', 'W': 'fw_circuits:advanced_circuit'})
    C = 'fw_centrifuge:centrifuging'
    for name, ing, outs, fl in [
        ('slag', 'fw_orewasher:slag', [('minecraft:iron_nugget', 3, 0.6), ('fw_metals:tin_nugget', 2, 0.4), ('fw_metals:lead_nugget', 2, 0.3), ('minecraft:gold_nugget', 1, 0.15)], None),
        ('dirt', 'minecraft:dirt', [('minecraft:clay_ball', 1, 0.5), ('minecraft:sand', 1, 0.5), ('minecraft:wheat_seeds', 1, 0.1)], None),
        ('magma_cream', 'minecraft:magma_cream', [('minecraft:slime_ball', 1), ('minecraft:blaze_powder', 1)], None),
        ('glowstone_dust', 'minecraft:glowstone_dust', [('minecraft:redstone', 1, 0.5), ('fw_metals:gold_dust', 1, 0.25)], None),
        ('redstone', 'minecraft:redstone', [('fw_metals:iron_dust', 1, 0.15)], None),
        ('sawdust', 'fw_sawmill:sawdust', [('minecraft:stick', 1, 0.2), ('minecraft:charcoal', 1, 0.05)], None),
        ('rotten_flesh', 'minecraft:rotten_flesh', [('minecraft:leather', 1, 0.3), ('minecraft:bone_meal', 1, 0.3)], None),
        ('melon', 'minecraft:melon_slice', [('minecraft:melon_seeds', 1, 0.5), ('minecraft:sugar', 1, 0.3)], ('minecraft:water', 100)),
        ('soul_sand', 'minecraft:soul_sand', [('minecraft:sand', 1), ('fw_metals:coal_dust', 1, 0.25), ('minecraft:quartz', 1, 0.1)], None),
        ('moon_dust', 'fw_moon:moon_dust', [('fw_moon:helium3_crystal', 1, 0.2), ('fw_metals:aluminum_dust', 1, 0.3), ('fw_metals:iron_dust', 1, 0.2)], None),
        ('mars_dust', 'fw_mars:mars_dust', [('fw_mars:titanium_dust', 1, 0.15), ('fw_metals:iron_dust', 1, 0.4), ('minecraft:red_sand', 1, 0.5)], None),
        ('electrum_dust', '#forge:dusts/electrum', [('fw_metals:gold_dust', 1), ('fw_metals:silver_dust', 1)], None),
        ('bronze_dust', '#forge:dusts/bronze', [('fw_metals:copper_dust', 3), ('fw_metals:tin_dust', 1)], None),
    ]:
        m.mrecipe(C, name, [(ing, 1)], outs, time=160, energy=40, fluid_out=fl)
    return m
