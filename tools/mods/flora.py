"""Wild Flora: maple, aspen and baobab trees, six new wildflowers and berry bushes."""
from gen.fw import Mod, wood_set, tree_feature, tree_placement, cross_block, const
from gen import tex

M = 'fw_flora'
FLOWERS = {
    # name: (stem, petal, effect, duration, biomes-key, style)
    'bluebell': ('#4f8a3a', '#5a74e6', 'MobEffects.SLOW_FALLING', 8, 'forest', 'cluster'),
    'sunpetal': ('#5c9a3a', '#ffcf3a', 'MobEffects.SATURATION', 7, 'plains', 'flower'),
    'violet': ('#4a7f3a', '#9b5de5', 'MobEffects.REGENERATION', 6, 'forest', 'cluster'),
    'frostbloom': ('#5f8f7a', '#bfeaff', 'MobEffects.FIRE_RESISTANCE', 6, 'snowy', 'flower'),
    'moonflower': ('#3f6f4f', '#f2f2ff', 'MobEffects.NIGHT_VISION', 8, 'plains', 'flower'),
    'firelily': ('#5a7a2a', '#ff5a2a', 'MobEffects.DAMAGE_BOOST', 4, 'savanna', 'cluster'),
}
BERRIES = {'blueberry': ('#3f6f3a', '#3a4fd0'), 'raspberry': ('#4f7f3a', '#d02a4a')}


def build():
    m = Mod(M, 'Wild Flora', 'New trees - fiery maples, golden aspens and savanna baobabs - plus six wildflowers and blueberry and '
                             'raspberry bushes to forage.', tab='WORLD')
    rl = m.rl
    m.imp('net.minecraft.world.effect.MobEffects', 'net.minecraft.world.food.FoodProperties')
    groups = {
        'forest': m.biome_tag('flowers_forest', ['#minecraft:is_forest', 'minecraft:meadow', 'minecraft:windswept_forest']),
        'plains': m.biome_tag('flowers_plains', ['#forge:is_plains', 'minecraft:meadow', 'minecraft:sunflower_plains', 'minecraft:flower_forest']),
        'snowy': m.biome_tag('flowers_snowy', ['minecraft:snowy_plains', 'minecraft:snowy_taiga', 'minecraft:grove', 'minecraft:snowy_slopes']),
        'savanna': m.biome_tag('flowers_savanna', ['#minecraft:is_savanna', 'minecraft:sparse_jungle']),
    }

    # ---------------------------------------------------------------- trees
    for (name, bark, inner, plank, leaf, kw, biomes, chance) in (
            ('maple', '#5e4434', '#d2a679', '#b8774a', '#d9622b', dict(height=6, rand_a=3, radius=3, foliage='blob'),
             m.biome_tag('maple_trees', ['#minecraft:is_forest', 'minecraft:windswept_forest', 'minecraft:old_growth_birch_forest']), 6),
            ('aspen', '#d6d2c4', '#efe6cf', '#e0cf9e', '#e2c044', dict(height=8, rand_a=3, radius=2, foliage='blob'),
             m.biome_tag('aspen_trees', ['#minecraft:is_taiga', 'minecraft:meadow', 'minecraft:windswept_hills']), 5),
            ('baobab', '#8a7560', '#c9a77a', '#a88a62', '#7a9a3a', dict(height=5, rand_a=2, rand_b=2, radius=2, foliage='acacia', trunk='forking'),
             m.biome_tag('baobab_trees', ['#minecraft:is_savanna']), 10)):
        log, leaves, sap = wood_set(m, name, bark, inner, plank, leaf, rl(name + '_tree'))
        tree_feature(m, name + '_tree', rl(log), rl(leaves), **kw)
        tree_placement(m, name + '_tree', rl(name + '_tree'), rl(sap), chance=chance, biomes=biomes)
        m.mrecipe('fw_sawmill:sawing', name + '_log', [(rl(log), 1)], [(rl(name + '_planks'), 6), ('fw_sawmill:sawdust', 1, 0.5)], time=100, energy=16)
    m.item('maple_syrup', ('bottle', '#b5651d'), desc='Sweet! Restores a lot of hunger.',
           java='new HoneyBottleItem(new Item.Properties().craftRemainder(Items.GLASS_BOTTLE).stacksTo(16).food(new FoodProperties.Builder().nutrition(7).saturationMod(0.4f).build()))',
           jtype='HoneyBottleItem')
    m.mrecipe('fw_machines:extracting', 'maple_syrup', [(rl('maple_log'), 1)], [(rl('maple_syrup'), 1)], time=120, energy=16)
    m.shapeless('maple_syrup', rl('maple_syrup'), ['minecraft:glass_bottle', rl('maple_log'), 'minecraft:sugar'])

    # ---------------------------------------------------------------- flowers
    entries = {}
    for name, (stem, petal, effect, dur, group, style) in FLOWERS.items():
        cross_block(m, name, tex.cross_plant(stem, petal, M + name, style), 'new FlowerBlock(() -> %s, %d, FWProps.plant().offsetType(BlockBehaviour.OffsetType.XZ))' % (effect, dur),
                    jtype='FlowerBlock', tags=['minecraft:small_flowers', 'minecraft:flowers'], item_tags=['minecraft:small_flowers', 'minecraft:flowers'])
        entries.setdefault(group, []).append(rl(name))
    dyes = {'bluebell': 'blue', 'sunpetal': 'yellow', 'violet': 'purple', 'frostbloom': 'light_blue', 'moonflower': 'white', 'firelily': 'red'}
    for name, dye in dyes.items():
        m.shapeless(name + '_dye', 'minecraft:%s_dye' % dye, [rl(name)])
    for group, names in entries.items():
        fname = 'flowers_' + group
        m.configured(fname, {'type': 'minecraft:flower', 'config': {'tries': 48, 'xz_spread': 6, 'y_spread': 2, 'feature': {
            'feature': {'type': 'minecraft:simple_block', 'config': {'to_place': {'type': 'minecraft:weighted_state_provider',
                                                                                   'entries': [{'weight': 1, 'data': {'Name': n}} for n in names]}}},
            'placement': [{'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:matching_blocks', 'blocks': 'minecraft:air'}}]}}})
        m.placed(fname, rl(fname), [{'type': 'minecraft:rarity_filter', 'chance': 6}, {'type': 'minecraft:in_square'},
                                    {'type': 'minecraft:heightmap', 'heightmap': 'MOTION_BLOCKING'}, {'type': 'minecraft:biome'}])
        m.add_feature(rl(fname), groups[group], 'vegetal_decoration')

    # ---------------------------------------------------------------- berry bushes
    t = lambda n: '%s:block/%s' % (M, n)
    for name, (leaf, fruit) in BERRIES.items():
        bush, berries = name + '_bush', name[:-1] + 'ies'
        C = const(bush)
        models, textures = {}, {}
        for age in range(4):
            mn = '%s_stage%d' % (bush, age)
            models[mn] = {'parent': 'minecraft:block/cross', 'render_type': 'minecraft:cutout', 'textures': {'cross': t(mn)}}
            if age == 0:
                textures['block/' + mn] = tex.cross_plant(leaf, leaf, M + mn, 'sapling')
            else:
                img = tex.cross_plant(leaf, fruit if age > 1 else leaf, M + bush, 'bush')
                if age == 3:
                    px = img.load()
                    for i in range(9):
                        x, y = 3 + (i * 5) % 10, 5 + (i * 7) % 9
                        px[x, y] = tex.shade(tex.rgb(fruit), 1.05)
                textures['block/' + mn] = img
        m.block(bush, java='new com.frontierworks.flora.BerryBushBlock(FWProps.plant().randomTicks().sound(SoundType.SWEET_BERRY_BUSH), () -> Content.%s.get())' % const(berries),
                jtype='com.frontierworks.flora.BerryBushBlock', item=False, models=models, textures=textures, mine=None,
                state={'variants': {'age=%d' % a: {'model': t('%s_stage%d' % (bush, a))} for a in range(4)}},
                loot={'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': rl(berries)}]}]})
        m.item(berries, ('berry', fruit), java='new ItemNameBlockItem(Content.%s.get(), new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationMod(0.2f).fast().build()))' % C,
               jtype='ItemNameBlockItem', desc='Plant them to grow a bush', tags=['forge:berries', 'forge:fruits'])
        pname = bush + '_patch'
        m.configured(pname, {'type': 'minecraft:random_patch', 'config': {'tries': 24, 'xz_spread': 5, 'y_spread': 2, 'feature': {
            'feature': {'type': 'minecraft:simple_block', 'config': {'to_place': {'type': 'minecraft:simple_state_provider', 'state': {'Name': rl(bush), 'Properties': {'age': '3'}}}}},
            'placement': [{'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:all_of', 'predicates': [
                {'type': 'minecraft:matching_blocks', 'blocks': 'minecraft:air'},
                {'type': 'minecraft:matching_blocks', 'offset': [0, -1, 0], 'blocks': ['minecraft:grass_block', 'minecraft:podzol', 'minecraft:coarse_dirt']}]}}]}}})
        m.placed(pname, rl(pname), [{'type': 'minecraft:rarity_filter', 'chance': 24}, {'type': 'minecraft:in_square'},
                                    {'type': 'minecraft:heightmap', 'heightmap': 'WORLD_SURFACE_WG'}, {'type': 'minecraft:biome'}])
        m.add_feature(rl(pname), m.biome_tag(name + '_bushes', ['#minecraft:is_taiga', '#minecraft:is_forest'] if name == 'blueberry' else
                                             ['#minecraft:is_forest', '#forge:is_plains', 'minecraft:meadow']), 'vegetal_decoration')
    m.item('berry_pie', ('round_food', '#7a3a6a'), java='new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(8).saturationMod(0.4f).build()))')
    m.shapeless('berry_pie', rl('berry_pie'), [rl('blueberries'), rl('raspberries'), 'minecraft:sugar', 'minecraft:egg', 'minecraft:wheat'])
    return m
