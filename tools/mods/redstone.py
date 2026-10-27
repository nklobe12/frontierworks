from gen.fw import Mod
from gen import tex

DIR_ROT = {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}


def build():
    m = Mod('fw_redstone', 'Redstone Devices', 'Automation helpers: Block Breaker, Block Placer, Player Detector and an adjustable '
                                               'Redstone Clock.', tab='LOGISTICS')
    rl = m.rl
    t = lambda n: '%s:block/%s' % (m.modid, n)
    side = tex.casing('basic', 'rsside', 'side')
    m.texture('block/device_side', side)
    for name, cls, glyph in (('block_breaker', 'Breaker', 'pick'), ('block_placer', 'Placer', 'arrow')):
        model = {'parent': 'minecraft:block/orientable_with_bottom', 'textures': {'top': t('device_side'), 'bottom': t('device_side'), 'side': t('device_side'),
                                                                                  'front': t(name + '_front')}}
        variants = {}
        for f, r in DIR_ROT.items():
            for trig in ('false', 'true'):
                variants['facing=%s,triggered=%s' % (f, trig)] = dict({'model': t(name)}, **r)
        m.block(name, java='new RedstoneBlocks.%s(FWProps.stone(3.0f))' % cls, jtype='RedstoneBlocks.' + cls, tab='LOGISTICS',
                state={'variants': variants}, models={name: model}, textures={'block/%s_front' % name: tex.machine_front('basic', glyph, False, name)})
    det = 'player_detector'
    m.block(det, java='new RedstoneBlocks.PlayerDetector(FWProps.stone(2.0f))', jtype='RedstoneBlocks.PlayerDetector', tab='LOGISTICS',
            state={'variants': {'powered=false': {'model': t(det)}, 'powered=true': {'model': t(det + '_on')}}},
            models={det: {'parent': 'minecraft:block/cube_all', 'textures': {'all': t(det)}}, det + '_on': {'parent': 'minecraft:block/cube_all', 'textures': {'all': t(det + '_on')}}},
            textures={'block/' + det: tex.machine_front('basic', 'eye', False, det), 'block/%s_on' % det: tex.machine_front('basic', 'eye', True, det)},
            desc='Emits redstone while a player is within 8 blocks')
    clk = 'redstone_clock'
    variants = {}
    for d in range(5):
        variants['delay=%d,powered=false' % d] = {'model': t(clk)}
        variants['delay=%d,powered=true' % d] = {'model': t(clk + '_on')}
    m.block(clk, java='new RedstoneBlocks.Clock(FWProps.stone(2.0f))', jtype='RedstoneBlocks.Clock', tab='LOGISTICS', state={'variants': variants},
            models={clk: {'parent': 'minecraft:block/cube_all', 'textures': {'all': t(clk)}}, clk + '_on': {'parent': 'minecraft:block/cube_all', 'textures': {'all': t(clk + '_on')}}},
            textures={'block/' + clk: tex.lamp('#7a1010', False, clk), 'block/%s_on' % clk: tex.lamp('#ff3030', True, clk)},
            desc='Pulses redstone. Right-click to change speed. Pauses while powered.')
    m.shaped('block_breaker', rl('block_breaker'), ['CCC', 'CPC', 'CRC'], {'C': '#forge:cobblestone', 'P': 'minecraft:iron_pickaxe', 'R': '#forge:dusts/redstone'})
    m.shaped('block_placer', rl('block_placer'), ['CCC', 'CDC', 'CRC'], {'C': '#forge:cobblestone', 'D': 'minecraft:dispenser', 'R': '#forge:dusts/redstone'})
    m.shaped('player_detector', rl('player_detector'), ['SRS', 'REC', 'SRS'], {'S': 'minecraft:stone', 'R': '#forge:dusts/redstone', 'E': 'minecraft:ender_eye',
                                                                               'C': 'minecraft:comparator'})
    m.shaped('redstone_clock', rl('redstone_clock'), ['SRS', 'RCR', 'SRS'], {'S': 'minecraft:stone', 'R': '#forge:dusts/redstone', 'C': 'minecraft:clock'})
    m.tr('msg.fw_redstone.clock', 'Interval: %ss')
    return m
