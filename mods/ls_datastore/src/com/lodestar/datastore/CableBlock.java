package com.lodestar.datastore;

import com.lodestar.core.block.PipeBlockBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/** Data cable: connects to every datastore device next to it (ports only from behind or the sides). */
public class CableBlock extends PipeBlockBase {
    public CableBlock(Properties props, Supplier<? extends BlockEntityType<?>> type) {
        super(props, 3, type);
    }

    @Override
    public boolean canConnect(BlockGetter level, BlockPos pos, Direction dir) {
        return level.getBlockEntity(pos.relative(dir)) instanceof DataNodeBE n && n.connectsOn(dir.getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState ns = state.setValue(PROPS.get(dir), canConnect(level, pos, dir));
        if (ns != state && level.getBlockEntity(pos) instanceof CableBE be) be.invalidateGrid();
        return ns;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        BlockState ns = connections(level, pos, state);
        if (ns != state) {
            level.setBlock(pos, ns, 3);
            if (level.getBlockEntity(pos) instanceof CableBE be) be.invalidateGrid();
        }
    }
}
