from PIL import Image, ImageDraw

from gen.ls import Mod, title, entity_block, element_model, custom_machine, const
from gen import tex

BITS = {
    # name: (enchant, level, colour, energy multiplier, lang)
    'fortune_drill_bit_1': ('FORTUNE', 1, '#3b5fc0', 1.5, 'Fortune Drill Bit I'),
    'fortune_drill_bit_2': ('FORTUNE', 2, '#2f7fe0', 2.0, 'Fortune Drill Bit II'),
    'fortune_drill_bit_3': ('FORTUNE', 3, '#22b8f0', 3.0, 'Fortune Drill Bit III'),
    'silk_drill_bit': ('SILK', 1, '#3ec27a', 2.0, 'Silk Touch Drill Bit'),
}

FRAME = '#d9a92c'


def lattice(color, seed, holes=True):
    """Steel lattice: thick border and a diagonal cross brace, see-through elsewhere."""
    c = tex.rgb(color)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            edge = x < 2 or y < 2 or x > 13 or y > 13
            diag = abs(x - y) <= 1 or abs(x - (15 - y)) <= 1
            n = 0.9 + tex.rnd(seed, x, y) * 0.2
            if edge:
                k = 1.15 if (x < 1 or y < 1) else (0.7 if (x > 14 or y > 14) else 1.0)
                px[x, y] = tex.shade(c, k * n)
            elif diag:
                px[x, y] = tex.shade(c, 0.85 * n)
            elif not holes:
                px[x, y] = tex.shade(c, 0.45)
    for (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)):
        px[x, y] = (60, 60, 64, 255)
    return img


def hazard(seed):
    img = tex.noise_fill('#2b2b2e', seed, 0.06)
    px = img.load()
    y0 = tex.rgb('#e0b020')
    for y in range(16):
        for x in range(16):
            if ((x + y) // 4) % 2 == 0 and (y < 4 or y > 11):
                px[x, y] = tex.shade(y0, 0.95 + tex.rnd(seed, x, y) * 0.1)
    for x in range(16):
        px[x, 4] = px[x, 11] = (30, 30, 32, 255)
    return img


def drill_tex(seed):
    """Spiral drill steel for the bit model."""
    img = tex.noise_fill('#5a6068', seed, 0.06)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if (x + y * 2) % 6 < 2:
                px[x, y] = (150, 158, 168, 255)
            elif (x + y * 2) % 6 == 5:
                px[x, y] = (40, 44, 50, 255)
    return img


def bit_item(color, seed):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = tex.rgb(color)
    steel = (150, 158, 168, 255)
    dark = (60, 64, 72, 255)
    # shank (top right) and fluted cone pointing down-left
    d.rectangle([10, 1, 13, 5], fill=tex.shade(c, 1.0), outline=tex.shade(c, 0.6))
    d.line([11, 2, 12, 2], fill=tex.shade(c, 1.4))
    pts = [(12, 6), (9, 4), (2, 13), (3, 14), (5, 13)]
    d.polygon([(13, 6), (9, 3), (1, 14), (2, 15)], fill=steel, outline=dark)
    for i in range(4):
        x, y = 10 - i * 2, 5 + i * 2
        d.line([x, y, x + 2, y + 1], fill=dark)
        d.point((x + 1, y - 1), fill=(220, 226, 232, 255))
    d.point((1, 14), fill=tex.shade(c, 1.3))
    return img


def sieve_item(color, seed, kind):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rim = tex.rgb('#8a6a3a')
    d.ellipse([1, 1, 14, 14], fill=tex.shade(rim, 0.9), outline=tex.shade(rim, 0.55))
    d.ellipse([3, 3, 12, 12], fill=(40, 40, 44, 255))
    c = tex.rgb(color)
    px = img.load()
    for y in range(3, 13):
        for x in range(3, 13):
            if (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 20:
                if kind == 'fine' and (x % 2 == 0 or y % 2 == 0):
                    px[x, y] = tex.shade(c, 0.9 + 0.1 * ((x + y) % 2))
                elif kind == 'coarse' and (x % 3 == 0 or y % 3 == 0):
                    px[x, y] = tex.shade(c, 0.9)
    if kind == 'fine':
        d.rectangle([6, 6, 9, 9], fill=(232, 180, 90, 255))
        px[7, 7] = (255, 230, 160, 255)
    else:
        d.rectangle([6, 6, 9, 9], fill=(128, 128, 132, 255))
    return img


def marker_item(seed):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([7, 6, 8, 15], fill=(120, 96, 64, 255))
    d.line([7, 6, 7, 15], fill=(150, 122, 84, 255))
    d.rectangle([5, 1, 10, 6], fill=(40, 70, 150, 255), outline=(20, 30, 80, 255))
    d.rectangle([6, 2, 9, 4], fill=(110, 180, 255, 255))
    d.point((7, 2), fill=(230, 250, 255, 255))
    return img


def marker_lamp(seed):
    img = tex.noise_fill('#3a66c8', seed, 0.05)
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 4:
                px[x, y] = (150 + int(20 * (4 - d)), 210 + int(10 * (4 - d)), 255, 255)
    return img


def build():
    m = Mod('ls_quarry', 'Quarry', 'Dig whole areas down to bedrock. Mark out a rectangle with survey markers and a powered quarry '
                                    'builds a steel frame around it and runs a drill gantry across it layer by layer; a laser quarry on '
                                    'a tower of masts cuts a wide square without any frame, much faster. Drill bits add fortune or '
                                    'silk touch, sieves keep only ores or throw away rubble, and land restoration fills the hole back '
                                    'in with its own spoil and topsoil.', tab='INDUSTRY')
    m.init('QuarrySetup.init(bus)')
    m.client('com.lodestar.quarry.client.QuarryClient.init(bus)')
    rl = m.rl
    t = lambda n: '%s:block/%s' % (m.modid, n)

    # ----------------------------------------------------------------- textures shared by blocks and the gantry models
    m.texture('block/quarry_frame', lattice(FRAME, m.modid + 'frame'))
    m.texture('block/laser_mast', lattice('#8e98a4', m.modid + 'mast'))
    m.texture('block/laser_mast_end', lattice('#8e98a4', m.modid + 'mastend', holes=False))
    m.texture('block/gantry_hazard', hazard(m.modid + 'hz'))
    m.texture('block/drill_steel', drill_tex(m.modid + 'drill'))
    m.texture('block/survey_marker_lamp', marker_lamp(m.modid + 'lamp'))
    m.texture('block/drill_head', tex.casing('advanced', m.modid + 'head', 'side'))

    # ----------------------------------------------------------------- survey markers
    post = {'texture': '#post'}
    marker_model = element_model([
        ([7, 0, 7], [9, 10, 9], {'north': post, 'south': post, 'east': post, 'west': post, 'down': post}),
        ([6, 10, 6], [10, 14, 10], '#lamp')], {'post': 'minecraft:block/stripped_spruce_log', 'lamp': t('survey_marker_lamp'),
                                               'particle': t('survey_marker_lamp')}, render='cutout')
    entity_block(m, 'survey_marker', 'MarkerBE',
                 'new LSEntityBlock(LSProps.machineTransparent().strength(0.3f).noCollission().lightLevel(s -> 7), BE_TYPE).noMenu().noBuckets()'
                 '.shape(Block.box(6, 0, 6, 10, 14, 10))',
                 tab='INDUSTRY', models={'survey_marker': marker_model}, item_model='generated',
                 desc='Place three in an L (up to 64 blocks apart) to mark\nthe frame of a quarry. Use one to link them.\n'
                      'Set the quarry against a marker outside the rectangle', mine='pickaxe')
    m.texture('item/survey_marker', marker_item(m.modid + 'mk'))
    m.shaped('survey_marker', rl('survey_marker'), ['L', 'T', 'S'], {'L': 'minecraft:lapis_lazuli', 'T': 'minecraft:redstone_torch',
                                                                     'S': 'minecraft:stick'}, count=2)

    # ----------------------------------------------------------------- frame, mast
    m.block('quarry_frame', java='new Block(LSProps.machineTransparent().strength(1.5f, 6.0f))',
            models={'quarry_frame': {'parent': 'minecraft:block/cube_all', 'render_type': 'minecraft:cutout',
                                     'textures': {'all': t('quarry_frame')}}}, loot=None, tab='INDUSTRY',
            desc='Built by the quarry around its area.\nDrops nothing when broken; the quarry rebuilds it')
    m.block('laser_mast', java='new Block(LSProps.machineTransparent().strength(3.0f, 8.0f))',
            models={'laser_mast': {'parent': 'minecraft:block/cube_column', 'render_type': 'minecraft:cutout',
                                   'textures': {'side': t('laser_mast'), 'end': t('laser_mast_end')}}}, tab='INDUSTRY', level='stone',
            desc='Stack masts under a laser quarry.\nEvery mast lets it reach two blocks further')
    m.shaped('laser_mast', rl('laser_mast'), ['R R', 'RPR', 'R R'], {'R': '#forge:rods/steel', 'P': '#forge:plates/steel'}, count=4)

    # ----------------------------------------------------------------- machines
    custom_machine(m, 'quarry', 'QuarryBE', tier='advanced', glyph='pick',
                   desc='Builds a frame around the marked area (or a 9x9 square\nbehind it) and drills it out layer by layer.\nNeeds power; pushes what it digs into inventories it touches')
    custom_machine(m, 'laser_quarry', 'LaserQuarryBE', tier='elite', glyph='laser',
                   desc='Sits on a tower of laser masts and cuts a square\naround the tower with no frame. Faster but hungrier')
    m.shaped('quarry', rl('quarry'), ['GPG', 'DFD', 'GCG'], {'G': '#forge:gears/steel', 'P': 'minecraft:diamond_pickaxe',
                                                            'D': '#forge:plates/invar', 'F': 'ls_machina:machine_frame',
                                                            'C': 'ls_machina:redstone_coil'})
    m.shaped('laser_quarry', rl('laser_quarry'), ['RDR', 'EQE', 'ACA'], {'R': 'minecraft:redstone_block', 'D': 'minecraft:diamond',
                                                                        'E': '#forge:plates/electrum', 'Q': rl('quarry'),
                                                                        'A': '#forge:plates/aluminum', 'C': 'ls_machina:redstone_coil'})

    # ----------------------------------------------------------------- drill bits and sieves
    for name, (ench, lvl, color, mult, lang) in BITS.items():
        m.item(name, bit_item(color, m.modid + name), java='new DrillBitItem(new Item.Properties().stacksTo(1), DrillBitItem.Kind.%s, %d, %sf)'
               % (ench, lvl, mult), jtype='DrillBitItem', lang=lang)
    m.shaped('fortune_drill_bit_1', rl('fortune_drill_bit_1'), [' L ', 'LDL', ' G '], {'L': 'minecraft:lapis_block', 'D': 'minecraft:diamond',
                                                                                       'G': '#forge:gears/gold'})
    m.shaped('fortune_drill_bit_2', rl('fortune_drill_bit_2'), ['LEL', 'EBE', 'LEL'], {'L': 'minecraft:lapis_block', 'E': 'minecraft:emerald',
                                                                                       'B': rl('fortune_drill_bit_1')})
    m.shaped('fortune_drill_bit_3', rl('fortune_drill_bit_3'), ['PDP', 'DBD', 'PDP'], {'P': '#forge:plates/platinum', 'D': 'minecraft:diamond_block',
                                                                                       'B': rl('fortune_drill_bit_2')})
    m.shaped('silk_drill_bit', rl('silk_drill_bit'), ['WEW', 'EDE', 'WGW'], {'W': 'minecraft:cobweb', 'E': 'minecraft:emerald',
                                                                             'D': 'minecraft:diamond', 'G': '#forge:gears/gold'})
    m.item('ore_sieve', sieve_item('#c0c6cc', m.modid + 'os', 'fine'), java='new SieveItem(new Item.Properties().stacksTo(1), SieveItem.Mode.ORES_ONLY)',
           jtype='SieveItem')
    m.item('rubble_screen', sieve_item('#8c8c90', m.modid + 'rs', 'coarse'),
           java='new SieveItem(new Item.Properties().stacksTo(1), SieveItem.Mode.VOID_RUBBLE)', jtype='SieveItem')
    m.shaped('ore_sieve', rl('ore_sieve'), ['BGB', 'GPG', 'BGB'], {'B': 'minecraft:iron_bars', 'G': '#forge:ingots/gold', 'P': '#forge:plates/silver'})
    m.shaped('rubble_screen', rl('rubble_screen'), ['PBP', 'BGB', 'PBP'], {'B': 'minecraft:iron_bars', 'G': 'minecraft:gravel', 'P': '#forge:plates/iron'})

    # ----------------------------------------------------------------- gantry and laser models drawn by the renderers
    hz = {'texture': '#hazard'}
    m.asset('models/block/gantry_rail.json', element_model([([0, 4, 5], [16, 12, 11], {
        'north': hz, 'south': hz, 'up': {'texture': '#frame'}, 'down': {'texture': '#frame'}, 'east': {'texture': '#frame'},
        'west': {'texture': '#frame'}})], {'hazard': t('gantry_hazard'), 'frame': t('quarry_frame'), 'particle': t('quarry_frame')}, render='cutout'))
    side = {'texture': '#side'}
    m.asset('models/block/drill_head.json', element_model([
        ([1, 2, 1], [15, 14, 15], {'north': side, 'south': side, 'east': side, 'west': side, 'up': {'texture': '#top'}, 'down': {'texture': '#top'}}),
        ([5, 0, 5], [11, 2, 11], '#steel')], {'side': t('drill_head'), 'top': t('gantry_hazard'), 'steel': t('drill_steel'),
                                               'particle': t('drill_head')}))
    m.asset('models/block/drill_shaft.json', element_model([([6, 0, 6], [10, 16, 10], '#steel')],
                                                           {'steel': t('drill_steel'), 'particle': t('drill_steel')}))
    m.asset('models/block/drill_bit.json', element_model([([4, 8, 4], [12, 16, 12], '#steel'), ([5.5, 3, 5.5], [10.5, 8, 10.5], '#steel'),
                                                          ([7, 0, 7], [9, 3, 9], '#steel')], {'steel': t('drill_steel'), 'particle': t('drill_steel')}))

    # ----------------------------------------------------------------- tags
    for b in ('#minecraft:base_stone_overworld', '#minecraft:base_stone_nether', 'minecraft:cobblestone', 'minecraft:cobbled_deepslate',
              'minecraft:calcite', 'minecraft:gravel', 'minecraft:dripstone_block', 'minecraft:end_stone', 'minecraft:smooth_basalt',
              'minecraft:mossy_cobblestone', 'minecraft:sandstone', 'minecraft:red_sandstone', 'minecraft:terracotta'):
        m.tag('blocks', 'ls_quarry:rubble', b)
    for b in ('#minecraft:dirt', '#minecraft:sand', 'minecraft:clay', 'minecraft:soul_sand', 'minecraft:soul_soil', 'minecraft:farmland',
              'minecraft:dirt_path', 'minecraft:snow_block', 'minecraft:mycelium'):
        m.tag('blocks', 'ls_quarry:soil', b)
    for b in ('#forge:ores', 'minecraft:ancient_debris', 'minecraft:amethyst_cluster', 'minecraft:budding_amethyst', 'minecraft:gilded_blackstone'):
        m.tag('blocks', 'ls_quarry:sieve_keeps', b)
    for b in (rl('quarry_frame'), rl('laser_mast'), rl('survey_marker'), rl('quarry'), rl('laser_quarry'), 'minecraft:spawner',
              'minecraft:end_portal_frame', 'minecraft:reinforced_deepslate'):
        m.tag('blocks', 'ls_quarry:immune', b)
    for i in ('#forge:cobblestone', '#forge:stone', 'minecraft:dirt', 'minecraft:coarse_dirt', 'minecraft:gravel', 'minecraft:sand',
              'minecraft:red_sand', 'minecraft:netherrack', 'minecraft:tuff', 'minecraft:cobbled_deepslate', 'minecraft:deepslate',
              'minecraft:granite', 'minecraft:diorite', 'minecraft:andesite', 'minecraft:calcite', 'minecraft:rooted_dirt', 'minecraft:mud'):
        m.tag('items', 'ls_quarry:fill_material', i)

    # ----------------------------------------------------------------- language
    for k, v in {
        'gui.ls_quarry.phase.no_area': 'Waiting: no area', 'gui.ls_quarry.phase.building': 'Building frame',
        'gui.ls_quarry.phase.mining': 'Mining', 'gui.ls_quarry.phase.restoring': 'Restoring land',
        'gui.ls_quarry.phase.done': 'Finished', 'gui.ls_quarry.phase.blocked': 'Blocked',
        'gui.ls_quarry.layer': 'Layer Y %s  (%s%%)', 'gui.ls_quarry.mined': '%s blocks, %s FE each',
        'gui.ls_quarry.spoil': 'Spoil: %s rubble, %s soil', 'gui.ls_quarry.restore_layer': 'Refilling Y %s',
        'gui.ls_quarry.needs_fill': 'Needs fill material', 'gui.ls_quarry.no_power': 'No power',
        'gui.ls_quarry.full': 'Output full', 'gui.ls_quarry.tower': 'Tower: %s masts, radius %s/%s',
        'gui.ls_quarry.no_tower': 'Needs 2+ laser masts below', 'gui.ls_quarry.size': 'Area %sx%s',
        'gui.ls_quarry.restore.on': 'Restore: On', 'gui.ls_quarry.restore.off': 'Restore: Off',
        'gui.ls_quarry.show.on': 'Area: Shown', 'gui.ls_quarry.show.off': 'Area: Hidden',
        'gui.ls_quarry.overflow.drop': 'Overflow: Drop', 'gui.ls_quarry.overflow.hold': 'Overflow: Hold',
        'gui.ls_quarry.reset': 'Restart', 'gui.ls_quarry.minus': '-', 'gui.ls_quarry.plus': '+',
        'gui.ls_quarry.slot.bit': 'Drill bit', 'gui.ls_quarry.slot.sieve': 'Sieve', 'gui.ls_quarry.slot.fill': 'Fill material for restoring',
        'msg.ls_quarry.linked': 'Markers linked: %s x %s frame', 'msg.ls_quarry.partial': 'Found a marker along one axis only; place one along the other',
        'msg.ls_quarry.none': 'No other marker in line with this one (within 64 blocks)',
        'msg.ls_quarry.too_small': 'Markers must be at least 2 blocks apart',
        'tooltip.ls_quarry.bit.fortune': 'Mines as if with Fortune %s', 'tooltip.ls_quarry.bit.silk': 'Mines blocks whole, as with Silk Touch',
        'tooltip.ls_quarry.bit.energy': 'Energy per block: x%s', 'tooltip.ls_quarry.bit.hint': 'Goes in the drill bit slot of a quarry',
        'tooltip.ls_quarry.sieve.ores_only': 'Keeps only what ores drop;\neverything else is thrown away',
        'tooltip.ls_quarry.sieve.void_rubble': 'Throws away stone, dirt, sand and\nother rubble; keeps everything else',
        'tooltip.ls_quarry.sieve.hint': 'Goes in the sieve slot of a quarry',
        'lens.category.ls_quarry.drill_bits': 'Quarry Drill Bits', 'lens.category.ls_quarry.sieves': 'Quarry Sieves',
        'lens.category.ls_quarry.restoration': 'Land Restoration', 'lens.ls_quarry.kept': 'Kept', 'lens.ls_quarry.voided': 'Thrown away',
        'lens.ls_quarry.energy': 'Energy x%s', 'lens.ls_quarry.fill': 'Fills the hole: spoil first, then these',
        'lens.ls_quarry.spoil': 'Rubble and soil dug while Restore is on are kept as spoil',
    }.items():
        m.tr(k, v)

    m.codex_intro('Quarries dig out a whole area, layer by layer, down to bedrock. They need energy (FE) and push what they dig into '
                  'any chest, duct or machine they touch; the nine slots in the middle of the screen hold the rest.')
    m.codex_page('Marking the area', 'Place survey markers at three corners of the rectangle you want, in an L shape on the same level, '
                                     'up to 64 blocks apart, then use one of them. Linked markers draw beams along the edges; hold a '
                                     'wrench to see the whole column down to bedrock. The markers stand where the frame will go: the '
                                     'quarry digs the inside.\n\nPlace the quarry right next to one of the markers, outside the '
                                     'rectangle. Without markers a quarry digs a 9x9 square behind itself; a range augment widens '
                                     'that to 17, 25 or 33.', items=[rl('survey_marker')])
    m.codex_page('The frame quarry', 'Powered up, the quarry builds a steel frame around the area, clearing anything in the way, then '
                                     'a drill gantry rides along the top of the frame and bores out one block at a time. Break a frame '
                                     'block and it is put back. Speed augments drill faster, efficiency augments use less energy per '
                                     'block and capacity augments enlarge the buffer. Redstone control pauses it.',
                 items=[rl('quarry'), rl('quarry_frame')])
    m.codex_page('The laser quarry', 'The laser quarry needs no frame. Stand it on a tower of at least two laser masts: it cuts a square '
                                     'around the tower, reaching two blocks per mast (up to 24 with range augments), and never digs the '
                                     'column under itself. Set the radius on its screen before it starts. Lasers work about twice as '
                                     'fast as the drill but use more energy per block.', items=[rl('laser_quarry'), rl('laser_mast')])
    m.codex_page('Drill bits and sieves', 'A drill bit in the bit slot makes the quarry dig as if with an enchanted pickaxe: Fortune I to '
                                          'III, or Silk Touch. Better bits cost more energy per block.\n\nAn ore sieve in the sieve slot '
                                          'keeps only what ores drop and throws the rest away. A rubble screen throws away stone, dirt, '
                                          'sand and gravel and keeps everything else.',
                 items=[rl('fortune_drill_bit_1'), rl('fortune_drill_bit_3'), rl('silk_drill_bit'), rl('ore_sieve'), rl('rubble_screen')])
    m.codex_page('Land restoration', 'Turn Restore on before you start and the quarry keeps the rubble and soil it digs as spoil instead '
                                     'of handing it out. When it reaches the bottom it fills the hole back in, layer by layer: deepslate '
                                     'and stone underneath, dirt under the old surface and grass on top where there was grass. If the '
                                     'spoil runs short it uses cobblestone, dirt, gravel or sand from its fill slots. The frame is taken '
                                     'down at the end.', items=[rl('quarry'), 'minecraft:grass_block'])
    return m
