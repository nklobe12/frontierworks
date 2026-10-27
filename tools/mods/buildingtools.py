from gen.fw import Mod


def build():
    m = Mod('fw_buildingtools', "Builder's Wands", "Builder's wands that extend a whole surface of matching blocks by one layer in a "
                                                   'single click, pulling the blocks from your inventory.', tab='TOOLS')
    rl = m.rl
    wands = [('stone_wand', '#8a8a8a', 9, 256, '#forge:cobblestone'), ('iron_wand', '#d8d8d8', 25, 1024, '#forge:ingots/iron'),
             ('diamond_wand', '#7ee6e0', 81, 4096, '#forge:gems/diamond'), ('voidite_wand', '#8a3cff', 225, 0, 'fw_crystals:voidite_shard')]
    for name, color, limit, dur, mat in wands:
        props = 'new Item.Properties().durability(%d)' % dur if dur else 'new Item.Properties().stacksTo(1)'
        m.item(name, ('wand', color, '#5a3a22', '#ffffff'), java='new BuildersWandItem(%s, %d)' % (props, limit), jtype='BuildersWandItem', model='handheld',
               lang={'stone_wand': "Stone Builder's Wand", 'iron_wand': "Iron Builder's Wand", 'diamond_wand': "Diamond Builder's Wand",
                     'voidite_wand': "Voidite Builder's Wand"}[name])
        m.shaped(name, rl(name), ['  M', ' S ', 'S  '], {'M': mat, 'S': '#forge:rods/wooden'})
    m.tr('tooltip.fw_buildingtools.wand', 'Extends up to %s blocks of a surface')
    return m
