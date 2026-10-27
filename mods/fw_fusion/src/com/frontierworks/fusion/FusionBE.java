package com.frontierworks.fusion;

import com.frontierworks.core.energy.EnergyUtil;
import com.frontierworks.core.machine.MachineBlockEntity;
import com.frontierworks.core.machine.MachineLayout;
import com.frontierworks.core.machine.MachineMenu;
import com.frontierworks.core.util.FW;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.List;

/**
 * Fusion reactor: spend 2,000,000 FE to ignite a plasma, then fuse deuterium with helium-3 for
 * 10,000 FE/t. Runs out of fuel -> plasma collapses and must be re-ignited.
 */
public class FusionBE extends MachineBlockEntity {
    public static final int IGNITION = 2_000_000;
    public static final int OUTPUT = 10_000;
    public static final MachineLayout LAYOUT = MachineLayout.builder()
            .energy(8, 17, 52)
            .tank(26, 17, 16, 52, 32000, true, false)
            .slot(56, 35, MachineLayout.Role.INPUT)
            .button(0, 104, 54, 64, 18, "gui.fw_fusion.ignite")
            .info(76, 20).extra(3).build();

    private boolean plasma;
    private int fuelTicks;

    public FusionBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, LAYOUT, 50_000_000, 100_000, 50_000);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return stack.is(FW.item("fw_moon:helium3_crystal"));
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return stack.getFluid().isSame(FW.fluid("fw_electrolysis:deuterium"));
    }

    @Override
    public void serverTick() {
        if (level == null || energy == null) return;
        if (plasma) {
            if (fuelTicks <= 0) {
                if (!items.getStackInSlot(0).isEmpty()) {
                    items.extractItem(0, 1, false);
                    fuelTicks = 1200;
                } else {
                    plasma = false;
                }
            }
            if (plasma) {
                FluidStack d = tanks[0].drain(4, IFluidHandler.FluidAction.EXECUTE);
                if (d.getAmount() < 4) {
                    plasma = false;
                } else {
                    fuelTicks--;
                    energy.produce(OUTPUT);
                }
            }
        }
        EnergyUtil.pushToNeighbors(level, worldPosition, energy, 50_000, null);
        setLit(plasma);
        setChanged();
    }

    @Override
    public void onButton(int id, Player player) {
        if (id != 0 || energy == null) return;
        if (plasma) {
            plasma = false;
        } else if (energy.getEnergyStored() >= IGNITION && tanks[0].getFluidAmount() >= 100 && !items.getStackInSlot(0).isEmpty()) {
            energy.consume(IGNITION);
            plasma = true;
        } else {
            player.displayClientMessage(Component.translatable("msg.fw_fusion.cannot_ignite"), true);
        }
        setChanged();
    }

    @Override
    public int getExtra(int i) {
        return i == 0 ? (plasma ? 1 : 0) : i == 1 ? fuelTicks : 0;
    }

    @Override
    public Component buttonLabel(int id, MachineMenu menu) {
        return Component.translatable(menu.extra(0) == 1 ? "gui.fw_core.stop" : "gui.fw_fusion.ignite");
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        boolean on = menu.extra(0) == 1;
        lines.add(Component.translatable(on ? "gui.fw_fusion.plasma" : "gui.fw_fusion.cold").withStyle(on ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("gui.fw_core.generating", on ? OUTPUT : 0));
        if (!on) lines.add(Component.translatable("gui.fw_fusion.needs"));
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        tag.putBoolean("Plasma", plasma);
        tag.putInt("FuelTicks", fuelTicks);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        plasma = tag.getBoolean("Plasma");
        fuelTicks = tag.getInt("FuelTicks");
    }
}
