from PIL import Image, ImageDraw

from gen.ls import Mod, entity_block, element_model, horizontal_model_state, const, connector_block
from gen import tex

BODY = '#2d333c'      # graphite casing
TRIM = '#1b1f25'
TEAL = '#3fd0c9'      # network accent
TEAL_DIM = '#1f6f6b'

# disk tier: (lang, bytes, types, label colour, tier ingredient)
DISKS = {
    'copper_data_disk': ('Copper Data Disk', 4096, 27, '#d9844f', '#forge:ingots/copper'),
    'electrum_data_disk': ('Electrum Data Disk', 16384, 45, '#ecd97a', '#forge:ingots/electrum'),
    'platinum_data_disk': ('Platinum Data Disk', 65536, 63, '#9fd0de', '#forge:ingots/platinum'),
    'cobalt_data_disk': ('Cobalt Data Disk', 262144, 63, '#3c6fd6', '#forge:ingots/cobalt_steel'),
}


# ----------------------------------------------------------------------------- textures

def casing(seed, part='side'):
    """Graphite casing with a bevelled frame, corner rivets and a thin teal seam."""
    c, t = tex.rgb(BODY), tex.rgb(TRIM)
    img = tex.noise_fill(BODY, seed + part, 0.05)
    px = img.load()
    hl, dk = tex.shade(c, 1.0, 0.12), tex.shade(c, 0.72)
    for i in range(16):
        px[i, 0] = px[0, i] = tex.shade(t, 1.2)
        px[i, 15] = px[15, i] = tex.shade(t, 0.8)
        if 0 < i < 15:
            px[i, 1] = px[1, i] = hl
            px[i, 14] = px[14, i] = dk
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        px[x, y] = tex.shade(c, 1.0, 0.3)
    if part == 'side':
        teal = tex.rgb(TEAL_DIM)
        for x in range(3, 13):
            px[x, 12] = tex.shade(teal, 1.0)
        for y in range(4, 11):
            px[3, y] = px[12, y] = dk
    elif part == 'top':
        for x in range(4, 12):
            for y in (5, 7, 9):
                px[x, y] = dk if x % 2 else tex.shade(c, 0.55)
    return img


def screen_face(seed, lit, pattern):
    """Casing with a recessed screen. pattern: 'grid' (item squares), 'craft' (3x3), 'ring' (core), 'bay' (drive slots)."""
    img = casing(seed, 'front')
    px = img.load()
    glow = tex.rgb(TEAL) if lit else tex.rgb('#3a4550')
    dark = (14, 18, 22, 255) if not lit else (10, 34, 36, 255)
    for x in range(3, 13):
        for y in range(3, 13):
            px[x, y] = dark
    for i in range(3, 13):
        px[i, 2] = px[2, i] = (10, 12, 14, 255)
        px[i, 13] = px[13, i] = tex.shade(tex.rgb(BODY), 1.0, 0.18)
    if pattern == 'grid':
        for gx in range(3):
            for gy in range(3):
                x0, y0 = 4 + gx * 3, 4 + gy * 3
                on = (gx * 7 + gy * 3 + tex.hsh(seed, gx, gy)) % 4 != 0
                for dx in range(2):
                    for dy in range(2):
                        px[x0 + dx, y0 + dy] = tex.shade(glow, 0.95 + 0.1 * dx) if on else tex.shade(glow, 0.4)
    elif pattern == 'craft':
        for gx in range(3):
            for gy in range(3):
                x0, y0 = 4 + gx * 3, 4 + gy * 3
                for dx in range(2):
                    for dy in range(2):
                        px[x0 + dx, y0 + dy] = tex.shade(glow, 0.55)
        for (x, y) in ((7, 4), (7, 5), (8, 4), (8, 5), (4, 7), (5, 7), (4, 8), (5, 8), (7, 7), (8, 8), (7, 8), (8, 7)):
            px[x, y] = tex.shade(glow, 1.15)
    elif pattern == 'ring':
        for x in range(3, 13):
            for y in range(3, 13):
                d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
                if 3.0 <= d < 4.6:
                    px[x, y] = tex.shade(glow, 1.0 - (d - 3.0) * 0.12)
                elif d < 1.6:
                    px[x, y] = tex.shade(glow, 1.3 if lit else 0.8)
    return img


def bay_face(seed, lit):
    img = casing(seed, 'front')
    px = img.load()
    for col in range(2):
        for row in range(4):
            x0, y0 = 2 + col * 6, 2 + row * 3
            for x in range(x0, x0 + 6):
                for y in range(y0, y0 + 2):
                    px[x, y] = (12, 14, 18, 255)
            px[x0 + 5, y0] = tex.shade(tex.rgb(TEAL), 0.9) if lit else (50, 56, 62, 255)
    for x in range(2, 14):
        px[x, 14] = tex.shade(tex.rgb(TEAL_DIM), 1.0 if lit else 0.6)
    return img


def port_face(seed, kind):
    """Front of an importer (arrow inwards), exporter (arrow outwards) or link (aperture)."""
    img = casing(seed, 'front')
    px = img.load()
    col = {'importer': '#4fc36a', 'exporter': '#e08a3c', 'link': '#9b7ae0'}[kind]
    c = tex.rgb(col)
    for x in range(3, 13):
        for y in range(3, 13):
            px[x, y] = (22, 26, 30, 255) if (x + y) % 2 else (30, 35, 40, 255)
    d = ImageDraw.Draw(img)
    if kind == 'importer':
        d.polygon([(8, 11), (4, 7), (6, 7), (6, 4), (9, 4), (9, 7), (11, 7)], fill=tex.shade(c, 1.0))
    elif kind == 'exporter':
        d.polygon([(7, 4), (11, 8), (9, 8), (9, 11), (6, 11), (6, 8), (4, 8)], fill=tex.shade(c, 1.0))
    else:
        d.ellipse([4, 4, 11, 11], outline=tex.shade(c, 1.1))
        d.ellipse([6, 6, 9, 9], fill=tex.shade(c, 0.8))
    for i in range(3, 13):
        px[i, 3] = px[3, i] = tex.shade(c, 0.6)
        px[i, 12] = px[12, i] = tex.shade(c, 0.6)
    return img


def port_back(seed):
    img = casing(seed, 'back')
    px = img.load()
    for x in range(5, 11):
        for y in range(5, 11):
            px[x, y] = (18, 22, 26, 255)
    for x in range(6, 10):
        for y in range(6, 10):
            px[x, y] = tex.shade(tex.rgb(TEAL_DIM), 1.0)
    return img


def fabricator_side(seed, lit):
    img = casing(seed, 'front')
    px = img.load()
    d = ImageDraw.Draw(img)
    d.rectangle([3, 3, 12, 12], fill=(20, 24, 28, 255))
    arm = tex.rgb('#b8c2cc')
    d.line([(4, 11), (7, 6), (11, 5)], fill=tex.shade(arm, 1.0))
    d.line([(4, 12), (7, 7), (11, 6)], fill=tex.shade(arm, 0.7))
    px[11, 4] = px[12, 5] = tex.shade(tex.rgb(TEAL), 1.2 if lit else 0.5)
    for x in range(4, 12):
        px[x, 12] = tex.shade(tex.rgb('#f2b33c' if lit else '#5a5040'), 1.0)
    return img


def encoder_top(seed):
    img = casing(seed, 'top')
    px = img.load()
    card = tex.rgb('#c8d0d8')
    for x in range(3, 13):
        for y in range(3, 13):
            px[x, y] = tex.shade(card, 0.95 + 0.04 * ((x + y) % 2))
    for gx in range(3):
        for gy in range(3):
            for dx in range(2):
                for dy in range(2):
                    px[4 + gx * 3 + dx, 4 + gy * 3 + dy] = tex.shade(tex.rgb(TEAL_DIM), 1.0)
    for i in range(3, 13):
        px[i, 3] = px[3, i] = tex.shade(card, 0.7)
    return img


def cable_body(seed):
    img = tex.noise_fill('#262a31', seed, 0.06)
    px = img.load()
    teal = tex.rgb(TEAL)
    for x in range(16):
        px[x, 0] = px[x, 15] = (14, 16, 20, 255)
        for y in range(6, 10):
            px[x, y] = tex.shade(teal, 0.85 + 0.1 * (y in (7, 8)))
        if x % 4 == 0:
            for y in range(1, 15):
                px[x, y] = tex.shade(tex.rgb('#262a31'), 0.7)
    return img


def cable_end(seed):
    img = tex.noise_fill('#3a414b', seed + 'e', 0.06)
    px = img.load()
    for x in range(16):
        for y in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if d > 6:
                px[x, y] = (20, 23, 28, 255)
            elif d < 3:
                px[x, y] = tex.shade(tex.rgb(TEAL), 1.0 if d < 2 else 0.7)
    return img


def disk_item(label):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = tex.rgb(BODY)
    d.rectangle([2, 2, 13, 13], fill=tex.shade(c, 1.1), outline=(14, 16, 20, 255))
    d.rectangle([3, 3, 12, 3], fill=tex.shade(c, 1.4))
    if label:
        lc = tex.rgb(label)
        d.rectangle([4, 5, 11, 9], fill=tex.shade(lc, 1.0), outline=tex.shade(lc, 0.6))
        d.line([(5, 6), (10, 6)], fill=tex.shade(lc, 1.3))
    else:
        d.rectangle([4, 5, 11, 9], fill=(20, 24, 28, 255))
    d.rectangle([5, 11, 10, 12], fill=(160, 168, 176, 255))
    img.putpixel((12, 12), tex.shade(tex.rgb(TEAL), 1.0))
    return img


def card_item(encoded):
    img = tex.item('card', '#c8d0d8')
    if encoded:
        px = img.load()
        for (x, y) in ((6, 6), (8, 6), (10, 6), (6, 8), (8, 8), (6, 10)):
            if px[x, y][3]:
                px[x, y] = tex.shade(tex.rgb(TEAL_DIM), 1.0)
                if px[x + 1, y][3]:
                    px[x + 1, y] = tex.shade(tex.rgb(TEAL), 1.0)
    return img


def six_state(m, name, model):
    """Blockstate for FACING_ALL blocks whose model faces north."""
    rot = {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}
    return {'variants': {'facing=%s' % d: dict({'model': '%s:block/%s' % (m.modid, model)}, **r) for d, r in rot.items()}}


def lit_state(m, name):
    return {'variants': {'lit=false': {'model': '%s:block/%s' % (m.modid, name)}, 'lit=true': {'model': '%s:block/%s_on' % (m.modid, name)}}}


# ----------------------------------------------------------------------------- spec

def build():
    m = Mod('ls_datastore', 'Datastore', 'A digital storage network. Data cables join a core, drive bays full of data disks and '
                                         'searchable terminals into one store; importers, exporters and inventory links move items in '
                                         'and out, and fabricators run recipe cards to craft whole trees of recipes on request.',
            tab='LOGISTICS')
    m.init('DatastoreSetup.init(bus)')
    m.client('com.lodestar.datastore.client.DatastoreClient.init(bus)')
    rl = m.rl
    t = lambda n: '%s:block/%s' % (m.modid, n)
    sd = m.modid

    for part in ('side', 'top'):
        m.texture('block/casing_' + part, casing(sd, part))

    # --- cable
    connector_block(m, 'data_cable', 'new CableBlock(LSProps.machineTransparent().strength(0.5f), () -> Content.DATA_CABLE_BE.get())',
                    'CableBlock', 3, cable_body(sd + 'cable'), cable_end(sd + 'cable'),
                    desc='Joins datastore devices into one network\nCarries no power cost')
    m.field('public static final RegistryObject<BlockEntityType<CableBE>> DATA_CABLE_BE = REG.blockEntity("data_cable", '
            '(pos, state) -> new CableBE(Content.DATA_CABLE_BE.get(), pos, state), Content.DATA_CABLE);')
    m.shaped('data_cable', rl('data_cable'), ['SSS', 'WQW', 'SSS'],
             {'S': 'minecraft:string', 'W': '#forge:wires/copper', 'Q': 'minecraft:quartz'}, count=8)

    # --- components
    m.item('logic_chip', ('circuit', BODY, None, TEAL), desc='The brains of every datastore device')
    m.shaped('logic_chip', rl('logic_chip'), ['RQR', 'GPG', 'RQR'],
             {'R': 'minecraft:redstone', 'Q': 'minecraft:quartz', 'G': 'minecraft:gold_nugget', 'P': '#forge:plates/silver'}, count=4)
    m.item('disk_housing', disk_item(None), desc='An empty disk shell')
    m.shaped('disk_housing', rl('disk_housing'), ['PGP', 'G G', 'PRP'],
             {'P': '#forge:plates/steel', 'G': '#forge:glass_panes', 'R': 'minecraft:redstone'}, count=2)
    for name, (lang, nbytes, types, col, ing) in DISKS.items():
        m.item(name, disk_item(col), java='new DiskItem(new Item.Properties().stacksTo(1), %d, %d)' % (nbytes, types), jtype='DiskItem',
               lang=lang, desc='Put it in a drive bay')
    m.shaped('copper_data_disk', rl('copper_data_disk'), ['MCM', 'CHC', 'MRM'],
             {'M': '#forge:ingots/copper', 'C': rl('logic_chip'), 'H': rl('disk_housing'), 'R': 'minecraft:redstone'})
    m.shaped('electrum_data_disk', rl('electrum_data_disk'), ['MCM', 'CHC', 'MQM'],
             {'M': '#forge:ingots/electrum', 'C': rl('logic_chip'), 'H': rl('copper_data_disk'), 'Q': 'minecraft:quartz_block'})
    m.shaped('platinum_data_disk', rl('platinum_data_disk'), ['MCM', 'CHC', 'MDM'],
             {'M': '#forge:ingots/platinum', 'C': rl('logic_chip'), 'H': rl('electrum_data_disk'), 'D': 'minecraft:diamond'})
    m.shaped('cobalt_data_disk', rl('cobalt_data_disk'), ['MCM', 'CHC', 'MEM'],
             {'M': '#forge:ingots/cobalt_steel', 'C': rl('logic_chip'), 'H': rl('platinum_data_disk'), 'E': 'minecraft:ender_eye'})
    m.item('blank_recipe_card', card_item(False), desc='Write a crafting recipe on it in a pattern encoder')
    m.shaped('blank_recipe_card', rl('blank_recipe_card'), ['PPP', 'PCP', 'PPP'], {'P': 'minecraft:paper', 'C': rl('logic_chip')}, count=8)
    m.item('recipe_card', card_item(True), java='new RecipeCardItem(new Item.Properties().stacksTo(16))', jtype='RecipeCardItem',
           tab='NONE', desc='Put it in a fabricator so the network can craft this')
    m.shapeless('recipe_card_reset', rl('blank_recipe_card'), [rl('recipe_card')])
    m.item('remote_terminal', ('tablet', BODY, None, TEAL), java='new RemoteTerminalItem(new Item.Properties().stacksTo(1))',
           jtype='RemoteTerminalItem', desc='Sneak-use it on a datastore core to link it\nThen use it anywhere within 64 blocks')
    m.shaped('remote_terminal', rl('remote_terminal'), [' E ', 'CTC', 'PBP'],
             {'E': 'minecraft:ender_pearl', 'C': rl('logic_chip'), 'T': rl('access_terminal'), 'P': '#forge:plates/aluminum',
              'B': 'minecraft:redstone_block'})

    # --- core
    side_off, side_on = screen_face(sd + 'core', False, 'ring'), screen_face(sd + 'core', True, 'ring')
    m.texture('block/core_side', side_off)
    m.texture('block/core_side_on', side_on)
    cm = lambda s: {'parent': 'minecraft:block/cube_bottom_top', 'textures': {'side': t(s), 'top': t('casing_top'), 'bottom': t('casing_top')}}
    entity_block(m, 'datastore_core', 'CoreBE', 'new LSEntityBlock(LSProps.machine().lightLevel(s -> s.getValue(LSEntityBlock.LIT) ? 7 : 0), BE_TYPE, LSEntityBlock.Facing.NONE, true).noBuckets()',
                 models={'datastore_core': cm('core_side'), 'datastore_core_on': cm('core_side_on')}, state=lit_state(m, 'datastore_core'),
                 desc='Powers a datastore network: one per network\nEvery device costs a little energy each tick', level='stone')
    m.shaped('datastore_core', rl('datastore_core'), ['PCP', 'CFC', 'PRP'],
             {'P': '#forge:plates/steel', 'C': rl('logic_chip'), 'F': 'ls_machina:machine_frame', 'R': 'ls_machina:redstone_coil'})

    def front_device(name, front_off, front_on, desc, recipe, java_extra='', ctor='', be='DeviceBE'):
        m.texture('block/%s_front' % name, front_off)
        m.texture('block/%s_front_on' % name, front_on)
        mk = lambda fr: {'parent': 'minecraft:block/orientable', 'textures': {'top': t('casing_top'), 'side': t('casing_side'), 'front': t(fr)}}
        entity_block(m, name, be, 'new LSEntityBlock(LSProps.machine(), BE_TYPE, LSEntityBlock.Facing.HORIZONTAL, true).noBuckets()' + java_extra,
                     models={name: mk(name + '_front'), name + '_on': mk(name + '_front_on')},
                     state=horizontal_model_state(m, name, name + '_on'), desc=desc, ctor_args=ctor, level='stone')
        m.shaped(name, rl(name), recipe[0], recipe[1])

    front_device('drive_bay', bay_face(sd + 'bay', False), bay_face(sd + 'bay', True),
                 'Holds eight data disks and adds them to the network', (['PCP', 'H H', 'PCP'],
                 {'P': '#forge:plates/steel', 'C': rl('logic_chip'), 'H': 'minecraft:chest'}), be='DriveBayBE')
    front_device('access_terminal', screen_face(sd + 'term', False, 'grid'), screen_face(sd + 'term', True, 'grid'),
                 'Search, sort, store and take everything in the network\nCtrl-click an item to craft it', (['GGG', 'CFC', 'PRP'],
                 {'G': '#forge:glass_panes', 'C': rl('logic_chip'), 'F': 'ls_machina:machine_frame', 'P': '#forge:plates/aluminum',
                  'R': 'minecraft:redstone'}), ctor='false', be='TerminalBE')
    front_device('crafting_terminal', screen_face(sd + 'cterm', False, 'craft'), screen_face(sd + 'cterm', True, 'craft'),
                 'An access terminal with a crafting grid\nthat refills itself from the network', (['T', 'W'],
                 {'T': rl('access_terminal'), 'W': 'minecraft:crafting_table'}), ctor='true', be='TerminalBE')

    # --- fabricator (crafts recipe cards)
    m.texture('block/fabricator_side', fabricator_side(sd + 'fab', False))
    m.texture('block/fabricator_side_on', fabricator_side(sd + 'fab', True))
    m.texture('block/fabricator_top', screen_face(sd + 'fabt', False, 'craft'))
    m.texture('block/fabricator_top_on', screen_face(sd + 'fabt', True, 'craft'))
    fm = lambda s, tp: {'parent': 'minecraft:block/cube_bottom_top', 'textures': {'side': t(s), 'top': t(tp), 'bottom': t('casing_top')}}
    entity_block(m, 'fabricator', 'FabricatorBE', 'new LSEntityBlock(LSProps.machine(), BE_TYPE, LSEntityBlock.Facing.NONE, true).noBuckets()',
                 models={'fabricator': fm('fabricator_side', 'fabricator_top'), 'fabricator_on': fm('fabricator_side_on', 'fabricator_top_on')},
                 state=lit_state(m, 'fabricator'), level='stone',
                 desc='Holds nine recipe cards and crafts them for the network\nSpeed augments make it faster')
    m.shaped('fabricator', rl('fabricator'), ['PCP', 'WFW', 'PCP'],
             {'P': '#forge:plates/steel', 'C': rl('logic_chip'), 'W': 'minecraft:crafting_table', 'F': 'ls_machina:machine_frame'})

    # --- pattern encoder (standalone)
    m.texture('block/pattern_encoder_top', encoder_top(sd + 'enc'))
    em = {'parent': 'minecraft:block/cube_bottom_top', 'textures': {'side': t('casing_side'), 'top': t('pattern_encoder_top'), 'bottom': t('casing_top')}}
    entity_block(m, 'pattern_encoder', 'EncoderBE', 'new LSEntityBlock(LSProps.machine(), BE_TYPE).noBuckets()',
                 models={'pattern_encoder': em}, desc='Writes crafting recipes onto blank recipe cards', level='stone')
    m.shaped('pattern_encoder', rl('pattern_encoder'), ['CCC', 'PWP', 'PPP'],
             {'C': rl('blank_recipe_card'), 'P': '#forge:plates/iron', 'W': 'minecraft:crafting_table'})

    # --- ports: importer, exporter, inventory link (face the inventory they work on)
    back = port_back(sd + 'pb')
    m.texture('block/port_back', back)
    ports = {'importer': ('PortBE', 'ImporterBE', 'Pulls items from the inventory it faces into the network\nFilter and speed augments in its menu'),
             'exporter': ('PortBE', 'ExporterBE', 'Pushes filtered items from the network into the inventory it faces\nSpeed augments in its menu'),
             'inventory_link': ('PortBE', 'LinkBE', 'Adds the inventory it faces to the network storage\nSet a priority and a filter in its menu')}
    for name, (_, cls, desc) in ports.items():
        kind = 'link' if name == 'inventory_link' else name
        m.texture('block/%s_front' % name, port_face(sd + name, kind))
        model = {'parent': 'minecraft:block/cube', 'textures': {'north': t(name + '_front'), 'south': t('port_back'), 'east': t('casing_side'),
                                                                 'west': t('casing_side'), 'up': t('casing_side'), 'down': t('casing_side'),
                                                                 'particle': t('casing_side')}}
        entity_block(m, name, cls, 'new PortBlock(LSProps.machine(), BE_TYPE)', jtype='PortBlock', models={name: model},
                     state=six_state(m, name, name), desc=desc, level='stone')
    m.shaped('importer', rl('importer'), [' C ', 'PHP'], {'C': rl('logic_chip'), 'P': '#forge:plates/steel', 'H': 'minecraft:hopper'})
    m.shaped('exporter', rl('exporter'), [' C ', 'PDP'], {'C': rl('logic_chip'), 'P': '#forge:plates/steel', 'D': 'minecraft:dropper'})
    m.shaped('inventory_link', rl('inventory_link'), [' C ', 'PXP'], {'C': rl('logic_chip'), 'P': '#forge:plates/steel', 'X': 'minecraft:chest'})

    # --- lang
    for k, v in {
        'gui.ls_datastore.core': 'Datastore Core', 'gui.ls_datastore.energy': 'Energy: %s / %s FE',
        'gui.ls_datastore.upkeep': 'Upkeep: %s FE/t', 'gui.ls_datastore.devices': 'Devices: %s', 'gui.ls_datastore.disks': 'Disks: %s',
        'gui.ls_datastore.bytes': 'Bytes: %s / %s', 'gui.ls_datastore.types': 'Types: %s / %s', 'gui.ls_datastore.jobs': 'Crafting jobs: %s',
        'gui.ls_datastore.status.0': 'Online', 'gui.ls_datastore.status.1': 'No core on this network',
        'gui.ls_datastore.status.2': 'More than one core on this network', 'gui.ls_datastore.status.3': 'Out of power',
        'gui.ls_datastore.status.4': 'Out of range', 'gui.ls_datastore.search': 'Search (@mod, #tag)',
        'gui.ls_datastore.sort.0': 'Sort by name', 'gui.ls_datastore.sort.1': 'Sort by count', 'gui.ls_datastore.sort.2': 'Sort by mod',
        'gui.ls_datastore.dir.0': 'Ascending', 'gui.ls_datastore.dir.1': 'Descending', 'gui.ls_datastore.show_jobs': 'Crafting jobs',
        'gui.ls_datastore.clear': 'Return the grid to the network', 'gui.ls_datastore.stored': 'Stored: %s',
        'gui.ls_datastore.craftable': 'Craftable: Ctrl-click or middle-click', 'gui.ls_datastore.craft': 'Craft',
        'gui.ls_datastore.amount': 'How many?', 'gui.ls_datastore.plan': 'Plan', 'gui.ls_datastore.start': 'Start',
        'gui.ls_datastore.back': 'Back', 'gui.ls_datastore.cancel': 'Cancel', 'gui.ls_datastore.planning': 'Planning...',
        'gui.ls_datastore.plan_title': 'Crafting %s x %s', 'gui.ls_datastore.plan_ok': '%s crafting steps, ready to start',
        'gui.ls_datastore.plan_missing': 'Missing items - cannot start', 'gui.ls_datastore.col.item': 'Item',
        'gui.ls_datastore.col.used': 'Stored', 'gui.ls_datastore.col.craft': 'Crafted', 'gui.ls_datastore.col.missing': 'Missing',
        'gui.ls_datastore.no_jobs': 'No crafting jobs', 'gui.ls_datastore.job': '%s x %s', 'gui.ls_datastore.job_progress': '%s / %s crafts',
        'gui.ls_datastore.job.0': 'Gathering items', 'gui.ls_datastore.job.1': 'Crafting', 'gui.ls_datastore.job.2': 'Waiting for a fabricator',
        'gui.ls_datastore.job.3': 'Storing results', 'gui.ls_datastore.priority': 'Priority: %s', 'gui.ls_datastore.priority.tip': 'Higher priority storage fills first',
        'gui.ls_datastore.allow': 'Allow', 'gui.ls_datastore.deny': 'Block', 'gui.ls_datastore.filter_mode': 'Allow only these, or block these',
        'gui.ls_datastore.substitute': 'Substitutes: %s', 'gui.ls_datastore.on': 'On', 'gui.ls_datastore.off': 'Off',
        'gui.ls_datastore.substitute.tip': 'Let the network use any item the recipe accepts in each slot',
        'gui.ls_datastore.encode': 'Encode', 'gui.ls_datastore.no_recipe': 'No crafting recipe', 'gui.ls_datastore.working': 'Crafting: %s',
        'gui.ls_datastore.idle': 'Idle', 'gui.ls_datastore.cards': 'Recipe cards', 'gui.ls_datastore.augments': 'Augments',
        'tooltip.ls_datastore.disk.bytes': 'Bytes: %s / %s', 'tooltip.ls_datastore.disk.types': 'Types: %s / %s',
        'tooltip.ls_datastore.disk.empty': 'Empty', 'tooltip.ls_datastore.card.makes': 'Makes %s x %s',
        'tooltip.ls_datastore.card.from': '  %s x %s', 'tooltip.ls_datastore.card.sub': 'Accepts substitutes',
        'tooltip.ls_datastore.remote.linked': 'Linked to the core at %s, %s, %s', 'tooltip.ls_datastore.remote.unlinked': 'Not linked',
        'msg.ls_datastore.linked': 'Remote terminal linked', 'msg.ls_datastore.unlinked': 'Not linked to a core: sneak-use it on one',
        'msg.ls_datastore.no_core': 'The linked core is gone', 'msg.ls_datastore.range': 'Too far from the linked core',
        'msg.ls_datastore.job_started': 'Crafting %s x %s', 'msg.ls_datastore.job_done': 'Finished crafting %s x %s',
        'lens.category.ls_datastore.disks': 'Data Disks', 'lens.ls_datastore.disk_info': '%s bytes, %s types',
        'lens.ls_datastore.disk_cost': '%s bytes per type',
    }.items():
        m.tr(k, v)

    m.codex_intro('Datastore turns a room full of chests into one searchable store. Every device joins the network through data cables; '
                  'a single datastore core powers it, and every device on the network costs the core a little energy each tick.')
    m.codex_page('The core', 'Each network needs exactly one datastore core. Feed it Forge Energy from any side (a fluxduct or a dynamo '
                             'next to it). Its upkeep is two FE per tick plus the cost of every device: drive bays cost two plus one per '
                             'disk, terminals two or three, fabricators two (twelve while crafting). When it runs dry the network goes '
                             'dark until power returns. Use the core to see its energy, devices and how full the disks are.',
                 items=[rl('datastore_core'), rl('data_cable'), rl('logic_chip')])
    m.codex_page('Disks and drive bays', 'Data disks hold items by bytes and by types: each item stored costs one byte, and each kind of item '
                                         'costs extra bytes and one of the disk\'s type slots. Copper disks hold 4,096 bytes; electrum, platinum '
                                         'and cobalt disks four times more each, built from the tier below. Put up to eight disks in a drive '
                                         'bay; the lights show how full each one is. Disk contents live in the world save, not on the item, so '
                                         'full disks never bloat your inventory or the network.',
                 items=[rl('drive_bay'), rl('disk_housing')] + [rl(n) for n in DISKS])
    m.codex_page('Terminals', 'The access terminal shows everything stored. Type to search (start with @ for a mod, # for a tag), sort by '
                              'name, count or mod, and scroll. Click to take a stack, right-click for half, shift-click to send it to your '
                              'inventory; click the grid with an item to store it. The crafting terminal adds a crafting grid that refills '
                              'itself from the network. A remote terminal, linked to a core by sneak-using it there, opens the network '
                              'from up to 64 blocks away.',
                 items=[rl('access_terminal'), rl('crafting_terminal'), rl('remote_terminal')])
    m.codex_page('Moving items', 'Importers pull items out of the inventory they face, exporters push the items in their filter into it, '
                                 'and inventory links make a chest or any other inventory part of the network storage. All three are '
                                 'placed against the inventory. Set a filter by clicking items into the ghost slots; speed augments make '
                                 'importers and exporters move more, and a redstone signal pauses them. Higher-priority links and drive '
                                 'bays fill first.',
                 items=[rl('importer'), rl('exporter'), rl('inventory_link')])
    m.codex_page('Autocrafting', 'Lay out a crafting recipe in the pattern encoder and press Encode to write it onto a blank recipe card. '
                                 'With substitutes on, the network may use any item the recipe accepts (any planks, say). Put cards in a '
                                 'fabricator on the network. In a terminal, ctrl-click or middle-click a craftable item, choose how many, '
                                 'and the network plans the whole tree of crafts: it shows what it will take from storage, what it will '
                                 'craft along the way and what is missing. Start the job and fabricators do the rest; the results go into '
                                 'storage.',
                 items=[rl('pattern_encoder'), rl('blank_recipe_card'), rl('fabricator')])
    return m
