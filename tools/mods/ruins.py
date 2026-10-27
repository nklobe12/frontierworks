"""Ancient Ruins: crumbling remains of an old civilisation scattered over the overworld."""
import math

from gen.fw import Mod, stairs_slab_wall, column
from gen import tex
from gen.structures import Struct, structure, loot_table, stairs, foundation, disc, AIR, LAND
from mods._build import pick, lantern, scatter

M = 'fw_ruins'
BR = M + ':ancient_bricks'
CR = M + ':cracked_ancient_bricks'
MO = M + ':mossy_ancient_bricks'
TI = M + ':ancient_tiles'
PI = M + ':ancient_pillar[axis=y]'
DECAY = {BR: (MO, 0.25), M + ':ancient_tiles': (CR, 0.1)}


def brick(s):
    return pick(s, (BR, 6), (CR, 2), (MO, 3))


def ruined_house(seed):
    s = Struct(11, 8, 9, seed)
    w = brick(s)
    foundation(s, 0, 0, 10, 8, 0, TI)
    s.fill(0, 1, 0, 10, 4, 8, w, hollow=False)
    s.clear(1, 1, 1, 9, 6, 7)
    s.clear(0, 5, 0, 10, 7, 8)
    for x in (3, 7):
        s.clear(x, 2, 0, x, 3, 0)
        s.clear(x, 2, 8, x, 3, 8)
    s.clear(5, 1, 0, 5, 3, 0)
    # half fallen roof beams
    for x in range(0, 11):
        s.set(x, 5, 2, M + ':ancient_brick_slab[type=bottom,waterlogged=false]')
        if x < 6:
            s.set(x, 5, 6, M + ':ancient_brick_slab[type=bottom,waterlogged=false]')
    s.fill(1, 0, 1, 9, 0, 7, pick(s, (TI, 5), ('minecraft:coarse_dirt', 2), ('minecraft:moss_block', 2)))
    s.chest(8, 1, 6, M + ':chests/ruins', 'west')
    s.set(2, 1, 6, 'minecraft:flower_pot')
    s.set(2, 1, 2, 'minecraft:cobweb')
    s.set(8, 3, 2, 'minecraft:cobweb')
    s.set(1, 1, 4, stairs('stone_brick', 'east'))
    s.decay(remove=0.08, y_bias=0.45, replace=DECAY, min_y=1)
    scatter(s, 0, 0, 10, 8, 1, ['minecraft:fern', 'minecraft:grass', 'minecraft:moss_carpet'], 0.15)
    return s


def column_circle(seed):
    s = Struct(17, 9, 17, seed)
    c = 8
    disc(s, c, c, 0, 7, pick(s, (TI, 6), ('minecraft:mossy_cobblestone', 2), ('minecraft:moss_block', 1)))
    disc(s, c, c, 0, 3, M + ':chiseled_ancient_bricks')
    for i in range(8):
        a = i * math.pi / 4
        x, z = int(round(c + 6 * math.cos(a))), int(round(c + 6 * math.sin(a)))
        h = s.rng.randint(2, 7)
        for y in range(1, h + 1):
            s.set(x, y, z, PI)
        if h >= 6:
            s.set(x, h + 1, z, M + ':chiseled_ancient_bricks')
        elif s.rng.random() < 0.6:
            # a fallen drum next to the broken column
            s.set(x + s.rng.choice((-1, 1)), 1, z, M + ':ancient_pillar[axis=x]')
    s.set(c, 1, c, M + ':chiseled_ancient_bricks')
    s.set(c, 2, c, 'minecraft:lodestone' if s.rng.random() < 0.3 else M + ':ancient_brick_slab[type=bottom,waterlogged=false]')
    # offering chest set into the floor in front of the altar
    s.fill(c - 2, 0, c - 2, c + 2, 0, c + 2, TI)
    s.chest(c, 0, c + 1, M + ':chests/altar', 'north')
    scatter(s, 0, 0, 16, 16, 1, ['minecraft:grass', 'minecraft:fern', 'minecraft:poppy', 'minecraft:azure_bluet'], 0.12)
    return s


def broken_arch(seed):
    s = Struct(13, 10, 5, seed)
    w = brick(s)
    foundation(s, 0, 0, 12, 4, 0, TI)
    s.fill(0, 1, 1, 2, 8, 3, w)
    s.fill(10, 1, 1, 12, 6, 3, w)
    s.fill(3, 7, 1, 9, 8, 3, w)
    for x in range(3, 10):
        s.set(x, 6, 2, stairs('stone_brick', 'east' if x < 6 else 'west', 'top'))
    s.clear(7, 7, 1, 12, 9, 3)
    s.fill(1, 9, 1, 1, 9, 3, w)
    s.chest(11, 1, 0, M + ':chests/ruins', 'north')
    s.set(5, 1, 0, PI)
    s.set(8, 1, 4, M + ':ancient_pillar[axis=x]')
    s.set(9, 1, 4, M + ':ancient_pillar[axis=x]')
    s.decay(remove=0.05, y_bias=0.3, replace=DECAY, min_y=1)
    scatter(s, 0, 0, 12, 4, 1, ['minecraft:grass', 'minecraft:moss_carpet'], 0.2)
    return s


def sunken_cellar(seed):
    """A foundation with a stair going down into a small vaulted cellar."""
    s = Struct(9, 9, 9, seed)
    w = brick(s)
    g = 5  # ground level in the template
    s.fill(0, g, 0, 8, g, 8, pick(s, (TI, 4), ('minecraft:moss_block', 2), ('minecraft:coarse_dirt', 2)))
    s.walls(0, g + 1, 0, 8, g + 1, 8, w)
    s.clear(3, g + 1, 0, 5, g + 1, 0)
    s.room(1, 0, 1, 7, g - 1, 7, w, floor=TI, ceiling=TI)
    s.clear(2, g - 1, 5, 2, g, 7)
    for i, y in enumerate(range(g, 0, -1)):
        z = 7 - i
        if z < 2:
            break
        s.set(2, y, z, stairs('stone_brick', 'south'))
        s.clear(2, y + 1, z, 2, y + 2, z)
    s.chest(6, 1, 2, M + ':chests/cellar', 'west')
    s.chest(6, 1, 3, M + ':chests/ruins', 'west', kind='barrel')
    s.spawner(4, 1, 4, 'minecraft:zombie' if seed % 2 else 'minecraft:skeleton')
    lantern(s, 4, g - 2, 2)
    s.decay(remove=0.06, y_bias=0.3, replace=DECAY, min_y=g + 1)
    return s, g


def build():
    m = Mod(M, 'Ancient Ruins', 'Crumbling houses, broken arches, column circles and sunken cellars from a forgotten age dot the '
                                'overworld, with ancient bricks to build with and treasure to dig up.', tab='DECOR')
    rl = m.rl
    m.block('ancient_bricks', props='FWProps.stone(2.0f)', textures={'block/ancient_bricks': tex.bricks('#a89a7c', 'anc')})
    m.block('cracked_ancient_bricks', props='FWProps.stone(2.0f)',
            textures={'block/cracked_ancient_bricks': tex.overlay(tex.bricks('#a89a7c', 'anc'), tex.ore_overlay('#4a4236', 'ancc', 'specks', 0.6))})
    m.block('mossy_ancient_bricks', props='FWProps.stone(2.0f)',
            textures={'block/mossy_ancient_bricks': tex.overlay(tex.bricks('#a89a7c', 'anc'), tex.ore_overlay('#4f7a35', 'ancm', 'blobs', 0.9))})
    m.block('ancient_tiles', props='FWProps.stone(2.0f)', textures={'block/ancient_tiles': tex.tiles('#9b8f74', 'anct')})
    m.block('chiseled_ancient_bricks', props='FWProps.stone(2.0f)', textures={'block/chiseled_ancient_bricks': tex.chiseled('#b2a383', 'ancch')})
    column(m, 'ancient_pillar', tex.chiseled('#b8aa8c', 'ancp'), tex.polished('#b8aa8c', 'ancpt'),
           java='new RotatedPillarBlock(FWProps.stone(2.0f))', jtype='RotatedPillarBlock', tab='DECOR', side_name='ancient_pillar', end_name='ancient_pillar_top')
    stairs_slab_wall(m, 'ancient_bricks', rl('block/ancient_bricks'))
    stairs_slab_wall(m, 'ancient_tiles', rl('block/ancient_tiles'), wall=False)
    m.shaped('ancient_bricks', rl('ancient_bricks'), ['SB', 'BS'], {'S': 'minecraft:stone_bricks', 'B': 'minecraft:mud_bricks'}, 4)
    m.shaped('ancient_tiles', rl('ancient_tiles'), ['##', '##'], {'#': rl('ancient_bricks')}, 4)
    m.shapeless('mossy_ancient_bricks', rl('mossy_ancient_bricks'), [rl('ancient_bricks'), 'minecraft:vine'])
    m.smelt('cracked_ancient_bricks', rl('ancient_bricks'), rl('cracked_ancient_bricks'), 0.1, blast=False)
    m.shaped('chiseled_ancient_bricks', rl('chiseled_ancient_bricks'), ['#', '#'], {'#': rl('ancient_brick_slab')})
    m.shaped('ancient_pillar', rl('ancient_pillar'), ['#', '#'], {'#': rl('ancient_bricks')}, 2)
    m.item('ancient_coin', ('coin', '#c9a646'), desc='Struck by a forgotten kingdom. Collectors and villagers pay well for these.')
    m.item('ancient_tablet', ('tablet', '#9b8f74'), desc='Covered in worn glyphs. Use it to gain a little experience.',
           java='new com.frontierworks.ruins.TabletItem(new Item.Properties().stacksTo(16))', jtype='com.frontierworks.ruins.TabletItem')
    m.tr('msg.fw_ruins.deciphered', 'You decipher the glyphs (+%s XP)')
    m.mrecipe('fw_machines:pulverizing', 'ancient_coin', [(rl('ancient_coin'), 1)], [('fw_metals:silver_dust', 1), ('minecraft:gold_nugget', 3)], time=100)
    common = [('minecraft:bone', 10, 1, 4), ('minecraft:rotten_flesh', 8, 1, 3), ('minecraft:iron_nugget', 8, 2, 7), ('minecraft:gold_nugget', 6, 2, 6),
              (rl('ancient_coin'), 8, 1, 4), (rl('ancient_tablet'), 3), ('minecraft:brush', 2), ('minecraft:clay_ball', 6, 2, 6),
              ('minecraft:wheat_seeds', 5, 2, 5), ('minecraft:book', 3)]
    loot_table(m, 'chests/ruins', [(3, 6, common), (0, 1, [('minecraft:emerald', 4, 1, 2), ('minecraft:iron_sword', 2, 1, 1, 'enchant'),
                                                          ('minecraft:golden_apple', 1), ('minecraft:name_tag', 2)])])
    loot_table(m, 'chests/altar', [(2, 4, [(rl('ancient_coin'), 10, 2, 8), (rl('ancient_tablet'), 5, 1, 3), ('minecraft:gold_ingot', 6, 1, 4),
                                           ('minecraft:amethyst_shard', 4, 2, 6), ('minecraft:lapis_lazuli', 5, 3, 9)]),
                                   (1, 1, [('minecraft:golden_apple', 3), ('minecraft:diamond', 2), ('minecraft:book', 3, 1, 1, 'enchant'),
                                           ('fw_gems:ruby', 3, 1, 3), ('fw_gems:sapphire', 3, 1, 3)])])
    loot_table(m, 'chests/cellar', [(3, 6, common + [('minecraft:potato', 6, 2, 6), ('minecraft:coal', 6, 2, 8)]),
                                    (1, 2, [('minecraft:iron_ingot', 6, 1, 4), ('minecraft:diamond', 1), ('minecraft:saddle', 2),
                                            ('minecraft:golden_carrot', 3, 1, 4), ('minecraft:iron_pickaxe', 2, 1, 1, 'enchant')])])
    structure(m, 'ruined_house', [(ruined_house(1), 1), (ruined_house(2), 1), (ruined_house(3), 1)], LAND, spacing=26, separation=9)
    structure(m, 'column_circle', [(column_circle(4), 1), (column_circle(5), 1)], LAND, spacing=40, separation=14)
    structure(m, 'broken_arch', [(broken_arch(6), 1), (broken_arch(7), 1)], LAND, spacing=30, separation=10)
    st1, g = sunken_cellar(8)
    st2, _ = sunken_cellar(9)
    structure(m, 'sunken_cellar', [(st1, 1), (st2, 1)], LAND, spacing=34, separation=12, height=-g, terrain='beard_thin')
    return m
