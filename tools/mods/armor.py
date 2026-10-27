from gen.fw import Mod, const
from gen import tex
from mods._gear import GEAR, ARMOR

PIECES = [('helmet', 'HELMET', ['MMM', 'M M']), ('chestplate', 'CHESTPLATE', ['M M', 'MMM', 'MMM']),
          ('leggings', 'LEGGINGS', ['MMM', 'M M', 'M M']), ('boots', 'BOOTS', ['M M', 'M M'])]


def build():
    m = Mod('fw_armor', 'Armory', 'Armor sets in eleven materials. Wear a full gem or space-metal set for a bonus: strength, '
                                  'water breathing, luck, haste, fire resistance, resistance, speed and more.', tab='TOOLS')
    rl = m.rl
    m.init('SetBonuses.init()')
    for mat, (mult, h, c, l, b, ench, tough, kb) in ARMOR.items():
        color, repair, tier, handle = GEAR[mat]
        M = const(mat) + '_MAT'
        m.field('public static final FWTiers.Armor %s = FWTiers.armor("fw_armor:%s", %d, %d, %d, %d, %d, %d, %sf, %sf, "%s");'
                % (M, mat, mult, h, c, l, b, ench, tough, kb, repair.lstrip('#')))
        for piece, typ, pattern in PIECES:
            name = '%s_%s' % (mat, piece)
            m.item(name, (piece, color), java='new ArmorItem(Content.%s, ArmorItem.Type.%s, new Item.Properties())' % (M, typ), jtype='ArmorItem',
                   tags=['forge:armors/' + {'helmet': 'helmets', 'chestplate': 'chestplates', 'leggings': 'leggings', 'boots': 'boots'}[piece]])
            m.shaped(name, rl(name), pattern, {'M': repair})
        m.asset('textures/models/armor/%s_layer_1.png' % mat, tex.armor_layer(color, 1, mat))
        m.asset('textures/models/armor/%s_layer_2.png' % mat, tex.armor_layer(color, 2, mat))
    m.tr('tooltip.fw_armor.set_bonus', 'Full set: %s')
    return m
