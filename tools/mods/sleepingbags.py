from gen.fw import Mod, horizontal_state
from gen import tex

COLORS = {'red': '#a12722', 'blue': '#35399d', 'green': '#546d1b', 'orange': '#f07613', 'gray': '#5a6066'}


def bag_tex(color, seed):
    img = tex.noise_fill(color, seed, 0.1)
    px = img.load()
    c = tex.rgb(color)
    for y in range(16):
        for x in range(16):
            if y < 4:
                px[x, y] = (232, 230, 220, 255) if 2 <= x <= 13 else tex.shade(c, 0.8)
            elif x in (0, 15) or y == 15:
                px[x, y] = tex.shade(c, 0.7)
            elif x == 8 and y > 5:
                px[x, y] = (200, 200, 200, 255)
    return img


def build():
    m = Mod('fw_sleepingbags', 'Sleeping Bags', 'Roll out a sleeping bag anywhere to skip the night without moving your spawn point - '
                                                'perfect for long expeditions.', tab='TOOLS')
    rl = m.rl
    m.init('SleepingBagBlock.init()')
    for c, hexc in COLORS.items():
        name = c + '_sleeping_bag'
        t = '%s:block/%s' % (m.modid, name)
        model = {'parent': 'minecraft:block/block', 'textures': {'particle': t, 'top': t, 'side': t},
                 'elements': [{'from': [1, 0, 0], 'to': [15, 3, 16], 'faces': {
                     'up': {'texture': '#top', 'uv': [1, 0, 15, 16]}, 'down': {'texture': '#side', 'cullface': 'down'},
                     'north': {'texture': '#side', 'uv': [1, 13, 15, 16]}, 'south': {'texture': '#side', 'uv': [1, 13, 15, 16]},
                     'east': {'texture': '#side', 'uv': [0, 13, 16, 16]}, 'west': {'texture': '#side', 'uv': [0, 13, 16, 16]}}}]}
        m.block(name, java='new SleepingBagBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.3f).sound(SoundType.WOOL).noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY))',
                jtype='SleepingBagBlock', tab='TOOLS', state=horizontal_state(m, name, name, lit_prop=False), models={name: model},
                textures={'block/' + name: bag_tex(hexc, name)}, mine=None, desc='Sleep through the night. Does not set your spawn.')
        m.shaped(name, rl(name), ['  W', 'LLL'], {'W': 'minecraft:%s_wool' % c, 'L': 'minecraft:leather'})
    return m
