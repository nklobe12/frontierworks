from gen.ls import Mod
from plan import TABS


def build():
    m = Mod('ls_core', 'Core', 'The Lodestar engine shared by every mod in the pack: Forge Energy, machines and their screens, '
                               'multiblocks, duct networks, player data, worldgen helpers and the self-test.', tab='CORE')
    m.init('CoreHooks.init(bus)')
    m.client('com.lodestar.core.client.CoreClient.init(bus)')
    m.item('wrench', ('hook', '#9aa3ad'), java='new WrenchItem(new Item.Properties())', jtype='WrenchItem', tab='CORE',
           lang="Engineer's Wrench", desc='Right-click: rotate a block\nSneak + right-click: dismantle Lodestar machines')
    m.shaped('wrench', 'ls_core:wrench', [' I ', ' II', 'I  '], {'I': '#forge:ingots/iron'})
    for key, title in TABS:
        m.tr('itemGroup.ls_core.' + key.lower(), 'Lodestar' if key == 'CORE' else 'Lodestar: ' + title)
    for k, v in {
        'gui.ls_core.generating': 'Generating: %s FE/t', 'gui.ls_core.empty': 'Empty',
        'gui.ls_core.on': 'On', 'gui.ls_core.off': 'Off', 'gui.ls_core.start': 'Start', 'gui.ls_core.stop': 'Stop',
        'msg.ls_core.empty_bag': 'There was nothing useful inside', 'key.categories.lodestar': 'Lodestar',
        'msg.ls_core.destination_unavailable': 'That destination is unavailable',
    }.items():
        m.tr(k, v)
    return m
