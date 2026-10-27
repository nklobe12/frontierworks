"""Heart Containers: permanently raise your maximum health with heart containers from bosses and dungeons."""
from gen.fw import Mod
from gen import tex

M = 'fw_hearts'


def build():
    m = Mod(M, 'Heart Containers', 'Heart Containers permanently add a heart to your maximum health (up to ten extra). Bosses drop '
                                   'them; dungeons and treasure hold heart fragments - four fragments make a container.', tab='ADVENTURE')
    rl = m.rl
    m.init('com.frontierworks.hearts.HeartEvents.init()')
    m.item('heart_container', ('heart', '#e0244a', None, '#ffd0d8'), desc='Right-click: +1 maximum heart, permanently',
           java='new com.frontierworks.hearts.HeartContainerItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC))', jtype='com.frontierworks.hearts.HeartContainerItem')
    m.item('heart_fragment', ('shard', '#e0244a'), desc='Four make a heart container', props='new Item.Properties().rarity(Rarity.RARE)')
    m.item('cursed_heart', ('heart', '#4a2a5a', None, '#a060c0'), desc='Right-click: remove one extra heart (gives back a container)',
           java='new com.frontierworks.hearts.CursedHeartItem(new Item.Properties().stacksTo(16))', jtype='com.frontierworks.hearts.CursedHeartItem')
    m.shapeless('heart_container', rl('heart_container'), [rl('heart_fragment')] * 4)
    m.shapeless('cursed_heart', rl('cursed_heart'), ['minecraft:fermented_spider_eye', 'minecraft:wither_rose', 'minecraft:poisonous_potato'])
    for k, v in {'msg.fw_hearts.gained': 'Your maximum health grew! (+%s extra hearts)', 'msg.fw_hearts.max': 'You can\'t hold any more hearts',
                 'msg.fw_hearts.removed': 'Your heart grows lighter (+%s extra hearts)', 'msg.fw_hearts.none': 'You have no extra hearts'}.items():
        m.tr(k, v)
    frag = {'type': 'minecraft:item', 'name': rl('heart_fragment'), 'weight': 1}
    m.inject_loot('fragments', ['fw_dungeons:chests/treasure', 'fw_dungeons:chests/crypt', 'fw_temples:chests/ziggurat', 'fw_towers:chests/wizard',
                                'fw_skyislands:chests/shrine', 'fw_treasure:chests/stash', 'fw_temples:chests/pyramid', 'fw_camps:chests/bandit_boss', 'minecraft:chests/simple_dungeon', 'minecraft:chests/ancient_city',
                                'minecraft:chests/stronghold_corridor', 'minecraft:chests/buried_treasure', 'minecraft:chests/bastion_treasure',
                                'minecraft:chests/end_city_treasure', 'minecraft:chests/desert_pyramid', 'minecraft:chests/jungle_temple'],
                  {'type': 'minecraft:chest', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:empty', 'weight': 4}, frag]}]})
    m.inject_loot('boss_hearts', ['minecraft:entities/wither', 'minecraft:entities/ender_dragon', 'minecraft:entities/elder_guardian', 'minecraft:entities/warden'],
                  {'type': 'minecraft:entity', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': rl('heart_container')}]}]})
    return m
