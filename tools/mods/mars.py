"""Mars: a red, dusty, thin-aired world reached by a Tier 2 rocket - the only source of titanium."""
from gen.fw import Mod, ore, stairs_slab_wall
from gen import tex
from mods._dims import dimension_type, dimension, biome, fixed, simple_noise, ore_in

M = 'fw_mars'


def build():
    m = Mod(M, 'Mars', 'The red planet, reached by a Tier 2 rocket: rust-red dunes and canyons under an orange sky, low gravity and no '
                       'breathable air. Mars is the only source of titanium - the strongest metal in the pack - and martian dust.', tab='SPACE')
    rl = m.rl
    m.init('com.frontierworks.core.util.LowGravity.register("fw_mars:mars", 0.045)')
    dimension_type(m, 'mars', 0, 256, skylight=True, ambient=0.0, effects='minecraft:overworld')
    m.block('mars_sand', java='new Block(FWProps.sand(MapColor.COLOR_ORANGE))', textures={'block/mars_sand': tex.noise_fill('#b8582e', 'marssand', 0.12)},
            mine='shovel', tab='SPACE')
    m.block('mars_rock', java='new Block(FWProps.stone(2.0f))', textures={'block/mars_rock': tex.stone('#8a3e22', 'marsrock', 'layered')}, tab='SPACE')
    m.block('mars_rock_bricks', java='new Block(FWProps.stone(2.0f))', textures={'block/mars_rock_bricks': tex.bricks('#9a4a2a', 'marsbrick')}, tab='SPACE')
    stairs_slab_wall(m, 'mars_rock_bricks', '%s:block/mars_rock_bricks' % M, tab='SPACE', props='FWProps.stone(2.0f)')
    m.shaped('mars_rock_bricks', rl('mars_rock_bricks'), ['SS', 'SS'], {'S': rl('mars_rock')}, 4)
    ore(m, 'titanium_ore', '#c9d3de', base_tex='%s:block/mars_rock' % M, drop=rl('raw_titanium'), hardness=5.0, level='diamond', tab='SPACE')
    m.item('raw_titanium', ('raw', '#b8c2cc'), tags=['forge:raw_materials', 'forge:raw_materials/titanium'], tab='SPACE')
    m.item('titanium_ingot', ('ingot', '#c9d3de'), tags=['forge:ingots', 'forge:ingots/titanium'], tab='SPACE')
    m.item('titanium_nugget', ('nugget', '#c9d3de'), tags=['forge:nuggets', 'forge:nuggets/titanium'], tab='SPACE')
    m.item('titanium_dust', ('dust', '#c9d3de'), tags=['forge:dusts', 'forge:dusts/titanium'], tab='SPACE')
    m.item('titanium_plate', ('plate', '#c9d3de'), tags=['forge:plates', 'forge:plates/titanium'], tab='SPACE')
    m.item('mars_dust', ('dust', '#b8582e'), tags=['forge:dusts/mars'], tab='SPACE')
    m.block('titanium_block', java='new Block(FWProps.metal(6.0f))', textures={'block/titanium_block': tex.metal_block('#c9d3de', 'tiblk', 'plain')},
            tab='SPACE', level='diamond', tags=['forge:storage_blocks/titanium', 'minecraft:beacon_base_blocks'], item_tags=['forge:storage_blocks/titanium'])
    m.smelt('titanium_ingot_from_raw', rl('raw_titanium'), rl('titanium_ingot'), 1.0)
    m.smelt('titanium_ingot_from_dust', rl('titanium_dust'), rl('titanium_ingot'), 0.5)
    m.shaped('titanium_block', rl('titanium_block'), ['III', 'III', 'III'], {'I': rl('titanium_ingot')})
    m.shapeless('titanium_ingot_from_block', rl('titanium_ingot'), [rl('titanium_block')], 9)
    m.shaped('titanium_ingot_from_nuggets', rl('titanium_ingot'), ['NNN', 'NNN', 'NNN'], {'N': rl('titanium_nugget')})
    m.shapeless('titanium_nugget', rl('titanium_nugget'), [rl('titanium_ingot')], 9)
    m.mrecipe('fw_machines:pulverizing', 'raw_titanium', [(rl('raw_titanium'), 1)], [(rl('titanium_dust'), 2), (rl('titanium_dust'), 1, 0.15)], time=160)
    m.mrecipe('fw_machines:pulverizing', 'titanium_ingot', [(rl('titanium_ingot'), 1)], [(rl('titanium_dust'), 1)], time=100)
    m.mrecipe('fw_machines:pulverizing', 'mars_sand', [(rl('mars_sand'), 1)], [(rl('mars_dust'), 2)], time=60)
    m.mrecipe('fw_machines:compressing', 'titanium_plate', [(rl('titanium_ingot'), 1)], [(rl('titanium_plate'), 1)], time=200, energy=40)
    m.shaped('titanium_plate_hammer', rl('titanium_plate'), ['H', 'I', 'I'], {'H': 'fw_parts:engineers_hammer', 'I': rl('titanium_ingot')})
    noise = simple_noise(m, 'mars', rl('mars_rock'), rl('mars_sand'), base_y=72, amplitude=0.75, roughness=0.1)
    ti = ore_in(m, 'titanium_ore', rl('titanium_ore'), rl('mars_rock'), size=6, count=9, lo=0, hi=100)
    ice = ore_in(m, 'martian_ice', 'minecraft:packed_ice', rl('mars_rock'), size=20, count=2, lo=40, hi=90)
    b = biome(m, 'red_plains', '#d89a62', '#c07a50', features={'underground_ores': [ti, ice]}, temp=0.4, particle=('minecraft:white_ash', 0.004),
              mood=False, lang='Red Plains')
    dimension(m, 'mars', noise, fixed(b), lang='Mars')
    return m
