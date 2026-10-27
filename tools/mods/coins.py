"""Coinage: copper, silver, gold and platinum coins dropped by hostile mobs, exchangeable and spent at bounty boards and traders."""
from gen.fw import Mod
from gen import tex

M = 'fw_coins'
COINS = [('copper_coin', '#c87533'), ('silver_coin', '#d8dee6'), ('gold_coin', '#f1c40f'), ('platinum_coin', '#9fd0de')]


def build():
    m = Mod(M, 'Coinage', 'A frontier currency: hostile mobs drop copper and silver coins (bosses drop gold and platinum). Nine of a '
                          'coin make one of the next. Spend them with wandering traders, swap them for emeralds, or earn more from bounties.',
            tab='ADVENTURE')
    rl = m.rl
    m.init('com.frontierworks.coins.CoinEvents.init()')
    for name, color in COINS:
        m.item(name, ('coin', color), tags=['forge:coins', 'forge:coins/' + name.replace('_coin', '')])
    for (a, _), (b, _) in zip(COINS, COINS[1:]):
        m.shapeless(b + '_from_' + a, rl(b), [rl(a)] * 9)
        m.shapeless(a + '_from_' + b, rl(a), [rl(b)], 9)
    m.item('coin_pouch', ('pouch', '#8a5a2a', None, '#d4af37'), desc='Right-click: a handful of coins',
           java='new LootBagItem(new Item.Properties().stacksTo(16), new ResourceLocation("fw_coins", "bags/coin_pouch"))', jtype='LootBagItem')
    m.shapeless('emerald_from_silver', 'minecraft:emerald', [rl('silver_coin')] * 4)
    m.shapeless('silver_from_emerald', rl('silver_coin'), ['minecraft:emerald'], 3)
    from gen.structures import loot_table
    loot_table(m, 'bags/coin_pouch', [(1, 2, [(rl('copper_coin'), 10, 4, 12), (rl('silver_coin'), 5, 1, 4), (rl('gold_coin'), 1)])])
    m.inject_loot('chest_coins', ['minecraft:chests/simple_dungeon', 'minecraft:chests/abandoned_mineshaft', 'minecraft:chests/village/village_weaponsmith',
                                  'minecraft:chests/shipwreck_treasure', 'minecraft:chests/pillager_outpost', 'minecraft:chests/desert_pyramid',
                                  'fw_dungeons:chests/treasure', 'fw_ruins:chests/ruins', 'fw_camps:chests/bandit', 'fw_camps:chests/explorer', 'fw_outposts:chests/supplies', 'fw_bunkers:chests/storage'],
                  {'type': 'minecraft:chest', 'pools': [{'rolls': {'type': 'minecraft:uniform', 'min': 1, 'max': 2}, 'bonus_rolls': 0, 'entries': [
                      {'type': 'minecraft:item', 'name': rl('copper_coin'), 'weight': 6, 'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 3, 'max': 10}, 'add': False}]},
                      {'type': 'minecraft:item', 'name': rl('silver_coin'), 'weight': 3, 'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 4}, 'add': False}]},
                      {'type': 'minecraft:item', 'name': rl('coin_pouch'), 'weight': 1}]}]})
    return m
