from gen.fw import Mod, const, title
from gen import tex

CRYSTALS = {
    # name: colour, light, geode y range, rarity, outer block
    'energite': ('#4fd8ff', 6, (-48, 30), 28, 'minecraft:smooth_basalt'),
    'voidite': ('#8a3cff', 4, (-62, -24), 40, 'minecraft:deepslate'),
}
ROT = {'down': {'x': 180}, 'east': {'x': 90, 'y': 90}, 'north': {'x': 90}, 'south': {'x': 90, 'y': 180}, 'up': {}, 'west': {'x': 90, 'y': 270}}


def build():
    m = Mod('fw_crystals', 'Crystals', 'Crystal geodes deep underground: Energite powers advanced circuits and batteries, Voidite '
                                       'bends space for teleporters. Budding blocks regrow crystals over time.', tab='MATERIALS')
    rl = m.rl
    for name, (color, light, (lo, hi), rarity, outer) in CRYSTALS.items():
        shard = name + '_shard'
        m.item(shard, ('shard', color), tags=['forge:gems/' + name, 'forge:gems'])
        m.item(name + '_dust', ('dust', color), tags=['forge:dusts/' + name])
        m.block(name + '_block', props='FWProps.crystal(%d)' % light, textures={'block/%s_block' % name: tex.gem_block(color, name + 'cb')},
                level='iron', tab='WORLD')
        stages = [('small_%s_bud' % name, 3, 4), ('medium_%s_bud' % name, 4, 3), ('large_%s_bud' % name, 5, 3), (name + '_cluster', 7, 3)]
        for i, (sn, h, off) in enumerate(stages):
            lvl = 1 + i + (light // 2)
            props = ('net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).forceSolidOn()'
                     '.noOcclusion().sound(SoundType.AMETHYST_CLUSTER).strength(1.5f).lightLevel(s -> %d)'
                     '.pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)' % lvl)
            img = tex.from_template(tex.T['crystal_cluster'], tex.palette(color))
            if i < 3:
                # smaller buds: crop the top of the cluster art
                from PIL import Image
                k = [6, 9, 12][i]
                im2 = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
                im2.alpha_composite(img.crop((0, 16 - k - 2, 16, 14)).resize((16, k)), (0, 16 - k))
                img = im2
            loot = ('ore', rl(shard), 2, 4) if i == 3 else 'none'
            state = {'variants': {'facing=%s' % f: dict({'model': '%s:block/%s' % (m.modid, sn)}, **r) for f, r in ROT.items()}}
            m.block(sn, java='new AmethystClusterBlock(%d, %d, %s)' % (h, off, props), jtype='AmethystClusterBlock', tab='WORLD',
                    state=state, models={sn: {'parent': 'minecraft:block/cross', 'render_type': 'minecraft:cutout', 'textures': {'cross': '%s:block/%s' % (m.modid, sn)}}},
                    item_model='blocktex:' + sn, textures={'block/' + sn: img}, loot=loot, level=None)
        stage_refs = ', '.join('() -> Content.%s.get()' % const(s[0]) for s in stages)
        m.block('budding_' + name, java='new BuddingCrystalBlock(FWProps.crystal(%d).randomTicks().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY), %s)' % (light, stage_refs),
                jtype='BuddingCrystalBlock', tab='WORLD', textures={'block/budding_' + name: tex.overlay(tex.gem_block(color, name + 'bud'), tex.ore_overlay('#ffffff', name, 'specks', 0.6))},
                loot='none', level='iron', desc='Grows %s crystals on its faces. Breaks when mined.' % title(name))
        m.shaped(name + '_block', rl(name + '_block'), ['##', '##'], {'#': rl(shard)})
        m.mrecipe('fw_machines:pulverizing', name + '_dust', [(rl(shard), 1)], [(rl(name + '_dust'), 1)], time=100, energy=16)
        geode = name + '_geode'
        m.configured(geode, {'type': 'minecraft:geode', 'config': {
            'blocks': {
                'filling_provider': {'type': 'minecraft:simple_state_provider', 'state': {'Name': 'minecraft:air'}},
                'inner_layer_provider': {'type': 'minecraft:simple_state_provider', 'state': {'Name': rl(name + '_block')}},
                'alternate_inner_layer_provider': {'type': 'minecraft:simple_state_provider', 'state': {'Name': rl('budding_' + name)}},
                'middle_layer_provider': {'type': 'minecraft:simple_state_provider', 'state': {'Name': 'minecraft:calcite'}},
                'outer_layer_provider': {'type': 'minecraft:simple_state_provider', 'state': {'Name': outer}},
                'inner_placements': [{'Name': rl(s[0]), 'Properties': {'facing': 'up', 'waterlogged': 'false'}} for s in stages],
                'cannot_replace': '#minecraft:features_cannot_replace',
                'invalid_blocks': '#minecraft:geode_invalid_blocks'},
            'layers': {'filling': 1.7, 'inner_layer': 2.2, 'middle_layer': 3.2, 'outer_layer': 4.2},
            'crack': {'generate_crack_chance': 0.95, 'base_crack_size': 2.0, 'crack_point_offset': 2},
            'noise_multiplier': 0.05, 'use_potential_placements_chance': 0.35, 'use_alternate_layer0_chance': 0.083,
            'placements_require_layer0_alternate': True,
            'outer_wall_distance': {'type': 'minecraft:uniform', 'value': {'min_inclusive': 4, 'max_inclusive': 6}},
            'distribution_points': {'type': 'minecraft:uniform', 'value': {'min_inclusive': 3, 'max_inclusive': 4}},
            'point_offset': {'type': 'minecraft:uniform', 'value': {'min_inclusive': 1, 'max_inclusive': 2}},
            'min_gen_offset': -16, 'max_gen_offset': 16, 'invalid_blocks_threshold': 1}})
        m.placed(geode, rl(geode), [{'type': 'minecraft:rarity_filter', 'chance': rarity}, {'type': 'minecraft:in_square'},
                                    {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'min_inclusive': {'absolute': lo}, 'max_inclusive': {'absolute': hi}}},
                                    {'type': 'minecraft:biome'}])
        m.add_feature(rl(geode), '#minecraft:is_overworld', 'local_modifications')
    m.item('charged_energite', ('shard', '#bff4ff'), desc='Energite saturated with power - used in elite circuits', tab='MATERIALS')
    m.mrecipe('fw_chemistry:reacting', 'charged_energite', [(rl('energite_shard'), 1), ('#forge:dusts/redstone', 4)], [(rl('charged_energite'), 1)],
              time=200, energy=60)
    m.shapeless('charged_energite_hand', rl('charged_energite'), [rl('energite_shard'), 'minecraft:redstone_block', '#forge:dusts/glowstone'])
    return m
