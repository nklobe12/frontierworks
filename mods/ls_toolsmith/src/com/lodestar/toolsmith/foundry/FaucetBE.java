package com.lodestar.toolsmith.foundry;

import com.lodestar.core.block.LSBlockEntity;
import com.lodestar.core.block.LSEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * Faucet: fixed to the side of a drain or tank, it pours into the casting table, basin or tank below when used
 * or given a redstone pulse, and keeps pouring until the target is full or the source runs dry.
 */
public class FaucetBE extends LSBlockEntity implements LSEntityBlock.IInteractable, LSEntityBlock.INeighborAware {
    public static final int RATE = 12;
    private boolean pouring;
    private boolean wasPowered;
    /** fluid currently flowing, for the renderer */
    public FluidStack stream = FluidStack.EMPTY;

    public FaucetBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private Direction facing() {
        return LSEntityBlock.facingOf(getBlockState());
    }

    @Override
    public InteractionResult onUse(Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.level().isClientSide) start();
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public void onNeighborChanged(BlockPos from) {
        boolean p = level.hasNeighborSignal(worldPosition);
        if (p && !wasPowered) start();
        wasPowered = p;
    }

    private void start() {
        pouring = true;
    }

    @Override
    public void tickServer() {
        if (!pouring) return;
        BlockEntity src = level.getBlockEntity(worldPosition.relative(facing().getOpposite()));
        BlockEntity dst = level.getBlockEntity(worldPosition.below());
        if (src == null || dst == null) {
            stop();
            return;
        }
        IFluidHandler in = src.getCapability(ForgeCapabilities.FLUID_HANDLER, facing()).resolve().orElse(null);
        IFluidHandler out = dst.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).resolve().orElse(null);
        if (in == null || out == null) {
            stop();
            return;
        }
        FluidStack avail = in.drain(RATE, IFluidHandler.FluidAction.SIMULATE);
        if (avail.isEmpty()) {
            stop();
            return;
        }
        int accepted = out.fill(avail, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            stop();
            return;
        }
        FluidStack moved = in.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        out.fill(moved, IFluidHandler.FluidAction.EXECUTE);
        if (!stream.isFluidEqual(moved)) {
            stream = new FluidStack(moved, 1);
            sync();
        }
    }

    private void stop() {
        pouring = false;
        if (!stream.isEmpty()) {
            stream = FluidStack.EMPTY;
            sync();
        }
    }

    @Override
    protected void saveAll(CompoundTag tag) {
        tag.putBoolean("Pouring", pouring);
        tag.putBoolean("Powered", wasPowered);
        saveSync(tag);
    }

    @Override
    protected void loadAll(CompoundTag tag) {
        pouring = tag.getBoolean("Pouring");
        wasPowered = tag.getBoolean("Powered");
        loadSync(tag);
    }

    @Override
    protected void saveSync(CompoundTag tag) {
        if (!stream.isEmpty()) tag.put("Stream", stream.writeToNBT(new CompoundTag()));
    }

    @Override
    protected void loadSync(CompoundTag tag) {
        stream = tag.contains("Stream") ? FluidStack.loadFluidStackFromNBT(tag.getCompound("Stream")) : FluidStack.EMPTY;
    }
}
