"""Alchemy+: five new status effects and twenty new potions, all brewable in a vanilla brewing stand."""
from PIL import Image

from gen.fw import Mod, const
from gen import tex

M = 'fw_potions'

# effect: (category, colour, icon template, attribute modifier or None, lang)
EFFECTS = {
    'vitality': ('BENEFICIAL', '#ff4f6d', 'heart', ('Attributes.MAX_HEALTH', '5a1b3c2e-8f64-4b3e-9c3a-1f2b3c4d5e01', 4.0, 'ADDITION')),
    'fortitude': ('BENEFICIAL', '#8a9aa8', 'chestplate', ('Attributes.KNOCKBACK_RESISTANCE', '5a1b3c2e-8f64-4b3e-9c3a-1f2b3c4d5e02', 0.25, 'ADDITION')),
    'berserk': ('BENEFICIAL', '#c0271a', 'battleaxe', ('Attributes.ATTACK_DAMAGE', '5a1b3c2e-8f64-4b3e-9c3a-1f2b3c4d5e03', 0.2, 'MULTIPLY_TOTAL')),
    'purity': ('BENEFICIAL', '#e8f4ff', 'flask', None),
    'magnetism': ('BENEFICIAL', '#4a6cff', 'magnet', None),
}

# potion: (effect java ref, duration ticks, amplifier, ingredient to brew from awkward / base potion)
POTIONS = [
    ('vitality', 'Content.VITALITY.get()', 3600, 0, 'awkward', 'minecraft:glow_berries'),
    ('long_vitality', 'Content.VITALITY.get()', 9600, 0, 'vitality', 'minecraft:redstone'),
    ('strong_vitality', 'Content.VITALITY.get()', 1800, 1, 'vitality', 'minecraft:glowstone_dust'),
    ('fortitude', 'Content.FORTITUDE.get()', 3600, 0, 'awkward', 'minecraft:iron_ingot'),
    ('long_fortitude', 'Content.FORTITUDE.get()', 9600, 0, 'fortitude', 'minecraft:redstone'),
    ('strong_fortitude', 'Content.FORTITUDE.get()', 1800, 1, 'fortitude', 'minecraft:glowstone_dust'),
    ('berserk', 'Content.BERSERK.get()', 1800, 0, 'awkward', 'minecraft:bone'),
    ('strong_berserk', 'Content.BERSERK.get()', 900, 1, 'berserk', 'minecraft:glowstone_dust'),
    ('purity', 'Content.PURITY.get()', 1200, 0, 'awkward', 'minecraft:lily_of_the_valley'),
    ('long_purity', 'Content.PURITY.get()', 3600, 0, 'purity', 'minecraft:redstone'),
    ('magnetism', 'Content.MAGNETISM.get()', 3600, 0, 'awkward', 'minecraft:iron_nugget'),
    ('long_magnetism', 'Content.MAGNETISM.get()', 9600, 0, 'magnetism', 'minecraft:redstone'),
    ('haste', 'MobEffects.DIG_SPEED', 3600, 0, 'awkward', 'minecraft:amethyst_shard'),
    ('long_haste', 'MobEffects.DIG_SPEED', 9600, 0, 'haste', 'minecraft:redstone'),
    ('strong_haste', 'MobEffects.DIG_SPEED', 1800, 1, 'haste', 'minecraft:glowstone_dust'),
    ('resistance', 'MobEffects.DAMAGE_RESISTANCE', 3600, 0, 'awkward', 'minecraft:shulker_shell'),
    ('long_resistance', 'MobEffects.DAMAGE_RESISTANCE', 9600, 0, 'resistance', 'minecraft:redstone'),
    ('strong_resistance', 'MobEffects.DAMAGE_RESISTANCE', 1800, 1, 'resistance', 'minecraft:glowstone_dust'),
    ('absorption', 'MobEffects.ABSORPTION', 2400, 1, 'awkward', 'minecraft:golden_apple'),
    ('luck', 'MobEffects.LUCK', 6000, 0, 'awkward', 'minecraft:emerald'),
]

DESC = {
    'vitality': '+2 hearts of maximum health per level',
    'fortitude': 'Resists knockback',
    'berserk': '+20% melee damage per level',
    'purity': 'Cleanses harmful effects every second',
    'magnetism': 'Pulls nearby items towards you',
}


def icon(kind, color):
    img = Image.new('RGBA', (18, 18), (0, 0, 0, 0))
    img.paste(tex.item(kind, color, '#6b4a2a', '#ffffff'), (1, 1))
    return img


def build():
    m = Mod(M, 'Alchemy+', 'Five new status effects - Vitality, Fortitude, Berserk, Purity and Magnetism - and twenty new potions '
                           '(including Haste, Resistance, Absorption and Luck) brewed from awkward potions in the vanilla brewing stand.',
            tab='ADVENTURE')
    m.imp('net.minecraft.world.effect.MobEffect', 'net.minecraft.world.effect.MobEffectCategory', 'net.minecraft.world.effect.MobEffectInstance',
          'net.minecraft.world.effect.MobEffects', 'net.minecraft.world.entity.ai.attributes.AttributeModifier',
          'net.minecraft.world.entity.ai.attributes.Attributes', 'net.minecraft.world.item.alchemy.Potion')
    m.init('com.frontierworks.potions.AlchemyHooks.init(bus)')
    for name, (cat, color, ic, attr) in EFFECTS.items():
        argb = '0x' + color.lstrip('#').upper()
        expr = 'new com.frontierworks.potions.AlchemyEffect(MobEffectCategory.%s, %s)' % (cat, argb)
        if attr:
            expr += '.addAttributeModifier(%s, "%s", %s, AttributeModifier.Operation.%s)' % attr
        m.field('public static final RegistryObject<MobEffect> %s = REG.effects().register("%s", () -> %s);' % (const(name), name, expr))
        m.texture('mob_effect/' + name, icon(ic, color))
        m.tr('effect.%s.%s' % (M, name), name.title())
        m.tr('effect.%s.%s.description' % (M, name), DESC[name])
    pot_names = set()
    for (pname, eff, dur, amp, base, ing) in POTIONS:
        base_name = pname.replace('long_', '').replace('strong_', '')
        m.field('public static final RegistryObject<Potion> P_%s = REG.potions().register("%s", () -> new Potion("%s", new MobEffectInstance(%s, %d, %d)));'
                % (const(pname), pname, base_name, eff, dur, amp))
        pot_names.add(base_name)
    for n in sorted(pot_names):
        nice = n.replace('_', ' ').title()
        m.tr('item.minecraft.potion.effect.' + n, 'Potion of ' + nice)
        m.tr('item.minecraft.splash_potion.effect.' + n, 'Splash Potion of ' + nice)
        m.tr('item.minecraft.lingering_potion.effect.' + n, 'Lingering Potion of ' + nice)
        m.tr('item.minecraft.tipped_arrow.effect.' + n, 'Arrow of ' + nice)
    # brewing table for the Java side: (from, ingredient, to)
    rows = []
    for (pname, eff, dur, amp, base, ing) in POTIONS:
        frm = 'Potions.AWKWARD' if base == 'awkward' else 'Content.P_%s.get()' % const(base)
        rows.append('        mix(() -> %s, "%s", () -> Content.P_%s.get());' % (frm, ing, const(pname)))
    m.java_file('Brewing.java', '\n'.join([
        '// GENERATED by tools/mods/potions.py', 'package com.frontierworks.potions;', '',
        'import net.minecraft.world.item.alchemy.Potion;', 'import net.minecraft.world.item.alchemy.Potions;',
        'import net.minecraftforge.common.brewing.BrewingRecipeRegistry;', 'import java.util.function.Supplier;', '',
        'final class Brewing {', '    private Brewing() {}', '',
        '    static void mix(Supplier<Potion> from, String ingredient, Supplier<Potion> to) {',
        '        BrewingRecipeRegistry.addRecipe(new PotionMix(from, com.frontierworks.core.util.FW.item(ingredient), to));', '    }', '',
        '    static void register() {'] + rows + ['    }', '}', '']))
    return m
