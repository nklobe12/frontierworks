from PIL import ImageDraw

from gen.ls import Mod, title, const, entity_block, element_model, horizontal_model_state, recipe_kind
from gen import tex
from mods import _gemtex as gt

# name: (texture colour, special pattern, map colour, geode min y, max y, rarity (1 in N chunks), cutting time)
GEMS = {
    'garnet': ('#8a1c2c', None, 'COLOR_RED', -16, 48, 40, 140),
    'topaz': ('#f0a12a', None, 'COLOR_ORANGE', 0, 64, 40, 140),
    'peridot': ('#8fc23a', None, 'COLOR_LIGHT_GREEN', -40, 24, 48, 140),
    'sapphire': ('#2a55c9', None, 'COLOR_BLUE', -64, -8, 56, 200),
    'ruby': ('#e3214f', None, 'COLOR_RED', -64, -16, 56, 160),
    'opal': ('#c9dce6', 'opal', 'SNOW', -24, 40, 64, 120),
    'onyx': ('#2b2a30', 'onyx', 'COLOR_BLACK', -64, 0, 52, 160),
    'aquamarine': ('#6fd3dc', None, 'COLOR_CYAN', -32, 40, 48, 140),
}
EFFECTS = {
    'garnet': ('heals you by part of the damage you deal', 'armor toughness'),
    'topaz': ('mining speed', 'movement speed'),
    'peridot': ('more experience from blocks and mobs', 'slowly heals you'),
    'sapphire': ('attack speed', 'armor'),
    'ruby': ('attack damage', 'max health'),
    'opal': ('luck', 'luck'),
    'onyx': ('extra knockback', 'knockback resistance'),
    'aquamarine': ('mining speed under water', 'swim speed'),
}
CUTS = ['cabochon', 'step', 'brilliant']
SETTINGS = {'silver': '#dce3ea', 'gold': '#f2c94c', 'platinum': '#a9d8e6'}
FACING_ROT = {'up': {}, 'down': {'x': 180}, 'north': {'x': 90}, 'south': {'x': 90, 'y': 180}, 'east': {'x': 90, 'y': 90},
              'west': {'x': 90, 'y': 270}}
SILK = {'condition': 'minecraft:match_tool', 'predicate': {'enchantments': [{'enchantment': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}


def wheel_disc(seed, spinning):
    """Grinding wheel face: a round stone disc with a steel hub; blurred rings and sparks while spinning."""
    img = tex.blank()
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d > 7.6:
                continue
            if d < 1.8:
                px[x, y] = (70, 74, 82, 255)
            elif d < 2.8:
                px[x, y] = (150, 156, 166, 255)
            elif spinning:
                ring = int(d) % 2
                g = 150 + ring * 22 + int(tex.rnd(seed, 'r', int(d)) * 12)
                px[x, y] = (g, g - 6, g - 14, 255)
            else:
                g = 132 + int(tex.rnd(seed, x, y) * 46)
                px[x, y] = (g, g - 6, g - 14, 255)
            if d > 6.8:
                px[x, y] = (96, 90, 84, 255)
    if spinning:
        for (x, y) in ((14, 3), (15, 2), (13, 1), (15, 5)):
            px[x, y] = (255, 214, 120, 255)
    return img


def wheel_rim(seed):
    img = tex.noise_fill('#8c8378', seed, 0.18)
    for x in range(16):
        img.putpixel((x, 0), (70, 66, 60, 255))
        img.putpixel((x, 15), (70, 66, 60, 255))
    return img


def slurry_top(seed):
    """The wheel's tray: a steel rim around grey-green slurry."""
    img = tex.noise_fill('#5e6a62', seed, 0.2)
    px = img.load()
    for i in range(16):
        for (x, y) in ((i, 0), (0, i), (i, 15), (15, i)):
            px[x, y] = (150, 154, 160, 255)
        for (x, y) in ((i, 1), (1, i)):
            px[x, y] = (110, 114, 120, 255)
    for i in range(6):
        x, y = 3 + tex.hsh(seed, 'g', i) % 10, 3 + tex.hsh(seed, 'h', i) % 10
        px[x, y] = (190, 200, 196, 255)
    return img


def casing_stripe(seed, colour):
    img = tex.casing('basic', seed, 'side')
    c = tex.rgb(colour)
    for x in range(16):
        for y in (10, 11):
            img.putpixel((x, y), tex.shade(c, 1.0 if y == 10 else 0.75))
    return img


def felt_top(seed):
    img = tex.noise_fill('#2f6b4a', seed, 0.08)
    px = img.load()
    wood = tex.rgb('#6b4426')
    for i in range(16):
        for (x, y) in ((i, 0), (0, i), (i, 15), (15, i), (i, 1), (1, i), (i, 14), (14, i)):
            px[x, y] = tex.shade(wood, 0.85 + 0.1 * ((x + y) % 3))
    for i in range(3, 13, 4):
        px[i, 5] = (240, 240, 230, 255)
    return img


def build():
    m = Mod('ls_lapidary', 'Lapidary', 'Eight gems grow in geodes deep underground: garnet, topaz, peridot, sapphire, ruby, opal, onyx and '
                                       'aquamarine. Break full clusters for rough stones, cut them on a powered cutting wheel as cabochons, '
                                       'step cuts or brilliants (harder cuts give stronger gems but may shatter), then set them into '
                                       'sockets on your tools, weapons and armor at the jeweler\'s table for bonuses that scale with cut '
                                       'and quality. Gem blocks and gem lamps for building.', tab='MATERIALS')
    m.init('LapidarySetup.init(bus)')
    m.client('com.lodestar.lapidary.client.LapidaryClient.init(bus)')
    m.imp('net.minecraft.world.level.material.PushReaction')
    rl = m.rl
    t = lambda n: '%s:block/%s' % (m.modid, n)
    cutting = recipe_kind(m, 'gem_cutting')

    # ----------------------------------------------------------------- geode shell blocks
    m.block('geode_crust', props='LSProps.stone(2.0f).mapColor(MapColor.COLOR_BLACK)', textures={'block/geode_crust': gt.geode_crust(m.modid + 'crust')},
            desc='The dark outer shell of a gem geode')
    m.block('geode_lining', props='LSProps.stone(1.2f).mapColor(MapColor.TERRACOTTA_WHITE)', textures={'block/geode_lining': gt.geode_lining(m.modid + 'lining')},
            desc='Chalky stone lining the inside of a geode shell')
    m.shapeless('geode_lining', rl('geode_lining'), ['minecraft:calcite', rl('gem_grit')], 2)

    # ----------------------------------------------------------------- gems
    for g, (col, special, mapc, lo, hi, rarity, ctime) in GEMS.items():
        G = const(g)
        seed = m.modid + g
        tool_fx, armor_fx = EFFECTS[g]
        # rough and cut stones
        m.item('rough_' + g, gt.rough_gem(col, seed, special), tags=['ls_lapidary:rough_gems'],
               desc='Uncut. Shape it on a cutting wheel')
        for c in CUTS:
            m.texture('item/%s_%s' % (g, c), gt.cut_gem(col, c, seed, special))
            m.asset('models/item/%s_%s.json' % (g, c), {'parent': 'minecraft:item/generated', 'textures': {'layer0': '%s:item/%s_%s' % (m.modid, g, c)}})
        overrides = [{'predicate': {'ls_lapidary:cut': 0.1 * (i + 1) - 0.01}, 'model': '%s:item/%s_%s' % (m.modid, g, c)} for i, c in enumerate(CUTS)]
        m.item(g, None, java='new CutGemItem(new Item.Properties(), GemType.%s)' % G, jtype='CutGemItem',
               model={'parent': 'minecraft:item/generated', 'textures': {'layer0': '%s:item/%s_step' % (m.modid, g)}, 'overrides': overrides},
               tags=['forge:gems/' + g, 'forge:gems', 'ls_lapidary:cut_gems'])
        m.tr('item.ls_lapidary.%s.desc' % g, 'Socketed in tools: %s\nSocketed in armor: %s' % (tool_fx, armor_fx))
        m.mrecipe('ls_lapidary:gem_cutting', g, [(rl('rough_' + g), 1)], [(rl(g), 1)], time=ctime, energy=20)

        # geode blocks
        m.block(g + '_druse', props='LSProps.crystal(0).mapColor(MapColor.%s)' % mapc, textures={'block/%s_druse' % g: gt.druse(col, seed, special)},
                lang=title(g) + ' Druse', desc='Glittering inner lining of a %s geode' % title(g))
        cl = g + '_cluster'
        m.block('budding_' + g, java='new GemBuddingBlock(LSProps.crystal(0).mapColor(MapColor.%s).randomTicks().pushReaction(PushReaction.DESTROY), '
                                    '() -> Content.%s_CLUSTER.get())' % (mapc, G), jtype='GemBuddingBlock',
                textures={'block/budding_' + g: gt.budding(col, seed, special)},
                loot={'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': rl('budding_' + g)}],
                                                            'conditions': [SILK]}]},
                desc='Grows %s buds on its open faces over time\nOnly Silk Touch can move it' % title(g))
        stage_models = {}
        for s in range(4):
            mn = '%s_stage%d' % (cl, s)
            stage_models[mn] = {'parent': 'minecraft:block/cross', 'render_type': 'minecraft:cutout', 'textures': {'cross': t(mn)}}
            m.texture('block/' + mn, gt.cluster_stage(col, s, seed, special))
        variants = {}
        for f, r in FACING_ROT.items():
            for s in range(4):
                variants['facing=%s,stage=%d' % (f, s)] = dict({'model': t('%s_stage%d' % (cl, s))}, **r)
        rough = rl('rough_' + g)
        stage3 = {'condition': 'minecraft:block_state_property', 'block': rl(cl), 'properties': {'stage': '3'}}
        cluster_loot = {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:alternatives', 'children': [
            {'type': 'minecraft:item', 'name': rl(cl), 'conditions': [SILK],
             'functions': [{'function': 'minecraft:copy_state', 'block': rl(cl), 'properties': ['stage']}]},
            {'type': 'minecraft:item', 'name': rough, 'conditions': [stage3, {'condition': 'minecraft:match_tool', 'predicate': {'tag': 'minecraft:cluster_max_harvestables'}}],
             'functions': [{'function': 'minecraft:set_count', 'count': 2, 'add': False},
                           {'function': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:ore_drops'}]},
            {'type': 'minecraft:item', 'name': rough, 'conditions': [stage3], 'functions': [{'function': 'minecraft:explosion_decay'}]}]}]}]}
        m.block(cl, java='new GemClusterBlock(GemClusterBlock.props(MapColor.%s))' % mapc, jtype='GemClusterBlock', state={'variants': variants},
                models=stage_models, item_model='blocktex:%s_stage3' % cl, loot=cluster_loot, lang=title(g) + ' Cluster',
                desc='Only a full cluster gives rough gems (two, more with a pickaxe and Fortune)')

        # decoration
        m.block(g + '_block', props='LSProps.gemBlock(5.0f).mapColor(MapColor.%s)' % mapc, textures={'block/%s_block' % g: gt.gem_block(col, seed, special)},
                tags=['forge:storage_blocks/' + g, 'minecraft:beacon_base_blocks'], item_tags=['forge:storage_blocks/' + g], lang='Block of Rough ' + title(g))
        m.shaped(g + '_block', rl(g + '_block'), ['###', '###', '###'], {'#': rough})
        m.shapeless('rough_%s_from_block' % g, rough, [rl(g + '_block')], 9)
        lamp = g + '_lamp'
        m.block(lamp, java='new RedstoneLampBlock(LSProps.lamp().mapColor(MapColor.%s))' % mapc, jtype='RedstoneLampBlock',
                state={'variants': {'lit=false': {'model': t(lamp)}, 'lit=true': {'model': t(lamp + '_on')}}},
                models={lamp: {'parent': 'minecraft:block/cube_all', 'textures': {'all': t(lamp)}},
                        lamp + '_on': {'parent': 'minecraft:block/cube_all', 'textures': {'all': t(lamp + '_on')}}},
                textures={'block/' + lamp: gt.gem_lamp(col, False, seed, special), 'block/%s_on' % lamp: gt.gem_lamp(col, True, seed, special)},
                tab='BUILDING', desc='Lights up with a redstone signal')
        m.shaped(lamp, rl(lamp), [' R ', 'RLR', ' R '], {'R': rough, 'L': 'minecraft:redstone_lamp'})

        # worldgen: a geode lined with this gem
        simple = lambda n, props=None: {'type': 'minecraft:simple_state_provider', 'state': dict({'Name': n}, **({'Properties': props} if props else {}))}
        m.configured('geode_' + g, {'type': 'minecraft:geode', 'config': {
            'blocks': {
                'filling_provider': simple('minecraft:air'),
                'inner_layer_provider': simple(rl(g + '_druse')),
                'alternate_inner_layer_provider': simple(rl('budding_' + g)),
                'middle_layer_provider': simple(rl('geode_lining')),
                'outer_layer_provider': simple(rl('geode_crust')),
                'inner_placements': [{'Name': rl(cl), 'Properties': {'stage': str(s), 'facing': 'up', 'waterlogged': 'false'}} for s in range(4)],
                'cannot_replace': '#minecraft:features_cannot_replace',
                'invalid_blocks': '#minecraft:geode_invalid_blocks'},
            'layers': {'filling': 1.7, 'inner_layer': 2.2, 'middle_layer': 3.2, 'outer_layer': 4.2},
            'crack': {'generate_crack_chance': 0.95, 'base_crack_size': 2.0, 'crack_point_offset': 2},
            'use_potential_placements_chance': 0.35, 'use_alternate_layer0_chance': 0.083, 'placements_require_layer0_alternate': True,
            'outer_wall_distance': {'type': 'minecraft:uniform', 'value': {'min_inclusive': 4, 'max_inclusive': 6}},
            'distribution_points': {'type': 'minecraft:uniform', 'value': {'min_inclusive': 3, 'max_inclusive': 4}},
            'point_offset': {'type': 'minecraft:uniform', 'value': {'min_inclusive': 1, 'max_inclusive': 2}},
            'min_gen_offset': -16, 'max_gen_offset': 16, 'noise_multiplier': 0.05, 'invalid_blocks_threshold': 1}})
        m.placed('geode_' + g, rl('geode_' + g), [
            {'type': 'minecraft:rarity_filter', 'chance': rarity}, {'type': 'minecraft:in_square'},
            {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'min_inclusive': {'absolute': lo}, 'max_inclusive': {'absolute': hi}}},
            {'type': 'minecraft:biome'}])
        m.add_feature(rl('geode_' + g), '#minecraft:is_overworld', 'local_modifications')

        # machina pulverizer: rough gems and druse into abrasive grit
        m.mrecipe('ls_machina:pulverizing', 'lapidary_rough_' + g, [(rough, 1)], [(rl('gem_grit'), 3)], time=120, energy=20)
        m.mrecipe('ls_machina:pulverizing', 'lapidary_druse_' + g, [(rl(g + '_druse'), 1)], [(rl('gem_grit'), 1), (rough, 1, 0.15)], time=120, energy=20)

    # ----------------------------------------------------------------- abrasives and settings
    m.item('emery_powder', gt.grit('#7a6e66', m.modid + 'emery'), desc='Coarse abrasive for the cutting wheel: two cuts per powder')
    m.shapeless('emery_powder', rl('emery_powder'), ['minecraft:flint', '#forge:nuggets/iron', '#minecraft:sand'], 2)
    m.item('gem_grit', gt.grit('#c79aa6', m.modid + 'grit'), desc='Fine abrasive for the cutting wheel: four cuts per grit and a better finish')
    m.shapeless('gem_grit', rl('gem_grit'), ['ls_metalwork:smithing_hammer', '#ls_lapidary:rough_gems'], 2)
    for metal, col in SETTINGS.items():
        n = metal + '_setting'
        m.item(n, gt.setting(col, m.modid + n), tags=['ls_lapidary:settings'],
               desc='Adds the %s socket to an item at the jeweler\'s table' % {'silver': 'first', 'gold': 'second', 'platinum': 'third'}[metal])
        m.shaped(n, rl(n), [' N ', 'NPN'], {'N': '#forge:nuggets/' + metal, 'P': '#forge:plates/' + metal}, 2)

    # ----------------------------------------------------------------- cutting wheel
    m.texture('block/cutting_wheel_side', casing_stripe(m.modid + 'cws', '#3fa7a0'))
    m.texture('block/cutting_wheel_bottom', tex.casing('basic', m.modid + 'cwb', 'bottom'))
    m.texture('block/cutting_wheel_tray', slurry_top(m.modid + 'cwt'))
    m.texture('block/cutting_wheel_disc', wheel_disc(m.modid + 'disc', False))
    m.texture('block/cutting_wheel_disc_on', wheel_disc(m.modid + 'disc', True))
    m.texture('block/cutting_wheel_rim', wheel_rim(m.modid + 'rim'))

    def wheel_model(disc):
        base = {'north': {'texture': '#side'}, 'south': {'texture': '#side'}, 'east': {'texture': '#side'}, 'west': {'texture': '#side'},
                'up': {'texture': '#tray'}, 'down': {'texture': '#bottom'}}
        wheel = {'east': {'texture': '#disc'}, 'west': {'texture': '#disc'}, 'north': {'texture': '#rim', 'uv': [0, 0, 2, 16]},
                 'south': {'texture': '#rim', 'uv': [0, 0, 2, 16]}, 'up': {'texture': '#rim', 'uv': [0, 0, 2, 16]}}
        motor = {'north': {'texture': '#side', 'uv': [0, 2, 8, 7]}, 'south': {'texture': '#side', 'uv': [0, 2, 8, 7]},
                 'east': {'texture': '#side', 'uv': [0, 2, 8, 7]}, 'west': {'texture': '#side', 'uv': [0, 2, 8, 7]},
                 'up': {'texture': '#bottom'}}
        return element_model([([0, 0, 0], [16, 9, 16], base), ([5, 6, 3], [7, 16, 13], wheel), ([7, 10, 7], [10, 11, 9], '#rim'),
                              ([10, 9, 4], [15, 14, 12], motor)],
                             {'side': t('cutting_wheel_side'), 'tray': t('cutting_wheel_tray'), 'bottom': t('cutting_wheel_bottom'),
                              'disc': t(disc), 'rim': t('cutting_wheel_rim'), 'particle': t('cutting_wheel_side')}, render='cutout')

    entity_block(m, 'cutting_wheel', 'CuttingWheelBE', 'new com.lodestar.core.machine.MachineBlock(LSProps.machine().noOcclusion(), BE_TYPE)',
                 jtype='com.lodestar.core.machine.MachineBlock',
                 models={'cutting_wheel': wheel_model('cutting_wheel_disc'), 'cutting_wheel_on': wheel_model('cutting_wheel_disc_on')},
                 state=horizontal_model_state(m, 'cutting_wheel', 'cutting_wheel_on'), level='stone',
                 desc='Cuts rough gems on power (20 FE/t) with an abrasive\nPick cabochon, step or brilliant in its screen')
    m.shaped('cutting_wheel', rl('cutting_wheel'), ['PWP', 'GFG', 'PCP'], {'P': '#forge:plates/iron', 'W': 'minecraft:grindstone',
                                                                      'G': '#forge:gears/bronze', 'F': 'ls_machina:machine_frame',
                                                                      'C': 'ls_machina:redstone_coil'})

    # ----------------------------------------------------------------- jeweler's table
    m.texture('block/jewelers_table_top', felt_top(m.modid + 'felt'))
    m.texture('block/jewelers_table_wood', tex.planks('#6b4426', m.modid + 'jwood'))
    m.texture('block/jewelers_table_steel', tex.metal_block('#a7adb6', m.modid + 'jsteel', 'riveted'))
    top = {'north': {'texture': '#wood', 'uv': [0, 0, 16, 3]}, 'south': {'texture': '#wood', 'uv': [0, 0, 16, 3]},
           'east': {'texture': '#wood', 'uv': [0, 0, 16, 3]}, 'west': {'texture': '#wood', 'uv': [0, 0, 16, 3]},
           'up': {'texture': '#top'}, 'down': {'texture': '#wood'}}
    els = [([0, 12, 0], [16, 15, 16], top), ([2, 4, 2], [14, 5, 14], '#wood')]
    for (x, z) in ((1, 1), (13, 1), (1, 13), (13, 13)):
        els.append(([x, 0, z], [x + 2, 12, z + 2], '#wood'))
    els += [([10, 15, 9], [14, 18, 12], '#steel'), ([11, 18, 10], [13, 19, 11], '#steel'), ([3, 15, 3], [6, 16, 6], '#steel')]
    table_model = element_model(els, {'top': t('jewelers_table_top'), 'wood': t('jewelers_table_wood'), 'steel': t('jewelers_table_steel'),
                                      'particle': t('jewelers_table_wood')})
    entity_block(m, 'jewelers_table', 'JewelerTableBE',
                 'new LSEntityBlock(LSProps.wood().noOcclusion(), BE_TYPE, LSEntityBlock.Facing.HORIZONTAL, false)',
                 models={'jewelers_table': table_model}, state=horizontal_model_state(m, 'jewelers_table'), mine='axe', lang="Jeweler's Table",
                 desc='Adds gem sockets to tools, weapons and armor,\nand sets cut gems into them')
    m.shaped('jewelers_table', rl('jewelers_table'), ['NSN', 'PTP', 'W W'], {'N': '#forge:nuggets/silver', 'S': 'minecraft:spyglass',
                                                                          'P': '#forge:plates/silver', 'T': 'minecraft:smithing_table',
                                                                          'W': '#minecraft:planks'})

    # ----------------------------------------------------------------- language
    for k, v in {
        'item.ls_lapidary.cut_gem': '%s %s %s',
        'cut.ls_lapidary.cabochon': 'Cabochon', 'cut.ls_lapidary.step': 'Step-Cut', 'cut.ls_lapidary.brilliant': 'Brilliant',
        'grade.ls_lapidary.chipped': 'Chipped', 'grade.ls_lapidary.flawed': 'Flawed', 'grade.ls_lapidary.fine': 'Fine',
        'grade.ls_lapidary.flawless': 'Flawless', 'grade.ls_lapidary.perfect': 'Perfect',
        'tooltip.ls_lapidary.quality': 'Quality %s%% (%s)', 'tooltip.ls_lapidary.in_tools': 'In tools: ',
        'tooltip.ls_lapidary.in_armor': 'In armor: ', 'tooltip.ls_lapidary.socket_hint': 'Set it into a socket at a jeweler\'s table',
        'tooltip.ls_lapidary.sockets': 'Gem sockets (%s):', 'tooltip.ls_lapidary.empty_socket': 'Empty socket',
        'effect.ls_lapidary.attack_damage': '+%s attack damage', 'effect.ls_lapidary.attack_speed': '+%s attack speed',
        'effect.ls_lapidary.max_health': '+%s max health', 'effect.ls_lapidary.armor': '+%s armor',
        'effect.ls_lapidary.toughness': '+%s armor toughness', 'effect.ls_lapidary.move_speed': '+%s speed',
        'effect.ls_lapidary.luck': '+%s luck', 'effect.ls_lapidary.knockback_resist': '+%s knockback resistance',
        'effect.ls_lapidary.swim_speed': '+%s swim speed', 'effect.ls_lapidary.mining_speed': '+%s mining speed',
        'effect.ls_lapidary.aqua_mining': '+%s mining speed under water', 'effect.ls_lapidary.lifesteal': '%s of damage dealt heals you',
        'effect.ls_lapidary.knockback': '+%s knockback', 'effect.ls_lapidary.wisdom': '+%s experience',
        'effect.ls_lapidary.regen': 'heals %s every 5 seconds',
        'gui.ls_lapidary.add_socket': 'Add Socket', 'gui.ls_lapidary.need_tool': 'Put a tool, weapon or piece of armor in the left slot',
        'gui.ls_lapidary.full': 'That item has all the sockets it can take', 'gui.ls_lapidary.need_setting': 'The next socket needs a %s',
        'gui.ls_lapidary.expected': 'Quality about %s (give or take %s)', 'gui.ls_lapidary.shatter': 'Shatter chance: %s%%',
        'gui.ls_lapidary.cut_stats': 'Bonus x%s, cutting time x%s', 'gui.ls_lapidary.charge': 'Abrasive left: %s cuts',
        'gui.ls_lapidary.last': 'Last cut: %s (%s%%)', 'gui.ls_lapidary.shattered': 'The last stone shattered into grit',
        'gui.ls_lapidary.rough_slot': 'Rough gem', 'gui.ls_lapidary.abrasive_slot': 'Abrasive: emery powder, gem grit or diamond dust',
        'gui.ls_lapidary.tool_slot': 'Tool, weapon or armor', 'gui.ls_lapidary.setting_slot': 'Setting: silver, then gold, then platinum',
        'gui.ls_lapidary.gem_slot': 'Cut gem', 'gui.ls_lapidary.socket_empty': 'Empty socket: click to set the gem from the gem slot',
        'gui.ls_lapidary.socket_filled': 'Click to take the gem back out', 'gui.ls_lapidary.socket_locked': 'Add this socket with a %s',
        'lens.category.ls_lapidary.gem_cutting': 'Cutting Wheel', 'lens.category.ls_lapidary.socketing': 'Gem Sockets',
        'lens.category.ls_lapidary.geodes': 'Geode Growth', 'lens.ls_lapidary.abrasive': 'Abrasive: used up every few cuts',
        'lens.ls_lapidary.cuts': 'Cabochon, step-cut or brilliant', 'lens.ls_lapidary.harder': 'Harder cuts: stronger, may shatter',
        'lens.ls_lapidary.per_gem': 'for one Fine step-cut gem', 'lens.ls_lapidary.tools': 'Tools: %s', 'lens.ls_lapidary.armor': 'Armor: %s',
        'lens.ls_lapidary.max_sockets': 'Tools take 3 sockets, armor 2', 'lens.ls_lapidary.random_ticks': 'Grows slowly on its own',
        'lens.ls_lapidary.full_only': 'Only full clusters give gems',
    }.items():
        m.tr(k, v)

    # ----------------------------------------------------------------- codex
    m.codex_intro('Gems grow inside geodes: hollow balls of dark crust and pale lining found deep underground, their insides glittering '
                  'with druse and budding gem rock. Rough stones become cut gems on the cutting wheel, and cut gems give their power to '
                  'tools, weapons and armor once set into sockets at the jeweler\'s table.')
    m.codex_page('Geodes', 'Each of the eight gems has its own geodes. Garnet and topaz lie high, peridot, aquamarine and opal in the middle '
                           'depths, and ruby, sapphire and onyx down in the deepslate. Budding blocks inside slowly grow buds on their open '
                           'faces: small, medium, large and finally a full cluster. Only a full cluster drops rough gems, two with a pickaxe '
                           '(more with Fortune). Budding blocks can only be moved with Silk Touch, so most jewelers build their workshop '
                           'around a geode.', items=[rl('budding_ruby'), rl('ruby_cluster'), rl('rough_ruby'), rl('geode_crust'), rl('geode_lining')])
    m.codex_page('The cutting wheel', 'The cutting wheel runs on 20 FE/t and needs an abrasive beside the rough stone: emery powder lasts two '
                                      'cuts, gem grit four and diamond dust eight, and the finer the abrasive the better the finish. Choose the '
                                      'cut with the buttons on the right:\n\nCabochon - quick and safe, quality around 60, bonus x0.8.\nStep '
                                      'cut - quality around 48, bonus x1.0, a small chance to shatter.\nBrilliant - slow, quality around 36 and '
                                      'very uneven, bonus x1.3, shatters one stone in eight.\n\nOutput augments add to the quality; a shattered '
                                      'stone leaves gem grit behind.', items=[rl('cutting_wheel'), rl('emery_powder'), rl('gem_grit')])
    m.codex_page('Quality', 'A cut gem is Chipped, Flawed, Fine, Flawless or Perfect (95 and up, which shimmers). Its strength is its cut\'s '
                            'bonus times one half plus its quality: a Perfect brilliant is more than twice as strong as a Fine step cut.',
                 items=[rl('ruby'), rl('sapphire'), rl('opal')])
    m.codex_page('Sockets', 'At the jeweler\'s table put an item in the left slot and a setting below it, then press Add Socket. The first '
                            'socket takes a silver setting, the second gold and the third platinum; tools and weapons take three sockets, '
                            'armor two. Put a cut gem in the right slot and click an empty socket to set it; click a set gem to take it back '
                            'out unharmed.', items=[rl('jewelers_table'), rl('silver_setting'), rl('gold_setting'), rl('platinum_setting')])
    m.codex_page('What gems do', 'In tools and weapons: ruby adds attack damage, sapphire attack speed, topaz mining speed, aquamarine mining '
                                 'speed under water, garnet heals you by part of the damage you deal, onyx knocks foes further back, peridot '
                                 'draws more experience and opal brings luck.\n\nIn armor: ruby adds health, sapphire armor, garnet toughness, '
                                 'topaz speed, aquamarine swim speed, onyx knockback resistance, peridot slowly heals you and opal brings '
                                 'luck. Bonuses only count where the item belongs: tools in the main hand, armor when worn.',
                 items=[rl(g) for g in GEMS])
    m.codex_page('Gem blocks and lamps', 'Nine rough stones pack into a block. Gem lamps are a redstone lamp set with four rough stones and '
                                         'glow in the gem\'s colour while powered.', items=[rl('ruby_block'), rl('ruby_lamp'), rl('topaz_lamp')])
    return m
