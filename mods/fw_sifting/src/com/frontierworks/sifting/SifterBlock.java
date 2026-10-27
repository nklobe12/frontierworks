package com.frontierworks.sifting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

/** Hand sifter: right-click with gravel, sand, dirt or soul sand to sift it for nuggets, gems and seeds. */
public class SifterBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 10, 16);
    public static final Map<String, String> TABLES = new HashMap<>();

    static {
        TABLES.put("minecraft:gravel", "fw_sifting:sifting/gravel");
        TABLES.put("minecraft:sand", "fw_sifting:sifting/sand");
        TABLES.put("minecraft:red_sand", "fw_sifting:sifting/sand");
        TABLES.put("minecraft:dirt", "fw_sifting:sifting/dirt");
        TABLES.put("minecraft:soul_sand", "fw_sifting:sifting/soul_sand");
        TABLES.put("minecraft:soul_soil", "fw_sifting:sifting/soul_sand");
    }

    public SifterBlock(Properties props) {
        super(props);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(held.getItem());
        if (held.isEmpty() || id == null || !TABLES.containsKey(id.toString())) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel) {
            ServerLevel sl = (ServerLevel) level;
            LootTable table = sl.getServer().getLootData().getLootTable(new ResourceLocation(TABLES.get(id.toString())));
            LootParams params = new LootParams.Builder(sl).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                    .withLuck(player.getLuck()).create(LootContextParamSets.CHEST);
            for (ItemStack out : table.getRandomItems(params)) {
                ItemEntity e = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, out);
                e.setDeltaMovement(level.random.nextGaussian() * 0.05, 0.2, level.random.nextGaussian() * 0.05);
                level.addFreshEntity(e);
            }
            if (held.getItem() instanceof BlockItem) {
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ((BlockItem) held.getItem()).getBlock().defaultBlockState()),
                        pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 12, 0.25, 0.1, 0.25, 0.05);
            }
            level.playSound(null, pos, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 0.8f, 0.9f + level.random.nextFloat() * 0.2f);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
