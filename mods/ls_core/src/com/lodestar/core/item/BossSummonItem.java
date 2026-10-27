package com.lodestar.core.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.function.Predicate;
import java.util.function.Supplier;

/** Right-click the ground to summon a boss (after checking a condition such as night-time or altitude). */
public class BossSummonItem extends Item {
    private final Supplier<? extends EntityType<? extends Mob>> boss;
    private final Predicate<UseOnContext> condition;
    private final String failKey;

    public BossSummonItem(Properties props, Supplier<? extends EntityType<? extends Mob>> boss, Predicate<UseOnContext> condition, String failKey) {
        super(props);
        this.boss = boss;
        this.condition = condition;
        this.failKey = failKey;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!condition.test(ctx)) {
            if (player != null) player.displayClientMessage(Component.translatable(failKey), true);
            return InteractionResult.FAIL;
        }
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace()).above(2);
        Mob mob = boss.get().create(level);
        if (mob == null) return InteractionResult.FAIL;
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360f, 0f);
        if (level instanceof ServerLevel sl) {
            mob.finalizeSpawn(sl, sl.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(pos.getX() + 0.5, pos.getY() - 2, pos.getZ() + 0.5);
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
        }
        level.addFreshEntity(mob);
        level.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0f, 1.0f);
        if (player == null || !player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
