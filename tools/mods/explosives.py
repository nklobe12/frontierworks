"""Demolitions: dynamite, mining charges, grenades and remote-detonated C4."""
from gen.fw import Mod, const
from gen import tex

M = 'fw_explosives'

# item: (kind enum, template, colour, accent, desc, stack)
THROWN = [
    ('dynamite', 'DYNAMITE', 'dynamite', '#c0392b', '#ffb030', 'Bounces, then blows. Breaks blocks.'),
    ('bundled_dynamite', 'BUNDLE', 'dynamite', '#8e2a20', '#ffb030', 'Three sticks, one very big bang'),
    ('mining_charge', 'MINING', 'canister', '#6b7a3a', '#ffd040', 'Shatters a 5x5x5 area of stone and ore into drops. Harmless to creatures.'),
    ('frag_grenade', 'FRAG', 'ball', '#4a5a3a', '#2a2a2a', 'Damages creatures without breaking blocks'),
    ('incendiary_grenade', 'FIRE', 'ball', '#b04020', '#ffb030', 'Bursts into flames'),
    ('smoke_grenade', 'SMOKE', 'ball', '#8a8a8a', '#e0e0e0', 'Blinds and slows everything in a thick cloud'),
]


def build():
    m = Mod(M, 'Demolitions', 'Things that go boom: bouncing dynamite and bundles, mining charges that turn rock into drops, frag, '
                              'incendiary and smoke grenades, and C4 you can detonate remotely.', tab='TOOLS')
    rl = m.rl
    m.client('com.frontierworks.explosives.client.ExplosivesClient.init(bus)')
    m.field('public static final RegistryObject<EntityType<com.frontierworks.explosives.ThrownExplosive>> THROWN = REG.entity("thrown_explosive", '
            '() -> EntityType.Builder.<com.frontierworks.explosives.ThrownExplosive>of(com.frontierworks.explosives.ThrownExplosive::new, MobCategory.MISC)'
            '.sized(0.3f, 0.3f).clientTrackingRange(8).updateInterval(5));')
    for (name, kind, tpl, col, acc, desc) in THROWN:
        m.item(name, (tpl, col, '#6b4a2a', acc), desc=desc,
               java='new com.frontierworks.explosives.ExplosiveItem(new Item.Properties().stacksTo(16), com.frontierworks.explosives.ThrownExplosive.Kind.%s)' % kind,
               jtype='com.frontierworks.explosives.ExplosiveItem')
    t = lambda n: '%s:block/%s' % (M, n)
    c4side = tex.metal_block('#6b6a58', 'c4', 'plated')
    px = c4side.load()
    for x in range(4, 12):
        px[x, 6] = (40, 40, 40, 255)
    for (x, y) in ((7, 9), (8, 9)):
        px[x, y] = (255, 40, 40, 255)
    m.block('c4', java='new com.frontierworks.explosives.C4Block(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(0.5f).sound(net.minecraft.world.level.block.SoundType.WOOL))',
            jtype='com.frontierworks.explosives.C4Block', textures={'block/c4': c4side}, mine='pickaxe', tab='TOOLS', lang='C4 Charge',
            desc='Link with a detonator (sneak + right-click), then detonate from up to 128 blocks away')
    m.item('detonator', ('card', '#3a3a40', None, '#ff3030'), java='new com.frontierworks.explosives.DetonatorItem(new Item.Properties().stacksTo(1))',
           jtype='com.frontierworks.explosives.DetonatorItem', desc='Sneak + right-click C4 to link. Right-click to detonate everything linked.')
    for k, v in {'msg.fw_explosives.linked': 'Linked C4 (%s total)', 'msg.fw_explosives.boom': 'Detonating %s charges',
                 'msg.fw_explosives.none': 'No linked C4', 'tooltip.fw_explosives.links': '%s linked charges'}.items():
        m.tr(k, v)
    m.shaped('dynamite', rl('dynamite'), [' S', 'G ', 'P '], {'S': 'minecraft:string', 'G': 'minecraft:gunpowder', 'P': 'minecraft:paper'}, 3)
    m.shapeless('bundled_dynamite', rl('bundled_dynamite'), [rl('dynamite'), rl('dynamite'), rl('dynamite'), 'minecraft:string'])
    m.shaped('mining_charge', rl('mining_charge'), ['IGI', 'GDG', 'IGI'], {'I': '#forge:nuggets/iron', 'G': 'minecraft:gunpowder', 'D': rl('dynamite')}, 2)
    m.shaped('frag_grenade', rl('frag_grenade'), [' N ', 'IGI', ' I '], {'N': '#forge:nuggets/iron', 'I': '#forge:ingots/iron', 'G': 'minecraft:gunpowder'}, 4)
    m.shaped('incendiary_grenade', rl('incendiary_grenade'), [' N ', 'IBI', ' I '], {'N': '#forge:nuggets/iron', 'I': '#forge:ingots/iron', 'B': 'minecraft:blaze_powder'}, 4)
    m.shaped('smoke_grenade', rl('smoke_grenade'), [' N ', 'ICI', ' I '], {'N': '#forge:nuggets/iron', 'I': '#forge:ingots/iron', 'C': 'minecraft:charcoal'}, 4)
    m.shaped('c4', rl('c4'), ['CTC', 'TRT', 'CTC'], {'C': 'minecraft:clay_ball', 'T': 'minecraft:tnt', 'R': 'minecraft:redstone'}, 2)
    m.shaped('detonator', rl('detonator'), [' A ', 'PRP', 'PBP'], {'A': 'minecraft:lightning_rod', 'P': '#forge:plates/iron', 'R': 'minecraft:redstone',
                                                                   'B': 'minecraft:stone_button'})
    return m
