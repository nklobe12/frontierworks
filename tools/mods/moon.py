"""The Moon: a grey, airless, low-gravity world reached by rocket, rich in helium-3."""
from gen.fw import Mod, ore, stairs_slab_wall
from gen import tex
from mods._dims import dimension_type, dimension, biome, fixed, simple_noise, ore_in

M = 'fw_moon'


def build():
    m = Mod(M, 'The Moon', 'A grey, airless world with one-third gravity, reached by a Tier 1 rocket. Bring a space suit (or an oxygen '
                           'generator). Mine helium-3 crystals for advanced circuits and fusion, and collect moon dust and moon rock.', tab='SPACE')
    rl = m.rl
    m.init('com.frontierworks.core.util.LowGravity.register("fw_moon:moon", 0.052)')
    dimension_type(m, 'moon', 0, 256, skylight=True, ambient=0.0, fixed_time=6000, effects='minecraft:overworld')
    m.block('lunar_regolith', java='new Block(FWProps.sand(MapColor.COLOR_LIGHT_GRAY))', textures={'block/lunar_regolith': tex.noise_fill('#a8a8aa', 'regolith', 0.12)},
            mine='shovel', tab='SPACE', tags=['minecraft:mineable/shovel'])
    m.block('moon_rock', java='new Block(FWProps.stone(1.8f))', textures={'block/moon_rock': tex.stone('#7a7a7e', 'moonrock', 'rough')}, tab='SPACE')
    m.block('polished_moon_rock', java='new Block(FWProps.stone(1.8f))', textures={'block/polished_moon_rock': tex.polished('#8a8a8e', 'pmoon')}, tab='SPACE')
    m.block('moon_rock_bricks', java='new Block(FWProps.stone(1.8f))', textures={'block/moon_rock_bricks': tex.bricks('#8a8a8e', 'mbrick')}, tab='SPACE')
    stairs_slab_wall(m, 'moon_rock_bricks', '%s:block/moon_rock_bricks' % M, tab='SPACE', props='FWProps.stone(1.8f)')
    m.shaped('polished_moon_rock', rl('polished_moon_rock'), ['SS', 'SS'], {'S': rl('moon_rock')}, 4)
    m.shaped('moon_rock_bricks', rl('moon_rock_bricks'), ['SS', 'SS'], {'S': rl('polished_moon_rock')}, 4)
    ore(m, 'helium3_ore', '#c8f0ff', base_tex='%s:block/moon_rock' % M, drop=rl('helium3_crystal'), xp=(2, 6), hardness=3.5, style='gem', level='iron', tab='SPACE')
    ore(m, 'lunar_iron_ore', '#d8b8a0', base_tex='%s:block/moon_rock' % M, drop='minecraft:raw_iron', hardness=3.0, level='stone', tab='SPACE',
        overlay_name='lunar_iron_overlay')
    m.item('helium3_crystal', ('gem', '#c8f0ff'), desc='Fusion fuel and quantum circuitry', tags=['forge:gems/helium3'], tab='SPACE', lang='Helium-3 Crystal')
    m.item('moon_dust', ('dust', '#a8a8aa'), tags=['forge:dusts/moon'], tab='SPACE')
    m.mrecipe('fw_machines:pulverizing', 'lunar_regolith', [(rl('lunar_regolith'), 1)], [(rl('moon_dust'), 2), (rl('helium3_crystal'), 1, 0.05)], time=80)
    m.mrecipe('fw_machines:pulverizing', 'moon_rock', [(rl('moon_rock'), 1)], [(rl('moon_dust'), 1), ('minecraft:iron_nugget', 2, 0.25)], time=100)
    m.smelt('moon_glass', rl('lunar_regolith'), 'minecraft:light_gray_stained_glass', 0.1, blast=False)
    noise = simple_noise(m, 'moon', rl('moon_rock'), rl('lunar_regolith'), base_y=80, amplitude=0.45, roughness=0.18)
    he3 = ore_in(m, 'helium3_ore', rl('helium3_ore'), rl('moon_rock'), size=5, count=10, lo=0, hi=110)
    fe = ore_in(m, 'lunar_iron_ore', rl('lunar_iron_ore'), rl('moon_rock'), size=8, count=12, lo=0, hi=110)
    b = biome(m, 'lunar_highlands', '#000000', '#0c0c10', features={'underground_ores': [he3, fe]}, mood=False, temp=0.5)
    dimension(m, 'moon', noise, fixed(b), lang='The Moon')
    return m
