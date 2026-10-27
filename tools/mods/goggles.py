from gen.fw import Mod
from gen import tex


def build():
    m = Mod('fw_goggles', 'Goggles', 'Powered goggles: night vision goggles light up caves, and tactical goggles also reveal every '
                                     'creature within 24 blocks through walls.', tab='TOOLS')
    rl = m.rl
    for name, color, cap, detect in (('night_vision_goggles', '#2f7d32', 200_000, False), ('tactical_goggles', '#8e2f2f', 1_000_000, True)):
        mat = name.upper() + '_MAT'
        m.field('public static final FWTiers.Armor %s = FWTiers.armor("fw_goggles:%s", 15, 1, 0, 0, 0, 10, 0.0f, 0.0f, null);' % (mat, name))
        m.item(name, ('helmet', color, None, '#7ee67e'), java='new GogglesItem(Content.%s, new Item.Properties(), %d, %s)' % (mat, cap, 'true' if detect else 'false'),
               jtype='GogglesItem')
        m.asset('textures/models/armor/%s_layer_1.png' % name, tex.armor_layer(color, 1, name))
        m.asset('textures/models/armor/%s_layer_2.png' % name, tex.armor_layer(color, 2, name))
    m.shaped('night_vision_goggles', rl('night_vision_goggles'), ['LCL', 'GBG'], {'L': 'minecraft:leather', 'C': 'fw_circuits:basic_circuit',
                                                                                'G': 'fw_parts:glass_lens', 'B': 'fw_batteries:battery_basic'})
    m.shaped('tactical_goggles', rl('tactical_goggles'), ['PCP', 'GNG'], {'P': '#forge:plates/steel', 'C': 'fw_circuits:advanced_circuit',
                                                                        'G': 'fw_crystals:energite_shard', 'N': rl('night_vision_goggles')})
    m.tr('tooltip.fw_goggles.night', 'Night vision while worn (20 FE/s)')
    m.tr('tooltip.fw_goggles.detect', 'Night vision + reveals creatures within 24 blocks (60 FE/s)')
    return m
