package com.lodestar.toolsmith.foundry;

import com.lodestar.core.block.LSBlockEntity;
import com.lodestar.core.block.LSEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Casting table or basin. A table holds a cast (or an item to make a gold cast from); pour molten metal onto it
 * with a faucet and, once full and cooled, take the result. A basin needs no cast and makes whole blocks.
 */
public class CastingBE extends LSBlockEntity implements LSEntityBlock.IInteractable, LSEntityBlock.IDropContents {
    public final boolean basin;
    public ItemStack cast = ItemStack.EMPTY;
    public ItemStack output = ItemStack.EMPTY;
    public FluidStack fluid = FluidStack.EMPTY;
    public int required;
    public int cooling;
    public int coolTotal;
    private final LazyOptional<IFluidHandler> cap = LazyOptional.of(Handler::new);

    public CastingBE(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean basin) {
        super(type, pos, state);
        this.basin = basin;
    }

    @Nullable
    private FoundryRules.CastResult rule(FluidStack f) {
        if (f.isEmpty()) return null;
        return basin ? FoundryRules.basin(f.getFluid()) : FoundryRules.table(cast, f.getFluid());
    }

    @Override
    public void tickServer() {
        if (fluid.isEmpty() || required <= 0 || fluid.getAmount() < required) return;
        FoundryRules.CastResult r = rule(fluid);
        if (r == null) return;
        coolTotal = r.coolTicks();
        if (++cooling >= coolTotal) {
            output = r.output().copy();
            if (r.consumesCast()) cast = ItemStack.EMPTY;
            fluid = FluidStack.EMPTY;
            required = 0;
            cooling = 0;
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4f, 1.6f);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, 6, 0.2, 0.05, 0.2, 0.01);
            }
        }
        if (cooling % 10 == 0) sync();
    }

    @Override
    public InteractionResult onUse(Player player, InteractionHand hand, BlockHitResult hit) {
        Level lvl = player.level();
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (lvl.isClientSide) return InteractionResult.SUCCESS;
        ItemStack held = player.getItemInHand(hand);
        if (!output.isEmpty()) {
            give(player, output);
            output = ItemStack.EMPTY;
        } else if (!fluid.isEmpty()) {
            return InteractionResult.CONSUME;
        } else if (!basin && cast.isEmpty() && !held.isEmpty()) {
            cast = held.split(1);
        } else if (!basin && !cast.isEmpty() && held.isEmpty()) {
            give(player, cast);
            cast = ItemStack.EMPTY;
        } else {
            return InteractionResult.PASS;
        }
        sync();
        return InteractionResult.CONSUME;
    }

    private static void give(Player p, ItemStack st) {
        if (!p.addItem(st.copy())) p.drop(st.copy(), false);
    }

    private class Handler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Nonnull
        @Override
        public FluidStack getFluidInTank(int tank) {
            return fluid;
        }

        @Override
        public int getTankCapacity(int tank) {
            return Math.max(required, 1);
        }

        @Override
        public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
            return output.isEmpty() && rule(stack) != null;
        }

        @Override
        public int fill(FluidStack res, FluidAction action) {
            if (!output.isEmpty() || res.isEmpty()) return 0;
            if (!fluid.isEmpty() && !fluid.isFluidEqual(res)) return 0;
            FoundryRules.CastResult r = rule(res);
            if (r == null) return 0;
            int have = fluid.isEmpty() ? 0 : fluid.getAmount();
            int n = Math.min(res.getAmount(), r.amount() - have);
            if (n <= 0) return 0;
            if (action.execute()) {
                if (fluid.isEmpty()) fluid = new FluidStack(res, n);
                else fluid.grow(n);
                required = r.amount();
                cooling = 0;
                sync();
            }
            return n;
        }

        @Nonnull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Nonnull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> c, @Nullable Direction side) {
        if (c == ForgeCapabilities.FLUID_HANDLER) return cap.cast();
        return super.getCapability(c, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        cap.invalidate();
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        if (!cast.isEmpty()) net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, cast);
        if (!output.isEmpty()) net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, output);
    }

    @Override
    protected void saveAll(CompoundTag tag) {
        if (!cast.isEmpty()) tag.put("Cast", cast.save(new CompoundTag()));
        if (!output.isEmpty()) tag.put("Output", output.save(new CompoundTag()));
        if (!fluid.isEmpty()) tag.put("Fluid", fluid.writeToNBT(new CompoundTag()));
        tag.putInt("Required", required);
        tag.putInt("Cooling", cooling);
        tag.putInt("CoolTotal", coolTotal);
    }

    @Override
    protected void loadAll(CompoundTag tag) {
        cast = tag.contains("Cast") ? ItemStack.of(tag.getCompound("Cast")) : ItemStack.EMPTY;
        output = tag.contains("Output") ? ItemStack.of(tag.getCompound("Output")) : ItemStack.EMPTY;
        fluid = tag.contains("Fluid") ? FluidStack.loadFluidStackFromNBT(tag.getCompound("Fluid")) : FluidStack.EMPTY;
        required = tag.getInt("Required");
        cooling = tag.getInt("Cooling");
        coolTotal = tag.getInt("CoolTotal");
    }
}
