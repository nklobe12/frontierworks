"""Small quality-of-life mods (one spec file, several mods)."""
from gen.fw import Mod, custom_machine, const
from gen import tex


def veinminer():
    m = Mod('fw_veinminer', 'Vein Miner', 'Sneak while mining an ore with the right tool to mine the whole connected vein '
                                          '(up to 64 blocks). Costs durability and a little hunger per block.', tab='TOOLS')
    m.init('VeinEvents.init()')
    return m


def treefeller():
    m = Mod('fw_treefeller', 'Tree Feller', 'Chop the base of a natural tree with any axe and the whole trunk comes down. '
                                            'Sneak to chop just one log.', tab='TOOLS')
    m.init('FellEvents.init()')
    return m


def harvest():
    m = Mod('fw_harvest', 'Right-Click Harvest', 'Right-click fully grown crops, nether wart and cocoa to harvest and replant them '
                                                 'in one go.', tab='TOOLS')
    m.init('HarvestEvents.init()')
    return m


def leafdecay():
    m = Mod('fw_leafdecay', 'Fast Leaf Decay', 'Leaves left floating after you chop a tree fall within a few seconds.', tab='WORLD')
    m.init('LeafEvents.init()')
    return m


def doors():
    m = Mod('fw_doors', 'Double Doors', 'Open one half of a double door and the other half opens with it.', tab='DECOR')
    m.init('DoorEvents.init()')
    return m


def hud():
    m = Mod('fw_hud', 'Explorer HUD', 'A compact HUD with coordinates, facing, biome, day and time, and block light level. '
                                      'Press F7 to move or hide it.', tab='TOOLS')
    m.client('com.frontierworks.hud.client.HudOverlay.init(bus)')
    m.tr('key.fw_hud.toggle', 'Explorer HUD position')
    return m


def sorting():
    m = Mod('fw_sorting', 'Inventory Sorting', 'Press R in any chest, crate or your inventory to merge and sort its contents.', tab='TOOLS')
    m.init('Sorter.init()')
    m.client('com.frontierworks.sorting.client.SortingClient.init(bus)')
    m.tr('key.fw_sorting.sort', 'Sort inventory')
    return m


def graves():
    m = Mod('fw_graves', 'Graves', 'When you die, a grave keeps all of your items safe at the spot. Only you can open it.', tab='ADVENTURE')
    rl = m.rl
    m.init('GraveEvents.init()')
    t = lambda n: '%s:block/%s' % (m.modid, n)
    model = {'parent': 'minecraft:block/block', 'textures': {'particle': t('grave'), 'stone': t('grave'), 'dirt': 'minecraft:block/coarse_dirt'},
             'elements': [{'from': [1, 0, 1], 'to': [15, 2, 15], 'faces': {f: {'texture': '#dirt'} for f in ('north', 'south', 'east', 'west', 'up', 'down')}},
                          {'from': [3, 2, 6], 'to': [13, 14, 10], 'faces': {f: {'texture': '#stone'} for f in ('north', 'south', 'east', 'west', 'up', 'down')}}]}
    m.block('grave', java='new GraveBlock(FWProps.stone(2.0f, 3600000f).noOcclusion().noLootTable(), () -> Content.GRAVE_BE.get())', jtype='GraveBlock',
            tab='ADVENTURE', models={'grave': model}, textures={'block/grave': tex.chiseled('#7d7d85', 'grave')}, loot=None, level=None)
    m.field('public static final RegistryObject<BlockEntityType<GraveBE>> GRAVE_BE = REG.blockEntity("grave", (pos, state) -> new GraveBE(Content.GRAVE_BE.get(), pos, state), Content.GRAVE);')
    m.tr('msg.fw_graves.placed', 'Your items are safe in a grave at %s, %s, %s')
    m.tr('msg.fw_graves.not_yours', 'This is the grave of %s')
    return m


def xpbank():
    m = Mod('fw_xpbank', 'Experience Bank', 'Bank your experience so you never lose it to death or lava: deposit and withdraw '
                                            'levels at the XP Bank.', tab='ADVENTURE')
    custom_machine(m, 'xp_bank', 'XpBankBE', 'elite', 'plus', tab='ADVENTURE', desc='Stores experience. Never loses it.')
    m.shaped('xp_bank', m.rl('xp_bank'), ['GEG', 'EBE', 'GEG'], {'G': '#forge:ingots/gold', 'E': 'minecraft:experience_bottle', 'B': 'minecraft:enchanting_table'})
    for k, v in {'gui.fw_xpbank.deposit1': 'Deposit 1', 'gui.fw_xpbank.deposit_all': 'Deposit all', 'gui.fw_xpbank.withdraw1': 'Withdraw 1',
                 'gui.fw_xpbank.withdraw_all': 'Withdraw all', 'gui.fw_xpbank.stored': 'Stored: %s XP (%s levels)'}.items():
        m.tr(k, v)
    return m


def build():
    veinminer()
    treefeller()
    harvest()
    leafdecay()
    doors()
    hud()
    sorting()
    graves()
    xpbank()
