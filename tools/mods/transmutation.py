from PIL import ImageDraw

from gen.ls import Mod, title, custom_model_machine
from gen import tex

BASE = [
    # earth and stone
    ('#forge:cobblestone', 1), ('minecraft:stone', 1), ('minecraft:dirt', 1), ('minecraft:grass_block', 1), ('minecraft:coarse_dirt', 1),
    ('minecraft:sand', 1), ('minecraft:red_sand', 1), ('minecraft:gravel', 4), ('minecraft:flint', 4), ('minecraft:netherrack', 1),
    ('minecraft:end_stone', 1), ('minecraft:obsidian', 64), ('minecraft:crying_obsidian', 768), ('minecraft:ice', 1), ('minecraft:snowball', 1),
    ('minecraft:clay_ball', 16), ('minecraft:deepslate', 1), ('minecraft:cobbled_deepslate', 1), ('minecraft:tuff', 1), ('minecraft:calcite', 1),
    ('minecraft:andesite', 1), ('minecraft:diorite', 1), ('minecraft:granite', 1), ('minecraft:basalt', 1), ('minecraft:blackstone', 1),
    ('minecraft:soul_sand', 49), ('minecraft:soul_soil', 49), ('minecraft:mud', 1), ('minecraft:moss_block', 16), ('minecraft:mycelium', 2),
    ('minecraft:podzol', 2), ('minecraft:magma_block', 128), ('minecraft:pointed_dripstone', 8), ('minecraft:dripstone_block', 32),
    ('minecraft:sculk', 64), ('minecraft:sculk_vein', 16), ('minecraft:glow_lichen', 8), ('minecraft:end_rod', 1024),
    # plants and food
    ('#minecraft:logs', 32), ('#minecraft:saplings', 32), ('#minecraft:leaves', 1), ('minecraft:cactus', 8), ('minecraft:sugar_cane', 32),
    ('minecraft:wheat', 24), ('minecraft:wheat_seeds', 16), ('minecraft:carrot', 64), ('minecraft:potato', 64), ('minecraft:poisonous_potato', 64),
    ('minecraft:beetroot', 64), ('minecraft:beetroot_seeds', 16), ('minecraft:melon_slice', 16), ('minecraft:pumpkin', 144), ('minecraft:apple', 128),
    ('minecraft:sweet_berries', 16), ('minecraft:glow_berries', 32), ('minecraft:cocoa_beans', 64), ('minecraft:bamboo', 32), ('minecraft:kelp', 1),
    ('minecraft:vine', 8), ('minecraft:lily_pad', 16), ('minecraft:brown_mushroom', 32), ('minecraft:red_mushroom', 32), ('minecraft:nether_wart', 24),
    ('minecraft:chorus_fruit', 192), ('#minecraft:small_flowers', 16), ('#minecraft:tall_flowers', 32), ('minecraft:grass', 1), ('minecraft:fern', 1),
    ('minecraft:dead_bush', 1), ('minecraft:seagrass', 1), ('minecraft:sea_pickle', 16), ('minecraft:crimson_fungus', 32), ('minecraft:warped_fungus', 32),
    ('minecraft:crimson_roots', 1), ('minecraft:warped_roots', 1), ('minecraft:twisting_vines', 8), ('minecraft:weeping_vines', 8),
    ('minecraft:spore_blossom', 64), ('minecraft:azalea', 32), ('minecraft:flowering_azalea', 32), ('minecraft:big_dripleaf', 32),
    ('minecraft:small_dripleaf', 32), ('minecraft:hanging_roots', 8), ('minecraft:pitcher_pod', 64), ('minecraft:torchflower_seeds', 64),
    ('minecraft:egg', 32), ('minecraft:honeycomb', 32), ('minecraft:porkchop', 64), ('minecraft:beef', 64), ('minecraft:chicken', 64),
    ('minecraft:mutton', 64), ('minecraft:rabbit', 64), ('minecraft:cod', 64), ('minecraft:salmon', 64), ('minecraft:tropical_fish', 64),
    ('minecraft:pufferfish', 64), ('minecraft:milk_bucket', 784), ('minecraft:honey_bottle', 48),
    # mob drops
    ('minecraft:string', 12), ('minecraft:feather', 48), ('minecraft:leather', 64), ('minecraft:rabbit_hide', 16), ('minecraft:rotten_flesh', 32),
    ('minecraft:bone', 144), ('minecraft:spider_eye', 128), ('minecraft:gunpowder', 192), ('minecraft:ender_pearl', 1024), ('minecraft:blaze_rod', 1536),
    ('minecraft:ghast_tear', 4096), ('minecraft:slime_ball', 32), ('minecraft:ink_sac', 16), ('minecraft:glow_ink_sac', 64),
    ('minecraft:prismarine_shard', 256), ('minecraft:prismarine_crystals', 512), ('minecraft:nautilus_shell', 1024),
    ('minecraft:heart_of_the_sea', 32768), ('minecraft:phantom_membrane', 256), ('minecraft:shulker_shell', 2048), ('minecraft:nether_star', 139264),
    ('minecraft:rabbit_foot', 128), ('minecraft:scute', 96), ('minecraft:turtle_egg', 192), ('minecraft:totem_of_undying', 16384),
    ('minecraft:echo_shard', 4096), ('minecraft:disc_fragment_5', 2048), ('minecraft:goat_horn', 512), ('minecraft:armadillo_scute', 96),
    ('#minecraft:wool', 48), ('minecraft:white_wool', 48), ('minecraft:saddle', 192), ('minecraft:name_tag', 192), ('minecraft:trident', 16398),
    ('#minecraft:music_discs', 2048), ('minecraft:wither_skeleton_skull', 4096), ('minecraft:skeleton_skull', 256), ('minecraft:zombie_head', 256),
    ('minecraft:creeper_head', 256), ('minecraft:dragon_breath', 512), ('minecraft:dragon_head', 32768), ('minecraft:elytra', 65536),
    ('minecraft:sniffer_egg', 2048), ('minecraft:frogspawn', 32), ('minecraft:ochre_froglight', 64), ('minecraft:verdant_froglight', 64),
    ('minecraft:pearlescent_froglight', 64),
    # minerals
    ('minecraft:coal', 128), ('minecraft:charcoal', 32), ('minecraft:iron_ingot', 256), ('minecraft:raw_iron', 256), ('minecraft:iron_nugget', 28),
    ('minecraft:gold_ingot', 2048), ('minecraft:raw_gold', 2048), ('minecraft:gold_nugget', 227), ('minecraft:copper_ingot', 128),
    ('minecraft:raw_copper', 128), ('minecraft:redstone', 64), ('minecraft:lapis_lazuli', 864), ('minecraft:diamond', 8192),
    ('minecraft:emerald', 16384), ('minecraft:quartz', 256), ('minecraft:glowstone_dust', 384), ('minecraft:amethyst_shard', 64),
    ('minecraft:netherite_scrap', 12288), ('minecraft:ancient_debris', 12288), ('minecraft:netherite_ingot', 57344),
    ('minecraft:lava_bucket', 832), ('minecraft:water_bucket', 768), ('minecraft:bucket', 768), ('minecraft:powder_snow_bucket', 769),
    ('minecraft:budding_amethyst', 4096), ('minecraft:reinforced_deepslate', 4096),
    ('#forge:ingots/tin', 256), ('#forge:ingots/lead', 384), ('#forge:ingots/silver', 1024), ('#forge:ingots/nickel', 512),
    ('#forge:ingots/zinc', 256), ('#forge:ingots/aluminum', 192), ('#forge:ingots/platinum', 4096), ('#forge:ingots/uranium', 2048),
    ('#forge:ingots/cobalt', 4096), ('#forge:ingots/tungsten', 6144), ('#forge:ingots/bronze', 160), ('#forge:ingots/brass', 160),
    ('#forge:ingots/steel', 512), ('#forge:ingots/electrum', 1536), ('#forge:ingots/invar', 341), ('#forge:ingots/constantan', 320),
    ('#forge:ingots/duralumin', 176), ('#forge:ingots/cobalt_steel', 1536),
    ('#forge:raw_materials/tin', 256), ('#forge:raw_materials/lead', 384), ('#forge:raw_materials/silver', 1024), ('#forge:raw_materials/nickel', 512),
    ('#forge:raw_materials/zinc', 256), ('#forge:raw_materials/aluminum', 192), ('#forge:raw_materials/platinum', 4096),
    ('#forge:raw_materials/uranium', 2048), ('#forge:raw_materials/cobalt', 4096), ('#forge:raw_materials/tungsten', 6144),
    ('#forge:dusts/iron', 256), ('#forge:dusts/gold', 2048), ('#forge:dusts/copper', 128), ('#forge:dusts/tin', 256), ('#forge:dusts/lead', 384),
    ('#forge:dusts/silver', 1024), ('#forge:dusts/nickel', 512), ('#forge:dusts/zinc', 256), ('#forge:dusts/aluminum', 192),
    ('#forge:dusts/platinum', 4096), ('#forge:dusts/uranium', 2048), ('#forge:dusts/cobalt', 4096), ('#forge:dusts/tungsten', 6144),
    ('#forge:dusts/coal', 128), ('#forge:dusts/diamond', 8192), ('#forge:dusts/emerald', 16384), ('#forge:dusts/lapis', 864),
    ('#forge:dusts/quartz', 256), ('#forge:dusts/obsidian', 16), ('#forge:dusts/ender_pearl', 1024), ('#forge:dusts/netherite', 6144),
    ('#forge:dusts/sulfur', 32), ('#forge:dusts/bronze', 160), ('#forge:dusts/brass', 160), ('#forge:dusts/steel', 512),
    ('#forge:dusts/electrum', 1536), ('#forge:dusts/invar', 341), ('#forge:dusts/constantan', 320), ('#forge:dusts/duralumin', 176),
    ('#forge:dusts/cobalt_steel', 1536), ('#forge:dusts/wood', 4), ('#forge:coal_coke', 256), ('#forge:tar', 64), ('ls_machina:slag', 8),
    ('ls_metalwork:steel_bloom', 448), ('ls_metalwork:fire_clay', 8), ('ls_toolsmith:grout', 9),
    # transmutation's own fuels
    ('ls_transmutation:kindled_coal', 512), ('ls_transmutation:quickfire', 2048), ('ls_transmutation:eternal_ember', 8192),
]

CELLS = [50_000, 200_000, 800_000, 3_200_000, 12_800_000, 51_200_000]


def glow_top(seed, color):
    img = tex.noise_fill('#2a2438', seed, 0.1)
    d = ImageDraw.Draw(img)
    c = tex.rgb(color)
    d.rectangle([2, 2, 13, 13], outline=c + (255,))
    d.rectangle([5, 5, 10, 10], fill=tex.shade(c, 1.2))
    d.line([(2, 2), (13, 13)], fill=c + (255,))
    d.line([(13, 2), (2, 13)], fill=c + (255,))
    return img


def trim_side(seed, color):
    img = tex.noise_fill('#2a2438', seed, 0.08)
    d = ImageDraw.Draw(img)
    c = tex.rgb(color)
    d.rectangle([0, 0, 15, 15], outline=c + (255,))
    d.rectangle([6, 6, 9, 9], fill=c + (255,))
    return img


def build():
    m = Mod('ls_transmutation', 'Transmutation', 'Every item has an aether value worked out from its recipes. Dissolve items at the exchange table '
                                                  'to learn them and bank their aether, then condense anything you have learned out of that aether. '
                                                  'Collectors gather aether from light, relays carry it, condensers turn junk into anything you choose. '
                                                  'The transmuter\'s stone changes blocks in the world, and umbral and radiant matter make '
                                                  'unbreakable chargeable tools, armor and five rings.', tab='ALCHEMY')
    m.init('TransmutationSetup.init(bus)')
    m.client('com.lodestar.transmutation.client.TransmutationClient.init(bus)')
    rl = m.rl
    t = lambda n: '%s:block/%s' % (m.modid, n)

    rows = ['        {"%s", %dL}' % (k, v) for k, v in BASE]
    m.java_file('BaseValues.java', '\n'.join([
        '// GENERATED by tools/mods/transmutation.py - do not edit', 'package com.lodestar.transmutation;', '',
        '/** Seed aether values: item id or #tag, value. Everything else is derived from recipes. */',
        'public final class BaseValues {', '    private BaseValues() {}', '', '    public static final Object[][] TABLE = {', ',\n'.join(rows), '    };', '}', '']))

    # ----------------------------------------------------------------- core items
    m.item('transmuters_stone', ('gem', '#c0392b', None, '#f5b7b1'), java='new TransmuterStoneItem(new Item.Properties())', jtype='TransmuterStoneItem',
           lang="Transmuter's Stone")
    m.shaped('transmuters_stone', rl('transmuters_stone'), ['GRG', 'RDR', 'GRG'], {'G': 'minecraft:glowstone_dust', 'R': 'minecraft:redstone', 'D': 'minecraft:diamond'})
    for name, color, burn, prev in (('kindled_coal', '#c0392b', 6400, 'minecraft:coal'), ('quickfire', '#e67e22', 25600, rl('kindled_coal')),
                                    ('eternal_ember', '#f1c40f', 102400, rl('quickfire'))):
        m.item(name, ('raw', color), java='new FuelItem(new Item.Properties(), %d)' % burn, jtype='FuelItem')
        m.shapeless(name, rl(name), [prev, prev, prev, prev, rl('transmuters_stone')])
    m.item('umbral_matter', ('orb', '#2a2438', None, '#7d5ba6'), desc='Darker than night, harder than diamond')
    m.item('radiant_matter', ('orb', '#c0392b', None, '#ff8a80'), desc='Burns with a light of its own')
    m.shaped('umbral_matter', rl('umbral_matter'), ['EEE', 'EDE', 'EEE'], {'E': rl('eternal_ember'), 'D': 'minecraft:diamond_block'})
    m.shaped('radiant_matter', rl('radiant_matter'), ['EEE', 'UUU', 'EEE'], {'E': rl('eternal_ember'), 'U': rl('umbral_matter')})
    for n, color in (('umbral_matter_block', '#2a2438'), ('radiant_matter_block', '#a93226')):
        m.block(n, props='LSProps.metal(50.0f)', textures={'block/' + n: tex.metal_block(color, m.modid + n)}, level='diamond', tab='ALCHEMY')
        item = rl(n.replace('_block', ''))
        m.shaped(n, rl(n), ['###', '###', '###'], {'#': item})
        m.shapeless(n.replace('_block', '') + '_from_block', item, [rl(n)], 9)
    for i, cap in enumerate(CELLS, 1):
        n = 'aether_cell_%d' % i
        m.item(n, ('orb', '#7fd4ff', None, '#ffffff'), java='new AetherCellItem(new Item.Properties(), %dL)' % cap, jtype='AetherCellItem',
               lang='Aether Cell %s' % ['I', 'II', 'III', 'IV', 'V', 'VI'][i - 1])
        if i == 1:
            m.shaped(n, rl(n), ['GRG', 'RDR', 'GRG'], {'G': '#forge:ingots/gold', 'R': 'minecraft:redstone_block', 'D': 'minecraft:diamond'})
        else:
            prev = rl('aether_cell_%d' % (i - 1))
            m.shapeless(n, rl(n), [prev, prev, prev, prev, rl('transmuters_stone')])
    m.item('exchange_tablet', ('tablet', '#2a2438', None, '#7fd4ff'), java='new ExchangeTabletItem(new Item.Properties())', jtype='ExchangeTabletItem')
    m.shaped('exchange_tablet', rl('exchange_tablet'), ['UTU', 'TST', 'UTU'], {'U': rl('umbral_matter'), 'T': rl('exchange_table'), 'S': 'minecraft:diamond'})

    # transmutation crafting equivalences
    eq = [('minecraft:iron_ingot', 8, 'minecraft:gold_ingot', 1), ('minecraft:gold_ingot', 4, 'minecraft:diamond', 1),
          ('minecraft:diamond', 2, 'minecraft:emerald', 1), ('minecraft:coal', 2, 'minecraft:iron_ingot', 1),
          ('minecraft:copper_ingot', 2, 'minecraft:iron_ingot', 1), ('minecraft:redstone', 4, 'minecraft:iron_ingot', 1),
          ('minecraft:flint', 1, 'minecraft:gravel', 1), ('minecraft:clay_ball', 4, 'minecraft:iron_ingot', 1)]
    for i, (a, na, b, nb) in enumerate(eq):
        an, bn = a.split(':')[1], b.split(':')[1]
        if na <= 8:
            m.shapeless('transmute_%s_to_%s' % (an, bn), b, [a] * na + [rl('transmuters_stone')], nb)
        m.shapeless('transmute_%s_to_%s' % (bn, an), a, [b] * nb + [rl('transmuters_stone')], na)

    # ----------------------------------------------------------------- blocks
    table_model = {'parent': 'minecraft:block/block', 'textures': {'top': t('exchange_table_top'), 'side': t('exchange_table_side'), 'particle': t('exchange_table_side')},
                   'elements': [{'from': [0, 0, 0], 'to': [16, 4, 16], 'faces': {
                       'up': {'texture': '#top'}, 'down': {'texture': '#side'}, 'north': {'texture': '#side'}, 'south': {'texture': '#side'},
                       'east': {'texture': '#side'}, 'west': {'texture': '#side'}}}]}
    top = glow_top(m.modid + 'ett', '#9b59b6')
    m.block('exchange_table', java='new ExchangeTableBlock(LSProps.stone(10.0f, 1200.0f).noOcclusion())', jtype='ExchangeTableBlock', tab='ALCHEMY',
            models={'exchange_table': table_model}, textures={'block/exchange_table_top': top, 'block/exchange_table_side': trim_side(m.modid + 'ets', '#9b59b6')},
            desc='Dissolve items into aether and condense what you have learned')
    m.shaped('exchange_table', rl('exchange_table'), ['OSO', 'STS', 'OSO'], {'O': 'minecraft:obsidian', 'S': 'minecraft:stone', 'T': rl('transmuters_stone')})

    def aether_block(name, be_class, args, color, desc, lang=None):
        side, topn = name + '_side', name + '_top'
        custom_model_machine(m, name, 'new %s(BE_TYPE, pos, state, %s)' % (be_class, args), be_class,
                             {'block/' + side: trim_side(m.modid + name, color), 'block/' + topn: glow_top(m.modid + name, color)},
                             top=topn, side=side, bottom=side, front=side, tab='ALCHEMY', desc=desc, lang=lang)
    for i, (rate, color, center) in enumerate([(4, '#f1c40f', 'minecraft:diamond_block'), (12, '#e67e22', rl('umbral_matter')),
                                               (40, '#e74c3c', rl('radiant_matter'))], 1):
        roman = ['I', 'II', 'III'][i - 1]
        aether_block('aether_collector_%d' % i, 'CollectorBE', str(rate), color,
                     'Gathers %d aether per second in full light,\nand refines fuel placed inside' % rate, 'Aether Collector %s' % roman)
        prev = 'minecraft:furnace' if i == 1 else rl('aether_collector_%d' % (i - 1))
        m.shaped('aether_collector_%d' % i, rl('aether_collector_%d' % i), ['GLG', 'GCG', 'GPG'],
                 {'G': 'minecraft:glowstone', 'L': '#forge:glass', 'C': center, 'P': prev})
        cap, tr = [(100_000, 64), (1_000_000, 192), (10_000_000, 640)][i - 1]
        aether_block('aether_relay_%d' % i, 'RelayBE', '%dL, %dL' % (cap, tr), color,
                     'Stores aether from collectors and passes it to condensers.\nDissolves items and charges aether cells.', 'Aether Relay %s' % roman)
        prevr = 'minecraft:diamond_block' if i == 1 else rl('aether_relay_%d' % (i - 1))
        m.shaped('aether_relay_%d' % i, rl('aether_relay_%d' % i), ['OGO', 'OCO', 'OPO'],
                 {'O': 'minecraft:obsidian', 'G': '#forge:glass', 'C': center, 'P': prevr})
    aether_block('aether_condenser', 'CondenserBE', '4', '#7fd4ff', 'Condenses copies of a target item from whatever you feed it')
    aether_block('aether_condenser_2', 'CondenserBE', '32', '#ff8a80', 'A much faster aether condenser', 'Aether Condenser II')
    m.shaped('aether_condenser', rl('aether_condenser'), ['ODO', 'DCD', 'ODO'], {'O': 'minecraft:obsidian', 'D': 'minecraft:diamond', 'C': 'minecraft:chest'})
    m.shaped('aether_condenser_2', rl('aether_condenser_2'), ['RUR', 'UCU', 'RUR'], {'R': rl('radiant_matter'), 'U': rl('umbral_matter'), 'C': rl('aether_condenser')})

    # ----------------------------------------------------------------- matter tools and armor
    for mat, tier, color, accent, handle in (('umbral', 'UMBRAL', '#2a2438', '#7d5ba6', 'minecraft:diamond'),
                                             ('radiant', 'RADIANT', '#a93226', '#ff8a80', rl('umbral_matter'))):
        M = rl(mat + '_matter')
        tools = {'pickaxe': ('Pickaxe', '1, -2.8f', ['MMM', ' H ', ' H ']), 'shovel': ('Shovel', '1.5f, -3.0f', ['M', 'H', 'H']),
                 'axe': ('Axe', '6.0f, -3.0f', ['MM', 'MH', ' H']), 'sword': ('Sword', '3, -2.4f', ['M', 'M', 'H'])}
        for tool, (cls, args, pattern) in tools.items():
            n = '%s_%s' % (mat, tool)
            m.item(n, (tool, color, '#3a3a3a', accent), java='new MatterTools.%s(MatterTools.%s, %s, new Item.Properties())' % (cls, tier, args),
                   jtype='MatterTools.%s' % cls, model='handheld', lang='%s %s' % (title(mat), title(tool)))
            keys = {'M': M, 'H': handle}
            if mat == 'radiant':
                first = next(i for i, r in enumerate(pattern) if 'H' in r)
                pattern = [r.replace('H', 'P', 1) if i == first else r for i, r in enumerate(pattern)]
                keys['P'] = rl('umbral_' + tool)
            m.shaped(n, rl(n), pattern, {k: v for k, v in keys.items() if any(k in r for r in pattern)})
        for slot, (typ, pattern) in {'helmet': ('HELMET', ['MMM', 'M M']), 'chestplate': ('CHESTPLATE', ['M M', 'MMM', 'MMM']),
                                     'leggings': ('LEGGINGS', ['MMM', 'M M', 'M M']), 'boots': ('BOOTS', ['M M', 'M M'])}.items():
            n = '%s_%s' % (mat, slot)
            m.item(n, (slot, color, None, accent), java='new MatterArmor(MatterArmor.%s, net.minecraft.world.item.ArmorItem.Type.%s, new Item.Properties())' % (tier, typ),
                   jtype='MatterArmor', lang='%s %s' % (title(mat), title(slot)))
            if mat == 'umbral':
                m.shaped(n, rl(n), pattern, {'M': M})
            else:
                m.shaped(n, rl(n), ['MMM', 'MPM', 'MMM'], {'M': M, 'P': rl('umbral_' + slot)})
        for layer in (1, 2):
            m.asset('textures/models/armor/%s_layer_%d.png' % (mat, layer), tex.armor_layer(color, layer, m.modid + mat))

    # ----------------------------------------------------------------- rings
    m.item('aether_band', ('ring', '#9aa3ad'), desc='The base of every transmutation ring')
    m.shaped('aether_band', rl('aether_band'), ['III', 'ILI', 'III'], {'I': '#forge:ingots/iron', 'L': 'minecraft:lava_bucket'})
    rings = {'gale': ('GALE', '#ecf0f1', 'minecraft:feather', 'Ring of the Gale'), 'ember': ('EMBER', '#e74c3c', 'minecraft:blaze_powder', 'Ember Ring'),
             'frost': ('FROST', '#85c1e9', 'minecraft:snow_block', 'Frost Ring'), 'verdant': ('VERDANT', '#58d68d', 'minecraft:moss_block', 'Verdant Ring'),
             'void': ('VOID', '#5b2c6f', 'minecraft:ender_pearl', 'Void Ring')}
    for k, (kind, color, item, nice) in rings.items():
        n = '%s_ring' % k
        m.item(n, ('ring', '#9aa3ad', None, color), java='new RingItem(new Item.Properties(), RingItem.Kind.%s)' % kind, jtype='RingItem', lang=nice)
        m.shaped(n, rl(n), ['XUX', 'XBX', 'XXX'], {'X': item, 'U': rl('umbral_matter'), 'B': rl('aether_band')})

    for k, v in {
        'tooltip.ls_transmutation.value': 'Aether: %s', 'tooltip.ls_transmutation.cell': 'Aether: %s / %s',
        'tooltip.ls_transmutation.stone': 'Use on blocks to transmute them (sneak: 3x3)\nCatalyses crafting transmutations and is never used up',
        'tooltip.ls_transmutation.charge': 'Charge: %s / %s', 'tooltip.ls_transmutation.charge_hint': 'Press the charge key (V) to change; costs aether per extra block',
        'tooltip.ls_transmutation.umbral_set': 'Full set: no fall damage', 'tooltip.ls_transmutation.radiant_set': 'Full set: no fall damage, fire immunity, water breathing',
        'tooltip.ls_transmutation.active': 'Active',
        'tooltip.ls_transmutation.ring.gale': 'Use to toggle flight (16 aether per second of flying)\nNo fall damage while active',
        'tooltip.ls_transmutation.ring.ember': 'Fire resistance while carried\nUse to shoot a fireball (32 aether)',
        'tooltip.ls_transmutation.ring.frost': 'Use to freeze water and lava around you\nYou never freeze while carrying it',
        'tooltip.ls_transmutation.ring.verdant': 'Use to toggle: grows crops and saplings around you (8 aether per second)',
        'tooltip.ls_transmutation.ring.void': 'Use to toggle an item magnet\nSneak-use to blink forward (64 aether)',
        'msg.ls_transmutation.charge': 'Charge %s / %s', 'msg.ls_transmutation.no_aether': 'Not enough aether in your cells',
        'msg.ls_transmutation.ring_on': '%s on', 'msg.ls_transmutation.ring_off': '%s off', 'msg.ls_transmutation.too_poor': 'Not enough aether',
        'gui.ls_transmutation.search': 'Search known items', 'gui.ls_transmutation.aether': 'Aether: %s', 'gui.ls_transmutation.dissolve': 'Dissolve',
        'gui.ls_transmutation.cell': 'Cell', 'gui.ls_transmutation.fill': 'Fill', 'gui.ls_transmutation.empty': 'Drain',
        'gui.ls_transmutation.value': 'Aether value: %s', 'gui.ls_transmutation.afford': 'You can afford %s',
        'gui.ls_transmutation.buy_hint': 'Click: 1, right-click: 8, shift: a stack',
        'gui.ls_transmutation.stored': 'Aether: %s / %s', 'gui.ls_transmutation.collecting': 'Collecting %s per second',
        'gui.ls_transmutation.condenser': 'Aether %s of %s', 'key.ls_transmutation.charge': 'Charge transmutation tool',
        'lens.category.ls_transmutation.world_transmutation': 'World Transmutation',
    }.items():
        m.tr(k, v)

    m.codex_intro('Everything has a price in aether. Values start from a table of raw materials and are worked out for everything else '
                  'from its recipes, so an iron pickaxe is worth three iron ingots and two sticks. Hover any item to see its value.')
    m.codex_page('The exchange table', 'Put items in the dissolve slot of an exchange table: they turn into aether stored with you, and '
                                       'you learn them. The grid shows everything you have learned; click an item to buy it back with '
                                       'aether. An aether cell in the cell slot can be filled from your store or drained into it.',
                 items=[rl('exchange_table'), rl('transmuters_stone'), rl('aether_cell_1')])
    m.codex_page('Collectors, relays and condensers', 'Aether collectors gather aether from light and pass it to relays and condensers '
                                                      'beside them. Set a target in a condenser and it condenses copies from aether, '
                                                      'dissolving whatever you put in its input grid. Collectors also refine fuel: charcoal '
                                                      'into coal, coal into redstone, all the way up to eternal ember.',
                 items=[rl('aether_collector_1'), rl('aether_relay_1'), rl('aether_condenser')])
    m.codex_page('Matter', 'Nine eternal embers around a block of diamond make umbral matter; umbral and eternal embers make radiant '
                           'matter. Matter tools never break and can be charged with the charge key to dig a wider area, paying aether '
                           'from your cells for each extra block. A full set of matter armor stops fall damage.',
                 items=[rl('umbral_matter'), rl('radiant_matter'), rl('umbral_pickaxe'), rl('radiant_pickaxe')])
    m.codex_page('Rings', 'Rings are built on an aether band and powered by the cells you carry: flight, fire, frost, growth and the void.',
                 items=[rl(k + '_ring') for k in rings])
    return m
