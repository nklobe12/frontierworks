package com.lodestar.transmutation;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The transmuter's stone. Right-click a block to turn it into the next block of its family (stone into
 * cobblestone into andesite..., one log into another); sneak to change a 3x3 patch. It also catalyses the
 * crafting transmutations (eight iron into a gold, four coal into kindled coal) and is never used up.
 */
public class TransmuterStoneItem extends Item {
    public static final Block[][] CYCLES = {
            {Blocks.STONE, Blocks.COBBLESTONE, Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRANITE, Blocks.TUFF},
            {Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.SAND, Blocks.GRAVEL, Blocks.CLAY},
            {Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, Blocks.JUNGLE_LOG, Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.CHERRY_LOG, Blocks.MANGROVE_LOG},
            {Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.CHERRY_LEAVES},
            {Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS, Blocks.JUNGLE_PLANKS, Blocks.ACACIA_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.CHERRY_PLANKS},
            {Blocks.SANDSTONE, Blocks.RED_SANDSTONE}, {Blocks.SAND, Blocks.RED_SAND},
            {Blocks.NETHERRACK, Blocks.SOUL_SAND, Blocks.SOUL_SOIL, Blocks.BLACKSTONE},
            {Blocks.ICE, Blocks.PACKED_ICE}, {Blocks.MELON, Blocks.PUMPKIN},
            {Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE, Blocks.TUFF, Blocks.CALCITE},
            {Blocks.WHITE_WOOL, Blocks.ORANGE_WOOL, Blocks.MAGENTA_WOOL, Blocks.LIGHT_BLUE_WOOL, Blocks.YELLOW_WOOL, Blocks.LIME_WOOL, Blocks.PINK_WOOL,
                    Blocks.GRAY_WOOL, Blocks.LIGHT_GRAY_WOOL, Blocks.CYAN_WOOL, Blocks.PURPLE_WOOL, Blocks.BLUE_WOOL, Blocks.BROWN_WOOL, Blocks.GREEN_WOOL,
                    Blocks.RED_WOOL, Blocks.BLACK_WOOL}};

    public TransmuterStoneItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Nullable
    public static Block next(Block b, boolean back) {
        for (Block[] cycle : CYCLES) {
            for (int i = 0; i < cycle.length; i++) {
                if (cycle[i] == b) return cycle[(i + (back ? cycle.length - 1 : 1)) % cycle.length];
            }
        }
        return null;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player p = ctx.getPlayer();
        Block to = next(state.getBlock(), false);
        if (to == null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        boolean area = p != null && p.isShiftKeyDown();
        Direction face = ctx.getClickedFace();
        int n = 0;
        for (int a = area ? -1 : 0; a <= (area ? 1 : 0); a++)
            for (int b = area ? -1 : 0; b <= (area ? 1 : 0); b++) {
                BlockPos q = switch (face.getAxis()) {
                    case X -> pos.offset(0, a, b);
                    case Y -> pos.offset(a, 0, b);
                    default -> pos.offset(a, b, 0);
                };
                if (level.getBlockState(q).getBlock() == state.getBlock()) {
                    level.setBlock(q, to.defaultBlockState(), 3);
                    n++;
                    if (level instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.WITCH, q.getX() + 0.5, q.getY() + 0.5, q.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.0);
                }
            }
        if (n > 0) level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 0.7f + level.random.nextFloat() * 0.3f);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return stack.copyWithCount(1);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        for (String s : Component.translatable("tooltip.ls_transmutation.stone").getString().split("\n")) tip.add(Component.literal(s).withStyle(ChatFormatting.GRAY));
    }
}
