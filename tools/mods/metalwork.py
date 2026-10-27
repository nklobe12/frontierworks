from PIL import Image, ImageDraw

from gen.ls import Mod, title, const, entity_block, horizontal_model_state, element_model, stairs_slab_wall, recipe_kind, fuel_machine
from gen import tex
from mods._materials import ALLOYS, PART_METALS, METALS, VANILLA_METALS, metal_color, ingot_item, nugget_item

WIRE_METALS = ['copper', 'gold', 'silver', 'aluminum', 'electrum', 'constantan', 'tungsten']
HARD = {'platinum': 1000, 'tungsten': 1100, 'cobalt': 1000, 'cobalt_steel': 1000, 'steel': 800}
BRICK = '#9c5a3c'


def brick_front(seed, lit):
    img = tex.bricks(BRICK, seed, '#d8c8b0')
    d = ImageDraw.Draw(img)
    d.rectangle([4, 6, 11, 15], fill=(28, 20, 18, 255))
    d.rectangle([5, 5, 10, 5], fill=(28, 20, 18, 255))
    if lit:
        for x in range(5, 11):
            h = 3 + (tex.hsh(seed, x) % 5)
            for y in range(15 - h, 16):
                t = (y - (15 - h)) / max(1, h)
                img.putpixel((x, y), (255, int(90 + 150 * t), int(20 + 40 * t), 255))
    return img


def grate(seed, lit):
    img = tex.noise_fill('#2e2a28', seed, 0.1)
    for x in range(16):
        for y in range(16):
            if x % 4 == 0 or y % 4 == 0:
                img.putpixel((x, y), (70, 64, 60, 255))
            elif lit and tex.rnd(seed, x, y) < 0.5:
                img.putpixel((x, y), (255, 120 + int(100 * tex.rnd(seed, y, x)), 30, 255))
    return img


def leather(seed):
    img = tex.noise_fill('#7a4a28', seed, 0.1)
    for y in (3, 7, 11):
        for x in range(16):
            img.putpixel((x, y), (90, 56, 30, 255))
    return img


def brick_block_models(m, name, front, top_ref, lit=True):
    t = lambda n: '%s:block/%s' % (m.modid, n)
    mk = lambda fr: {'parent': 'minecraft:block/orientable_with_bottom', 'textures': {
        'top': top_ref, 'bottom': t('fire_bricks'), 'side': t('fire_bricks'), 'front': t(fr)}}
    models = {name: mk(front)}
    if lit:
        models[name + '_on'] = mk(front + '_on')
    return models


def build():
    m = Mod('ls_metalwork', 'Metalwork', 'Work metal by hand: heat it in a smith\'s forge (bellows make it hotter), then shape it on the '
                                          'smithing anvil with hammer strikes while it glows. Hot metal burns bare hands, so carry it with '
                                          'tongs or quench it in a cauldron. Smelt steel in a fire-brick bloomery, mix bronze, brass, '
                                          'electrum and five more alloys in the alloy kiln, and make plates, rods, gears and wires.', tab='MATERIALS')
    m.init('MetalworkSetup.init(bus)')
    m.client('com.lodestar.metalwork.client.MetalworkClient.init(bus)')
    rl = m.rl

    # ----------------------------------------------------------------- alloys
    for name, (color, recipe, out) in ALLOYS.items():
        m.item(name + '_ingot', ('ingot', color), tags=['forge:ingots/' + name, 'forge:ingots'])
        m.item(name + '_nugget', ('nugget', color), tags=['forge:nuggets/' + name, 'forge:nuggets'])
        m.item(name + '_dust', ('dust', color), tags=['forge:dusts/' + name, 'forge:dusts'])
        m.block(name + '_block', props='LSProps.metal(5.0f)', textures={'block/%s_block' % name: tex.metal_block(color, m.modid + name)},
                level='iron' if name in ('steel', 'cobalt_steel') else 'stone',
                tags=['forge:storage_blocks/' + name, 'forge:storage_blocks', 'minecraft:beacon_base_blocks'],
                item_tags=['forge:storage_blocks/' + name, 'forge:storage_blocks'], lang='Block of ' + title(name))
        m.shaped(name + '_block', rl(name + '_block'), ['###', '###', '###'], {'#': '#forge:ingots/' + name})
        m.shapeless(name + '_ingot_from_block', rl(name + '_ingot'), ['#forge:storage_blocks/' + name], 9)
        m.shaped(name + '_ingot_from_nuggets', rl(name + '_ingot'), ['###', '###', '###'], {'#': '#forge:nuggets/' + name})
        m.shapeless(name + '_nugget', rl(name + '_nugget'), ['#forge:ingots/' + name], 9)
        m.smelt(name + '_ingot_from_dust', '#forge:dusts/' + name, rl(name + '_ingot'), 0.3, time=150)
        m.tag('items', 'forge:ingots', '#forge:ingots/' + name)

    alloying = recipe_kind(m, 'alloying')
    for name, (color, recipe, out) in ALLOYS.items():
        if not recipe:
            continue
        (a, na), (b, nb) = list(recipe.items())
        for form in ('ingots', 'dusts'):
            m.mrecipe('ls_metalwork:alloying', '%s_from_%s' % (name, form),
                      [('#forge:%s/%s' % (form, a), na), ('#forge:%s/%s' % (form, b), nb)], [(rl(name + '_ingot'), out)], time=200, energy=0)
    fuel_machine(m, 'alloy_kiln', alloying, inputs=2, outputs=1, tier='steam', glyph='flame',
                 desc='Burns fuel to melt two metals together into an alloy')

    # ----------------------------------------------------------------- parts
    for metal in PART_METALS:
        color = metal_color(metal)
        ing = '#forge:ingots/' + metal
        m.item(metal + '_plate', ('plate', color), tags=['forge:plates/' + metal, 'forge:plates'])
        m.item(metal + '_rod', ('rod', color), tags=['forge:rods/' + metal, 'forge:rods'])
        m.item(metal + '_gear', ('gear', color), tags=['forge:gears/' + metal, 'forge:gears'])
        heat = HARD.get(metal, 600)
        m.shapeless(metal + '_plate_by_hand', rl(metal + '_plate'), [rl('smithing_hammer'), ing, ing])
        m.shapeless(metal + '_rod_by_hand', rl(metal + '_rod'), [rl('smithing_hammer'), ing])
        m.shaped(metal + '_gear', rl(metal + '_gear'), [' P ', 'PNP', ' P '], {'P': '#forge:plates/' + metal, 'N': '#forge:nuggets/' + metal})
        smith(m, metal + '_plate', ing, 'plate', 3, heat, rl(metal + '_plate'), 1)
        smith(m, metal + '_rod', ing, 'rod', 2, heat, rl(metal + '_rod'), 2)
        smith(m, metal + '_gear', ing, 'gear', 5, heat, rl(metal + '_gear'), 1)
        if metal in WIRE_METALS:
            m.item(metal + '_wire', ('wire', color), tags=['forge:wires/' + metal, 'forge:wires'])
            m.shapeless(metal + '_wire_by_hand', rl(metal + '_wire'), [rl('wire_cutters'), '#forge:plates/' + metal], 2)
            smith(m, metal + '_wire', ing, 'wire', 4, heat, rl(metal + '_wire'), 3)
    smith(m, 'steel_from_bloom', rl('steel_bloom'), 'ingot', 8, 900, rl('steel_ingot'), 1)
    m.item('steel_bloom', ('bloom', '#5e5a58'), desc='Spongy iron full of slag. Hammer it hot on the anvil to make steel.')

    # ----------------------------------------------------------------- tools
    m.item('smithing_hammer', ('hammer', '#8a8f98', '#5a3a22'), java='new HammerItem(new Item.Properties().durability(320), "tooltip.ls_metalwork.hammer")',
           jtype='HammerItem', model='handheld', tab='TOOLS')
    m.item('tongs', ('tongs', '#6f747c', '#5a3a22'), java='new Item(new Item.Properties().stacksTo(1))', tab='TOOLS',
           desc='Hold in your off hand to carry hot metal safely')
    m.item('wire_cutters', ('cutters', '#a0a6ae', '#b03a2e'), java='new HammerItem(new Item.Properties().durability(200), "tooltip.ls_metalwork.cutters")',
           jtype='HammerItem', model='handheld', tab='TOOLS')
    m.shaped('smithing_hammer', rl('smithing_hammer'), [' II', ' SI', 'S  '], {'I': '#forge:ingots/iron', 'S': '#forge:rods/wooden'})
    m.shapeless('tongs', rl('tongs'), ['#forge:ingots/iron', '#forge:ingots/iron', '#forge:rods/wooden', '#forge:rods/wooden'])
    m.shaped('wire_cutters', rl('wire_cutters'), ['I I', ' I ', 'S S'], {'I': '#forge:ingots/iron', 'S': '#forge:rods/wooden'})

    # ----------------------------------------------------------------- fire bricks
    m.item('fire_clay', ('clay_lump', '#c9a98a'), desc='Clay tempered with sand and gravel; fire it into fire bricks')
    m.item('fire_brick', ('brick', BRICK))
    m.shapeless('fire_clay', rl('fire_clay'), ['minecraft:clay_ball', 'minecraft:clay_ball', '#minecraft:sand', 'minecraft:gravel'], 4)
    m.smelt('fire_brick', rl('fire_clay'), rl('fire_brick'), 0.2, blast=False)
    m.block('fire_bricks', props='LSProps.stone(2.5f, 9.0f)', textures={'block/fire_bricks': tex.bricks(BRICK, m.modid + 'fb', '#d8c8b0')},
            desc='Refractory bricks for forges and bloomeries')
    m.shaped('fire_bricks', rl('fire_bricks'), ['##', '##'], {'#': rl('fire_brick')})
    stairs_slab_wall(m, 'fire_bricks', '%s:block/fire_bricks' % m.modid, tab='MATERIALS', props='LSProps.stone(2.5f, 9.0f)')

    # ----------------------------------------------------------------- anvil, forge, bellows, bloomery
    steel_dark = tex.metal_block('#4a4d55', m.modid + 'anvil')
    anvil_top = tex.metal_block('#5c6069', m.modid + 'anviltop', 'plain')
    t = lambda n: '%s:block/%s' % (m.modid, n)
    anvil_model = element_model([
        ([2, 0, 2], [14, 4, 14], '#side'), ([4, 4, 3], [12, 5, 13], '#side'), ([6, 5, 4], [10, 10, 12], '#side'),
        ([3, 10, 0], [13, 16, 16], {'north': {'texture': '#side'}, 'south': {'texture': '#side'}, 'east': {'texture': '#side'},
                                     'west': {'texture': '#side'}, 'up': {'texture': '#top'}, 'down': {'texture': '#side'}})],
        {'side': t('smithing_anvil'), 'top': t('smithing_anvil_top'), 'particle': t('smithing_anvil')})
    entity_block(m, 'smithing_anvil', 'AnvilBE',
                 'new LSEntityBlock(LSProps.metal(5.0f).noOcclusion(), BE_TYPE, LSEntityBlock.Facing.HORIZONTAL, false).noMenu().shape(Block.box(2, 0, 0, 14, 16, 16))',
                 tab='MATERIALS', models={'smithing_anvil': anvil_model}, state=horizontal_model_state(m, 'smithing_anvil'),
                 textures={'block/smithing_anvil': steel_dark, 'block/smithing_anvil_top': anvil_top},
                 desc='Put hot metal on it, sneak-use to pick a shape,\nthen strike it with a smithing hammer')
    m.shaped('smithing_anvil', rl('smithing_anvil'), ['BBB', ' I ', 'III'], {'B': '#forge:storage_blocks/iron', 'I': '#forge:ingots/iron'})

    # forge (machine framework, fuel based)
    m.field('public static final RegistryObject<BlockEntityType<ForgeBE>> SMITHS_FORGE_BE = REG.blockEntity("smiths_forge", (pos, state) -> new ForgeBE(Content.SMITHS_FORGE_BE.get(), pos, state), Content.SMITHS_FORGE);')
    m.block('smiths_forge', java='new com.lodestar.core.machine.MachineBlock(LSProps.stone(3.0f).lightLevel(s -> s.getValue(LSEntityBlock.LIT) ? 13 : 0), () -> Content.SMITHS_FORGE_BE.get())',
            jtype='com.lodestar.core.machine.MachineBlock', tab='MATERIALS',
            models=brick_block_models(m, 'smiths_forge', 'smiths_forge_front', t('smiths_forge_top')),
            state=horizontal_model_state(m, 'smiths_forge', 'smiths_forge_on'),
            textures={'block/smiths_forge_front': brick_front(m.modid + 'sf', False), 'block/smiths_forge_front_on': brick_front(m.modid + 'sf', True),
                      'block/smiths_forge_top': grate(m.modid + 'sft', True)},
            desc='Burns fuel to heat metal for the anvil.\nBellows make it hotter.', lang="Smith's Forge")
    m.shaped('smiths_forge', rl('smiths_forge'), ['FFF', 'FCF', 'SSS'], {'F': rl('fire_brick'), 'C': 'minecraft:campfire', 'S': 'minecraft:stone_bricks'})

    bellows_model = element_model([
        ([0, 0, 0], [16, 2, 16], '#wood'), ([1, 2, 1], [15, 9, 15], '#leather'), ([0, 9, 0], [16, 11, 16], '#wood'),
        ([6, 3, -3], [10, 7, 1], '#iron')], {'wood': 'minecraft:block/oak_planks', 'leather': t('bellows_leather'),
                                               'iron': 'minecraft:block/iron_block', 'particle': 'minecraft:block/oak_planks'})
    m.block('bellows', java='new BellowsBlock(LSProps.wood().noOcclusion())', jtype='BellowsBlock', tab='MATERIALS',
            models={'bellows': bellows_model}, state={'variants': {
                'facing=%s,powered=%s' % (f, pw): dict({'model': t('bellows')}, **({'y': r} if r else {}))
                for f, r in {'north': 0, 'east': 90, 'south': 180, 'west': 270}.items() for pw in ('true', 'false')}},
            textures={'block/bellows_leather': leather(m.modid + 'bl')}, mine='axe',
            desc='Use it or pulse it with redstone to blow air\ninto the forge or bloomery it faces')
    m.shaped('bellows', rl('bellows'), ['WWW', 'LLL', 'WIW'], {'W': '#minecraft:planks', 'L': 'minecraft:leather', 'I': '#forge:ingots/iron'})

    m.field('public static final RegistryObject<BlockEntityType<BloomeryBE>> BLOOMERY_BE = REG.blockEntity("bloomery", (pos, state) -> new BloomeryBE(Content.BLOOMERY_BE.get(), pos, state), Content.BLOOMERY);')
    m.block('bloomery', java='new com.lodestar.core.machine.MachineBlock(LSProps.stone(3.0f).lightLevel(s -> s.getValue(LSEntityBlock.LIT) ? 11 : 0), () -> Content.BLOOMERY_BE.get())',
            jtype='com.lodestar.core.machine.MachineBlock', tab='MATERIALS',
            models=brick_block_models(m, 'bloomery', 'bloomery_front', t('fire_bricks')),
            state=horizontal_model_state(m, 'bloomery', 'bloomery_on'),
            textures={'block/bloomery_front': brick_front(m.modid + 'bm', False), 'block/bloomery_front_on': brick_front(m.modid + 'bm', True)},
            desc='Controller of the bloomery: build a 3x3 chimney\nof fire bricks four blocks tall behind it')
    m.shaped('bloomery', rl('bloomery'), ['FFF', 'F F', 'FUF'], {'F': rl('fire_bricks'), 'U': 'minecraft:furnace'})
    m.tag('items', 'ls_metalwork:bloomery_ore', 'minecraft:raw_iron', '#forge:ores/iron')
    m.tag('items', 'ls_metalwork:bloomery_fuel', 'minecraft:charcoal', 'minecraft:coal', '#forge:coal_coke')

    m.shaped('alloy_kiln', rl('alloy_kiln'), ['FFF', 'FUF', 'FFF'], {'F': rl('fire_brick'), 'U': 'minecraft:furnace'})

    for k, v in {
        'shape.ls_metalwork.plate': 'Plate', 'shape.ls_metalwork.rod': 'Rods', 'shape.ls_metalwork.gear': 'Gear',
        'shape.ls_metalwork.wire': 'Wires', 'shape.ls_metalwork.ingot': 'Ingot',
        'msg.ls_metalwork.not_workable': 'That can\'t be forged on an anvil',
        'msg.ls_metalwork.shape': 'Forging: %s', 'msg.ls_metalwork.too_cold': 'Too cold to forge - heat it to at least %s C',
        'msg.ls_metalwork.too_hot': 'Hot metal burns! Hold tongs in your off hand',
        'tooltip.ls_metalwork.heat': 'Heat: %s C (%s)', 'tooltip.ls_metalwork.heat.cold': 'too cold to forge',
        'tooltip.ls_metalwork.heat.workable': 'workable', 'tooltip.ls_metalwork.heat.ideal': 'ideal: strikes count double',
        'tooltip.ls_metalwork.hammer': 'Strike hot metal on a smithing anvil.\nAlso shapes ingots into plates and rods in a crafting grid.',
        'tooltip.ls_metalwork.cutters': 'Cuts plates into wires in a crafting grid',
        'gui.ls_metalwork.forge_temp': 'Forge: %s C', 'gui.ls_metalwork.bloomery.incomplete': 'Chimney incomplete',
        'gui.ls_metalwork.bloomery.ready': 'Chimney complete',
        'lens.category.ls_metalwork.smithing': 'Smithing Anvil', 'lens.category.ls_metalwork.alloying': 'Alloy Kiln',
        'lens.category.ls_metalwork.bloomery': 'Bloomery', 'lens.ls_metalwork.strikes': '%s strikes',
        'lens.ls_metalwork.min_heat': 'at %s C or hotter', 'lens.ls_metalwork.bloomery_time': '30 seconds',
        'lens.ls_metalwork.bloomery_hint': 'needs the full chimney',
    }.items():
        m.tr(k, v)

    m.codex_intro('Before machines, metal is worked by hand. Heat it in a forge until it glows, shape it on an anvil with a hammer, '
                  'and melt metals together into alloys in a kiln.')
    m.codex_page('Heat', 'Metal heated in the smith\'s forge carries its heat with it. Below 600 C it is too cold to work; from 600 C '
                         'it is workable and from 900 C every strike counts double. Hot metal cools in your inventory and burns your hands '
                         'unless you hold tongs in your off hand. Use hot metal on a water cauldron to quench it.\n\nBellows facing the '
                         'forge, pumped by hand or with redstone pulses, push it up to 1550 C, which the toughest metals need.',
                 items=[rl('smiths_forge'), rl('bellows'), rl('tongs')])
    m.codex_page('The smithing anvil', 'Place a hot ingot on the anvil, then sneak-use it with an empty hand to choose what to make: a '
                                       'plate, two rods, a gear or three wires. Strike it with the smithing hammer until it is done. Each '
                                       'strike cools the metal, so a gear may need a second trip to the forge. The anvil gets more out of an '
                                       'ingot than hammering in a crafting grid.', items=[rl('smithing_anvil'), rl('smithing_hammer')])
    m.codex_page('The bloomery', 'Steel starts in the bloomery: a chimney of fire bricks four blocks tall with the bloomery controller at '
                                 'the front of its base. Feed it iron ore or raw iron and two charcoal or coal per bloom. After half a minute it '
                                 'leaves a hot steel bloom; hammer it into a steel ingot on the anvil while it still glows.',
                 items=[rl('bloomery'), rl('fire_bricks'), rl('steel_bloom')], multiblock='ls_metalwork:bloomery')
    m.codex_page('Alloys', 'The alloy kiln burns fuel to melt two metals into an alloy: bronze, brass, electrum, invar, constantan, '
                           'duralumin and cobalt steel. Dusts work as well as ingots.', items=[rl('alloy_kiln')])
    return m


def smith(m, name, ingredient, shape, strikes, heat, result, count):
    from gen.ls import I, mc
    m.recipe('smithing/' + name, {'type': 'ls_metalwork:smithing', 'input': I(ingredient), 'shape': shape, 'strikes': strikes,
                                  'min_heat': heat, 'result': {'item': mc(result), 'count': count}})
