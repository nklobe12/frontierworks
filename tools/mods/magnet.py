from gen.fw import Mod


def build():
    m = Mod('fw_magnet', 'Magnets', 'Item magnets: switch one on and every item and experience orb nearby flies to you. '
                                    'Sneak to pause it.', tab='TOOLS')
    rl = m.rl
    m.item('item_magnet', ('magnet', '#c0392b'), java='new MagnetItem(new Item.Properties(), 6)', jtype='MagnetItem')
    m.item('advanced_magnet', ('magnet', '#2980b9'), java='new MagnetItem(new Item.Properties(), 12)', jtype='MagnetItem')
    m.shaped('item_magnet', rl('item_magnet'), ['R L', 'I I', 'III'], {'R': '#forge:dusts/redstone', 'L': '#forge:gems/lapis', 'I': '#forge:ingots/iron'})
    m.shaped('advanced_magnet', rl('advanced_magnet'), ['E E', 'MCM', 'PPP'], {'E': '#forge:ender_pearls', 'M': rl('item_magnet'), 'C': 'fw_circuits:advanced_circuit',
                                                                              'P': '#forge:plates/electrum'})
    m.tr('tooltip.fw_magnet.on', 'Active - pulling items within %s blocks (right-click to toggle)')
    m.tr('tooltip.fw_magnet.off', 'Inactive - range %s blocks (right-click to toggle)')
    return m
