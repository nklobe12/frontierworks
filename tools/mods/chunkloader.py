from gen.fw import Mod
from gen import tex


def build():
    m = Mod('fw_chunkloader', 'Chunk Loaders', 'Keep your machines running while you explore: chunk loaders keep a 3x3 or 5x5 '
                                               'area of chunks loaded and ticking.', tab='LOGISTICS')
    rl = m.rl
    m.init('ChunkLoaderBlock.init()')
    for name, r, tier in (('chunk_loader', 1, 'advanced'), ('advanced_chunk_loader', 2, 'elite')):
        m.block(name, java='new ChunkLoaderBlock(FWProps.metal(5.0f).lightLevel(s -> 7), %d)' % r, jtype='ChunkLoaderBlock', tab='LOGISTICS',
                models={name: {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
                    'top': '%s:block/%s_top' % (m.modid, name), 'bottom': '%s:block/%s_top' % (m.modid, name), 'side': '%s:block/%s_side' % (m.modid, name)}}},
                textures={'block/%s_top' % name: tex.machine_front(tier, 'eye', True, name), 'block/%s_side' % name: tex.machine_front(tier, 'grid', True, name + 's')},
                level='iron')
    m.shaped('chunk_loader', rl('chunk_loader'), ['PEP', 'EFE', 'PCP'], {'P': '#forge:plates/steel', 'E': '#forge:ender_pearls', 'F': 'fw_parts:advanced_frame',
                                                                        'C': 'fw_circuits:advanced_circuit'})
    m.shaped('advanced_chunk_loader', rl('advanced_chunk_loader'), ['PVP', 'VLV', 'PCP'], {'P': '#forge:plates/platinum', 'V': 'fw_crystals:voidite_shard',
                                                                                          'L': rl('chunk_loader'), 'C': 'fw_circuits:elite_circuit'})
    m.tr('tooltip.fw_chunkloader.area', 'Keeps %sx%s chunks loaded')
    return m
