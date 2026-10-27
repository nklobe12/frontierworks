from gen.fw import Mod, custom_machine


def build():
    m = Mod('fw_mobgrinder', 'Mob Grinder', 'Automated mob farming: the Mob Grinder strikes mobs above it like a player would '
                                            '(rare drops included), uses the weapon you give it, and banks the XP. Pets, villagers, '
                                            'golems, named mobs and bosses are always safe.', tab='LOGISTICS')
    custom_machine(m, 'mob_grinder', 'MobGrinderBE', 'advanced', 'cross', tab='LOGISTICS',
                   desc='Hits mobs in a 5x5x3 area above. Put a sword in the weapon slot for Looting and more damage.')
    m.shaped('mob_grinder', m.rl('mob_grinder'), ['SSS', 'PFP', 'CWC'], {'S': 'minecraft:iron_sword', 'P': '#forge:plates/steel', 'F': 'fw_parts:advanced_frame',
                                                                        'C': 'fw_parts:motor', 'W': 'fw_circuits:advanced_circuit'})
    m.tr('gui.fw_mobgrinder.take_xp', 'Take XP')
    m.tr('gui.fw_mobgrinder.xp', 'XP: %s')
    return m
