package com.frontierworks.nuclear;

import com.frontierworks.core.energy.EnergyUtil;
import com.frontierworks.core.machine.MachineBlockEntity;
import com.frontierworks.core.machine.MachineLayout;
import com.frontierworks.core.machine.MachineMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.List;

/**
 * Fission reactor. Fuel rods produce energy and heat; more rods together are more efficient but run
 * hotter. Water coolant (and cryotheum) removes heat. Over 100% heat the reactor melts down - unless
 * auto-SCRAM (on by default) shuts it off at 90%. A redstone signal also forces a shutdown.
 */
public class ReactorBE extends MachineBlockEntity {
    public static final int MAX_HEAT = 10000;
    public static final MachineLayout LAYOUT = MachineLayout.builder()
            .energy(8, 17, 52)
            .grid(44, 17, 2, 2, MachineLayout.Role.INPUT)
            .slot(44, 55, MachineLayout.Role.FUEL)
            .slot(152, 17, MachineLayout.Role.OUTPUT).slot(152, 35, MachineLayout.Role.OUTPUT)
            .tank(26, 17, 14, 52, 24000, true, false)
            .button(0, 84, 52, 40, 18, "gui.fw_nuclear.toggle")
            .button(1, 128, 52, 40, 18, "gui.fw_nuclear.scram")
            .info(44, 75).extra(4).height(186).build();

    private boolean running;
    private boolean autoScram = true;
    private int heat;
    private int output;
    private int coolantTimer;

    public ReactorBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, LAYOUT, 2_000_000, 0, 8192);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        MachineLayout.Role r = layout.slots.get(slot).role;
        if (r == MachineLayout.Role.INPUT) return stack.getItem() instanceof FuelRodItem && !((FuelRodItem) stack.getItem()).depleted(stack);
        if (r == MachineLayout.Role.FUEL) return stack.is(com.frontierworks.core.util.FW.item("fw_freezer:cryotheum_dust"));
        return false;
    }

    @Override
    public int getSlotLimit(int slot) {
        return layout.slots.get(slot).role == MachineLayout.Role.INPUT ? 1 : 64;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return stack.getFluid().isSame(Fluids.WATER);
    }

    @Override
    public void serverTick() {
        if (level == null || energy == null) return;
        boolean powered = level.hasNeighborSignal(worldPosition);
        if (powered) running = false;
        int rods = 0;
        if (running) {
            for (int i = 0; i < 4; i++) {
                ItemStack st = items.getStackInSlot(i);
                if (st.getItem() instanceof FuelRodItem && !((FuelRodItem) st.getItem()).depleted(st)) rods++;
            }
        }
        output = 0;
        if (rods > 0) {
            float efficiency = 1.0f + 0.3f * (rods - 1);
            output = Math.round(rods * 150 * efficiency);
            heat += rods * rods * 3;
            for (int i = 0; i < 4; i++) {
                ItemStack st = items.getStackInSlot(i);
                if (st.getItem() instanceof FuelRodItem) {
                    FuelRodItem rod = (FuelRodItem) st.getItem();
                    if (!rod.depleted(st)) {
                        rod.burn(st, 1);
                        if (rod.depleted(st)) {
                            ItemStack spent = new ItemStack(Content.DEPLETED_FUEL_ROD.get());
                            if (insertOutput(spent, true).isEmpty()) {
                                insertOutput(spent, false);
                                items.setStackInSlot(i, ItemStack.EMPTY);
                            }
                        }
                    }
                }
            }
            energy.produce(output);
        }
        // cooling
        int passive = 2;
        int want = Math.min(heat, rods * 12 + 8);
        int drained = tanks[0].drain(want, IFluidHandler.FluidAction.EXECUTE).getAmount();
        heat -= drained + passive;
        if (heat > 2000 && coolantTimer <= 0) {
            ItemStack cryo = items.getStackInSlot(4);
            if (!cryo.isEmpty()) {
                items.extractItem(4, 1, false);
                coolantTimer = 200;
            }
        }
        if (coolantTimer > 0) {
            coolantTimer--;
            heat -= 25;
        }
        heat = Math.max(0, heat);
        if (autoScram && heat >= MAX_HEAT * 9 / 10) running = false;
        if (heat >= MAX_HEAT) meltdown();
        if (heat > MAX_HEAT * 7 / 10 && level.random.nextInt(20) == 0 && level instanceof ServerLevel) {
            ((ServerLevel) level).sendParticles(ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1,
                    worldPosition.getZ() + 0.5, 3, 0.3, 0.1, 0.3, 0.01);
        }
        EnergyUtil.pushToNeighbors(level, worldPosition, energy, 8192, null);
        setLit(rods > 0);
        setChanged();
    }

    private void meltdown() {
        Level lvl = level;
        BlockPos p = worldPosition;
        heat = 0;
        running = false;
        lvl.playSound(null, p, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4f, 0.6f);
        lvl.removeBlock(p, false);
        lvl.explode(null, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6.0f, true, Level.ExplosionInteraction.BLOCK);
        for (int i = 0; i < 6; i++) {
            BlockPos l = p.offset(lvl.random.nextInt(5) - 2, lvl.random.nextInt(3) - 1, lvl.random.nextInt(5) - 2);
            if (lvl.isEmptyBlock(l)) lvl.setBlockAndUpdate(l, Blocks.LAVA.defaultBlockState());
        }
    }

    @Override
    public void onButton(int id, Player player) {
        if (id == 0) running = !running;
        if (id == 1) autoScram = !autoScram;
        setChanged();
    }

    @Override
    public int getExtra(int i) {
        switch (i) {
            case 0: return heat;
            case 1: return output;
            case 2: return running ? 1 : 0;
            default: return autoScram ? 1 : 0;
        }
    }

    @Override
    public Component buttonLabel(int id, MachineMenu menu) {
        if (id == 0) return Component.translatable(menu.extra(2) == 1 ? "gui.fw_core.stop" : "gui.fw_core.start");
        return Component.translatable(menu.extra(3) == 1 ? "gui.fw_nuclear.scram_on" : "gui.fw_nuclear.scram_off");
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        int h = menu.extra(0) * 100 / MAX_HEAT;
        ChatFormatting c = h > 80 ? ChatFormatting.RED : h > 50 ? ChatFormatting.GOLD : ChatFormatting.DARK_GREEN;
        lines.add(Component.translatable("gui.fw_nuclear.heat", h).withStyle(c));
        lines.add(Component.translatable("gui.fw_core.generating", menu.extra(1)));
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        tag.putBoolean("Running", running);
        tag.putBoolean("AutoScram", autoScram);
        tag.putInt("Heat", heat);
        tag.putInt("CoolantTimer", coolantTimer);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        running = tag.getBoolean("Running");
        autoScram = !tag.contains("AutoScram") || tag.getBoolean("AutoScram");
        heat = tag.getInt("Heat");
        coolantTimer = tag.getInt("CoolantTimer");
    }
}
