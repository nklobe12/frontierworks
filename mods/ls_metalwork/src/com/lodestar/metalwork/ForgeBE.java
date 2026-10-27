package com.lodestar.metalwork;

import com.lodestar.core.machine.MachineBlockEntity;
import com.lodestar.core.machine.MachineLayout;
import com.lodestar.core.machine.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;

import java.util.List;

/**
 * Smith's forge: burns furnace fuel to heat up to three workpieces. The forge itself warms towards 1300 C while
 * burning (1550 C while bellows blow into it) and the metal follows the forge's temperature. Only items with
 * smithing recipes can be heated.
 */
public class ForgeBE extends MachineBlockEntity {
    public static final MachineLayout LAYOUT = MachineLayout.builder()
            .grid(44, 17, 3, 1, MachineLayout.Role.INPUT).slot(62, 53, MachineLayout.Role.FUEL).flame(63, 36).info(104, 20).extra(1).build();
    private static final int BASE_MAX = 1300, BOOST_MAX = 1550;

    private int temperature = 20;
    private int boost;

    public ForgeBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, LAYOUT, 0, 0, 0);
    }

    @Override
    public boolean hasSideConfig() {
        return true;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        MachineLayout.Role r = layout.slots.get(slot).role;
        if (r == MachineLayout.Role.FUEL) return ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0;
        if (r == MachineLayout.Role.INPUT) return level == null || !SmithingRecipe.forItem(level, stack).isEmpty();
        return super.isItemValid(slot, stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        return layout.slots.get(slot).role == MachineLayout.Role.INPUT ? 1 : 64;
    }

    /** Called by bellows. */
    public void blow() {
        boost = Math.min(400, boost + 60);
        if (burn > 0) burn = Math.max(0, burn - 20);
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        boolean hasWork = false;
        for (int i = 0; i < 3; i++) if (!items.getStackInSlot(i).isEmpty()) hasWork = true;
        if (burn <= 0 && hasWork && canRun()) {
            ItemStack fuel = items.getStackInSlot(3);
            int t = ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING);
            if (t > 0) {
                burn = maxBurn = t;
                ItemStack rem = fuel.getCraftingRemainingItem();
                items.extractItem(3, 1, false);
                if (!rem.isEmpty() && items.getStackInSlot(3).isEmpty()) items.setStackInSlot(3, rem);
            }
        }
        int max = boost > 0 ? BOOST_MAX : BASE_MAX;
        if (burn > 0) {
            burn--;
            temperature = Math.min(max, temperature + (boost > 0 ? 6 : 3));
        } else {
            temperature = Math.max(20, temperature - 4);
        }
        if (boost > 0) boost--;
        if (temperature > max) temperature = Math.max(max, temperature - 4);
        for (int i = 0; i < 3; i++) {
            ItemStack st = items.getStackInSlot(i);
            if (st.isEmpty()) continue;
            int h = Math.max(20, Heat.get(st));
            int target = temperature;
            int nh = h < target ? Math.min(target, h + 5) : Math.max(target, h - 3);
            if (nh != h) {
                ItemStack copy = st.copy();
                Heat.set(copy, nh);
                items.setStackInSlot(i, copy);
            }
        }
        if (burn > 0 && level.random.nextInt(4) == 0 && level instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(boost > 0 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                    worldPosition.getZ() + 0.5, 1, 0.2, 0.05, 0.2, 0.01);
        }
        setLit(burn > 0);
        setChanged();
    }

    @Override
    public int getExtra(int i) {
        return temperature;
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        lines.add(Component.translatable("gui.ls_metalwork.forge_temp", menu.extra(0)));
        for (int i = 0; i < 3; i++) {
            ItemStack st = menu.getSlot(i).getItem();
            if (!st.isEmpty()) lines.add(Component.literal((i + 1) + ": " + Heat.get(st) + " C"));
        }
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        tag.putInt("Temp", temperature);
        tag.putInt("Boost", boost);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        temperature = Math.max(20, tag.getInt("Temp"));
        boost = tag.getInt("Boost");
    }
}
