from gen.fw import Mod
from gen import tex

COLORS = {'white': '#e9ecec', 'orange': '#f07613', 'magenta': '#bd44b3', 'light_blue': '#3aafd9', 'yellow': '#f8c627', 'lime': '#70b919',
          'pink': '#ed8dac', 'gray': '#3e4447', 'light_gray': '#8e8e86', 'cyan': '#158991', 'purple': '#792aac', 'blue': '#35399d',
          'brown': '#724728', 'green': '#546d1b', 'red': '#a12722', 'black': '#141519'}


def elevator_tex(color, seed):
    img = tex.metal_block(color, seed, 'plain')
    px = img.load()
    for y in range(5, 11):
        w = 3 - abs(y - 7) if y < 8 else 3 - abs(y - 8)
        for x in range(8 - max(0, w), 8 + max(0, w)):
            px[x, y] = (255, 255, 255, 220)
    return img


def build():
    m = Mod('fw_elevators', 'Elevators', 'Elevator blocks in all 16 colours: jump to rise to the next elevator above, sneak to '
                                         'drop to the one below. Up to 24 blocks apart.', tab='DECOR')
    rl = m.rl
    m.init('ElevatorEvents.init()')
    for c, hexc in COLORS.items():
        name = c + '_elevator'
        m.block(name, java='new ElevatorEvents.ElevatorBlock(FWProps.metal(1.5f))', jtype='ElevatorEvents.ElevatorBlock', tab='DECOR',
                textures={'block/' + name: elevator_tex(hexc, name)}, level=None, tags=['fw_elevators:elevators'], item_tags=['fw_elevators:elevators'],
                desc='Jump to go up, sneak to go down')
        m.shaped(name, rl(name), ['WWW', 'WEW', 'WWW'], {'W': 'minecraft:%s_wool' % c, 'E': '#forge:ender_pearls'})
        m.shapeless(name + '_dye', rl(name), ['#fw_elevators:elevators', 'minecraft:%s_dye' % c])
    return m
