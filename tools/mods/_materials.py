"""Shared material definitions used across specs (Lodestar)."""

VANILLA_METALS = {
    'iron': '#d8d8d8',
    'gold': '#f2c94c',
    'copper': '#d9844f',
}

# Deep Veins metals: colour, mining level, hardness, scattered-ore params (size, count, min_y, max_y, shape)
METALS = {
    'tin': ('#c9d1d9', 'stone', 3.0, (8, 8, -16, 96, 'trapezoid')),
    'lead': ('#6a7690', 'stone', 3.0, (7, 5, -48, 48, 'trapezoid')),
    'silver': ('#e4ebf2', 'iron', 3.0, (6, 3, -64, 32, 'trapezoid')),
    'nickel': ('#d6cd98', 'iron', 3.0, (6, 4, -40, 40, 'trapezoid')),
    'zinc': ('#a9bcb2', 'stone', 3.0, (8, 6, 0, 80, 'trapezoid')),
    'aluminum': ('#e6eaee', 'stone', 3.0, (8, 5, 16, 112, 'trapezoid')),
    'platinum': ('#9fd0de', 'diamond', 4.0, (4, 1, -64, -16, 'uniform')),
    'uranium': ('#6ec54b', 'diamond', 4.0, (4, 2, -64, 0, 'uniform')),
    'cobalt': ('#3c6fd6', 'diamond', 4.0, (4, 1, -64, -24, 'uniform')),
    'tungsten': ('#4a4f57', 'diamond', 5.0, (3, 1, -64, -32, 'uniform')),
}

# Big veins: name -> (primary metal or vanilla ore, secondary, secondary chance, rarity (1 in N chunks), min_y, max_y, radius, length)
VEINS = {
    'cassiterite': ('tin', 'minecraft:copper', 0.25, 10, -16, 80, 4, 18),
    'galena': ('lead', 'silver', 0.2, 12, -48, 40, 4, 16),
    'pentlandite': ('nickel', 'platinum', 0.08, 14, -48, 24, 4, 16),
    'sphalerite': ('zinc', 'lead', 0.2, 12, 0, 72, 4, 16),
    'bauxite': ('aluminum', 'minecraft:iron', 0.15, 12, 24, 100, 5, 14),
    'pitchblende': ('uranium', 'lead', 0.3, 20, -64, -8, 3, 14),
    'cobaltite': ('cobalt', 'nickel', 0.25, 22, -64, -16, 3, 14),
    'wolframite': ('tungsten', 'tin', 0.25, 24, -64, -24, 3, 12),
    'magnetite': ('minecraft:iron', 'nickel', 0.1, 10, -24, 56, 5, 18),
    'auriferous': ('minecraft:gold', 'silver', 0.3, 18, -64, 16, 3, 14),
}

ALLOYS = {
    # name: colour, recipe (inputs as metal: count), output count
    'bronze': ('#c8873a', {'copper': 3, 'tin': 1}, 4),
    'brass': ('#e0b84a', {'copper': 3, 'zinc': 1}, 4),
    'steel': ('#7b8591', None, 1),
    'electrum': ('#ecd97a', {'gold': 1, 'silver': 1}, 2),
    'invar': ('#b7bfaa', {'iron': 2, 'nickel': 1}, 3),
    'constantan': ('#d79a6b', {'copper': 1, 'nickel': 1}, 2),
    'duralumin': ('#c6cfd8', {'aluminum': 3, 'copper': 1}, 4),
    'cobalt_steel': ('#5a77b8', {'steel': 2, 'cobalt': 1}, 2),
}

PART_METALS = ['iron', 'gold', 'copper', 'tin', 'lead', 'silver', 'nickel', 'zinc', 'aluminum', 'platinum', 'cobalt', 'tungsten',
               'bronze', 'brass', 'steel', 'electrum', 'invar', 'constantan', 'duralumin', 'cobalt_steel']

ALL_METALS = dict(VANILLA_METALS)
ALL_METALS.update({k: v[0] for k, v in METALS.items()})
ALL_METALS.update({k: v[0] for k, v in ALLOYS.items()})


def metal_color(name):
    return ALL_METALS[name]


def ingot_item(name):
    if name in VANILLA_METALS:
        return 'minecraft:%s_ingot' % name
    if name in METALS:
        return 'ls_veins:%s_ingot' % name
    return 'ls_metalwork:%s_ingot' % name


def nugget_item(name):
    if name in ('iron', 'gold'):
        return 'minecraft:%s_nugget' % name
    if name == 'copper':
        return 'ls_veins:copper_nugget'
    if name in METALS:
        return 'ls_veins:%s_nugget' % name
    return 'ls_metalwork:%s_nugget' % name


def dust_item(name):
    if name in VANILLA_METALS or name in METALS:
        return 'ls_veins:%s_dust' % name
    return 'ls_metalwork:%s_dust' % name


def block_item(name):
    if name in VANILLA_METALS:
        return 'minecraft:%s_block' % name
    if name in METALS:
        return 'ls_veins:%s_block' % name
    return 'ls_metalwork:%s_block' % name


def raw_item(name):
    if name in VANILLA_METALS:
        return 'minecraft:raw_%s' % name
    return 'ls_veins:raw_%s' % name


def ore_tag(name):
    return '#forge:ores/' + name


def ingot(name):
    return '#forge:ingots/' + name


def plate(name):
    return '#forge:plates/' + name


def gear(name):
    return '#forge:gears/' + name


def rod(name):
    return '#forge:rods/' + name
