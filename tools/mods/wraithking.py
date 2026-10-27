"""The Wraith King: an undead monarch boss summoned at night with the Cursed Crown."""
from gen.fw import Mod
from gen import tex

M = 'fw_wraithking'


def build():
    m = Mod(M, 'The Wraith King', 'A boss of the night: place the Cursed Crown after dark to summon the Wraith King. He hurls wither '
                                  'skulls, blinks behind you and raises skeleton guards - wither skeletons once he\'s enraged. Drops a Heart '
                                  'Container, soul gems and the life-draining Wraith Blade.', deps=['fw_hearts'], tab='ADVENTURE')
    rl = m.rl
    m.imp('com.frontierworks.core.item.BossSummonItem')
    m.mob('wraith_king', 'WraithKing', size=(0.9, 3.0), egg=('#2a1a3a', '#a060ff'), scale=1.6, spawn=None, fire_immune=True, tracking=10,
          skin=tex.humanoid_skin('#3a2a4a', '#1a0e24', '#b8b0c8', 'wraithking', (180, 90, 255)),
          loot={'type': 'minecraft:entity', 'pools': [
              {'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': 'fw_hearts:heart_container'}]},
              {'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': rl('soul_gem'), 'functions': [
                  {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 2, 'max': 4}, 'add': False}]}]},
              {'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:wither_skeleton_skull'}]},
              {'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:gold_ingot', 'functions': [
                  {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 4, 'max': 10}, 'add': False}]}]}]})
    m.item('cursed_crown', ('trophy', '#d4af37', None, '#a060ff'), desc='Right-click the ground at night to summon the Wraith King',
           java='new BossSummonItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), Content.WRAITH_KING, '
                'c -> !c.getLevel().isDay() || !c.getLevel().dimensionType().natural(), "msg.fw_wraithking.fail")', jtype='BossSummonItem')
    m.item('soul_gem', ('gem', '#a060ff'), desc='Boss material', props='new Item.Properties().rarity(Rarity.UNCOMMON)')
    m.item('wraith_blade', ('sword', '#3a2a4a', '#1a0e24', '#a060ff'), model='handheld', desc='Heals you and withers your foes with every hit',
           java='new com.frontierworks.wraithking.WraithBlade(new Item.Properties().fireResistant().rarity(Rarity.EPIC))', jtype='com.frontierworks.wraithking.WraithBlade')
    m.tr('msg.fw_wraithking.fail', 'The crown only answers in darkness')
    m.shaped('cursed_crown', rl('cursed_crown'), ['GWG', 'SDS'], {'G': 'minecraft:gold_ingot', 'W': 'minecraft:wither_skeleton_skull',
                                                                 'S': 'minecraft:soul_sand', 'D': 'minecraft:diamond'})
    m.shaped('wraith_blade', rl('wraith_blade'), [' G ', ' G ', ' N '], {'G': rl('soul_gem'), 'N': 'minecraft:netherite_sword'})
    return m
