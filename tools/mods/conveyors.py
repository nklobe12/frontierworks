from gen.fw import Mod, horizontal_state
from gen import tex

BELTS = [('conveyor_belt', 0.12, '#3a3a3e', '#e0b84a'), ('fast_conveyor_belt', 0.22, '#3a3a3e', '#e74c3c'),
         ('express_conveyor_belt', 0.36, '#3a3a3e', '#3498db')]


def belt_frames(base, arrow, frames=8):
    """Animated texture strip: arrows scrolling 'up' the texture (the belt's direction of travel)."""
    from PIL import Image
    strip = Image.new('RGBA', (16, 16 * frames))
    c, a = tex.rgb(base), tex.rgb(arrow)
    for f in range(frames):
        img = tex.noise_fill(base, 'belt%d' % f, 0.08)
        px = img.load()
        off = (f * 2) % 16
        for y in range(16):
            for x in range(16):
                yy = (y + off) % 8
                if (yy == 2 and 5 <= x <= 10) or (yy == 3 and 6 <= x <= 9) or (yy == 4 and 7 <= x <= 8):
                    px[x, y] = a + (255,)
                if x in (0, 15):
                    px[x, y] = tex.shade(c, 0.6)
                if x in (1, 14):
                    px[x, y] = (150, 150, 158, 255)
        strip.paste(img, (0, 16 * f))
    return strip


def build():
    m = Mod('fw_conveyors', 'Conveyors', 'Conveyor belts in three speeds that carry items, mobs and players - and drop items '
                                         'straight into any inventory at the end of the line. Sneak to stand still.', tab='LOGISTICS')
    rl = m.rl
    for name, speed, base, arrow in BELTS:
        t = lambda n: '%s:block/%s' % (m.modid, n)
        model = {'parent': 'minecraft:block/block', 'textures': {'particle': t(name), 'top': t(name), 'side': t(name + '_side')},
                 'elements': [{'from': [0, 0, 0], 'to': [16, 3, 16], 'faces': {
                     'up': {'texture': '#top', 'rotation': 0}, 'down': {'texture': '#side', 'cullface': 'down'},
                     'north': {'texture': '#side', 'uv': [0, 13, 16, 16]}, 'south': {'texture': '#side', 'uv': [0, 13, 16, 16]},
                     'east': {'texture': '#side', 'uv': [0, 13, 16, 16]}, 'west': {'texture': '#side', 'uv': [0, 13, 16, 16]}}}]}
        state = horizontal_state(m, name, name, lit_prop=False)
        m.block(name, java='new ConveyorBlock(FWProps.metal(1.0f).noOcclusion(), %s)' % speed, jtype='ConveyorBlock', tab='LOGISTICS',
                state=state, models={name: model}, textures={'block/%s_side' % name: tex.metal_block('#7b8591', name)}, level=None,
                desc='Moves things at %d blocks/second' % round(speed * 20))
        m.asset('textures/block/%s.png' % name, belt_frames(base, arrow))
        m.asset('textures/block/%s.png.mcmeta' % name, {'animation': {'frametime': max(1, int(round(0.2 / speed)))}})
    m.shaped('conveyor_belt', rl('conveyor_belt'), ['RRR', 'IMI'], {'R': '#forge:rubber', 'I': '#forge:ingots/iron', 'M': 'fw_parts:motor'}, 8)
    m.shaped('fast_conveyor_belt', rl('fast_conveyor_belt'), ['BBB', 'GRG'], {'B': rl('conveyor_belt'), 'G': '#forge:ingots/gold', 'R': '#forge:dusts/redstone'}, 3)
    m.shaped('express_conveyor_belt', rl('express_conveyor_belt'), ['BBB', 'CSC'], {'B': rl('fast_conveyor_belt'), 'C': 'fw_circuits:basic_circuit',
                                                                                    'S': 'fw_parts:servo'}, 3)
    return m
