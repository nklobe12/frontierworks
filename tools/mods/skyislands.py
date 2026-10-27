"""Sky Islands: floating chunks of land drifting high above the overworld."""
import math

from gen.fw import Mod, stairs_slab_wall, ore, cross_block
from gen import tex
from gen.structures import Struct, structure, loot_table, AIR
from mods._build import pick, lantern, scatter, fence_line

M = 'fw_skyislands'
OVERWORLD = '#minecraft:is_overworld'


def island_shape(s, c, top, r, depth, rng):
    """Carrot-shaped island: grass top at y=top, tapering down `depth` blocks."""
    stone = pick(s, ('minecraft:stone', 8), ('minecraft:andesite', 2), (M + ':skystone', 3), ('minecraft:tuff', 1))
    for x in range(s.size[0]):
        for z in range(s.size[2]):
            d = math.sqrt((x - c) ** 2 + (z - c) ** 2)
            edge = r + (rng.random() - 0.5) * 1.6
            if d > edge:
                continue
            k = d / max(1.0, edge)
            bottom = top - int(depth * (1 - k ** 1.6)) - (rng.randint(0, 1) if k < 0.8 else 0)
            for y in range(max(0, bottom), top + 1):
                if y == top:
                    st = 'minecraft:grass_block[snowy=false]'
                elif y >= top - 2:
                    st = 'minecraft:dirt'
                else:
                    st = stone()
                s.set(x, y, z, st)


def oak(s, x, y, z, h, log='minecraft:oak_log[axis=y]', leaves='minecraft:oak_leaves[distance=1,persistent=true,waterlogged=false]'):
    for i in range(h):
        s.set(x, y + i, z, log)
    for dy in range(h - 2, h + 2):
        rad = 2 if dy < h else 1
        for dx in range(-rad, rad + 1):
            for dz in range(-rad, rad + 1):
                if abs(dx) == rad and abs(dz) == rad and (dy >= h or s.rng.random() < 0.5):
                    continue
                if s.get(x + dx, y + dy, z + dz) is None:
                    s.set(x + dx, y + dy, z + dz, leaves)


def ore_pocket(s, cx, cy, cz, ore, n):
    for _ in range(n):
        x, y, z = cx + s.rng.randint(-1, 1), cy + s.rng.randint(-1, 1), cz + s.rng.randint(-1, 1)
        if s.get(x, y, z) not in (None, AIR, 'minecraft:grass_block[snowy=false]'):
            s.set(x, y, z, ore)


def island(seed, r, kind):
    depth = r + 3
    size = 2 * r + 5
    c = size // 2
    top = depth + 1
    s = Struct(size, top + 9, size, seed)
    island_shape(s, c, top, r, depth, s.rng)
    ores = ['minecraft:iron_ore', 'minecraft:coal_ore', 'minecraft:gold_ore', 'minecraft:lapis_ore', M + ':aether_crystal_ore',
            'fw_metals:aluminum_ore', 'fw_metals:silver_ore', 'minecraft:diamond_ore']
    for i in range(2 + r // 3):
        ore_pocket(s, c + s.rng.randint(-r // 2, r // 2), top - 3 - s.rng.randint(0, depth // 2), c + s.rng.randint(-r // 2, r // 2),
                   s.rng.choice(ores[:6]) if i else M + ':aether_crystal_ore', 5)
    if kind == 'grove':
        oak(s, c - 1, top + 1, c - 1, 5)
        if r >= 7:
            oak(s, c + 3, top + 1, c + 2, 4, 'minecraft:birch_log[axis=y]', 'minecraft:birch_leaves[distance=1,persistent=true,waterlogged=false]')
        s.chest(c + 1, top + 1, c - 2, M + ':chests/island', 'south')
        scatter(s, 0, 0, size - 1, size - 1, top + 1, ['minecraft:grass', 'minecraft:poppy', 'minecraft:dandelion', 'minecraft:oxeye_daisy',
                                                       M + ':cloud_bloom'], 0.25, only_on=['minecraft:grass_block[snowy=false]'])
    elif kind == 'shrine':
        for x in range(c - 2, c + 3):
            for z in range(c - 2, c + 3):
                s.set(x, top, z, 'minecraft:quartz_bricks')
        for (x, z) in ((c - 2, c - 2), (c + 2, c - 2), (c - 2, c + 2), (c + 2, c + 2)):
            for y in range(top + 1, top + 4):
                s.set(x, y, z, 'minecraft:quartz_pillar[axis=y]')
        s.fill(c - 2, top + 4, c - 2, c + 2, top + 4, c + 2, 'minecraft:smooth_quartz_slab[type=bottom,waterlogged=false]')
        s.chest(c, top + 1, c, M + ':chests/shrine', 'south')
        s.set(c, top + 3, c, 'minecraft:end_rod[facing=down]')
        scatter(s, 0, 0, size - 1, size - 1, top + 1, [M + ':cloud_bloom', 'minecraft:grass'], 0.2, only_on=['minecraft:grass_block[snowy=false]'])
    elif kind == 'pond':
        for x in range(c - 2, c + 2):
            for z in range(c - 1, c + 3):
                if (x - c + 0.5) ** 2 + (z - c - 1) ** 2 <= 4.5:
                    s.set(x, top, z, 'minecraft:water[level=0]')
                    s.set(x, top - 1, z, 'minecraft:clay')
        s.set(c - 1, top, c + 1, 'minecraft:water[level=0]')
        oak(s, c + 3, top + 1, c - 3, 5)
        s.chest(c - 3, top + 1, c - 3, M + ':chests/island', 'east')
        scatter(s, 0, 0, size - 1, size - 1, top + 1, ['minecraft:grass', 'minecraft:sugar_cane', M + ':cloud_bloom'], 0.15,
                only_on=['minecraft:grass_block[snowy=false]'])
    elif kind == 'ruin':
        for x in range(c - 3, c + 4):
            for z in range(c - 3, c + 4):
                if x in (c - 3, c + 3) or z in (c - 3, c + 3):
                    for y in range(top + 1, top + 1 + s.rng.randint(0, 3)):
                        s.set(x, y, z, s.rng.choice(['minecraft:stone_bricks', 'minecraft:mossy_stone_bricks', 'minecraft:cracked_stone_bricks']))
        s.spawner(c, top + 1, c, 'minecraft:phantom' if seed % 2 else 'minecraft:skeleton')
        s.chest(c + 2, top + 1, c + 2, M + ':chests/shrine', 'west')
    # hanging roots under the island
    for _ in range(r):
        x, z = c + s.rng.randint(-r // 2, r // 2), c + s.rng.randint(-r // 2, r // 2)
        for y in range(0, top):
            if s.get(x, y, z) not in (None, AIR):
                if y > 0 and s.get(x, y - 1, z) in (None, AIR):
                    s.set(x, y - 1, z, 'minecraft:hanging_roots[waterlogged=false]')
                break
    return s


def build():
    m = Mod(M, 'Sky Islands', 'Floating islands drift high above the overworld - groves, sky ponds, quartz shrines and haunted ruins - '
                              'with skystone, aether crystals and treasure for anyone who can get up there.', tab='WORLD')
    rl = m.rl
    m.block('skystone', props='FWProps.stone(1.5f)', textures={'block/skystone': tex.stone('#c9d3dc', 'skys', 'mottled')}, tab='WORLD',
            item_tags=['minecraft:stone_crafting_materials', 'minecraft:stone_tool_materials'])
    m.block('polished_skystone', props='FWProps.stone(1.5f)', textures={'block/polished_skystone': tex.polished('#d3dce5', 'skysp')}, tab='DECOR')
    m.block('skystone_bricks', props='FWProps.stone(1.5f)', textures={'block/skystone_bricks': tex.bricks('#c9d3dc', 'skysb')}, tab='DECOR')
    stairs_slab_wall(m, 'skystone_bricks', rl('block/skystone_bricks'))
    m.shaped('polished_skystone', rl('polished_skystone'), ['##', '##'], {'#': rl('skystone')}, 4)
    m.shaped('skystone_bricks', rl('skystone_bricks'), ['##', '##'], {'#': rl('polished_skystone')}, 4)
    ore(m, 'aether_crystal_ore', '#9ff5ff', 'stone', drop=rl('aether_crystal'), xp=(3, 7), hardness=3.0, level='iron', style='gem',
        base_tex=rl('block/skystone'), tab='WORLD')
    m.item('aether_crystal', ('gem', '#9ff5ff'), desc='Light as air. Feather Falling for anything it is crafted into.', tags=['forge:gems/aether'])
    m.item('feather_charm', ('amulet', '#9ff5ff'), desc='While in your inventory you take no fall damage',
           java='new com.frontierworks.skyislands.CharmItem(new Item.Properties().stacksTo(1).durability(256))', jtype='com.frontierworks.skyislands.CharmItem')
    m.shaped('feather_charm', rl('feather_charm'), [' S ', 'FCF', ' F '], {'S': 'minecraft:string', 'F': 'minecraft:feather', 'C': rl('aether_crystal')})
    cross_block(m, 'cloud_bloom', tex.cross_plant('#7fc28a', '#e8f6ff', 'cbl'), 'new FlowerBlock(() -> MobEffects.SLOW_FALLING, 8, FWProps.plant())',
                jtype='FlowerBlock', tab='WORLD', tags=['minecraft:small_flowers'], item_tags=['minecraft:small_flowers'])
    m.imp('net.minecraft.world.effect.MobEffects')
    m.shapeless('cloud_bloom_dye', 'minecraft:light_blue_dye', [rl('cloud_bloom')])
    m.init('com.frontierworks.skyislands.CharmItem.init()')
    loot_table(m, 'chests/island', [(3, 6, [('minecraft:apple', 8, 1, 4), ('minecraft:feather', 8, 2, 6), ('minecraft:iron_ingot', 6, 1, 4),
                                            ('minecraft:gold_ingot', 4, 1, 3), (rl('aether_crystal'), 6, 1, 3), ('minecraft:phantom_membrane', 4, 1, 2),
                                            ('minecraft:oak_sapling', 4, 1, 2), ('minecraft:elytra', 1)]), (0, 1, [(rl('feather_charm'), 2), ('minecraft:diamond', 2)])])
    loot_table(m, 'chests/shrine', [(3, 6, [(rl('aether_crystal'), 8, 2, 5), ('minecraft:quartz', 6, 2, 8), ('minecraft:ender_pearl', 4, 1, 2),
                                            ('minecraft:experience_bottle', 5, 2, 5), ('minecraft:diamond', 3, 1, 2), ('minecraft:book', 4, 1, 1, 'enchant'),
                                            ('minecraft:golden_apple', 2)]), (1, 1, [(rl('feather_charm'), 3), ('minecraft:elytra', 1), ('minecraft:totem_of_undying', 1)])])
    height = {'type': 'minecraft:uniform', 'min_inclusive': {'absolute': 175}, 'max_inclusive': {'absolute': 230}}
    structure(m, 'sky_grove', [(island(1, 6, 'grove'), 2), (island(2, 8, 'grove'), 2), (island(3, 5, 'grove'), 1)], OVERWORLD, spacing=24, separation=8,
              terrain='none', heightmap=None, height=height)
    structure(m, 'sky_pond', [(island(4, 7, 'pond'), 1), (island(5, 8, 'pond'), 1)], OVERWORLD, spacing=34, separation=12, terrain='none', heightmap=None,
              height=height)
    structure(m, 'sky_shrine', [(island(6, 7, 'shrine'), 1)], OVERWORLD, spacing=48, separation=16, terrain='none', heightmap=None, height=height)
    structure(m, 'sky_ruin', [(island(7, 6, 'ruin'), 1), (island(8, 7, 'ruin'), 1)], OVERWORLD, spacing=40, separation=14, terrain='none', heightmap=None,
              height=height)
    return m
