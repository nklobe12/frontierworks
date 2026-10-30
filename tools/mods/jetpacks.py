from PIL import Image, ImageDraw

from gen.ls import Mod, I
from gen import tex

TIERS = {
    # tier: (lang, metal colour, accent, fuel?, description)
    'bronze': ('Bronze Jetpack', '#c08040', '#e8a050', True, 'Burns liquid fuel. A short hop and a steady climb'),
    'steel': ('Steel Jetpack', '#8a9099', '#ff9a40', True, 'Burns liquid fuel. Faster, holds twice the fuel and can hover'),
    'electrum': ('Electrum Jetpack', '#d8c050', '#60d8ff', False, 'Runs on FE: charge it like any battery'),
    'duralumin': ('Duralumin Jetpack', '#b8c4d6', '#70e0ff', False, 'Runs on FE. Fast, light and can hover'),
    'platinum': ('Platinum Jetpack', '#dce6f0', '#a0f0ff', False, 'Runs on FE. The fastest climb and cruise, and it hovers'),
}

UPGRADES = {
    'thrust_booster': ('Thrust Booster', '#e06030', 'Up to 3: faster climb and acceleration, more fuel used'),
    'efficiency_regulator': ('Efficiency Regulator', '#40b060', 'Up to 3: each uses a fifth less fuel or energy'),
    'hover_stabilizer': ('Hover Stabilizer', '#60a0e0', 'Adds hover mode to the bronze and electrum jetpacks'),
    'armor_plating': ('Armor Plating', '#9098a8', 'Up to 2: +2 armor each'),
}


def jetpack_icon(metal, accent, fuel, seed):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    m = tex.rgb(metal)
    for x0 in (2, 9):
        for x in range(x0, x0 + 5):
            for y in range(2, 12):
                edge = x in (x0, x0 + 4) or y in (2, 11)
                light = 1.25 if x == x0 + 1 else 0.85 if x == x0 + 3 else 1.0
                px[x, y] = tex.shade(m, 0.6 if edge else light * (0.95 + 0.1 * tex.rnd(seed, x, y)))
        for x in range(x0 + 1, x0 + 4):
            px[x, 1] = tex.shade(m, 0.75)
            px[x, 12] = (60, 60, 66, 255)
            px[x, 13] = (40, 40, 44, 255)
    for y in range(3, 11):
        px[7, y] = (70, 50, 36, 255)
        px[8, y] = (90, 64, 44, 255)
    flame = [(255, 200, 80, 255), (240, 120, 40, 230)] if fuel else [(170, 240, 255, 255), (80, 190, 255, 220)]
    for x0 in (3, 10):
        for x in range(x0, x0 + 3):
            px[x, 14] = flame[0]
        px[x0 + 1, 15] = flame[1]
    a = tex.rgb(accent)
    px[4, 5] = px[11, 5] = tex.shade(a, 1.0)
    return img


def tank_tex(metal, seed):
    img = tex.noise_fill(metal, seed, 0.05)
    px = img.load()
    m = tex.rgb(metal)
    for y in range(16):
        px[0, y] = tex.shade(m, 0.65)
        px[15, y] = tex.shade(m, 0.65)
        px[3, y] = tex.shade(m, 1.25)
        px[4, y] = tex.shade(m, 1.15)
    for x in range(16):
        px[x, 0] = px[x, 15] = tex.shade(m, 0.6)
        px[x, 5] = tex.shade(m, 0.8)
    for x in (2, 7, 12):
        px[x, 6] = tex.shade(m, 1.4)
    return img


def frame_tex(seed):
    img = tex.noise_fill('#4a3424', seed, 0.08)
    px = img.load()
    for y in range(0, 16, 4):
        for x in range(16):
            px[x, y] = (40, 28, 20, 255)
    for x in (2, 13):
        for y in range(16):
            px[x, y] = (150, 140, 120, 255) if y % 4 == 2 else px[x, y]
    return img


def nozzle_tex(seed):
    img = tex.noise_fill('#3a3c42', seed, 0.06)
    px = img.load()
    for x in range(5, 11):
        for y in range(5, 11):
            px[x, y] = (20, 20, 22, 255)
    return img


def upgrade_icon(colour, kind, seed):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = tex.rgb(colour)
    d.rectangle([2, 2, 13, 13], fill=(58, 62, 70, 255), outline=(30, 32, 36, 255))
    d.rectangle([3, 3, 12, 4], fill=tex.shade(c, 0.8))
    if kind == 'thrust_booster':
        d.polygon([(8, 5), (12, 10), (10, 10), (10, 12), (6, 12), (6, 10), (4, 10)], fill=tex.shade(c, 1.2))
    elif kind == 'efficiency_regulator':
        d.ellipse([5, 7, 10, 12], fill=tex.shade(c, 1.1))
        d.polygon([(7.5, 4), (10, 9), (5, 9)], fill=tex.shade(c, 1.1))
    elif kind == 'hover_stabilizer':
        d.line([3, 9, 12, 9], fill=tex.shade(c, 1.3))
        d.line([5, 7, 10, 7], fill=tex.shade(c, 1.0))
        d.line([5, 11, 10, 11], fill=tex.shade(c, 1.0))
        d.rectangle([7, 8, 8, 10], fill=(255, 255, 255, 255))
    else:
        d.polygon([(4, 5), (11, 5), (11, 9), (7.5, 12), (4, 9)], fill=tex.shade(c, 1.2), outline=tex.shade(c, 0.6))
    return img


def part_icon(kind, seed):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if kind == 'fuel_tank':
        b = tex.rgb('#c08040')
        d.rounded_rectangle([4, 1, 11, 14], 2, fill=b, outline=tex.shade(b, 0.55))
        d.rectangle([6, 4, 9, 11], fill=(200, 230, 240, 255))
        d.rectangle([6, 8, 9, 11], fill=(110, 80, 40, 255))
        d.rectangle([6, 0, 9, 1], fill=tex.shade(b, 0.7))
    elif kind == 'combustion_thruster':
        g = tex.rgb('#8a9099')
        d.rectangle([5, 1, 10, 6], fill=g, outline=tex.shade(g, 0.6))
        d.polygon([(5, 7), (10, 7), (13, 13), (2, 13)], fill=tex.shade(g, 0.85), outline=tex.shade(g, 0.5))
        d.line([4, 14, 11, 14], fill=(255, 150, 60, 255))
    elif kind == 'ion_thruster':
        e = tex.rgb('#d8c050')
        d.ellipse([2, 2, 13, 13], fill=e, outline=tex.shade(e, 0.6))
        d.ellipse([5, 5, 10, 10], fill=(90, 210, 255, 255))
        d.ellipse([6, 6, 9, 9], fill=(220, 250, 255, 255))
    elif kind == 'flight_controller':
        d.rectangle([1, 3, 14, 12], fill=(40, 110, 60, 255), outline=(20, 60, 30, 255))
        d.ellipse([4, 4, 11, 11], fill=(230, 230, 220, 255), outline=(120, 100, 40, 255))
        d.line([7.5, 7.5, 10, 5], fill=(200, 40, 40, 255))
        for x in (2, 13):
            d.point([x, 5], fill=(220, 190, 80, 255))
            d.point([x, 10], fill=(220, 190, 80, 255))
    elif kind == 'capacitor_pack':
        e = tex.rgb('#d8c050')
        d.rectangle([2, 3, 13, 14], fill=(50, 54, 60, 255), outline=(25, 27, 30, 255))
        for x0 in (3, 6, 9):
            d.rectangle([x0, 5, x0 + 2, 12], fill=(200, 40, 40, 255))
            d.rectangle([x0, 5, x0 + 2, 6], fill=e)
        d.rectangle([5, 1, 10, 3], fill=e)
    elif kind == 'fuel_canister':
        r = tex.rgb('#b83a2a')
        d.rounded_rectangle([3, 3, 12, 14], 1, fill=r, outline=tex.shade(r, 0.55))
        d.line([4, 6, 11, 13], fill=tex.shade(r, 0.75))
        d.line([4, 13, 11, 6], fill=tex.shade(r, 0.75))
        d.rectangle([4, 1, 8, 2], fill=(60, 60, 64, 255))
        d.rectangle([10, 1, 11, 3], fill=(200, 200, 60, 255))
    return img


def box(frm, to, t):
    return {'from': frm, 'to': to, 'faces': {f: {'texture': t} for f in ('north', 'south', 'east', 'west', 'up', 'down')}}


def worn_model(tier):
    t = 'ls_jetpacks:block/'
    return {'parent': 'minecraft:block/block',
            'textures': {'tank': t + 'jetpack_tank_' + tier, 'frame': t + 'jetpack_frame', 'nozzle': t + 'jetpack_nozzle', 'particle': t + 'jetpack_tank_' + tier},
            'elements': [box([7, 2, 8], [9, 10, 10], '#frame'), box([4, 8, 8], [12, 9, 9.5], '#frame'),
                         box([3.5, 1, 9], [7.5, 11, 13], '#tank'), box([8.5, 1, 9], [12.5, 11, 13], '#tank'),
                         box([4.5, 11, 10], [6.5, 12, 12], '#tank'), box([9.5, 11, 10], [11.5, 12, 12], '#tank'),
                         box([4, -1, 9.5], [7, 1, 12.5], '#nozzle'), box([9, -1, 9.5], [12, 1, 12.5], '#nozzle')]}


def build():
    m = Mod('ls_jetpacks', 'Jetpacks', 'Fuel and electric jetpacks in five tiers with hover mode, thrust and efficiency upgrades, '
                                       'a fuel canister and a flight HUD.', tab='GEAR')
    m.init('JetpacksSetup.init(bus)')
    m.client('com.lodestar.jetpacks.client.JetpacksClient.init(bus)')
    rl = m.rl
    m.texture('models/armor/empty', Image.new('RGBA', (64, 32), (0, 0, 0, 0)))
    m.texture('block/jetpack_frame', frame_tex(m.modid + 'frame'))
    m.texture('block/jetpack_nozzle', nozzle_tex(m.modid + 'nozzle'))
    for tier, (lang, metal, accent, fuel, desc) in TIERS.items():
        name = tier + '_jetpack'
        m.item(name, jetpack_icon(metal, accent, fuel, m.modid + tier), java='new JetpackItem(JetpackTier.%s, new Item.Properties())' % tier.upper(),
               jtype='JetpackItem', tab='GEAR', lang=lang, desc=desc + '\nWear it, hold jump to fly')
        m.texture('block/jetpack_tank_' + tier, tank_tex(metal, m.modid + 'tank' + tier))
        m.asset('models/block/worn_%s.json' % tier, worn_model(tier))
    for name, (lang, colour, desc) in UPGRADES.items():
        m.item(name, upgrade_icon(colour, name, m.modid + name), java='new UpgradeItem(new Item.Properties(), JetpackItem.Mod.%s)'
               % {'thrust_booster': 'THRUST', 'efficiency_regulator': 'EFFICIENCY', 'hover_stabilizer': 'STABILIZER', 'armor_plating': 'PLATING'}[name],
               jtype='UpgradeItem', tab='GEAR', lang=lang, desc=desc + '\nCraft it together with a jetpack to install it')
    parts = {'fuel_tank': 'Fuel Tank', 'combustion_thruster': 'Combustion Thruster', 'ion_thruster': 'Ion Thruster',
             'flight_controller': 'Flight Controller', 'capacitor_pack': 'Capacitor Pack'}
    for name, lang in parts.items():
        m.item(name, part_icon(name, m.modid + name), tab='GEAR', lang=lang)
    m.item('fuel_canister', part_icon('fuel_canister', m.modid + 'can'), java='new FuelCanisterItem(new Item.Properties())', jtype='FuelCanisterItem',
           tab='GEAR', lang='Fuel Canister', desc='Holds 8,000 mB of fuel. Use it on a tank to fill it,\nuse it in the air to refuel the jetpack you wear')

    # components
    m.shaped('fuel_tank', rl('fuel_tank'), ['PGP', 'P P', 'PPP'], {'P': '#forge:plates/bronze', 'G': '#forge:glass_panes'})
    m.shaped('combustion_thruster', rl('combustion_thruster'), [' P ', 'PFP', 'P P'], {'P': '#forge:plates/bronze', 'F': 'minecraft:furnace'}, count=2)
    m.shaped('flight_controller', rl('flight_controller'), ['RCR', 'GQG', 'RCR'],
             {'R': 'minecraft:redstone', 'C': 'ls_machina:redstone_coil', 'G': '#forge:ingots/gold', 'Q': 'minecraft:compass'})
    m.shaped('capacitor_pack', rl('capacitor_pack'), ['ERE', 'RBR', 'ERE'], {'E': '#forge:plates/electrum', 'R': 'minecraft:redstone', 'B': 'minecraft:redstone_block'})
    m.shaped('ion_thruster', rl('ion_thruster'), [' E ', 'ECE', 'S S'], {'E': '#forge:plates/electrum', 'C': 'ls_machina:redstone_coil', 'S': '#forge:ingots/silver'}, count=2)
    m.shaped('fuel_canister', rl('fuel_canister'), ['TN ', 'T T', 'TTT'], {'T': '#forge:plates/tin', 'N': 'minecraft:iron_nugget'})
    # jetpacks: the first of each line is a plain recipe, later tiers carry upgrades over from the pack they are made from
    m.shaped('bronze_jetpack', rl('bronze_jetpack'), ['TCT', 'PLP', 'H H'],
             {'T': rl('fuel_tank'), 'C': rl('flight_controller'), 'P': '#forge:plates/bronze', 'L': 'minecraft:leather', 'H': rl('combustion_thruster')})
    m.shaped('electrum_jetpack', rl('electrum_jetpack'), ['PAP', 'PCP', 'I I'],
             {'A': rl('capacitor_pack'), 'C': rl('flight_controller'), 'P': '#forge:plates/electrum', 'I': rl('ion_thruster')})

    def tiered(name, pattern, key):
        m.recipe(name, {'type': 'ls_jetpacks:tiered_jetpack', 'category': 'equipment', 'pattern': pattern,
                        'key': {k: I(v) for k, v in key.items()}, 'result': {'item': rl(name)}})
    tiered('steel_jetpack', ['PTP', 'PJP', 'H H'], {'P': '#forge:plates/steel', 'T': rl('fuel_tank'), 'J': rl('bronze_jetpack'), 'H': rl('combustion_thruster')})
    tiered('duralumin_jetpack', ['PAP', 'PJP', 'I I'], {'P': '#forge:plates/duralumin', 'A': rl('capacitor_pack'), 'J': rl('electrum_jetpack'), 'I': rl('ion_thruster')})
    tiered('platinum_jetpack', ['PAP', 'DJD', 'I I'], {'P': '#forge:plates/platinum', 'A': rl('capacitor_pack'), 'D': 'minecraft:diamond',
                                                     'J': rl('duralumin_jetpack'), 'I': rl('ion_thruster')})
    # upgrades
    m.shaped('thrust_booster', rl('thrust_booster'), ['PRP', 'HBH', 'PRP'],
             {'P': '#forge:plates/steel', 'R': 'minecraft:redstone', 'H': rl('combustion_thruster'), 'B': 'minecraft:blaze_powder'})
    m.shaped('efficiency_regulator', rl('efficiency_regulator'), ['PRP', 'RCR', 'PRP'],
             {'P': '#forge:plates/invar', 'R': 'minecraft:redstone', 'C': 'ls_machina:redstone_coil'})
    m.shaped('hover_stabilizer', rl('hover_stabilizer'), ['FPF', 'PCP', 'FPF'],
             {'F': 'minecraft:feather', 'P': '#forge:plates/silver', 'C': rl('flight_controller')})
    m.shaped('armor_plating', rl('armor_plating'), ['P P', 'PPP', 'P P'], {'P': '#forge:plates/steel'})
    m.recipe('jetpack_upgrade', {'type': 'ls_jetpacks:jetpack_upgrade', 'category': 'equipment'})

    for k, v in {
        'tooltip.ls_jetpacks.fuel': 'Fuel: %s, %s / %s mB', 'tooltip.ls_jetpacks.empty': 'empty',
        'tooltip.ls_jetpacks.energy': 'Energy: %s / %s FE',
        'tooltip.ls_jetpacks.engine_on': 'Engine on', 'tooltip.ls_jetpacks.engine_off': 'Engine off',
        'tooltip.ls_jetpacks.hover_on': 'Hover on', 'tooltip.ls_jetpacks.hover_off': 'Hover off',
        'tooltip.ls_jetpacks.stats': 'Climb %s m/s, cruise %s m/s', 'tooltip.ls_jetpacks.keys': 'J: engine on/off   H: hover   sneak: descend',
        'tooltip.ls_jetpacks.max_level': 'Installs up to %s times',
        'msg.ls_jetpacks.low': 'Jetpack running low!', 'msg.ls_jetpacks.no_hover': 'This jetpack needs a hover stabilizer to hover',
        'msg.ls_jetpacks.refuelled': 'Refuelled with %s mB',
        'hud.ls_jetpacks.engine_on': 'ENGINE', 'hud.ls_jetpacks.engine_off': 'OFF', 'hud.ls_jetpacks.hover_on': 'HOVER', 'hud.ls_jetpacks.hover_off': 'hover',
        'key.ls_jetpacks.engine': 'Jetpack engine on/off', 'key.ls_jetpacks.hover': 'Jetpack hover mode',
        'lens.category.ls_jetpacks.jet_fuels': 'Jetpack Fuels', 'lens.category.ls_jetpacks.jetpack_upgrades': 'Jetpack Upgrades',
        'lens.ls_jetpacks.grade': 'Grade %s', 'lens.ls_jetpacks.flight': '%s s of bronze jetpack thrust per bucket',
        'lens.ls_jetpacks.max': 'Craft with any jetpack (up to %s)',
    }.items():
        m.tr(k, v)

    m.codex_intro('Jetpacks are worn in the chest slot. Hold jump to climb, steer with the movement keys while you fly, and hold sneak '
                  'to come down gently. J switches the engine off so you can walk normally; H switches hover mode, which holds you in '
                  'the air when you let go of jump.')
    m.codex_page('Fuel jetpacks', 'Bronze and steel jetpacks burn liquid fuel. Creosote from the Machina pyrolyzer works; light oil from the '
                                  'refinery lasts twice as long, and refined fuels like diesel and kerosene longer still. Fill a jetpack by using it '
                                  'on a tank, in a bottler, or carry a fuel canister and use it in the air to top up the pack you wear.',
                 items=[rl('bronze_jetpack'), rl('steel_jetpack'), rl('fuel_tank'), rl('combustion_thruster'), rl('fuel_canister')])
    m.codex_page('Electric jetpacks', 'Electrum, duralumin and platinum jetpacks run on FE. Charge them in a dynamo charge slot or any '
                                      'charger. Each tier climbs faster, cruises faster and holds more energy; duralumin and platinum '
                                      'also get a boost while you sprint.',
                 items=[rl('electrum_jetpack'), rl('duralumin_jetpack'), rl('platinum_jetpack'), rl('capacitor_pack'), rl('ion_thruster')])
    m.codex_page('Upgrades', 'Craft an upgrade together with a jetpack to install it. Thrust boosters make you climb and accelerate faster '
                             'but burn more; efficiency regulators save fuel; a hover stabilizer gives the bronze and electrum packs hover '
                             'mode; armor plating adds protection. Making the next tier from a jetpack keeps its upgrades.',
                 items=[rl(n) for n in UPGRADES])
    m.codex_page('Flying with an elytra', 'Wear an elytra over... no: a jetpack takes the chest slot, so you cannot wear both. '
                                          'Instead, steel and better jetpacks push you along your look direction while you glide '
                                          'from a fall, which makes long descents into fast swoops.', items=[rl('steel_jetpack')])
    return m
