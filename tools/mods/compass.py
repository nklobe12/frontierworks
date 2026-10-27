from gen.fw import Mod
from gen import tex


def build():
    m = Mod('fw_compass', "Explorer's Compass", "The Explorer's Compass finds every Frontierworks structure: sneak-right-click to "
                                                'choose a target (ruins, dungeons, towers, craters...), right-click to locate it.', tab='TOOLS')
    rl = m.rl
    m.client('com.frontierworks.compass.client.CompassClient.init(bus)')
    name = 'explorers_compass'
    n = 32
    overrides = []
    for i in range(n):
        fn = '%s_%02d' % (name, i)
        m.texture('item/' + fn, tex.compass_frame(i, n))
        m.asset('models/item/%s.json' % fn, {'parent': 'minecraft:item/generated', 'textures': {'layer0': '%s:item/%s' % (m.modid, fn)}})
        overrides.append({'predicate': {'angle': (i - 0.5) / n if i > 0 else 0.0}, 'model': '%s:item/%s' % (m.modid, fn)})
    overrides.append({'predicate': {'angle': (n - 0.5) / n}, 'model': '%s:item/%s_00' % (m.modid, name)})
    m.item(name, None, java='new ExplorersCompassItem(new Item.Properties())', jtype='ExplorersCompassItem', lang="Explorer's Compass",
           model={'parent': 'minecraft:item/generated', 'textures': {'layer0': '%s:item/%s_16' % (m.modid, name)}, 'overrides': overrides})
    m.shaped(name, rl(name), [' E ', 'GCG', ' R '], {'E': '#forge:ender_pearls', 'G': '#forge:ingots/gold', 'C': 'minecraft:compass', 'R': 'minecraft:map'})
    for k, v in {'tooltip.fw_compass.use': 'Sneak + right-click: change target. Right-click: locate.',
                 'msg.fw_compass.target': 'Target: %s', 'msg.fw_compass.none': 'No %s found nearby',
                 'msg.fw_compass.found': '%s found %s blocks away'}.items():
        m.tr(k, v)
    return m
