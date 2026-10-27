package com.frontierworks.rope;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Use on the underside or side of a block: drops a 24-block rope straight down at once. */
public class RopeCoilItem extends Item {
    public RopeCoilItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos start = ctx.getClickedFace() == Direction.DOWN ? ctx.getClickedPos().below() : ctx.getClickedPos().relative(ctx.getClickedFace());
        BlockState rope = Content.ROPE.get().defaultBlockState();
        if (!level.getBlockState(start).canBeReplaced() || !rope.canSurvive(level, start)) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            BlockPos p = start;
            for (int i = 0; i < 24; i++) {
                if (level.isOutsideBuildHeight(p) || !level.getBlockState(p).canBeReplaced()) break;
                level.setBlockAndUpdate(p, rope);
                p = p.below();
            }
            if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
