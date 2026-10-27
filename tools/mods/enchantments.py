"""Enchantments+: eleven new enchantments for weapons, tools and boots, found on the enchanting table and in loot."""
from gen.fw import Mod, const

M = 'fw_enchantments'

# name: (rarity, category, slots, max level, min cost base, per level, treasure, desc)
ENCH = {
    'lifesteal': ('RARE', 'WEAPON', 'MAINHAND', 3, 15, 10, False, 'Heals you for 5% of melee damage dealt per level'),
    'frostbite': ('UNCOMMON', 'WEAPON', 'MAINHAND', 2, 10, 10, False, 'Slows enemies you hit'),
    'venom': ('UNCOMMON', 'WEAPON', 'MAINHAND', 2, 10, 10, False, 'Poisons enemies you hit'),
    'thunderstrike': ('VERY_RARE', 'WEAPON', 'MAINHAND', 1, 25, 0, True, '15% chance to call lightning on the target'),
    'beheading': ('RARE', 'WEAPON', 'MAINHAND', 3, 15, 8, False, 'Chance for mobs to drop their heads'),
    'wisdom': ('RARE', 'WEAPON', 'MAINHAND', 3, 12, 10, False, '+50% experience from kills per level'),
    'smelting': ('RARE', 'DIGGER', 'MAINHAND', 1, 15, 0, False, 'Mined blocks drop as if smelted'),
    'long_reach': ('RARE', 'DIGGER', 'MAINHAND', 2, 15, 10, False, '+1 block reach per level while held'),
    'swiftstep': ('RARE', 'ARMOR_FEET', 'FEET', 2, 10, 10, False, 'Move faster'),
    'leaping': ('RARE', 'ARMOR_FEET', 'FEET', 2, 10, 10, False, 'Jump higher'),
    'soulbound': ('VERY_RARE', 'BREAKABLE', 'MAINHAND', 1, 25, 0, True, 'Stays with you when you die'),
}


def build():
    m = Mod(M, 'Enchantments+', 'Eleven new enchantments: Lifesteal, Frostbite, Venom, Thunderstrike, Beheading and Wisdom for weapons; '
                                'Smelting and Long Reach for tools; Swiftstep and Leaping for boots; and the treasure enchantment Soulbound.',
            tab='ADVENTURE')
    m.imp('net.minecraft.world.entity.EquipmentSlot', 'net.minecraft.world.item.enchantment.Enchantment',
          'net.minecraft.world.item.enchantment.EnchantmentCategory', 'com.frontierworks.enchantments.FWEnchantment')
    m.init('com.frontierworks.enchantments.EnchantEvents.init(bus)')
    for name, (rar, cat, slot, mx, base, per, treasure, desc) in ENCH.items():
        slots = 'EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND' if slot == 'MAINHAND' and cat == 'BREAKABLE' else 'EquipmentSlot.' + slot
        m.field('public static final RegistryObject<Enchantment> %s = REG.enchantments().register("%s", () -> new FWEnchantment(Enchantment.Rarity.%s, '
                'EnchantmentCategory.%s, new EquipmentSlot[]{%s}, %d, %d, %d, %s));'
                % (const(name), name, rar, cat, slots, mx, base, per, 'true' if treasure else 'false'))
        m.tr('enchantment.%s.%s' % (M, name), name.replace('_', ' ').title())
        m.tr('enchantment.%s.%s.desc' % (M, name), desc)
    m.field('public static final RegistryObject<com.mojang.serialization.Codec<com.frontierworks.enchantments.SmeltingModifier>> SMELTING_MODIFIER = '
            'REG.lootModifiers().register("smelting", com.frontierworks.enchantments.SmeltingModifier.CODEC);')
    m.data('loot_modifiers/smelting.json', {'type': m.rl('smelting'), 'conditions': [{'condition': 'minecraft:match_tool', 'predicate': {
        'enchantments': [{'enchantment': m.rl('smelting'), 'levels': {'min': 1}}]}}]})
    m.loot_mods.append(m.rl('smelting'))
    # enchanted books in dungeon-ish loot
    books = [{'type': 'minecraft:item', 'name': 'minecraft:book', 'weight': 1, 'functions': [
        {'function': 'minecraft:enchant_randomly', 'enchantments': [m.rl(n)]}]} for n in ENCH]
    m.inject_loot('treasure_books', ['minecraft:chests/simple_dungeon', 'minecraft:chests/abandoned_mineshaft', 'minecraft:chests/stronghold_library',
                                     'minecraft:chests/ancient_city', 'minecraft:chests/bastion_treasure', 'minecraft:chests/end_city_treasure',
                                     'minecraft:chests/woodland_mansion', 'minecraft:chests/desert_pyramid', 'minecraft:chests/jungle_temple',
                                     'fw_dungeons:chests/treasure', 'fw_dungeons:chests/library', 'fw_towers:chests/wizard'],
                  {'type': 'minecraft:chest', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:empty', 'weight': 14}] + books}]})
    return m
