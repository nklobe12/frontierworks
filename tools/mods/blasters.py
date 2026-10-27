"""Blasters: energy weapons powered by Forge Energy - pistol, rifle, scattergun and plasma cannon."""
from gen.fw import Mod
from gen import tex

M = 'fw_blasters'

# name: (colour, accent, capacity, cost/shot, damage, cooldown, pellets, spread, explosive power, bolt colour, lang)
BLASTERS = [
    ('blaster_pistol', '#5a6470', '#ff4040', 60_000, 300, 6.0, 8, 1, 0.0, 0.0, 'red', 'Blaster Pistol'),
    ('blaster_rifle', '#3e4650', '#40c0ff', 250_000, 800, 11.0, 16, 1, 0.0, 0.0, 'blue', 'Blaster Rifle'),
    ('scatter_blaster', '#6a5a48', '#ffb030', 200_000, 1200, 4.0, 22, 6, 0.12, 0.0, 'orange', 'Scatter Blaster'),
    ('plasma_cannon', '#40384a', '#c060ff', 1_000_000, 5000, 14.0, 40, 1, 0.0, 2.2, 'purple', 'Plasma Cannon'),
]
BOLTS = {'red': '#ff4a3a', 'blue': '#4ad8ff', 'orange': '#ffb84a', 'purple': '#d070ff'}


def bolt_tex(color):
    c = tex.rgb(color)
    img = tex.blank()
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 2.2:
                px[x, y] = (255, 255, 255, 255)
            elif d < 4.2:
                px[x, y] = c + (255,)
            elif d < 6:
                px[x, y] = c + (110,)
    return img


def build():
    m = Mod(M, 'Blasters', 'Energy weapons that run on Forge Energy: a quick blaster pistol, a hard-hitting rifle, a six-bolt scatter '
                           'blaster and an explosive plasma cannon. Recharge them in any energy cell.', tab='TOOLS')
    rl = m.rl
    m.client('com.frontierworks.blasters.client.BlastersClient.init(bus)')
    m.field('public static final RegistryObject<EntityType<com.frontierworks.blasters.BoltEntity>> BOLT = REG.entity("bolt", '
            '() -> EntityType.Builder.<com.frontierworks.blasters.BoltEntity>of(com.frontierworks.blasters.BoltEntity::new, MobCategory.MISC)'
            '.sized(0.3f, 0.3f).clientTrackingRange(8).updateInterval(2));')
    for c, hexc in BOLTS.items():
        m.item('bolt_' + c, None, tab='NONE', lang=c.title() + ' Bolt')
        m.texture('item/bolt_' + c, bolt_tex(hexc))
    for (name, col, acc, cap, cost, dmg, cd, pellets, spread, boom, bolt, lang) in BLASTERS:
        m.item(name, ('blaster', col, '#2a2a30', acc), lang=lang, model='handheld',
               java='new com.frontierworks.blasters.BlasterItem(new Item.Properties().stacksTo(1), %d, %d, %sf, %d, %d, %sf, %sf, "%s")'
                    % (cap, cost, dmg, cd, pellets, spread, boom, bolt),
               jtype='com.frontierworks.blasters.BlasterItem',
               desc='%s damage%s, %d FE per shot' % (int(dmg), ' x%d' % pellets if pellets > 1 else (' + explosion' if boom else ''), cost))
    m.tr('tooltip.fw_blasters.empty', 'Out of energy - recharge in an energy cell')
    m.item('focusing_lens', ('lens', '#ff6a5a'), tab='MATERIALS')
    m.shaped('focusing_lens', rl('focusing_lens'), [' G ', 'GRG', ' G '], {'G': '#forge:glass_panes', 'R': '#forge:gems/ruby'})
    m.shaped('blaster_pistol', rl('blaster_pistol'), ['PPL', ' CB', ' P '], {'P': '#forge:plates/steel', 'L': rl('focusing_lens'),
                                                                           'C': 'fw_circuits:basic_circuit', 'B': 'fw_batteries:battery_basic'})
    m.shaped('blaster_rifle', rl('blaster_rifle'), ['PPL', 'PCB', '  P'], {'P': '#forge:plates/steel', 'L': rl('focusing_lens'),
                                                                          'C': 'fw_circuits:advanced_circuit', 'B': 'fw_batteries:battery_advanced'})
    m.shaped('scatter_blaster', rl('scatter_blaster'), ['LLL', 'PCB', ' P '], {'P': '#forge:plates/steel', 'L': rl('focusing_lens'),
                                                                              'C': 'fw_circuits:advanced_circuit', 'B': 'fw_batteries:battery_advanced'})
    m.shaped('plasma_cannon', rl('plasma_cannon'), ['PEL', 'PCB', 'PP '], {'P': '#forge:plates/titanium', 'L': rl('focusing_lens'), 'E': 'fw_crystals:charged_energite',
                                                                          'C': 'fw_circuits:elite_circuit', 'B': 'fw_batteries:battery_elite'})
    return m
