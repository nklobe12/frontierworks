from gen.fw import Mod, title
from gen import tex
from mods._gear import GEAR

TOOLS = {
    'sword': ('SwordItem', '%s, 3, -2.4f', ['M', 'M', 'S'], 'forge:tools/swords', 'minecraft:swords'),
    'pickaxe': ('PickaxeItem', '%s, 1, -2.8f', ['MMM', ' S ', ' S '], 'forge:tools/pickaxes', 'minecraft:pickaxes'),
    'axe': ('AxeItem', '%s, 5.5f, -3.05f', ['MM', 'MS', ' S'], 'forge:tools/axes', 'minecraft:axes'),
    'shovel': ('ShovelItem', '%s, 1.5f, -3.0f', ['M', 'S', 'S'], 'forge:tools/shovels', 'minecraft:shovels'),
    'hoe': ('HoeItem', '%s, -2, -1.0f', ['MM', ' S', ' S'], 'forge:tools/hoes', 'minecraft:hoes'),
}


def build():
    m = Mod('fw_tools', 'Toolsmith', 'Full tool sets in thirteen materials, from early bronze to space-age titanium: swords, '
                                     'pickaxes, axes, shovels and hoes.', tab='TOOLS')
    rl = m.rl
    for mat, (color, repair, tier, handle) in GEAR.items():
        for kind, (cls, args, pattern, ftag, mtag) in TOOLS.items():
            name = '%s_%s' % (mat, kind)
            m.item(name, (kind, color, handle), java='new %s(%s, new Item.Properties())' % (cls, args % ('FWTiers.' + tier)),
                   jtype=cls, model='handheld', tags=[ftag, mtag])
            m.shaped(name, rl(name), pattern, {'M': repair, 'S': '#forge:rods/wooden'})
    return m
