"""Shared material definitions used across specs."""

# name: (colour, owner mod for ingot/dust/nugget/block, is vanilla)
VANILLA_METALS = {
    'iron': '#d8d8d8',
    'gold': '#f2c94c',
    'copper': '#d9844f',
}

METALS = {
    # name: colour, ore params (size, count, min_y, max_y, shape), mining level, hardness
    'tin': ('#c9d1d9', (9, 10, -16, 96, 'trapezoid'), 'stone', 3.0),
    'lead': ('#6a7690', (8, 6, -48, 48, 'trapezoid'), 'stone', 3.0),
    'silver': ('#e4ebf2', (7, 4, -64, 32, 'trapezoid'), 'iron', 3.0),
    'nickel': ('#d6cd98', (7, 5, -40, 40, 'trapezoid'), 'iron', 3.0),
    'zinc': ('#a9bcb2', (9, 7, 0, 80, 'trapezoid'), 'stone', 3.0),
    'aluminum': ('#e6eaee', (9, 6, 16, 112, 'trapezoid'), 'stone', 3.0),
    'platinum': ('#9fd0de', (4, 2, -64, -16, 'uniform'), 'diamond', 4.0),
    'uranium': ('#6ec54b', (4, 3, -64, 0, 'uniform'), 'diamond', 4.0),
}

ALLOYS = {
    'bronze': '#c8873a',
    'brass': '#e0b84a',
    'steel': '#7b8591',
    'electrum': '#ecd97a',
    'invar': '#b7bfaa',
    'constantan': '#d79a6b',
    'duralumin': '#c6cfd8',
}

# metals that get plates/gears/rods (fw_parts)
PART_METALS = ['iron', 'gold', 'copper', 'tin', 'lead', 'silver', 'nickel', 'zinc', 'aluminum', 'platinum',
               'bronze', 'brass', 'steel', 'electrum', 'invar', 'constantan', 'duralumin']

WIRE_METALS = ['copper', 'gold', 'electrum', 'aluminum', 'constantan']

GEMS = {
    # name: colour, size, count, min, max, level
    'ruby': ('#e0245e', 5, 3, -48, 32, 'iron'),
    'sapphire': ('#2f6fe4', 5, 3, -48, 32, 'iron'),
    'peridot': ('#8fd14f', 5, 3, -32, 48, 'iron'),
    'topaz': ('#f5a623', 5, 3, -32, 48, 'iron'),
    'onyx': ('#2e2a33', 4, 2, -64, -8, 'diamond'),
}

MINERALS = {
    # name: colour, size, count, min, max, drop count range
    'sulfur': ('#e8d84a', 8, 6, -32, 48, (2, 4)),
    'saltpeter': ('#ece6dc', 8, 5, 0, 64, (2, 4)),
    'graphite': ('#3b3d42', 8, 5, -48, 32, (1, 3)),
    'fluorite': ('#7fe0d0', 6, 4, -32, 32, (2, 3)),
    'apatite': ('#57b8e8', 8, 5, 0, 80, (2, 5)),
}

ALL_METALS = dict(VANILLA_METALS)
ALL_METALS.update({k: v[0] for k, v in METALS.items()})
ALL_METALS.update(ALLOYS)


def metal_color(name):
    return ALL_METALS[name]


def owner(name):
    """Mod that owns ingot/dust/nugget/block for a metal."""
    if name in VANILLA_METALS:
        return 'minecraft'
    if name in METALS:
        return 'fw_metals'
    if name in ALLOYS:
        return 'fw_alloys'
    raise KeyError(name)


def ingot(name):
    return '#forge:ingots/' + name


def dust_item(name):
    return ('fw_metals:%s_dust' % name) if (name in VANILLA_METALS or name in METALS) else 'fw_alloys:%s_dust' % name
