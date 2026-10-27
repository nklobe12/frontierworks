package com.lodestar.core.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

/** Machine augment. Installed in a machine's augment slots (one per slot); effects stack across slots. */
public class AugmentItem extends Item {
    public enum Kind {
        /** faster processing, more energy per tick */
        SPEED(new float[]{0.25f, 0.5f, 1.0f}),
        /** less energy per operation */
        EFFICIENCY(new float[]{0.15f, 0.25f, 0.4f}),
        /** more energy storage */
        CAPACITY(new float[]{1f, 2f, 4f}),
        /** better secondary output chances, generators produce more */
        OUTPUT(new float[]{0.1f, 0.2f, 0.3f}),
        /** larger tanks */
        FLUID(new float[]{1f, 2f, 4f}),
        /** wider working area for area machines */
        RANGE(new float[]{1f, 2f, 3f});

        private final float[] values;

        Kind(float[] values) {
            this.values = values;
        }

        public float value(int tier) {
            return values[Math.max(0, Math.min(values.length - 1, tier - 1))];
        }
    }

    public final Kind kind;
    public final int tier;

    public AugmentItem(Properties props, Kind kind, int tier) {
        super(props.stacksTo(16));
        this.kind = kind;
        this.tier = tier;
    }

    public float value() {
        return kind.value(tier);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int pct = Math.round(value() * 100);
        tooltip.add(Component.translatable("tooltip.ls_core.augment." + kind.name().toLowerCase(Locale.ROOT), pct).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.ls_core.augment.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
