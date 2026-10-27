package com.lodestar.transmutation;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Transmutation rings, powered by the aether cells you carry. Use to switch a ring on or off (or to use its
 * ability); active rings work from anywhere in your inventory.
 */
public class RingItem extends Item {
    public enum Kind { GALE, EMBER, FROST, VERDANT, VOID }

    public final Kind kind;

    public RingItem(Properties props, Kind kind) {
        super(props.stacksTo(1));
        this.kind = kind;
    }

    public static boolean active(ItemStack st) {
        return st.hasTag() && st.getTag().getBoolean("Active");
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return active(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        ItemStack st = p.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(st);
        switch (kind) {
            case EMBER -> {
                if (AetherCellItem.consume(p, 32)) {
                    Vec3 look = p.getLookAngle();
                    SmallFireball fb = new SmallFireball(level, p, look.x, look.y, look.z);
                    fb.setPos(p.getX() + look.x, p.getEyeY() + look.y * 0.5, p.getZ() + look.z);
                    level.addFreshEntity(fb);
                    level.playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8f, 1.2f);
                } else noAether(p);
            }
            case FROST -> {
                int n = 0;
                BlockPos c = p.blockPosition();
                for (BlockPos q : BlockPos.betweenClosed(c.offset(-4, -2, -4), c.offset(4, 1, 4))) {
                    BlockState s = level.getBlockState(q);
                    if (s.is(Blocks.WATER) && s.getFluidState().isSource()) {
                        level.setBlock(q, Blocks.ICE.defaultBlockState(), 3);
                        n++;
                    } else if (s.is(Blocks.LAVA) && s.getFluidState().isSource()) {
                        level.setBlock(q, Blocks.OBSIDIAN.defaultBlockState(), 3);
                        n++;
                    }
                }
                if (n > 0 && AetherCellItem.consume(p, n * 4L)) level.playSound(null, c, SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1f, 0.6f);
            }
            case VOID -> {
                if (p.isShiftKeyDown()) teleport(p);
                else toggle(p, st);
            }
            default -> toggle(p, st);
        }
        return InteractionResultHolder.success(st);
    }

    private static void noAether(Player p) {
        p.displayClientMessage(Component.translatable("msg.ls_transmutation.no_aether").withStyle(ChatFormatting.RED), true);
    }

    private static void toggle(Player p, ItemStack st) {
        boolean on = !active(st);
        st.getOrCreateTag().putBoolean("Active", on);
        p.displayClientMessage(Component.translatable(on ? "msg.ls_transmutation.ring_on" : "msg.ls_transmutation.ring_off", st.getHoverName()), true);
        if (!on && st.getItem() instanceof RingItem r && r.kind == Kind.GALE) stopFlight(p);
    }

    private static void teleport(Player p) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(24));
        BlockHitResult hit = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 to = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation().subtract(p.getLookAngle().scale(0.6)) : end;
        if (!AetherCellItem.consume(p, 64)) {
            noAether(p);
            return;
        }
        if (p instanceof ServerPlayer sp) sp.teleportTo(to.x, to.y, to.z);
        p.fallDistance = 0;
        p.level().playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    private static void stopFlight(Player p) {
        if (!p.isCreative() && !p.isSpectator() && p.getPersistentData().getBoolean("ls_gale")) {
            p.getAbilities().mayfly = false;
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
        }
        p.getPersistentData().remove("ls_gale");
    }

    @Override
    public void inventoryTick(ItemStack st, Level level, Entity holder, int slot, boolean selected) {
        if (level.isClientSide || !(holder instanceof Player p)) return;
        boolean second = level.getGameTime() % 20 == 0;
        switch (kind) {
            case EMBER -> {
                if (second) p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, true, false));
            }
            case FROST -> p.setTicksFrozen(0);
            case GALE -> {
                if (!active(st)) return;
                if (!p.isCreative() && !p.getAbilities().mayfly) {
                    p.getAbilities().mayfly = true;
                    p.onUpdateAbilities();
                    p.getPersistentData().putBoolean("ls_gale", true);
                }
                p.fallDistance = 0;
                if (second && p.getAbilities().flying && !AetherCellItem.consume(p, 16)) {
                    st.getOrCreateTag().putBoolean("Active", false);
                    stopFlight(p);
                    noAether(p);
                }
            }
            case VERDANT -> {
                if (!active(st) || !second) return;
                if (!AetherCellItem.consume(p, 8)) {
                    st.getOrCreateTag().putBoolean("Active", false);
                    return;
                }
                BlockPos c = p.blockPosition();
                for (int i = 0; i < 6; i++) {
                    BlockPos q = c.offset(level.random.nextInt(11) - 5, level.random.nextInt(3) - 1, level.random.nextInt(11) - 5);
                    BlockState s = level.getBlockState(q);
                    if ((s.getBlock() instanceof CropBlock || s.getBlock() instanceof net.minecraft.world.level.block.SaplingBlock)
                            && s.getBlock() instanceof BonemealableBlock bm && bm.isValidBonemealTarget(level, q, s, false) && level instanceof ServerLevel sl) {
                        bm.performBonemeal(sl, level.random, q, s);
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, q.getX() + 0.5, q.getY() + 0.5, q.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0);
                    }
                }
            }
            case VOID -> {
                if (!active(st)) return;
                for (ItemEntity it : level.getEntitiesOfClass(ItemEntity.class, new AABB(p.blockPosition()).inflate(8))) {
                    if (!it.hasPickUpDelay()) it.setPos(p.getX(), p.getY(), p.getZ());
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        for (String s : Component.translatable("tooltip.ls_transmutation.ring." + kind.name().toLowerCase(java.util.Locale.ROOT)).getString().split("\n")) {
            tip.add(Component.literal(s).withStyle(ChatFormatting.GRAY));
        }
        if (active(stack)) tip.add(Component.translatable("tooltip.ls_transmutation.active").withStyle(ChatFormatting.GREEN));
    }
}
