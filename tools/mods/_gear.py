"""Gear materials shared by tools, armor, hammers and weapons specs."""

# name: (colour, repair tag / ingredient, java tier name, handle colour)
GEAR = {
    'bronze': ('#c8873a', '#forge:ingots/bronze', 'BRONZE', '#6b4a2a'),
    'steel': ('#7b8591', '#forge:ingots/steel', 'STEEL', '#3b3b3b'),
    'invar': ('#b7bfaa', '#forge:ingots/invar', 'INVAR', '#6b4a2a'),
    'silver': ('#e4ebf2', '#forge:ingots/silver', 'SILVER', '#4a3a5a'),
    'electrum': ('#ecd97a', '#forge:ingots/electrum', 'ELECTRUM', '#4a3a2a'),
    'platinum': ('#9fd0de', '#forge:ingots/platinum', 'PLATINUM', '#2a3a4a'),
    'ruby': ('#e0245e', '#forge:gems/ruby', 'RUBY', '#5a3a2a'),
    'sapphire': ('#2f6fe4', '#forge:gems/sapphire', 'SAPPHIRE', '#5a3a2a'),
    'peridot': ('#8fd14f', '#forge:gems/peridot', 'PERIDOT', '#5a3a2a'),
    'topaz': ('#f5a623', '#forge:gems/topaz', 'TOPAZ', '#5a3a2a'),
    'onyx': ('#3a3442', '#forge:gems/onyx', 'ONYX', '#8a1a2a'),
    'meteoric_steel': ('#6f86b8', '#forge:ingots/meteoric_steel', 'METEORIC', '#2a2a3a'),
    'titanium': ('#c9d3de', '#forge:ingots/titanium', 'TITANIUM', '#1a1a1a'),
}

# armor: (multiplier, helmet, chest, legs, boots, enchant, toughness, knockback)
ARMOR = {
    'bronze': (14, 2, 6, 5, 2, 15, 0.0, 0.0),
    'steel': (24, 3, 7, 6, 2, 12, 1.0, 0.0),
    'silver': (12, 2, 5, 4, 2, 22, 0.0, 0.0),
    'platinum': (30, 3, 7, 6, 3, 18, 1.5, 0.0),
    'ruby': (32, 3, 7, 6, 3, 12, 1.5, 0.0),
    'sapphire': (32, 3, 7, 6, 3, 12, 1.5, 0.0),
    'peridot': (32, 3, 7, 6, 3, 12, 1.5, 0.0),
    'topaz': (32, 3, 7, 6, 3, 12, 1.5, 0.0),
    'onyx': (37, 3, 8, 6, 3, 15, 3.0, 0.1),
    'meteoric_steel': (40, 3, 8, 7, 3, 14, 3.5, 0.15),
    'titanium': (42, 4, 9, 7, 4, 16, 4.0, 0.2),
}
