package com.lodestar.quarry;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

/** Goes in a quarry's sieve slot and decides which mined blocks are kept and which are thrown away. */
public class SieveItem extends Item {
    public enum Mode { ORES_ONLY, VOID_RUBBLE }

    public static final TagKey<Block> RUBBLE = TagKey.create(Registries.BLOCK, new ResourceLocation("ls_quarry", "rubble"));
    public static final TagKey<Block> SOIL = TagKey.create(Registries.BLOCK, new ResourceLocation("ls_quarry", "soil"));
    public static final TagKey<Block> KEEPS = TagKey.create(Registries.BLOCK, new ResourceLocation("ls_quarry", "sieve_keeps"));

    public final Mode mode;

    public SieveItem(Properties props, Mode mode) {
        super(props);
        this.mode = mode;
    }

    /** Whether the drops of a block mined with this sieve installed are kept (false: thrown away). */
    public static boolean keeps(ItemStack sieve, BlockState mined) {
        if (!(sieve.getItem() instanceof SieveItem s)) return true;
        return switch (s.mode) {
            case ORES_ONLY -> mined.is(KEEPS);
            case VOID_RUBBLE -> !mined.is(RUBBLE) && !mined.is(SOIL);
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        for (String line : Component.translatable("tooltip.ls_quarry.sieve." + mode.name().toLowerCase(Locale.ROOT)).getString().split("\n")) {
            tip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
        tip.add(Component.translatable("tooltip.ls_quarry.sieve.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
