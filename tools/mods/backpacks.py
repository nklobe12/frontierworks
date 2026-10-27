from gen.fw import Mod


def build():
    m = Mod('fw_backpacks', 'Backpacks', 'Leather, iron, steel and explorer backpacks with up to 54 extra slots. Right-click to open; '
                                         'contents travel with the backpack. Backpacks and shulker boxes cannot be nested.', tab='TOOLS')
    rl = m.rl
    m.client('com.frontierworks.backpacks.client.BackpackScreen.init(bus)')
    m.field('public static final RegistryObject<net.minecraft.world.inventory.MenuType<BackpackMenu>> BACKPACK_MENU = REG.menu("backpack", BackpackMenu::new);')
    packs = [('leather_backpack', '#8a5a33', 2), ('iron_backpack', '#9aa0a8', 3), ('steel_backpack', '#5f6670', 4), ('explorer_backpack', '#2f7d6e', 6)]
    for name, color, rows in packs:
        m.item(name, ('backpack', color, '#5a3a22'), java='new BackpackItem(new Item.Properties(), %d)' % rows, jtype='BackpackItem')
    m.shaped('leather_backpack', rl('leather_backpack'), ['SLS', 'LCL', 'LLL'], {'S': 'minecraft:string', 'L': 'minecraft:leather', 'C': 'minecraft:chest'})
    m.shaped('iron_backpack', rl('iron_backpack'), ['III', 'IBI', 'III'], {'I': '#forge:ingots/iron', 'B': rl('leather_backpack')})
    m.shaped('steel_backpack', rl('steel_backpack'), ['SSS', 'SBS', 'SSS'], {'S': '#forge:ingots/steel', 'B': rl('iron_backpack')})
    m.shaped('explorer_backpack', rl('explorer_backpack'), ['CDC', 'RBR', 'CDC'], {'C': 'fw_chemistry:carbon_fiber', 'D': '#forge:gems/diamond',
                                                                                  'R': '#forge:rubber', 'B': rl('steel_backpack')})
    m.tr('tooltip.fw_backpacks.slots', '%s slots')
    m.tr('msg.fw_backpacks.main_hand', 'Hold the backpack in your main hand to open it')
    return m
