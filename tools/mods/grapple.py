from gen.fw import Mod


def build():
    m = Mod('fw_grapple', 'Grappling Hooks', 'Grappling hooks for explorers: fire at any surface, keep holding to be reeled in, '
                                             'sneak to let go. Three tiers with longer reach and faster pulls.', tab='TOOLS')
    rl = m.rl
    m.client('com.frontierworks.grapple.client.GrappleClient.init(bus)')
    m.field('public static final RegistryObject<EntityType<GrappleHookEntity>> GRAPPLE_HOOK_ENTITY = REG.entity("grapple_hook", '
            '() -> EntityType.Builder.<GrappleHookEntity>of(GrappleHookEntity::new, MobCategory.MISC).sized(0.25f, 0.25f).clientTrackingRange(8).updateInterval(2));')
    m.item('grappling_hook', ('hook', '#9aa0a8'), java='new GrappleItem(new Item.Properties().durability(128), 2.2, 0.9)', jtype='GrappleItem',
           desc='Fire at a surface and hold to be pulled. Sneak to let go.')
    m.item('steel_grappling_hook', ('hook', '#7b8591'), java='new GrappleItem(new Item.Properties().durability(512), 3.0, 1.3)', jtype='GrappleItem',
           desc='Longer reach, faster pull')
    m.item('ender_grappling_hook', ('hook', '#2a8f7e'), java='new GrappleItem(new Item.Properties().durability(2048), 4.0, 1.8)', jtype='GrappleItem',
           desc='Huge reach, very fast pull')
    m.shaped('grappling_hook', rl('grappling_hook'), ['  I', ' S ', 'S  '], {'I': 'minecraft:tripwire_hook', 'S': 'minecraft:string'})
    m.shaped('steel_grappling_hook', rl('steel_grappling_hook'), [' P ', 'PHP', ' R '], {'P': '#forge:plates/steel', 'H': rl('grappling_hook'), 'R': 'fw_rope:rope'})
    m.shaped('ender_grappling_hook', rl('ender_grappling_hook'), [' E ', 'EHE', ' C '], {'E': '#forge:ender_pearls', 'H': rl('steel_grappling_hook'),
                                                                                      'C': 'fw_circuits:basic_circuit'})
    return m
