"""Relics: rare trinkets found in dungeons and ruins that grant passive powers while carried."""
from gen.fw import Mod
from gen import tex

M = 'fw_relics'

# name: (kind enum, template, colour, accent, rarity, desc)
RELICS = [
    ('amulet_of_vigor', 'VIGOR', 'amulet', '#c0392b', '#ffd040', 'RARE', '+2 hearts'),
    ('ring_of_haste', 'HASTE', 'ring', '#d4af37', '#ff8030', 'RARE', 'Haste I'),
    ('ring_of_swiftness', 'SWIFT', 'ring', '#3a8ad4', '#c0e8ff', 'RARE', 'Speed I'),
    ('featherfall_charm', 'FEATHER', 'orb', '#f0f0f8', '#a0c8ff', 'RARE', 'No fall damage'),
    ('ember_heart', 'EMBER', 'heart', '#ff7a2a', '#ffd040', 'RARE', 'Fire resistance'),
    ('tidal_pearl', 'TIDAL', 'orb', '#2ab0b8', '#c0fff8', 'RARE', 'Water breathing and faster swimming'),
    ('eye_of_the_owl', 'OWL', 'orb', '#e2b81e', '#3a2a10', 'RARE', 'Night vision'),
    ('phoenix_feather', 'PHOENIX', 'amulet', '#ff5a1a', '#ffe040', 'EPIC', 'Saves you from death once every 10 minutes'),
    ('lucky_coin', 'LUCK', 'coin', '#3aa04a', '#d4af37', 'UNCOMMON', '+1 luck: better loot'),
    ('thorn_ring', 'THORNS', 'ring', '#2e6b2a', '#8a4a2a', 'RARE', 'Reflects 25% of melee damage taken'),
    ('band_of_regeneration', 'REGEN', 'ring', '#ff6ab0', '#ffffff', 'EPIC', 'Slowly regenerates health'),
    ('warding_amulet', 'WARDING', 'amulet', '#9aa8b8', '#4ad8ff', 'RARE', '+3 armor and +2 armor toughness'),
]


def build():
    m = Mod(M, 'Relics', 'Twelve rare trinkets hidden in dungeons, temples, towers and vanilla structures. Keep one anywhere in your '
                         'inventory for its power: extra hearts, haste, speed, fire immunity, night vision, a second life and more.',
            tab='ADVENTURE')
    m.init('com.frontierworks.relics.RelicEvents.init()')
    for (name, kind, tpl, col, acc, rarity, desc) in RELICS:
        m.item(name, (tpl, col, '#6b4a2a', acc), desc=desc + '\nWorks anywhere in your inventory',
               java='new com.frontierworks.relics.RelicItem(new Item.Properties().stacksTo(1).rarity(Rarity.%s), com.frontierworks.relics.RelicItem.Kind.%s)' % (rarity, kind),
               jtype='com.frontierworks.relics.RelicItem')
    m.tr('msg.fw_relics.phoenix', 'The Phoenix Feather flares and saves you!')
    entries = [{'type': 'minecraft:empty', 'weight': 40}] + [{'type': 'minecraft:item', 'name': m.rl(r[0]), 'weight': 1 if r[5] == 'EPIC' else 3} for r in RELICS]
    m.inject_loot('dungeon_relics', ['fw_dungeons:chests/treasure', 'fw_dungeons:chests/crypt', 'fw_dungeons:chests/armory', 'fw_temples:chests/pyramid', 'fw_temples:chests/ziggurat', 'fw_temples:chests/frost',
                                     'fw_towers:chests/wizard', 'fw_skyislands:chests/shrine', 'fw_ruins:chests/altar', 'fw_bunkers:chests/command', 'fw_camps:chests/bandit_boss', 'fw_outposts:chests/lab'],
                  {'type': 'minecraft:chest', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:empty', 'weight': 10}] + entries[1:]}]})
    m.inject_loot('vanilla_relics', ['minecraft:chests/simple_dungeon', 'minecraft:chests/desert_pyramid', 'minecraft:chests/jungle_temple',
                                     'minecraft:chests/stronghold_corridor', 'minecraft:chests/ancient_city', 'minecraft:chests/bastion_treasure',
                                     'minecraft:chests/end_city_treasure', 'minecraft:chests/woodland_mansion', 'minecraft:chests/buried_treasure'],
                  {'type': 'minecraft:chest', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': entries}]})
    return m
