from gen.fw import Mod, connector_block, pipe_tex, pipe_end, const

PIPES = [
    # name, rate (items per op, 0 = transport only), interval ticks, body colour, band colour
    ('item_pipe', 0, 0, '#9aa0a8', '#5c5f66'),
    ('extraction_pipe', 8, 20, '#9aa0a8', '#c0392b'),
    ('fast_extraction_pipe', 32, 10, '#7b8591', '#f2c94c'),
    ('elite_extraction_pipe', 64, 4, '#3d3f48', '#9b5de5'),
]


def build():
    m = Mod('fw_pipes', 'Item Pipes', 'Instant item logistics: extraction pipes pull from inventories and deliver to the nearest '
                                      'inventory on the network. Right-click an extraction pipe with an item to filter it; redstone pauses it.', tab='LOGISTICS')
    rl = m.rl
    for name, rate, interval, body, band in PIPES:
        C = const(name)
        connector_block(m, name, 'new ItemPipeBlock(FWProps.metal(0.8f).noOcclusion(), %d, %d, () -> Content.%s_BE.get())' % (rate, max(1, interval), C),
                        'ItemPipeBlock', 3, pipe_tex(body, band, name), pipe_end(body, band, name))
        m.field('public static final RegistryObject<BlockEntityType<ItemPipeBE>> %s_BE = REG.blockEntity("%s", (pos, state) -> new ItemPipeBE(Content.%s_BE.get(), pos, state), Content.%s);'
                % (C, name, C, C))
    m.shaped('item_pipe', rl('item_pipe'), ['PGP'], {'P': '#forge:plates/iron', 'G': '#forge:glass'}, 8)
    m.shaped('extraction_pipe', rl('extraction_pipe'), [' R ', 'HPH'], {'R': '#forge:dusts/redstone', 'H': 'minecraft:hopper', 'P': rl('item_pipe')}, 2)
    m.shaped('fast_extraction_pipe', rl('fast_extraction_pipe'), ['GEG'], {'G': '#forge:ingots/gold', 'E': rl('extraction_pipe')}, 1)
    m.shaped('elite_extraction_pipe', rl('elite_extraction_pipe'), ['CEC'], {'C': 'fw_circuits:advanced_circuit', 'E': rl('fast_extraction_pipe')}, 1)
    for k, v in {'tooltip.fw_pipes.extract': 'Pulls %s items every %ss from adjacent inventories',
                 'tooltip.fw_pipes.filter': 'Right-click with an item to filter. Sneak + empty hand clears. Redstone pauses.',
                 'tooltip.fw_pipes.transport': 'Carries items between extraction pipes and inventories',
                 'msg.fw_pipes.filter_set': 'Filter: %s', 'msg.fw_pipes.filter_cleared': 'Filter cleared', 'msg.fw_pipes.no_filter': 'No filter'}.items():
        m.tr(k, v)
    return m
