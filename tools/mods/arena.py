"""Trial Grounds: activate a trial altar with a sigil and survive waves of monsters for treasure."""
from gen.fw import Mod
from gen import tex
from gen.structures import loot_table

M = 'fw_arena'


def altar_top(seed):
    img = tex.chiseled('#5a5f6a', seed)
    px = img.load()
    for i in range(3, 13):
        px[i, i] = (255, 170, 60, 255)
        px[15 - i, i] = (255, 170, 60, 255)
    for (x, y) in ((7, 7), (8, 8), (7, 8), (8, 7)):
        px[x, y] = (255, 230, 150, 255)
    return img


def build():
    m = Mod(M, 'Trial Grounds', 'Place a Trial Altar, feed it a Trial Sigil and fight off waves of monsters that spawn around it. Clear '
                                'every wave for a treasure hoard; a Greater Sigil brings five tougher waves and better loot. Leave and the trial '
                                'fails.', tab='ADVENTURE')
    rl = m.rl
    t = lambda n: '%s:block/%s' % (M, n)
    m.block('trial_altar', java='new com.frontierworks.arena.TrialAltarBlock(FWProps.stone(5.0f, 1200.0f).lightLevel(s -> 7))',
            jtype='com.frontierworks.arena.TrialAltarBlock',
            models={'trial_altar': {'parent': 'minecraft:block/cube_bottom_top', 'textures': {'top': t('trial_altar_top'), 'side': t('trial_altar_side'),
                                                                                               'bottom': t('trial_altar_side')}}},
            textures={'block/trial_altar_top': altar_top('alt'), 'block/trial_altar_side': tex.bricks('#4a4f5a', 'alts', '#2a2e36')},
            mine='pickaxe', level='iron', tab='ADVENTURE', desc='Right-click with a Trial Sigil to begin a trial')
    m.field('public static final RegistryObject<BlockEntityType<com.frontierworks.arena.TrialAltarBE>> TRIAL_ALTAR_BE = '
            'REG.blockEntity("trial_altar", com.frontierworks.arena.TrialAltarBE::new, Content.TRIAL_ALTAR);')
    m.item('trial_sigil', ('key', '#c0c0c8', None, '#ff8a2a'), desc='Starts a trial: three waves', props='new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)')
    m.item('greater_trial_sigil', ('key', '#d4af37', None, '#c040ff'), desc='Starts a greater trial: five deadly waves',
           props='new Item.Properties().stacksTo(16).rarity(Rarity.RARE)')
    m.shaped('trial_altar', rl('trial_altar'), ['GDG', 'SCS', 'SSS'], {'G': 'minecraft:gold_ingot', 'D': 'minecraft:diamond',
                                                                     'C': 'minecraft:chiseled_stone_bricks', 'S': 'minecraft:stone_bricks'})
    m.shaped('trial_sigil', rl('trial_sigil'), [' I ', 'IRI', ' B '], {'I': 'minecraft:iron_ingot', 'R': 'minecraft:rotten_flesh', 'B': 'minecraft:bone'})
    m.shaped('greater_trial_sigil', rl('greater_trial_sigil'), ['GEG', 'ESE', 'GEG'], {'G': 'minecraft:gold_ingot', 'E': 'minecraft:ender_pearl',
                                                                                    'S': rl('trial_sigil')})
    for k, v in {'msg.fw_arena.started': 'The trial begins!', 'msg.fw_arena.wave': 'Wave %s of %s', 'msg.fw_arena.cleared': 'Trial complete! Claim your reward.',
                 'msg.fw_arena.failed': 'The trial failed - nobody stood their ground', 'msg.fw_arena.busy': 'A trial is already underway',
                 'bossbar.fw_arena.trial': 'Trial - wave %s of %s'}.items():
        m.tr(k, v)
    m.inject_loot('sigils', ['minecraft:chests/simple_dungeon', 'minecraft:chests/abandoned_mineshaft', 'minecraft:chests/pillager_outpost',
                             'fw_dungeons:chests/armory', 'fw_camps:chests/bandit_boss'],
                  {'type': 'minecraft:chest', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [
                      {'type': 'minecraft:empty', 'weight': 6}, {'type': 'minecraft:item', 'name': rl('trial_sigil'), 'weight': 3},
                      {'type': 'minecraft:item', 'name': rl('greater_trial_sigil'), 'weight': 1}]}]})
    loot_table(m, 'rewards/trial', [(3, 5, [('minecraft:emerald', 6, 3, 8), ('minecraft:diamond', 3, 1, 3), ('minecraft:gold_ingot', 6, 3, 8),
                                            ('minecraft:book', 4, 1, 1, 'enchant'), ('fw_coins:silver_coin', 6, 3, 9), ('fw_hearts:heart_fragment', 2),
                                            ('minecraft:golden_apple', 3), ('fw_relics:lucky_coin', 1)])])
    loot_table(m, 'rewards/greater_trial', [(4, 7, [('minecraft:diamond', 6, 2, 5), ('minecraft:book', 5, 1, 1, 'enchant'), ('fw_coins:gold_coin', 6, 2, 6),
                                                    ('fw_hearts:heart_container', 1), ('fw_hearts:heart_fragment', 4), ('minecraft:netherite_scrap', 3, 1, 2),
                                                    ('minecraft:enchanted_golden_apple', 1), ('minecraft:totem_of_undying', 2),
                                                    ('fw_relics:band_of_regeneration', 1), ('fw_relics:phoenix_feather', 1), ('fw_relics:amulet_of_vigor', 1)])])
    return m
