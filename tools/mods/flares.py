from gen.fw import Mod
from gen import tex

STICKS = {'green': '#5dff6a', 'blue': '#4fd8ff', 'pink': '#ff6ad5', 'yellow': '#ffe94a'}


def build():
    m = Mod('fw_flares', 'Flares & Glowsticks', 'Light the dark: throw flares that light up caves for two minutes, or place glowsticks '
                                                'that glow forever.', tab='TOOLS')
    rl = m.rl
    m.client('com.frontierworks.flares.client.FlaresClient.init(bus)')
    m.block('flare_light', java='new FlareBlocks.FlareLight(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().replaceable().noCollission().noLootTable().instabreak().lightLevel(s -> 15).air())',
            jtype='FlareBlocks.FlareLight', item=False, state={'variants': {'': {'model': '%s:block/flare_light' % m.modid}}},
            models={'flare_light': {'textures': {'particle': 'minecraft:block/glowstone'}}}, loot=None, mine=None)
    m.item('flare', ('dynamite', '#c0392b', None, '#ffb030'), java='new FlareBlocks.FlareItem(new Item.Properties().stacksTo(16))', jtype='FlareBlocks.FlareItem',
           desc='Throw to light up an area for two minutes')
    m.field('public static final RegistryObject<EntityType<FlareEntity>> FLARE_ENTITY = REG.entity("flare", '
            '() -> EntityType.Builder.<FlareEntity>of(FlareEntity::new, MobCategory.MISC).sized(0.25f, 0.25f).clientTrackingRange(8).updateInterval(5));')
    m.shapeless('flare', rl('flare'), ['minecraft:paper', 'minecraft:gunpowder', '#forge:dusts/redstone', '#forge:dusts/glowstone'], 4)
    for c, hexc in STICKS.items():
        name = c + '_glowstick'
        t = '%s:block/%s' % (m.modid, name)
        img = tex.item('glowstick', hexc)
        model = {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'textures': {'particle': t, 'stick': t},
                 'elements': [{'from': [7, 0, 7], 'to': [9, 9, 9], 'shade': False, 'faces': {
                     f: {'texture': '#stick', 'uv': [7, 3, 9, 12]} for f in ('north', 'south', 'east', 'west')}}]}
        m.block(name, java='new FlareBlocks.Glowstick(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().noCollission().instabreak().lightLevel(s -> 12).noOcclusion())',
                jtype='FlareBlocks.Glowstick', tab='TOOLS', models={name: model}, item_model='generated', textures={'block/' + name: img, 'item/' + name: img},
                mine=None, desc='Place on a surface. Glows forever.')
        m.shapeless(name, rl(name), ['#forge:glass_panes', '#forge:dusts/glowstone', 'minecraft:%s_dye' % ('lime' if c == 'green' else 'light_blue' if c == 'blue' else c)], 4)
    return m
