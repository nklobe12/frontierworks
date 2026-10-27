from gen.fw import Mod
from mods._gear import GEAR

MATS = {
    'iron': ('#d8d8d8', 'Tiers.IRON', '#forge:ingots/iron', '#forge:storage_blocks/iron'),
    'diamond': ('#7ee6e0', 'Tiers.DIAMOND', '#forge:gems/diamond', '#forge:storage_blocks/diamond'),
    'bronze': (GEAR['bronze'][0], 'FWTiers.BRONZE', '#forge:ingots/bronze', '#forge:storage_blocks/bronze'),
    'steel': (GEAR['steel'][0], 'FWTiers.STEEL', '#forge:ingots/steel', '#forge:storage_blocks/steel'),
    'platinum': (GEAR['platinum'][0], 'FWTiers.PLATINUM', '#forge:ingots/platinum', '#forge:storage_blocks/platinum'),
    'onyx': (GEAR['onyx'][0], 'FWTiers.ONYX', '#forge:gems/onyx', '#forge:storage_blocks/onyx'),
    'meteoric_steel': (GEAR['meteoric_steel'][0], 'FWTiers.METEORIC', '#forge:ingots/meteoric_steel', '#forge:storage_blocks/meteoric_steel'),
    'titanium': (GEAR['titanium'][0], 'FWTiers.TITANIUM', '#forge:ingots/titanium', '#forge:storage_blocks/titanium'),
}


def build():
    m = Mod('fw_hammers', 'Hammers', 'Hammers and excavators that dig a 3x3 area at once, in eight materials. They mine a '
                                     'little slower and last three times longer. Sneak to dig a single block.', tab='TOOLS')
    rl = m.rl
    for mat, (color, tier, ing, blk) in MATS.items():
        for kind, shovel, pattern in (('hammer', False, ['IBI', ' S ', ' S ']), ('excavator', True, [' B ', ' S ', ' S '])):
            name = '%s_%s' % (mat, kind)
            m.item(name, (kind, color, '#5a3a22'), java='new AreaToolItem(%s, %s, new Item.Properties())' % (tier, 'true' if shovel else 'false'),
                   jtype='AreaToolItem', model='handheld', tags=['forge:tools/' + ('hammers' if not shovel else 'excavators')])
            keys = {'I': ing, 'B': blk, 'S': '#forge:rods/wooden'}
            m.shaped(name, rl(name), pattern, {k: v for k, v in keys.items() if any(k in row for row in pattern)})
    m.tr('tooltip.fw_hammers.hammer', 'Mines 3x3. Sneak for a single block.')
    m.tr('tooltip.fw_hammers.excavator', 'Digs 3x3. Sneak for a single block.')
    return m
