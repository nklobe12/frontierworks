"""Space Suit: a pressurised suit with an oxygen supply, and oxygen generators for breathable bases on the Moon and Mars."""
from gen.fw import Mod, horizontal_state
from gen import tex

M = 'fw_spacesuit'
PIECES = [('space_helmet', 'HELMET', ['GGG', 'PCP'], 'helmet'), ('space_suit', 'CHESTPLATE', ['P P', 'PTP', 'PPP'], 'chestplate'),
          ('space_leggings', 'LEGGINGS', ['PPP', 'P P', 'P P'], 'leggings'), ('space_boots', 'BOOTS', ['P P', 'R R'], 'boots')]


def build():
    m = Mod(M, 'Space Suit', 'Space has no air. Wear the full space suit to breathe on the Moon and Mars - its tank holds 30 minutes of '
                             'oxygen - and build Oxygen Generators (powered by Forge Energy) to make an 8-block breathable bubble around your '
                             'base and refill your suit.', deps=['fw_moon', 'fw_mars'], tab='SPACE')
    rl = m.rl
    m.init('com.frontierworks.spacesuit.OxygenEvents.init()')
    m.client('com.frontierworks.spacesuit.client.OxygenHud.init(bus)')
    m.field('public static final FWTiers.Armor SUIT_MAT = FWTiers.armor("fw_spacesuit:space_suit", 22, 2, 6, 5, 2, 10, 1.0f, 0.0f, "forge:plates/steel");')
    for name, typ, pattern, tpl in PIECES:
        m.item(name, (tpl, '#e8e8ec', None, '#ff8a2a'), java='new ArmorItem(Content.SUIT_MAT, ArmorItem.Type.%s, new Item.Properties())' % typ,
               jtype='ArmorItem', desc='Full suit: breathe in space' + ('. Holds 30 minutes of oxygen.' if typ == 'CHESTPLATE' else ''), tab='SPACE')
        keys = {k: v for k, v in {'P': '#forge:plates/steel', 'G': '#forge:glass_panes', 'C': 'fw_circuits:basic_circuit',
                                                             'T': rl('oxygen_tank'), 'R': 'minecraft:leather'}.items()
                                           if any(k in row for row in pattern)}
        if typ == 'CHESTPLATE':
            m.recipe(name, {'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'pattern': pattern,
                            'key': {k: ({'tag': v[1:]} if v.startswith('#') else {'item': v}) for k, v in keys.items()},
                            'result': {'item': rl(name), 'count': 1, 'nbt': '{Oxygen:600}'}})
        else:
            m.shaped(name, rl(name), pattern, keys)
    m.asset('textures/models/armor/space_suit_layer_1.png', tex.armor_layer('#e8e8ec', 1, 'suit'))
    m.asset('textures/models/armor/space_suit_layer_2.png', tex.armor_layer('#d8d8dc', 2, 'suit'))
    m.item('oxygen_tank', ('canister', '#4a90e2', None, '#ffffff'), tab='SPACE')
    m.shaped('oxygen_tank', rl('oxygen_tank'), [' P ', 'PGP', 'PPP'], {'P': '#forge:plates/iron', 'G': '#forge:glass'})
    t = lambda n: '%s:block/%s' % (M, n)
    front = tex.machine_front('advanced', 'wave', False, 'oxgen')
    front_on = tex.machine_front('advanced', 'wave', True, 'oxgen')
    side = tex.casing('advanced', 'oxgens', 'side')
    top = tex.casing('advanced', 'oxgent', 'top')
    mk = lambda f: {'parent': 'minecraft:block/orientable', 'textures': {'top': t('oxygen_generator_top'), 'side': t('oxygen_generator_side'), 'front': t(f)}}
    m.block('oxygen_generator', java='new com.frontierworks.spacesuit.OxygenGeneratorBlock(FWProps.machine())', jtype='com.frontierworks.spacesuit.OxygenGeneratorBlock',
            state=horizontal_state(m, 'oxygen_generator', 'oxygen_generator', 'oxygen_generator_on'),
            models={'oxygen_generator': mk('oxygen_generator_front'), 'oxygen_generator_on': mk('oxygen_generator_front_on')},
            textures={'block/oxygen_generator_front': front, 'block/oxygen_generator_front_on': front_on, 'block/oxygen_generator_side': side,
                      'block/oxygen_generator_top': top},
            tab='SPACE', level='stone', desc='Uses 40 FE/t. Makes air within 8 blocks and refills space suits (right-click while wearing one).')
    m.field('public static final RegistryObject<BlockEntityType<com.frontierworks.spacesuit.OxygenGeneratorBE>> OXYGEN_GENERATOR_BE = '
            'REG.blockEntity("oxygen_generator", com.frontierworks.spacesuit.OxygenGeneratorBE::new, Content.OXYGEN_GENERATOR);')
    m.shaped('oxygen_generator', rl('oxygen_generator'), ['PFP', 'TMT', 'PCP'], {'P': '#forge:plates/steel', 'F': 'fw_parts:fan', 'T': rl('oxygen_tank'),
                                                                                'M': 'fw_parts:machine_frame', 'C': 'fw_circuits:advanced_circuit'})
    for k, v in {'msg.fw_spacesuit.no_air': 'No air! Wear a full space suit or stand near an oxygen generator.',
                 'msg.fw_spacesuit.low': 'Oxygen low: %s seconds left', 'msg.fw_spacesuit.refilled': 'Suit oxygen refilled',
                 'msg.fw_spacesuit.empty': 'The generator has no oxygen stored', 'hud.fw_spacesuit.oxygen': 'O2 %s%%',
                 'hud.fw_spacesuit.bubble': 'O2 bubble', 'tooltip.fw_spacesuit.oxygen': 'Oxygen: %s / %s',
                 'tooltip.fw_spacesuit.generator': 'Energy %s FE, oxygen %s'}.items():
        m.tr(k, v)
    return m
