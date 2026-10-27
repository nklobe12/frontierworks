"""Bounty Board: take hunting contracts from a bounty board, slay the target, and return for coins and loot."""
from gen.fw import Mod, horizontal_state
from gen import tex
from gen.structures import loot_table

M = 'fw_bounties'


def board_front(seed):
    img = tex.planks('#8a6a42', seed)
    px = img.load()
    for (x0, y0, x1, y1) in ((2, 2, 7, 8), (9, 3, 14, 9), (3, 10, 8, 14), (10, 11, 13, 14)):
        for y in range(y0, y1):
            for x in range(x0, x1):
                px[x, y] = (232, 222, 190, 255) if (y - y0) % 2 == 0 or x in (x0, x1 - 1) else (200, 188, 150, 255)
        px[(x0 + x1) // 2, y0] = (180, 40, 40, 255)
    return img


def build():
    m = Mod(M, 'Bounty Board', 'Craft a bounty board and right-click it for a hunting contract: slay a number of a given monster (vanilla '
                               'or Frontierworks), then hand the finished contract back for coins and a loot roll. Harder targets pay more.',
            deps=['fw_coins'], tab='ADVENTURE')
    rl = m.rl
    m.init('com.frontierworks.bounties.BountyEvents.init()')
    t = lambda n: '%s:block/%s' % (M, n)
    m.block('bounty_board', java='new com.frontierworks.bounties.BountyBoardBlock(FWProps.wood())', jtype='com.frontierworks.bounties.BountyBoardBlock',
            state=horizontal_state(m, 'bounty_board', 'bounty_board', lit_prop=False),
            models={'bounty_board': {'parent': 'minecraft:block/orientable', 'textures': {'top': t('bounty_board_side'), 'side': t('bounty_board_side'),
                                                                                              'front': t('bounty_board_front')}}},
            textures={'block/bounty_board_front': board_front('bbf'), 'block/bounty_board_side': tex.planks('#6a4a2a', 'bbs')},
            mine='axe', tab='ADVENTURE', desc='Right-click for a contract; right-click with a finished contract for your reward')
    m.item('bounty_contract', ('scroll', '#e8dcb8', None, '#b02e26'),
           java='new com.frontierworks.bounties.ContractItem(new Item.Properties().stacksTo(1))', jtype='com.frontierworks.bounties.ContractItem', tab='NONE')
    m.shaped('bounty_board', rl('bounty_board'), ['PPP', 'PBP', 'S S'], {'P': '#minecraft:planks', 'B': 'minecraft:writable_book', 'S': '#forge:rods/wooden'})
    for k, v in {'tooltip.fw_bounties.target': 'Slay %s: %s / %s', 'tooltip.fw_bounties.done': 'Complete! Return to a bounty board.',
                 'tooltip.fw_bounties.tier': 'Reward tier: %s', 'msg.fw_bounties.new': 'New contract: slay %s %s',
                 'msg.fw_bounties.progress': 'Bounty: %s / %s', 'msg.fw_bounties.complete': 'Bounty complete! Return to a bounty board.',
                 'msg.fw_bounties.have': 'Finish your current contract first', 'msg.fw_bounties.paid': 'Bounty paid!',
                 'tier.fw_bounties.1': 'Common', 'tier.fw_bounties.2': 'Dangerous', 'tier.fw_bounties.3': 'Deadly'}.items():
        m.tr(k, v)
    loot_table(m, 'rewards/tier1', [(1, 2, [('minecraft:emerald', 6, 1, 3), ('minecraft:iron_ingot', 6, 2, 5), ('minecraft:bread', 4, 3, 6),
                                            ('fw_food:energy_bar', 4, 2, 4), ('minecraft:book', 2, 1, 1, 'enchant'), ('fw_treasure:stash_map', 1)])])
    loot_table(m, 'rewards/tier2', [(2, 3, [('minecraft:emerald', 6, 2, 5), ('minecraft:diamond', 3, 1, 2), ('minecraft:book', 4, 1, 1, 'enchant'),
                                            ('fw_hearts:heart_fragment', 2), ('fw_treasure:explorers_map', 2), ('minecraft:golden_apple', 2),
                                            ('fw_explosives:dynamite', 3, 3, 6)])])
    loot_table(m, 'rewards/tier3', [(2, 4, [('minecraft:diamond', 6, 2, 4), ('minecraft:book', 5, 1, 1, 'enchant'), ('fw_hearts:heart_fragment', 4),
                                            ('fw_relics:lucky_coin', 1), ('fw_relics:ring_of_haste', 1), ('minecraft:netherite_scrap', 2),
                                            ('minecraft:enchanted_golden_apple', 1), ('minecraft:totem_of_undying', 1)])])
    return m
