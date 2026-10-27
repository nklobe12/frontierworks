package com.frontierworks.rubber;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Right-click a rubber log to tap latex (chance per use, costs durability). */
public class TreetapItem extends Item {
    public static final TagKey<Block> RUBBER_LOGS = TagKey.create(Registries.BLOCK, new ResourceLocation("fw_rubber", "rubber_logs"));

    public TreetapItem(Properties props) {
        super(props.durability(256));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(RUBBER_LOGS)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            Player player = ctx.getPlayer();
            if (level.random.nextFloat() < 0.35f) {
                int n = 1 + level.random.nextInt(2);
                BlockPos out = pos.relative(ctx.getClickedFace());
                ItemEntity drop = new ItemEntity(level, out.getX() + 0.5, out.getY() + 0.3, out.getZ() + 0.5,
                        new ItemStack(Content.LATEX.get(), n));
                level.addFreshEntity(drop);
                level.playSound(null, pos, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 0.8f, 1.2f);
            } else {
                level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.6f, 1.4f);
            }
            if (player != null) {
                ctx.getItemInHand().hurtAndBreak(1, player, p -> p.broadcastBreakEvent(ctx.getHand()));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
