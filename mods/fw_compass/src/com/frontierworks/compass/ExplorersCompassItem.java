package com.frontierworks.compass;

import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Explorer's Compass: sneak + right-click cycles through every Frontierworks structure, right-click
 * locates the nearest one and the needle points to it.
 */
public class ExplorersCompassItem extends Item {
    public static final TagKey<Structure> EXPLORABLE = TagKey.create(Registries.STRUCTURE, new ResourceLocation("fw_core", "explorable"));

    public ExplorersCompassItem(Properties props) {
        super(props.stacksTo(1));
    }

    private static List<Holder<Structure>> targets(Level level) {
        Registry<Structure> reg = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        List<Holder<Structure>> out = new ArrayList<>();
        reg.getTag(EXPLORABLE).ifPresent(set -> set.forEach(out::add));
        out.sort(Comparator.comparing(h -> h.unwrapKey().map(k -> k.location().toString()).orElse("")));
        return out;
    }

    @Nullable
    public static GlobalPos target(ItemStack stack) {
        CompoundTag t = stack.getTag();
        if (t == null || !t.contains("TX")) return null;
        ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(t.getString("TDim")));
        return GlobalPos.of(dim, new BlockPos(t.getInt("TX"), 64, t.getInt("TZ")));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel)) return InteractionResultHolder.success(stack);
        ServerLevel sl = (ServerLevel) level;
        List<Holder<Structure>> list = targets(level);
        if (list.isEmpty()) return InteractionResultHolder.fail(stack);
        CompoundTag tag = stack.getOrCreateTag();
        int idx = Math.floorMod(tag.getInt("Index"), list.size());
        if (player.isShiftKeyDown()) {
            idx = (idx + 1) % list.size();
            tag.putInt("Index", idx);
            tag.remove("TX");
            player.displayClientMessage(Component.translatable("msg.fw_compass.target", name(list.get(idx))), true);
            return InteractionResultHolder.success(stack);
        }
        Holder<Structure> h = list.get(idx);
        Pair<BlockPos, Holder<Structure>> found = sl.getChunkSource().getGenerator()
                .findNearestMapStructure(sl, HolderSet.direct(h), player.blockPosition(), 64, false);
        if (found == null) {
            player.displayClientMessage(Component.translatable("msg.fw_compass.none", name(h)), true);
            tag.remove("TX");
        } else {
            BlockPos p = found.getFirst();
            tag.putInt("TX", p.getX());
            tag.putInt("TZ", p.getZ());
            tag.putString("TDim", level.dimension().location().toString());
            int dist = (int) Math.sqrt(p.distSqr(player.blockPosition()));
            player.displayClientMessage(Component.translatable("msg.fw_compass.found", name(h), dist), true);
        }
        player.getCooldowns().addCooldown(this, 40);
        return InteractionResultHolder.success(stack);
    }

    private static Component name(Holder<Structure> h) {
        String id = h.unwrapKey().map(k -> k.location().getPath()).orElse("?");
        StringBuilder sb = new StringBuilder();
        for (String w : id.split("_")) {
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        }
        return Component.literal(sb.toString().trim());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.fw_compass.use").withStyle(ChatFormatting.GRAY));
        GlobalPos t = target(stack);
        if (t != null) tooltip.add(Component.literal(t.pos().getX() + ", " + t.pos().getZ()).withStyle(ChatFormatting.AQUA));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return target(stack) != null;
    }
}
