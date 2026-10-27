from gen.fw import Mod
from gen import tex

PACKS = [
    # name, colour, capacity, cost/t, accel, maxUp, sprint boost, armor (def chest), lang
    ('jetpack_basic', '#b87333', 200_000, 32, 0.10, 0.35, 0.0, 2, 'Basic Jetpack'),
    ('jetpack_reinforced', '#7b8591', 1_000_000, 48, 0.14, 0.55, 0.02, 4, 'Reinforced Jetpack'),
    ('jetpack_flux', '#8e44ad', 8_000_000, 96, 0.18, 0.85, 0.05, 6, 'Flux Jetpack'),
]


def build():
    m = Mod('fw_jetpack', 'Jetpacks', 'Energy-powered jetpacks in three tiers. Hold jump to fly, sprint for a horizontal boost '
                                      '(reinforced and flux), and press H to toggle hover mode for gentle descents.', tab='TOOLS')
    rl = m.rl
    m.init('JetpackItem.init()')
    m.client('com.frontierworks.jetpack.client.JetpackClient.init(bus)')
    for name, color, cap, cost, acc, up, boost, armor, lang in PACKS:
        mat = name.upper() + '_MAT'
        m.field('public static final FWTiers.Armor %s = FWTiers.armor("fw_jetpack:%s", 20, 0, %d, 0, 0, 10, 0.0f, 0.0f, null);' % (mat, name, armor))
        m.item(name, ('jetpack', color, None, '#ff8a2a'), jtype='JetpackItem', lang=lang,
               java='new JetpackItem(Content.%s, new Item.Properties(), %d, %d, %s, %s, %s)' % (mat, cap, cost, acc, up, boost))
        m.asset('textures/models/armor/%s_layer_1.png' % name, tex.armor_layer(color, 1, name))
        m.asset('textures/models/armor/%s_layer_2.png' % name, tex.armor_layer(color, 2, name))
    m.item('thruster', ('canister', '#b87333', None, '#ff8a2a'), tab='MATERIALS')
    m.shaped('thruster', rl('thruster'), ['PCP', 'PFP', 'P P'], {'P': '#forge:plates/bronze', 'C': 'fw_parts:heating_coil', 'F': 'fw_parts:fan'})
    m.shaped('jetpack_basic', rl('jetpack_basic'), ['PCP', 'PBP', 'T T'], {'P': '#forge:plates/bronze', 'C': 'fw_circuits:basic_circuit',
                                                                          'B': 'fw_batteries:battery_basic', 'T': rl('thruster')})
    m.shaped('jetpack_reinforced', rl('jetpack_reinforced'), ['PCP', 'PJP', 'TBT'], {'P': '#forge:plates/steel', 'C': 'fw_circuits:advanced_circuit',
                                                                                    'J': rl('jetpack_basic'), 'T': rl('thruster'), 'B': 'fw_batteries:battery_advanced'})
    m.shaped('jetpack_flux', rl('jetpack_flux'), ['PCP', 'EJE', 'TBT'], {'P': 'fw_chemistry:carbon_fiber', 'C': 'fw_circuits:elite_circuit',
                                                                        'E': 'fw_crystals:charged_energite', 'J': rl('jetpack_reinforced'), 'T': rl('thruster'),
                                                                        'B': 'fw_batteries:battery_elite'})
    for k, v in {'tooltip.fw_jetpack.use': 'Wear on your chest. Hold jump to fly.', 'tooltip.fw_jetpack.hover_on': 'Hover: on (H)',
                 'tooltip.fw_jetpack.hover_off': 'Hover: off (H)', 'msg.fw_jetpack.hover_on': 'Hover mode on', 'msg.fw_jetpack.hover_off': 'Hover mode off',
                 'key.fw_jetpack.hover': 'Jetpack hover', 'key.categories.frontierworks': 'Frontierworks'}.items():
        m.tr(k, v)
    return m
