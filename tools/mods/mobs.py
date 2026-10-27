"""Frontier Bestiary: new wildlife and monsters - deer and boars, cave beetles, scrap drones, bog lurkers, mummies, frost wraiths and ember imps."""
from gen.fw import Mod
from gen import tex
from gen.models import quadruped, beetle, drone

M = 'fw_mobs'


def drops(*entries, smelt=None):
    """entries: (item, min, max[, chance]) -> entity loot table with looting."""
    pools = []
    for e in entries:
        item, lo, hi = e[0], e[1], e[2]
        fns = [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}, 'add': False}]
        if item == smelt:
            fns.append({'function': 'minecraft:furnace_smelt', 'conditions': [{'condition': 'minecraft:entity_properties', 'entity': 'this',
                                                                               'predicate': {'flags': {'is_on_fire': True}}}]})
        fns.append({'function': 'minecraft:looting_enchant', 'count': {'type': 'minecraft:uniform', 'min': 0, 'max': 1}})
        pool = {'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': item, 'functions': fns}]}
        if len(e) > 3:
            pool['conditions'] = [{'condition': 'minecraft:killed_by_player'},
                                  {'condition': 'minecraft:random_chance_with_looting', 'chance': e[3], 'looting_multiplier': 0.02}]
        pools.append(pool)
    return {'type': 'minecraft:entity', 'pools': pools}


def build():
    m = Mod(M, 'Frontier Bestiary', 'New creatures for the frontier: deer and wild boars in the forests, rust beetles and venomous cave '
                                    'crawlers underground, scrap drones patrolling the plains at night, bog lurkers in swamps, sand mummies '
                                    'in deserts, frost wraiths in the snow and ember imps in the Nether.', tab='ADVENTURE')
    rl = m.rl
    m.field('public static final RegistryObject<EntityType<MobBolt>> MOB_BOLT = REG.entity("mob_bolt", '
            '() -> EntityType.Builder.<MobBolt>of(MobBolt::new, MobCategory.MISC).sized(0.3f, 0.3f).clientTrackingRange(8).updateInterval(2));')
    m.extra_renderers.append('e.registerEntityRenderer(Content.MOB_BOLT.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.0f, true));')
    # items
    m.item('raw_venison', ('jerky', '#c0504a'), java='new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationMod(0.3f).meat().build()))',
           tags=['forge:raw_meats'])
    m.item('cooked_venison', ('jerky', '#7a3a1e'), java='new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(8).saturationMod(0.8f).meat().build()))',
           tags=['forge:cooked_meats'])
    m.imp('net.minecraft.world.food.FoodProperties')
    m.smelt('cooked_venison', rl('raw_venison'), rl('cooked_venison'), 0.35, blast=False, smoke=True)
    m.item('boar_tusk', ('hook', '#f0e8d0'), desc='Bone-hard. Crafts into bone meal or a sharp dagger handle.')
    m.item('beetle_carapace', ('plate', '#8a4a2a'), desc='Tough shell plate')
    m.item('drone_core', ('circuit', '#7b8591', None, '#4ad8ff'), desc='Salvaged guidance core')
    m.item('frost_essence', ('orb', '#a0e0ff'))
    m.item('ember_core', ('orb', '#ff7a2a'))
    m.item('mummy_wrap', ('scroll', '#d8c8a0'))
    m.item('bog_moss', ('seeds', '#4a6a3a'))
    for n, c in (('spark_bolt', '#4ad8ff'), ('frost_bolt', '#c0f0ff')):
        m.item(n, ('orb', c), tab='NONE')
    m.shapeless('tusk_bonemeal', 'minecraft:bone_meal', [rl('boar_tusk')], 4)
    m.shapeless('carapace_leather', 'minecraft:leather', [rl('beetle_carapace'), rl('beetle_carapace')])
    m.shapeless('wrap_string', 'minecraft:string', [rl('mummy_wrap')], 3)
    m.shapeless('bog_moss_slime', 'minecraft:slime_ball', [rl('bog_moss'), rl('bog_moss'), rl('bog_moss')])
    m.shapeless('drone_core_salvage', 'minecraft:redstone', [rl('drone_core')], 4)
    m.shaped('frost_essence_ice', 'minecraft:blue_ice', ['EEE', 'EIE', 'EEE'], {'E': rl('frost_essence'), 'I': 'minecraft:packed_ice'}, 2)
    m.shapeless('ember_core_blaze', 'minecraft:blaze_powder', [rl('ember_core')], 3)

    # creatures
    deer = quadruped('DeerModel', 'deer', (7, 7, 14), (2, 11), (5, 5, 6), ('#9a6a3a', '#6b4423', '#a8784a'),
                     extras=[('antlers', (0, -3, -3), [(-3, -4, 0, 1, 4, 1, '#e0d0a8'), (2, -4, 0, 1, 4, 1, '#e0d0a8'),
                                                       (-5, -5, 0, 3, 1, 1, '#e0d0a8'), (2, -5, 0, 3, 1, 1, '#e0d0a8')], 'head'),
                             ('tail', (0, 0, 7), [(-1, -1, 0, 2, 3, 1, '#f0e8d8')], 'body', 'sway')])
    m.mob('deer', 'Deer', 'CREATURE', (0.9, 1.6), ('#9a6a3a', '#f0e8d8'), model=deer, spawn='animal',
          loot=drops((rl('raw_venison'), 1, 3), ('minecraft:leather', 0, 2), smelt=rl('raw_venison')))
    boar = quadruped('BoarModel', 'boar', (8, 7, 13), (3, 6), (6, 6, 6), ('#5a3d2b', '#3e2a1e', '#6b4a35'),
                     extras=[('tusks', (0, 0, 0), [(-3, 0, -7, 1, 2, 1, '#f0e8d0'), (2, 0, -7, 1, 2, 1, '#f0e8d0'),
                                                   (-2, -1, -7, 4, 3, 1, '#c09080')], 'head'),
                             ('mane', (0, 0, 0), [(-2, -2, -6, 4, 2, 12, '#2a1e14')], 'body')])
    m.mob('boar', 'Boar', 'CREATURE', (0.9, 0.9), ('#5a3d2b', '#f0e8d0'), model=boar, spawn='animal',
          loot=drops(('minecraft:porkchop', 1, 3), (rl('boar_tusk'), 0, 1), ('minecraft:leather', 0, 1), smelt='minecraft:porkchop'))
    m.mob('rust_beetle', 'RustBeetle', size=(0.9, 0.6), egg=('#8a4a2a', '#3a2a20'), model=beetle('RustBeetleModel', 'rust_beetle', '#8a4a2a', '#3a2a20'),
          loot=drops((rl('beetle_carapace'), 0, 1), ('minecraft:iron_nugget', 1, 4), ('minecraft:raw_copper', 0, 1)))
    m.mob('cave_crawler', 'CaveCrawler', size=(0.7, 0.5), egg=('#3a2a5a', '#a0ff60'), scale=0.8,
          model=beetle('CaveCrawlerModel', 'cave_crawler', '#3a2a5a', '#1a1a2a'),
          spawn=('ON_GROUND', 'MOTION_BLOCKING_NO_LEAVES', 'CaveCrawler::checkCaveSpawn'),
          loot=drops(('minecraft:spider_eye', 0, 1), ('minecraft:amethyst_shard', 0, 1), (rl('beetle_carapace'), 0, 1)))
    m.mob('scrap_drone', 'ScrapDrone', size=(0.8, 0.8), egg=('#7b8591', '#4ad8ff'), model=drone('ScrapDroneModel', 'scrap_drone', '#7b8591', '#3a3a3a'),
          loot=drops(('minecraft:redstone', 1, 3), ('minecraft:iron_nugget', 1, 3), (rl('drone_core'), 1, 1, 0.08)))
    m.mob('bog_lurker', 'BogLurker', size=(0.6, 1.95), egg=('#4a5a3a', '#2a3a2a'),
          skin=tex.humanoid_skin('#4a5a3a', '#2a3a2a', '#6a7a4a', 'boglurker', (230, 230, 80)),
          loot=drops(('minecraft:rotten_flesh', 0, 2), (rl('bog_moss'), 0, 2), ('minecraft:slime_ball', 0, 1)))
    m.mob('sand_mummy', 'SandMummy', size=(0.6, 1.95), egg=('#d8c8a0', '#6a5a3a'),
          skin=tex.humanoid_skin('#d8c8a0', '#a89870', '#c8b890', 'mummy', (255, 200, 60)),
          loot=drops((rl('mummy_wrap'), 0, 2), ('minecraft:rotten_flesh', 0, 1), ('minecraft:gold_nugget', 0, 2)))
    m.mob('frost_wraith', 'FrostWraith', size=(0.6, 1.95), egg=('#c8e0f0', '#3a6aa0'),
          skin=tex.humanoid_skin('#c8e0f0', '#8ab0d0', '#e0f0ff', 'wraith', (80, 200, 255)),
          loot=drops((rl('frost_essence'), 0, 2), ('minecraft:snowball', 0, 3), ('minecraft:ice', 0, 1)))
    m.mob('ember_imp', 'EmberImp', size=(0.5, 1.2), egg=('#8a1a10', '#ffb030'), scale=0.6, fire_immune=True,
          skin=tex.humanoid_skin('#8a1a10', '#3a0a08', '#c0301a', 'imp', (255, 220, 60)),
          spawn=('ON_GROUND', 'MOTION_BLOCKING_NO_LEAVES', 'Monster::checkAnyLightMonsterSpawnRules'),
          loot=drops((rl('ember_core'), 0, 1), ('minecraft:blaze_powder', 0, 1), ('minecraft:magma_cream', 0, 1)))
    # spawning
    m.add_spawn(rl('deer'), '#minecraft:is_forest', 8, 2, 4)
    m.add_spawn(rl('deer'), '#minecraft:is_taiga', 8, 2, 4)
    m.add_spawn(rl('boar'), m.biome_tag('boar_spawns', ['#minecraft:is_forest', '#minecraft:is_savanna', '#minecraft:is_jungle']), 6, 2, 3)
    m.add_spawn(rl('rust_beetle'), '#minecraft:is_overworld', 30, 1, 2)
    m.add_spawn(rl('cave_crawler'), '#minecraft:is_overworld', 25, 1, 3)
    m.add_spawn(rl('scrap_drone'), m.biome_tag('drone_spawns', ['#forge:is_plains', 'minecraft:desert', '#minecraft:is_savanna', '#minecraft:is_badlands']), 10, 1, 2)
    m.add_spawn(rl('bog_lurker'), ['minecraft:swamp', 'minecraft:mangrove_swamp'], 50, 1, 3)
    m.add_spawn(rl('sand_mummy'), ['minecraft:desert'], 40, 1, 3)
    m.add_spawn(rl('frost_wraith'), ['minecraft:snowy_plains', 'minecraft:snowy_taiga', 'minecraft:ice_spikes', 'minecraft:frozen_peaks',
                                     'minecraft:snowy_slopes', 'minecraft:grove', 'minecraft:jagged_peaks'], 25, 1, 2)
    m.add_spawn(rl('ember_imp'), ['minecraft:nether_wastes', 'minecraft:crimson_forest', 'minecraft:basalt_deltas'], 15, 1, 3)
    return m
