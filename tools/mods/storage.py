from gen.fw import Mod, custom_model_machine, horizontal_state, const
from gen import tex

CRATES = [('wooden_crate', 3, '#9a7048', 'axe'), ('iron_crate', 4, '#9aa0a8', 'pickaxe'), ('steel_crate', 6, '#5f6670', 'pickaxe')]
BARRELS = [('bulk_barrel', 64, '#8a6236'), ('reinforced_bulk_barrel', 256, '#7b8591'), ('elite_bulk_barrel', 1024, '#3d3f48')]


def crate_tex(color, seed):
    img = tex.planks(color, seed) if color == '#9a7048' else tex.metal_block(color, seed, 'plated')
    px = img.load()
    c = tex.rgb(color)
    for i in range(16):
        px[i, i] = tex.shade(c, 0.65)
        px[15 - i, i] = tex.shade(c, 0.65)
    return img


def barrel_front(color, seed):
    img = tex.planks(color, seed) if color == '#8a6236' else tex.metal_block(color, seed, 'plain')
    px = img.load()
    for y in range(3, 13):
        for x in range(3, 13):
            px[x, y] = (40, 34, 30, 255)
    return img


def build():
    m = Mod('fw_storage', 'Storage', 'Crates that keep their contents when picked up, and bulk barrels that hold up to 1,024 '
                                     'stacks of a single item and show it on their face.', tab='LOGISTICS')
    rl = m.rl
    for name, rows, color, mine in CRATES:
        side = crate_tex(color, name)
        custom_model_machine(m, name, 'new CrateBE(BE_TYPE, pos, state, %d)' % rows, 'CrateBE', {'block/' + name: side}, top=name, side=name,
                             tab='LOGISTICS', props='FWProps.wood()' if mine == 'axe' else 'FWProps.metal(3.0f)',
                             desc='%d slots. Keeps its contents when broken.' % (rows * 9), level=None)
        m.tag('blocks', 'minecraft:mineable/' + mine, rl(name))
        m.data('loot_tables/blocks/%s.json' % name, {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{
            'type': 'minecraft:item', 'name': rl(name), 'functions': [{'function': 'minecraft:copy_nbt', 'source': 'block_entity',
                                                                       'ops': [{'source': 'Items', 'target': 'BlockEntityTag.Items', 'op': 'replace'}]}]}]}]})
    m.shaped('wooden_crate', rl('wooden_crate'), ['PSP', 'S S', 'PSP'], {'P': '#minecraft:planks', 'S': '#forge:rods/wooden'})
    m.shaped('iron_crate', rl('iron_crate'), ['III', 'ICI', 'III'], {'I': '#forge:ingots/iron', 'C': rl('wooden_crate')})
    m.shaped('steel_crate', rl('steel_crate'), ['SSS', 'SCS', 'SSS'], {'S': '#forge:ingots/steel', 'C': rl('iron_crate')})

    for name, stacks, color in BARRELS:
        C = const(name)
        t = lambda n: '%s:block/%s' % (m.modid, n)
        side = tex.planks(color, name + 's') if color == '#8a6236' else tex.metal_block(color, name + 's', 'riveted')
        model = {'parent': 'minecraft:block/orientable', 'textures': {'top': t(name + '_side'), 'side': t(name + '_side'), 'front': t(name + '_front')}}
        m.block(name, java='new BarrelBlock(%s, %d, () -> Content.%s_BE.get())' % ('FWProps.wood()' if stacks == 64 else 'FWProps.metal(3.0f)', stacks, C),
                jtype='BarrelBlock', tab='LOGISTICS', state=horizontal_state(m, name, name, lit_prop=False), models={name: model},
                textures={'block/%s_side' % name: side, 'block/%s_front' % name: barrel_front(color, name)}, mine='axe' if stacks == 64 else 'pickaxe',
                level=None, desc='Holds %d stacks of one item. Right-click insert, left-click take, sneak-right-click to lock.' % stacks,
                loot={'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{
                    'type': 'minecraft:item', 'name': rl(name), 'functions': [{'function': 'minecraft:copy_nbt', 'source': 'block_entity', 'ops': [
                        {'source': 'Type', 'target': 'BlockEntityTag.Type', 'op': 'replace'}, {'source': 'Count', 'target': 'BlockEntityTag.Count', 'op': 'replace'},
                        {'source': 'Locked', 'target': 'BlockEntityTag.Locked', 'op': 'replace'}]}]}]}]})
        m.field('public static final RegistryObject<BlockEntityType<BarrelBE>> %s_BE = REG.blockEntity("%s", (pos, state) -> new BarrelBE(Content.%s_BE.get(), pos, state, %d), Content.%s);'
                % (C, name, C, stacks, C))
    m.shaped('bulk_barrel', rl('bulk_barrel'), ['PSP', 'PCP', 'PSP'], {'P': '#minecraft:planks', 'S': '#minecraft:wooden_slabs', 'C': 'minecraft:chest'})
    m.shaped('reinforced_bulk_barrel', rl('reinforced_bulk_barrel'), ['PIP', 'IBI', 'PIP'], {'P': '#forge:plates/iron', 'I': '#forge:ingots/iron', 'B': rl('bulk_barrel')})
    m.shaped('elite_bulk_barrel', rl('elite_bulk_barrel'), ['PDP', 'DBD', 'PDP'], {'P': '#forge:plates/steel', 'D': '#forge:gems/diamond', 'B': rl('reinforced_bulk_barrel')})
    m.client('com.frontierworks.storage.client.StorageClient.init(bus)')
    for k, v in {'msg.fw_storage.locked': 'Item type locked', 'msg.fw_storage.unlocked': 'Item type unlocked',
                 'tooltip.fw_storage.barrel': 'Capacity: %s stacks', 'tooltip.fw_storage.contains': 'Contains %s items'}.items():
        m.tr(k, v)
    return m
