package com.lodestar.machina;

import com.lodestar.core.machine.MachineLayout;
import com.lodestar.core.machine.MachineMenu;
import com.lodestar.core.machine.MachineSpec;
import com.lodestar.core.machine.ProcessingMachineBE;
import com.lodestar.core.machine.RecipeSource;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import java.util.List;
import java.util.Optional;

/**
 * Fluid Bottler. In fill mode it fills containers (buckets, cells, bottles) from its tank, or soaks items in fluid
 * by imbuing recipes (dirt + water = mud, concrete powder + water = concrete). In empty mode it drains containers
 * into its tank.
 */
public class BottlerBE extends ProcessingMachineBE {
    public static final MachineLayout LAYOUT = MachineLayout.builder()
            .energy(8, 17, 38).slot(9, 59, MachineLayout.Role.BATTERY).tank(30, 17, 16, 52, 8000, true, true)
            .slot(62, 35, MachineLayout.Role.INPUT).arrow(86, 34).slot(120, 35, MachineLayout.Role.OUTPUT)
            .slot(152, 17, MachineLayout.Role.UPGRADE).slot(152, 35, MachineLayout.Role.UPGRADE).slot(152, 53, MachineLayout.Role.UPGRADE)
            .button(0, 58, 58, 52, 14, "gui.ls_machina.bottler.fill").extra(1).build();

    private boolean emptyMode;
    private int containerWork;

    public BottlerBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, new MachineSpec(LAYOUT, RecipeSource.machine(() -> Content.IMBUING), 20000, 400, 1f, 1f));
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (layout.slots.get(slot).role == MachineLayout.Role.INPUT) {
            return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent() || super.isItemValid(slot, stack);
        }
        return super.isItemValid(slot, stack);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return true;
    }

    @Override
    public void onCustomButton(int id, Player player) {
        if (id == 0) {
            emptyMode = !emptyMode;
            containerWork = 0;
            inventoryDirty = true;
        }
    }

    @Override
    public Component buttonLabel(int id, MachineMenu menu) {
        return Component.translatable(menu.extra(0) == 1 ? "gui.ls_machina.bottler.empty" : "gui.ls_machina.bottler.fill");
    }


    @Override
    public int getExtra(int i) {
        return emptyMode ? 1 : 0;
    }

    @Override
    public void serverTick() {
        if (level == null || energy == null) return;
        ItemStack in = items.getStackInSlot(0);
        Optional<IFluidHandlerItem> cap = in.isEmpty() ? Optional.empty()
                : in.copyWithCount(1).getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve();
        if (cap.isPresent() && canRun()) {
            dischargeBatteries();
            if (containerTick(in)) {
                setLit(true);
                return;
            }
        }
        containerWork = 0;
        if (!emptyMode) super.serverTick();
        else {
            dischargeBatteries();
            setLit(false);
        }
    }

    /** Fills or empties one container at a time; returns true while working. */
    private boolean containerTick(ItemStack in) {
        ItemStack one = in.copyWithCount(1);
        IFluidHandlerItem h = one.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().orElse(null);
        if (h == null) return false;
        int cost = (int) Math.round(10 * energyMultiplier());
        if (energy.getEnergyStored() < cost) return false;
        if (emptyMode) {
            FluidStack drained = h.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
            if (drained.isEmpty() || tanks[0].fill(drained, IFluidHandler.FluidAction.SIMULATE) < drained.getAmount()) return false;
            if (!work(cost)) return true;
            h.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
            tanks[0].fill(drained, IFluidHandler.FluidAction.EXECUTE);
        } else {
            if (tanks[0].isEmpty()) return false;
            int filled = h.fill(tanks[0].getFluid().copy(), IFluidHandler.FluidAction.SIMULATE);
            if (filled <= 0) return false;
            ItemStack test = one.copy();
            IFluidHandlerItem th = test.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().orElse(null);
            if (th == null) return false;
            th.fill(tanks[0].getFluid().copy(), IFluidHandler.FluidAction.EXECUTE);
            if (!insertOutput(th.getContainer(), true).isEmpty()) return false;
            if (!work(cost)) return true;
            int f = h.fill(tanks[0].getFluid().copy(), IFluidHandler.FluidAction.EXECUTE);
            tanks[0].drain(f, IFluidHandler.FluidAction.EXECUTE);
        }
        ItemStack result = h.getContainer();
        if (!insertOutput(result, true).isEmpty()) return true;
        insertOutput(result, false);
        items.extractItem(0, 1, false);
        containerWork = 0;
        return true;
    }

    private boolean work(int cost) {
        energy.consume(cost);
        containerWork++;
        progress = containerWork;
        maxProgress = (int) Math.max(4, Math.round(32 * speedMultiplier()));
        rate = cost;
        return containerWork >= maxProgress;
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {}

    @Override
    protected void saveExtra(CompoundTag tag) {
        super.saveExtra(tag);
        tag.putBoolean("EmptyMode", emptyMode);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        super.loadExtra(tag);
        emptyMode = tag.getBoolean("EmptyMode");
    }
}
