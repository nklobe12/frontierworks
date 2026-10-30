from PIL import Image, ImageDraw

from gen.ls import Mod, I
from gen import tex

TIERS = {
    # tier: (trim colour, lang, rows, upgrades, recipe pattern, keys)
    'leather': ('#5a3a1e', 'Leather Backpack', 3, 1),
    'iron': ('#c8c8c8', 'Iron Backpack', 4, 2),
    'gold': ('#f2c94c', 'Golden Backpack', 5, 3),
    'diamond': ('#5fe0d6', 'Diamond Backpack', 6, 4),
    'netherite': ('#5a4e55', 'Netherite Backpack', 8, 5),
}

UPGRADES = {
    # name: (kind, icon colour, lang, tooltip, pattern, keys)
    'magnet_upgrade': ('MAGNET', '#d64541', 'Magnet Upgrade', 'Pulls loose items within six blocks to you',
                       ['I I', 'IUI', ' R '], {'I': '#forge:ingots/iron', 'U': 'ls_backpacks:upgrade_base', 'R': 'minecraft:redstone'}),
    'pickup_upgrade': ('PICKUP', '#8e7cc3', 'Pickup Upgrade', 'Items of a kind the backpack already holds\ngo straight into it when picked up',
                       ['H', 'U'], {'H': 'minecraft:hopper', 'U': 'ls_backpacks:upgrade_base'}),
    'feeding_upgrade': ('FEEDING', '#e8a33d', 'Feeding Upgrade', 'Eats food from the backpack when you are hungry\n(skips food with harmful effects)',
                        [' G ', 'BUB'], {'G': 'minecraft:golden_carrot', 'B': 'minecraft:bread', 'U': 'ls_backpacks:upgrade_base'}),
    'refill_upgrade': ('REFILL', '#4aa564', 'Refill Upgrade', 'Tops up hotbar stacks that run below half\nfrom matching items in the backpack',
                       [' P ', 'RUR'], {'P': 'minecraft:piston', 'R': 'minecraft:redstone', 'U': 'ls_backpacks:upgrade_base'}),
    'crafting_upgrade': ('CRAFTING', '#b5835a', 'Crafting Upgrade', 'Adds a crafting grid to the backpack screen',
                         ['C', 'U'], {'C': 'minecraft:crafting_table', 'U': 'ls_backpacks:upgrade_base'}),
    'smelting_upgrade': ('SMELTING', '#e05a2b', 'Smelting Upgrade', 'Adds a small furnace that keeps smelting\nwhile you carry the backpack or it is set down',
                         ['F', 'U'], {'F': 'minecraft:furnace', 'U': 'ls_backpacks:upgrade_base'}),
    'tank_upgrade': ('TANK', '#3b8fd9', 'Tank Upgrade', 'Adds a 16 bucket fluid tank:\nput buckets in to fill or empty it',
                     ['GBG', 'GUG'], {'G': '#forge:glass', 'B': 'minecraft:bucket', 'U': 'ls_backpacks:upgrade_base'}),
}


def fabric(seed):
    """Grey canvas for dye tinting."""
    img = tex.noise_fill('#c8c8c8', seed, 0.07)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if (x + y * 2) % 7 == 0:
                r, g, b, a = px[x, y]
                px[x, y] = (r - 18, g - 18, b - 18, a)
    for x in range(16):
        px[x, 0] = px[x, 15] = (150, 150, 150, 255)
    for i in range(1, 15, 2):
        px[1, i] = px[14, i] = (235, 235, 235, 255)
    return img


def trim(colour, seed):
    img = tex.noise_fill(colour, seed, 0.05)
    px = img.load()
    c = tex.rgb(colour)
    for x in range(16):
        px[x, 0] = tex.shade(c, 1.25)
        px[x, 15] = tex.shade(c, 0.6)
    return img


def backpack_item_layers(colour, seed):
    """layer0: grey bag (dyed), layer1: straps, buckle and trim in the tier colour."""
    base = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(base)
    d.rounded_rectangle([3, 3, 12, 14], radius=2, fill=(200, 200, 200, 255), outline=(90, 90, 90, 255))
    d.rectangle([4, 9, 11, 13], fill=(178, 178, 178, 255), outline=(110, 110, 110, 255))
    d.rectangle([4, 3, 11, 6], fill=(215, 215, 215, 255), outline=(110, 110, 110, 255))
    d.line([6, 1, 9, 1], fill=(90, 90, 90, 255))
    d.point([(5, 2), (10, 2)], fill=(90, 90, 90, 255))
    top = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    t = ImageDraw.Draw(top)
    c = tex.rgb(colour)
    t.line([5, 2, 10, 2], fill=tex.shade(c, 0.8))
    t.line([5, 3, 5, 13], fill=tex.shade(c, 0.9))
    t.line([10, 3, 10, 13], fill=tex.shade(c, 0.9))
    t.rectangle([7, 6, 8, 8], fill=tex.shade(c, 1.3), outline=tex.shade(c, 0.6))
    return base, top


def upgrade_icon(colour, kind, seed):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([1, 2, 14, 13], radius=2, fill=(92, 72, 52, 255), outline=(52, 38, 26, 255))
    d.rectangle([3, 4, 12, 11], fill=(60, 64, 72, 255))
    c = tex.rgb(colour)
    hi = tex.shade(c, 1.25)
    if kind == 'MAGNET':
        d.arc([4, 4, 11, 12], 180, 360, fill=c, width=2)
        d.line([4, 8, 4, 10], fill=(220, 220, 220, 255))
        d.line([11, 8, 11, 10], fill=(220, 220, 220, 255))
    elif kind == 'PICKUP':
        d.polygon([(5, 5), (10, 5), (8, 9), (7, 9)], fill=c)
        d.rectangle([7, 9, 8, 10], fill=hi)
    elif kind == 'FEEDING':
        d.ellipse([5, 5, 10, 10], fill=c)
        d.point([(8, 4)], fill=(80, 160, 60, 255))
    elif kind == 'REFILL':
        d.rectangle([5, 7, 10, 10], fill=c)
        d.polygon([(7, 3), (9, 3), (9, 5), (10, 5), (8, 7), (6, 5), (7, 5)], fill=hi)
    elif kind == 'CRAFTING':
        for i in (5, 8):
            d.line([i, 5, i, 10], fill=c)
            d.line([4, i, 11, i], fill=c)
    elif kind == 'SMELTING':
        d.polygon([(7, 4), (10, 8), (9, 11), (6, 11), (5, 8)], fill=c)
        d.polygon([(7, 7), (8, 9), (7, 10)], fill=(255, 220, 90, 255))
    else:
        d.rectangle([5, 4, 10, 11], outline=(200, 220, 230, 255))
        d.rectangle([6, 7, 9, 10], fill=c)
    return img


def tinted(face_tex, tint=True, uv=None):
    f = {'texture': face_tex}
    if tint:
        f['tintindex'] = 0
    if uv:
        f['uv'] = uv
    return f


def box(frm, to, tex_ref, tint=True):
    return {'from': frm, 'to': to, 'faces': {d: tinted(tex_ref, tint) for d in ('north', 'south', 'east', 'west', 'up', 'down')}}


def placed_model(tier):
    t = 'ls_backpacks:block/'
    return {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
            'textures': {'fabric': t + 'backpack_fabric', 'trim': t + 'backpack_trim_' + tier, 'particle': t + 'backpack_fabric'},
            'display': {'gui': {'rotation': [30, 225, 0], 'scale': [0.8, 0.8, 0.8]}},
            'elements': [box([3, 0, 5], [13, 11, 11], '#fabric'), box([3, 11, 5], [13, 12.5, 11.5], '#fabric'),
                         box([4, 2, 4], [12, 8, 5], '#fabric'), box([7, 6, 3.6], [9, 8.4, 4], '#trim', False),
                         box([4.5, 1, 11], [6, 11, 11.6], '#trim', False), box([10, 1, 11], [11.5, 11, 11.6], '#trim', False)]}


def worn_model(tier):
    t = 'ls_backpacks:block/'
    return {'parent': 'minecraft:block/block', 'textures': {'fabric': t + 'backpack_fabric', 'trim': t + 'backpack_trim_' + tier,
                                                            'particle': t + 'backpack_fabric'},
            'elements': [box([4, 0, 8], [12, 10, 12], '#fabric'), box([4, 10, 7.5], [12, 11, 12.5], '#fabric'),
                         box([5, 1.5, 12], [11, 6.5, 13.2], '#fabric'), box([7, 5, 13.2], [9, 7, 13.5], '#trim', False)]}


def build():
    m = Mod('ls_backpacks', 'Backpacks', 'Backpacks in five tiers that you wear or set down, with upgrade modules: magnet, pickup, '
                                         'feeding, refill, crafting, smelting and a fluid tank.', tab='LOGISTICS')
    m.init('BackpacksSetup.init(bus)')
    m.client('com.lodestar.backpacks.client.BackpacksClient.init(bus)')
    rl = m.rl
    m.texture('block/backpack_fabric', fabric(m.modid + 'fabric'))
    prev = None
    for tier, (colour, lang, rows, ups) in TIERS.items():
        name = tier + '_backpack'
        base, top = backpack_item_layers(colour, m.modid + tier)
        m.texture('item/%s' % name, base)
        m.texture('item/%s_trim' % name, top)
        props = 'new Item.Properties()' + ('.fireResistant()' if tier == 'netherite' else '')
        m.item(name, java='new BackpackItem(%s, BackpackTier.%s)' % (props, tier.upper()), jtype='BackpackItem', tab='LOGISTICS', lang=lang,
               model={'parent': 'minecraft:item/generated', 'textures': {'layer0': 'ls_backpacks:item/' + name, 'layer1': 'ls_backpacks:item/%s_trim' % name}},
               desc='%d slots, %d upgrade slot%s' % (rows * 9, ups, '' if ups == 1 else 's'))
        m.texture('block/backpack_trim_' + tier, trim(colour, m.modid + 'trim' + tier))
        m.asset('models/block/placed_backpack_%s.json' % tier, placed_model(tier))
        m.asset('models/block/worn_%s.json' % tier, worn_model(tier))
        if prev is None:
            m.shaped(name, rl(name), ['SLS', 'LCL', 'LLL'], {'S': 'minecraft:string', 'L': 'minecraft:leather', 'C': '#forge:chests/wooden'})
        else:
            keys = {'iron': {'X': '#forge:ingots/iron', 'Y': '#forge:ingots/iron'},
                    'gold': {'X': '#forge:ingots/gold', 'Y': '#forge:ingots/gold'},
                    'diamond': {'X': 'minecraft:diamond', 'Y': '#forge:ingots/steel'},
                    'netherite': {'X': 'minecraft:obsidian', 'Y': 'minecraft:netherite_ingot'}}[tier]
            pattern = ['XYX', 'XBX', 'XYX']
            key = {k: I(v) for k, v in keys.items()}
            key['B'] = I(rl(prev))
            m.recipe(name, {'type': 'ls_backpacks:upgrade', 'category': 'equipment', 'pattern': pattern, 'key': key,
                            'result': {'item': rl(name)}})
        prev = name
    # the placed backpack (no item of its own: you place the backpack item itself)
    state = {'variants': {}}
    for f, r in {'north': 0, 'east': 90, 'south': 180, 'west': 270}.items():
        for tier in TIERS:
            v = {'model': 'ls_backpacks:block/placed_backpack_' + tier}
            if r:
                v['y'] = r
            state['variants']['facing=%s,tier=%s' % (f, tier)] = v
    m.block('placed_backpack', java='new BackpackBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(0.5f)'
                                    '.sound(net.minecraft.world.level.block.SoundType.WOOL).noOcclusion(), () -> Content.PLACED_BACKPACK_BE.get())',
            jtype='BackpackBlock', item=False, state=state, models={}, loot='none', mine=None, lang='Backpack')
    m.field('public static final RegistryObject<BlockEntityType<BackpackBE>> PLACED_BACKPACK_BE = REG.blockEntity("placed_backpack", '
            '(pos, state) -> new BackpackBE(Content.PLACED_BACKPACK_BE.get(), pos, state), Content.PLACED_BACKPACK);')

    m.item('upgrade_base', upgrade_icon('#777777', 'BASE', m.modid + 'base'), tab='LOGISTICS', lang='Blank Upgrade Module',
           desc='Crafted into backpack upgrades')
    m.shaped('upgrade_base', rl('upgrade_base'), ['SLS', 'LRL', 'SLS'], {'S': 'minecraft:string', 'L': 'minecraft:leather', 'R': 'minecraft:redstone'}, count=2)
    for name, (kind, colour, lang, tip, pattern, keys) in UPGRADES.items():
        m.item(name, upgrade_icon(colour, kind, m.modid + name), java='new UpgradeItem(new Item.Properties(), UpgradeItem.Kind.%s)' % kind,
               jtype='UpgradeItem', tab='LOGISTICS', lang=lang)
        m.tr('tooltip.ls_backpacks.' + kind.lower(), tip)
        m.shaped(name, rl(name), pattern, keys)

    for k, v in {'tooltip.ls_backpacks.size': '%s slots, %s upgrade slots',
                 'tooltip.ls_backpacks.use': 'Use to open; sneak-use on the ground to set down;\nwear it and press B',
                 'key.ls_backpacks.open': 'Open backpack', 'gui.ls_backpacks.empty': 'Empty',
                 'gui.ls_backpacks.upgrade_slot': 'Upgrade modules go here'}.items():
        m.tr(k, v)
    m.codex_intro('A backpack carries a chest\'s worth of items or more. Use it to open it, wear it in your chestplate slot and press '
                  'B, or sneak-use it on the ground to set it down like a chest that hoppers and ducts can reach. Pick it back up by '
                  'sneak-using it with an empty hand: everything inside stays with it.')
    m.codex_page('Tiers', 'Leather backpacks hold 27 stacks and one upgrade. Surround a backpack with iron, gold, diamond and steel, '
                          'or obsidian and netherite, to make the next tier: the contents, colour and name come along. A netherite '
                          'backpack holds 72 stacks and five upgrades and survives fire and lava.',
                 items=[rl(t + '_backpack') for t in TIERS])
    m.codex_page('Upgrades', 'Put upgrade modules in the slots at the right of the backpack screen. The magnet pulls nearby items to '
                             'you, the pickup upgrade sends items you already keep in the backpack straight into it, the feeding '
                             'upgrade eats for you, and the refill upgrade keeps your hotbar stacked.',
                 items=[rl('upgrade_base'), rl('magnet_upgrade'), rl('pickup_upgrade'), rl('feeding_upgrade'), rl('refill_upgrade')])
    m.codex_page('Crafting, smelting and a tank', 'The crafting, smelting and tank upgrades each add a page to the side of the screen; '
                                                  'switch between them with the C, S and T buttons. The smelting upgrade keeps cooking '
                                                  'while you walk. Put a full bucket in the tank page to pour it in, an empty one to fill it.',
                 items=[rl('crafting_upgrade'), rl('smelting_upgrade'), rl('tank_upgrade')])
    m.codex_page('Dyeing', 'Backpacks take dye like leather armour: craft one with any dyes to colour it. The colour shows on your back '
                           'and when it is set down.')
    return m
