package com.frontierworks.storage;

import com.frontierworks.core.machine.MachineBlockEntity;
import com.frontierworks.core.machine.MachineLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Storage crate: rows of nine slots that keep their contents when the crate is broken. */
public class CrateBE extends MachineBlockEntity {
    public CrateBE(BlockEntityType<?> type, BlockPos pos, BlockState state, int rows) {
        super(type, pos, state, layout(rows), 0, 0, 0);
    }

    public static MachineLayout layout(int rows) {
        return MachineLayout.builder().grid(8, 18, 9, rows, MachineLayout.Role.STORAGE).height(114 + rows * 18).build();
    }

    @Override
    public void serverTick() {}

    @Override
    public void dropContents() {
        // contents travel with the crate item (see loot table copy_nbt)
    }
}
