from gen.fw import Mod, const, horizontal_state
from gen import tex


def build():
    m = Mod('fw_teleporters', 'Teleporters', 'Powered teleport pads: record one pad on a Coordinate Card, link another to it, then '
                                             'crouch on the pad to jump - across the world or even between dimensions.', tab='LOGISTICS')
    rl = m.rl
    t = lambda n: '%s:block/%s' % (m.modid, n)
    name = 'teleport_pad'
    C = const(name)
    model = lambda top: {'parent': 'minecraft:block/block', 'textures': {'particle': t('pad_side'), 'top': t(top), 'side': t('pad_side')},
                         'elements': [{'from': [0, 0, 0], 'to': [16, 4, 16], 'faces': {
                             'up': {'texture': '#top'}, 'down': {'texture': '#side', 'cullface': 'down'},
                             'north': {'texture': '#side', 'uv': [0, 12, 16, 16]}, 'south': {'texture': '#side', 'uv': [0, 12, 16, 16]},
                             'east': {'texture': '#side', 'uv': [0, 12, 16, 16]}, 'west': {'texture': '#side', 'uv': [0, 12, 16, 16]}}}]}
    m.block(name, java='new PadBlock(FWProps.machineTransparent(), () -> Content.%s_BE.get())' % C, jtype='PadBlock', tab='LOGISTICS',
            state=horizontal_state(m, name, name, name + '_on'), models={name: model('pad_top'), name + '_on': model('pad_top_on')},
            textures={'block/pad_side': tex.casing('advanced', 'pad', 'side'), 'block/pad_top': tex.machine_front('advanced', 'atom', False, 'pad'),
                      'block/pad_top_on': tex.machine_front('advanced', 'atom', True, 'pad')}, level='stone',
            desc='Crouch on it for a second to teleport to its linked pad')
    m.field('public static final RegistryObject<BlockEntityType<TeleportPadBE>> %s_BE = REG.blockEntity("%s", (pos, state) -> new TeleportPadBE(Content.%s_BE.get(), pos, state), Content.%s);'
            % (C, name, C, C))
    m.item('coordinate_card', ('card', '#9b5de5'), java='new CoordinateCardItem(new Item.Properties())', jtype='CoordinateCardItem', tab='TOOLS',
           desc='Sneak-right-click a pad to record it. Right-click another pad to link it.')
    m.shaped('coordinate_card', rl('coordinate_card'), ['E', 'B'], {'E': 'fw_crystals:voidite_shard', 'B': 'fw_parts:blank_card'})
    m.shaped(name, rl(name), ['EEE', 'PFP', 'CWC'], {'E': '#forge:ender_pearls', 'P': '#forge:plates/electrum', 'F': 'fw_parts:advanced_frame',
                                                     'C': 'fw_crystals:voidite_shard', 'W': 'fw_circuits:advanced_circuit'})
    for k, v in {'gui.fw_teleporters.unlinked': 'Not linked', 'gui.fw_teleporters.linked': 'Linked', 'gui.fw_teleporters.cost': 'Cost: %s FE',
                 'gui.fw_teleporters.howto': 'Crouch on the pad to go', 'msg.fw_teleporters.no_power': 'Not enough power (%s FE needed)',
                 'msg.fw_teleporters.recorded': 'Pad recorded', 'msg.fw_teleporters.linked': 'Pad linked',
                 'msg.fw_teleporters.empty': 'Sneak-right-click a pad first to record it'}.items():
        m.tr(k, v)
    return m
