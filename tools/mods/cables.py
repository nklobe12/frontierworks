from gen.fw import Mod, const
from gen import tex

CABLES = [
    ('copper_cable', 256, '#2d2d2d', '#d9844f'),
    ('gold_cable', 1024, '#3a2d2d', '#f2c94c'),
    ('electrum_cable', 4096, '#2d2d3a', '#ecd97a'),
    ('superconductor_cable', 32768, '#e8f6ff', '#4fd8ff'),
]
ROT = {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}


def cable_tex(insul, core, seed):
    img = tex.noise_fill(insul, seed, 0.08)
    px = img.load()
    c = tex.rgb(core)
    for x in range(16):
        px[x, 7] = c + (255,)
        px[x, 8] = tex.shade(c, 0.8)
    return img


def end_tex(insul, core, seed):
    img = tex.noise_fill(insul, seed + 'e', 0.08)
    px = img.load()
    c = tex.rgb(core)
    for y in range(5, 11):
        for x in range(5, 11):
            px[x, y] = tex.shade(c, 1.0 if (x + y) % 3 else 1.2)
    return img


def build():
    m = Mod('fw_cables', 'Energy Cables', 'Four tiers of energy cables - copper, gold, electrum and superconductor - that link '
                                          'generators, batteries and machines into one network with no losses.', tab='MACHINES')
    rl = m.rl
    for name, rate, insul, core in CABLES:
        C = const(name)
        t = lambda n: '%s:block/%s' % (m.modid, n)
        core_model = {'parent': 'minecraft:block/block', 'textures': {'particle': t(name), 'cable': t(name), 'end': t(name + '_end')},
                      'elements': [{'from': [6, 6, 6], 'to': [10, 10, 10], 'faces': {d: {'texture': '#end'} for d in ('north', 'south', 'east', 'west', 'up', 'down')}}]}
        side_model = {'parent': 'minecraft:block/block', 'textures': {'particle': t(name), 'cable': t(name), 'end': t(name + '_end')},
                      'elements': [{'from': [6, 6, 0], 'to': [10, 10, 6], 'faces': {
                          'north': {'texture': '#end', 'cullface': 'north'}, 'east': {'texture': '#cable', 'uv': [0, 6, 6, 10]},
                          'west': {'texture': '#cable', 'uv': [0, 6, 6, 10]}, 'up': {'texture': '#cable', 'uv': [6, 0, 10, 6], 'rotation': 90},
                          'down': {'texture': '#cable', 'uv': [6, 0, 10, 6], 'rotation': 90}}}]}
        parts = [{'apply': {'model': '%s:block/%s_core' % (m.modid, name)}}]
        for d, r in ROT.items():
            parts.append({'when': {d: 'true'}, 'apply': dict({'model': '%s:block/%s_side' % (m.modid, name)}, **r)})
        inv = {'parent': 'minecraft:block/block', 'textures': {'particle': t(name), 'cable': t(name), 'end': t(name + '_end')},
               'display': {'gui': {'rotation': [30, 225, 0], 'scale': [0.9, 0.9, 0.9]}},
               'elements': [{'from': [6, 6, 0], 'to': [10, 10, 16], 'faces': {
                   'north': {'texture': '#end'}, 'south': {'texture': '#end'}, 'east': {'texture': '#cable', 'uv': [0, 6, 16, 10]},
                   'west': {'texture': '#cable', 'uv': [0, 6, 16, 10]}, 'up': {'texture': '#cable', 'uv': [0, 6, 16, 10], 'rotation': 90},
                   'down': {'texture': '#cable', 'uv': [0, 6, 16, 10], 'rotation': 90}}}]}
        m.block(name, java='new CableBlock(FWProps.metal(0.6f).noOcclusion(), %d, () -> Content.%s_BE.get())' % (rate, C), jtype='CableBlock',
                state={'multipart': parts}, models={name + '_core': core_model, name + '_side': side_model, name + '_inventory': inv},
                item_model='block:' + name + '_inventory', textures={'block/' + name: cable_tex(insul, core, name), 'block/%s_end' % name: end_tex(insul, core, name)},
                mine='pickaxe', level=None)
        m.field('public static final RegistryObject<BlockEntityType<CableBE>> %s_BE = REG.blockEntity("%s", (pos, state) -> new CableBE(Content.%s_BE.get(), pos, state), Content.%s);'
                % (C, name, C, C))
    m.shaped('copper_cable', rl('copper_cable'), ['RRR', 'WWW', 'RRR'], {'R': '#forge:rubber', 'W': '#forge:wires/copper'}, 6)
    m.shaped('gold_cable', rl('gold_cable'), ['RRR', 'WWW', 'RRR'], {'R': '#forge:rubber', 'W': '#forge:wires/gold'}, 6)
    m.shaped('electrum_cable', rl('electrum_cable'), ['RRR', 'WWW', 'RRR'], {'R': 'fw_chemistry:vulcanized_rubber', 'W': '#forge:wires/electrum'}, 6)
    m.shaped('superconductor_cable', rl('superconductor_cable'), ['CPC', 'EEE', 'CPC'], {'C': 'fw_freezer:cryotheum_dust', 'P': '#forge:plates/platinum',
                                                                                        'E': 'fw_crystals:charged_energite'}, 8)
    m.tr('tooltip.fw_cables.rate', 'Transfers up to %s FE/t')
    return m
