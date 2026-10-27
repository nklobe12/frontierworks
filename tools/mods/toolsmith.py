from PIL import Image, ImageDraw

from gen.ls import Mod, title, const, entity_block, horizontal_model_state, element_model
from gen import tex
from mods._materials import PART_METALS, ALLOYS, metal_color

# id: (colour, tier, head durability, head speed, head attack, handle multiplier, binding durability, trait, source item/tag ('' = cast only))
MATERIALS = {
    'wood': ('#9c7a4a', 0, 60, 2.0, 0.0, 1.0, 10, 'ecological', '#minecraft:planks'),
    'stone': ('#8a8a8a', 1, 130, 4.0, 1.0, 0.9, 15, 'stonebound', '#forge:cobblestone'),
    'flint': ('#5a5a5e', 1, 150, 5.0, 1.5, 0.85, 15, 'jagged', 'minecraft:flint'),
    'bone': ('#e8e2cc', 1, 200, 5.0, 1.25, 1.1, 20, 'fractured', 'minecraft:bone'),
    'cactus': ('#4f8a3a', 1, 210, 4.0, 1.5, 0.9, 25, 'prickly', 'minecraft:cactus'),
    'netherrack': ('#7a3030', 1, 270, 4.5, 2.0, 0.85, 25, 'hellfire', 'minecraft:netherrack'),
    'prismarine': ('#5fa8a0', 2, 430, 5.5, 2.0, 0.9, 50, 'aquadynamic', 'minecraft:prismarine_shard'),
    'end_stone': ('#dedca0', 2, 420, 4.0, 2.5, 0.95, 50, 'voidtouched', 'minecraft:end_stone'),
    'amethyst': ('#9a5cc6', 2, 340, 7.0, 2.5, 0.9, 30, 'crystalline', 'minecraft:amethyst_shard'),
    'obsidian': ('#3a2a5a', 3, 1000, 4.0, 2.5, 0.8, 150, 'hardened', 'minecraft:obsidian'),
    'slime': ('#76be6d', 0, 400, 3.0, 0.0, 1.3, 100, 'bouncy', 'minecraft:slime_ball'),
    'leather': ('#8b5a2b', 0, 100, 2.0, 0.0, 1.1, 60, 'supple', 'minecraft:leather'),
    'string': ('#eeeeee', 0, 40, 1.0, 0.0, 0.9, 40, 'supple', 'minecraft:string'),
    'blaze': ('#e8a33c', 2, 300, 6.0, 2.5, 0.8, 40, 'searing', 'minecraft:blaze_rod'),
    'diamond': ('#7ee6e0', 3, 1300, 8.0, 3.0, 0.75, 100, 'brilliant', 'minecraft:diamond'),
    'emerald': ('#2ecc71', 3, 1000, 7.5, 2.5, 1.0, 150, 'prosperous', 'minecraft:emerald'),
    # castable metals
    'copper': ('#d9844f', 1, 210, 5.0, 2.0, 1.05, 30, 'lightweight', ''),
    'iron': ('#d8d8d8', 2, 250, 6.0, 2.0, 0.85, 60, 'sturdy', ''),
    'gold': ('#f2c94c', 0, 80, 12.0, 0.5, 0.75, 10, 'lucky', ''),
    'tin': ('#c9d1d9', 1, 220, 5.5, 1.5, 1.0, 40, 'malleable', ''),
    'lead': ('#6a7690', 2, 350, 4.5, 2.5, 0.9, 80, 'dense', ''),
    'silver': ('#e4ebf2', 2, 280, 7.0, 2.0, 0.9, 40, 'holy', ''),
    'nickel': ('#d6cd98', 2, 300, 6.0, 2.0, 1.0, 50, 'magnetic', ''),
    'zinc': ('#a9bcb2', 1, 200, 6.0, 1.5, 1.0, 30, 'galvanized', ''),
    'aluminum': ('#e6eaee', 1, 190, 7.0, 1.5, 1.05, 30, 'airy', ''),
    'platinum': ('#9fd0de', 3, 600, 7.5, 2.5, 0.9, 80, 'refined', ''),
    'cobalt': ('#3c6fd6', 3, 750, 9.0, 3.0, 1.0, 90, 'momentum', ''),
    'tungsten': ('#4a4f57', 4, 1500, 6.5, 4.0, 0.9, 120, 'heavy', ''),
    'bronze': ('#c8873a', 2, 400, 6.0, 2.0, 1.1, 60, 'reinforced', ''),
    'brass': ('#e0b84a', 1, 260, 7.0, 1.5, 1.0, 40, 'resonant', ''),
    'steel': ('#7b8591', 3, 700, 7.0, 3.0, 1.05, 100, 'tempered', ''),
    'electrum': ('#ecd97a', 2, 200, 11.0, 1.5, 0.9, 30, 'swift', ''),
    'invar': ('#b7bfaa', 2, 500, 6.0, 2.5, 1.15, 70, 'balanced', ''),
    'constantan': ('#d79a6b', 2, 350, 6.5, 2.0, 1.0, 50, 'shocking', ''),
    'duralumin': ('#c6cfd8', 2, 420, 7.5, 2.0, 1.1, 50, 'lightweight', ''),
    'cobalt_steel': ('#5a77b8', 3, 1100, 8.5, 3.5, 1.2, 140, 'unyielding', ''),
    'netherite': ('#4a3b3b', 4, 2000, 9.5, 4.5, 1.25, 200, 'fireproof', ''),
}
MOLTEN = PART_METALS + ['netherite']
TEMPS = {'tin': 600, 'lead': 700, 'zinc': 750, 'aluminum': 950, 'duralumin': 950, 'silver': 1250, 'gold': 1350, 'copper': 1360,
         'bronze': 1200, 'brass': 1200, 'electrum': 1300, 'constantan': 1500, 'invar': 1700, 'iron': 1800, 'nickel': 1730, 'steel': 1800,
         'cobalt': 1770, 'platinum': 2040, 'tungsten': 3700, 'cobalt_steel': 1900, 'netherite': 2000}

TRAITS = {
    'ecological': ('Ecological', 'Slowly repairs itself in sunlight'),
    'stonebound': ('Stonebound', 'Mines faster but hits softer as it wears'),
    'jagged': ('Jagged', 'Hits harder as it wears'),
    'fractured': ('Fractured', 'Mines a little faster'),
    'prickly': ('Prickly', 'Pricks targets for extra damage, and sometimes you'),
    'hellfire': ('Hellfire', 'Sets targets on fire'),
    'aquadynamic': ('Aquadynamic', 'Mines much faster in water or rain'),
    'voidtouched': ('Voidtouched', 'Mined drops go straight into your inventory'),
    'crystalline': ('Crystalline', 'Mines faster while nearly undamaged'),
    'hardened': ('Hardened', 'Often ignores durability loss'),
    'bouncy': ('Bouncy', 'Knocks targets back'),
    'supple': ('Supple', 'More durability'),
    'searing': ('Searing', 'Smelts what it mines'),
    'brilliant': ('Brilliant', 'One extra modifier slot'),
    'prosperous': ('Prosperous', 'Ores mined drop extra experience'),
    'lightweight': ('Lightweight', 'Mines and swings a little faster'),
    'sturdy': ('Sturdy', 'More durability'),
    'lucky': ('Lucky', 'Fortune and Looting'),
    'malleable': ('Malleable', 'Easy to repair'),
    'dense': ('Dense', 'Sometimes ignores durability loss, but is slower'),
    'holy': ('Holy', 'Extra damage against the undead'),
    'magnetic': ('Magnetic', 'Pulls nearby drops to you after mining'),
    'galvanized': ('Galvanized', 'Slowly repairs itself'),
    'airy': ('Airy', 'Swings much faster'),
    'refined': ('Refined', 'Fortune'),
    'momentum': ('Momentum', 'Mines faster the longer you keep mining'),
    'heavy': ('Heavy', 'Hits much harder with more knockback, but swings slower'),
    'reinforced': ('Reinforced', 'Sometimes ignores durability loss'),
    'resonant': ('Resonant', 'Earns tool experience faster and ores drop experience'),
    'tempered': ('Tempered', 'More attack damage and durability'),
    'swift': ('Swift', 'Mines much faster'),
    'balanced': ('Balanced', 'Swings faster'),
    'shocking': ('Shocking', 'Hits may arc lightning to a nearby creature'),
    'unyielding': ('Unyielding', 'Much more durability'),
    'fireproof': ('Fireproof', 'Stronger in the Nether'),
    'crude': ('Crude', 'Extra damage against unarmored targets'),
}
MODIFIERS = {
    'haste': ('Haste', '+15% mining speed per level (block of redstone)'),
    'sharpness': ('Sharpness', '+1 attack damage per level (block of quartz)'),
    'luck': ('Luck', 'Fortune and Looting per level (block of lapis)'),
    'silky': ('Silky', 'Silk Touch (silky cloth)'),
    'reinforced': ('Reinforced', '15% chance per level to ignore durability loss (obsidian)'),
    'diamond': ('Diamond', '+500 durability, mines like diamond (diamond)'),
    'emerald': ('Emerald', '+50% durability (emerald)'),
    'netherite': ('Netherite', 'Netherite mining tier, +20% speed and damage (netherite ingot; needs diamond)'),
    'beheading': ('Beheading', 'Chance for creature heads (wither skeleton skull)'),
    'knockback': ('Knockback', 'More knockback (piston)'),
    'fiery': ('Fiery', 'Sets targets on fire (blaze powder)'),
    'expanded': ('Expanded', 'Large tools work a 5x5 area (expander; uses two slots)'),
    'smite': ('Smite', '+2.5 damage per level against the undead (silver ingot)'),
}
PARTS = {'pick_head': 'Pickaxe Head', 'axe_head': 'Axe Head', 'shovel_head': 'Shovel Head', 'sword_blade': 'Sword Blade',
         'kama_head': 'Kama Head', 'hammer_head': 'Hammer Head', 'broad_axe_head': 'Broad Axe Head', 'excavator_head': 'Excavator Head',
         'scythe_blade': 'Scythe Blade', 'tool_handle': 'Tool Handle', 'tough_handle': 'Tough Handle', 'tool_binding': 'Tool Binding',
         'tough_binding': 'Tough Binding', 'cross_guard': 'Cross Guard'}
PART_COST = {'pick_head': 2, 'axe_head': 2, 'shovel_head': 1, 'sword_blade': 2, 'kama_head': 2, 'hammer_head': 8, 'broad_axe_head': 8,
             'excavator_head': 8, 'scythe_blade': 8, 'tool_handle': 1, 'tough_handle': 3, 'tool_binding': 1, 'tough_binding': 3, 'cross_guard': 1}
# tool id -> (template, head part, handle part, binding part, display name)
TOOLS = {
    'pickaxe': ('pickaxe', 'pick_head', 'tool_handle', 'tool_binding', 'Pickaxe'),
    'axe': ('axe', 'axe_head', 'tool_handle', 'tool_binding', 'Hand Axe'),
    'shovel': ('shovel', 'shovel_head', 'tool_handle', 'tool_binding', 'Shovel'),
    'sword': ('sword', 'sword_blade', 'tool_handle', 'cross_guard', 'Sword'),
    'kama': ('hoe', 'kama_head', 'tool_handle', 'tool_binding', 'Kama'),
    'hammer': ('warhammer', 'hammer_head', 'tough_handle', 'tough_binding', 'Hammer'),
    'broad_axe': ('battleaxe', 'broad_axe_head', 'tough_handle', 'tough_binding', 'Broad Axe'),
    'excavator': ('excavator', 'excavator_head', 'tough_handle', 'tough_binding', 'Excavator'),
    'scythe': ('scythe', 'scythe_blade', 'tough_handle', 'tough_binding', 'Scythe'),
}
SCYTHE = [
    "................",
    "...KKKKKKKK.....",
    "..K5WWW5554KK...",
    ".K5W44KKKK443K..",
    ".K4KK.....KK32K.",
    ".KK........KcK1K",
    "..........KcbK.K",
    ".........KcbK...",
    "........KcbK....",
    ".......KcbK.....",
    "......KcbK......",
    ".....KcbK.......",
    "....KcbK........",
    "...KcbK.........",
    "..KbaK..........",
    "..KKK...........",
]
BINDING = [
    "................", "................", "................", "................",
    "......K..K......", ".....KbKKbK.....", "....KbcbbcbK....", "....KcbccbcK....",
    "....KbcbbcbK....", ".....KbKKbK.....", "......K..K......", "................",
    "................", "................", "................", "................"]
TOUGH_BINDING = [
    "................", "................", "................", "....K.K..K.K....",
    "...KbKbKKbKbK...", "..KbcbcbbcbcbK..", "..KcbcbccbcbcK..", "..KbcbcbbcbcbK..",
    "..KcbcbccbcbcK..", "..KbcbcbbcbcbK..", "...KbKbKKbKbK...", "....K.K..K.K....",
    "................", "................", "................", "................"]
GUARD = [
    "................", "................", "................", "................",
    "................", "................", ".KKKKKKKKKKKKKK.", "K5WW55444433221K",
    "K4433322221111K.", ".KKKKKKKKKKKKKK.", "................", "................",
    "................", "................", "................", "................"]

GREY = '#c4c4c4'
HEAD_CH, HANDLE_CH = set('12345W'), set('abc')


def split_layers(rows):
    """Split a tool template into head/handle/binding masks (sets of (x, y))."""
    head, handle = set(), set()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in HEAD_CH:
                head.add((x, y))
            elif ch in HANDLE_CH:
                handle.add((x, y))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != 'K':
                continue
            nb = [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]
            if any(n in head for n in nb):
                head.add((x, y))
            elif any(n in handle for n in nb):
                handle.add((x, y))
            else:
                head.add((x, y))
    binding = set()
    for p in list(handle):
        if any(max(abs(p[0] - h[0]), abs(p[1] - h[1])) <= 2 for h in head if h not in handle):
            binding.add(p)
    handle -= binding
    return head, handle, binding


def render_mask(rows, mask, pal):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for (x, y) in mask:
        ch = rows[y][x]
        if ch in pal:
            img.putpixel((x, y), pal[ch])
    return img


def recolor_handle_as_main(rows):
    """Binding pixels come from handle chars; re-render them with the main ramp so they tint well."""
    return rows


def centered(img):
    bbox = img.getbbox()
    if not bbox:
        return img
    crop = img.crop(bbox)
    out = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    out.paste(crop, ((16 - crop.width) // 2, (16 - crop.height) // 2))
    return out


def broken(img, seed):
    out = img.copy()
    px = out.load()
    for y in range(16):
        for x in range(16):
            if px[x, y][3] and (x + y * 2 + tex.hsh(seed, x, y) % 3) % 7 == 0:
                px[x, y] = (0, 0, 0, 0)
    return out


def cast_image(mask_img, seed):
    base = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    gold = tex.ramp('#f2c94c')
    d = ImageDraw.Draw(base)
    d.rectangle([1, 1, 14, 14], fill=gold['3'])
    d.rectangle([1, 1, 14, 1], fill=gold['5'])
    d.rectangle([1, 1, 1, 14], fill=gold['5'])
    d.rectangle([1, 14, 14, 14], fill=gold['1'])
    d.rectangle([14, 1, 14, 14], fill=gold['1'])
    m = centered(mask_img)
    for y in range(16):
        for x in range(16):
            if m.getpixel((x, y))[3] and 1 < x < 14 and 1 < y < 14:
                base.putpixel((x, y), gold['K'])
    return base


def build():
    m = Mod('ls_toolsmith', 'Toolsmith', 'Build tools from parts. Each part can be made of a different material, and every material brings '
                                          'its own stats and trait. Shape wood, stone, bone and gems into parts on the part builder, or melt '
                                          'metals in a foundry of scorched bricks and pour them over gold casts. Assemble pickaxes, axes, '
                                          'shovels, swords and kamas at the tool station; hammers, broad axes, excavators and scythes at the '
                                          'tool forge. Tools level up as you use them, gain modifier slots, and never break: they just stop '
                                          'working until repaired.', tab='TOOLS')
    m.init('ToolsmithSetup.init(bus)')
    m.client('com.lodestar.toolsmith.client.ToolsmithClient.init(bus)')
    rl = m.rl
    t = lambda n: '%s:block/%s' % (m.modid, n)

    # ----------------------------------------------------------------- generated tables
    mat_rows = []
    for mid, (color, tier, dur, speed, atk, handle, binding, trait, source) in MATERIALS.items():
        fluid = ('ls_toolsmith:molten_' + mid) if mid in MOLTEN else ''
        mat_rows.append('        new ToolMaterial("%s", 0x%s, %d, %d, %sf, %sf, %sf, %d, "%s", "%s", "%s")'
                        % (mid, color.lstrip('#').upper(), tier, dur, speed, atk, handle, binding, trait, source, fluid))
        m.tr('material.ls_toolsmith.' + mid, title(mid))
    m.java_file('MaterialTable.java', '\n'.join([
        '// GENERATED by tools/mods/toolsmith.py - do not edit', 'package com.lodestar.toolsmith;', '', 'import java.util.List;', '',
        'public final class MaterialTable {', '    private MaterialTable() {}', '',
        '    public static final List<ToolMaterial> ALL = List.of(', ',\n'.join(mat_rows), '    );', '}', '']))
    alloy_rows = []
    for name, (color, recipe, out) in ALLOYS.items():
        if not recipe:
            continue
        ins = ', '.join('"%s", %d' % (k, v) for k, v in recipe.items())
        alloy_rows.append('        {"%s", java.util.Map.of(%s), %d}' % (name, ins, out))
    m.java_file('ToolsmithTables.java', '\n'.join([
        '// GENERATED by tools/mods/toolsmith.py - do not edit', 'package com.lodestar.toolsmith;', '', 'import java.util.List;', '',
        'public final class ToolsmithTables {', '    private ToolsmithTables() {}', '',
        '    public static final List<String> MOLTEN_METALS = List.of(%s);' % ', '.join('"%s"' % x for x in MOLTEN), '',
        '    /** output metal, input metal -> units, output units (one unit = 1 mB per alloying step) */',
        '    public static final Object[][] ALLOYS = {', ',\n'.join(alloy_rows), '    };', '}', '']))

    # ----------------------------------------------------------------- molten fluids
    for metal in MOLTEN:
        color = MATERIALS[metal][0] if metal in MATERIALS else metal_color(metal)
        C = 'MOLTEN_' + const(metal)
        m.field('public static final LSFluid %s = LSFluid.molten(REG, "molten_%s", 0xFF%s, %d, LSTab.TOOLS);'
                % (C, metal, color.lstrip('#').upper(), TEMPS.get(metal, 1500)))
        nice = 'Molten ' + title(metal)
        m.tr('fluid_type.ls_toolsmith.molten_' + metal, nice)
        m.tr('block.ls_toolsmith.molten_' + metal, nice)
        m.asset('blockstates/molten_%s.json' % metal, {'variants': {'': {'model': t('molten_' + metal)}}})
        m.asset('models/block/molten_%s.json' % metal, {'textures': {'particle': 'minecraft:block/lava_still'}})
        bn = 'molten_%s_bucket' % metal
        m.asset('models/item/%s.json' % bn, {'parent': 'minecraft:item/generated', 'textures': {'layer0': '%s:item/%s' % (m.modid, bn)}})
        m.texture('item/' + bn, tex.item('bucket', '#9aa0a8', None, color))
        m.tr('item.ls_toolsmith.' + bn, nice + ' Bucket')
        m.tag('fluids', 'forge:molten_' + metal, rl('molten_' + metal), rl('flowing_molten_' + metal))

    # ----------------------------------------------------------------- tool and part textures
    grey = tex.palette(GREY, GREY)
    masks = {}
    for tid, (tmpl, head_p, handle_p, bind_p, nice) in TOOLS.items():
        rows = SCYTHE if tmpl == 'scythe' else tex.T[tmpl]
        rows = tex.fix_template(rows) if tmpl == 'scythe' else rows
        head, handle, binding = split_layers(rows)
        h_img, hd_img, b_img = render_mask(rows, head, grey), render_mask(rows, handle, grey), render_mask(rows, binding, grey)
        m.texture('item/tool/%s_head' % tid, h_img)
        m.texture('item/tool/%s_head_broken' % tid, broken(h_img, tid))
        m.texture('item/tool/%s_handle' % tid, hd_img)
        m.texture('item/tool/%s_binding' % tid, b_img)
        masks.setdefault(head_p, h_img)
        masks.setdefault(handle_p, hd_img)
        layers = lambda hd: {'layer0': '%s:item/tool/%s_handle' % (m.modid, tid), 'layer1': '%s:item/tool/%s_binding' % (m.modid, tid),
                             'layer2': '%s:item/tool/%s_%s' % (m.modid, tid, hd)}
        m.asset('models/item/%s_broken.json' % tid, {'parent': 'minecraft:item/handheld', 'textures': layers('head_broken')})
        m.item(tid, None, java='new ModularToolItem(new Item.Properties(), ToolType.%s)' % tid.upper(), jtype='ModularToolItem', tab='NONE',
               model={'parent': 'minecraft:item/handheld', 'textures': layers('head'),
                      'overrides': [{'predicate': {'ls_toolsmith:broken': 1}, 'model': '%s:item/%s_broken' % (m.modid, tid)}]})
        m.tr('tool.ls_toolsmith.' + tid, nice)
    masks['tool_binding'] = tex.from_template(BINDING, grey)
    masks['tough_binding'] = tex.from_template(TOUGH_BINDING, grey)
    masks['cross_guard'] = tex.from_template(GUARD, grey)
    for pid, nice in PARTS.items():
        img = centered(masks[pid]) if pid not in ('tool_binding', 'tough_binding', 'cross_guard') else masks[pid]
        m.texture('item/part/' + pid, img)
        m.item(pid, None, java='new PartItem(new Item.Properties(), PartType.%s)' % pid.upper(), jtype='PartItem', tab='NONE',
               model={'parent': 'minecraft:item/generated', 'textures': {'layer0': '%s:item/part/%s' % (m.modid, pid)}})
        m.tr('part.ls_toolsmith.' + pid, nice)
    # casts
    shapes = {'ingot': (tex.item('ingot', GREY), 144), 'nugget': (tex.item('nugget', GREY), 16), 'plate': (tex.item('plate', GREY), 144),
              'gear': (tex.item('gear', GREY), 576), 'rod': (tex.item('rod', GREY), 72)}
    for pid in PARTS:
        shapes[pid] = (masks[pid], PART_COST[pid] * 144)
    for shape, (mask, amount) in shapes.items():
        n = shape + '_cast'
        m.item(n, cast_image(mask, n), java='new CastItem(new Item.Properties(), "%s", %d)' % (shape, amount), jtype='CastItem',
               lang='%s Cast' % (PARTS.get(shape) or title(shape)), tags=['ls_toolsmith:casts'])

    # ----------------------------------------------------------------- misc items
    m.item('blank_pattern', ('card', '#c9a36b'), desc='Needed in the part builder (not used up)')
    m.item('silky_cloth', ('sheet', '#f4f0e6', None, '#f2c94c'), desc='Tool modifier: Silk Touch')
    m.item('expander', ('upgrade', '#7f8c8d'), desc='Tool modifier: large tools work a 5x5 area')
    m.item('grout', ('clay_lump', '#9a9088'), tab='MATERIALS', desc='Fire it into scorched bricks')
    m.item('scorched_brick', ('brick', '#4a3b33'), tab='MATERIALS')
    m.shaped('blank_pattern', rl('blank_pattern'), ['PS', 'SP'], {'P': '#minecraft:planks', 'S': '#forge:rods/wooden'}, 4)
    m.shaped('silky_cloth', rl('silky_cloth'), ['SSS', 'SGS', 'SSS'], {'S': 'minecraft:string', 'G': '#forge:ingots/gold'})
    m.shaped('expander', rl('expander'), ['PIP', 'I I', 'PIP'], {'P': 'minecraft:piston', 'I': '#forge:storage_blocks/iron'})
    m.shapeless('grout', rl('grout'), ['minecraft:clay_ball', '#minecraft:sand', 'minecraft:gravel'], 2)
    m.smelt('scorched_brick', rl('grout'), rl('scorched_brick'), 0.3, blast=False)

    # ----------------------------------------------------------------- foundry blocks
    brick_img = tex.bricks('#4a3b33', m.modid + 'sb', '#2a221e')
    m.block('scorched_bricks', props='LSProps.stone(3.0f, 12.0f)', textures={'block/scorched_bricks': brick_img}, tab='TOOLS',
            tags=['ls_toolsmith:foundry_walls', 'ls_toolsmith:foundry_floor'])
    m.shaped('scorched_bricks', rl('scorched_bricks'), ['##', '##'], {'#': rl('scorched_brick')})
    glass_img = tex.glass('#4a3b33', m.modid + 'sg')
    m.block('scorched_glass', java='new GlassBlock(LSProps.glass())', jtype='GlassBlock', tab='TOOLS',
            models={'scorched_glass': {'parent': 'minecraft:block/cube_all', 'render_type': 'minecraft:cutout', 'textures': {'all': t('scorched_glass')}}},
            textures={'block/scorched_glass': glass_img}, tags=['ls_toolsmith:foundry_walls'])
    m.shaped('scorched_glass', rl('scorched_glass'), [' B ', 'BGB', ' B '], {'B': rl('scorched_brick'), 'G': '#forge:glass'})

    def front(lit):
        img = brick_img.copy()
        d = ImageDraw.Draw(img)
        d.rectangle([3, 4, 12, 12], fill=(24, 18, 16, 255))
        for x in range(4, 12, 2):
            d.line([(x, 5), (x, 11)], fill=(70, 60, 54, 255))
        if lit:
            for x in range(4, 12):
                for y in range(9, 12):
                    if x % 2:
                        img.putpixel((x, y), (255, 120 + (x * 13) % 80, 30, 255))
        return img
    mk = lambda fr: {'parent': 'minecraft:block/orientable', 'textures': {'top': t('scorched_bricks'), 'side': t('scorched_bricks'), 'front': t(fr)}}
    entity_block(m, 'foundry_controller', 'com.lodestar.toolsmith.foundry.FoundryControllerBE',
                 'new LSEntityBlock(LSProps.stone(3.0f, 12.0f).lightLevel(s -> s.getValue(LSEntityBlock.LIT) ? 12 : 0), BE_TYPE, LSEntityBlock.Facing.HORIZONTAL, true)',
                 tab='TOOLS', models={'foundry_controller': mk('foundry_front'), 'foundry_controller_on': mk('foundry_front_on')},
                 state=horizontal_model_state(m, 'foundry_controller', 'foundry_controller_on'),
                 textures={'block/foundry_front': front(False), 'block/foundry_front_on': front(True)},
                 desc='The heart of a foundry. Build a room of scorched bricks\nwith this in a wall, a foundry tank of lava and a drain.')
    m.tag('blocks', 'ls_toolsmith:foundry_walls', rl('foundry_controller'))
    m.shaped('foundry_controller', rl('foundry_controller'), ['BBB', 'B B', 'BBB'], {'B': rl('scorched_brick')})

    drain_img = brick_img.copy()
    dd = ImageDraw.Draw(drain_img)
    dd.ellipse([4, 4, 11, 11], fill=(20, 16, 14, 255), outline=(90, 80, 70, 255))
    entity_block(m, 'foundry_drain', 'com.lodestar.toolsmith.foundry.FoundryDrainBE', 'new LSEntityBlock(LSProps.stone(3.0f, 12.0f), BE_TYPE).noMenu().noBuckets()',
                 tab='TOOLS', models={'foundry_drain': {'parent': 'minecraft:block/cube_all', 'textures': {'all': t('foundry_drain')}}},
                 textures={'block/foundry_drain': drain_img}, desc='Part of the foundry wall that faucets pour from')
    m.tag('blocks', 'ls_toolsmith:foundry_walls', rl('foundry_drain'))
    m.shaped('foundry_drain', rl('foundry_drain'), ['B B', 'B B', 'B B'], {'B': rl('scorched_brick')})

    tank_side = tex.glass('#4a3b33', m.modid + 'tank', None, 'framed')
    entity_block(m, 'foundry_tank', 'com.lodestar.toolsmith.foundry.FoundryTankBE', 'new LSEntityBlock(LSProps.stone(3.0f, 12.0f).noOcclusion(), BE_TYPE).noMenu()',
                 tab='TOOLS', models={'foundry_tank': {'parent': 'minecraft:block/cube_all', 'render_type': 'minecraft:cutout', 'textures': {'all': t('foundry_tank')}}},
                 textures={'block/foundry_tank': tank_side}, desc='Holds four buckets. In a foundry wall, its lava fuels the melting.')
    m.tag('blocks', 'ls_toolsmith:foundry_walls', rl('foundry_tank'))
    m.shaped('foundry_tank', rl('foundry_tank'), ['BBB', 'BGB', 'BBB'], {'B': rl('scorched_brick'), 'G': '#forge:glass'})

    faucet_model = element_model([([4, 4, 10], [12, 10, 16], {'north': {'texture': '#b'}, 'south': {'texture': '#b'}, 'east': {'texture': '#b'},
                                                              'west': {'texture': '#b'}, 'up': {'texture': '#b'}, 'down': {'texture': '#b'}})],
                                 {'b': t('scorched_bricks'), 'particle': t('scorched_bricks')})
    faucet_model['elements'].append({'from': [5, 6, 11], 'to': [11, 10, 16], 'faces': {'up': {'texture': '#dark'}}})
    faucet_model['textures']['dark'] = 'minecraft:block/black_concrete'
    entity_block(m, 'faucet', 'com.lodestar.toolsmith.foundry.FaucetBE', 'new com.lodestar.toolsmith.foundry.FaucetBlock(LSProps.stone(2.0f).noOcclusion(), BE_TYPE)', jtype='com.lodestar.toolsmith.foundry.FaucetBlock',
                 tab='TOOLS', models={'faucet': faucet_model}, state=horizontal_model_state(m, 'faucet'), item_model='block',
                 desc='Place on a drain or tank. Use it or pulse it with redstone\nto pour into the casting table or basin below.')
    m.shaped('faucet', rl('faucet'), ['B B', ' B '], {'B': rl('scorched_brick')})

    table_model = element_model([
        ([0, 12, 0], [16, 16, 16], {'north': {'texture': '#b'}, 'south': {'texture': '#b'}, 'east': {'texture': '#b'}, 'west': {'texture': '#b'},
                                    'up': {'texture': '#top'}, 'down': {'texture': '#b'}}),
        ([0, 0, 0], [4, 12, 4], '#b'), ([12, 0, 0], [16, 12, 4], '#b'), ([0, 0, 12], [4, 12, 16], '#b'), ([12, 0, 12], [16, 12, 16], '#b')],
        {'b': t('scorched_bricks'), 'top': t('casting_top'), 'particle': t('scorched_bricks')})
    top_img = brick_img.copy()
    ImageDraw.Draw(top_img).rectangle([2, 2, 13, 13], fill=(36, 28, 24, 255))
    entity_block(m, 'casting_table', 'com.lodestar.toolsmith.foundry.CastingBE', 'new LSEntityBlock(LSProps.stone(2.0f).noOcclusion(), BE_TYPE).noMenu().noBuckets()',
                 tab='TOOLS', models={'casting_table': table_model}, textures={'block/casting_top': top_img}, ctor_args='false',
                 desc='Put a cast on it and pour molten metal from a faucet')
    m.shaped('casting_table', rl('casting_table'), ['BBB', 'B B', 'B B'], {'B': rl('scorched_brick')})
    basin_model = element_model([
        ([0, 0, 0], [16, 4, 16], '#b'), ([0, 4, 0], [16, 16, 2], '#b'), ([0, 4, 14], [16, 16, 16], '#b'),
        ([0, 4, 2], [2, 16, 14], '#b'), ([14, 4, 2], [16, 16, 14], '#b')], {'b': t('scorched_bricks'), 'particle': t('scorched_bricks')})
    entity_block(m, 'casting_basin', 'com.lodestar.toolsmith.foundry.CastingBE', 'new LSEntityBlock(LSProps.stone(2.0f).noOcclusion(), BE_TYPE).noMenu().noBuckets()',
                 tab='TOOLS', models={'casting_basin': basin_model}, ctor_args='true', desc='Pour in a block\'s worth of molten metal to cast a block')
    m.shaped('casting_basin', rl('casting_basin'), ['B B', 'B B', 'BBB'], {'B': rl('scorched_brick')})

    # ----------------------------------------------------------------- stations
    def station_model(top_tex, side_tex):
        return element_model([([0, 12, 0], [16, 16, 16], {'north': {'texture': '#side'}, 'south': {'texture': '#side'}, 'east': {'texture': '#side'},
                                                          'west': {'texture': '#side'}, 'up': {'texture': '#top'}, 'down': {'texture': '#side'}}),
                              ([1, 0, 1], [4, 12, 4], '#leg'), ([12, 0, 1], [15, 12, 4], '#leg'), ([1, 0, 12], [4, 12, 15], '#leg'),
                              ([12, 0, 12], [15, 12, 15], '#leg')], {'top': top_tex, 'side': side_tex, 'leg': side_tex, 'particle': side_tex})

    def station_top(seed, accent):
        img = tex.planks('#a0784a', seed)
        d = ImageDraw.Draw(img)
        d.rectangle([3, 3, 12, 12], fill=tex.rgb(accent) + (255,), outline=(60, 40, 24, 255))
        return img
    stations = {'part_builder': ('PART_BUILDER', '#d8c8a0', 'Shape materials into tool parts.\nNeeds a blank pattern.'),
                'tool_station': ('TOOL_STATION', '#9aa3ad', 'Assemble tools from parts, repair them\nand add modifiers'),
                'tool_forge': ('TOOL_FORGE', '#5c6069', 'Like the tool station, but also\nbuilds hammers, broad axes, excavators and scythes')}
    for sid, (kind, accent, desc) in stations.items():
        side = t('scorched_bricks') if sid == 'tool_forge' else 'minecraft:block/oak_planks'
        entity_block(m, sid, 'com.lodestar.toolsmith.station.StationBE', 'new LSEntityBlock(LSProps.wood().noOcclusion(), BE_TYPE)', tab='TOOLS',
                     models={sid: station_model(t(sid + '_top'), side)}, textures={'block/%s_top' % sid: station_top(m.modid + sid, accent)},
                     ctor_args='com.lodestar.toolsmith.station.StationBE.Kind.%s' % kind, desc=desc, mine='axe' if sid != 'tool_forge' else 'pickaxe')
    m.shaped('part_builder', rl('part_builder'), ['P', 'W'], {'P': rl('blank_pattern'), 'W': '#minecraft:logs'})
    m.shaped('tool_station', rl('tool_station'), ['P', 'C'], {'P': rl('blank_pattern'), 'C': 'minecraft:crafting_table'})
    m.shaped('tool_forge', rl('tool_forge'), ['BBB', 'MTM', 'M M'], {'B': rl('scorched_bricks'), 'M': '#forge:storage_blocks/iron', 'T': rl('tool_station')})

    m.data('loot_modifiers/tool_traits.json', {'type': 'ls_toolsmith:tool_traits', 'conditions': []})
    m.loot_mods.append(rl('tool_traits'))

    # ----------------------------------------------------------------- lang
    for k, (name, desc) in TRAITS.items():
        m.tr('trait.ls_toolsmith.' + k, name)
        m.tr('trait.ls_toolsmith.%s.desc' % k, desc)
    for k, (name, desc) in MODIFIERS.items():
        m.tr('modifier.ls_toolsmith.' + k, name)
        m.tr('modifier.ls_toolsmith.%s.desc' % k, desc)
    for i, n in enumerate(['Wood', 'Stone', 'Iron', 'Diamond', 'Netherite']):
        m.tr('tier.ls_toolsmith.%d' % i, n)
    for k, v in {
        'tool.ls_toolsmith.named': '%s %s', 'part.ls_toolsmith.named': '%s %s',
        'tooltip.ls_toolsmith.broken': 'Broken! Repair it in a tool station', 'tooltip.ls_toolsmith.level': 'Level %s (%s XP)',
        'tooltip.ls_toolsmith.durability': 'Durability: %s / %s', 'tooltip.ls_toolsmith.speed': 'Mining speed: %s',
        'tooltip.ls_toolsmith.tier': 'Mines like: %s', 'tooltip.ls_toolsmith.slots': 'Modifier slots free: %s / %s',
        'tooltip.ls_toolsmith.shift': 'Hold Shift for trait details', 'tooltip.ls_toolsmith.part.durability': 'Durability: %s',
        'tooltip.ls_toolsmith.part.attack': 'Attack: +%s', 'tooltip.ls_toolsmith.part.handle': 'Durability x%s',
        'tooltip.ls_toolsmith.part.binding': 'Durability +%s', 'tooltip.ls_toolsmith.part.cost': 'Costs %s material',
        'tooltip.ls_toolsmith.cast': 'Holds %s mB of molten metal. Never used up.',
        'msg.ls_toolsmith.broke': '%s is broken! Repair it in a tool station.', 'msg.ls_toolsmith.level_up': '%s reached level %s and gained a modifier slot!',
        'msg.ls_toolsmith.not_tool': 'Not a tool', 'msg.ls_toolsmith.large_only': 'Only for large tools', 'msg.ls_toolsmith.max_level': 'Already at max level',
        'msg.ls_toolsmith.excludes': 'Can\'t combine with %s', 'msg.ls_toolsmith.requires': 'Needs %s first', 'msg.ls_toolsmith.no_slots': 'No free modifier slots',
        'msg.ls_toolsmith.needs_forge': 'Large tools need a tool forge', 'msg.ls_toolsmith.not_usable': 'Not a repair material or modifier',
        'gui.ls_toolsmith.cost': 'Cost: %s', 'gui.ls_toolsmith.needs_pattern': 'Insert a blank pattern',
        'gui.ls_toolsmith.foundry.incomplete': 'Structure incomplete', 'gui.ls_toolsmith.foundry.select': 'Click to pour this first',
        'gui.ls_toolsmith.foundry.selected': 'Poured first', 'gui.ls_toolsmith.foundry.capacity': '%s / %s mB',
        'lens.category.ls_toolsmith.melting': 'Foundry Melting', 'lens.category.ls_toolsmith.alloying': 'Foundry Alloying',
        'lens.category.ls_toolsmith.casting_table': 'Casting Table', 'lens.category.ls_toolsmith.casting_basin': 'Casting Basin',
        'lens.category.ls_toolsmith.part_building': 'Part Builder', 'lens.category.ls_toolsmith.tool_building': 'Tool Station',
        'lens.category.ls_toolsmith.modifiers': 'Tool Modifiers', 'lens.ls_toolsmith.forge_only': 'Tool forge only',
    }.items():
        m.tr(k, v)

    m.codex_intro('Toolsmith tools are built from parts, and each part can be a different material. The head decides how fast the '
                  'tool mines, what it can mine and how hard it hits; the handle multiplies durability and the binding adds to it. '
                  'Every material also brings a trait.')
    m.codex_page('Parts', 'Make a blank pattern from planks and sticks and put it in the part builder (it is not used up). Add a '
                          'material such as planks, cobblestone, flint, bone, obsidian or diamonds, pick a part shape and take the part. '
                          'Metal parts can\'t be shaped by hand: they are cast in the foundry.', items=[rl('part_builder'), rl('blank_pattern')])
    m.codex_page('Tools', 'Put three parts into the tool station to assemble a tool: a head, a handle and a binding (or a cross guard for '
                          'swords). The large tools - hammer, broad axe, excavator and scythe - need the tool forge. Hammers and excavators '
                          'dig 3x3, broad axes fell whole trees and scythes reap crops and sweep enemies.',
                 items=[rl('tool_station'), rl('tool_forge')])
    m.codex_page('Repairs and modifiers', 'Tools never break. At zero durability they stop working until you repair them: put the tool in '
                                          'the middle of the tool station with its head material around it. The same station adds '
                                          'modifiers such as Haste (block of redstone), Luck (block of lapis) or Silky (silky cloth). '
                                          'Each modifier level takes a slot; tools have three and gain one per tool level.',
                 items=[rl('silky_cloth'), rl('expander')])
    m.codex_page('The foundry', 'Fire grout into scorched bricks. Build a floor of scorched bricks, then walls around a hollow room of any '
                                'size up to 9x9 and ten high, with the foundry controller in a wall, at least one foundry tank filled with lava '
                                'and a drain. Ores, ingots, dusts and other metal items put inside melt into a pool; ores melt into twice the '
                                'metal. Molten copper and tin mix into bronze by themselves, and the same goes for the other alloys.',
                 items=[rl('foundry_controller'), rl('scorched_bricks'), rl('foundry_tank'), rl('foundry_drain'), rl('scorched_glass')])
    m.codex_page('Casting', 'Put a faucet on the drain and a casting table below it. Make casts by putting an ingot or a part on the table '
                            'and pouring molten gold over it. With a cast on the table, pouring molten metal makes that shape: ingots, '
                            'nuggets, plates, gears, rods, or tool parts in any metal. A casting basin below a faucet makes blocks.',
                 items=[rl('faucet'), rl('casting_table'), rl('casting_basin'), rl('ingot_cast'), rl('pick_head_cast')])
    return m
