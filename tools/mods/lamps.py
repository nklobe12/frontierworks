"""Lamps & Lights: always-on lamps and redstone lamps in all sixteen colours, plus cage lights."""
from gen.fw import Mod, const
from gen import tex

M = 'fw_lamps'

COLORS = {
    'white': '#f2f2f2', 'orange': '#f9801d', 'magenta': '#c74ebd', 'light_blue': '#3ab3da', 'yellow': '#fed83d', 'lime': '#80c71f',
    'pink': '#f38baa', 'gray': '#6b7075', 'light_gray': '#a0a0a0', 'cyan': '#169c9c', 'purple': '#8932b8', 'blue': '#3c44aa',
    'brown': '#835432', 'green': '#5e7c16', 'red': '#b02e26', 'black': '#3a3a40',
}


def cage_model(m, name, tex_name):
    t = '%s:block/%s' % (M, tex_name)
    cage = '%s:block/cage_frame' % M
    return {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': {'particle': t, 'lamp': t, 'cage': cage},
            'elements': [
                {'from': [5, 0, 5], 'to': [11, 8, 11], 'faces': {d: {'texture': '#lamp'} for d in ('north', 'south', 'east', 'west', 'up', 'down')}},
                {'from': [4, 0, 4], 'to': [12, 9, 12], 'faces': {d: {'texture': '#cage'} for d in ('north', 'south', 'east', 'west', 'up')}}]}


def build():
    m = Mod(M, 'Lamps & Lights', 'Colourful lighting: always-on lamps and redstone-controlled lamps in all sixteen dye colours, and '
                                 'compact cage lights for industrial builds.', tab='DECOR')
    rl = m.rl
    t = lambda n: '%s:block/%s' % (M, n)
    cage = tex.blank()
    px = cage.load()
    for i in range(16):
        for j in (0, 15, 5, 10):
            px[i, j] = (70, 70, 76, 255)
            px[j, i] = (70, 70, 76, 255)
    m.texture('block/cage_frame', cage)
    for color, hexc in COLORS.items():
        dye = 'minecraft:%s_dye' % color
        lamp = 'lamp_' + color
        m.block(lamp, java='new Block(FWProps.lamp())', textures={'block/' + lamp: tex.lamp(hexc, True, M + lamp)}, mine='pickaxe', tab='DECOR',
                lang='%s Lamp' % color.replace('_', ' ').title(), tags=['fw_lamps:lamps'])
        m.shapeless(lamp, rl(lamp), ['minecraft:glowstone', dye, '#forge:glass/colorless'], 2)
        rlamp = 'redstone_lamp_' + color
        m.block(rlamp, java='new RedstoneLampBlock(FWProps.lamp())', jtype='RedstoneLampBlock',
                state={'variants': {'lit=false': {'model': t(rlamp)}, 'lit=true': {'model': t(rlamp + '_on')}}},
                models={rlamp: {'parent': 'minecraft:block/cube_all', 'textures': {'all': t(rlamp)}},
                        rlamp + '_on': {'parent': 'minecraft:block/cube_all', 'textures': {'all': t(rlamp + '_on')}}},
                textures={'block/' + rlamp: tex.lamp(hexc, False, M + rlamp), 'block/%s_on' % rlamp: tex.lamp(hexc, True, M + rlamp + 'on')},
                mine='pickaxe', tab='DECOR', lang='%s Redstone Lamp' % color.replace('_', ' ').title())
        m.shapeless(rlamp, rl(rlamp), ['minecraft:redstone_lamp', dye])
        cl = 'cage_light_' + color
        m.block(cl, java='new com.frontierworks.lamps.CageLightBlock(FWProps.lamp().noOcclusion())', jtype='com.frontierworks.lamps.CageLightBlock',
                state={'variants': {'face=floor': {'model': t(cl)}, 'face=ceiling': {'model': t(cl), 'x': 180},
                                    'face=wall_north': {'model': t(cl), 'x': 270}, 'face=wall_south': {'model': t(cl), 'x': 90},
                                    'face=wall_west': {'model': t(cl), 'x': 270, 'y': 270}, 'face=wall_east': {'model': t(cl), 'x': 270, 'y': 90}}},
                models={cl: cage_model(m, cl, lamp)}, mine='pickaxe', tab='DECOR', lang='%s Cage Light' % color.replace('_', ' ').title())
        m.shaped(cl, rl(cl), [' N ', 'NLN', ' N '], {'N': '#forge:nuggets/iron', 'L': rl(lamp)}, 4)
    return m
