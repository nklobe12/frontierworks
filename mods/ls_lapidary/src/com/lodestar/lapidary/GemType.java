package com.lodestar.lapidary;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Locale;

/** The eight Lapidary gems: colour, and the bonus each gives when socketed into a tool or weapon and into armor. */
public enum GemType {
    GARNET(0x8A1C2C, GemEffect.LIFESTEAL, GemEffect.TOUGHNESS),
    TOPAZ(0xF0A12A, GemEffect.MINING_SPEED, GemEffect.MOVE_SPEED),
    PERIDOT(0x8FC23A, GemEffect.WISDOM, GemEffect.REGEN),
    SAPPHIRE(0x2A55C9, GemEffect.ATTACK_SPEED, GemEffect.ARMOR),
    RUBY(0xE3214F, GemEffect.ATTACK_DAMAGE, GemEffect.MAX_HEALTH),
    OPAL(0xC9DCE6, GemEffect.LUCK, GemEffect.LUCK),
    ONYX(0x55525E, GemEffect.KNOCKBACK, GemEffect.KNOCKBACK_RESIST),
    AQUAMARINE(0x6FD3DC, GemEffect.AQUA_MINING, GemEffect.SWIM_SPEED);

    public final int color;
    public final GemEffect toolEffect;
    public final GemEffect armorEffect;

    GemType(int color, GemEffect tool, GemEffect armor) {
        this.color = color;
        this.toolEffect = tool;
        this.armorEffect = armor;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public GemEffect effect(boolean armor) {
        return armor ? armorEffect : toolEffect;
    }

    public Component displayName() {
        return Component.translatable("item.ls_lapidary." + id());
    }

    public Item cutItem() {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation("ls_lapidary", id()));
    }

    public Item roughItem() {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation("ls_lapidary", "rough_" + id()));
    }

    @Nullable
    public static GemType byId(String id) {
        for (GemType g : values()) if (g.id().equals(id)) return g;
        return null;
    }

    @Nullable
    public static GemType ofCut(ItemStack stack) {
        return stack.getItem() instanceof CutGemItem c ? c.gem : null;
    }
}
