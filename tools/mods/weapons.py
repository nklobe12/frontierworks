"""Arsenal: spears, daggers, warhammers and battleaxes in twelve materials, each with its own fighting style."""
from gen.fw import Mod
from mods._gear import GEAR

M = 'fw_weapons'

MATS = {
    'iron': ('#d8d8d8', 'minecraft:iron_ingot', 'Tiers.IRON', '#6b4a2a'),
    'diamond': ('#4ee6d8', 'minecraft:diamond', 'Tiers.DIAMOND', '#6b4a2a'),
    'netherite': ('#4a4248', 'minecraft:netherite_ingot', 'Tiers.NETHERITE', '#2a2226'),
}
for k in ('bronze', 'steel', 'silver', 'platinum', 'ruby', 'sapphire', 'onyx', 'meteoric_steel', 'titanium'):
    c, rep, tier, handle = GEAR[k]
    MATS[k] = (c, rep, 'FWTiers.' + tier, handle)

# kind: (java kind enum, damage, speed, pattern)
KINDS = {
    'spear': ('SPEAR', 3, -2.7, ['  M', ' S ', 'S  ']),
    'dagger': ('DAGGER', 1, -1.6, [' M', 'S ']),
    'warhammer': ('WARHAMMER', 6, -3.4, ['MMM', 'MMM', ' S ']),
    'battleaxe': ('BATTLEAXE', 5, -3.1, ['MSM', 'MSM', ' S ']),
}
DESC = {'spear': '+1.5 reach', 'dagger': 'Double damage when striking from behind', 'warhammer': 'Fully charged hits shake nearby enemies',
        'battleaxe': 'Disables shields'}


def build():
    m = Mod(M, 'Arsenal', 'New weapon types in twelve materials: long-reach spears, backstabbing daggers, warhammers whose charged '
                          'blows hit everything nearby, and shield-breaking battleaxes.', tab='TOOLS')
    m.imp('net.minecraft.world.item.Tiers', 'com.frontierworks.weapons.WeaponItem')
    m.init('com.frontierworks.weapons.WeaponEvents.init()')
    rl = m.rl
    for mat, (color, rep, tier, handle) in MATS.items():
        for kind, (jk, dmg, spd, pattern) in KINDS.items():
            name = '%s_%s' % (mat, kind)
            props = 'new Item.Properties()' + ('.fireResistant()' if mat == 'netherite' else '')
            m.item(name, (kind, color, handle), java='new WeaponItem(%s, WeaponItem.Kind.%s, %d, %sf, %s)' % (tier, jk, dmg, spd, props),
                   jtype='WeaponItem', model='handheld', desc=DESC[kind], tags=['fw_weapons:' + kind + 's'])
            if mat == 'netherite':
                m.recipe(name, {'type': 'minecraft:smithing_transform', 'template': {'item': 'minecraft:netherite_upgrade_smithing_template'},
                                'base': {'item': rl('diamond_' + kind)}, 'addition': {'item': 'minecraft:netherite_ingot'}, 'result': {'item': rl(name)}})
            else:
                m.shaped(name, rl(name), pattern, {'M': rep, 'S': '#forge:rods/wooden'})
    return m
