package com.lodestar.veins;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Strike rock to listen for ore. Reports the strongest traces within the radius, with a compass direction,
 * whether they lie above or below and roughly how far. Better picks listen further.
 */
public class ProspectorsPickItem extends Item {
    private final int radius;

    public ProspectorsPickItem(Properties props, int radius) {
        super(props);
        this.radius = radius;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockPos center = ctx.getClickedPos();
        level.playSound(player, center, SoundEvents.STONE_HIT, SoundSource.PLAYERS, 1.0f, 1.4f);
        if (level.isClientSide || player == null) return InteractionResult.SUCCESS;
        player.getCooldowns().addCooldown(this, 20);
        Map<String, long[]> found = new HashMap<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++)
            for (int dy = -radius; dy <= radius; dy++)
                for (int dz = -radius; dz <= radius; dz++) {
                    p.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!level.isLoaded(p)) continue;
                    BlockState st = level.getBlockState(p);
                    String n = OreIndex.oreName(st);
                    if (n == null) continue;
                    long[] a = found.computeIfAbsent(n, k -> new long[4]);
                    a[0]++;
                    a[1] += dx;
                    a[2] += dy;
                    a[3] += dz;
                }
        ctx.getItemInHand().hurtAndBreak(1, player, pl -> pl.broadcastBreakEvent(ctx.getHand()));
        if (found.isEmpty()) {
            player.displayClientMessage(Component.translatable("msg.ls_veins.no_traces").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }
        List<Map.Entry<String, long[]>> list = new ArrayList<>(found.entrySet());
        list.sort((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]));
        Map.Entry<String, long[]> best = list.get(0);
        player.displayClientMessage(describe(best.getKey(), best.getValue()), false);
        if (list.size() > 1) {
            Map.Entry<String, long[]> second = list.get(1);
            player.displayClientMessage(Component.translatable("msg.ls_veins.also", OreIndex.display(second.getKey()), strength(second.getValue()[0]))
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return InteractionResult.CONSUME;
    }

    private static Component strength(long n) {
        String k = n < 6 ? "faint" : n < 25 ? "moderate" : n < 80 ? "strong" : "very_strong";
        return Component.translatable("msg.ls_veins.strength." + k);
    }

    private static Component describe(String ore, long[] a) {
        double cx = (double) a[1] / a[0], cy = (double) a[2] / a[0], cz = (double) a[3] / a[0];
        double dist = Math.sqrt(cx * cx + cy * cy + cz * cz);
        String dir;
        if (Math.abs(cx) < 2 && Math.abs(cz) < 2) {
            dir = "here";
        } else {
            double ang = Math.toDegrees(Math.atan2(cx, -cz));
            if (ang < 0) ang += 360;
            String[] names = {"north", "northeast", "east", "southeast", "south", "southwest", "west", "northwest"};
            dir = names[(int) Math.round(ang / 45.0) % 8];
        }
        String vert = cy < -2 ? "below" : cy > 2 ? "above" : "level";
        return Component.translatable("msg.ls_veins.traces", strength(a[0]), OreIndex.display(ore).withStyle(s -> s.withColor(OreIndex.color(ore))),
                Component.translatable("msg.ls_veins.dir." + dir), Component.translatable("msg.ls_veins.vert." + vert), Math.round(dist));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.ls_veins.prospectors_pick", radius).withStyle(ChatFormatting.GRAY));
    }
}
