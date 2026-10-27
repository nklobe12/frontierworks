package com.frontierworks.conveyors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/** Conveyor belt: moves entities and items along its facing; delivers items into an inventory at the end. */
public class ConveyorBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 3, 16);
    private final double speed;

    public ConveyorBlock(Properties props, double speed) {
        super(props);
        this.speed = speed;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos np) {
        return dir == Direction.DOWN && !canSurvive(state, level, pos) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, dir, neighbor, level, pos, np);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        move(level, pos, state, entity);
        super.stepOn(level, pos, state, entity);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        move(level, pos, state, entity);
    }

    private void move(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (entity instanceof Player && ((Player) entity).isShiftKeyDown()) return;
        Direction f = state.getValue(FACING);
        Vec3 v = entity.getDeltaMovement();
        // centre the entity on the belt's cross axis
        double cx = pos.getX() + 0.5 - entity.getX();
        double cz = pos.getZ() + 0.5 - entity.getZ();
        double nx = f.getStepX() != 0 ? f.getStepX() * speed : cx * 0.2;
        double nz = f.getStepZ() != 0 ? f.getStepZ() * speed : cz * 0.2;
        entity.setDeltaMovement(nx, v.y, nz);
        if (entity instanceof ItemEntity && !level.isClientSide) {
            ItemEntity ie = (ItemEntity) entity;
            ie.setExtendedLifetime();
            double along = f.getStepX() != 0 ? (entity.getX() - pos.getX()) : (entity.getZ() - pos.getZ());
            boolean atEnd = (f.getStepX() + f.getStepZ()) > 0 ? along > 0.8 : along < 0.2;
            if (atEnd) {
                BlockPos target = pos.relative(f);
                BlockEntity be = level.getBlockEntity(target);
                if (be != null) {
                    IItemHandler h = be.getCapability(ForgeCapabilities.ITEM_HANDLER, f.getOpposite()).orElse(null);
                    if (h != null) {
                        ItemStack rem = ItemHandlerHelper.insertItem(h, ie.getItem().copy(), false);
                        if (rem.isEmpty()) ie.discard();
                        else ie.setItem(rem);
                    }
                }
            }
        }
    }
}
