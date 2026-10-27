import math

from gen.fw import Mod, ore
from gen import tex
from gen.structures import Struct, structure, loot_table, sphere, AIR, LAND


def crater(seed, radius):
    R = radius
    size = 2 * R + 5
    depth = max(4, R // 2 + 1)
    ground = depth + 1          # template y of the surrounding ground surface
    top = ground + 6
    s = Struct(size, top + 1, size, seed)
    c = size // 2
    rng = s.rng
    for x in range(size):
        for z in range(size):
            r = math.sqrt((x - c) ** 2 + (z - c) ** 2)
            if r <= R:
                d = int(round(depth * (1 - (r / R) ** 2)))
                floor = ground - d
                s.clear(x, floor + 1, z, x, top, z)
                pick = rng.random()
                st = ('fw_meteors:scorched_stone' if pick < 0.55 else 'minecraft:coarse_dirt' if pick < 0.75
                      else 'minecraft:magma_block' if pick < 0.82 and r < R * 0.5 else 'minecraft:cobblestone')
                s.set(x, floor, z, st)
                s.set(x, floor - 1, z, 'fw_meteors:scorched_stone' if rng.random() < 0.7 else 'minecraft:stone')
            elif r <= R + 2.2:
                # raised ejecta rim
                h = 1 + (1 if rng.random() < 0.5 else 0)
                for yy in range(ground + 1, ground + 1 + h):
                    s.set(x, yy, z, 'minecraft:coarse_dirt' if rng.random() < 0.6 else 'fw_meteors:scorched_stone')
                s.clear(x, ground + 1 + h, z, x, top, z)
    cy = ground - depth
    core = 2.2 if R < 9 else 3.0
    sphere(s, c, cy, c, core, lambda: rng.choice(['fw_meteors:meteorite', 'fw_meteors:meteorite', 'fw_meteors:meteoric_iron_ore',
                                                   'fw_meteors:meteoric_iron_ore', 'minecraft:magma_block']))
    if R >= 9:
        s.set(c, int(cy + core), c, 'fw_meteors:star_fragment_ore')
    s.chest(c + 2, ground - depth + 2, c, 'fw_meteors:chests/crater')
    return s, ground


def build():
    m = Mod('fw_meteors', 'Meteors', 'Meteor craters scar the overworld. Their cores hold meteorite, meteoric iron and rare star '
                                     'fragments - the key to meteoric steel tools and late-game tech.', tab='WORLD')
    rl = m.rl
    m.block('scorched_stone', props='FWProps.stone(1.8f)', textures={'block/scorched_stone': tex.stone('#3a3532', 'scorch', 'mottled')},
            tab='WORLD')
    m.block('meteorite', props='FWProps.stone(5.0f).lightLevel(s -> 3)', textures={'block/meteorite': tex.overlay(tex.stone('#2e2a2c', 'meteor', 'rough'),
                                                                                                              tex.ore_overlay('#ff7b2e', 'meteorglow', 'specks', 0.8))},
            level='iron', tab='WORLD', desc='A chunk of space rock, still warm')
    ore(m, 'meteoric_iron_ore', '#9db1c9', 'stone', drop=rl('raw_meteoric_iron'), hardness=5.0, level='iron', base_tex=m.rl('block/meteorite'),
        tab='WORLD', tags=['forge:ores/meteoric_iron'], item_tags=['forge:ores/meteoric_iron'])
    ore(m, 'star_fragment_ore', '#fff3a0', 'stone', drop=rl('star_fragment'), hardness=6.0, level='diamond', style='gem', xp=(8, 15),
        base_tex=m.rl('block/meteorite'), tab='WORLD')
    m.item('raw_meteoric_iron', ('raw', '#9db1c9'))
    m.item('meteoric_iron_ingot', ('ingot', '#9db1c9'), tags=['forge:ingots/meteoric_iron'])
    m.item('meteoric_steel_ingot', ('ingot', '#6f86b8'), tags=['forge:ingots/meteoric_steel'], desc='The toughest metal on this world')
    m.item('star_fragment', ('shard', '#fff3a0'), desc='A splinter of a fallen star. Hums with energy.')
    m.block('meteoric_steel_block', props='FWProps.metal(7.0f)', textures={'block/meteoric_steel_block': tex.metal_block('#6f86b8', 'msb', 'riveted')},
            level='iron', tags=['forge:storage_blocks/meteoric_steel', 'minecraft:beacon_base_blocks'])
    m.smelt('meteoric_iron', rl('raw_meteoric_iron'), rl('meteoric_iron_ingot'), 1.0)
    m.smelt('meteoric_iron_from_ore', rl('meteoric_iron_ore'), rl('meteoric_iron_ingot'), 1.0)
    m.mrecipe('fw_alloysmelter:alloying', 'meteoric_steel', [(rl('meteoric_iron_ingot'), 1), ('#forge:ingots/steel', 2)], [(rl('meteoric_steel_ingot'), 2)],
              time=300, energy=60)
    m.shaped('meteoric_steel_block', rl('meteoric_steel_block'), ['###', '###', '###'], {'#': rl('meteoric_steel_ingot')})
    m.shapeless('meteoric_steel_from_block', rl('meteoric_steel_ingot'), [rl('meteoric_steel_block')], 9)
    m.mrecipe('fw_machines:pulverizing', 'meteorite', [(rl('meteorite'), 1)], [('fw_metals:iron_dust', 2), ('fw_metals:nickel_dust', 1, 0.5),
                                                                               (rl('raw_meteoric_iron'), 1, 0.2)], time=200, energy=24)
    loot_table(m, 'chests/crater', [(2, 4, [(rl('raw_meteoric_iron'), 10, 1, 4), ('fw_metals:nickel_ingot', 6, 1, 3), (rl('star_fragment'), 1),
                                            ('minecraft:iron_ingot', 8, 2, 5), ('fw_crystals:energite_shard', 5, 1, 3)])])
    for name, r, spacing, sep in (('crater_small', 6, 36, 12), ('crater_medium', 9, 52, 18), ('crater_large', 12, 80, 28)):
        st1, ground = crater(r * 31, r)
        st2, _ = crater(r * 31 + 7, r)
        structure(m, name, [(st1, 1), (st2, 1)], LAND, spacing=spacing, separation=sep, terrain='none',
                  height=-ground)
    return m
