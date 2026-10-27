from PIL import ImageDraw

from gen.ls import Mod, title, entity_block, element_model
from gen import tex

DYNAMOS = {
    # name: (rate, BE expression args, plate, gear, feature item, accent colour, desc)
    'stirling': ('ItemDynamoBE', '40, DynamoFuels::stirling', 'iron', 'copper', 'minecraft:furnace', '#c0392b',
                 'Burns furnace fuel: 40 FE/t'),
    'magmatic': ('FluidDynamoBE', '60, DynamoFuels::magmatic', 'invar', 'invar', 'minecraft:magma_block', '#e67e22',
                 'Burns lava: 60 FE/t'),
    'compression': ('FluidDynamoBE', '80, DynamoFuels::compression', 'bronze', 'bronze', 'minecraft:piston', '#d4ac0d',
                    'Burns liquid fuels such as light oil: 80 FE/t'),
    'lapidary': ('ItemDynamoBE', '60, DynamoFuels::lapidary', 'silver', 'silver', 'minecraft:amethyst_block', '#8e44ad',
                 'Releases the energy locked in gems: 60 FE/t'),
    'redflux': ('ItemDynamoBE', '40, DynamoFuels::redflux', 'electrum', 'electrum', 'minecraft:redstone_block', '#e74c3c',
                'Burns redstone and glowstone: 40 FE/t'),
    'thermoelectric': ('ThermoelectricDynamoBE', '', 'constantan', 'constantan', 'minecraft:packed_ice', '#3498db',
                       'Makes power from hot and cold blocks touching it: up to 60 FE/t'),
}


def side_tex(accent, seed):
    img = tex.casing('basic', seed, 'side')
    c = tex.rgb(accent)
    for x in range(16):
        for y in (6, 7, 8):
            img.putpixel((x, y), tex.shade(c, 1.0 if y == 7 else 0.75))
    return img


def coil_tex(seed, lit):
    img = tex.noise_fill('#b86a3a', seed, 0.08)
    for y in range(0, 16, 2):
        for x in range(16):
            img.putpixel((x, y), (120, 60, 30, 255) if not lit else (255, 150, 60, 255))
    return img


def build():
    m = Mod('ls_dynamos', 'Dynamos', 'Six generators that push power out of their coil into whatever they are placed against: '
                                      'stirling (furnace fuel), magmatic (lava), compression (liquid fuels), lapidary (gems), '
                                      'redflux (redstone) and thermoelectric (hot and cold blocks). Output augments make them faster '
                                      'and efficiency augments stretch each piece of fuel.', tab='POWER')
    m.init('DynamosSetup.init(bus)')
    rl = m.rl
    t = lambda n: '%s:block/%s' % (m.modid, n)
    m.texture('block/dynamo_top', tex.casing('basic', m.modid + 'top', 'top'))
    m.texture('block/dynamo_bottom', tex.casing('basic', m.modid + 'bottom', 'bottom'))
    m.texture('block/dynamo_coil', coil_tex(m.modid + 'coil', False))
    m.texture('block/dynamo_coil_on', coil_tex(m.modid + 'coil', True))
    rot = {'up': {}, 'down': {'x': 180}, 'north': {'x': 90}, 'south': {'x': 90, 'y': 180}, 'east': {'x': 90, 'y': 90}, 'west': {'x': 90, 'y': 270}}
    for name, (be, args, plate, gear, feature, accent, desc) in DYNAMOS.items():
        n = name + '_dynamo'

        def model(coil):
            base = {'north': {'texture': '#side'}, 'south': {'texture': '#side'}, 'east': {'texture': '#side'}, 'west': {'texture': '#side'},
                    'up': {'texture': '#top'}, 'down': {'texture': '#bottom'}}
            return element_model([([0, 0, 0], [16, 10, 16], base), ([4, 10, 4], [12, 16, 12], '#coil')],
                                 {'side': t(n + '_side'), 'top': t('dynamo_top'), 'bottom': t('dynamo_bottom'), 'coil': t(coil), 'particle': t(n + '_side')})
        state = {'variants': {}}
        for f, r in rot.items():
            for lit in ('false', 'true'):
                state['variants']['facing=%s,lit=%s' % (f, lit)] = dict({'model': t(n + ('_on' if lit == 'true' else ''))}, **r)
        entity_block(m, n, be, 'new DynamoBlock(LSProps.machine().noOcclusion().lightLevel(s -> s.getValue(LSEntityBlock.LIT) ? 7 : 0), BE_TYPE)',
                     jtype='DynamoBlock', tab='POWER', models={n: model('dynamo_coil'), n + '_on': model('dynamo_coil_on')}, state=state,
                     textures={'block/%s_side' % n: side_tex(accent, m.modid + n)}, desc=desc + '\nThe coil faces the block you place it on',
                     ctor_args=args, level='stone')
        m.shaped(n, rl(n), [' C ', 'PXP', 'GGG'], {'C': 'ls_machina:redstone_coil', 'P': '#forge:plates/' + plate, 'X': feature,
                                                   'G': '#forge:gears/' + gear})
        m.tr('lens.category.ls_dynamos.' + name, title(name) + ' Dynamo')
    m.tag('fluids', 'ls_dynamos:magmatic_fuel', 'minecraft:lava', 'minecraft:flowing_lava')
    for k, v in {'gui.ls_dynamos.hot': 'Heat sources: %s', 'gui.ls_dynamos.cold': 'Cold sources: %s',
                 'lens.ls_dynamos.fe': '%s FE', 'lens.ls_dynamos.fe_bucket': '%s FE per bucket'}.items():
        m.tr(k, v)
    m.codex_intro('Dynamos turn fuel into energy (FE) and push it out of their copper coil, into the machine, cable or duct the coil '
                  'touches. Place a dynamo against the block you want to power. Each dynamo also has a charge slot for batteries '
                  'and tools.')
    m.codex_page('Choosing a dynamo', 'The stirling dynamo burns anything a furnace would. Magmatic dynamos drink lava and compression '
                                      'dynamos burn refined liquid fuels: light oil from the Machina refinery first, then diesel and '
                                      'kerosene. Lapidary dynamos crack gems for a lot of energy each, redflux dynamos burn redstone, and '
                                      'the thermoelectric dynamo needs no fuel at all: give it lava or magma on one side and water or ice '
                                      'on another.', items=[rl(n + '_dynamo') for n in DYNAMOS])
    return m
