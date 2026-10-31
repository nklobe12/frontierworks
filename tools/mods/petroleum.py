from PIL import Image, ImageDraw

from gen.ls import Mod, title, const, entity_block, element_model, stairs_slab_wall, recipe_kind, processing_machine, custom_machine, \
    machine, fluid, cross_block
from gen import tex

# fluid: (colour, gas, density, viscosity, temperature, lang)
FLUIDS = {
    'crude_oil': ('#1c1712', False, 900, 4000, 300, 'Crude Oil'),
    'heavy_oil': ('#3d2a16', False, 980, 6000, 300, 'Heavy Oil'),
    'diesel': ('#c8901e', False, 850, 1500, 300, 'Diesel'),
    'kerosene': ('#9fc3d6', False, 800, 1100, 300, 'Kerosene'),
    'naphtha': ('#e3d36e', False, 740, 900, 300, 'Naphtha'),
    'gasoline': ('#d8663a', False, 720, 800, 300, 'Gasoline'),
    'refinery_gas': ('#b9c7a4', True, 0, 0, 300, 'Refinery Gas'),
}
# distillation fractions, bottom tray first: (fluid, mB per 100 mB of crude, band colour)
FRACTIONS = [('heavy_oil', 30, '#5a3a1c'), ('diesel', 25, '#c8901e'), ('kerosene', 20, '#7fb2cc'), ('naphtha', 15, '#e3d36e'),
             ('refinery_gas', 10, '#c9d6b4')]


def tar_sand(seed):
    img = tex.noise_fill('#8a7350', seed, 0.14)
    px = img.load()
    for y in range(16):
        for x in range(16):
            n = tex.rnd(seed + 'tar', x // 2, y // 2) * 0.6 + tex.rnd(seed + 't2', x, y) * 0.4
            if n > 0.52:
                f = 0.28 + (1 - n) * 0.5
                r, g, b, a = px[x, y]
                px[x, y] = (tex.clamp(r * f), tex.clamp(g * f * 0.95), tex.clamp(b * f * 0.85), 255)
            elif tex.rnd(seed + 'gl', x, y) < 0.03:
                px[x, y] = (60, 52, 44, 255)
    return img


def asphalt(seed):
    img = tex.noise_fill('#34363a', seed, 0.10)
    px = img.load()
    for y in range(16):
        for x in range(16):
            n = tex.rnd(seed + 'ag', x, y)
            if n < 0.07:
                px[x, y] = (96, 96, 98, 255)
            elif n < 0.11:
                px[x, y] = (22, 22, 24, 255)
    return img


def girder(seed):
    """Painted steel deck of the pumpjack base."""
    img = tex.noise_fill('#5e6670', seed, 0.06)
    px = img.load()
    yel, blk = (214, 168, 42, 255), (36, 36, 40, 255)
    for i in range(16):
        for j in (0, 15):
            px[i, j] = yel if ((i + j) // 3) % 2 == 0 else blk
            px[j, i] = yel if ((i + j) // 3) % 2 == 0 else blk
    for i in range(2, 14):
        px[i, 2] = (120, 128, 138, 255)
        px[2, i] = (120, 128, 138, 255)
        px[i, 13] = (60, 64, 70, 255)
        px[13, i] = (60, 64, 70, 255)
    for (x, y) in ((4, 4), (11, 4), (4, 11), (11, 11)):
        px[x, y] = (40, 42, 46, 255)
        px[x + 1, y] = (150, 156, 164, 255)
    for i in range(4, 12):
        px[i, 7] = (72, 78, 86, 255)
        px[i, 8] = (100, 106, 114, 255)
    return img


def steel(seed, base='#4b525b', bolts=True):
    img = tex.noise_fill(base, seed, 0.05)
    px = img.load()
    c = tex.rgb(base)
    for i in range(16):
        px[i, 0] = tex.shade(c, 1.0, 0.12)
        px[0, i] = tex.shade(c, 1.0, 0.12)
        px[i, 15] = tex.shade(c, 0.6)
        px[15, i] = tex.shade(c, 0.6)
    if bolts:
        for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
            px[x, y] = tex.shade(c, 1.0, 0.25)
    return img


def beam_tex(seed):
    img = tex.noise_fill('#3f4a57', seed, 0.05)
    px = img.load()
    for x in range(16):
        px[x, 0] = (88, 100, 114, 255)
        px[x, 15] = (28, 32, 38, 255)
        px[x, 5] = (30, 34, 40, 255)
        px[x, 10] = (30, 34, 40, 255)
    for x in range(0, 16, 4):
        px[x, 7] = (150, 160, 170, 255)
        px[x, 8] = (110, 118, 128, 255)
    return img


def head_tex(seed):
    img = tex.noise_fill('#c9a033', seed, 0.06)
    px = img.load()
    for i in range(16):
        px[i, 0] = (234, 196, 80, 255)
        px[i, 15] = (120, 92, 24, 255)
        px[0, i] = (234, 196, 80, 255)
        px[15, i] = (120, 92, 24, 255)
    for y in range(3, 13, 3):
        for x in range(3, 13):
            px[x, y] = (150, 116, 34, 255)
    return img


def tray_side(band, seed, outlet):
    img = tex.noise_fill('#6f7780', seed, 0.05)
    px = img.load()
    for x in range(16):
        px[x, 0] = (150, 158, 166, 255)
        px[x, 1] = (52, 56, 62, 255)
        px[x, 14] = (52, 56, 62, 255)
        px[x, 15] = (150, 158, 166, 255)
    for x in range(16):
        px[0, x] = (96, 104, 112, 255)
        px[15, x] = (60, 64, 70, 255)
    if band:
        c = tex.rgb(band)
        for x in range(1, 15):
            px[x, 3] = tex.shade(c, 1.15)
            px[x, 4] = tex.shade(c, 0.9)
    else:
        for x in range(1, 15):
            px[x, 3] = (84, 90, 96, 255)
            px[x, 4] = (70, 76, 82, 255)
    if outlet:
        d = ImageDraw.Draw(img)
        d.ellipse([4, 6, 11, 13], fill=(70, 76, 84, 255), outline=(40, 44, 50, 255))
        d.ellipse([6, 8, 9, 11], fill=(18, 18, 20, 255))
    return img


def tray_top(seed):
    img = tex.noise_fill('#5b636c', seed, 0.05)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(150, 158, 166, 255))
    d.ellipse([3, 3, 12, 12], outline=(40, 44, 50, 255), fill=(78, 84, 92, 255))
    for i in range(4, 12, 2):
        d.point((i, 7), fill=(30, 30, 34, 255))
        d.point((7, i), fill=(30, 30, 34, 255))
    return img


def geophone_tex():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # spike
    d.line([(4, 15), (5, 10)], fill=(150, 156, 164, 255))
    d.point((4, 15), fill=(200, 205, 210, 255))
    # sensor body
    d.ellipse([2, 5, 9, 11], fill=(196, 152, 40, 255), outline=(110, 80, 20, 255))
    d.ellipse([4, 7, 7, 9], fill=(60, 52, 40, 255))
    # cable to earpiece
    pts = [(8, 7), (10, 5), (12, 6), (13, 9), (12, 11)]
    d.line(pts, fill=(30, 30, 34, 255))
    d.rectangle([11, 1, 14, 4], fill=(48, 50, 56, 255), outline=(22, 22, 24, 255))
    d.point((12, 2), fill=(200, 60, 40, 255))
    return img


def wellhead_model(m):
    t = lambda n: '%s:block/%s' % (m.modid, n)
    return element_model([
        ([2, 0, 2], [14, 3, 14], '#steel'), ([5, 3, 5], [11, 12, 11], '#steel'), ([4, 6, 4], [12, 8, 12], '#steel'),
        ([3, 9, 7], [5, 11, 9], '#valve'), ([11, 9, 7], [13, 11, 9], '#valve'), ([6, 12, 6], [10, 14, 10], '#valve')],
        {'steel': t('wellhead'), 'valve': t('wellhead_valve'), 'particle': t('wellhead')}, render='cutout')


def post_model(m):
    t = lambda n: '%s:block/%s' % (m.modid, n)
    return element_model([([4, 0, 4], [12, 16, 12], '#post'), ([3, 0, 3], [13, 2, 13], '#post')],
                         {'post': t('pumpjack_post'), 'particle': t('pumpjack_post')})


def build():
    m = Mod('ls_petroleum', 'Petroleum', 'Oil lies in reservoirs under some chunks; a geophone tells you where, and oil seeps and tar pits '
                                         'on the surface give it away. Pump it with a pumpjack, split it into heavy oil, diesel, kerosene, '
                                         'naphtha and gas in a distillation column as tall as the fractions you want, crack and reform the '
                                         'heavy and light ends, and turn the gas into plastics and synthetic rubber. Bitumen makes asphalt '
                                         'roads you walk faster on.', tab='MATERIALS')
    m.init('PetroleumSetup.init(bus)')
    m.client('com.lodestar.petroleum.client.PetroleumClient.init(bus)')
    rl = m.rl
    t = lambda n: '%s:block/%s' % (m.modid, n)

    # ----------------------------------------------------------------- fluids
    for name, (color, gas, dens, visc, temp, lang) in FLUIDS.items():
        fluid(m, name, color, lang=lang, gas=gas, density=dens, viscosity=visc, temperature=temp, bucket=not gas)

    # ----------------------------------------------------------------- materials
    m.item('bitumen', ('ball', '#2a2018'), tags=['forge:bitumen', 'forge:tar'], desc='Sticky residue of heavy oil and tar sand.\nBinds gravel into asphalt.')
    m.item('petroleum_coke', ('raw', '#2e2f33'), java='new FuelItem(new Item.Properties(), 2400)', jtype='FuelItem', tags=['forge:coal_coke'],
           desc='Burns like coal coke and works wherever coke does')
    m.item('plastic_pellets', ('pellet', '#e8e4da'), tags=['forge:pellets/plastic'], desc='Polymerized refinery gas. Melt them into sheets.')
    m.item('plastic_sheet', ('sheet', '#ece8de'), tags=['forge:plastic', 'forge:sheets/plastic', 'forge:plates/plastic'])
    m.item('synthetic_rubber', ('plate', '#2b2b2e'), tags=['forge:rubber'], desc='Vulcanized from naphtha and sulfur')
    m.item('paraffin_wax', ('bar', '#efe8cf', None, '#ddd3b0'), tags=['forge:wax'], desc='Makes candles, like beeswax')
    m.item('geophone', geophone_tex(), java='new GeophoneItem(new Item.Properties().durability(200))', jtype='GeophoneItem', tab='TOOLS',
           desc='Press it into the ground to listen for oil\nunder this chunk and the ones around it')

    m.block('tar_sand', java='new Block(LSProps.sand(MapColor.TERRACOTTA_BROWN).strength(0.8f))',
            textures={'block/tar_sand': tar_sand(m.modid + 'ts')}, mine='shovel', tab='MATERIALS', tags=['minecraft:sand'],
            desc='Sand soaked with bitumen. Smelt or spin it for bitumen.')
    asphalt_props = 'LSProps.stone(1.8f).speedFactor(1.25f)'
    m.block('asphalt', props=asphalt_props, textures={'block/asphalt': asphalt(m.modid + 'as')}, tab='BUILDING',
            desc='You walk and ride a quarter faster on it')
    stairs_slab_wall(m, 'asphalt', t('asphalt'), tab='BUILDING', wall=False, props=asphalt_props)
    m.block('plexiglass', java='new GlassBlock(LSProps.glass().strength(1.5f, 12.0f))', jtype='GlassBlock', tab='BUILDING',
            models={'plexiglass': {'parent': 'minecraft:block/cube_all', 'render_type': 'minecraft:translucent',
                                   'textures': {'all': t('plexiglass')}}},
            textures={'block/plexiglass': tex.glass('#cfe3ea', m.modid + 'pg', '#d8f0f8', 'clear')}, desc='Clear plastic: as tough as stone')

    # ----------------------------------------------------------------- pumpjack
    custom_machine(m, 'pumpjack', 'PumpjackBE', tier='steam', glyph='drop', tab='INDUSTRY',
                   desc='Pumpjack motor: build the pumpjack frame in front of it.\nPumps crude oil from the reservoir under the wellhead.')
    m.block('pumpjack_base', props='LSProps.metal(4.0f)', textures={'block/pumpjack_base': girder(m.modid + 'pb')}, tab='INDUSTRY',
            desc='Base frame of a pumpjack')
    m.block('pumpjack_post', java='new LSBlocks.FlammableBlock(LSProps.metal(4.0f).noOcclusion(), 0, 0)', jtype='LSBlocks.FlammableBlock',
            tab='INDUSTRY', models={'pumpjack_post': post_model(m)}, textures={'block/pumpjack_post': steel(m.modid + 'pp', '#58616b')},
            desc='Samson post: carries the walking beam')
    entity_block(m, 'wellhead', 'WellheadBE', 'new LSEntityBlock(LSProps.metal(4.0f).noOcclusion(), BE_TYPE).noMenu().shape(Block.box(2, 0, 2, 14, 14, 14))',
                 tab='INDUSTRY', models={'wellhead': wellhead_model(m)},
                 textures={'block/wellhead': steel(m.modid + 'wh', '#4a5058'), 'block/wellhead_valve': tex.noise_fill('#b8352a', m.modid + 'whv', 0.08)},
                 desc='Caps the well at the front of a pumpjack.\nPipes can draw crude oil from it.', level='stone')
    for n, img in {'pumpjack_beam': beam_tex(m.modid + 'beam'), 'pumpjack_head': head_tex(m.modid + 'head'),
                   'pumpjack_weight': steel(m.modid + 'cw', '#7a2e24')}.items():
        m.texture('block/' + n, img)
    # renderer-only models (baked through RenderUtil.extraModel)
    m.asset('models/block/pumpjack_beam_segment.json', element_model([([5, 5, 0], [11, 10, 16], '#beam')], {'beam': t('pumpjack_beam'), 'particle': t('pumpjack_beam')}))
    m.asset('models/block/pumpjack_horsehead.json', element_model([
        ([4, 2, 0], [12, 14, 3], '#head'), ([4, 0, 3], [12, 16, 6], '#head'), ([4, 1, 6], [12, 15, 8], '#head')],
        {'head': t('pumpjack_head'), 'particle': t('pumpjack_head')}))
    m.asset('models/block/pumpjack_crank.json', element_model([
        ([2, 6, 6], [4, 10, 10], '#beam'), ([12, 6, 6], [14, 10, 10], '#beam'), ([1, 2, 5], [3, 14, 11], '#weight'), ([13, 2, 5], [15, 14, 11], '#weight')],
        {'beam': t('pumpjack_beam'), 'weight': t('pumpjack_weight'), 'particle': t('pumpjack_weight')}))
    m.asset('models/block/pumpjack_saddle.json', element_model([([4, 0, 5], [12, 4, 11], '#beam')], {'beam': t('pumpjack_beam'), 'particle': t('pumpjack_beam')}))

    # ----------------------------------------------------------------- distillation column
    custom_machine(m, 'distillation_column', 'DistillationBE', tier='advanced', glyph='beaker', tab='INDUSTRY',
                   desc='Reboiler at the foot of a distillation column.\nStack 2 to 5 distillation trays on top of it:\neach tray draws off one fraction, so taller columns\nseparate more of the crude.')
    tray_models = {}
    state = {'variants': {}}
    for i in range(6):
        band = FRACTIONS[i - 1][2] if i > 0 else None
        m.texture('block/distillation_tray_%d' % i, tray_side(band, m.modid + 'tray%d' % i, i > 0))
        tray_models['distillation_tray_%d' % i] = {'parent': 'minecraft:block/cube_column', 'textures': {
            'side': t('distillation_tray_%d' % i), 'end': t('distillation_tray_top')}}
        state['variants']['fraction=%d' % i] = {'model': t('distillation_tray_%d' % i)}
    m.texture('block/distillation_tray_top', tray_top(m.modid + 'traytop'))
    entity_block(m, 'distillation_tray', 'TrayBE', 'new TrayBlock(LSProps.metal(4.0f), BE_TYPE)', jtype='TrayBlock', tab='INDUSTRY',
                 models=tray_models, state=state, item_model='block:distillation_tray_0', level='stone',
                 desc='One level of a distillation column.\nDraw that level\'s fraction off its sides with pipes or buckets.')

    # ----------------------------------------------------------------- cracker and polymerizer
    cracking = recipe_kind(m, 'cracking')
    polymerizing = recipe_kind(m, 'polymerizing')
    m.field('public static final MachineSpec CRACKER_SPEC = new MachineSpec(PetroleumSetup.CRACKER_LAYOUT, RecipeSource.machine(() -> %s), 30000, 400, 1.0f, 1.0f);' % cracking)
    machine(m, 'cracker', 'new ProcessingMachineBE(BE_TYPE, pos, state, Content.CRACKER_SPEC)', tier='advanced', glyph='spin', tab='INDUSTRY',
            desc='Cracks heavy oil into diesel with steam,\nreforms naphtha into gasoline')
    processing_machine(m, 'polymerizer', polymerizing, inputs=1, outputs=1, tanks_in=1, tier='advanced', glyph='grid',
                       desc='Polymerizes refinery gas into plastic\nand naphtha with sulfur into rubber')

    # ----------------------------------------------------------------- recipes
    crk = lambda n, fin, fin2, fout, fout2, items=(), t_=100, e=40: m.mrecipe('ls_petroleum:cracking', n, [], list(items), time=t_, energy=e,
                                                                              fluid_in=fin, fluid_in2=fin2, fluid_out=fout, fluid_out2=fout2)
    crk('heavy_oil_steam', (rl('heavy_oil'), 200), ('minecraft:water', 200), (rl('diesel'), 100), (rl('refinery_gas'), 60),
        [(rl('petroleum_coke'), 1, 0.5)], 120, 50)
    crk('naphtha_reforming', (rl('naphtha'), 100), None, (rl('gasoline'), 80), (rl('refinery_gas'), 20), (), 80, 40)
    crk('heavy_oil_dewaxing', (rl('heavy_oil'), 250), (rl('naphtha'), 50), None, None, [(rl('bitumen'), 2), (rl('paraffin_wax'), 1)], 100, 30)
    crk('light_oil', ('ls_machina:light_oil', 100), None, (rl('naphtha'), 60), (rl('refinery_gas'), 40), (), 80, 30)
    crk('kerosene_cracking', (rl('kerosene'), 100), ('minecraft:water', 50), (rl('naphtha'), 70), (rl('refinery_gas'), 30), (), 80, 40)
    m.mrecipe('ls_petroleum:polymerizing', 'plastic', [], [(rl('plastic_pellets'), 2)], time=100, energy=40, fluid_in=(rl('refinery_gas'), 250))
    m.mrecipe('ls_petroleum:polymerizing', 'synthetic_rubber', [('#forge:dusts/sulfur', 1)], [(rl('synthetic_rubber'), 2)], time=160, energy=40,
              fluid_in=(rl('naphtha'), 200))
    m.mrecipe('ls_petroleum:polymerizing', 'plexiglass', [('#forge:glass', 2)], [(rl('plexiglass'), 2)], time=120, energy=40,
              fluid_in=(rl('refinery_gas'), 100))
    # vanilla-machine and Machina recipes
    m.smelt('plastic_sheet', rl('plastic_pellets'), rl('plastic_sheet'), 0.1, time=100, blast=False)
    m.smelt('bitumen_from_tar_sand', rl('tar_sand'), rl('bitumen'), 0.1, time=200, blast=False)
    m.mrecipe('ls_machina:centrifuging', 'ls_petroleum_tar_sand', [(rl('tar_sand'), 2)], [('minecraft:sand', 2), (rl('bitumen'), 2), (rl('bitumen'), 1, 0.5)],
              time=160, energy=30, fluid_out=(rl('heavy_oil'), 100))
    m.mrecipe('ls_machina:pressing', 'ls_petroleum_plastic_sheet', [(rl('plastic_pellets'), 1), ('ls_machina:plate_mold', 0)],
              [(rl('plastic_sheet'), 2)], time=60, energy=20)
    m.mrecipe('ls_machina:refining', 'ls_petroleum_crude_oil', [], [(rl('bitumen'), 1, 0.25)], time=100, energy=40,
              fluid_in=(rl('crude_oil'), 100), fluid_out=('ls_machina:light_oil', 50))
    m.shaped('asphalt', rl('asphalt'), ['GSG', 'SBS', 'GSG'], {'G': 'minecraft:gravel', 'S': '#minecraft:sand', 'B': '#forge:bitumen'}, count=8)
    m.shapeless('candle_from_paraffin', 'minecraft:candle', ['minecraft:string', rl('paraffin_wax')], count=2)
    m.shaped('plexiglass', rl('plexiglass'), [' P ', 'PGP', ' P '], {'P': rl('plastic_sheet'), 'G': '#forge:glass'}, count=2)

    m.shaped('geophone', rl('geophone'), [' C ', 'NIN', ' R '], {'C': 'minecraft:copper_ingot', 'N': '#forge:nuggets/iron', 'I': 'minecraft:amethyst_shard',
                                                                'R': '#forge:rods/iron'})
    m.shaped('pumpjack', rl('pumpjack'), ['GPG', 'MFM', 'GRG'], {'G': '#forge:gears/steel', 'P': 'minecraft:piston', 'M': '#forge:plates/steel',
                                                                'F': 'ls_machina:machine_frame', 'R': 'ls_machina:redstone_coil'})
    m.shaped('pumpjack_base', rl('pumpjack_base'), ['PRP', 'R R', 'PRP'], {'P': '#forge:plates/iron', 'R': '#forge:rods/iron'}, count=4)
    m.shaped('pumpjack_post', rl('pumpjack_post'), ['R', 'P', 'R'], {'R': '#forge:rods/steel', 'P': '#forge:plates/steel'}, count=2)
    m.shaped('wellhead', rl('wellhead'), ['VPV', ' B ', ' P '], {'V': '#forge:ingots/copper', 'P': '#forge:plates/steel', 'B': 'minecraft:bucket'})
    m.shaped('distillation_column', rl('distillation_column'), ['PGP', 'HFH', 'PCP'], {'P': '#forge:plates/steel', 'G': '#forge:glass',
                                                                                       'H': 'ls_machina:heating_coil', 'F': 'ls_machina:machine_frame',
                                                                                       'C': 'ls_machina:redstone_coil'})
    m.shaped('distillation_tray', rl('distillation_tray'), ['PBP', 'G G', 'PBP'], {'P': '#forge:plates/steel', 'B': 'minecraft:iron_bars',
                                                                                  'G': '#forge:glass'}, count=2)
    m.shaped('cracker', rl('cracker'), ['PHP', 'TFT', 'GCG'], {'P': '#forge:plates/invar', 'H': 'ls_machina:heating_coil', 'T': rl('distillation_tray'),
                                                              'F': 'ls_machina:machine_frame', 'G': '#forge:gears/steel', 'C': 'ls_machina:redstone_coil'})
    m.shaped('polymerizer', rl('polymerizer'), ['PBP', 'GFG', 'ICI'], {'P': '#forge:plates/aluminum', 'B': 'minecraft:bucket', 'G': '#forge:glass',
                                                                      'F': 'ls_machina:machine_frame', 'I': '#forge:gears/invar', 'C': 'ls_machina:redstone_coil'})

    # ----------------------------------------------------------------- worldgen: oil seeps over rich reservoirs, tar pits in dry lands
    crude = {'Name': rl('crude_oil'), 'Properties': {'level': '0'}}
    for name, size, rarity, extra, biomes in (('tar_pit', 4, 6, 0.0, m.biome_tag('has_tar_pits', ['#minecraft:is_badlands', 'minecraft:desert',
                                                                                                     '#minecraft:is_savanna'])),
                                               ('oil_seep', 2, 10, 0.1, m.biome_tag('has_oil_seeps', ['#minecraft:is_overworld']))):
        m.configured(name, {'type': 'ls_petroleum:oil_seep', 'config': {'fluid': crude, 'stain': {'Name': rl('tar_sand')}, 'size': size, 'margin': extra}})
        m.placed(name, rl(name), [{'type': 'minecraft:rarity_filter', 'chance': rarity}, {'type': 'minecraft:in_square'},
                                  {'type': 'minecraft:heightmap', 'heightmap': 'WORLD_SURFACE_WG'}, {'type': 'minecraft:biome'}])
        m.add_feature(rl(name), biomes, 'lakes')

    # ----------------------------------------------------------------- lang
    for k, v in {
        'lens.category.ls_petroleum.cracking': 'Cracker', 'lens.category.ls_petroleum.polymerizing': 'Polymerizer',
        'lens.category.ls_petroleum.distillation': 'Distillation Column', 'lens.category.ls_petroleum.pumpjack': 'Pumpjack',
        'lens.ls_petroleum.trays': '%s trays', 'lens.ls_petroleum.flared': 'The rest is flared at the top',
        'lens.ls_petroleum.reservoir': 'Oil reservoir under the wellhead', 'lens.ls_petroleum.stroke': '%s mB per stroke, %s FE/t',
        'lens.ls_petroleum.survey': 'Find reservoirs with a geophone',
        'gui.ls_petroleum.not_formed': 'Frame incomplete', 'gui.ls_petroleum.no_oil': 'No oil under the wellhead',
        'gui.ls_petroleum.depleted': 'Reservoir depleted', 'gui.ls_petroleum.full': 'Tank full',
        'gui.ls_petroleum.no_power': 'Needs power', 'gui.ls_petroleum.pumping': 'Pumping',
        'gui.ls_petroleum.idle': 'Idle', 'gui.ls_petroleum.reservoir': 'Reservoir: %s B',
        'gui.ls_petroleum.trays': 'Trays: %s', 'gui.ls_petroleum.need_trays': 'Stack 2-5 trays on top',
        'gui.ls_petroleum.missing': 'Check the block at %s %s %s',
        'msg.ls_petroleum.geophone.none': 'Silence: no oil under this chunk.',
        'msg.ls_petroleum.geophone.here': 'Oil below: %s reservoir, about %s buckets left.',
        'msg.ls_petroleum.geophone.depleted': 'This chunk\'s reservoir has been pumped dry.',
        'msg.ls_petroleum.geophone.best': 'The strongest echo comes from %s chunks %s (%s).',
        'msg.ls_petroleum.geophone.nothing_near': 'No other reservoirs within %s chunks.',
        'msg.ls_petroleum.geophone.wrong_dim': 'The ground here carries no echo of oil.',
        'oil.ls_petroleum.small': 'a small', 'oil.ls_petroleum.fair': 'a fair', 'oil.ls_petroleum.rich': 'a rich',
        'oil.ls_petroleum.vast': 'a vast',
        'dir.ls_petroleum.north': 'north', 'dir.ls_petroleum.northeast': 'northeast', 'dir.ls_petroleum.east': 'east',
        'dir.ls_petroleum.southeast': 'southeast', 'dir.ls_petroleum.south': 'south', 'dir.ls_petroleum.southwest': 'southwest',
        'dir.ls_petroleum.west': 'west', 'dir.ls_petroleum.northwest': 'northwest',
    }.items():
        m.tr(k, v)
    for i, (fl, amt, band) in enumerate(FRACTIONS):
        m.tr('gui.ls_petroleum.tray%d' % (i + 1), 'Tray %d: %s' % (i + 1, FLUIDS[fl][5]))

    # ----------------------------------------------------------------- codex
    m.codex_intro('Oil is the next step after coal and lava: dense fuels for compression dynamos, and the raw stuff of plastics, rubber '
                  'and asphalt. It lies in reservoirs under about one chunk in five, in broad fields that span many chunks.')
    m.codex_page('Finding oil', 'Press a geophone into the ground and listen. It tells you how much oil lies under the chunk you stand in '
                                'and where the strongest echo nearby comes from. Oil seeps, small black pools ringed with tar sand, only '
                                'appear above rich fields; larger tar pits form in deserts, badlands and savannas. Each chunk\'s reservoir '
                                'is finite: once it is pumped dry, move on.', items=[rl('geophone'), rl('tar_sand'), rl('crude_oil_bucket')])
    m.codex_page('The pumpjack', 'Place the pumpjack motor, then build its frame in front of it: a 3 wide, 4 long deck of pumpjack bases '
                                 'with the motor in the middle of the back row, a samson post two blocks high in the middle of the deck, '
                                 'and a wellhead one block in front of the deck. Keep the space above the deck clear for the walking beam. '
                                 'Powered, it nods slowly and pumps 200 mB of crude oil a stroke from the reservoir under the wellhead. '
                                 'Speed augments make it stroke faster.',
                 items=[rl('pumpjack'), rl('pumpjack_base'), rl('pumpjack_post'), rl('wellhead')], multiblock='ls_petroleum:pumpjack')
    m.codex_page('Distillation', 'The distillation column boils crude oil at its foot and draws off one fraction per tray above it. Stack '
                                 'two to five trays: the lowest gives heavy oil, then diesel, kerosene, naphtha and, at the top, refinery '
                                 'gas. A short column flares whatever its missing trays would have caught, so build it tall. Pipe each '
                                 'fraction from the side of its tray, or use a bucket on it.\n\n100 mB of crude gives 30 heavy oil, 25 '
                                 'diesel, 20 kerosene, 15 naphtha and 10 refinery gas.',
                 items=[rl('distillation_column'), rl('distillation_tray')], multiblock='ls_petroleum:distillation')
    m.codex_page('Fuels', 'Diesel, gasoline, kerosene and naphtha all burn in the compression dynamo; diesel gives the most energy per '
                          'bucket, and crude and heavy oil burn poorly. The dynamo recognises fuels by their common fluid tags '
                          '(forge:diesel, forge:kerosene, forge:crude_oil...), so other mods\' fuels work too. The cracker turns heavy oil and water into diesel and gas (with petroleum coke), dewaxes heavy oil '
                          'with a little naphtha as solvent into bitumen and paraffin, and reforms naphtha into gasoline.',
                 items=[rl('cracker'), rl('diesel_bucket'), rl('gasoline_bucket'), rl('kerosene_bucket'), rl('petroleum_coke')])
    m.codex_page('Plastics and rubber', 'The polymerizer turns refinery gas into plastic pellets; melt them into sheets in a furnace or '
                                        'press them in a compactor. Naphtha and sulfur dust make synthetic rubber, and gas with glass makes '
                                        'plexiglass. Paraffin wax and string make candles.',
                 items=[rl('polymerizer'), rl('plastic_pellets'), rl('plastic_sheet'), rl('synthetic_rubber'), rl('plexiglass'), rl('paraffin_wax')])
    m.codex_page('Asphalt', 'Bitumen from tar sand or heavy oil binds sand and gravel into asphalt. Players, mobs and horses move a '
                            'quarter faster on asphalt, its slabs and its stairs.', items=[rl('asphalt'), rl('bitumen')])
    return m
