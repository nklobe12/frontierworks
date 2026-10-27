package com.frontierworks.explosives;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class C4Block extends Block {
    public C4Block(Properties props) {
        super(props);
    }

    public static void detonate(Level level, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() instanceof C4Block) {
            level.removeBlock(pos, false);
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6.0f, Level.ExplosionInteraction.TNT);
        }
    }
}
