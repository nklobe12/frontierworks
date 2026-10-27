"""The Sky Wyvern: a fire-breathing flying boss of the high skies, and armor forged from its scales."""
from gen.fw import Mod
from gen import tex
from gen.models import BoxModel

M = 'fw_wyvern'
PIECES = [('helmet', 'HELMET', ['MMM', 'M M']), ('chestplate', 'CHESTPLATE', ['M M', 'MMM', 'MMM']),
          ('leggings', 'LEGGINGS', ['MMM', 'M M', 'M M']), ('boots', 'BOOTS', ['M M', 'M M'])]


def model():
    m = BoxModel('WyvernModel', 'sky_wyvern', 128, 128)
    b, s, w, h = '#3f6f8f', '#2c4f66', '#7fb0d0', '#e8d8a0'
    m.part('body', (0, 12, 0), [(-5, -4, -10, 10, 8, 20, b), (-3, -6, -8, 6, 2, 14, s)])
    m.part('head', (0, 9, -11), [(-3, -3, -8, 6, 5, 8, s, 'eyes'), (-2, -1, -13, 4, 3, 5, s), (-3, -6, -4, 1, 3, 2, h), (2, -6, -4, 1, 3, 2, h)], anim='head')
    m.part('wing_l', (5, 9, -4), [(0, 0, -4, 22, 1, 14, w), (0, -1, -4, 22, 1, 2, s)], anim='wing_a')
    m.part('wing_r', (-5, 9, -4), [(-22, 0, -4, 22, 1, 14, w), (-22, -1, -4, 22, 1, 2, s)], anim='wing_b')
    m.part('tail', (0, 11, 10), [(-2, -2, 0, 4, 4, 14, b), (-1, -1, 14, 2, 2, 8, s), (-3, -1, 20, 6, 1, 3, h)], anim='sway')
    m.part('leg_l', (-3, 15, 2), [(-1, 0, -1, 2, 6, 2, s)], anim='leg_a')
    m.part('leg_r', (3, 15, 2), [(-1, 0, -1, 2, 6, 2, s)], anim='leg_b')
    return m


def build():
    m = Mod(M, 'The Sky Wyvern', 'A boss of the high skies: blow a Wyvern Lure above y=140 (or anywhere in the Skylands) and the Sky '
                                 'Wyvern answers - a winged beast that rains fireballs and dives at you. Its scales make fireproof armor that '
                                 'lets you drift down gently while sneaking.', deps=['fw_hearts'], tab='ADVENTURE')
    rl = m.rl
    m.imp('com.frontierworks.core.item.BossSummonItem')
    m.init('com.frontierworks.wyvern.WyvernArmorEvents.init()')
    m.mob('sky_wyvern', 'SkyWyvern', size=(3.0, 2.0), egg=('#3f6f8f', '#e8d8a0'), model=model(), scale=2.0, shadow=1.5, spawn=None,
          fire_immune=True, tracking=12, loot={'type': 'minecraft:entity', 'pools': [
              {'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': 'fw_hearts:heart_container'}]},
              {'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': rl('wyvern_scale'), 'functions': [
                  {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 8, 'max': 14}, 'add': False}]}]},
              {'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:phantom_membrane', 'functions': [
                  {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 2, 'max': 6}, 'add': False}]}]}]})
    m.item('wyvern_lure', ('flask', '#3f6f8f', None, '#e8d8a0'), desc='Right-click the ground above y=140 or in the Skylands to call the Sky Wyvern',
           java='new BossSummonItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), Content.SKY_WYVERN, '
                'c -> c.getClickedPos().getY() > 140 || c.getLevel().dimension().location().toString().equals("fw_skylands:skylands"), "msg.fw_wyvern.fail")',
           jtype='BossSummonItem')
    m.item('wyvern_scale', ('shard', '#3f6f8f'), desc='Boss material', props='new Item.Properties().rarity(Rarity.UNCOMMON)')
    m.tr('msg.fw_wyvern.fail', 'Nothing answers from down here - climb higher')
    m.field('public static final FWTiers.Armor WYVERN_MAT = FWTiers.armor("fw_wyvern:wyvern", 38, 3, 8, 6, 3, 20, 3.0f, 0.1f, "fw_wyvern:wyvern_scales");')
    m.tag('items', 'fw_wyvern:wyvern_scales', rl('wyvern_scale'))
    for piece, typ, pattern in PIECES:
        name = 'wyvern_' + piece
        m.item(name, (piece, '#3f6f8f'), java='new ArmorItem(Content.WYVERN_MAT, ArmorItem.Type.%s, new Item.Properties().fireResistant())' % typ,
               jtype='ArmorItem', desc='Full set: fire immunity, and sneak in mid-air to glide down slowly')
        m.shaped(name, rl(name), pattern, {'M': rl('wyvern_scale')})
    m.asset('textures/models/armor/wyvern_layer_1.png', tex.armor_layer('#3f6f8f', 1, 'wyvern'))
    m.asset('textures/models/armor/wyvern_layer_2.png', tex.armor_layer('#3f6f8f', 2, 'wyvern'))
    m.shaped('wyvern_lure', rl('wyvern_lure'), ['FPF', 'PGP', 'FPF'], {'F': 'minecraft:feather', 'P': 'minecraft:phantom_membrane', 'G': 'minecraft:gold_block'})
    return m
