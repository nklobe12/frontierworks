"""Wind & Water (ls_aeolus): wind turbines with wearing rotors, water wheels and tidal generators."""
import math

from PIL import Image, ImageDraw

from gen.ls import Mod, entity_block, element_model, custom_model_machine, horizontal_model_state, const
from gen import tex

# name: (lang, radius in blocks, blades, peak FE/t, lifetime in seconds, colour, accent, repair tag, recipe keys)
ROTORS = {
    'wooden_rotor': ('Wooden Rotor', 2, 4, 10, 1800, '#a07a48', '#6b4a26', 'minecraft:planks',
                     ([' P ', 'PSP', ' P '], {'P': '#minecraft:planks', 'S': 'minecraft:stick'})),
    'canvas_rotor': ('Canvas Sail Rotor', 2, 4, 18, 3600, '#e8e2d2', '#7a5a34', 'minecraft:wool',
                     ([' W ', 'WRW', ' W '], {'W': '#minecraft:wool', 'R': 'ls_aeolus:wooden_rotor'})),
    'bronze_rotor': ('Bronze Rotor', 3, 3, 32, 7200, '#c8873a', '#7a5020', 'forge:plates/bronze',
                     ([' P ', 'PGP', ' P '], {'P': '#forge:plates/bronze', 'G': '#forge:gears/bronze'})),
    'steel_rotor': ('Steel Rotor', 3, 3, 50, 14400, '#9aa4ae', '#4d555e', 'forge:plates/steel',
                    ([' P ', 'PGP', ' P '], {'P': '#forge:plates/steel', 'G': '#forge:gears/steel'})),
    'duralumin_rotor': ('Duralumin Rotor', 4, 3, 80, 28800, '#dfe6ee', '#5b7aa0', 'forge:plates/duralumin',
                        ([' P ', 'PGP', 'R R'], {'P': '#forge:plates/duralumin', 'G': '#forge:gears/duralumin', 'R': '#forge:rods/aluminum'})),
}


# ----------------------------------------------------------------------------- textures

def painted(seed, base='#d9dde2', trim='#8d959e'):
    img = tex.noise_fill(base, seed, 0.035)
    px = img.load()
    c, t = tex.rgb(base), tex.rgb(trim)
    for i in range(16):
        px[i, 0] = px[0, i] = tex.shade(t, 1.1)
        px[i, 15] = px[15, i] = tex.shade(t, 0.75)
    for i in range(1, 15):
        px[i, 1] = px[1, i] = tex.shade(c, 1.0, 0.06)
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        px[x, y] = tex.shade(t, 0.6)
    return img


def turbine_side(seed):
    img = painted(seed)
    px = img.load()
    t = tex.rgb('#8d959e')
    for x in range(3, 13):
        px[x, 6] = tex.shade(t, 0.8)
        px[x, 9] = tex.shade(t, 1.0, 0.1)
    for y in range(6, 10):
        px[3, y] = px[12, y] = tex.shade(t, 0.8)
    stripe = tex.rgb('#c0392b')
    for x in range(1, 15):
        px[x, 12] = tex.shade(stripe, 1.0)
        px[x, 13] = tex.shade(stripe, 0.8)
    return img


def turbine_top(seed):
    img = painted(seed + 't')
    px = img.load()
    for y in range(4, 12, 2):
        for x in range(4, 12):
            px[x, y] = (70, 76, 84, 255)
            px[x, y + 1] = (150, 156, 164, 255)
    return img


def turbine_front(seed):
    img = painted(seed + 'f')
    d = ImageDraw.Draw(img)
    d.ellipse([3, 3, 12, 12], fill=(110, 116, 124, 255), outline=(60, 64, 70, 255))
    d.ellipse([5, 5, 10, 10], fill=(150, 156, 164, 255), outline=(80, 86, 92, 255))
    px = img.load()
    for (x, y) in ((7, 4), (4, 7), (11, 8), (8, 11)):
        px[x, y] = (40, 42, 46, 255)
    return img


def blade_tex(colour, accent, seed, kind):
    img = tex.noise_fill(colour, seed, 0.06 if kind == 'metal' else 0.1)
    px = img.load()
    c, a = tex.rgb(colour), tex.rgb(accent)
    if kind == 'wood':
        for y in range(16):
            for x in range(16):
                if (x * 3 + y // 4) % 7 == 0:
                    px[x, y] = tex.shade(c, 0.8)
    elif kind == 'canvas':
        for y in range(16):
            for x in range(16):
                if (x + y) % 4 == 0 or (x - y) % 4 == 0:
                    px[x, y] = tex.shade(c, 0.92)
        for y in range(16):
            px[0, y] = px[15, y] = tex.shade(a, 1.0)
    else:
        for y in range(16):
            px[0, y] = tex.shade(c, 1.0, 0.18)
            px[15, y] = tex.shade(c, 0.7)
        for x in range(16):
            px[x, 3] = tex.shade(a, 1.0)
    return img


def rotor_icon(colour, accent, blades, seed):
    """Front view of a rotor: tapered blades with a light leading edge around a hub."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c, a = tex.rgb(colour), tex.rgb(accent)
    cx = cy = 7.5
    for i in range(blades):
        ang = -math.pi / 2 + i * 2 * math.pi / blades + 0.3
        dx, dy = math.cos(ang), math.sin(ang)
        px_, py_ = -dy, dx
        root, tip = 2.0, 7.6
        pts = [(cx + dx * root + px_ * 1.2, cy + dy * root + py_ * 1.2),
               (cx + dx * 5.0 + px_ * 2.3, cy + dy * 5.0 + py_ * 2.3),
               (cx + dx * tip + px_ * 1.0, cy + dy * tip + py_ * 1.0),
               (cx + dx * tip - px_ * 0.6, cy + dy * tip - py_ * 0.6),
               (cx + dx * root - px_ * 1.0, cy + dy * root - py_ * 1.0)]
        d.polygon(pts, fill=tex.shade(c, 1.0), outline=tex.shade(c, 0.5))
        d.line([(cx + dx * root - px_ * 0.5, cy + dy * root - py_ * 0.5), (cx + dx * (tip - 0.6) - px_ * 0.2, cy + dy * (tip - 0.6) - py_ * 0.2)],
               fill=tex.shade(c, 1.0, 0.22))
    d.ellipse([5, 5, 10, 10], fill=tex.shade(a, 1.0), outline=tex.shade(a, 0.45))
    img.putpixel((7, 7), tex.shade(a, 1.0, 0.3))
    return img


def plank_tex(seed, colour='#9b7442'):
    return tex.planks(colour, seed)


def iron_band(seed):
    img = tex.planks('#7d5a32', seed)
    px = img.load()
    for y in (0, 1, 14, 15):
        for x in range(16):
            px[x, y] = (96, 100, 106, 255) if y in (0, 15) else (140, 146, 154, 255)
    for x in (3, 12):
        px[x, 1] = px[x, 14] = (60, 62, 66, 255)
    return img


def axle_tex(seed):
    img = tex.noise_fill('#6e7178', seed, 0.06)
    px = img.load()
    for x in range(16):
        for y in range(16):
            if (x + y) % 6 == 0:
                px[x, y] = (90, 94, 100, 255)
    return img


def wheel_icon(seed):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    wood, dark = (160, 118, 66, 255), (92, 64, 34, 255)
    d.ellipse([1, 1, 14, 14], outline=dark)
    d.ellipse([2, 2, 13, 13], outline=wood)
    for i in range(8):
        ang = i * math.pi / 4
        x1, y1 = 7.5 + math.cos(ang) * 6.5, 7.5 + math.sin(ang) * 6.5
        d.line([(7.5, 7.5), (x1, y1)], fill=wood)
        x2, y2 = 7.5 + math.cos(ang) * 8.2, 7.5 + math.sin(ang) * 8.2
        d.line([(x1, y1), (x2, y2)], fill=dark)
    d.rectangle([6, 6, 9, 9], fill=(110, 114, 122, 255), outline=(60, 62, 68, 255))
    return img


def tidal_side(seed):
    img = tex.casing('advanced', seed, 'side')
    px = img.load()
    teal = tex.rgb('#2a9d8f')
    for x in range(2, 14):
        px[x, 7] = tex.shade(teal, 1.0)
        px[x, 8] = tex.shade(teal, 0.75)
    return img


def tidal_front(seed):
    img = tex.casing('advanced', seed, 'front')
    px = img.load()
    for y in range(3, 13):
        for x in range(3, 13):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 5.2:
                px[x, y] = (24, 40, 46, 255) if (x + y) % 2 else (40, 70, 78, 255)
            elif d < 6.2:
                px[x, y] = (150, 170, 178, 255)
    return img


def anemometer_tex(seed, kind):
    if kind == 'base':
        img = tex.noise_fill('#8e9196', seed, 0.05)
        px = img.load()
        for i in range(16):
            px[i, 0] = px[0, i] = (170, 172, 178, 255)
            px[i, 15] = px[15, i] = (80, 82, 88, 255)
        return img
    if kind == 'cup':
        img = tex.noise_fill('#d9844f', seed, 0.08)
        return img
    img = tex.noise_fill('#5c5f66', seed, 0.05)
    return img


def anemometer_icon():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([7, 5, 8, 14], fill=(110, 114, 122, 255))
    d.rectangle([4, 14, 11, 15], fill=(90, 92, 98, 255))
    d.line([(2, 4), (13, 4)], fill=(80, 84, 90, 255))
    for x in (1, 12):
        d.rectangle([x, 2, x + 2, 5], fill=(217, 132, 79, 255), outline=(130, 70, 36, 255))
    d.rectangle([6, 1, 9, 4], fill=(217, 132, 79, 255), outline=(130, 70, 36, 255))
    return img


# ----------------------------------------------------------------------------- build

def build():
    m = Mod('ls_aeolus', 'Wind & Water', 'Power from the weather and the sea. Wind turbines turn faster the higher they stand and in '
                                          'storms, but need open air around their rotors, and rotors wear out. Water wheels turn in moving '
                                          'water, and tidal generators follow the tides of the ocean through the day and the phases of '
                                          'the moon. An anemometer tells you how hard the wind is blowing.', tab='POWER')
    m.init('AeolusSetup.init(bus)')
    m.client('com.lodestar.aeolus.client.AeolusClient.init(bus)')
    rl = m.rl
    t = lambda n: '%s:block/%s' % (m.modid, n)

    # ---------------------------------------------------------------- rotors
    for name, (lang, radius, blades, power, life, colour, accent, repair, (pattern, keys)) in ROTORS.items():
        kind = 'wood' if name.startswith('wooden') else 'canvas' if name.startswith('canvas') else 'metal'
        m.item(name, rotor_icon(colour, accent, blades, m.modid + name),
               java='new RotorItem(new Item.Properties().durability(%d), %d, %d, %d, "%s")' % (life, radius, blades, power, repair),
               jtype='RotorItem', lang=lang, tags=['ls_aeolus:rotors'])
        m.texture('block/%s_blade' % name, blade_tex(colour, accent, m.modid + name + 'b', kind))
        bt = {'blade': t(name + '_blade'), 'particle': t(name + '_blade')}
        # a blade one block long from the hub centre along +y, in a plane just in front of the north face
        root = ([7, 9, -4.5], [9, 13, -3.5], '#blade')
        vane = ([6.2, 13, -4.25], [9.8, 24, -3.75], '#blade')
        m.asset('models/block/%s_blade.json' % name, element_model([root, vane], bt))
        m.shaped(name, rl(name), pattern, keys)
    m.texture('block/rotor_hub', tex.metal_block('#8a9099', m.modid + 'hub'))
    m.asset('models/block/rotor_hub.json', element_model([([5, 5, -5], [11, 11, 0], '#hub'), ([6.5, 6.5, -6], [9.5, 9.5, -5], '#hub')],
                                                         {'hub': t('rotor_hub'), 'particle': t('rotor_hub')}))

    # ---------------------------------------------------------------- wind turbine
    s = m.modid + 'wt'
    custom_model_machine(m, 'wind_turbine', 'new WindTurbineBE(BE_TYPE, pos, state)', 'WindTurbineBE',
                         {'block/wind_turbine_side': turbine_side(s), 'block/wind_turbine_top': turbine_top(s),
                          'block/wind_turbine_front': turbine_front(s), 'block/wind_turbine_bottom': painted(s + 'b', '#b9bec4')},
                         top='wind_turbine_top', side='wind_turbine_side', bottom='wind_turbine_bottom', front='wind_turbine_front',
                         tab='POWER', props='LSProps.metal(3.5f)', desc='Fit a rotor; it spins on the front face\nMore power the higher it stands and in storms\n'
                                           'The rotor needs clear air around it')
    m.shaped('wind_turbine', rl('wind_turbine'), ['PRP', 'GFG', 'PCP'], {'P': '#forge:plates/iron', 'R': '#forge:rods/iron',
                                                                       'G': '#forge:gears/copper', 'F': 'ls_machina:machine_frame',
                                                                       'C': 'ls_machina:redstone_coil'})

    # ---------------------------------------------------------------- water wheel
    m.texture('block/water_wheel_hub', iron_band(m.modid + 'wwh'))
    m.texture('block/water_wheel_axle', axle_tex(m.modid + 'wwa'))
    m.texture('block/water_wheel_planks', plank_tex(m.modid + 'wwp'))
    m.texture('item/water_wheel', wheel_icon(m.modid + 'wwi'))
    wtx = {'hub': t('water_wheel_hub'), 'axle': t('water_wheel_axle'), 'particle': t('water_wheel_hub')}
    hub_model = element_model([([6, 6, 0], [10, 10, 16], '#axle'), ([3, 3, 4.5], [13, 13, 11.5], '#hub')], wtx)
    entity_block(m, 'water_wheel', 'WaterWheelBE', 'new MachineBlock(LSProps.wood().strength(2.5f).noOcclusion(), BE_TYPE)'
                 '.shape(net.minecraft.world.level.block.Block.box(3, 3, 3, 13, 13, 13))', jtype='LSEntityBlock', tab='POWER', models={'water_wheel': hub_model},
                 state=horizontal_model_state(m, 'water_wheel', 'water_wheel'), item_model='generated', mine='axe',
                 desc='Turns in moving water around it\nWater falling or flowing on one side makes it spin;\n'
                      'water pushing both ways cancels out\nPower comes out of both ends of the axle')
    ptx = {'planks': t('water_wheel_planks'), 'particle': t('water_wheel_planks')}
    # one of eight arms: spoke, rim segment and paddle, pointing up from the hub (radius ~1.45 blocks)
    arm = element_model([([7, 11, 6.5], [9, 27, 9.5], '#planks'), ([-1.5, 27, 5], [17.5, 29, 11], '#planks'),
                         ([7.4, 19, 3], [8.6, 31.5, 13], '#planks')], ptx)
    m.asset('models/block/water_wheel_arm.json', arm)
    m.shaped('water_wheel', rl('water_wheel'), ['LPL', 'PCP', 'LGL'], {'L': '#minecraft:logs', 'P': '#minecraft:planks',
                                                                      'C': 'ls_machina:redstone_coil', 'G': '#forge:gears/copper'})

    # ---------------------------------------------------------------- tidal generator
    s = m.modid + 'td'
    custom_model_machine(m, 'tidal_generator', 'new TidalGeneratorBE(BE_TYPE, pos, state)', 'TidalGeneratorBE',
                         {'block/tidal_generator_side': tidal_side(s), 'block/tidal_generator_top': tex.casing('advanced', s, 'top'),
                          'block/tidal_generator_bottom': tex.casing('advanced', s, 'bottom'), 'block/tidal_generator_front': tidal_front(s)},
                         top='tidal_generator_top', side='tidal_generator_side', bottom='tidal_generator_bottom', front='tidal_generator_front',
                         tab='POWER', props='LSProps.metal(3.5f)', desc='Sink it in an ocean, under water\nMost power while the tide runs in or out,\n'
                                           'none at high and low water; spring tides at full and new moon')
    m.texture('block/tidal_blade', blade_tex('#b8c4cc', '#2a9d8f', m.modid + 'tb', 'metal'))
    m.asset('models/block/tidal_blade.json', element_model([([7, 9, -2.5], [9, 13, -1.5], '#blade'), ([5.8, 12, -2.25], [10.2, 22, -1.75], '#blade')],
                                                           {'blade': t('tidal_blade'), 'particle': t('tidal_blade')}))
    m.shaped('tidal_generator', rl('tidal_generator'), ['PGP', 'CFC', 'PSP'], {'P': '#forge:plates/bronze', 'G': '#forge:gears/bronze',
                                                                              'C': 'ls_machina:redstone_coil', 'F': 'ls_machina:machine_frame',
                                                                              'S': ['minecraft:prismarine_shard', 'minecraft:nautilus_shell']})

    # ---------------------------------------------------------------- anemometer
    m.texture('block/anemometer_base', anemometer_tex(m.modid + 'ab', 'base'))
    m.texture('block/anemometer_cup', anemometer_tex(m.modid + 'ac', 'cup'))
    m.texture('block/anemometer_post', anemometer_tex(m.modid + 'ap', 'post'))
    m.texture('item/anemometer', anemometer_icon())
    atx = {'base': t('anemometer_base'), 'post': t('anemometer_post'), 'cup': t('anemometer_cup'), 'particle': t('anemometer_base')}
    base_model = element_model([([3, 0, 3], [13, 2, 13], '#base'), ([7, 2, 7], [9, 14, 9], '#post')], atx)
    entity_block(m, 'anemometer', 'AnemometerBE', 'new LSEntityBlock(LSProps.metal(1.5f).noOcclusion(), BE_TYPE).noMenu()'
                 '.shape(net.minecraft.world.level.block.Block.box(3, 0, 3, 13, 16, 13))',
                 tab='POWER', models={'anemometer': base_model}, item_model='generated',
                 desc='Shows the wind where it stands\nUse it for a reading; a comparator reads wind strength')
    cups = []
    for i in range(3):
        ang = i * 2 * math.pi / 3
        cx, cz = 8 + math.cos(ang) * 5.5, 8 + math.sin(ang) * 5.5
        cups.append(([cx - 1.2, 14, cz - 1.2], [cx + 1.2, 16.4, cz + 1.2], '#cup'))
    arms = [([2.5, 15, 7.6], [13.5, 15.4, 8.4], '#post'), ([7.6, 15, 2.5], [8.4, 15.4, 13.5], '#post'), ([7, 14, 7], [9, 16.6, 9], '#post')]
    m.asset('models/block/anemometer_cups.json', element_model(arms + cups, atx))
    vane = [([7.7, 17, 1.5], [8.3, 17.6, 14], '#post'), ([7.5, 16.5, 11], [8.5, 19.5, 15], '#cup'), ([7.4, 16.8, 0.5], [8.6, 17.8, 2.5], '#cup'),
            ([7.5, 16.6, 7.5], [8.5, 17.2, 8.5], '#post')]
    m.asset('models/block/anemometer_vane.json', element_model(vane, atx))
    m.shaped('anemometer', rl('anemometer'), ['CNC', ' R ', ' S '], {'C': '#forge:plates/copper', 'N': 'minecraft:compass',
                                                                     'R': '#forge:rods/iron', 'S': 'minecraft:smooth_stone'})

    # ---------------------------------------------------------------- lang
    for k, v in {
        'gui.ls_aeolus.output': 'Output: %s FE/t',
        'gui.ls_aeolus.wind': 'Wind: %s m/s %s',
        'gui.ls_aeolus.height': 'Height: x%s',
        'gui.ls_aeolus.clear': 'Air: %s%% clear',
        'gui.ls_aeolus.status.ok': 'Running',
        'gui.ls_aeolus.status.no_rotor': 'No rotor fitted',
        'gui.ls_aeolus.status.blocked': 'Rotor blocked (%s)',
        'gui.ls_aeolus.status.collide': 'Rotors overlap',
        'gui.ls_aeolus.status.low': 'Too low for wind',
        'gui.ls_aeolus.status.no_wind': 'No wind here',
        'gui.ls_aeolus.status.stalled': 'Wheel jammed (%s)',
        'gui.ls_aeolus.status.dry': 'No moving water',
        'gui.ls_aeolus.status.not_ocean': 'Not in an ocean',
        'gui.ls_aeolus.status.not_submerged': 'Must be under water',
        'gui.ls_aeolus.status.shallow': 'Too little water',
        'gui.ls_aeolus.flow': 'Moving water: %s, torque %s',
        'gui.ls_aeolus.tide.flood': 'Tide: rising',
        'gui.ls_aeolus.tide.ebb': 'Tide: falling',
        'gui.ls_aeolus.tide.slack': 'Tide: slack water',
        'gui.ls_aeolus.water': 'Open water: %s%%',
        'gui.ls_aeolus.moon': 'Moon: %s (x%s)',
        'gui.ls_aeolus.moon.spring': 'spring tide',
        'gui.ls_aeolus.moon.neap': 'neap tide',
        'gui.ls_aeolus.moon.normal': 'ordinary tide',
        'gui.ls_aeolus.chart': 'Output through the day (now marked)',
        'gui.ls_aeolus.dial': 'Wind from the %s, turbine faces %s',
        'gui.ls_aeolus.dial.hint': 'Facing into or away from the wind is best',
        'msg.ls_aeolus.reading': 'Wind %s m/s from the %s (%s). Turbines here would run at %s%%.',
        'msg.ls_aeolus.calm': 'calm', 'msg.ls_aeolus.breeze': 'breeze', 'msg.ls_aeolus.strong': 'strong wind', 'msg.ls_aeolus.gale': 'gale',
        'msg.ls_aeolus.no_wind': 'No wind blows here.',
        'msg.ls_aeolus.rotor_broke': 'The rotor of a wind turbine wore out',
        'tooltip.ls_aeolus.rotor': 'Radius %s blocks, %s FE/t at full wind',
        'tooltip.ls_aeolus.rotor_life': 'Lasts %s of running (wears twice as fast in storms)',
        'tooltip.ls_aeolus.rotor_left': '%s left',
        'tooltip.ls_aeolus.rotor_repair': 'Repair it on an anvil',
        'lens.category.ls_aeolus.rotors': 'Wind Turbine Rotors',
        'lens.category.ls_aeolus.water_wheel': 'Water Wheel',
        'lens.category.ls_aeolus.tidal': 'Tidal Generator',
        'lens.ls_aeolus.rotor': 'Radius %s, %s blades',
        'lens.ls_aeolus.rotor2': '%s FE/t peak, lasts %s',
        'lens.ls_aeolus.wheel1': 'Up to %s FE/t per block of water',
        'lens.ls_aeolus.wheel2': 'moving one way around the wheel',
        'lens.ls_aeolus.tidal1': 'Up to %s FE/t in an ocean',
        'lens.ls_aeolus.tidal2': 'following the tide and the moon',
    }.items():
        m.tr(k, v)
    for d, nm in (('n', 'north'), ('ne', 'north-east'), ('e', 'east'), ('se', 'south-east'), ('s', 'south'), ('sw', 'south-west'),
                  ('w', 'west'), ('nw', 'north-west')):
        m.tr('gui.ls_aeolus.dir.' + d, d.upper())
        m.tr('msg.ls_aeolus.dir.' + d, nm)
    for i, nm in enumerate(['full', 'waning gibbous', 'last quarter', 'waning crescent', 'new', 'waxing crescent', 'first quarter', 'waxing gibbous']):
        m.tr('gui.ls_aeolus.phase.%d' % i, nm)

    # ---------------------------------------------------------------- codex
    m.codex_intro('Wind & Water makes power from the weather and the sea, with no fuel at all. Each generator depends on where you '
                  'build it: high in open air, in moving water, or out on the ocean.')
    m.codex_page('Wind turbines', 'A wind turbine spins a rotor on its front face. It makes more power the higher it stands above sea '
                                  'level (nothing below the sea, about full output at height 160 and half again at the build limit), '
                                  'and more in rain and much more in thunderstorms. The wind changes strength and direction through the '
                                  'day: a turbine facing into or away from the wind does best, one side-on does a little over half.\n\n'
                                  'The rotor needs room. Every block in the circle it sweeps must be open air, and blocks in the air '
                                  'in front of and behind it slow it down. Two rotors in the same plane must not overlap. Power comes '
                                  'out of the back, sides and bottom.',
                 items=[rl('wind_turbine')])
    m.codex_page('Rotors', 'Rotors wear out as they turn: a wooden rotor lasts half an hour of running, a duralumin rotor eight hours, '
                           'and storms wear them twice as fast. A worn-out rotor breaks, so keep spares (hoppers can feed new rotors '
                           'into the turbine), or repair them on an anvil with their material. Bigger rotors sweep wider circles and '
                           'need more room.', items=[rl(n) for n in ROTORS])
    m.codex_page('Water wheels', 'A water wheel turns when water moves around it. Build it with room to turn: the eight blocks around '
                                 'its hub, across its axle, must be air or water. Water falling down one side or flowing along the '
                                 'bottom pushes it round; water pushing both ways cancels out, and still water only drags. Each block '
                                 'of moving water adds up to about five FE/t. Take power from the ends of the axle.',
                 items=[rl('water_wheel')])
    m.codex_page('Tides', 'The tidal generator only works under water in an ocean, with plenty of open water around it. Two tides come '
                          'and go each day: the water is highest at sunrise and sunset and lowest at noon and midnight. The current, and '
                          'the power, are strongest halfway between, and stop at slack water. Spring tides at full and new moon give a '
                          'quarter more, neap tides at the quarter moons a quarter less. Its screen charts the day ahead.',
                 items=[rl('tidal_generator')])
    m.codex_page('Anemometer', 'The anemometer spins with the wind and its vane points into it. Use it for a reading of the wind at that '
                               'spot, and how hard a turbine there would run. A comparator next to it gives a signal that grows with the '
                               'wind, so you can switch things on in a storm.', items=[rl('anemometer')])
    return m
