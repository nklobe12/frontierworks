"""Glowing Caves: bioluminescent fungi, luminous moss and crystal growths light up the overworld's caves."""
from gen.fw import Mod, cross_block
from gen import tex

M = 'fw_caves'


def floor_patch(m, name, states, tries=48, spread=6, count=20, lo=-56, hi=40, biomes='#minecraft:is_overworld'):
    """Scatter one or more block states over cave floors between lo and hi."""
    if len(states) == 1:
        provider = {'type': 'minecraft:simple_state_provider', 'state': states[0]}
    else:
        provider = {'type': 'minecraft:weighted_state_provider', 'entries': [{'weight': 1, 'data': st} for st in states]}
    m.configured(name, {'type': 'minecraft:random_patch', 'config': {'tries': tries, 'xz_spread': spread, 'y_spread': 3, 'feature': {
        'feature': {'type': 'minecraft:simple_block', 'config': {'to_place': provider}},
        'placement': [{'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:matching_blocks', 'blocks': 'minecraft:air'}}]}}})
    m.placed(name, m.rl(name), [
        {'type': 'minecraft:count', 'count': count}, {'type': 'minecraft:in_square'},
        {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'min_inclusive': {'absolute': lo}, 'max_inclusive': {'absolute': hi}}},
        {'type': 'minecraft:environment_scan', 'direction_of_search': 'down', 'max_steps': 12, 'target_condition': {'type': 'minecraft:solid'},
         'allowed_search_condition': {'type': 'minecraft:matching_blocks', 'blocks': 'minecraft:air'}},
        {'type': 'minecraft:random_offset', 'xz_spread': 0, 'y_spread': 1}, {'type': 'minecraft:biome'}])
    m.add_feature(m.rl(name), biomes, 'underground_decoration')


def build():
    m = Mod(M, 'Glowing Caves', 'The overworld\'s caves come alive with bioluminescent glowcaps, luminous moss and crystal growths - '
                                'natural light for spelunkers, and ingredients for lamps and night-vision stew.', tab='WORLD')
    rl = m.rl
    plant = 'FWProps.plant().lightLevel(s -> %d).offsetType(BlockBehaviour.OffsetType.XZ)'
    cross_block(m, 'glowcap', tex.cross_plant('#d8e8e0', '#4fe3c1', 'glowcap', 'mushroom'),
                'new com.frontierworks.caves.CavePlantBlock(%s)' % (plant % 11), jtype='com.frontierworks.caves.CavePlantBlock',
                desc='Glows softly. Makes night-vision stew.')
    cross_block(m, 'emberstool', tex.cross_plant('#e0d0c0', '#ff7a2e', 'emberstool', 'mushroom'),
                'new com.frontierworks.caves.CavePlantBlock(%s)' % (plant % 8), jtype='com.frontierworks.caves.CavePlantBlock')
    cross_block(m, 'cave_crystal', tex.cross_plant('#b38cff', '#c9a6ff', 'cavecr', 'crystal'),
                'new com.frontierworks.caves.CavePlantBlock(FWProps.crystal(9).noCollission().noOcclusion().offsetType(BlockBehaviour.OffsetType.XZ))',
                jtype='com.frontierworks.caves.CavePlantBlock', loot=('item', rl('cave_crystal_shard'), 2), mine='pickaxe',
                desc='A cluster of glowing crystal')
    m.item('cave_crystal_shard', ('shard', '#c9a6ff'), tags=['forge:gems/cave_crystal'])
    lm_tex = tex.overlay(tex.noise_fill('#2f6b4f', 'lmoss', 0.15), tex.ore_overlay('#8dffc8', 'lmossg', 'specks', 1.4))
    m.block('luminous_moss', java='new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.1f).sound(SoundType.MOSS).lightLevel(s -> 10))',
            textures={'block/luminous_moss': lm_tex}, mine='hoe', tab='WORLD')
    t = lambda n: '%s:block/%s' % (M, n)
    m.block('luminous_moss_carpet', java='new CarpetBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.1f).sound(SoundType.MOSS_CARPET).lightLevel(s -> 6))',
            jtype='CarpetBlock', models={'luminous_moss_carpet': {'parent': 'minecraft:block/carpet', 'textures': {'wool': t('luminous_moss')}}}, mine='hoe', tab='WORLD')
    m.shaped('luminous_moss_carpet', rl('luminous_moss_carpet'), ['##'], {'#': rl('luminous_moss')}, 3)
    m.shapeless('luminous_moss', rl('luminous_moss'), ['minecraft:moss_block', rl('glowcap'), rl('glowcap')])
    m.block('crystal_lamp', java='new Block(FWProps.crystal(15).requiresCorrectToolForDrops())',
            textures={'block/crystal_lamp': tex.lamp('#c9a6ff', True, 'crlamp')}, tab='DECOR', desc='Bright, cold light')
    m.shaped('crystal_lamp', rl('crystal_lamp'), ['GSG', 'SCS', 'GSG'], {'G': 'minecraft:glass', 'S': rl('cave_crystal_shard'), 'C': 'minecraft:glowstone_dust'}, 2)
    m.item('glowcap_stew', ('bowl', '#4fe3c1'), desc='Night vision for two minutes',
           java='new BowlFoodItem(new Item.Properties().stacksTo(1).food(new FoodProperties.Builder().nutrition(6).saturationMod(0.6f)'
                '.effect(() -> new MobEffectInstance(MobEffects.NIGHT_VISION, 2400, 0), 1.0f).build()))', jtype='BowlFoodItem')
    m.imp('net.minecraft.world.food.FoodProperties', 'net.minecraft.world.effect.MobEffectInstance', 'net.minecraft.world.effect.MobEffects')
    m.shapeless('glowcap_stew', rl('glowcap_stew'), ['minecraft:bowl', rl('glowcap'), rl('glowcap'), 'minecraft:brown_mushroom'])
    m.item('ember_tea', ('bottle', '#ff7a2e'), desc='Fire resistance for one minute',
           java='new Item(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(1).saturationMod(0.1f).alwaysEat()'
                '.effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200, 0), 1.0f).build()))')
    m.shapeless('ember_tea', rl('ember_tea'), ['minecraft:glass_bottle', rl('emberstool'), rl('emberstool'), 'minecraft:sugar'])
    m.mrecipe('fw_machines:pulverizing', 'cave_crystal_shard', [(rl('cave_crystal_shard'), 1)], [('minecraft:glowstone_dust', 2), ('minecraft:amethyst_shard', 1, 0.3)], time=80)
    m.smelt('glowcap_dye', rl('glowcap'), 'minecraft:cyan_dye', 0.1, blast=False)
    floor_patch(m, 'glowcap_patch', [{'Name': rl('glowcap')}], count=10, lo=-56, hi=40)
    floor_patch(m, 'emberstool_patch', [{'Name': rl('emberstool')}], count=8, lo=-60, hi=0)
    floor_patch(m, 'luminous_moss_patch', [{'Name': rl('luminous_moss_carpet')}], tries=64, spread=5, count=8, lo=-40, hi=50)
    floor_patch(m, 'cave_crystal_patch', [{'Name': rl('cave_crystal')}], tries=24, spread=4, count=6, lo=-60, hi=10)
    return m
