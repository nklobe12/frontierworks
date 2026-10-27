package com.frontierworks.dungeons;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Floor spikes: walkable (1px collision) but hurt and slow anything living that crosses them. */
public class SpikeBlock extends Block {
    private static final VoxelShape COLLISION = Block.box(0, 0, 0, 16, 1, 16);
    private static final VoxelShape OUTLINE = Block.box(0, 0, 0, 16, 7, 16);

    public SpikeBlock(Properties props) {
        super(props);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return OUTLINE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return COLLISION;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity instanceof ItemEntity || !(entity instanceof LivingEntity)) return;
        entity.makeStuckInBlock(state, new net.minecraft.world.phys.Vec3(0.8, 0.9, 0.8));
        if (!level.isClientSide && entity.tickCount % 10 == 0) {
            entity.hurt(level.damageSources().cactus(), 2.0f);
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float distance) {
        entity.causeFallDamage(distance + 2.0f, 1.5f, level.damageSources().fall());
    }
}
