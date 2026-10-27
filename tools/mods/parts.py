from gen.fw import Mod, title
from gen import tex
from mods._materials import PART_METALS, WIRE_METALS, metal_color


def build():
    m = Mod('fw_parts', 'Parts', 'Plates, gears, rods and wires for every metal, hand crafting tools, and the machine frames, '
                                 'motors and coils that all Frontierworks machines are assembled from.')
    rl = m.rl
    m.item('engineers_hammer', ('hammer', '#8d939b'), java='new CraftingToolItem(new Item.Properties(), 128)', jtype='CraftingToolItem',
           tab='TOOLS', lang="Engineer's Hammer", desc='Crafting tool: hammer an ingot into a plate')
    m.item('wire_cutters', ('hook', '#c0392b'), java='new CraftingToolItem(new Item.Properties(), 128)', jtype='CraftingToolItem',
           tab='TOOLS', desc='Crafting tool: cut a plate into wires')
    m.item('hand_mortar', ('bowl', '#9e9e9e', '#6b5a48'), java='new CraftingToolItem(new Item.Properties(), 96)', jtype='CraftingToolItem',
           tab='TOOLS', desc='Crafting tool: grind an ingot or raw ore into dust')
    m.shaped('engineers_hammer', rl('engineers_hammer'), [' II', ' SI', 'S  '], {'I': '#forge:ingots/iron', 'S': '#forge:rods/wooden'})
    m.shaped('wire_cutters', rl('wire_cutters'), ['I I', ' I ', 'S S'], {'I': '#forge:ingots/iron', 'S': '#forge:rods/wooden'})
    m.shaped('hand_mortar', rl('hand_mortar'), [' F ', 'S S', ' S '], {'F': 'minecraft:flint', 'S': '#forge:stone'})

    for name in PART_METALS:
        color = metal_color(name)
        plate, gear, rod = name + '_plate', name + '_gear', name + '_rod'
        m.item(plate, ('plate', color), tags=['forge:plates/' + name, 'forge:plates'])
        m.item(gear, ('gear', color), tags=['forge:gears/' + name, 'forge:gears'])
        m.item(rod, ('rod', color), tags=['forge:rods/' + name, 'forge:rods'])
        m.shapeless(plate + '_hammering', rl(plate), [rl('engineers_hammer'), '#forge:ingots/' + name])
        m.shaped(gear, rl(gear), [' P ', 'PIP', ' P '], {'P': '#forge:plates/' + name, 'I': '#forge:ingots/' + name})
        m.shaped(rod, rl(rod), ['I', 'I'], {'I': '#forge:ingots/' + name}, 2)
        # machine recipes
        m.mrecipe('fw_machines:compressing', 'compress_%s_plate' % name, [('#forge:ingots/' + name, 1)], [(rl(plate), 1)], time=100, energy=20)
        m.mrecipe('fw_press:pressing', 'press_%s_plate' % name, [('#forge:ingots/' + name, 1), ('fw_press:plate_mold', 1)],
                  [(rl(plate), 1), ('fw_press:plate_mold', 1)], time=80, energy=30)
        m.mrecipe('fw_press:pressing', 'press_%s_gear' % name, [('#forge:ingots/' + name, 4), ('fw_press:gear_mold', 1)],
                  [(rl(gear), 1), ('fw_press:gear_mold', 1)], time=120, energy=30)
        m.mrecipe('fw_press:pressing', 'press_%s_rod' % name, [('#forge:ingots/' + name, 1), ('fw_press:rod_mold', 1)],
                  [(rl(rod), 2), ('fw_press:rod_mold', 1)], time=80, energy=30)
        m.mrecipe('fw_machines:pulverizing', 'pulverize_%s_plate' % name, [('#forge:plates/' + name, 1)],
                  [('fw_metals:%s_dust' % name if name in ('iron', 'gold', 'copper', 'tin', 'lead', 'silver', 'nickel', 'zinc', 'aluminum', 'platinum')
                    else 'fw_alloys:%s_dust' % name, 1)], time=100, energy=16)
        dust = 'fw_metals:%s_dust' % name if name in ('iron', 'gold', 'copper', 'tin', 'lead', 'silver', 'nickel', 'zinc', 'aluminum', 'platinum') else 'fw_alloys:%s_dust' % name
        m.shapeless(name + '_dust_from_mortar', dust, [rl('hand_mortar'), '#forge:ingots/' + name])
    for name in ('iron', 'gold', 'copper', 'tin', 'lead', 'silver', 'nickel', 'zinc', 'aluminum', 'platinum', 'uranium'):
        if name in ('iron', 'gold', 'copper'):
            raw = 'minecraft:raw_' + name
        else:
            raw = 'fw_metals:raw_' + name
        m.shapeless('%s_dust_from_raw_mortar' % name, 'fw_metals:%s_dust' % name if name != 'uranium' else 'fw_metals:uranium_dust', [rl('hand_mortar'), raw])

    for name in WIRE_METALS:
        color = metal_color(name)
        w = name + '_wire'
        m.item(w, ('wire', color, '#5a3a22'), tags=['forge:wires/' + name, 'forge:wires'])
        m.shapeless(w + '_cutting', rl(w), [rl('wire_cutters'), '#forge:plates/' + name], 2)
        m.mrecipe('fw_press:pressing', 'press_%s_wire' % name, [('#forge:ingots/' + name, 1), ('fw_press:wire_mold', 1)],
                  [(rl(w), 3), ('fw_press:wire_mold', 1)], time=80, energy=30)

    # components
    comps = [
        ('machine_frame', ('frame', '#8e9196'), 'Basic machine chassis'),
        ('advanced_frame', ('frame', '#5d6b80'), 'Advanced machine chassis'),
        ('elite_frame', ('frame', '#c9a23a'), 'Elite machine chassis'),
        ('ultimate_frame', ('frame', '#9b5de5'), 'Ultimate machine chassis'),
        ('motor', ('canister', '#b87333'), None),
        ('heating_coil', ('wire', '#d79a6b', '#3b3b3b'), None),
        ('cooling_coil', ('wire', '#7fd6ff', '#3b3b3b'), None),
        ('blank_card', ('card', '#2f9e64'), 'Base for upgrade cards'),
        ('fan', ('gear', '#b0b8c0'), None),
        ('glass_lens', ('lens', '#b0b8c0', None, '#bfe7ff'), None),
        ('servo', ('canister', '#5d6b80'), None),
        ('reinforced_plate', ('sheet', '#5f6670'), 'Steel plate reinforced with obsidian'),
    ]
    for name, t, desc in comps:
        m.item(name, t, desc=desc)
    m.shaped('machine_frame', rl('machine_frame'), ['PIP', 'I I', 'PIP'], {'P': '#forge:plates/iron', 'I': '#forge:ingots/tin'})
    m.shaped('advanced_frame', rl('advanced_frame'), ['PCP', 'SFS', 'PCP'], {'P': '#forge:plates/steel', 'S': '#forge:ingots/invar',
                                                                             'C': 'fw_circuits:advanced_circuit', 'F': rl('machine_frame')})
    m.shaped('elite_frame', rl('elite_frame'), ['PCP', 'SFS', 'PCP'], {'P': '#forge:plates/platinum', 'S': '#forge:ingots/electrum',
                                                                        'C': 'fw_circuits:elite_circuit', 'F': rl('advanced_frame')})
    m.shaped('ultimate_frame', rl('ultimate_frame'), ['PCP', 'SFS', 'PCP'], {'P': 'fw_mars:titanium_plate', 'S': '#forge:gems/onyx',
                                                                              'C': 'fw_circuits:quantum_circuit', 'F': rl('elite_frame')})
    m.shaped('motor', rl('motor'), [' R ', 'WIW', ' P '], {'R': '#forge:rods/iron', 'W': '#forge:wires/copper', 'I': '#forge:ingots/iron', 'P': '#forge:plates/tin'})
    m.shaped('servo', rl('servo'), ['PWP', 'WMW', 'PCP'], {'P': '#forge:plates/steel', 'W': '#forge:wires/gold', 'M': rl('motor'), 'C': 'fw_circuits:basic_circuit'})
    m.shaped('heating_coil', rl('heating_coil'), ['WWW', 'WRW', 'WWW'], {'W': '#forge:wires/constantan', 'R': '#forge:rods/iron'})
    m.shaped('cooling_coil', rl('cooling_coil'), ['WWW', 'WRW', 'WWW'], {'W': '#forge:wires/aluminum', 'R': 'minecraft:packed_ice'})
    m.shaped('blank_card', rl('blank_card'), ['PPP', 'GRG'], {'P': '#forge:plates/iron', 'G': '#forge:nuggets/gold', 'R': '#forge:dusts/redstone'})
    m.shaped('fan', rl('fan'), [' P ', 'PRP', ' P '], {'P': '#forge:plates/aluminum', 'R': '#forge:rods/iron'})
    m.shaped('glass_lens', rl('glass_lens'), [' G ', 'GDG', ' G '], {'G': '#forge:glass_panes', 'D': '#forge:dusts/glowstone'})
    m.shaped('reinforced_plate', rl('reinforced_plate'), [' O ', 'OPO', ' O '], {'O': 'fw_metals:obsidian_dust', 'P': '#forge:plates/steel'})
    return m
