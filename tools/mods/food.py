"""Frontier Kitchen: four new crops and hearty frontier cooking - burgers, chili, salsa, cornbread, rations and more."""
from gen.fw import Mod, const
from gen import tex

M = 'fw_food'

CROPS = {
    # crop: (stem, fruit colour, produce lang, nutrition, saturation)
    'tomato': ('#4f8a3a', '#e0362c', 'Tomato', 3, 0.4),
    'corn': ('#7aa83a', '#f2c94c', 'Corn', 3, 0.5),
    'onion': ('#6fae4f', '#e8d7b0', 'Onion', 2, 0.3),
    'chili_pepper': ('#3f7a2e', '#c0271a', 'Chili Pepper', 2, 0.3),
}

# name: (template, colour, nutrition, saturation, extras, lang)
FOODS = [
    ('roasted_corn', 'food_bar', '#e8b040', 6, 0.6, '', None),
    ('popcorn', 'bowl', '#fff4c8', 4, 0.3, 'fast', None),
    ('cornbread', 'round_food', '#e2b85a', 5, 0.6, '', None),
    ('onion_rings', 'ring', '#d9a441', 5, 0.5, 'fast', None),
    ('tomato_soup', 'bowl', '#c8321e', 7, 0.7, '', None),
    ('salsa', 'bowl', '#d8452c', 6, 0.6, '', None),
    ('burger', 'round_food', '#a0622a', 12, 0.9, 'meat', None),
    ('chili_con_carne', 'bowl', '#8a2a14', 10, 0.8, 'meat|fire', 'Chili con Carne'),
    ('pizza_slice', 'food_bar', '#e8a040', 5, 0.5, 'fast', None),
    ('beef_jerky', 'jerky', '#7a3a1e', 4, 0.6, 'fast|meat', None),
    ('trail_mix', 'seeds', '#b0784a', 3, 0.4, 'fast|always', None),
    ('energy_bar', 'food_bar', '#c88a3a', 5, 0.7, 'fast|always|speed', None),
    ('mre', 'pouch', '#6b7a3a', 14, 1.0, 'meat|stack16', 'MRE Ration'),
    ('stuffed_pepper', 'round_food', '#3f9a2e', 8, 0.8, '', None),
    ('fried_egg', 'round_food', '#f6f0d8', 4, 0.5, 'fast', None),
    ('corn_chowder', 'bowl', '#f0d890', 8, 0.8, '', None),
]

DESCS = {'chili_con_carne': 'Fire resistance for 30 seconds', 'energy_bar': 'A quick burst of speed', 'mre': 'A full meal in a pouch',
         'trail_mix': 'Edible even when full'}


def crop_tex(kind, stage, seed):
    stem, fruit = tex.rgb(CROPS[kind][0]), tex.rgb(CROPS[kind][1])
    img = tex.blank()
    px = img.load()
    h = [4, 7, 11, 14][stage]
    for i, col in enumerate((3, 8, 12)):
        top = 16 - h + (i % 2)
        for y in range(top, 16):
            px[col, y] = tex.shade(stem, 0.75 + (16 - y) / 50.0)
            if (y + i) % 3 == 0:
                lx = col - 1 if (y // 3) % 2 else col + 1
                if 0 <= lx < 16:
                    px[lx, y] = tex.shade(stem, 1.05)
                lx2 = col - 2 if (y // 3) % 2 else col + 2
                if 0 <= lx2 < 16 and stage > 1:
                    px[lx2, y - 1] = tex.shade(stem, 0.9)
        if stage == 3:
            if kind == 'corn':
                for y in range(top + 3, top + 8):
                    if col + 1 < 16:
                        px[col + 1, y] = tex.shade(fruit, 0.9 + (y % 2) * 0.15)
            elif kind == 'onion':
                for dx in (-1, 0, 1):
                    if 0 <= col + dx < 16:
                        px[col + dx, 15] = tex.shade(fruit, 1.0)
                        px[col + dx, 14] = tex.shade(fruit, 0.9)
            else:
                for (dx, dy) in ((1, 4), (2, 4), (1, 5), (2, 5), (-2, 8), (-1, 8), (-2, 9), (-1, 9)):
                    x, y = col + dx, top + dy
                    if 0 <= x < 16 and 0 <= y < 16:
                        px[x, y] = tex.shade(fruit, 1.0 if dy % 2 == 0 else 0.8)
    return img


def crop_loot(crop_rl, produce, seeds):
    age7 = {'condition': 'minecraft:block_state_property', 'block': crop_rl, 'properties': {'age': '7'}}
    return {'type': 'minecraft:block', 'functions': [{'function': 'minecraft:explosion_decay'}], 'pools': [
        {'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:alternatives', 'children': [
            {'type': 'minecraft:item', 'name': produce, 'conditions': [age7], 'functions': [
                {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 3}, 'add': False},
                {'function': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:uniform_bonus_count',
                 'parameters': {'bonusMultiplier': 1}}]},
            {'type': 'minecraft:item', 'name': seeds}]}]},
        {'rolls': 1, 'bonus_rolls': 0, 'conditions': [age7], 'entries': [{'type': 'minecraft:item', 'name': seeds, 'functions': [
            {'function': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:binomial_with_bonus_count',
             'parameters': {'extra': 1, 'probability': 0.5}}]}]}]}


def food_java(n, s, extras, stack=64, bowl=False):
    b = 'new FoodProperties.Builder().nutrition(%d).saturationMod(%sf)' % (n, s)
    ex = extras.split('|') if extras else []
    if 'fast' in ex:
        b += '.fast()'
    if 'always' in ex:
        b += '.alwaysEat()'
    if 'meat' in ex:
        b += '.meat()'
    if 'fire' in ex:
        b += '.effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0), 1.0f)'
    if 'speed' in ex:
        b += '.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 0), 1.0f)'
    if 'stack16' in ex:
        stack = 16
    props = 'new Item.Properties().food(%s.build())' % b
    if bowl:
        return 'new BowlFoodItem(%s.stacksTo(1))' % props, 'BowlFoodItem'
    if stack != 64:
        props += '.stacksTo(%d)' % stack
    return 'new Item(%s)' % props, 'Item'


def build():
    m = Mod(M, 'Frontier Kitchen', 'Tomatoes, corn, onions and chili peppers (seeds drop from grass), plus sixteen hearty frontier meals: '
                                   'burgers, chili con carne, salsa, cornbread, beef jerky, energy bars, MRE rations and more. Seeds can '
                                   'also be spun into seed oil for biodiesel.', tab='ADVENTURE')
    m.imp('net.minecraft.world.food.FoodProperties', 'net.minecraft.world.effect.MobEffectInstance', 'net.minecraft.world.effect.MobEffects')
    m.init('com.frontierworks.food.FoodHooks.init(bus)')
    rl = m.rl
    t = lambda n: '%s:block/%s' % (M, n)
    for crop, (stem, fruit, lang, n, s) in CROPS.items():
        block, seeds = crop + '_crop', crop + '_seeds'
        stages = {}
        models = {}
        for st in range(4):
            stages['block/%s_stage%d' % (crop, st)] = crop_tex(crop, st, M + crop + str(st))
            models['%s_stage%d' % (crop, st)] = {'parent': 'minecraft:block/crop', 'render_type': 'minecraft:cutout', 'textures': {'crop': t('%s_stage%d' % (crop, st))}}
        age_stage = [0, 0, 1, 1, 2, 2, 2, 3]
        state = {'variants': {'age=%d' % a: {'model': t('%s_stage%d' % (crop, age_stage[a]))} for a in range(8)}}
        m.block(block, java='new com.frontierworks.food.FoodCropBlock(FWProps.crop(), () -> Content.%s.get())' % const(seeds),
                jtype='com.frontierworks.food.FoodCropBlock', item=False, state=state, models=models, textures=stages,
                loot=crop_loot(rl(block), rl(crop), rl(seeds)), mine=None, lang=lang + ' Crop', tags=['minecraft:crops'])
        m.item(seeds, ('seeds', fruit), java='new ItemNameBlockItem(Content.%s.get(), new Item.Properties())' % const(block), jtype='ItemNameBlockItem',
               tags=['forge:seeds', 'forge:seeds/' + crop], lang=lang + ' Seeds')
        java, jt = food_java(n, s, '')
        m.item(crop, ('round_food' if crop in ('tomato', 'onion') else 'food_bar' if crop == 'corn' else 'jerky', fruit, None, stem),
               java=java, jtype=jt, lang=lang, tags=['forge:crops', 'forge:crops/' + crop, 'forge:vegetables', 'forge:vegetables/' + crop])
        m.shapeless(seeds + '_from_' + crop, rl(seeds), [rl(crop)], 2 if crop != 'corn' else 3)
        m.mrecipe('fw_centrifuge:centrifuging', 'seed_oil_' + crop, [(rl(seeds), 8)], [], time=120, energy=20, fluid_out=('fw_food:seed_oil', 250))
    for name, tpl, color, n, s, extras, lang in FOODS:
        java, jt = food_java(n, s, extras, bowl=(tpl == 'bowl'))
        m.item(name, (tpl, color, '#8a6a3a', '#f1e0a0'), java=java, jtype=jt, lang=lang, desc=DESCS.get(name))
    from gen.fw import fluid
    fluid(m, 'seed_oil', '#d8c25a', density=920, viscosity=1400, tab='MATERIALS')
    m.mrecipe('fw_centrifuge:centrifuging', 'seed_oil_wheat', [('minecraft:wheat_seeds', 8)], [], time=120, energy=20, fluid_out=('fw_food:seed_oil', 200))
    # recipes
    m.smelt('roasted_corn', rl('corn'), rl('roasted_corn'), 0.35, blast=False, smoke=True)
    m.smelt('onion_rings', rl('onion'), rl('onion_rings'), 0.35, blast=False, smoke=True)
    m.smelt('fried_egg', 'minecraft:egg', rl('fried_egg'), 0.35, blast=False, smoke=True)
    m.shapeless('popcorn', rl('popcorn'), ['minecraft:bowl', rl('corn'), rl('corn')])
    m.shapeless('cornbread', rl('cornbread'), [rl('corn'), rl('corn'), rl('corn'), 'minecraft:egg', 'minecraft:milk_bucket'], 3)
    m.shapeless('tomato_soup', rl('tomato_soup'), ['minecraft:bowl', rl('tomato'), rl('tomato'), rl('onion')])
    m.shapeless('salsa', rl('salsa'), ['minecraft:bowl', rl('tomato'), rl('onion'), rl('chili_pepper')])
    m.shapeless('burger', rl('burger'), ['minecraft:bread', 'minecraft:cooked_beef', rl('tomato'), rl('onion')])
    m.shapeless('chili_con_carne', rl('chili_con_carne'), ['minecraft:bowl', rl('chili_pepper'), rl('chili_pepper'), 'minecraft:cooked_beef', rl('tomato')])
    m.shapeless('pizza_slice', rl('pizza_slice'), ['minecraft:bread', rl('tomato'), 'minecraft:cooked_porkchop', 'minecraft:brown_mushroom'], 4)
    m.shapeless('beef_jerky', rl('beef_jerky'), ['minecraft:cooked_beef', 'minecraft:sugar'], 3)
    m.shapeless('trail_mix', rl('trail_mix'), ['minecraft:sweet_berries', 'minecraft:wheat_seeds', 'minecraft:cocoa_beans', 'minecraft:pumpkin_seeds'], 4)
    m.shapeless('energy_bar', rl('energy_bar'), ['minecraft:honey_bottle', 'minecraft:wheat', 'minecraft:cocoa_beans', 'minecraft:sugar'], 3)
    m.shapeless('mre', rl('mre'), [rl('beef_jerky'), rl('cornbread'), 'minecraft:apple', rl('energy_bar'), 'minecraft:paper'])
    m.shapeless('stuffed_pepper', rl('stuffed_pepper'), [rl('chili_pepper'), 'minecraft:cooked_beef', rl('onion')], 2)
    m.shapeless('corn_chowder', rl('corn_chowder'), ['minecraft:bowl', rl('corn'), rl('corn'), 'minecraft:milk_bucket', 'minecraft:baked_potato'])
    # seeds from grass
    m.inject_loot('grass_seeds', ['minecraft:blocks/grass', 'minecraft:blocks/tall_grass', 'minecraft:blocks/fern'], {
        'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:empty', 'weight': 64}] +
                                              [{'type': 'minecraft:item', 'name': rl(c + '_seeds'), 'weight': 1} for c in CROPS],
                                              'conditions': [{'condition': 'minecraft:inverted', 'term': {'condition': 'minecraft:match_tool',
                                                                                                          'predicate': {'items': ['minecraft:shears']}}}]}]})
    # village and treasure chests get some seeds/rations
    m.inject_loot('village_food', ['minecraft:chests/village/village_plains_house', 'minecraft:chests/village/village_savanna_house',
                                   'minecraft:chests/village/village_desert_house', 'minecraft:chests/village/village_taiga_house'], {
        'type': 'minecraft:chest', 'pools': [{'rolls': {'type': 'minecraft:uniform', 'min': 0, 'max': 2}, 'bonus_rolls': 0, 'entries':
                                              [{'type': 'minecraft:item', 'name': rl(c + '_seeds'), 'weight': 2, 'functions': [
                                                  {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 4}, 'add': False}]} for c in CROPS] +
                                              [{'type': 'minecraft:item', 'name': rl('cornbread'), 'weight': 2}]}]})
    m.inject_loot('dungeon_rations', ['minecraft:chests/simple_dungeon', 'minecraft:chests/abandoned_mineshaft'], {
        'type': 'minecraft:chest', 'pools': [{'rolls': {'type': 'minecraft:uniform', 'min': 0, 'max': 1}, 'bonus_rolls': 0, 'entries': [
            {'type': 'minecraft:item', 'name': rl('mre'), 'weight': 1}, {'type': 'minecraft:item', 'name': rl('beef_jerky'), 'weight': 2, 'functions': [
                {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 2, 'max': 6}, 'add': False}]}]}]})
    return m
