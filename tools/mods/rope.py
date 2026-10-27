from gen.fw import Mod
from gen import tex


def rope_tex():
    img = tex.blank()
    px = img.load()
    for y in range(16):
        for x in range(6, 10):
            twist = (x + y) % 4
            px[x, y] = (176, 138, 82, 255) if twist < 2 else (138, 104, 58, 255)
    return img


def build():
    m = Mod('fw_rope', 'Rope', 'Climbable rope for cave diving and cliff faces. Right-click a hanging rope with more rope to extend it, '
                               'or throw down a whole rope coil at once.', tab='TOOLS')
    rl = m.rl
    t = '%s:block/rope' % m.modid
    model = {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': {'particle': t, 'rope': t},
             'elements': [{'from': [6.5, 0, 6.5], 'to': [9.5, 16, 9.5], 'faces': {
                 f: {'texture': '#rope', 'uv': [6, 0, 9, 16]} for f in ('north', 'south', 'east', 'west')}},
                          {'from': [6.5, 0, 6.5], 'to': [9.5, 16, 9.5], 'faces': {'up': {'texture': '#rope', 'uv': [6, 6, 9, 9]}, 'down': {'texture': '#rope', 'uv': [6, 6, 9, 9]}}}]}
    m.block('rope', java='new RopeBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.4f).sound(SoundType.WOOL).noOcclusion().noCollission())',
            jtype='RopeBlock', tab='TOOLS', models={'rope': model}, item_model='blocktex:rope', textures={'block/rope': rope_tex()}, mine='axe', level=None,
            desc='Hangs from blocks. Climbable.', tags=['minecraft:climbable'])
    m.item('rope_coil', ('rope', '#b08a52'), java='new RopeCoilItem(new Item.Properties().stacksTo(16))', jtype='RopeCoilItem',
           desc='Use on a block edge to drop 24 rope at once')
    m.item('plant_fiber', ('seeds', '#7a9a4a'), tab='MATERIALS', desc='Gathered from grass. Twist into rope.')
    m.shaped('rope', rl('rope'), ['F', 'F', 'F'], {'F': [rl('plant_fiber'), 'minecraft:string']}, 3)
    m.shaped('rope_coil', rl('rope_coil'), ['RRR', 'R R', 'RRR'], {'R': rl('rope')})
    m.shapeless('rope_from_coil', rl('rope'), [rl('rope_coil')], 8)
    m.inject_loot('plant_fiber_grass', ['minecraft:blocks/grass', 'minecraft:blocks/tall_grass', 'minecraft:blocks/fern'],
                  {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': rl('plant_fiber')}],
                                                         'conditions': [{'condition': 'minecraft:random_chance', 'chance': 0.15}]}]})
    return m
