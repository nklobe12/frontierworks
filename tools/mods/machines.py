from gen.fw import Mod, processing_machine, recipe_kind
from mods._materials import METALS, GEMS, MINERALS

BYPRODUCT = {
    'iron': ('fw_metals:nickel_dust', 0.10), 'gold': ('fw_metals:silver_dust', 0.10), 'copper': ('fw_metals:gold_dust', 0.05),
    'tin': ('fw_metals:zinc_dust', 0.10), 'lead': ('fw_metals:silver_dust', 0.15), 'silver': ('fw_metals:lead_dust', 0.10),
    'nickel': ('fw_metals:platinum_dust', 0.05), 'zinc': ('fw_metals:tin_dust', 0.10), 'aluminum': ('fw_metals:iron_dust', 0.10),
    'platinum': ('fw_metals:nickel_dust', 0.10), 'uranium': ('fw_metals:lead_dust', 0.15),
}


def build():
    m = Mod('fw_machines', 'Machines', 'The first electric machines: Pulverizer (ore doubling), Electric Furnace, '
                                       'Compressor and Extractor. All accept Speed, Efficiency and Capacity upgrades.', tab='MACHINES')
    pul = recipe_kind(m, 'pulverizing')
    com = recipe_kind(m, 'compressing')
    ext = recipe_kind(m, 'extracting')
    processing_machine(m, 'pulverizer', pul, 1, 2, 'basic', 'crush', desc='Grinds ores into two dusts, with a chance of a bonus byproduct')
    processing_machine(m, 'electric_furnace', None, 1, 1, 'basic', 'flame', cooking='SMELTING', time_factor=0.6,
                       desc='Smelts anything a furnace can, faster and without fuel')
    processing_machine(m, 'compressor', com, 1, 1, 'basic', 'press', desc='Compresses ingots into plates and more')
    processing_machine(m, 'extractor', ext, 1, 1, 'basic', 'drop', desc='Extracts latex, dyes and sugars from plants')

    rl = m.rl
    m.shaped('pulverizer', rl('pulverizer'), ['FFF', 'PMP', 'CWC'], {'F': 'minecraft:flint', 'P': '#forge:plates/iron', 'M': 'fw_parts:machine_frame',
                                                                     'C': 'fw_parts:motor', 'W': 'fw_circuits:basic_circuit'})
    m.shaped('electric_furnace', rl('electric_furnace'), ['PHP', 'PMP', 'CWC'], {'H': 'fw_parts:heating_coil', 'P': '#forge:plates/iron',
                                                                                 'M': 'fw_parts:machine_frame', 'C': '#forge:ingots/copper', 'W': 'fw_circuits:basic_circuit'})
    m.shaped('compressor', rl('compressor'), ['PIP', 'PMP', 'CWC'], {'I': 'minecraft:piston', 'P': '#forge:plates/iron', 'M': 'fw_parts:machine_frame',
                                                                     'C': 'fw_parts:motor', 'W': 'fw_circuits:basic_circuit'})
    m.shaped('extractor', rl('extractor'), ['PTP', 'PMP', 'CWC'], {'T': 'minecraft:bucket', 'P': '#forge:plates/tin', 'M': 'fw_parts:machine_frame',
                                                                   'C': 'fw_parts:motor', 'W': 'fw_circuits:basic_circuit'})

    P = 'fw_machines:pulverizing'
    for name in list(METALS.keys()) + ['iron', 'gold', 'copper']:
        dust = 'fw_metals:%s_dust' % name
        bp = BYPRODUCT.get(name)
        outs = [(dust, 2)] + ([(bp[0], 1, bp[1])] if bp else [])
        m.mrecipe(P, 'ore_' + name, [('#forge:ores/' + name, 1)], outs, time=200, energy=20)
        raw = ('minecraft:raw_' + name) if name in ('iron', 'gold', 'copper') else 'fw_metals:raw_' + name
        m.mrecipe(P, 'raw_' + name, [(raw, 1)], [(dust, 1), (dust, 1, 0.5)] + ([(bp[0], 1, bp[1] / 2)] if bp else []), time=160, energy=20)
        m.mrecipe(P, 'ingot_' + name, [('#forge:ingots/' + name, 1)], [(dust, 1)], time=100, energy=16)
    for name, (color, *_rest) in GEMS.items():
        m.mrecipe(P, 'ore_' + name, [('#forge:ores/' + name, 1)], [('fw_gems:' + name, 2), ('fw_gems:' + name, 1, 0.25)], time=200, energy=20)
    for name, (color, s, c, lo, hi, cnt) in MINERALS.items():
        m.mrecipe(P, 'ore_' + name, [('#forge:ores/' + name, 1)], [('fw_minerals:' + name, cnt[1] + 1)], time=200, energy=20)
    misc = [
        ('cobblestone', '#forge:cobblestone', [('minecraft:gravel', 1)]),
        ('gravel', 'minecraft:gravel', [('minecraft:sand', 1), ('minecraft:flint', 1, 0.15)]),
        ('stone', 'minecraft:stone', [('fw_metals:stone_dust', 1)]),
        ('coal', 'minecraft:coal', [('fw_metals:coal_dust', 1)]),
        ('charcoal', 'minecraft:charcoal', [('fw_metals:coal_dust', 1)]),
        ('diamond', 'minecraft:diamond', [('fw_metals:diamond_dust', 1)]),
        ('obsidian', 'minecraft:obsidian', [('fw_metals:obsidian_dust', 4)]),
        ('ender_pearl', 'minecraft:ender_pearl', [('fw_metals:ender_dust', 2)]),
        ('blaze_rod', 'minecraft:blaze_rod', [('minecraft:blaze_powder', 4)]),
        ('bone', 'minecraft:bone', [('minecraft:bone_meal', 6)]),
        ('glowstone', 'minecraft:glowstone', [('minecraft:glowstone_dust', 4)]),
        ('sandstone', 'minecraft:sandstone', [('minecraft:sand', 2)]),
        ('wool', '#minecraft:wool', [('minecraft:string', 4)]),
        ('quartz_ore', '#forge:ores/quartz', [('minecraft:quartz', 2), ('minecraft:quartz', 1, 0.5)]),
        ('lapis_ore', '#forge:ores/lapis', [('minecraft:lapis_lazuli', 10)]),
        ('redstone_ore', '#forge:ores/redstone', [('minecraft:redstone', 6)]),
        ('diamond_ore', '#forge:ores/diamond', [('minecraft:diamond', 2)]),
        ('emerald_ore', '#forge:ores/emerald', [('minecraft:emerald', 2)]),
        ('coal_ore', '#forge:ores/coal', [('minecraft:coal', 2), ('fw_metals:coal_dust', 1, 0.25)]),
        ('netherrack', 'minecraft:netherrack', [('minecraft:gravel', 1), ('minecraft:glowstone_dust', 1, 0.05)]),
    ]
    for name, ing, outs in misc:
        m.mrecipe(P, 'misc_' + name, [(ing, 1)], outs, time=120, energy=16)

    C = 'fw_machines:compressing'
    for name, ing, n, out in [('sandstone', 'minecraft:sand', 4, 'minecraft:sandstone'), ('snow', 'minecraft:snowball', 4, 'minecraft:snow_block'),
                              ('packed_ice', 'minecraft:ice', 4, 'minecraft:packed_ice'), ('blue_ice', 'minecraft:packed_ice', 4, 'minecraft:blue_ice'),
                              ('diamond', 'fw_metals:diamond_dust', 1, 'minecraft:diamond'), ('coal', 'fw_metals:coal_dust', 2, 'minecraft:coal'),
                              ('obsidian', 'fw_metals:obsidian_dust', 4, 'minecraft:obsidian'), ('ender_pearl', 'fw_metals:ender_dust', 2, 'minecraft:ender_pearl'),
                              ('paper', 'minecraft:sugar_cane', 3, 'minecraft:paper')]:
        m.mrecipe(C, name, [(ing, n)], [(out, 1)], time=160, energy=20)

    E = 'fw_machines:extracting'
    for name, ing, out, n in [('latex', '#fw_rubber:rubber_logs', 'fw_rubber:latex', 3), ('sugar', 'minecraft:sugar_cane', 'minecraft:sugar', 2),
                              ('green_dye', 'minecraft:cactus', 'minecraft:green_dye', 2), ('slime', 'minecraft:slime_block', 'minecraft:slime_ball', 9),
                              ('string', 'minecraft:cobweb', 'minecraft:string', 3), ('ink', 'minecraft:kelp', 'minecraft:black_dye', 1),
                              ('red_dye', 'minecraft:poppy', 'minecraft:red_dye', 3), ('blaze_powder', 'minecraft:magma_cream', 'minecraft:blaze_powder', 2)]:
        m.mrecipe(E, name, [(ing, 1)], [(out, n)], time=120, energy=16)
    return m
