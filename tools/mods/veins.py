from gen.ls import Mod, ore, title, custom_machine, const
from gen import tex
from mods._materials import METALS, VANILLA_METALS, VEINS

ORE_NAMES = ["coal", "iron", "copper", "gold", "redstone", "lapis", "diamond", "emerald", "tin", "lead", "silver", "nickel", "zinc",
             "aluminum", "platinum", "uranium", "cobalt", "tungsten", "quartz", "netherite_scrap", "other"]


def vein_state(m, metal, deep=False):
    if metal.startswith('minecraft:'):
        n = metal.split(':')[1]
        return {'Name': 'minecraft:%s%s_ore' % ('deepslate_' if deep else '', n)}
    return {'Name': m.rl('%s%s_ore' % ('deepslate_' if deep else '', metal))}


def outcrop_model(m, name):
    t = '%s:block/%s' % (m.modid, name)
    faces = {d: {'texture': '#all'} for d in ('north', 'south', 'east', 'west', 'up', 'down')}
    return {'parent': 'minecraft:block/block', 'textures': {'all': t, 'particle': t}, 'elements': [
        {'from': [2, 0, 3], 'to': [9, 5, 10], 'faces': faces},
        {'from': [7, 0, 6], 'to': [14, 4, 13], 'faces': faces},
        {'from': [4, 0, 9], 'to': [9, 3, 14], 'faces': faces}]}


def build():
    m = Mod('ls_veins', 'Deep Veins', 'Ten new metals that form huge ore veins winding through the rock, with outcrops on the surface '
                                      'above them. Strike rock with a prospector\'s pick to hear which ores lie nearby and in which direction, '
                                      'or power a seismic surveyor to chart every ore under a 5x5 chunk area layer by layer.', tab='MATERIALS')
    m.init('VeinsSetup.init(bus)')
    rl = m.rl
    for name, (color, level, hardness, gen) in METALS.items():
        ore(m, name + '_ore', color, 'stone', drop=rl('raw_' + name), hardness=hardness, level=level,
            item_tags=['forge:ores/' + name, 'forge:ores'], tags=['forge:ores/' + name, 'forge:ores'], lang=title(name) + ' Ore')
        ore(m, 'deepslate_' + name + '_ore', color, 'deepslate', drop=rl('raw_' + name), hardness=hardness + 1.5, level=level,
            item_tags=['forge:ores/' + name, 'forge:ores'], tags=['forge:ores/' + name, 'forge:ores'], lang='Deepslate ' + title(name) + ' Ore')
        m.item('raw_' + name, ('raw', color), tags=['forge:raw_materials/' + name, 'forge:raw_materials'])
        m.item(name + '_ingot', ('ingot', color), tags=['forge:ingots/' + name, 'forge:ingots'])
        m.item(name + '_nugget', ('nugget', color), tags=['forge:nuggets/' + name, 'forge:nuggets'])
        m.item(name + '_dust', ('dust', color), tags=['forge:dusts/' + name, 'forge:dusts'])
        m.block(name + '_block', props='LSProps.metal(5.0f)', textures={'block/%s_block' % name: tex.metal_block(color, m.modid + name)},
                level=level, tags=['forge:storage_blocks/' + name, 'forge:storage_blocks', 'minecraft:beacon_base_blocks'],
                item_tags=['forge:storage_blocks/' + name, 'forge:storage_blocks'], lang='Block of ' + title(name))
        m.block('raw_%s_block' % name, props='LSProps.stone(5.0f)', textures={'block/raw_%s_block' % name: tex.raw_block(color, m.modid + 'raw' + name)},
                level=level, tags=['forge:storage_blocks/raw_' + name], item_tags=['forge:storage_blocks/raw_' + name],
                lang='Block of Raw ' + title(name))
        # outcrop marking veins
        oc = name + '_outcrop'
        stone_img = tex.stone('#86888c', m.modid + oc)
        m.block(oc, java='new OutcropBlock(LSProps.stone(0.8f).noOcclusion(), "%s")' % name, jtype='OutcropBlock',
                models={oc: outcrop_model(m, oc)}, textures={'block/' + oc: tex.overlay(stone_img, tex.ore_overlay(color, m.modid + oc + 'o', 'specks', 1.4))},
                loot=('ore', rl('raw_' + name), 1, 2), lang=title(name) + ' Outcrop',
                desc='Weathered rock that hints at a %s vein somewhere below' % title(name))

        size, count, lo, hi, shape = gen
        m.ore_gen('ore_' + name, rl(name + '_ore'), rl('deepslate_%s_ore' % name), size=size, count=count, min_y=lo, max_y=hi, shape=shape)

        ing, nug, blk, raw, rawb = rl(name + '_ingot'), rl(name + '_nugget'), rl(name + '_block'), rl('raw_' + name), rl('raw_%s_block' % name)
        m.shaped(name + '_block', blk, ['###', '###', '###'], {'#': '#forge:ingots/' + name})
        m.shapeless(name + '_ingot_from_block', ing, ['#forge:storage_blocks/' + name], 9)
        m.shaped(name + '_ingot_from_nuggets', ing, ['###', '###', '###'], {'#': '#forge:nuggets/' + name})
        m.shapeless(name + '_nugget', nug, ['#forge:ingots/' + name], 9)
        m.shaped('raw_%s_block' % name, rawb, ['###', '###', '###'], {'#': raw})
        m.shapeless('raw_%s' % name, raw, [rawb], 9)
        m.smelt(name + '_ingot_from_raw', raw, ing, 0.7)
        m.smelt(name + '_ingot_from_ore', '#forge:ores/' + name, ing, 0.7)
        m.smelt(name + '_ingot_from_dust', '#forge:dusts/' + name, ing, 0.3, time=150)
        m.tag('items', 'forge:ingots', '#forge:ingots/' + name)

    # vanilla metal dusts, copper nuggets, common dusts
    for name, color in VANILLA_METALS.items():
        m.item(name + '_dust', ('dust', color), tags=['forge:dusts/' + name, 'forge:dusts'])
        m.smelt(name + '_ingot_from_dust', '#forge:dusts/' + name, 'minecraft:%s_ingot' % name, 0.3, time=150)
    m.item('copper_nugget', ('nugget', VANILLA_METALS['copper']), tags=['forge:nuggets/copper', 'forge:nuggets'])
    m.shapeless('copper_nugget', rl('copper_nugget'), ['minecraft:copper_ingot'], 9)
    m.shaped('copper_ingot_from_nuggets', 'minecraft:copper_ingot', ['###', '###', '###'], {'#': '#forge:nuggets/copper'})
    for name, color in {'coal': '#2b2b2e', 'diamond': '#7ee6e0', 'emerald': '#2ecc71', 'lapis': '#2348b8', 'quartz': '#ede6dd',
                        'obsidian': '#3a2a55', 'ender_pearl': '#2a8f7e', 'netherite': '#4a3b3b', 'sulfur': '#e8d84a'}.items():
        m.item(name + '_dust', ('dust', color), tags=['forge:dusts/' + name, 'forge:dusts'])
    m.smelt('diamond_from_dust', '#forge:dusts/diamond', 'minecraft:diamond', 1.0, blast=False)
    m.smelt('emerald_from_dust', '#forge:dusts/emerald', 'minecraft:emerald', 1.0, blast=False)
    m.smelt('netherite_scrap_from_dust', '#forge:dusts/netherite', 'minecraft:netherite_scrap', 2.0, blast=False)

    # ----------------------------------------------------------------- big veins
    vein_rows = []
    for vname, (prim, sec, chance, rarity, lo, hi, radius, length) in VEINS.items():
        cfg = {'ore': vein_state(m, prim), 'deep_ore': vein_state(m, prim, True), 'secondary_chance': chance, 'density': 0.55,
               'radius': radius, 'length': length}
        if sec:
            cfg['secondary'] = vein_state(m, sec)
            cfg['deep_secondary'] = vein_state(m, sec, True)
        if not prim.startswith('minecraft:'):
            cfg['indicator'] = {'Name': rl(prim + '_outcrop')}
        m.configured('vein_' + vname, {'type': 'ls_veins:vein', 'config': cfg})
        m.placed('vein_' + vname, rl('vein_' + vname), [
            {'type': 'minecraft:rarity_filter', 'chance': rarity}, {'type': 'minecraft:in_square'},
            {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'min_inclusive': {'absolute': lo}, 'max_inclusive': {'absolute': hi}}},
            {'type': 'minecraft:biome'}])
        m.add_feature(rl('vein_' + vname), '#minecraft:is_overworld', 'underground_ores')
        pn = prim.split(':')[-1]
        sn = sec.split(':')[-1] if sec else ''
        vein_rows.append('        new Vein("%s", "%s", "%s", %sf, %d, %d, %d)' % (vname, pn, sn, chance, rarity, lo, hi))
        m.tr('vein.ls_veins.' + vname, title(vname) + ' Vein')

    m.java_file('VeinTable.java', '\n'.join([
        '// GENERATED by tools/mods/veins.py - do not edit', 'package com.lodestar.veins;', '', 'import java.util.List;', '',
        '/** The big vein types, for the Lens vein pages and the Codex. */', 'public final class VeinTable {', '    private VeinTable() {}', '',
        '    public record Vein(String id, String primary, String secondary, float secondaryChance, int rarity, int minY, int maxY) {}', '',
        '    public static final List<Vein> ALL = List.of(', ',\n'.join(vein_rows), '    );', '}', '']))

    # ----------------------------------------------------------------- tools and machines
    for tier, (mat, radius, dur, color) in {'copper': ('minecraft:copper_ingot', 6, 96, '#d9844f'), 'iron': ('#forge:ingots/iron', 9, 256, '#d8d8d8'),
                                              'cobalt': ('#forge:ingots/cobalt', 13, 1024, '#3c6fd6')}.items():
        n = '%s_prospectors_pick' % tier
        m.item(n, ('pickaxe', color, '#6b4a2b'), java='new ProspectorsPickItem(new Item.Properties().durability(%d), %d)' % (dur, radius),
               jtype='ProspectorsPickItem', model='handheld', tab='TOOLS', lang='%s Prospector\'s Pick' % title(tier))
        m.shaped(n, rl(n), ['MM ', 'MS ', '  S'], {'M': mat, 'S': '#forge:rods/wooden'})
    m.item('survey_report', ('map', '#e8e0c8', None, '#8b6a3a'), java='new SurveyReportItem(new Item.Properties())', jtype='SurveyReportItem',
           tab='TOOLS', desc='Right-click to read the depth chart')
    custom_machine(m, 'seismic_surveyor', 'SurveyorBE', tier='basic', glyph='wave', tab='INDUSTRY',
                   desc='Charts every ore under the surrounding 5x5 chunks.\nNeeds power and a sheet of paper per survey.')
    m.shaped('seismic_surveyor', rl('seismic_surveyor'), ['PCP', 'RFR', 'GTG'],
             {'P': 'minecraft:piston', 'C': 'minecraft:compass', 'R': '#forge:dusts/redstone', 'F': 'ls_machina:machine_frame',
              'G': '#forge:gears/iron', 'T': 'minecraft:tnt'})

    for n in ORE_NAMES:
        m.tr('ore.ls_veins.' + n, {'lapis': 'Lapis Lazuli', 'netherite_scrap': 'Ancient Debris', 'quartz': 'Nether Quartz', 'other': 'Other ores'}.get(n, title(n)))
    for k, v in {
        'msg.ls_veins.no_traces': 'No traces of ore anywhere near here.',
        'msg.ls_veins.traces': '%s traces of %s %s, %s (about %s blocks away)',
        'msg.ls_veins.also': 'Also: %s (%s)',
        'msg.ls_veins.strength.faint': 'Faint', 'msg.ls_veins.strength.moderate': 'Moderate',
        'msg.ls_veins.strength.strong': 'Strong', 'msg.ls_veins.strength.very_strong': 'Very strong',
        'msg.ls_veins.dir.here': 'right here', 'msg.ls_veins.dir.north': 'to the north', 'msg.ls_veins.dir.northeast': 'to the northeast',
        'msg.ls_veins.dir.east': 'to the east', 'msg.ls_veins.dir.southeast': 'to the southeast', 'msg.ls_veins.dir.south': 'to the south',
        'msg.ls_veins.dir.southwest': 'to the southwest', 'msg.ls_veins.dir.west': 'to the west', 'msg.ls_veins.dir.northwest': 'to the northwest',
        'msg.ls_veins.vert.below': 'below the rock you struck', 'msg.ls_veins.vert.above': 'above the rock you struck',
        'msg.ls_veins.vert.level': 'level with the rock you struck',
        'tooltip.ls_veins.prospectors_pick': 'Right-click rock to listen for ore within %s blocks',
        'gui.ls_veins.scanning': 'Surveying: %s%%', 'gui.ls_veins.insert_paper': 'Insert paper to survey',
        'gui.ls_veins.cell': '%s blocks between Y %s and %s',
        'tooltip.ls_veins.report.at': 'Surveyed around X %s, Z %s', 'tooltip.ls_veins.report.empty': 'No ore found',
        'lens.category.ls_veins.veins': 'Ore Veins', 'lens.ls_veins.depth': 'Y %s to %s', 'lens.ls_veins.rarity': '1 in %s chunks',
        'lens.ls_veins.secondary': 'with %s (%s%%)',
    }.items():
        m.tr(k, v)
    m.codex_intro('Ores in Lodestar gather in veins: long tubes of ore that wind through the rock for dozens of blocks. Small '
                  'scattered deposits exist too, but a vein can hold hundreds of ore blocks. Weathered outcrops on the surface mark '
                  'where a vein runs below.')
    m.codex_page('Prospecting', 'Strike any rock with a prospector\'s pick to listen for ore. The pick names the strongest traces it hears, '
                                'which way they lie, whether above or below, and roughly how far. Better picks hear further: copper 6 '
                                'blocks, iron 9 and cobalt 13.', items=[rl('copper_prospectors_pick'), rl('iron_prospectors_pick'), rl('cobalt_prospectors_pick')])
    m.codex_page('Seismic surveys', 'The seismic surveyor charts every ore under the 5x5 chunks around it. Give it power and a sheet of '
                                    'paper; it scans one 16-block layer at a time and prints a survey report. Right-click the report to '
                                    'read a depth chart with a column for each ore.', items=[rl('seismic_surveyor'), rl('survey_report')])
    return m
