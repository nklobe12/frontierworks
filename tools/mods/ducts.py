from PIL import Image, ImageDraw

from gen.ls import Mod, title, entity_block, element_model, const
from gen import tex

# name: (kind, tier, half-width px, body colour, band colour, core colour, upgrade ingredient, lang, desc)
DUCTS = {
    'fluxduct': ('ENERGY', 0, 2, '#5d6470', '#2c2f36', '#c0392b', None, 'Fluxduct',
                 'Carries energy: 1,000 FE/t into each machine it touches'),
    'hardened_fluxduct': ('ENERGY', 1, 2, '#a9a48f', '#5b584b', '#e74c3c', '#forge:ingots/invar', 'Hardened Fluxduct',
                          'Carries energy: 4,000 FE/t into each machine it touches'),
    'resonant_fluxduct': ('ENERGY', 2, 2, '#c9b25a', '#6e5d22', '#f1c40f', '#forge:ingots/electrum', 'Resonant Fluxduct',
                          'Carries energy: 16,000 FE/t into each machine it touches'),
    'superconducting_fluxduct': ('ENERGY', 3, 2, '#c7d3dd', '#56677a', '#5dade2', '#forge:ingots/platinum', 'Superconducting Fluxduct',
                                 'Carries energy: 100,000 FE/t into each machine it touches'),
    'fluiduct': ('FLUID', 0, 3, '#b87333', '#6b3e18', None, None, 'Fluiduct',
                 'Carries one fluid at a time: 200 mB/t into each tank it touches'),
    'hardened_fluiduct': ('FLUID', 1, 3, '#a9a48f', '#5b584b', None, '#forge:ingots/invar', 'Hardened Fluiduct',
                          'Carries one fluid at a time: 800 mB/t into each tank it touches'),
    'super_fluiduct': ('FLUID', 2, 3, '#d5dbe0', '#6f7f8c', None, '#forge:ingots/duralumin', 'Super Fluiduct',
                       'Carries one fluid at a time: 4,000 mB/t into each tank it touches'),
    'itemduct': ('ITEM', 0, 3, '#c9ccd1', '#6c7280', None, None, 'Itemduct',
                 'Carries items between inventories. Servos pull 8 items every second'),
    'impulse_itemduct': ('ITEM', 1, 3, '#d9c27a', '#7a6420', None, 'minecraft:glowstone_dust', 'Impulse Itemduct',
                         'Fast itemduct: servos pull 16 items twice a second and items move four times as fast'),
}

ATTACH = {
    # name: (type, fluid, colour, lang, tooltip)
    'servo': ('SERVO', False, '#c0392b', 'Servo',
              'Fit to an itemduct side touching an inventory\nPulls items out of it into the network\nRedstone on the duct pauses it'),
    'fluid_servo': ('SERVO', True, '#2e86c1', 'Fluid Servo',
                    'Fit to a fluiduct side touching a tank\nPulls fluid out of it into the network\nRedstone on the duct pauses it'),
    'filter': ('FILTER', False, '#ecf0f1', 'Filter',
               'Fit to an itemduct side\nOnly the items you choose are delivered there'),
    'fluid_filter': ('FILTER', True, '#85c1e9', 'Fluid Filter',
                     'Fit to a fluiduct side\nOnly the fluids you choose are delivered there\n(put a filled bucket in the filter)'),
    'retriever': ('RETRIEVER', False, '#8e44ad', 'Retriever',
                  'Fit to an itemduct side touching an inventory\nFetches the items you choose from every other\ninventory on the network'),
}


def duct_body(body, band, seed, core=None, glass=False):
    """Side texture of a duct arm: metal shell, coloured core line or glass window."""
    img = tex.noise_fill(body, seed, 0.05)
    px = img.load()
    c, b = tex.rgb(body), tex.rgb(band)
    for x in range(16):
        px[x, 0] = tex.shade(b, 0.9)
        px[x, 15] = tex.shade(b, 0.7)
    if core:
        k = tex.rgb(core)
        for x in range(16):
            for y in range(5, 11):
                d = abs(y - 7.5)
                px[x, y] = tex.shade(k, 1.15 - d * 0.12) if d < 2 else tex.shade(c, 0.6)
    if glass:
        for x in range(16):
            for y in range(4, 12):
                if x % 8 in (0, 7):
                    continue
                r, g, bb, a = px[x, y]
                px[x, y] = (min(255, r + 40), min(255, g + 50), min(255, bb + 60), 110)
    for x in (0, 8):
        for y in range(1, 15):
            px[x, y] = tex.shade(b, 1.0)
    return img


def duct_core(body, band, seed, core=None):
    img = tex.noise_fill(body, seed + 'c', 0.05)
    px = img.load()
    b = tex.rgb(band)
    for i in range(16):
        for j in (0, 15):
            px[i, j] = tex.shade(b, 0.8)
            px[j, i] = tex.shade(b, 0.8)
    if core:
        k = tex.rgb(core)
        for x in range(5, 11):
            for y in range(5, 11):
                px[x, y] = tex.shade(k, 1.1 - 0.06 * (abs(x - 7.5) + abs(y - 7.5)))
    return img


def flange(body, band, seed):
    img = tex.noise_fill(band, seed + 'f', 0.06)
    px = img.load()
    c = tex.rgb(body)
    for x in range(2, 14):
        for y in range(2, 14):
            px[x, y] = tex.shade(c, 0.85 + 0.05 * ((x + y) % 3))
    for x in range(5, 11):
        for y in range(5, 11):
            px[x, y] = (34, 34, 38, 255)
    return img


def duct_models(m, name, r, textures, inv_tex, conn_values=('duct|end',), end_value='end', render='cutout', flange_model=True):
    """Multipart duct: core, an arm per connected side, a flange where it meets a machine."""
    t = lambda n: '%s:block/%s' % (m.modid, n)
    a, b = 8 - r, 8 + r
    core = element_model([([a, a, a], [b, b, b], '#core')], textures, render=render)
    arm = element_model([([a, a, 0], [b, b, a], {
        'east': {'texture': '#body', 'uv': [0, a, a, b]}, 'west': {'texture': '#body', 'uv': [16 - a, a, 16, b]},
        'up': {'texture': '#body', 'uv': [a, 0, b, a], 'rotation': 90}, 'down': {'texture': '#body', 'uv': [a, 16 - a, b, 16], 'rotation': 90}})],
        textures, render=render)
    models = {name + '_core': core, name + '_arm': arm}
    if flange_model:
        f0, f1 = a - 1, b + 1
        models[name + '_flange'] = element_model([([f0, f0, 0], [f1, f1, 2], {
            'north': {'texture': '#flange', 'cullface': 'north'}, 'south': {'texture': '#flange'},
            'east': {'texture': '#flange', 'uv': [0, f0, 2, f1]}, 'west': {'texture': '#flange', 'uv': [14, f0, 16, f1]},
            'up': {'texture': '#flange', 'uv': [f0, 0, f1, 2]}, 'down': {'texture': '#flange', 'uv': [f0, 14, f1, 16]}})], textures, render=render)
    inv = element_model([([a, a, 0], [b, b, 16], {
        'north': {'texture': '#core', 'uv': [a, a, b, b]}, 'south': {'texture': '#core', 'uv': [a, a, b, b]},
        'east': {'texture': '#body', 'uv': [0, a, 16, b]}, 'west': {'texture': '#body', 'uv': [0, a, 16, b]},
        'up': {'texture': '#body', 'uv': [0, a, 16, b], 'rotation': 90}, 'down': {'texture': '#body', 'uv': [0, a, 16, b], 'rotation': 90}})],
        inv_tex, render=render)
    inv['display'] = {'gui': {'rotation': [30, 225, 0], 'scale': [0.9, 0.9, 0.9]}, 'ground': {'scale': [0.5, 0.5, 0.5]},
                      'fixed': {'scale': [0.8, 0.8, 0.8]}}
    models[name + '_inventory'] = inv
    rot = {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}
    parts = [{'apply': {'model': t(name + '_core')}}]
    for d, rr in rot.items():
        for v in conn_values:
            parts.append({'when': {d: v}, 'apply': dict({'model': t(name + '_arm')}, **rr)})
        if flange_model:
            parts.append({'when': {d: end_value}, 'apply': dict({'model': t(name + '_flange')}, **rr)})
    return models, {'multipart': parts}


def attachment_face(colour, kind, seed):
    img = tex.noise_fill('#3b3f46', seed, 0.05)
    px = img.load()
    c = tex.rgb(colour)
    for i in range(16):
        for j in range(16):
            edge = i in (0, 15) or j in (0, 15)
            if edge:
                px[i, j] = tex.shade(c, 0.6)
    d = ImageDraw.Draw(img)
    if kind == 'SERVO':
        d.ellipse([3, 3, 12, 12], fill=tex.shade(c, 0.8), outline=tex.shade(c, 0.5))
        for k in range(4):
            x0, y0 = [(7, 3), (12, 7), (8, 12), (3, 8)][k]
            d.line([7.5, 7.5, x0, y0], fill=tex.shade(c, 1.25))
        d.rectangle([7, 7, 8, 8], fill=(30, 30, 30, 255))
    elif kind == 'FILTER':
        for x in range(2, 14):
            for y in range(2, 14):
                px[x, y] = tex.shade(c, 0.95) if (x + y) % 3 == 0 or (x - y) % 3 == 0 else (60, 64, 70, 255)
    else:
        d.rectangle([3, 3, 12, 12], fill=tex.shade(c, 0.7))
        d.polygon([(7, 3), (12, 8), (9, 8), (9, 12), (6, 12), (6, 8), (3, 8)], fill=tex.shade(c, 1.3))
    return img


def attachment_side(colour, seed):
    img = tex.noise_fill('#4a4f57', seed + 's', 0.05)
    px = img.load()
    c = tex.rgb(colour)
    for x in range(16):
        for y in range(6, 10):
            px[x, y] = tex.shade(c, 0.9)
    return img


def attachment_item(colour, kind, seed):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    face = attachment_face(colour, kind, seed)
    face = face.resize((12, 12), Image.NEAREST)
    img.paste(face, (2, 2))
    d = ImageDraw.Draw(img)
    d.rectangle([1, 1, 14, 14], outline=(40, 42, 48, 255))
    return img


def viaduct_glass(frame, seed):
    img = Image.new('RGBA', (16, 16), (200, 230, 245, 70))
    px = img.load()
    f = tex.rgb(frame)
    for i in range(16):
        for j in (0, 1, 14, 15):
            px[i, j] = tex.shade(f, 0.9 if j in (0, 15) else 1.1)
    for x in (0, 7, 8, 15):
        for y in range(16):
            px[x, y] = tex.shade(f, 1.0)
    for k in range(3, 7):
        px[k + 2, k] = (255, 255, 255, 110)
    return img


def station_tex(part, seed):
    img = tex.casing('basic', seed, 'top' if part == 'top' else 'side')
    px = img.load()
    brass = tex.rgb('#c8a24a')
    if part == 'top':
        for x in range(3, 13):
            for y in range(3, 13):
                d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
                if d < 5:
                    px[x, y] = tex.shade(brass, 1.2 - d * 0.08)
                if d < 2:
                    px[x, y] = (120, 210, 255, 255)
    else:
        for x in range(4, 12):
            for y in range(3, 13):
                px[x, y] = (150, 200, 225, 255) if (x + y) % 5 else (210, 240, 255, 255)
        for x in range(3, 13):
            px[x, 2] = px[x, 13] = tex.shade(brass, 1.0)
        for y in range(2, 14):
            px[3, y] = px[12, y] = tex.shade(brass, 0.9)
    return img


def build():
    m = Mod('ls_ducts', 'Ducts', 'Energy, fluid and item ducts with servos, filters and retrievers, plus glass viaducts '
                                 'that carry you between named transit stations.', tab='LOGISTICS')
    m.init('DuctsSetup.init(bus)')
    m.client('com.lodestar.ducts.client.DuctsClient.init(bus)')
    rl = m.rl
    t = lambda n: '%s:block/%s' % (m.modid, n)
    be_refs = []
    for name, (kind, tier, r, body, band, core, up, lang, desc) in DUCTS.items():
        glass = kind != 'ENERGY'
        m.texture('block/%s' % name, duct_body(body, band, m.modid + name, core, glass))
        m.texture('block/%s_core' % name, duct_core(body, band, m.modid + name, core))
        m.texture('block/%s_flange' % name, flange(body, band, m.modid + name))
        tx = {'particle': t(name), 'body': t(name), 'core': t(name + '_core'), 'flange': t(name + '_flange')}
        models, state = duct_models(m, name, r, tx, tx)
        props = 'LSProps.machineTransparent().strength(0.6f)'
        entity_block(m, name, 'DuctBE', 'new DuctBlock(%s, DuctKind.%s, %d, %sf, BE_TYPE)' % (props, kind, tier, r / 16.0),
                     jtype='DuctBlock', tab='LOGISTICS', models=models, state=state, item_model='block:%s_inventory' % name,
                     lang=lang, desc=desc + '\nWrench a side to disconnect it', mine='pickaxe')
        be_refs.append('Content.%s_BE' % const(name))
        if up is None:
            key = {'ENERGY': {'L': '#forge:ingots/lead', 'R': 'minecraft:redstone'},
                   'FLUID': {'L': '#forge:ingots/copper', 'R': '#forge:glass'},
                   'ITEM': {'L': '#forge:ingots/tin', 'R': '#forge:glass'}}[kind]
            m.shaped(name, rl(name), ['LRL'], key, count=4)
        else:
            prev = [n for n, v in DUCTS.items() if v[0] == kind and v[1] == tier - 1][0]
            m.shaped(name, rl(name), ['DDD', 'DXD', 'DDD'], {'D': rl(prev), 'X': up}, count=8)
    m.java_file('DuctTable.java', 'package com.lodestar.ducts;\n\nimport net.minecraft.world.level.block.entity.BlockEntityType;\n\n'
                                  'import java.util.List;\nimport java.util.function.Supplier;\n\n'
                                  '/** Generated: every duct block entity type. */\npublic final class DuctTable {\n'
                                  '    private DuctTable() {}\n\n'
                                  '    public static List<Supplier<BlockEntityType<DuctBE>>> types() {\n'
                                  '        return List.of(%s);\n    }\n}\n' % ', '.join('%s::get' % r for r in be_refs))

    # attachments: items plus the block models the duct renderer draws on duct sides
    for name, (typ, fluid, colour, lang, tip) in ATTACH.items():
        m.item(name, attachment_item(colour, typ, m.modid + name), java='new AttachmentItem(new Item.Properties(), Attachment.Type.%s, %s)'
               % (typ, 'true' if fluid else 'false'), jtype='AttachmentItem', tab='LOGISTICS', lang=lang)
        m.tr('tooltip.ls_ducts.%s%s' % (typ.lower(), '_fluid' if fluid else ''), tip)
        m.texture('block/attachment_%s' % name, attachment_face(colour, typ, m.modid + name))
        m.texture('block/attachment_%s_side' % name, attachment_side(colour, m.modid + name))
        ft, st = t('attachment_' + name), t('attachment_%s_side' % name)
        sides = lambda x0, x1, y0, y1, z0, z1: {'east': {'texture': '#side', 'uv': [z0, y0, z1, y1]}, 'west': {'texture': '#side', 'uv': [z0, y0, z1, y1]},
                                               'up': {'texture': '#side', 'uv': [x0, z0, x1, z1]}, 'down': {'texture': '#side', 'uv': [x0, z0, x1, z1]}}
        plate = dict(sides(4, 12, 4, 12, 0, 3), north={'texture': '#face'}, south={'texture': '#face'})
        neck = sides(5.5, 10.5, 5.5, 10.5, 3, 5)
        m.asset('models/block/attachment_%s.json' % name,
                element_model([([4, 4, 0.2], [12, 12, 3], plate), ([5.5, 5.5, 3], [10.5, 10.5, 5], neck)],
                              {'face': ft, 'side': st, 'particle': st}))
    m.shaped('servo', rl('servo'), ['RPR'], {'R': 'minecraft:redstone', 'P': 'minecraft:piston'}, count=2)
    m.shaped('fluid_servo', rl('fluid_servo'), ['RPR', ' B '], {'R': 'minecraft:redstone', 'P': 'minecraft:piston', 'B': 'minecraft:bucket'}, count=2)
    m.shaped('filter', rl('filter'), [' N ', 'NPN', ' N '], {'N': 'minecraft:iron_nugget', 'P': 'minecraft:paper'}, count=2)
    m.shapeless('fluid_filter', rl('fluid_filter'), [rl('filter'), 'minecraft:bucket'])
    m.shaped('retriever', rl('retriever'), [' E ', 'SXS'], {'E': 'minecraft:ender_pearl', 'S': rl('servo'), 'X': '#forge:ingots/electrum'})

    # viaducts and stations
    m.texture('block/viaduct', viaduct_glass('#c8a24a', m.modid + 'via'))
    m.texture('block/viaduct_core', viaduct_glass('#a7832e', m.modid + 'viac'))
    vtx = {'particle': t('viaduct'), 'body': t('viaduct'), 'core': t('viaduct_core')}
    vmodels, vstate = duct_models(m, 'viaduct', 5, vtx, vtx, conn_values=('true',), render='translucent', flange_model=False)
    m.block('viaduct', java='new com.lodestar.ducts.transit.ViaductBlock(LSProps.glass().strength(0.5f))', jtype='com.lodestar.ducts.transit.ViaductBlock',
            tab='TRANSPORT', models=vmodels, state=vstate, item_model='block:viaduct_inventory', mine='pickaxe',
            desc='Glass transit tube\nJoin two transit stations with it and ride a pod between them')
    m.shaped('viaduct', rl('viaduct'), ['BGB', 'G G', 'BGB'], {'B': '#forge:ingots/brass', 'G': '#forge:glass'}, count=8)
    m.texture('block/transit_station_top', station_tex('top', m.modid + 'st'))
    m.texture('block/transit_station_side', station_tex('side', m.modid + 'ss'))
    entity_block(m, 'transit_station', 'com.lodestar.ducts.transit.StationBE',
                 'new com.lodestar.ducts.transit.StationBlock(LSProps.machine(), BE_TYPE)', jtype='com.lodestar.ducts.transit.StationBlock',
                 tab='TRANSPORT', models={'transit_station': {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
                     'top': t('transit_station_top'), 'bottom': t('transit_station_top'), 'side': t('transit_station_side')}}},
                 desc='Use it to pick a destination; a pod carries you along the viaducts\nName it with a name tag', mine='pickaxe')
    m.shaped('transit_station', rl('transit_station'), ['BGB', 'GEG', 'BRB'], {'B': '#forge:ingots/brass', 'G': '#forge:glass',
                                                                              'E': 'minecraft:ender_pearl', 'R': 'minecraft:redstone_block'})
    m.tr('entity.ls_ducts.transit_pod', 'Transit Pod')

    for k, v in {'gui.ls_ducts.servo': 'Servo', 'gui.ls_ducts.filter': 'Filter', 'gui.ls_ducts.retriever': 'Retriever',
                 'gui.ls_ducts.whitelist': 'Allow', 'gui.ls_ducts.blacklist': 'Block',
                 'gui.ls_ducts.filter_mode': 'Allow only these, or block these',
                 'gui.ls_ducts.station': 'Transit Station', 'gui.ls_ducts.choose': 'Choose a destination',
                 'gui.ls_ducts.blocks': '%s blocks', 'msg.ls_ducts.station_named': 'Station named %s',
                 'msg.ls_ducts.no_stations': 'No other stations are joined to this one by viaducts'}.items():
        m.tr(k, v)

    m.codex_intro('Ducts move energy, fluids and items between machines. A duct joins every duct of its kind next to it and every '
                  'machine, tank or chest that can hold what it carries. Wrench a side to cut it, and wrench it again to join it.')
    m.codex_page('Energy', 'Fluxducts take energy from dynamos and batteries and share it out evenly to every machine they touch. Each '
                           'tier carries more: hardened, resonant and superconducting. Upgrade eight ducts at a time with an invar, '
                           'electrum or platinum ingot in the middle of the crafting grid.',
                 items=[rl(n) for n, v in DUCTS.items() if v[0] == 'ENERGY'])
    m.codex_page('Fluids', 'Fluiducts carry one fluid at a time. Machines that push fluid out fill them on their own; to pull fluid '
                           'out of a tank, fit a fluid servo to the side touching it. A fluid filter limits what a side receives: put '
                           'a filled bucket in it to stand for that fluid.',
                 items=[rl(n) for n, v in DUCTS.items() if v[0] == 'FLUID'] + [rl('fluid_servo'), rl('fluid_filter')])
    m.codex_page('Items', 'Items travel along itemducts to the nearest inventory that will take them. Servos pull items out of the '
                          'inventory they touch. Filters on a side decide what may be delivered there, so you can sort to many chests. '
                          'A retriever fetches the items in its filter from every other inventory on the network.\n\n'
                          'Use an attachment with an empty hand to set its filter; sneak-use it to take it off. A redstone signal on '
                          'the duct pauses its servos.',
                 items=[rl('itemduct'), rl('impulse_itemduct'), rl('servo'), rl('filter'), rl('retriever')])
    m.codex_page('Viaducts', 'Build a line of viaducts between two or more transit stations. Use a station to see every station joined '
                             'to it and pick one: a pod carries you along the tubes at fourteen blocks a second and sets you down on '
                             'top of the far station. Name stations with a name tag so the list makes sense.',
                 items=[rl('viaduct'), rl('transit_station')])
    return m
