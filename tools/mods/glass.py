"""Glassworks: hardened (blast-proof) glass, clear, glowing, dark and framed glass, with panes."""
from gen.fw import Mod
from gen import tex

M = 'fw_glass'

GLASS = [
    # name, java, frame colour, tint, style, render, desc
    ('hardened_glass', 'new GlassBlock(FWProps.glass().strength(4.0f, 1200.0f).requiresCorrectToolForDrops())', '#5b6470', '#9fb4c8', 'framed',
     'cutout', 'Blast-proof. Withers and creepers can\'t break it.'),
    ('clear_glass', 'new GlassBlock(FWProps.glass())', '#d8eef5', None, 'clear', 'translucent', 'Frameless glass for seamless windows'),
    ('glowing_glass', 'new GlassBlock(FWProps.glass().lightLevel(s -> 15))', '#f2d77a', '#ffe9a0', 'framed', 'translucent', 'Glass that glows as bright as glowstone'),
    ('dark_glass', 'new TintedGlassBlock(FWProps.glass())', '#2a2a30', '#24242c', 'framed', 'translucent', 'See through it, but light can\'t pass'),
    ('framed_glass', 'new GlassBlock(FWProps.glass())', '#7b8591', None, 'framed', 'cutout', 'Glass in a steel frame'),
]
PANES = ['hardened_glass', 'clear_glass', 'glowing_glass', 'framed_glass']


def pane(m, base, java_props, render):
    name = base + '_pane'
    t = '%s:block/%s' % (M, base)
    edge = '%s:block/%s_pane_top' % (M, base)
    mk = lambda parent: {'parent': 'minecraft:block/' + parent, 'render_type': 'minecraft:' + render, 'textures': {'pane': t, 'edge': edge}}
    models = {name + '_post': mk('template_glass_pane_post'), name + '_side': mk('template_glass_pane_side'),
              name + '_side_alt': mk('template_glass_pane_side_alt'), name + '_noside': mk('template_glass_pane_noside'),
              name + '_noside_alt': mk('template_glass_pane_noside_alt')}
    b = lambda n, **r: dict({'model': '%s:block/%s_%s' % (M, name, n)}, **r)
    state = {'multipart': [
        {'apply': b('post')},
        {'when': {'north': 'true'}, 'apply': b('side')}, {'when': {'east': 'true'}, 'apply': b('side', y=90)},
        {'when': {'south': 'true'}, 'apply': b('side_alt')}, {'when': {'west': 'true'}, 'apply': b('side_alt', y=90)},
        {'when': {'north': 'false'}, 'apply': b('noside')}, {'when': {'east': 'false'}, 'apply': b('noside_alt')},
        {'when': {'south': 'false'}, 'apply': b('noside_alt', y=90)}, {'when': {'west': 'false'}, 'apply': b('noside', y=270)}]}
    m.block(name, java='new IronBarsBlock(%s)' % java_props, jtype='IronBarsBlock', state=state, models=models,
            item_model='blocktex:' + base, mine='pickaxe' if base == 'hardened_glass' else None, loot='self', tab='DECOR',
            tags=['forge:glass_panes'], item_tags=['forge:glass_panes'])
    m.shaped(name, m.rl(name), ['###', '###'], {'#': m.rl(base)}, 16)


def build():
    m = Mod(M, 'Glassworks', 'Better glass: blast-proof hardened glass, frameless clear glass, glowing glass, light-blocking dark glass '
                             'and steel-framed glass, plus panes.', tab='DECOR')
    rl = m.rl
    for name, java, frame, tint, style, render, desc in GLASS:
        img = tex.glass(frame, M + name, tint, style)
        edge = tex.noise_fill(frame, M + name + 'e', 0.05)
        m.block(name, java=java, jtype='TintedGlassBlock' if 'Tinted' in java else 'GlassBlock',
                models={name: {'parent': 'minecraft:block/cube_all', 'render_type': 'minecraft:' + render, 'textures': {'all': '%s:block/%s' % (M, name)}}},
                textures={'block/' + name: img, 'block/%s_pane_top' % name: edge}, mine='pickaxe' if name == 'hardened_glass' else None,
                level='iron' if name == 'hardened_glass' else None, desc=desc, tags=['forge:glass'], item_tags=['forge:glass'], tab='DECOR')
        if name in PANES:
            props = java[java.index('(') + 1:java.rindex(')')]
            pane(m, name, props, render)
    m.smelt('clear_glass', 'minecraft:glass', rl('clear_glass'), 0.1, blast=False)
    m.shaped('glowing_glass', rl('glowing_glass'), ['GGG', 'GDG', 'GGG'], {'G': '#forge:glass/colorless', 'D': '#forge:dusts/glowstone'}, 8)
    m.shaped('dark_glass', rl('dark_glass'), ['GGG', 'GDG', 'GGG'], {'G': '#forge:glass/colorless', 'D': 'minecraft:black_dye'}, 8)
    m.shaped('framed_glass', rl('framed_glass'), ['NGN', 'GNG', 'NGN'], {'G': '#forge:glass/colorless', 'N': '#forge:nuggets/iron'}, 4)
    m.shaped('hardened_glass_obsidian', rl('hardened_glass'), ['OGO', 'GDG', 'OGO'], {'G': '#forge:glass/colorless', 'O': '#forge:obsidian', 'D': 'minecraft:diamond'}, 4)
    return m
