from gen.fw import Mod, custom_model_machine
from gen import tex

TANKS = [('tank_basic', 16000, '#9aa0a8'), ('tank_reinforced', 64000, '#7b8591'), ('tank_elite', 256000, '#c9a23a'),
         ('tank_ultimate', 1024000, '#9b5de5')]


def tank_side(frame, seed):
    img = tex.glass(frame, seed, '#9fc8e0', 'framed')
    px = img.load()
    c = tex.rgb(frame)
    for x in range(16):
        for y in (0, 1, 14, 15):
            px[x, y] = tex.shade(c, 1.0 if y < 2 else 0.7)
    return img


def build():
    m = Mod('fw_tanks', 'Tanks', 'Portable fluid tanks from 16 to 1,024 buckets that keep their contents when broken. '
                                 'Stack them and they drain downwards.', tab='LOGISTICS')
    rl = m.rl
    for name, cap, frame in TANKS:
        textures = {'block/%s_side' % name: tank_side(frame, name), 'block/%s_top' % name: tex.metal_block(frame, name + 't', 'plated')}
        custom_model_machine(m, name, 'new TankBE(BE_TYPE, pos, state, %d)' % cap, 'TankBE', textures, top=name + '_top', side=name + '_side',
                             tab='LOGISTICS', props='FWProps.machineTransparent()', desc='Holds %d buckets. Keeps its contents when broken.' % (cap // 1000))
        m.data('loot_tables/blocks/%s.json' % name, {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{
            'type': 'minecraft:item', 'name': rl(name), 'functions': [{'function': 'minecraft:copy_nbt', 'source': 'block_entity',
                                                                       'ops': [{'source': 'Tank0', 'target': 'BlockEntityTag.Tank0', 'op': 'replace'}]}]}]}]})
        for mn in (name, name + '_on'):
            m.asset('models/block/%s.json' % mn, {'parent': 'minecraft:block/cube_bottom_top', 'render_type': 'minecraft:cutout',
                                                  'textures': {'top': '%s:block/%s_top' % (m.modid, name), 'bottom': '%s:block/%s_top' % (m.modid, name),
                                                               'side': '%s:block/%s_side' % (m.modid, name)}})
    m.shaped('tank_basic', rl('tank_basic'), ['PGP', 'G G', 'PGP'], {'P': '#forge:plates/iron', 'G': '#forge:glass'})
    m.shaped('tank_reinforced', rl('tank_reinforced'), ['PGP', 'GTG', 'PGP'], {'P': '#forge:plates/steel', 'G': '#forge:glass', 'T': rl('tank_basic')})
    m.shaped('tank_elite', rl('tank_elite'), ['PGP', 'GTG', 'PGP'], {'P': '#forge:plates/platinum', 'G': 'fw_glass:hardened_glass', 'T': rl('tank_reinforced')})
    m.shaped('tank_ultimate', rl('tank_ultimate'), ['PGP', 'GTG', 'PGP'], {'P': 'fw_mars:titanium_plate', 'G': 'fw_glass:hardened_glass', 'T': rl('tank_elite')})
    return m
