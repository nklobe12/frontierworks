package com.lodestar.transmutation;

import com.lodestar.core.machine.MachineLayout;
import com.lodestar.core.machine.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Supplier;

/**
 * Aether collector: gathers aether from light, faster in brighter places, and passes it to relays and condensers
 * beside it. Put fuel in its upgrade slot and it spends aether to refine it up the chain: charcoal, coal,
 * redstone, glowstone, kindled coal, quickfire, eternal ember.
 */
public class CollectorBE extends AetherMachineBE {
    public static final MachineLayout LAYOUT = MachineLayout.builder()
            .slot(44, 35, MachineLayout.Role.INPUT).arrow(70, 34).slot(104, 35, MachineLayout.Role.OUTPUT).info(8, 60).extra(3).build();
    @SuppressWarnings("unchecked")
    private static final Supplier<Item>[] CHAIN = new Supplier[]{() -> Items.CHARCOAL, () -> Items.COAL, () -> Items.REDSTONE,
            () -> Items.GLOWSTONE_DUST, () -> Content.KINDLED_COAL.get(), () -> Content.QUICKFIRE.get(), () -> Content.ETERNAL_EMBER.get()};

    private final int perSecond;
    private int made;

    public CollectorBE(BlockEntityType<?> type, BlockPos pos, BlockState state, int perSecond) {
        super(type, pos, state, LAYOUT, perSecond * 2000L, perSecond * 4L);
        this.perSecond = perSecond;
    }

    @Override
    public boolean providesAether() {
        return true;
    }

    @Override
    public long receiveAether(long amount, boolean simulate) {
        return 0;
    }

    private static int chainIndex(ItemStack st) {
        for (int i = 0; i < CHAIN.length; i++) if (st.is(CHAIN[i].get())) return i;
        return -1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (layout.slots.get(slot).role == MachineLayout.Role.INPUT) {
            int i = chainIndex(stack);
            return i >= 0 && i < CHAIN.length - 1;
        }
        return super.isItemValid(slot, stack);
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        if (level.getGameTime() % 20 == 0) {
            int light = level.getMaxLocalRawBrightness(worldPosition.above());
            made = Math.round(perSecond * light / 15f);
            aether = Math.min(capacity, aether + made);
            setLit(made > 0);
        }
        ItemStack in = items.getStackInSlot(0);
        int idx = chainIndex(in);
        if (idx >= 0 && idx < CHAIN.length - 1) {
            ItemStack next = new ItemStack(CHAIN[idx + 1].get());
            long cost = Math.max(1, AetherValues.get(next) - AetherValues.get(in.copyWithCount(1)));
            maxProgress = (int) Math.min(Integer.MAX_VALUE, cost);
            progress = (int) Math.min(aether, cost);
            if (aether >= cost && insertOutput(next, true).isEmpty()) {
                aether -= cost;
                items.extractItem(0, 1, false);
                insertOutput(next, false);
                progress = 0;
            }
        } else {
            progress = 0;
            pushAether(true);
        }
        setChanged();
    }

    @Override
    public int getExtra(int i) {
        return i == 2 ? made : super.getExtra(i);
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        lines.add(Component.translatable("gui.ls_transmutation.stored", AetherValues.format(menu.extra(0)), AetherValues.format(menu.extra(1))));
        lines.add(Component.translatable("gui.ls_transmutation.collecting", menu.extra(2)));
    }
}
