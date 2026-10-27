from gen.fw import Mod


def build():
    m = Mod('fw_drills', 'Power Tools', 'Electric drills and chainsaws that run on Forge Energy instead of durability. '
                                        'The advanced drill can switch to 3x3 mining. Charge them in any energy cell.', tab='TOOLS')
    rl = m.rl
    tools = [
        ('electric_drill', 'drill', '#c0392b', 'FWTiers.POWERED', 'PoweredToolItem.drillTags()', 50_000, 50, False, 3.0, -2.6),
        ('advanced_drill', 'drill', '#2980b9', 'FWTiers.POWERED_ADV', 'PoweredToolItem.drillTags()', 400_000, 100, True, 4.0, -2.6),
        ('chainsaw', 'chainsaw', '#e67e22', 'FWTiers.POWERED', 'PoweredToolItem.sawTags()', 50_000, 50, False, 7.0, -3.0),
        ('advanced_chainsaw', 'chainsaw', '#16a085', 'FWTiers.POWERED_ADV', 'PoweredToolItem.sawTags()', 400_000, 100, False, 9.0, -2.9),
    ]
    for name, art, col, tier, tags, cap, cost, area, dmg, spd in tools:
        m.item(name, (art, '#b0b8c0', None, col), model='handheld', jtype='PoweredToolItem',
               java='new PoweredToolItem(%s, %sf, %sf, %s, %d, %d, %s, new Item.Properties())' % (tier, dmg, spd, tags, cap, cost, 'true' if area else 'false'))
    m.item('drill_head', ('gear', '#c9d3de'), tab='MATERIALS')
    m.shaped('drill_head', rl('drill_head'), [' S ', 'SDS', 'SSS'], {'S': '#forge:plates/steel', 'D': '#forge:gems/diamond'})
    m.shaped('electric_drill', rl('electric_drill'), [' H ', 'PMP', 'PBP'], {'H': rl('drill_head'), 'P': '#forge:plates/iron', 'M': 'fw_parts:motor',
                                                                            'B': 'fw_batteries:battery_basic'})
    m.shaped('advanced_drill', rl('advanced_drill'), ['CHC', 'PDP', 'PBP'], {'H': rl('drill_head'), 'C': 'fw_circuits:advanced_circuit', 'P': '#forge:plates/steel',
                                                                            'D': rl('electric_drill'), 'B': 'fw_batteries:battery_advanced'})
    m.shaped('chainsaw', rl('chainsaw'), [' PP', 'PMP', 'BP '], {'P': '#forge:plates/iron', 'M': 'fw_parts:motor', 'B': 'fw_batteries:battery_basic'})
    m.shaped('advanced_chainsaw', rl('advanced_chainsaw'), ['CPP', 'PSP', 'BP '], {'C': 'fw_circuits:advanced_circuit', 'P': '#forge:plates/steel',
                                                                                  'S': rl('chainsaw'), 'B': 'fw_batteries:battery_advanced'})
    for k, v in {'tooltip.fw_drills.cost': '%s FE per block', 'tooltip.fw_drills.area': 'Mode: 3x3 (sneak + right-click to change)',
                 'tooltip.fw_drills.single': 'Mode: single block (sneak + right-click to change)', 'msg.fw_drills.area_on': '3x3 mode',
                 'msg.fw_drills.area_off': 'Single block mode'}.items():
        m.tr(k, v)
    return m
