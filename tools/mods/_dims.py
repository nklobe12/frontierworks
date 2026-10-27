"""Shared helpers for dimension mods: dimension types, dimensions, biomes and simple custom noise settings."""
from gen.fw import title

STEPS = ['raw_generation', 'lakes', 'local_modifications', 'underground_structures', 'surface_structures', 'strongholds',
         'underground_ores', 'underground_decoration', 'fluid_springs', 'vegetal_decoration', 'top_layer_modification']


def col(h):
    return int(h.lstrip('#'), 16)


def dimension_type(m, name, min_y, height, skylight=True, ceiling=False, ambient=0.0, fixed_time=None, effects='minecraft:overworld',
                   natural=False, ultrawarm=False, bed=False, monster_light=0):
    obj = {'ultrawarm': ultrawarm, 'natural': natural, 'coordinate_scale': 1.0, 'has_skylight': skylight, 'has_ceiling': ceiling,
           'ambient_light': ambient, 'monster_spawn_light_level': monster_light, 'monster_spawn_block_light_limit': 0,
           'piglin_safe': False, 'bed_works': bed, 'respawn_anchor_works': False, 'has_raids': False,
           'logical_height': height, 'min_y': min_y, 'height': height, 'infiniburn': '#minecraft:infiniburn_overworld', 'effects': effects}
    if fixed_time is not None:
        obj['fixed_time'] = fixed_time
    m.data('dimension_type/%s.json' % name, obj)


def dimension(m, name, settings, biome_source, lang=None):
    m.data('dimension/%s.json' % name, {'type': m.rl(name), 'generator': {'type': 'minecraft:noise', 'settings': settings, 'biome_source': biome_source}})
    m.tr('dimension.%s.%s' % (m.modid, name), lang or title(name))


def fixed(biome):
    return {'type': 'minecraft:fixed', 'biome': biome}


def checkerboard(biomes, scale=4):
    return {'type': 'minecraft:checkerboard', 'biomes': biomes, 'scale': scale}


def biome(m, name, sky, fog, features=None, spawns=None, water='#3f76e4', water_fog='#050533', grass=None, foliage=None, temp=0.5,
          downfall=0.0, precip=False, particle=None, carvers=None, lang=None, mood=True):
    feats = [[] for _ in STEPS]
    for step, ids in (features or {}).items():
        feats[STEPS.index(step)] = list(ids)
    eff = {'sky_color': col(sky), 'fog_color': col(fog), 'water_color': col(water), 'water_fog_color': col(water_fog)}
    if grass:
        eff['grass_color'] = col(grass)
    if foliage:
        eff['foliage_color'] = col(foliage)
    if particle:
        eff['particle'] = {'options': {'type': particle[0]}, 'probability': particle[1]}
    if mood:
        eff['mood_sound'] = {'sound': 'minecraft:ambient.cave', 'tick_delay': 6000, 'block_search_extent': 8, 'offset': 2.0}
    spawners = {}
    for cat, entries in (spawns or {}).items():
        spawners[cat] = [{'type': e[0], 'weight': e[1], 'minCount': e[2], 'maxCount': e[3]} for e in entries]
    m.data('worldgen/biome/%s.json' % name, {
        'has_precipitation': precip, 'temperature': temp, 'downfall': downfall, 'effects': eff,
        'spawners': spawners, 'spawn_costs': {}, 'carvers': carvers or {}, 'features': feats})
    m.tr('biome.%s.%s' % (m.modid, name), lang or title(name))
    return m.rl(name)


def simple_noise(m, name, default_block, surface_block, under_block=None, base_y=80, amplitude=0.6, roughness=0.12, height=256):
    """Rolling terrain from a height gradient plus two 2D noises; surface/under blocks via surface rules, bedrock floor."""
    span = 160.0
    grad = {'type': 'minecraft:y_clamped_gradient', 'from_y': base_y - int(span / 2), 'to_y': base_y + int(span / 2), 'from_value': 1.5, 'to_value': -1.5}
    hills = {'type': 'minecraft:mul', 'argument1': amplitude, 'argument2': {'type': 'minecraft:noise', 'noise': 'minecraft:surface', 'xz_scale': 0.35, 'y_scale': 0.0}}
    rough = {'type': 'minecraft:mul', 'argument1': roughness, 'argument2': {'type': 'minecraft:noise', 'noise': 'minecraft:gravel', 'xz_scale': 2.0, 'y_scale': 0.0}}
    caves = {'type': 'minecraft:mul', 'argument1': 0.25, 'argument2': {'type': 'minecraft:noise', 'noise': 'minecraft:cave_cheese', 'xz_scale': 1.0, 'y_scale': 1.5}}
    initial = {'type': 'minecraft:add', 'argument1': grad, 'argument2': {'type': 'minecraft:add', 'argument1': hills, 'argument2': rough}}
    final = {'type': 'minecraft:add', 'argument1': initial, 'argument2': {'type': 'minecraft:mul', 'argument1': caves,
                                                                          'argument2': {'type': 'minecraft:y_clamped_gradient', 'from_y': 0, 'to_y': base_y,
                                                                                        'from_value': 1.0, 'to_value': 0.0}}}
    blk = lambda b: {'type': 'minecraft:block', 'result_state': {'Name': b}}
    rules = [
        {'type': 'minecraft:condition', 'if_true': {'type': 'minecraft:vertical_gradient', 'random_name': 'minecraft:bedrock_floor',
                                                     'true_at_and_below': {'above_bottom': 0}, 'false_at_and_above': {'above_bottom': 5}},
         'then_run': blk('minecraft:bedrock')},
        {'type': 'minecraft:condition', 'if_true': {'type': 'minecraft:stone_depth', 'offset': 0, 'add_surface_depth': False,
                                                     'secondary_depth_range': 0, 'surface_type': 'floor'}, 'then_run': blk(surface_block)},
        {'type': 'minecraft:condition', 'if_true': {'type': 'minecraft:stone_depth', 'offset': 0, 'add_surface_depth': True,
                                                     'secondary_depth_range': 0, 'surface_type': 'floor'}, 'then_run': blk(under_block or surface_block)},
    ]
    zero = 0
    m.data('worldgen/noise_settings/%s.json' % name, {
        'sea_level': 0, 'disable_mob_generation': False, 'aquifers_enabled': False, 'ore_veins_enabled': False, 'legacy_random_source': False,
        'default_block': {'Name': default_block}, 'default_fluid': {'Name': 'minecraft:air'},
        'noise': {'min_y': 0, 'height': height, 'size_horizontal': 1, 'size_vertical': 2},
        'noise_router': {'barrier': zero, 'fluid_level_floodedness': zero, 'fluid_level_spread': zero, 'lava': zero, 'temperature': zero,
                         'vegetation': zero, 'continents': zero, 'erosion': zero, 'depth': zero, 'ridges': zero,
                         'initial_density_without_jaggedness': initial, 'final_density': final,
                         'vein_toggle': zero, 'vein_ridged': zero, 'vein_gap': zero},
        'spawn_target': [], 'surface_rule': {'type': 'minecraft:sequence', 'sequence': rules}})
    return m.rl(name)


def ore_in(m, name, ore_block, replace_block, size=6, count=8, lo=0, hi=128, extra=None):
    """Ore feature replacing a specific block (custom stone), placed by count/height range."""
    targets = [{'target': {'predicate_type': 'minecraft:block_match', 'block': replace_block}, 'state': {'Name': ore_block}}]
    if extra:
        targets += extra
    m.configured(name, {'type': 'minecraft:ore', 'config': {'size': size, 'discard_chance_on_air_exposure': 0.0, 'targets': targets}})
    m.placed(name, m.rl(name), [{'type': 'minecraft:count', 'count': count}, {'type': 'minecraft:in_square'},
                                {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'min_inclusive': {'absolute': lo},
                                                                              'max_inclusive': {'absolute': hi}}}, {'type': 'minecraft:biome'}])
    return m.rl(name)


def boulders(m, name, block, chance=4):
    m.configured(name, {'type': 'minecraft:forest_rock', 'config': {'state': {'Name': block}}})
    m.placed(name, m.rl(name), [{'type': 'minecraft:rarity_filter', 'chance': chance}, {'type': 'minecraft:in_square'},
                                {'type': 'minecraft:heightmap', 'heightmap': 'MOTION_BLOCKING'}, {'type': 'minecraft:biome'}])
    return m.rl(name)
