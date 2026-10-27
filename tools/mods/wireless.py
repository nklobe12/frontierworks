from gen.fw import Mod, custom_model_machine
from gen import tex


def build():
    m = Mod('fw_wireless', 'Wireless Energy', 'Beam power anywhere: feed a Wireless Transmitter, link a Receiver to it with a '
                                              'Link Card, and energy flows across any distance - even between dimensions.', tab='MACHINES')
    rl = m.rl
    for name, tx, tier in (('wireless_transmitter', True, 'advanced'), ('wireless_receiver', False, 'advanced')):
        textures = {'block/%s_side' % name: tex.casing(tier, m.modid + name, 'side'), 'block/%s_top' % name: tex.machine_front(tier, 'eye', False, name + 't'),
                    'block/%s_front' % name: tex.machine_front(tier, 'bolt', False, name), 'block/%s_front_on' % name: tex.machine_front(tier, 'bolt', True, name)}
        custom_model_machine(m, name, 'new WirelessBE(BE_TYPE, pos, state, %s, 2048)' % ('true' if tx else 'false'), 'WirelessBE', textures,
                             top=name + '_top', side=name + '_side', front=name + '_front', front_on=name + '_front_on',
                             desc='Accepts power from cables' if tx else 'Pulls power from its linked transmitter (2048 FE/t, more with Speed Upgrades)')
    m.item('link_card', ('card', '#2f6fe4'), java='new LinkCardItem(new Item.Properties())', jtype='LinkCardItem', tab='TOOLS',
           desc='Right-click a transmitter to store it, then a receiver to link')
    m.shaped('link_card', rl('link_card'), ['E', 'B'], {'E': '#forge:ender_pearls', 'B': 'fw_parts:blank_card'})
    m.shaped('wireless_transmitter', rl('wireless_transmitter'), [' E ', 'PFP', 'CWC'], {'E': 'fw_crystals:voidite_shard', 'P': '#forge:plates/electrum',
                                                                                       'F': 'fw_parts:advanced_frame', 'C': 'fw_circuits:advanced_circuit',
                                                                                       'W': 'fw_cables:gold_cable'})
    m.shaped('wireless_receiver', rl('wireless_receiver'), ['PEP', 'CFC', 'PWP'], {'E': 'fw_crystals:voidite_shard', 'P': '#forge:plates/electrum',
                                                                                  'F': 'fw_parts:advanced_frame', 'C': 'fw_circuits:advanced_circuit',
                                                                                  'W': 'fw_cables:gold_cable'})
    for k, v in {'gui.fw_wireless.transmitter': 'Transmitter', 'gui.fw_wireless.linked': 'Linked', 'gui.fw_wireless.unlinked': 'Not linked',
                 'msg.fw_wireless.stored': 'Transmitter stored on card', 'msg.fw_wireless.linked': 'Receiver linked',
                 'msg.fw_wireless.empty': 'Card is empty - use it on a transmitter first'}.items():
        m.tr(k, v)
    return m
