package com.frontierworks.doors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/** Opening one half of a double door opens its partner too. */
public final class DoorEvents {
    private DoorEvents() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(DoorEvents::onUse);
    }

    private static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getEntity().isShiftKeyDown()) return;
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState st = level.getBlockState(pos);
        if (!(st.getBlock() instanceof DoorBlock) || !((DoorBlock) st.getBlock()).type().canOpenByHand()) return;
        if (st.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) pos = pos.below();
        st = level.getBlockState(pos);
        if (!(st.getBlock() instanceof DoorBlock)) return;
        Direction facing = st.getValue(DoorBlock.FACING);
        DoorHingeSide hinge = st.getValue(DoorBlock.HINGE);
        Direction side = hinge == DoorHingeSide.LEFT ? facing.getClockWise() : facing.getCounterClockWise();
        BlockPos other = pos.relative(side);
        BlockState os = level.getBlockState(other);
        if (os.getBlock() == st.getBlock() && os.getValue(DoorBlock.FACING) == facing && os.getValue(DoorBlock.HINGE) != hinge
                && os.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER && os.getValue(DoorBlock.OPEN) == st.getValue(DoorBlock.OPEN)) {
            ((DoorBlock) os.getBlock()).setOpen(event.getEntity(), level, os, other, !st.getValue(DoorBlock.OPEN));
        }
    }
}
