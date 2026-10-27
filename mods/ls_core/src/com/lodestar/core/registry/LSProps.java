package com.lodestar.core.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/** Block property presets used by generated registration code. */
public final class LSProps {
    private LSProps() {}

    public static BlockBehaviour.Properties stone(float hardness, float resistance) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
                .strength(hardness, resistance).requiresCorrectToolForDrops();
    }

    public static BlockBehaviour.Properties stone(float hardness) {
        return stone(hardness, 6.0f);
    }

    public static BlockBehaviour.Properties ore(float hardness) {
        return stone(hardness, 3.0f);
    }

    public static BlockBehaviour.Properties deepslateOre(float hardness) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM)
                .strength(hardness + 1.5f, 3.0f).sound(SoundType.DEEPSLATE).requiresCorrectToolForDrops();
    }

    public static BlockBehaviour.Properties metal(float hardness) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                .strength(hardness, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    public static BlockBehaviour.Properties gemBlock(float hardness) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).strength(hardness, 6.0f)
                .sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    public static BlockBehaviour.Properties machine() {
        return metal(3.5f).lightLevel(s -> s.hasProperty(BlockStateProperties.LIT) && s.getValue(BlockStateProperties.LIT) ? 9 : 0);
    }

    public static BlockBehaviour.Properties machineTransparent() {
        return machine().noOcclusion();
    }

    public static BlockBehaviour.Properties wood() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS)
                .strength(2.0f, 3.0f).sound(SoundType.WOOD).ignitedByLava();
    }

    public static BlockBehaviour.Properties leaves() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.2f).randomTicks().sound(SoundType.GRASS)
                .noOcclusion().isValidSpawn(LSProps::ocelotOrParrot).isSuffocating(LSProps::never)
                .isViewBlocking(LSProps::never).ignitedByLava().pushReaction(PushReaction.DESTROY)
                .isRedstoneConductor(LSProps::never);
    }

    public static BlockBehaviour.Properties plant() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().instabreak()
                .sound(SoundType.GRASS).pushReaction(PushReaction.DESTROY);
    }

    public static BlockBehaviour.Properties sapling() {
        return plant().randomTicks();
    }

    public static BlockBehaviour.Properties crop() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().randomTicks().instabreak()
                .sound(SoundType.CROP).pushReaction(PushReaction.DESTROY);
    }

    public static BlockBehaviour.Properties glass() {
        return BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.HAT).strength(0.3f).sound(SoundType.GLASS)
                .noOcclusion().isValidSpawn(LSProps::neverSpawn).isRedstoneConductor(LSProps::never)
                .isSuffocating(LSProps::never).isViewBlocking(LSProps::never);
    }

    public static BlockBehaviour.Properties sand(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).instrument(NoteBlockInstrument.SNARE).strength(0.5f).sound(SoundType.SAND);
    }

    public static BlockBehaviour.Properties dirt(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(0.5f).sound(SoundType.GRAVEL);
    }

    public static BlockBehaviour.Properties crystal(int light) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.5f).sound(SoundType.AMETHYST)
                .lightLevel(s -> light).requiresCorrectToolForDrops();
    }

    public static BlockBehaviour.Properties lamp() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(0.3f).sound(SoundType.GLASS)
                .lightLevel(s -> s.hasProperty(BlockStateProperties.LIT) && !s.getValue(BlockStateProperties.LIT) ? 0 : 15);
    }

    public static boolean never(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    public static boolean ocelotOrParrot(BlockState state, BlockGetter level, BlockPos pos, EntityType<?> type) {
        return type == EntityType.OCELOT || type == EntityType.PARROT;
    }

    public static boolean neverSpawn(BlockState state, BlockGetter level, BlockPos pos, EntityType<?> type) {
        return false;
    }
}
