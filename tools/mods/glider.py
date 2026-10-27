from gen.fw import Mod


def build():
    m = Mod('fw_glider', 'Hang Gliders', 'Hang gliders for soaring off cliffs and sky islands: hold one while falling to glide '
                                         'in the direction you look. Sneak to drop.', tab='TOOLS')
    rl = m.rl
    m.init('GliderItem.init()')
    m.client('com.frontierworks.glider.client.GliderClient.init(bus)')
    m.item('hang_glider', ('glider', '#d8c690', '#6b4a2a'), java='new GliderItem(new Item.Properties().durability(400), 0.12, 0.035)', jtype='GliderItem')
    m.item('reinforced_glider', ('glider', '#3a7bd5', '#3b3b3b'), java='new GliderItem(new Item.Properties().durability(1600), 0.08, 0.05)', jtype='GliderItem',
           desc='Sinks slower and flies faster')
    m.shaped('hang_glider', rl('hang_glider'), ['LLL', 'SSS', ' S '], {'L': 'minecraft:leather', 'S': '#forge:rods/wooden'})
    m.shaped('reinforced_glider', rl('reinforced_glider'), ['CCC', 'RGR', ' R '], {'C': 'fw_chemistry:carbon_fiber', 'R': '#forge:rods/aluminum',
                                                                                  'G': rl('hang_glider')})
    m.tr('tooltip.fw_glider.use', 'Hold while falling to glide. Sneak to drop.')
    return m
