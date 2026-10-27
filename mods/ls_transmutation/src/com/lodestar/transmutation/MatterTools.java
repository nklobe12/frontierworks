package com.lodestar.transmutation;

import com.lodestar.core.util.AreaMining;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/**
 * Umbral and radiant matter tools: unbreakable, and with a charge level (press the charge key) that makes them
 * work a wider area, paying aether from your cells for each extra block.
 */
public final class MatterTools {
    private MatterTools() {}

    public record MatterTier(int level, float speed, float attack, int enchant, int maxCharge, long costPerBlock, Supplier<Ingredient> repair) implements Tier {
        @Override
        public int getUses() {
            return 0;
        }

        @Override
        public float getSpeed() {
            return speed;
        }

        @Override
        public float getAttackDamageBonus() {
            return attack;
        }

        @Override
        public int getLevel() {
            return level;
        }

        @Override
        public int getEnchantmentValue() {
            return enchant;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return repair.get();
        }
    }

    public static final MatterTier UMBRAL = new MatterTier(4, 14f, 5f, 15, 2, 8, () -> Ingredient.of(Content.UMBRAL_MATTER.get()));
    public static final MatterTier RADIANT = new MatterTier(4, 20f, 7f, 20, 3, 4, () -> Ingredient.of(Content.RADIANT_MATTER.get()));

    public interface Chargeable {
        int maxCharge();
    }

    public static int charge(ItemStack st) {
        return st.hasTag() ? st.getTag().getInt("Charge") : 0;
    }

    public static void cycleCharge(ServerPlayer p, ItemStack st) {
        if (!(st.getItem() instanceof Chargeable c)) return;
        int n = (charge(st) + 1) % (c.maxCharge() + 1);
        st.getOrCreateTag().putInt("Charge", n);
        p.displayClientMessage(Component.translatable("msg.ls_transmutation.charge", n, c.maxCharge()).withStyle(ChatFormatting.AQUA), true);
    }

    static void area(ItemStack stack, MatterTier tier, BlockState state, BlockPos pos, LivingEntity user, TagKey<Block> mineable) {
        if (!(user instanceof ServerPlayer sp) || AreaMining.isBusy() || sp.isShiftKeyDown()) return;
        int r = charge(stack);
        if (r <= 0 || !state.is(mineable)) return;
        Direction face = AreaMining.face(sp);
        List<BlockPos> list = AreaMining.plane(pos, face, r);
        long cost = tier.costPerBlock() * list.size();
        if (!AetherCellItem.consume(sp, cost)) {
            sp.displayClientMessage(Component.translatable("msg.ls_transmutation.no_aether").withStyle(ChatFormatting.RED), true);
            return;
        }
        AreaMining.breakAll(sp, list, s -> s.is(mineable), state.getDestroySpeed(sp.level(), pos) + 5f);
    }

    static void tip(ItemStack stack, int max, List<Component> tip) {
        tip.add(Component.translatable("tooltip.ls_transmutation.charge", charge(stack), max).withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatable("tooltip.ls_transmutation.charge_hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static class Pickaxe extends PickaxeItem implements Chargeable {
        private final MatterTier tier;

        public Pickaxe(MatterTier tier, int atk, float spd, Properties p) {
            super(tier, atk, spd, p.fireResistant());
            this.tier = tier;
        }

        @Override
        public int maxCharge() {
            return tier.maxCharge();
        }

        @Override
        public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity user) {
            if (!level.isClientSide) area(stack, tier, state, pos, user, BlockTags.MINEABLE_WITH_PICKAXE);
            return true;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
            MatterTools.tip(stack, maxCharge(), tip);
        }
    }

    public static class Shovel extends ShovelItem implements Chargeable {
        private final MatterTier tier;

        public Shovel(MatterTier tier, float atk, float spd, Properties p) {
            super(tier, atk, spd, p.fireResistant());
            this.tier = tier;
        }

        @Override
        public int maxCharge() {
            return tier.maxCharge();
        }

        @Override
        public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity user) {
            if (!level.isClientSide) area(stack, tier, state, pos, user, BlockTags.MINEABLE_WITH_SHOVEL);
            return true;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
            MatterTools.tip(stack, maxCharge(), tip);
        }
    }

    public static class Axe extends AxeItem implements Chargeable {
        private final MatterTier tier;

        public Axe(MatterTier tier, float atk, float spd, Properties p) {
            super(tier, atk, spd, p.fireResistant());
            this.tier = tier;
        }

        @Override
        public int maxCharge() {
            return tier.maxCharge();
        }

        @Override
        public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity user) {
            if (!level.isClientSide) area(stack, tier, state, pos, user, BlockTags.MINEABLE_WITH_AXE);
            return true;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
            MatterTools.tip(stack, maxCharge(), tip);
        }
    }

    public static class Sword extends SwordItem implements Chargeable {
        private final MatterTier tier;

        public Sword(MatterTier tier, int atk, float spd, Properties p) {
            super(tier, atk, spd, p.fireResistant());
            this.tier = tier;
        }

        @Override
        public int maxCharge() {
            return tier.maxCharge();
        }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            int r = charge(stack);
            if (r > 0 && attacker instanceof Player p && !p.level().isClientSide && AetherCellItem.consume(p, tier.costPerBlock() * 4L * r)) {
                for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(1.5 * r),
                        e -> e != p && e != target && e.isAlive() && !(e instanceof Player))) {
                    e.hurt(p.damageSources().playerAttack(p), getDamage() * 0.6f);
                }
            }
            return true;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
            MatterTools.tip(stack, maxCharge(), tip);
        }
    }
}
