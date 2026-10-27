"""Rocketry: build and fuel rockets on a launch pad and fly to the Moon (Tier 1) and Mars (Tier 2)."""
from gen.fw import Mod
from gen import tex
from gen.models import rocket

M = 'fw_rocketry'


def pad_top(seed):
    img = tex.metal_block('#5a5f66', seed, 'plain')
    px = img.load()
    for i in range(16):
        for j in (0, 15):
            px[i, j] = (226, 184, 30, 255) if (i // 2) % 2 else (40, 40, 44, 255)
            px[j, i] = (226, 184, 30, 255) if (i // 2) % 2 else (40, 40, 44, 255)
    for i in range(5, 11):
        px[i, 7] = (240, 240, 240, 255)
        px[7, i] = (240, 240, 240, 255)
    return img


def build():
    m = Mod(M, 'Rocketry', 'Build a rocket, place it on a launch pad, fill it with rocket fuel, climb in and press jump. Tier 1 rockets '
                           'fly to the Moon, Tier 2 rockets to Mars - and either will bring you home again. Bring a space suit!',
            deps=['fw_moon', 'fw_mars'], tab='SPACE')
    rl = m.rl
    m.init('com.frontierworks.rocketry.RocketHooks.init()')
    m.client('com.frontierworks.rocketry.client.RocketryClient.init(bus)')
    m.field('public static final RegistryObject<EntityType<com.frontierworks.rocketry.RocketEntity>> ROCKET = REG.entity("rocket", '
            '() -> EntityType.Builder.<com.frontierworks.rocketry.RocketEntity>of(com.frontierworks.rocketry.RocketEntity::new, MobCategory.MISC)'
            '.sized(1.4f, 4.2f).clientTrackingRange(10).updateInterval(1));')
    r1 = rocket('RocketModel', 'rocket', '#e8e8ec', '#c0392b')
    m.model(r1)
    r2 = rocket('RocketModel', 'rocket', '#9aa4b0', '#2f6fe4')
    m.texture('entity/rocket_t2', r2.texture(M + 'rocket2'))
    m.extra_renderers.append('e.registerEntityRenderer(Content.ROCKET.get(), RocketRenderer::new);')
    m.extra_layers.append('e.registerLayerDefinition(RocketModel.LAYER, RocketModel::createBodyLayer);')
    m.tr('entity.fw_rocketry.rocket', 'Rocket')
    t = lambda n: '%s:block/%s' % (M, n)
    m.block('launch_pad', java='new Block(FWProps.metal(4.0f))', tab='SPACE',
            models={'launch_pad': {'parent': 'minecraft:block/cube_bottom_top', 'textures': {'top': t('launch_pad_top'), 'side': t('launch_pad_side'), 'bottom': t('launch_pad_side')}}},
            textures={'block/launch_pad_top': pad_top('pad'), 'block/launch_pad_side': tex.metal_block('#4a4f56', 'pads', 'riveted')},
            desc='Place a rocket on top of this')
    for (name, tier, hull, trim, lang, dest) in (('rocket_t1', 1, '#e8e8ec', '#c0392b', 'Tier 1 Rocket', 'the Moon'),
                                                 ('rocket_t2', 2, '#9aa4b0', '#2f6fe4', 'Tier 2 Rocket', 'Mars')):
        m.item(name, ('rocket', hull, None, trim), lang=lang, desc='Flies to %s. Place on a launch pad, fuel it (%d rocket fuel) and press jump while seated.'
                                                             % (dest, 4 if tier == 1 else 8),
               java='new com.frontierworks.rocketry.RocketItem(new Item.Properties().stacksTo(1), %d)' % tier, jtype='com.frontierworks.rocketry.RocketItem')
    m.item('rocket_fuel', ('canister', '#c0392b', None, '#ffb030'), desc='Right-click a rocket to fuel it', tab='SPACE')
    for n, tpl, c in (('nose_cone', 'plate', '#e8e8ec'), ('rocket_fin', 'plate', '#c0392b'), ('rocket_engine', 'canister', '#5a5f66'),
                      ('heat_shield', 'plate', '#3a3a40'), ('fusion_engine', 'canister', '#2f6fe4')):
        m.item(n, (tpl, c, None, '#ffb030'), tab='SPACE')
    for k, v in {'msg.fw_rocketry.fuel': 'Fuel: %s / %s', 'msg.fw_rocketry.need_fuel': 'Not enough fuel (%s / %s)',
                 'msg.fw_rocketry.launch': 'Launch in 3... 2... 1...', 'msg.fw_rocketry.no_dest': 'The destination is unreachable',
                 'msg.fw_rocketry.pad': 'Rockets must be placed on a launch pad', 'msg.fw_rocketry.board': 'Press jump to launch. Sneak to get out.',
                 'tooltip.fw_rocketry.fuel': 'Fuel: %s / %s'}.items():
        m.tr(k, v)
    P = '#forge:plates/steel'
    m.shaped('rocket_fuel', rl('rocket_fuel'), ['PDP', 'PBP', 'PPP'], {'P': '#forge:plates/iron', 'D': 'fw_petro:diesel_bucket', 'B': 'minecraft:blaze_powder'}, 4)
    m.shaped('rocket_fuel_biodiesel', rl('rocket_fuel'), ['PDP', 'PBP', 'PPP'], {'P': '#forge:plates/iron', 'D': 'fw_petro:biodiesel_bucket', 'B': 'minecraft:blaze_powder'}, 4)
    m.shaped('nose_cone', rl('nose_cone'), [' P ', 'PCP', 'PPP'], {'P': P, 'C': 'fw_circuits:basic_circuit'})
    m.shaped('rocket_fin', rl('rocket_fin'), ['  P', ' PP', 'PPP'], {'P': P}, 2)
    m.shaped('rocket_engine', rl('rocket_engine'), ['PHP', 'PCP', 'B B'], {'P': P, 'H': 'fw_parts:heating_coil', 'C': 'fw_circuits:advanced_circuit', 'B': 'minecraft:blaze_rod'})
    m.shaped('heat_shield', rl('heat_shield'), ['OOO', 'PPP'], {'O': 'minecraft:obsidian', 'P': P}, 2)
    m.shaped('fusion_engine', rl('fusion_engine'), ['HEH', 'CRC', 'HEH'], {'H': 'fw_moon:helium3_crystal', 'E': rl('rocket_engine'), 'C': 'fw_circuits:elite_circuit',
                                                                         'R': 'minecraft:redstone_block'})
    m.shaped('rocket_t1', rl('rocket_t1'), [' N ', 'PCP', 'FEF'], {'N': rl('nose_cone'), 'P': P, 'C': 'fw_circuits:advanced_circuit', 'F': rl('rocket_fin'),
                                                                  'E': rl('rocket_engine')})
    m.shaped('rocket_t2', rl('rocket_t2'), [' N ', 'SRS', 'FEF'], {'N': rl('nose_cone'), 'S': rl('heat_shield'), 'R': rl('rocket_t1'), 'F': rl('rocket_fin'),
                                                                  'E': rl('fusion_engine')})
    m.shaped('launch_pad', rl('launch_pad'), ['PPP', 'CCC'], {'P': P, 'C': 'minecraft:gray_concrete'}, 2)
    return m
