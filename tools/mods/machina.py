from gen.ls import Mod, title, recipe_kind, processing_machine, custom_machine, fluid
from gen import tex
from mods._materials import METALS, VANILLA_METALS, ALLOYS, PART_METALS, metal_color, dust_item, ingot_item, nugget_item, block_item

WIRE_METALS = ['copper', 'gold', 'silver', 'aluminum', 'electrum', 'constantan', 'tungsten']
SECONDARY = {'iron': ('nickel', 0.1), 'gold': ('copper', 0.15), 'copper': ('gold', 0.1), 'tin': ('iron', 0.1), 'lead': ('silver', 0.15),
             'silver': ('lead', 0.15), 'nickel': ('platinum', 0.05), 'zinc': ('lead', 0.1), 'aluminum': ('iron', 0.1),
             'platinum': ('nickel', 0.15), 'uranium': ('lead', 0.2), 'cobalt': ('nickel', 0.15), 'tungsten': ('tin', 0.15)}
WOODS = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry']
COLORS = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown',
          'green', 'red', 'black']
AUG_TOKENS = {'speed': 'minecraft:redstone_block', 'efficiency': 'minecraft:lapis_block', 'capacity': 'ls_machina:redstone_coil',
              'output': 'minecraft:quartz_block', 'fluid': 'minecraft:bucket', 'range': 'minecraft:ender_pearl'}
AUG_COLORS = {'speed': '#e74c3c', 'efficiency': '#2ecc71', 'capacity': '#3498db', 'output': '#f1c40f', 'fluid': '#1abc9c', 'range': '#9b59b6'}
TIERS = {1: ('#forge:plates/bronze', 'minecraft:redstone'), 2: ('#forge:plates/electrum', 'minecraft:quartz'), 3: ('#forge:plates/platinum', 'minecraft:diamond')}


def build():
    m = Mod('ls_machina', 'Machina', 'Thirteen powered machines for processing: pulverizer (ore doubling), induction smelter, sawmill, '
                                      'insolator, centrifuge, magma crucible, blast chiller, compactor, refinery, fluid bottler, pyrolyzer, '
                                      'igneous extruder and an electric furnace. Every machine has augment slots, per-side configuration '
                                      'with auto-eject and auto-input, and redstone control.', tab='INDUSTRY')
    m.init('MachinaSetup.init(bus)')
    rl = m.rl

    # ----------------------------------------------------------------- components and materials
    m.item('machine_frame', ('frame', '#9aa3ad'), desc='The base of every Machina machine')
    m.item('redstone_coil', ('wire', '#c0392b', None, '#f2c94c'), desc='Moves energy into and out of machines')
    m.item('heating_coil', ('wire', '#d9844f', None, '#ff6a00'))
    m.item('blank_mold', ('plate', '#5f646b'), desc='Combine with an example item to make a mold')
    m.shaped('machine_frame', rl('machine_frame'), ['PGP', 'G G', 'PGP'], {'P': '#forge:plates/iron', 'G': '#forge:glass'})
    m.shaped('redstone_coil', rl('redstone_coil'), ['  R', ' G ', 'R  '], {'R': 'minecraft:redstone', 'G': '#forge:ingots/gold'})
    m.shaped('heating_coil', rl('heating_coil'), ['  W', ' R ', 'W  '], {'W': '#forge:wires/copper', 'R': 'minecraft:redstone'})
    m.shaped('blank_mold', rl('blank_mold'), ['PP', 'PP'], {'P': '#forge:plates/steel'})
    molds = {'plate': '#forge:plates', 'gear': '#forge:gears', 'rod': '#forge:rods', 'wire': '#forge:wires', 'ingot': '#forge:ingots',
             'nugget': '#forge:nuggets', 'block': '#forge:storage_blocks', 'ball': 'minecraft:snowball', 'packing_2x2': 'minecraft:piston',
             'packing_3x3': 'minecraft:crafting_table', 'unpacking': 'minecraft:shears'}
    for k, ex in molds.items():
        m.item(k + '_mold', ('plate', '#5f646b', None, '#e0e0ff'), lang=title(k).replace('2x2', '2x2').replace('3x3', '3x3') + ' Mold',
               desc='Not used up in the compactor or blast chiller', tags=['ls_machina:molds'])
        m.shapeless(k + '_mold', rl(k + '_mold'), [rl('blank_mold'), ex])
    m.item('coal_coke', ('raw', '#3a3a40'), java='new FuelItem(new Item.Properties(), 3200)', jtype='FuelItem', tags=['forge:coal_coke'])
    m.block('coal_coke_block', props='LSProps.stone(5.0f)', textures={'block/coal_coke_block': tex.raw_block('#3a3a40', m.modid + 'ccb')},
            item_java='b -> new FuelItem.Block_(b, new Item.Properties(), 32000)', tags=['forge:storage_blocks/coal_coke'],
            item_tags=['forge:storage_blocks/coal_coke'], tab='MATERIALS')
    m.shaped('coal_coke_block', rl('coal_coke_block'), ['###', '###', '###'], {'#': rl('coal_coke')})
    m.shapeless('coal_coke', rl('coal_coke'), [rl('coal_coke_block')], 9)
    m.item('tar', ('ball', '#1e1a16'), tab='MATERIALS', tags=['forge:tar'])
    m.item('sawdust', ('dust', '#c8a47a'), tab='MATERIALS', tags=['forge:dusts/wood', 'forge:sawdust'])
    m.item('slag', ('dust', '#6b6258'), tab='MATERIALS', desc='Leftover from smelting ores. Spin it in a centrifuge for scraps.')
    m.shapeless('paper_from_sawdust', 'minecraft:paper', [rl('sawdust'), rl('sawdust'), rl('sawdust'), 'minecraft:water_bucket'], 3)
    fluid(m, 'creosote', '#4a3a24', tab='MATERIALS')
    fluid(m, 'light_oil', '#d6c25a', tab='MATERIALS')

    # ----------------------------------------------------------------- augments
    for kind, token in AUG_TOKENS.items():
        for t in (1, 2, 3):
            n = '%s_augment_%d' % (kind, t)
            m.item(n, ('upgrade', AUG_COLORS[kind]), java='new AugmentItem(new Item.Properties(), AugmentItem.Kind.%s, %d)' % (kind.upper(), t),
                   jtype='AugmentItem', lang='%s Augment %s' % (title(kind), 'I' * t), tags=['ls_machina:augments'])
            plate, comp = TIERS[t]
            if t == 1:
                m.shaped(n, rl(n), ['PCP', 'CTC', 'PCP'], {'P': plate, 'C': comp, 'T': token})
            else:
                m.shaped(n, rl(n), ['PCP', 'CAC', 'PCP'], {'P': plate, 'C': comp, 'A': rl('%s_augment_%d' % (kind, t - 1))})

    # ----------------------------------------------------------------- machines
    k = {n: recipe_kind(m, n) for n in ('pulverizing', 'induction_smelting', 'sawing', 'insolating', 'centrifuging', 'melting', 'chilling',
                                        'pressing', 'refining', 'imbuing', 'pyrolysis')}
    processing_machine(m, 'electric_furnace', None, cooking='SMELTING', glyph='flame', time_factor=0.5, desc='Smelts like a furnace, twice as fast, on power')
    processing_machine(m, 'pulverizer', k['pulverizing'], outputs=2, glyph='crush', desc='Grinds ores into two dusts, plus rocks, bones and more')
    processing_machine(m, 'induction_smelter', k['induction_smelting'], inputs=2, outputs=2, glyph='flame',
                       desc='Smelts alloys, steel, and ores with sand for double ingots')
    processing_machine(m, 'sawmill', k['sawing'], outputs=2, glyph='saw', desc='Cuts six planks from a log, with sawdust')
    processing_machine(m, 'insolator', k['insolating'], outputs=3, tanks_in=1, glyph='sun', desc='Grows crops and trees from seeds, saplings and water')
    processing_machine(m, 'centrifuge', k['centrifuging'], outputs=4, tanks_out=1, glyph='spin', desc='Separates alloy dusts and mixtures')
    processing_machine(m, 'magma_crucible', k['melting'], outputs=0, tanks_out=1, tank_cap=16000, glyph='flame', capacity=40000,
                       desc='Melts rock into lava and metal into molten metal')
    processing_machine(m, 'blast_chiller', k['chilling'], tanks_in=1, tank_cap=16000, glyph='snow', desc='Freezes fluids into solids using molds')
    processing_machine(m, 'compactor', k['pressing'], inputs=2, glyph='press', desc='Presses plates, gears, rods, wires and blocks using molds')
    processing_machine(m, 'refinery', k['refining'], inputs=0, outputs=1, tanks_in=1, tanks_out=1, tank_cap=16000, glyph='drop',
                       desc='Refines fluids into lighter fuels')
    processing_machine(m, 'pyrolyzer', k['pyrolysis'], tanks_out=1, glyph='flame', desc='Bakes logs and coal into charcoal and coke, collecting creosote')
    custom_machine(m, 'fluid_bottler', 'BottlerBE', glyph='drop', desc='Fills and empties fluid containers, and soaks items in fluids')
    custom_machine(m, 'igneous_extruder', 'ExtruderBE', glyph='cross', desc='Makes rock from lava and water without using them up')

    machines = {
        'electric_furnace': ('ls_machina:heating_coil', 'minecraft:bricks', '#forge:plates/copper', '#forge:gears/copper'),
        'pulverizer': ('minecraft:piston', 'minecraft:flint', '#forge:plates/iron', '#forge:gears/bronze'),
        'induction_smelter': ('ls_machina:heating_coil', '#minecraft:sand', '#forge:plates/invar', '#forge:gears/invar'),
        'sawmill': ('minecraft:stonecutter', '#minecraft:planks', '#forge:plates/iron', '#forge:gears/iron'),
        'insolator': ('minecraft:glowstone', 'minecraft:dirt', '#forge:plates/tin', '#forge:gears/tin'),
        'centrifuge': ('minecraft:hopper', '#forge:ingots/iron', '#forge:plates/aluminum', '#forge:gears/aluminum'),
        'magma_crucible': ('ls_machina:heating_coil', 'minecraft:nether_bricks', '#forge:plates/invar', 'minecraft:magma_block'),
        'blast_chiller': ('minecraft:packed_ice', 'minecraft:snow_block', '#forge:plates/aluminum', '#forge:gears/constantan'),
        'compactor': ('minecraft:piston', '#forge:plates/steel', '#forge:plates/bronze', '#forge:gears/steel'),
        'refinery': ('minecraft:cauldron', '#forge:glass', '#forge:plates/steel', '#forge:gears/invar'),
        'pyrolyzer': ('ls_machina:heating_coil', 'ls_metalwork:fire_bricks', '#forge:plates/iron', '#forge:gears/iron'),
        'fluid_bottler': ('minecraft:bucket', '#forge:glass', '#forge:plates/copper', '#forge:gears/copper'),
        'igneous_extruder': ('minecraft:piston', '#forge:glass', '#forge:plates/invar', 'minecraft:obsidian'),
    }
    for name, (top, side, plate, gear) in machines.items():
        m.shaped(name, rl(name), ['PTP', 'SFS', 'GRG'], {'P': plate, 'T': top, 'S': side, 'F': rl('machine_frame'), 'G': gear,
                                                        'R': rl('redstone_coil')})

    # ----------------------------------------------------------------- recipes
    pulv = lambda n, ins, outs, t=200, e=20: m.mrecipe('ls_machina:pulverizing', n, ins, outs, time=t, energy=e)
    for metal in list(VANILLA_METALS) + list(METALS):
        dust = dust_item(metal)
        sec, chance = SECONDARY[metal]
        pulv(metal + '_ore', [('#forge:ores/' + metal, 1)], [(dust, 2), (dust_item(sec), 1, chance)], 240, 20)
        pulv(metal + '_raw', [('#forge:raw_materials/' + metal, 1)], [(dust, 1, 1.25)], 200, 20)
    for metal in PART_METALS:
        pulv(metal + '_ingot', [('#forge:ingots/' + metal, 1)], [(dust_item(metal), 1)], 120, 20)
    vanilla_pulv = [
        ('cobblestone', 'minecraft:cobblestone', [('minecraft:gravel', 1)]), ('gravel', 'minecraft:gravel', [('minecraft:sand', 1), ('minecraft:flint', 1, 0.1)]),
        ('stone', 'minecraft:stone', [('minecraft:cobblestone', 1)]), ('sandstone', 'minecraft:sandstone', [('minecraft:sand', 2)]),
        ('glowstone', 'minecraft:glowstone', [('minecraft:glowstone_dust', 4)]), ('bone', 'minecraft:bone', [('minecraft:bone_meal', 6)]),
        ('blaze_rod', 'minecraft:blaze_rod', [('minecraft:blaze_powder', 4), ('minecraft:blaze_powder', 1, 0.5)]),
        ('wool', '#minecraft:wool', [('minecraft:string', 4)]), ('sugar_cane', 'minecraft:sugar_cane', [('minecraft:sugar', 2)]),
        ('coal', 'minecraft:coal', [('ls_veins:coal_dust', 1), ('ls_veins:sulfur_dust', 1, 0.1)]), ('charcoal', 'minecraft:charcoal', [('ls_veins:coal_dust', 1)]),
        ('diamond', 'minecraft:diamond', [('ls_veins:diamond_dust', 1)]), ('emerald', 'minecraft:emerald', [('ls_veins:emerald_dust', 1)]),
        ('lapis', 'minecraft:lapis_lazuli', [('ls_veins:lapis_dust', 1)]), ('quartz', 'minecraft:quartz', [('ls_veins:quartz_dust', 1)]),
        ('obsidian', 'minecraft:obsidian', [('ls_veins:obsidian_dust', 4)]), ('ender_pearl', 'minecraft:ender_pearl', [('ls_veins:ender_pearl_dust', 1)]),
        ('redstone_ore', '#forge:ores/redstone', [('minecraft:redstone', 8)]), ('lapis_ore', '#forge:ores/lapis', [('minecraft:lapis_lazuli', 10)]),
        ('diamond_ore', '#forge:ores/diamond', [('minecraft:diamond', 2)]), ('emerald_ore', '#forge:ores/emerald', [('minecraft:emerald', 2)]),
        ('coal_ore', '#forge:ores/coal', [('minecraft:coal', 3), ('ls_veins:sulfur_dust', 1, 0.15)]),
        ('quartz_ore', '#forge:ores/quartz', [('minecraft:quartz', 3)]), ('ancient_debris', 'minecraft:ancient_debris', [('ls_veins:netherite_dust', 2)]),
        ('netherrack', 'minecraft:netherrack', [('minecraft:gravel', 1), ('ls_veins:sulfur_dust', 1, 0.1)]),
        ('clay', 'minecraft:clay', [('minecraft:clay_ball', 4)]), ('bricks', 'minecraft:bricks', [('minecraft:brick', 4)]),
        ('amethyst', 'minecraft:amethyst_block', [('minecraft:amethyst_shard', 4)]),
    ]
    for n, ing, outs in vanilla_pulv:
        pulv(n, [(ing, 1)], outs, 160, 20)

    ind = lambda n, ins, outs, t=200, e=30: m.mrecipe('ls_machina:induction_smelting', n, ins, outs, time=t, energy=e)
    for name, (color, recipe, out) in ALLOYS.items():
        if not recipe:
            continue
        (a, na), (b, nb) = list(recipe.items())
        ind(name + '_from_ingots', [('#forge:ingots/' + a, na), ('#forge:ingots/' + b, nb)], [(ingot_item(name), out)])
        ind(name + '_from_dusts', [('#forge:dusts/' + a, na), ('#forge:dusts/' + b, nb)], [(ingot_item(name), out)], 160)
    ind('steel_from_iron', [('#forge:ingots/iron', 1), ('#forge:dusts/coal', 2)], [(ingot_item('steel'), 1)], 300, 40)
    ind('steel_from_iron_coke', [('#forge:ingots/iron', 1), ('#forge:coal_coke', 1)], [(ingot_item('steel'), 1)], 300, 40)
    for metal in list(VANILLA_METALS) + list(METALS):
        ind(metal + '_ore_sand', [('#forge:ores/' + metal, 1), ('#minecraft:sand', 1)], [(ingot_item(metal), 2), (rl('slag'), 1, 0.2)], 300, 40)
        ind(metal + '_raw_sand', [('#forge:raw_materials/' + metal, 1), ('#minecraft:sand', 1)], [(ingot_item(metal), 1, 1.5), (rl('slag'), 1, 0.1)], 240, 40)

    saw = lambda n, ins, outs: m.mrecipe('ls_machina:sawing', n, ins, outs, time=120, energy=20)
    for w in WOODS:
        saw(w + '_logs', [('#minecraft:%s_logs' % w, 1)], [('minecraft:%s_planks' % w, 6), (rl('sawdust'), 1, 0.5)])
    saw('crimson_stems', [('#minecraft:crimson_stems', 1)], [('minecraft:crimson_planks', 6), (rl('sawdust'), 1, 0.5)])
    saw('warped_stems', [('#minecraft:warped_stems', 1)], [('minecraft:warped_planks', 6), (rl('sawdust'), 1, 0.5)])
    saw('bamboo_block', [('minecraft:bamboo_block', 1)], [('minecraft:bamboo_planks', 3), (rl('sawdust'), 1, 0.25)])
    saw('planks', [('#minecraft:planks', 1)], [('minecraft:stick', 4), (rl('sawdust'), 1, 0.25)])

    ins = lambda n, item, outs: m.mrecipe('ls_machina:insolating', n, [(item, 1)], outs, time=400, energy=20, fluid_in=('minecraft:water', 500))
    for seed, crop, extra in [('wheat_seeds', ('minecraft:wheat', 2), ('minecraft:wheat_seeds', 1, 1.1)),
                              ('beetroot_seeds', ('minecraft:beetroot', 2), ('minecraft:beetroot_seeds', 1, 1.1)),
                              ('carrot', ('minecraft:carrot', 3), None), ('potato', ('minecraft:potato', 3), ('minecraft:poisonous_potato', 1, 0.05)),
                              ('melon_seeds', ('minecraft:melon_slice', 6), ('minecraft:melon_seeds', 1, 1.0)),
                              ('pumpkin_seeds', ('minecraft:pumpkin', 1), ('minecraft:pumpkin_seeds', 1, 1.0)),
                              ('sugar_cane', ('minecraft:sugar_cane', 3), None), ('cactus', ('minecraft:cactus', 2), None),
                              ('bamboo', ('minecraft:bamboo', 4), None), ('sweet_berries', ('minecraft:sweet_berries', 3), None),
                              ('kelp', ('minecraft:kelp', 3), None), ('cocoa_beans', ('minecraft:cocoa_beans', 3), None),
                              ('nether_wart', ('minecraft:nether_wart', 3), None), ('brown_mushroom', ('minecraft:brown_mushroom', 2), None),
                              ('red_mushroom', ('minecraft:red_mushroom', 2), None), ('vine', ('minecraft:vine', 2), None),
                              ('glow_berries', ('minecraft:glow_berries', 3), None)]:
        outs = [crop] + ([extra] if extra else [])
        ins(seed, 'minecraft:' + seed, outs)
    for w in WOODS:
        sap = 'minecraft:mangrove_propagule' if w == 'mangrove' else 'minecraft:%s_sapling' % w
        outs = [('minecraft:%s_log' % w, 4), (sap, 1, 1.1)]
        if w in ('oak', 'dark_oak'):
            outs.append(('minecraft:apple', 1, 0.1))
        ins(w + '_sapling', sap, outs)

    cen = lambda n, ins_, outs, fo=None: m.mrecipe('ls_machina:centrifuging', n, ins_, outs, time=200, energy=30, fluid_out=fo)
    for name, (color, recipe, out) in ALLOYS.items():
        if not recipe:
            continue
        cen(name + '_dust', [('#forge:dusts/' + name, out)], [(dust_item(a), n) for a, n in recipe.items()])
    cen('gravel', [('minecraft:gravel', 1)], [('minecraft:sand', 1), ('minecraft:flint', 1, 0.3)])
    cen('magma_cream', [('minecraft:magma_cream', 1)], [('minecraft:slime_ball', 1), ('minecraft:blaze_powder', 1)])
    cen('slag', [(rl('slag'), 1)], [('minecraft:gravel', 1), ('ls_veins:iron_dust', 1, 0.2), ('ls_veins:copper_dust', 1, 0.2), ('ls_veins:tin_dust', 1, 0.1)])
    cen('soul_sand', [('minecraft:soul_sand', 1)], [('minecraft:sand', 1), ('ls_veins:sulfur_dust', 1, 0.2)])
    cen('melon', [('minecraft:melon', 1)], [('minecraft:melon_seeds', 2)], ('minecraft:water', 250))

    melt = lambda n, ing, fl, amt, t=200, e=40: m.mrecipe('ls_machina:melting', n, [(ing, 1)], [], time=t, energy=e, fluid_out=(fl, amt))
    melt('cobblestone', 'minecraft:cobblestone', 'minecraft:lava', 250, 400, 60)
    melt('stone', 'minecraft:stone', 'minecraft:lava', 250, 400, 60)
    melt('netherrack', 'minecraft:netherrack', 'minecraft:lava', 500, 300, 50)
    melt('magma_block', 'minecraft:magma_block', 'minecraft:lava', 1000, 200, 40)
    melt('obsidian', 'minecraft:obsidian', 'minecraft:lava', 1000, 600, 60)
    melt('ice', 'minecraft:ice', 'minecraft:water', 1000, 100, 10)
    melt('packed_ice', 'minecraft:packed_ice', 'minecraft:water', 2000, 150, 10)
    melt('snow_block', 'minecraft:snow_block', 'minecraft:water', 500, 80, 10)
    for metal in PART_METALS:
        fl = 'ls_toolsmith:molten_' + metal
        melt(metal + '_ingot', '#forge:ingots/' + metal, fl, 144, 120, 30)
        melt(metal + '_dust', '#forge:dusts/' + metal, fl, 144, 100, 30)
        melt(metal + '_nugget', '#forge:nuggets/' + metal, fl, 16, 20, 30)
        melt(metal + '_block', '#forge:storage_blocks/' + metal, fl, 1296, 900, 30)
        melt(metal + '_plate', '#forge:plates/' + metal, fl, 144, 120, 30)
        if metal in METALS or metal in VANILLA_METALS:
            melt(metal + '_ore', '#forge:ores/' + metal, fl, 288, 240, 40)
            melt(metal + '_raw', '#forge:raw_materials/' + metal, fl, 216, 200, 40)

    chill = lambda n, mold, fl, amt, out, t=200, e=30: m.mrecipe('ls_machina:chilling', n, [(rl(mold + '_mold'), 0)], [out], time=t, energy=e, fluid_in=(fl, amt))
    chill('ice', 'block', 'minecraft:water', 1000, ('minecraft:ice', 1), 120)
    chill('snowball', 'ball', 'minecraft:water', 250, ('minecraft:snowball', 1), 60)
    chill('obsidian', 'block', 'minecraft:lava', 1000, ('minecraft:obsidian', 1), 300, 40)
    chill('cobblestone', 'ingot', 'minecraft:lava', 125, ('minecraft:cobblestone', 1), 80)
    for metal in PART_METALS:
        fl = 'ls_toolsmith:molten_' + metal
        chill(metal + '_ingot', 'ingot', fl, 144, (ingot_item(metal), 1), 80)
        chill(metal + '_nugget', 'nugget', fl, 16, (nugget_item(metal), 1), 20)
        chill(metal + '_block', 'block', fl, 1296, (block_item(metal), 1), 400)
        chill(metal + '_plate', 'plate', fl, 144, (rl_part(metal, 'plate'), 1), 80)
        chill(metal + '_gear', 'gear', fl, 576, (rl_part(metal, 'gear'), 1), 160)

    press = lambda n, ing, cnt, mold, out, t=120, e=30: m.mrecipe('ls_machina:pressing', n, [(ing, cnt), (rl(mold + '_mold'), 0)], [out], time=t, energy=e)
    for metal in PART_METALS:
        ing = '#forge:ingots/' + metal
        press(metal + '_plate', ing, 1, 'plate', (rl_part(metal, 'plate'), 1))
        press(metal + '_gear', ing, 4, 'gear', (rl_part(metal, 'gear'), 1), 200)
        press(metal + '_rod', ing, 1, 'rod', (rl_part(metal, 'rod'), 2))
        if metal in WIRE_METALS:
            press(metal + '_wire', ing, 1, 'wire', (rl_part(metal, 'wire'), 3))
        press(metal + '_block', ing, 9, 'packing_3x3', (block_item(metal), 1), 200)
        press(metal + '_ingot_from_nuggets', '#forge:nuggets/' + metal, 9, 'packing_3x3', (ingot_item(metal), 1), 80)
        press(metal + '_unpack', '#forge:storage_blocks/' + metal, 1, 'unpacking', (ingot_item(metal), 9), 80)
    for n, ing, cnt, mold, out in [('clay', 'minecraft:clay_ball', 4, 'packing_2x2', 'minecraft:clay'), ('snow', 'minecraft:snowball', 4, 'packing_2x2', 'minecraft:snow_block'),
                                   ('quartz_block', 'minecraft:quartz', 4, 'packing_2x2', 'minecraft:quartz_block'),
                                   ('glowstone', 'minecraft:glowstone_dust', 4, 'packing_2x2', 'minecraft:glowstone'),
                                   ('amethyst_block', 'minecraft:amethyst_shard', 4, 'packing_2x2', 'minecraft:amethyst_block'),
                                   ('redstone_block', 'minecraft:redstone', 9, 'packing_3x3', 'minecraft:redstone_block'),
                                   ('lapis_block', 'minecraft:lapis_lazuli', 9, 'packing_3x3', 'minecraft:lapis_block'),
                                   ('coal_block', 'minecraft:coal', 9, 'packing_3x3', 'minecraft:coal_block'),
                                   ('diamond_block', 'minecraft:diamond', 9, 'packing_3x3', 'minecraft:diamond_block'),
                                   ('emerald_block', 'minecraft:emerald', 9, 'packing_3x3', 'minecraft:emerald_block'),
                                   ('coke_block', rl('coal_coke'), 9, 'packing_3x3', rl('coal_coke_block')),
                                   ('bone_block', 'minecraft:bone_meal', 9, 'packing_3x3', 'minecraft:bone_block')]:
        press(n, ing, cnt, mold, (out, 1), 80)

    m.mrecipe('ls_machina:refining', 'creosote', [], [(rl('tar'), 1, 0.5)], time=100, energy=40,
              fluid_in=('ls_machina:creosote', 200), fluid_out=('ls_machina:light_oil', 100))

    imb = lambda n, item, out, amt=250, fl='minecraft:water': m.mrecipe('ls_machina:imbuing', n, [(item, 1)], [(out, 1)], time=60, energy=20, fluid_in=(fl, amt))
    imb('mud', 'minecraft:dirt', 'minecraft:mud')
    imb('wet_sponge', 'minecraft:sponge', 'minecraft:wet_sponge', 1000)
    imb('mossy_cobblestone', 'minecraft:cobblestone', 'minecraft:mossy_cobblestone')
    imb('mossy_stone_bricks', 'minecraft:stone_bricks', 'minecraft:mossy_stone_bricks')
    for c in COLORS:
        imb(c + '_concrete', 'minecraft:%s_concrete_powder' % c, 'minecraft:%s_concrete' % c)

    pyro = lambda n, item, out, fl_amt, t=400: m.mrecipe('ls_machina:pyrolysis', n, [(item, 1)], [out], time=t, energy=20, fluid_out=('ls_machina:creosote', fl_amt))
    pyro('logs', '#minecraft:logs_that_burn', ('minecraft:charcoal', 1), 250)
    pyro('coal', 'minecraft:coal', (rl('coal_coke'), 1), 500)
    pyro('coal_block', 'minecraft:coal_block', (rl('coal_coke_block'), 1), 4500, 2400)

    for kname in k:
        m.tr('lens.category.ls_machina.' + kname, {'induction_smelting': 'Induction Smelter', 'pyrolysis': 'Pyrolyzer',
                                                   'melting': 'Magma Crucible', 'chilling': 'Blast Chiller', 'pressing': 'Compactor',
                                                   'imbuing': 'Fluid Bottler', 'insolating': 'Insolator', 'refining': 'Refinery',
                                                   'pulverizing': 'Pulverizer', 'sawing': 'Sawmill', 'centrifuging': 'Centrifuge'}[kname])
    for key, v in {'gui.ls_machina.bottler.fill': 'Mode: Fill', 'gui.ls_machina.bottler.empty': 'Mode: Empty'}.items():
        m.tr(key, v)

    m.codex_intro('Machina machines run on energy (FE) from Dynamos. Each has slots for up to three augments, a side configuration '
                  'tab and a redstone control tab on the right of its screen.')
    m.codex_page('Side configuration', 'Open the blue tab to set each face of a machine: blue faces accept items and fluids, orange '
                                       'faces give out products, green faces do both and gray faces are closed. Left-click cycles forward, '
                                       'right-click backward. Turn on Out to push products into neighbouring inventories and In to pull '
                                       'inputs from them.', items=[rl('machine_frame'), rl('redstone_coil')])
    m.codex_page('Augments', 'Augments change how a machine works. Speed makes it faster but hungrier, efficiency cuts the energy per '
                             'job, capacity enlarges its energy buffer, output improves the chance of bonus products, and fluid enlarges '
                             'its tanks. Each kind comes in three tiers; only one of each kind fits in a machine.',
                 items=[rl('speed_augment_1'), rl('efficiency_augment_1'), rl('capacity_augment_1'), rl('output_augment_1'), rl('fluid_augment_1')])
    m.codex_page('Ore processing', 'Pulverize an ore into two dusts with a chance of a second metal, then smelt the dusts. The induction '
                                   'smelter turns an ore and a block of sand straight into two ingots. With the magma crucible and blast '
                                   'chiller you can also melt ores into molten metal and cast it with molds.',
                 items=[rl('pulverizer'), rl('induction_smelter'), rl('magma_crucible'), rl('blast_chiller')])
    m.codex_page('Molds', 'The compactor and blast chiller shape their products with molds, which are never used up. Combine a blank '
                          'mold with an example of the shape: a plate, a gear, a rod, a wire, an ingot, a nugget or a block. Packing molds '
                          'press 2x2 or 3x3 groups into blocks and the unpacking mold breaks blocks back down.',
                 items=[rl('blank_mold'), rl('plate_mold'), rl('gear_mold'), rl('packing_3x3_mold')])
    return m


def rl_part(metal, part):
    return 'ls_metalwork:%s_%s' % (metal, part)
