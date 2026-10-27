package com.lodestar.dynamos;

import com.lodestar.core.machine.MachineLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.ToLongFunction;

/** Dynamo that burns items: stirling (furnace fuel), lapidary (gems), redflux (redstone). */
public class ItemDynamoBE extends DynamoBE {
    public static final MachineLayout LAYOUT = MachineLayout.builder()
            .energy(156, 17, 52).slot(80, 53, MachineLayout.Role.FUEL).flame(81, 35).slot(130, 53, MachineLayout.Role.CHARGE)
            .slot(8, 17, MachineLayout.Role.UPGRADE).slot(8, 35, MachineLayout.Role.UPGRADE).slot(8, 53, MachineLayout.Role.UPGRADE)
            .info(30, 20).build();

    private final ToLongFunction<ItemStack> fuelValue;

    public ItemDynamoBE(BlockEntityType<?> type, BlockPos pos, BlockState state, int rate, ToLongFunction<ItemStack> fuelValue) {
        super(type, pos, state, LAYOUT, rate);
        this.fuelValue = fuelValue;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (layout.slots.get(slot).role == MachineLayout.Role.FUEL) return fuelValue.applyAsLong(stack) > 0;
        return super.isItemValid(slot, stack);
    }

    @Override
    protected long consumeFuel() {
        ItemStack st = items.getStackInSlot(0);
        long v = fuelValue.applyAsLong(st);
        if (v <= 0) return 0;
        ItemStack rem = st.getCraftingRemainingItem();
        items.extractItem(0, 1, false);
        if (!rem.isEmpty() && items.getStackInSlot(0).isEmpty()) items.setStackInSlot(0, rem);
        return v;
    }
}
