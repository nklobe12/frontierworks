package com.lodestar.metalwork;

import com.lodestar.core.block.LSBlockEntity;
import com.lodestar.core.block.LSEntityBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * Smithing anvil. Place a hot workpiece on it, sneak-use with an empty hand to pick what to forge, then strike it
 * with a smithing hammer while it glows. Ideal heat counts double; too cold and the strike does nothing.
 * Every strike cools the metal, so long jobs need trips back to the forge.
 */
public class AnvilBE extends LSBlockEntity implements LSEntityBlock.IInteractable, LSEntityBlock.IDropContents {
    private ItemStack work = ItemStack.EMPTY;
    private String shape = "";
    /** progress in half-strikes */
    private int progress;
    private int required;

    public AnvilBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ItemStack work() {
        return work;
    }

    public String shape() {
        return shape;
    }

    public int progress() {
        return progress;
    }

    public int required() {
        return required;
    }

    @Override
    public void tickServer() {
        if (work.isEmpty() || level == null) return;
        if (level.getGameTime() % 10 == 0) {
            int h = Heat.get(work);
            if (h > 0) {
                Heat.set(work, h - 12);
                sync();
            }
        }
    }

    @Override
    public void tickClient() {
        if (work.isEmpty() || level == null) return;
        int h = Heat.get(work);
        if (h > 700 && level.random.nextInt(12) == 0) {
            level.addParticle(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.05, worldPosition.getZ() + 0.5, 0, 0.02, 0);
        }
    }

    @Override
    public InteractionResult onUse(Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        Level lvl = player.level();
        if (work.isEmpty()) {
            if (held.isEmpty() || held.getItem() instanceof HammerItem) return InteractionResult.PASS;
            if (lvl.isClientSide) return InteractionResult.SUCCESS;
            List<SmithingRecipe> options = SmithingRecipe.forItem(lvl, held);
            if (options.isEmpty()) {
                player.displayClientMessage(Component.translatable("msg.ls_metalwork.not_workable").withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.CONSUME;
            }
            work = held.split(1);
            selectShape(options.get(0));
            lvl.playSound(null, worldPosition, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.3f, 1.6f);
            sync();
            return InteractionResult.CONSUME;
        }
        if (held.getItem() == Content.SMITHING_HAMMER.get()) {
            if (!lvl.isClientSide) strike(player, held);
            return InteractionResult.sidedSuccess(lvl.isClientSide);
        }
        if (held.isEmpty()) {
            if (lvl.isClientSide) return InteractionResult.SUCCESS;
            if (player.isShiftKeyDown()) {
                List<SmithingRecipe> options = SmithingRecipe.forItem(lvl, work);
                if (!options.isEmpty()) {
                    int i = 0;
                    for (int k = 0; k < options.size(); k++) if (options.get(k).shape.equals(shape)) i = k;
                    selectShape(options.get((i + 1) % options.size()));
                    progress = 0;
                    player.displayClientMessage(Component.translatable("msg.ls_metalwork.shape", shapeName(shape)), true);
                    sync();
                }
            } else {
                giveBack(player);
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    private void selectShape(SmithingRecipe r) {
        shape = r.shape;
        required = r.strikes * 2;
    }

    public static Component shapeName(String shape) {
        return Component.translatable("shape.ls_metalwork." + shape);
    }

    private void giveBack(Player player) {
        if (!player.addItem(work)) player.drop(work, false);
        work = ItemStack.EMPTY;
        progress = 0;
        shape = "";
        sync();
    }

    private void strike(Player player, ItemStack hammer) {
        if (player.getCooldowns().isOnCooldown(hammer.getItem())) return;
        player.getCooldowns().addCooldown(hammer.getItem(), 8);
        SmithingRecipe r = SmithingRecipe.find(level, work, shape);
        if (r == null) {
            List<SmithingRecipe> options = SmithingRecipe.forItem(level, work);
            if (options.isEmpty()) {
                giveBack(player);
                return;
            }
            selectShape(options.get(0));
            r = options.get(0);
        }
        required = r.strikes * 2;
        int heat = Heat.get(work);
        if (heat < r.minHeat) {
            level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.25f, 2.0f);
            player.displayClientMessage(Component.translatable("msg.ls_metalwork.too_cold", r.minHeat).withStyle(ChatFormatting.AQUA), true);
            return;
        }
        boolean ideal = heat >= r.minHeat + 300;
        progress += ideal ? 2 : 1;
        Heat.set(work, heat - 45);
        hammer.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
        level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5f, ideal ? 1.3f : 0.9f);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LAVA, worldPosition.getX() + 0.5, worldPosition.getY() + 1.05, worldPosition.getZ() + 0.5, ideal ? 4 : 2, 0.15, 0.05, 0.15, 0.0);
            sl.sendParticles(ParticleTypes.FLAME, worldPosition.getX() + 0.5, worldPosition.getY() + 1.05, worldPosition.getZ() + 0.5, 3, 0.2, 0.05, 0.2, 0.03);
        }
        if (progress >= required) {
            ItemStack out = r.result.copy();
            Heat.set(out, Heat.get(work));
            work = ItemStack.EMPTY;
            progress = 0;
            shape = "";
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, out);
            level.playSound(null, worldPosition, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8f, 1.0f);
        }
        sync();
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        if (!work.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, work);
    }

    @Override
    protected void saveAll(CompoundTag tag) {
        if (!work.isEmpty()) tag.put("Work", work.save(new CompoundTag()));
        tag.putString("Shape", shape);
        tag.putInt("Progress", progress);
        tag.putInt("Required", required);
    }

    @Override
    protected void loadAll(CompoundTag tag) {
        work = tag.contains("Work") ? ItemStack.of(tag.getCompound("Work")) : ItemStack.EMPTY;
        shape = tag.getString("Shape");
        progress = tag.getInt("Progress");
        required = tag.getInt("Required");
    }
}
