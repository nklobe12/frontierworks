"""The Iron Colossus: a towering war machine boss summoned with a Colossus Core."""
from gen.fw import Mod
from gen import tex
from gen.models import BoxModel

M = 'fw_colossus'


def model():
    m = BoxModel('ColossusModel', 'iron_colossus', 128, 128)
    g, d, o = '#7b8591', '#5a626c', '#ff7a2a'
    m.part('body', (0, -2, 0), [(-9, -16, -6, 18, 16, 12, g), (-3, -12, -6.5, 6, 6, 1, o, 'glow'), (-7, -2, -4, 14, 4, 8, d)])
    m.part('head', (0, -18, 0), [(-5, -9, -5, 10, 9, 10, d, 'eyes'), (-6, -6, -6, 12, 2, 1, g)], anim='head')
    m.part('arm_l', (-11, -16, 0), [(-5, -2, -3.5, 7, 28, 7, g), (-6, 18, -4.5, 9, 8, 9, d)], anim='arm_a')
    m.part('arm_r', (11, -16, 0), [(-2, -2, -3.5, 7, 28, 7, g), (-3, 18, -4.5, 9, 8, 9, d)], anim='arm_b')
    m.part('leg_l', (-5, 2, 0), [(-3.5, 0, -3.5, 7, 22, 7, d)], anim='leg_a')
    m.part('leg_r', (5, 2, 0), [(-3.5, 0, -3.5, 7, 22, 7, d)], anim='leg_b')
    return m


def build():
    m = Mod(M, 'The Iron Colossus', 'A boss: a four-block-tall war machine awakened with a Colossus Core. It smashes the ground, flings '
                                    'you skyward and calls scrap drones when damaged. Defeat it for a Heart Container, colossus plating and the '
                                    'ground-shaking Colossus Maul.', deps=['fw_mobs', 'fw_hearts'], tab='ADVENTURE')
    rl = m.rl
    m.imp('com.frontierworks.core.item.BossSummonItem')
    m.mob('iron_colossus', 'IronColossus', size=(2.2, 4.4), egg=('#7b8591', '#ff7a2a'), model=model(), scale=1.3, shadow=1.2, spawn=None,
          tracking=10, lang='Iron Colossus', loot={'type': 'minecraft:entity', 'pools': [
              {'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': 'fw_hearts:heart_container'}]},
              {'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': rl('colossus_plating'), 'functions': [
                  {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 4, 'max': 7}, 'add': False}]}]},
              {'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:iron_block', 'functions': [
                  {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 2, 'max': 5}, 'add': False}]}]},
              {'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': 'fw_mobs:drone_core', 'functions': [
                  {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 3}, 'add': False}]}]}]})
    m.item('colossus_core', ('circuit', '#5a626c', None, '#ff7a2a'), desc='Right-click the ground to awaken the Iron Colossus',
           java='new BossSummonItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), Content.IRON_COLOSSUS, c -> true, "msg.fw_colossus.fail")',
           jtype='BossSummonItem')
    m.item('colossus_plating', ('plate', '#7b8591', None, '#ff7a2a'), desc='Boss material', props='new Item.Properties().rarity(Rarity.UNCOMMON)')
    m.item('colossus_maul', ('warhammer', '#7b8591', '#3a3a3a', '#ff7a2a'), model='handheld', desc='Right-click: ground slam (5s cooldown)',
           java='new com.frontierworks.colossus.ColossusMaul(new Item.Properties().fireResistant().rarity(Rarity.EPIC))', jtype='com.frontierworks.colossus.ColossusMaul')
    m.tr('msg.fw_colossus.fail', 'The core stays silent')
    m.shaped('colossus_core', rl('colossus_core'), ['IDI', 'DRD', 'IDI'], {'I': 'minecraft:iron_block', 'D': 'fw_mobs:drone_core', 'R': 'minecraft:redstone_block'})
    m.shaped('colossus_maul', rl('colossus_maul'), ['PPP', 'PNP', ' S '], {'P': rl('colossus_plating'), 'N': 'minecraft:netherite_ingot', 'S': '#forge:rods/wooden'})
    return m
