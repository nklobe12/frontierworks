package com.lodestar.kinetics;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A gear train: input shaft on the back, output shaft on the front turning at a chosen ratio (1/4 to 4 times) and
 * optionally reversed. Use it to change the ratio; sneak-use it to reverse the output.
 */
public class GearTrainBE extends KineticBE {
    public static final double[] RATIOS = {0.25, 0.5, 1, 2, 4};
    private static final String[] LABELS = {"1:4", "1:2", "1:1", "2:1", "4:1"};
    private int ratioIdx = 3;
    private boolean reversed;

    public GearTrainBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Output spin as a multiple of input spin. */
    public double outputFactor() {
        return RATIOS[ratioIdx] * (reversed ? -1 : 1);
    }

    @Override
    public double faceFactor(Direction d) {
        Direction f = facing();
        if (d == f) return outputFactor();
        if (d == f.getOpposite()) return 1;
        return 0;
    }

    public String ratioLabel() {
        return LABELS[ratioIdx];
    }

    public boolean reversed() {
        return reversed;
    }

    @Override
    public InteractionResult onUse(Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.getItemInHand(hand).isEmpty()) return InteractionResult.PASS;
        if (level == null) return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) reversed = !reversed;
            else ratioIdx = (ratioIdx + 1) % RATIOS.length;
            invalidateGrid();
            sync();
            level.playSound(null, worldPosition, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.6f, 1.4f);
            player.displayClientMessage(Component.translatable(reversed ? "msg.ls_kinetics.gear_train_rev" : "msg.ls_kinetics.gear_train",
                    ratioLabel()).withStyle(ChatFormatting.YELLOW), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void saveAll(CompoundTag tag) {
        tag.putInt("Ratio", ratioIdx);
        tag.putBoolean("Rev", reversed);
    }

    @Override
    protected void loadAll(CompoundTag tag) {
        ratioIdx = tag.contains("Ratio") ? Math.max(0, Math.min(RATIOS.length - 1, tag.getInt("Ratio"))) : 3;
        reversed = tag.getBoolean("Rev");
    }

    /** Test hook: set the ratio index (0..4) and direction. */
    public void configure(int idx, boolean rev) {
        ratioIdx = Math.max(0, Math.min(RATIOS.length - 1, idx));
        reversed = rev;
        invalidateGrid();
        sync();
    }
}
