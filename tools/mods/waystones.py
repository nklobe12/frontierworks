from gen.fw import Mod, const
from gen import tex
from gen.structures import Struct, structure, stairs, FLAT_LAND


def shrine(seed, style):
    s = Struct(7, 6, 7, seed)
    mats = {'stone': ('minecraft:stone_bricks', 'minecraft:mossy_stone_bricks', 'stone_brick'),
            'sand': ('minecraft:cut_sandstone', 'minecraft:chiseled_sandstone', 'sandstone'),
            'snow': ('minecraft:packed_ice', 'minecraft:snow_block', 'stone_brick')}[style]
    base, accent, st = mats
    s.fill(0, 0, 0, 6, 0, 6, base)
    s.fill(1, 1, 1, 5, 1, 5, lambda: s.rng.choice([base, base, accent]))
    s.clear(0, 1, 0, 6, 5, 6)
    s.fill(1, 1, 1, 5, 1, 5, lambda: s.rng.choice([base, base, accent]))
    for x, z in ((1, 1), (5, 1), (1, 5), (5, 5)):
        s.set(x, 2, z, 'minecraft:%s_wall' % ('stone_brick' if st == 'stone_brick' else 'sandstone'))
        s.set(x, 3, z, 'minecraft:lantern[hanging=false,waterlogged=false]')
    s.set(3, 2, 3, 'fw_waystones:waystone')
    for d, (x, z) in (('south', (3, 0)), ('north', (3, 6)), ('east', (0, 3)), ('west', (6, 3))):
        s.set(x, 1, z, stairs(st, d))
    return s


def build():
    m = Mod('fw_waystones', 'Waystones', 'Ancient waystones dot the land. Activate them to build a fast-travel network and teleport '
                                         'between any you have found - for a small cost in experience. You can craft your own, too.', tab='WORLD')
    rl = m.rl
    m.client('com.frontierworks.waystones.client.WaystoneScreen.init(bus)')
    m.field('public static final RegistryObject<net.minecraft.world.inventory.MenuType<WaystoneMenu>> WAYSTONE_MENU = REG.menu("waystone", WaystoneMenu::new);')
    name = 'waystone'
    t = lambda n: '%s:block/%s' % (m.modid, n)
    model = {'parent': 'minecraft:block/block', 'textures': {'particle': t('waystone_side'), 'side': t('waystone_side'), 'top': t('waystone_top')},
             'elements': [{'from': [2, 0, 2], 'to': [14, 3, 14], 'faces': {f: {'texture': '#side' if f not in ('up', 'down') else '#top', 'uv': [2, 13, 14, 16] if f not in ('up', 'down') else [2, 2, 14, 14]} for f in ('north', 'south', 'east', 'west', 'up', 'down')}},
                          {'from': [4, 3, 4], 'to': [12, 16, 12], 'faces': {f: {'texture': '#side' if f not in ('up', 'down') else '#top', 'uv': [4, 0, 12, 13] if f not in ('up', 'down') else [4, 4, 12, 12]} for f in ('north', 'south', 'east', 'west', 'up', 'down')}}]}
    side = tex.stone('#6d6a75', 'wayside', 'smooth')
    px = side.load()
    for y in range(3, 12):
        px[8, y] = (90, 220, 255, 255)
        if y % 3 == 0:
            px[7, y] = px[9, y] = (60, 170, 220, 255)
    C = const(name)
    m.block(name, java='new WaystoneBlock(FWProps.stone(3.0f, 1200f).lightLevel(s -> 6), () -> Content.%s_BE.get())' % C, jtype='WaystoneBlock', tab='WORLD',
            models={name: model}, textures={'block/waystone_side': side, 'block/waystone_top': tex.polished('#6d6a75', 'waytop')}, level=None,
            desc='Right-click to activate, then to travel to any waystone you have activated')
    m.field('public static final RegistryObject<BlockEntityType<WaystoneBE>> %s_BE = REG.blockEntity("%s", (pos, state) -> new WaystoneBE(Content.%s_BE.get(), pos, state), Content.%s);'
            % (C, name, C, C))
    m.shaped(name, rl(name), [' E ', 'SVS', 'SSS'], {'E': '#forge:ender_pearls', 'S': 'minecraft:stone_bricks', 'V': 'fw_crystals:voidite_shard'})
    structure(m, 'waystone_shrine', [(shrine(1, 'stone'), 3), (shrine(2, 'stone'), 2)], FLAT_LAND, spacing=40, separation=14)
    structure(m, 'desert_waystone_shrine', [(shrine(3, 'sand'), 1)], ['#forge:is_desert', '#minecraft:is_badlands'], spacing=44, separation=16)
    structure(m, 'snowy_waystone_shrine', [(shrine(4, 'snow'), 1)], ['minecraft:snowy_plains', 'minecraft:ice_spikes', 'minecraft:snowy_taiga'], spacing=44, separation=16)
    for k, v in {'msg.fw_waystones.activated': 'Waystone activated: %s', 'msg.fw_waystones.gone': 'Waystone %s no longer exists',
                 'msg.fw_waystones.cost': 'You need %s experience levels', 'gui.fw_waystones.empty': 'Activate more waystones to travel.'}.items():
        m.tr(k, v)
    return m
