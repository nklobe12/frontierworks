package com.lodestar.veins;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;

import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Maps ore blocks to an ore name ("tin", "iron", "diamond"...) using the forge:ores/<name> tags, cached per block. */
public final class OreIndex {
    /** every ore the prospecting tools know, in display order */
    public static final List<String> NAMES = List.of("coal", "iron", "copper", "gold", "redstone", "lapis", "diamond", "emerald",
            "tin", "lead", "silver", "nickel", "zinc", "aluminum", "platinum", "uranium", "cobalt", "tungsten", "quartz", "netherite_scrap");
    private static final Map<Block, String> CACHE = new IdentityHashMap<>();
    private static final String NONE = "";

    private OreIndex() {}

    @Nullable
    public static String oreName(BlockState state) {
        String n = CACHE.get(state.getBlock());
        if (n == null) {
            n = NONE;
            if (state.is(Tags.Blocks.ORES)) {
                n = "other";
                for (String name : NAMES) {
                    if (state.is(TagKey.create(Registries.BLOCK, new ResourceLocation("forge", "ores/" + name)))) {
                        n = name;
                        break;
                    }
                }
            }
            CACHE.put(state.getBlock(), n);
        }
        return n.isEmpty() ? null : n;
    }

    /** Called when tags reload. */
    public static void clear() {
        CACHE.clear();
    }

    public static net.minecraft.network.chat.MutableComponent display(String name) {
        return Component.translatable("ore.ls_veins." + name);
    }

    public static int color(String name) {
        return switch (name) {
            case "coal" -> 0x2B2B2E;
            case "iron" -> 0xD8AF93;
            case "copper" -> 0xD9844F;
            case "gold" -> 0xF2C94C;
            case "redstone" -> 0xC81E1E;
            case "lapis" -> 0x2348B8;
            case "diamond" -> 0x7EE6E0;
            case "emerald" -> 0x2ECC71;
            case "tin" -> 0xC9D1D9;
            case "lead" -> 0x6A7690;
            case "silver" -> 0xE4EBF2;
            case "nickel" -> 0xD6CD98;
            case "zinc" -> 0xA9BCB2;
            case "aluminum" -> 0xE6EAEE;
            case "platinum" -> 0x9FD0DE;
            case "uranium" -> 0x6EC54B;
            case "cobalt" -> 0x3C6FD6;
            case "tungsten" -> 0x4A4F57;
            case "quartz" -> 0xEDE6DD;
            case "netherite_scrap" -> 0x5A3A30;
            default -> 0x9A9A9A;
        };
    }
}
