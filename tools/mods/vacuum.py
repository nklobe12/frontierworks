from gen.fw import Mod, custom_machine


def build():
    m = Mod('fw_vacuum', 'Vacuum Hopper', 'A hopper with ambition: pulls in every item and experience orb within 4 blocks '
                                          '(more with Range Upgrades) and feeds them to the inventory below.', tab='LOGISTICS')
    custom_machine(m, 'vacuum_hopper', 'VacuumHopperBE', 'basic', 'magnet', tab='LOGISTICS', desc='Collects items and XP in a 4-block radius')
    m.shaped('vacuum_hopper', m.rl('vacuum_hopper'), [' E ', 'PHP', ' O '], {'E': '#forge:ender_pearls', 'P': '#forge:plates/iron', 'H': 'minecraft:hopper',
                                                                             'O': 'minecraft:obsidian'})
    m.tr('gui.fw_vacuum.take_xp', 'Take XP')
    m.tr('gui.fw_vacuum.xp', 'XP: %s')
    return m
