"""Deep Dungeons: sprawling jigsaw dungeons below a ruined entrance shaft, with a locked vault at the heart."""
from gen.fw import Mod, stairs_slab_wall, column
from gen import tex
from gen.structures import Struct, structure, add_pool, loot_table, stairs, AIR, LAND
from mods._build import pick, lantern, ladder, torch_wall, bars, scatter, slab

M = 'fw_dungeons'
BR = M + ':dungeon_bricks'
CR = M + ':cracked_dungeon_bricks'
MO = M + ':mossy_dungeon_bricks'
TI = M + ':dungeon_tiles'
LINK = M + ':link'
HALLS = M + ':dungeon/halls'
ROOMS = M + ':dungeon/rooms'
CAPS = M + ':dungeon/caps'
DECAY = {BR: (CR, 0.12)}
SPIKES = M + ':spike_trap'
VAULT = M + ':vault[facing=%s,open=false]'
MOBS = ['minecraft:zombie', 'minecraft:skeleton', 'minecraft:spider', 'minecraft:cave_spider', 'minecraft:husk', 'minecraft:stray']


def wall_fn(s):
    return pick(s, (BR, 10), (CR, 3), (MO, 3), ('minecraft:cobbled_deepslate', 1))


def floor_fn(s):
    return pick(s, (TI, 8), (BR, 2), ('minecraft:cobbled_deepslate', 1), ('minecraft:gravel', 1))


DOORS = {'north': (5, 0), 'south': (5, 10), 'west': (0, 5), 'east': (10, 5)}


def room(seed, doors=('north', 'south', 'east', 'west'), h=7):
    """11x(h+1)x11 room shell with 3x3 doorways + jigsaws on the chosen walls."""
    s = Struct(11, h + 1, 11, seed)
    s.room(0, 0, 0, 10, h, 10, wall_fn(s), floor=floor_fn(s), ceiling=wall_fn(s))
    for d in doors:
        x, z = DOORS[d]
        if d in ('north', 'south'):
            s.clear(4, 1, z, 6, 3, z)
        else:
            s.clear(x, 1, 4, x, 3, 6)
        s.jigsaw(x, 1, z, d, LINK, LINK, HALLS)
    return s


def finish(s, remove=0.0):
    s.decay(remove=remove, replace=DECAY)
    return s


# ----------------------------------------------------------------------------- start: entrance shaft

G = 26  # template y of the ground surface for the entrance pieces


def entrance(seed, style):
    s = Struct(7, G + 7, 7, seed)
    w = wall_fn(s)
    for y in range(0, G):
        s.walls(1, y, 1, 5, y, 5, w)
        s.clear(2, y, 2, 4, y, 4)
    s.fill(2, 0, 2, 4, 0, 4, TI)
    ladder(s, 3, 1, G, 2, 'south')
    s.jigsaw(3, 0, 2, 'down', M + ':shaft_down', M + ':shaft_up', M + ':dungeon/hub', final='minecraft:ladder[facing=south,waterlogged=false]')
    s.clear(2, G + 1, 2, 4, G + 6, 4)
    if style == 'gazebo':
        s.fill(0, G, 0, 6, G, 6, floor_fn(s))
        s.clear(2, G, 2, 4, G, 4)
        s.set(3, G, 2, 'minecraft:ladder[facing=south,waterlogged=false]')
        for (x, z) in ((0, 0), (6, 0), (0, 6), (6, 6)):
            for y in range(G + 1, G + 5):
                s.set(x, y, z, M + ':dungeon_brick_wall' if y < G + 4 else BR)
        for x in range(7):
            for z in range(7):
                if x in (0, 6) or z in (0, 6):
                    s.set(x, G + 5, z, M + ':dungeon_brick_slab[type=bottom,waterlogged=false]')
        s.decay(remove=0.15, min_y=G + 1)
        s.set(3, G + 5, 3, BR)
        lantern(s, 3, G + 4, 3)
    else:  # broken well
        s.walls(1, G, 1, 5, G + 1, 5, w)
        s.clear(2, G, 2, 4, G + 1, 4)
        s.set(3, G, 2, 'minecraft:ladder[facing=south,waterlogged=false]')
        s.set(3, G + 1, 2, 'minecraft:ladder[facing=south,waterlogged=false]')
        for (x, z) in ((1, 1), (5, 5)):
            s.set(x, G + 2, z, 'minecraft:oak_fence[east=false,west=false,north=false,south=false,waterlogged=false]')
            s.set(x, G + 3, z, 'minecraft:oak_fence[east=false,west=false,north=false,south=false,waterlogged=false]')
        s.decay(remove=0.1, min_y=G)
    torch_wall(s, 2, G - 6, 3, 'east')
    torch_wall(s, 4, G - 16, 3, 'west')
    return s


# ----------------------------------------------------------------------------- hub (below the shaft)

def hub(seed):
    s = room(seed, h=7)
    s.jigsaw(5, 7, 5, 'up', M + ':shaft_up', M + ':shaft_down', 'minecraft:empty', final='minecraft:ladder[facing=north,waterlogged=false]')
    for y in range(1, 7):
        s.set(5, y, 6, M + ':dungeon_pillar[axis=y]')
    ladder(s, 5, 1, 6, 5, 'north')
    for (x, z) in ((2, 2), (8, 2), (2, 8), (8, 8)):
        for y in range(1, 7):
            s.set(x, y, z, M + ':dungeon_pillar[axis=y]')
        lantern(s, x, 6, z + (1 if z < 5 else -1))
    s.chest(8, 1, 5, M + ':chests/dungeon', 'west', kind='barrel')
    s.set(2, 1, 5, 'minecraft:crafting_table')
    return finish(s)


# ----------------------------------------------------------------------------- halls

def hall_shell(sx, sy, sz, seed):
    s = Struct(sx, sy, sz, seed)
    s.fill(0, 0, 0, sx - 1, sy - 1, sz - 1, wall_fn(s))
    return s


def hall_straight(seed, length=11, spikes=False):
    s = hall_shell(5, 5, length, seed)
    s.clear(1, 1, 0, 3, 3, length - 1)
    s.fill(1, 0, 0, 3, 0, length - 1, floor_fn(s))
    s.jigsaw(2, 1, 0, 'north', LINK, LINK, ROOMS)
    s.jigsaw(2, 1, length - 1, 'south', LINK, LINK, ROOMS)
    for z in range(3, length - 2, 6):
        torch_wall(s, 1, 2, z, 'east')
        torch_wall(s, 3, 2, z + 3 if z + 3 < length - 1 else z, 'west')
    if spikes:
        for z in range(length // 2 - 1, length // 2 + 2):
            for x in (1, 2, 3):
                s.set(x, 1, z, SPIKES)
    scatter(s, 1, 0, 3, length - 1, 3, ['minecraft:cobweb'], 0.05)
    return finish(s, 0.0)


def hall_turn(seed):
    s = hall_shell(7, 5, 7, seed)
    s.clear(2, 1, 0, 4, 3, 4)
    s.clear(2, 1, 2, 6, 3, 4)
    s.fill(2, 0, 0, 4, 0, 4, floor_fn(s))
    s.fill(2, 0, 2, 6, 0, 4, floor_fn(s))
    s.jigsaw(3, 1, 0, 'north', LINK, LINK, ROOMS)
    s.jigsaw(6, 1, 3, 'east', LINK, LINK, ROOMS)
    lantern(s, 3, 3, 3)
    return finish(s)


def hall_t(seed):
    s = hall_shell(11, 5, 6, seed)
    s.clear(0, 1, 1, 10, 3, 3)
    s.clear(4, 1, 4, 6, 3, 5)
    s.fill(0, 0, 1, 10, 0, 3, floor_fn(s))
    s.jigsaw(0, 1, 2, 'west', LINK, LINK, ROOMS)
    s.jigsaw(10, 1, 2, 'east', LINK, LINK, ROOMS)
    s.jigsaw(5, 1, 5, 'south', LINK, LINK, ROOMS)
    torch_wall(s, 5, 2, 1, 'south')
    return finish(s)


def hall_stairs(seed):
    """Descends 4 blocks: enters high at the north end, leaves low at the south end."""
    s = hall_shell(5, 9, 11, seed)
    s.clear(1, 5, 0, 3, 7, 2)
    for i in range(4):
        z = 3 + i
        y = 4 - i
        s.clear(1, y, z, 3, y + 3, z)
        for x in (1, 2, 3):
            s.set(x, y, z, stairs('deepslate_brick', 'north'))
    s.clear(1, 1, 7, 3, 3, 10)
    s.fill(1, 4, 0, 3, 4, 2, floor_fn(s))
    s.fill(1, 0, 7, 3, 0, 10, floor_fn(s))
    s.jigsaw(2, 5, 0, 'north', LINK, LINK, ROOMS)
    s.jigsaw(2, 1, 10, 'south', LINK, LINK, ROOMS)
    torch_wall(s, 1, 6, 1, 'east')
    torch_wall(s, 3, 2, 9, 'west')
    return finish(s)


# ----------------------------------------------------------------------------- rooms

def spawner_room(seed):
    s = room(seed)
    s.fill(3, 1, 3, 7, 1, 7, M + ':dungeon_tiles_slab[type=bottom,waterlogged=false]')
    s.fill(4, 1, 4, 6, 1, 6, TI)
    s.spawner(5, 2, 5, MOBS[seed % len(MOBS)])
    s.chest(1, 1, 1, M + ':chests/dungeon', 'south')
    s.chest(9, 1, 9, M + ':chests/dungeon', 'north')
    for (x, z) in ((1, 9), (9, 1)):
        lantern(s, x, 1, z, hanging=False)
    scatter(s, 1, 1, 9, 9, 6, ['minecraft:cobweb'], 0.06)
    return finish(s, 0.01)


def crypt(seed):
    s = room(seed, doors=('north', 'south'))
    for x in (2, 8):
        for z in (2, 5, 8):
            s.fill(x - 1, 1, z, x + 1, 1, z, 'minecraft:polished_deepslate')
            s.set(x, 2, z, slab('polished_deepslate'))
            lantern(s, x, 6, z, soul=True)
    s.spawner(5, 1, 5, 'minecraft:skeleton')
    s.chest(8, 2, 5, M + ':chests/crypt', 'west')
    s.set(8, 1, 5, 'minecraft:polished_deepslate')
    s.set(2, 2, 2, 'minecraft:skeleton_skull[rotation=4]')
    return finish(s)


def library(seed):
    s = room(seed, doors=('north', 'east'))
    for y in (1, 2, 3):
        for z in range(1, 10):
            if z not in (4, 5, 6) or y > 3:
                s.set(1, y, z, 'minecraft:bookshelf')
        for x in range(1, 10):
            if x not in (4, 5, 6):
                s.set(x, y, 9, 'minecraft:bookshelf')
    for z in (3, 7):
        for x in range(4, 8):
            s.set(x, 1, z, 'minecraft:bookshelf')
            s.set(x, 2, z, 'minecraft:bookshelf' if s.rng.random() < 0.7 else AIR)
    s.set(5, 1, 5, 'minecraft:lectern[facing=north,has_book=false,powered=false]')
    s.chest(8, 1, 1, M + ':chests/library', 'south')
    s.set(2, 1, 8, 'minecraft:enchanting_table' if seed % 3 == 0 else 'minecraft:cartography_table')
    lantern(s, 5, 6, 5)
    scatter(s, 1, 1, 9, 9, 6, ['minecraft:cobweb'], 0.08)
    return finish(s)


def prison(seed):
    s = room(seed, doors=('south', 'west'))
    for cx in (1, 4, 7):
        for y in (1, 2, 3):
            s.set(cx + 2, y, 1, BR)
            s.set(cx + 2, y, 2, BR)
            for x in range(cx, cx + 2):
                bars(s, x, y, 3, 'x')
        s.set(cx, 1, 1, 'minecraft:chain[axis=y,waterlogged=false]' if s.rng.random() < 0.5 else 'minecraft:cobweb')
    s.chest(8, 1, 1, M + ':chests/dungeon', 'south')
    s.set(8, 3, 3, AIR)
    s.spawner(5, 1, 7, 'minecraft:zombie')
    lantern(s, 5, 6, 6)
    return finish(s)


def fungus_cellar(seed):
    s = room(seed, doors=('north', 'south', 'west'))
    s.fill(1, 0, 1, 9, 0, 9, pick(s, ('minecraft:moss_block', 3), ('minecraft:rooted_dirt', 2), (TI, 2), ('minecraft:mycelium', 2)))
    scatter(s, 1, 1, 9, 9, 1, ['minecraft:brown_mushroom', 'minecraft:red_mushroom', 'minecraft:moss_carpet', 'minecraft:moss_carpet'], 0.45)
    for (x, z) in ((3, 3), (7, 7)):
        s.set(x, 1, z, 'minecraft:mushroom_stem[down=true,east=true,north=true,south=true,up=true,west=true]')
        s.set(x, 2, z, 'minecraft:mushroom_stem[down=true,east=true,north=true,south=true,up=true,west=true]')
        s.fill(x - 1, 3, z - 1, x + 1, 3, z + 1, 'minecraft:red_mushroom_block[down=true,east=true,north=true,south=true,up=true,west=true]')
    s.chest(9, 1, 5, M + ':chests/dungeon', 'west', kind='barrel')
    return finish(s)


def armory(seed):
    s = room(seed, doors=('east', 'west'))
    for x in (2, 4, 6, 8):
        s.set(x, 1, 1, 'minecraft:anvil[facing=east]' if x % 4 == 0 else 'minecraft:grindstone[face=floor,facing=south]')
        s.set(x, 1, 9, 'minecraft:smithing_table' if x == 4 else 'minecraft:barrel[facing=up,open=false]')
    s.chest(5, 1, 1, M + ':chests/armory', 'south')
    s.chest(6, 1, 9, M + ':chests/armory', 'north')
    s.spawner(5, 1, 5, 'minecraft:skeleton' if seed % 2 else 'minecraft:husk')
    lantern(s, 3, 6, 5)
    lantern(s, 7, 6, 5)
    return finish(s)


def closet(seed):
    """Tiny dead end with a chest: cheap terminator that is still worth walking to."""
    s = Struct(5, 5, 5, seed)
    s.room(0, 0, 0, 4, 4, 4, wall_fn(s), floor=floor_fn(s))
    s.clear(1, 1, 0, 3, 3, 0)
    s.jigsaw(2, 1, 0, 'north', LINK, LINK, HALLS)
    s.chest(2, 1, 3, M + ':chests/dungeon', 'north')
    lantern(s, 2, 3, 2)
    return finish(s)


def treasure(seed):
    s = room(seed, doors=('south',), h=8)
    s.fill(1, 0, 1, 9, 0, 9, pick(s, (TI, 5), ('minecraft:polished_blackstone_bricks', 2)))
    s.fill(3, 1, 1, 7, 1, 3, 'minecraft:polished_blackstone_bricks')
    s.set(5, 2, 2, VAULT % 'south')
    s.set(4, 2, 2, 'minecraft:gold_block')
    s.set(6, 2, 2, 'minecraft:gold_block')
    for x in (3, 7):
        s.set(x, 2, 1, 'minecraft:soul_lantern[hanging=false,waterlogged=false]')
    s.chest(1, 1, 5, M + ':chests/treasure', 'east')
    s.chest(9, 1, 5, M + ':chests/treasure', 'west')
    s.spawner(2, 1, 8, MOBS[seed % 3])
    s.spawner(8, 1, 8, MOBS[(seed + 1) % 3])
    for x in (2, 8):
        for y in range(1, 8):
            s.set(x, y, 4, M + ':dungeon_pillar[axis=y]')
    lantern(s, 5, 7, 5)
    return finish(s)


def cap(seed):
    """Wall plug for unconnected doorways (fallback piece)."""
    s = Struct(3, 3, 1, seed)
    s.fill(0, 0, 0, 2, 2, 0, wall_fn(s))
    s.jigsaw(1, 0, 0, 'north', LINK, LINK, 'minecraft:empty', final=BR)
    return s


def build():
    m = Mod(M, 'Deep Dungeons', 'A ruined shaft leads down into sprawling dungeons: spawner halls, crypts, libraries, prisons and '
                                'armories, with spike traps in the corridors and a locked vault in the treasure room. Find a Dungeon '
                                'Key in the dungeon\'s chests to open it.', tab='ADVENTURE')
    rl = m.rl
    m.block('dungeon_bricks', props='FWProps.stone(3.0f, 8.0f)', textures={'block/dungeon_bricks': tex.bricks('#45444c', 'dgb')}, tab='DECOR')
    m.block('cracked_dungeon_bricks', props='FWProps.stone(3.0f, 8.0f)', tab='DECOR',
            textures={'block/cracked_dungeon_bricks': tex.overlay(tex.bricks('#45444c', 'dgb'), tex.ore_overlay('#1c1b20', 'dgbc', 'specks', 0.7))})
    m.block('mossy_dungeon_bricks', props='FWProps.stone(3.0f, 8.0f)', tab='DECOR',
            textures={'block/mossy_dungeon_bricks': tex.overlay(tex.bricks('#45444c', 'dgb'), tex.ore_overlay('#3f6a30', 'dgbm', 'blobs', 0.9))})
    m.block('dungeon_tiles', props='FWProps.stone(3.0f, 8.0f)', textures={'block/dungeon_tiles': tex.tiles('#3d3c44', 'dgt')}, tab='DECOR')
    column(m, 'dungeon_pillar', tex.chiseled('#4d4c55', 'dgp'), tex.polished('#4d4c55', 'dgpt'), java='new RotatedPillarBlock(FWProps.stone(3.0f, 8.0f))',
           jtype='RotatedPillarBlock', tab='DECOR', side_name='dungeon_pillar', end_name='dungeon_pillar_top')
    stairs_slab_wall(m, 'dungeon_bricks', rl('block/dungeon_bricks'), props='FWProps.stone(3.0f, 8.0f)')
    stairs_slab_wall(m, 'dungeon_tiles', rl('block/dungeon_tiles'), wall=False, props='FWProps.stone(3.0f, 8.0f)')
    m.shaped('dungeon_bricks', rl('dungeon_bricks'), ['DB', 'BD'], {'D': 'minecraft:deepslate_bricks', 'B': 'minecraft:stone_bricks'}, 4)
    m.shaped('dungeon_tiles', rl('dungeon_tiles'), ['##', '##'], {'#': rl('dungeon_bricks')}, 4)
    m.shaped('dungeon_pillar', rl('dungeon_pillar'), ['#', '#'], {'#': rl('dungeon_bricks')}, 2)
    m.shapeless('mossy_dungeon_bricks', rl('mossy_dungeon_bricks'), [rl('dungeon_bricks'), 'minecraft:moss_block'])
    m.smelt('cracked_dungeon_bricks', rl('dungeon_bricks'), rl('cracked_dungeon_bricks'), 0.1, blast=False)

    # spike trap: a low block that hurts anything walking over it
    t = lambda n: '%s:block/%s' % (M, n)
    spike_model = {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': {'particle': t('spike_trap'), 'spikes': t('spike_trap'), 'base': t('dungeon_tiles')},
                   'elements': [{'from': [0, 0, 0], 'to': [16, 1, 16], 'faces': {f: {'texture': '#base', 'cullface': 'down'} if f == 'down' else {'texture': '#base'} for f in ('north', 'south', 'east', 'west', 'up', 'down')}},
                                {'from': [0, 1, 8], 'to': [16, 7, 8], 'shade': False, 'faces': {'north': {'texture': '#spikes'}, 'south': {'texture': '#spikes'}}},
                                {'from': [8, 1, 0], 'to': [8, 7, 16], 'shade': False, 'faces': {'east': {'texture': '#spikes'}, 'west': {'texture': '#spikes'}}}]}
    m.block('spike_trap', java='new com.frontierworks.dungeons.SpikeBlock(FWProps.metal(2.0f).noOcclusion())', jtype='com.frontierworks.dungeons.SpikeBlock',
            tab='ADVENTURE', models={'spike_trap': spike_model}, textures={'block/spike_trap': spike_tex()},
            desc='Hurts anything that walks across it')
    m.shaped('spike_trap', rl('spike_trap'), [' I ', 'IPI'], {'I': 'minecraft:iron_ingot', 'P': 'minecraft:stone_pressure_plate'}, 2)

    # vault + key
    m.item('dungeon_key', ('key', '#d4af37'), desc='Opens one dungeon vault', stack=16)
    vt = lambda n: '%s:block/%s' % (M, n)
    vmodels = {'vault': {'parent': 'minecraft:block/orientable', 'textures': {'top': vt('vault_top'), 'side': vt('vault_side'), 'front': vt('vault_front')}},
               'vault_open': {'parent': 'minecraft:block/orientable', 'textures': {'top': vt('vault_top'), 'side': vt('vault_side'), 'front': vt('vault_front_open')}}}
    vstate = {'variants': {}}
    for f, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        for o in ('false', 'true'):
            e = {'model': vt('vault_open' if o == 'true' else 'vault')}
            if y:
                e['y'] = y
            vstate['variants']['facing=%s,open=%s' % (f, o)] = e
    m.block('vault', java='new com.frontierworks.dungeons.VaultBlock(FWProps.metal(50.0f).strength(-1.0f, 3600000f).noLootTable(), () -> Content.DUNGEON_KEY.get(), '
                          'new ResourceLocation("fw_dungeons", "vault/dungeon"))', jtype='com.frontierworks.dungeons.VaultBlock', tab='ADVENTURE',
            state=vstate, models=vmodels, loot=None, level=None, mine=None, desc='Needs a Dungeon Key',
            textures={'block/vault_top': tex.metal_block('#5b5348', 'vt', 'riveted'), 'block/vault_side': tex.metal_block('#5b5348', 'vs', 'riveted'),
                      'block/vault_front': vault_front(False), 'block/vault_front_open': vault_front(True)})
    m.tr('msg.fw_dungeons.vault_locked', 'Locked. It needs a %s')
    m.tr('msg.fw_dungeons.vault_empty', 'The vault is empty')

    # loot
    key = (rl('dungeon_key'), 3)
    common = [('minecraft:bone', 10, 1, 5), ('minecraft:rotten_flesh', 10, 1, 4), ('minecraft:string', 8, 1, 4), ('minecraft:gunpowder', 8, 1, 4),
              ('minecraft:iron_ingot', 8, 1, 4), ('minecraft:gold_ingot', 5, 1, 3), ('minecraft:bread', 7, 1, 3), ('minecraft:coal', 8, 2, 8),
              ('minecraft:redstone', 6, 2, 8), ('minecraft:torch', 6, 4, 12), ('minecraft:arrow', 6, 4, 12), ('fw_metals:silver_ingot', 4, 1, 3),
              ('fw_metals:lead_ingot', 4, 1, 3), ('minecraft:name_tag', 2), ('minecraft:saddle', 2)]
    loot_table(m, 'chests/dungeon', [(3, 6, common), (1, 1, [key, ('minecraft:golden_apple', 2), ('minecraft:diamond', 2, 1, 2), ('minecraft:emerald', 4, 1, 3),
                                                             ('minecraft:iron_sword', 3, 1, 1, 'enchant'), ('minecraft:book', 3, 1, 1, 'enchant'),
                                                             ('minecraft:music_disc_13', 1), ('minecraft:music_disc_cat', 1)])])
    loot_table(m, 'chests/crypt', [(2, 5, [('minecraft:bone', 14, 2, 7), ('minecraft:arrow', 8, 4, 16), ('minecraft:gold_nugget', 8, 3, 9),
                                           ('minecraft:skeleton_skull', 1), ('minecraft:bow', 3, 1, 1, 'enchant')]), (1, 1, [key, ('minecraft:emerald', 3, 1, 4)])])
    loot_table(m, 'chests/library', [(2, 5, [('minecraft:paper', 10, 2, 8), ('minecraft:book', 10, 1, 4), ('minecraft:writable_book', 3),
                                             ('minecraft:map', 3), ('minecraft:compass', 2), ('minecraft:experience_bottle', 5, 1, 4),
                                             ('minecraft:lapis_lazuli', 6, 2, 8)]),
                                     (1, 2, [('minecraft:book', 6, 1, 1, 'enchant'), key])])
    loot_table(m, 'chests/armory', [(2, 4, [('minecraft:iron_ingot', 10, 2, 6), ('minecraft:chainmail_chestplate', 3), ('minecraft:chainmail_helmet', 3),
                                            ('minecraft:iron_helmet', 3, 1, 1, 'enchant'), ('minecraft:iron_axe', 3, 1, 1, 'enchant'), ('minecraft:shield', 4),
                                            ('minecraft:crossbow', 3, 1, 1, 'enchant'), ('fw_alloys:steel_ingot', 5, 1, 4)]), (1, 1, [key, ('minecraft:arrow', 5, 8, 24)])])
    loot_table(m, 'chests/treasure', [(4, 7, [('minecraft:gold_ingot', 10, 2, 7), ('minecraft:diamond', 5, 1, 3), ('minecraft:emerald', 7, 2, 6),
                                              ('minecraft:golden_apple', 4), ('minecraft:book', 5, 1, 1, 'enchant'), ('minecraft:diamond_sword', 2, 1, 1, 'enchant'),
                                              ('fw_gems:ruby', 4, 1, 4), ('fw_gems:sapphire', 4, 1, 4), ('fw_metals:platinum_ingot', 3, 1, 3)]), (1, 1, [(rl('dungeon_key'), 1)])])
    loot_table(m, 'vault/dungeon', [(5, 8, [('minecraft:diamond', 8, 2, 5), ('minecraft:emerald', 8, 3, 8), ('minecraft:gold_block', 4, 1, 2),
                                            ('minecraft:netherite_scrap', 3, 1, 2), ('minecraft:diamond_chestplate', 2, 1, 1, 'enchant'),
                                            ('minecraft:diamond_pickaxe', 2, 1, 1, 'enchant'), ('minecraft:book', 6, 1, 1, 'enchant'),
                                            ('minecraft:golden_apple', 5, 1, 3), ('minecraft:experience_bottle', 6, 4, 12),
                                            ('fw_meteors:star_fragment', 2, 1, 2), ('fw_crystals:energite_shard', 5, 3, 8)]),
                                    (1, 1, [('minecraft:enchanted_golden_apple', 1), ('minecraft:totem_of_undying', 1), ('minecraft:trident', 1),
                                            ('minecraft:heart_of_the_sea', 1), ('minecraft:diamond_block', 2)])])

    # structure
    add_pool(m, 'dungeon/hub', [(hub(11), 1), (hub(12), 1)])
    add_pool(m, 'dungeon/caps', [(cap(1), 1)])
    add_pool(m, 'dungeon/halls', [(hall_straight(21), 4), (hall_straight(22, 17), 2), (hall_straight(23, spikes=True), 2), (hall_turn(24), 3),
                                  (hall_t(25), 2), (hall_stairs(26), 2)], fallback=CAPS)
    add_pool(m, 'dungeon/rooms', [(spawner_room(31), 4), (spawner_room(32), 3), (crypt(33), 2), (library(34), 2), (prison(35), 2),
                                  (fungus_cellar(36), 2), (armory(37), 2), (closet(38), 3), (treasure(39), 1)], fallback=CAPS)
    structure(m, 'dungeon', [(entrance(1, 'gazebo'), 1), (entrance(2, 'well'), 1)], LAND, spacing=40, separation=16, terrain='none',
              height=-G, size=7, max_distance=96, step='underground_structures')
    return m


def spike_tex():
    from PIL import Image
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    for i, cx in enumerate(range(1, 16, 4)):
        h = 5 + (i % 2)
        for y in range(16 - h * 2, 16):
            half = (y - (16 - h * 2)) // 4
            for x in range(cx - half, cx + half + 1):
                if 0 <= x < 16:
                    shade = 150 + (x - cx) * 15
                    px[x, y] = (shade, shade + 4, shade + 10, 255)
    return img


def vault_front(open_):
    img = tex.metal_block('#5b5348', 'vf', 'riveted')
    px = img.load()
    for y in range(3, 13):
        for x in range(3, 13):
            if open_:
                px[x, y] = (18, 16, 14, 255) if 4 <= x <= 11 and 4 <= y <= 11 else (90, 82, 70, 255)
            elif x in (3, 12) or y in (3, 12):
                px[x, y] = (196, 160, 60, 255)
    if not open_:
        for (x, y) in ((7, 6), (8, 6), (7, 7), (8, 7), (7, 8), (7, 9), (8, 9)):
            px[x, y] = (30, 26, 22, 255)
    return img
