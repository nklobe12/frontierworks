package com.frontierworks.drills;

import com.frontierworks.core.energy.IEnergyItem;
import com.frontierworks.core.energy.ItemEnergy;
import com.frontierworks.core.util.AreaMining;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Energy-powered digger (drill = pickaxe + shovel, chainsaw = axe + leaves). Uses FE instead of durability.
 * Area-capable tools toggle 3x3 mode with sneak + right-click in the air.
 */
public class PoweredToolItem extends DiggerItem implements IEnergyItem {
    private final List<TagKey<Block>> tags;
    private final int capacity;
    private final int cost;
    private final boolean areaCapable;

    public PoweredToolItem(Tier tier, float damage, float speed, List<TagKey<Block>> tags, int capacity, int cost, boolean areaCapable, Properties props) {
        super(damage, speed, tier, tags.get(0), props.stacksTo(1));
        this.tags = tags;
        this.capacity = capacity;
        this.cost = cost;
        this.areaCapable = areaCapable;
    }

    public static List<TagKey<Block>> drillTags() {
        return java.util.Arrays.asList(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.MINEABLE_WITH_SHOVEL);
    }

    public static List<TagKey<Block>> sawTags() {
        return java.util.Arrays.asList(BlockTags.MINEABLE_WITH_AXE, BlockTags.LEAVES, BlockTags.WOOL);
    }

    private boolean inTags(BlockState state) {
        for (TagKey<Block> t : tags) if (state.is(t)) return true;
        return false;
    }

    private boolean area(ItemStack stack) {
        return areaCapable && stack.hasTag() && stack.getTag().getBoolean("Area");
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!ItemEnergy.has(stack, cost)) return 0.5f;
        if (!inTags(state)) return 1.0f;
        return getTier().getSpeed() * (area(stack) ? 0.5f : 1.0f);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return inTags(state) && TierSortingRegistry.isCorrectTierForDrops(getTier(), state);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        if (level.isClientSide) return true;
        if (state.getDestroySpeed(level, pos) != 0) ItemEnergy.use(stack, Math.min(cost, ItemEnergy.get(stack)));
        if (area(stack) && entity instanceof ServerPlayer && !entity.isShiftKeyDown() && !AreaMining.isBusy() && inTags(state)) {
            ServerPlayer player = (ServerPlayer) entity;
            float maxHardness = state.getDestroySpeed(level, pos) * 2.0f + 1.5f;
            AreaMining.breakAll(player, AreaMining.plane(pos, AreaMining.face(player), 1),
                    st -> inTags(st) && ItemEnergy.has(stack, cost), maxHardness);
        }
        return true;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        ItemEnergy.use(stack, Math.min(cost, ItemEnergy.get(stack)));
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (areaCapable && player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                CompoundTag tag = stack.getOrCreateTag();
                tag.putBoolean("Area", !tag.getBoolean("Area"));
                player.displayClientMessage(Component.translatable(tag.getBoolean("Area") ? "msg.fw_drills.area_on" : "msg.fw_drills.area_off"), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    @Override
    public int getEnergyCapacity(ItemStack stack) {
        return capacity;
    }

    @Override
    public int getEnergyTransfer(ItemStack stack) {
        return Math.max(1000, capacity / 50);
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return ItemEnergy.provider(this, stack);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return ItemEnergy.barWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return ItemEnergy.barColor(stack);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 14;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ItemEnergy.tooltip(stack, tooltip);
        tooltip.add(Component.translatable("tooltip.fw_drills.cost", cost).withStyle(ChatFormatting.GRAY));
        if (areaCapable) {
            tooltip.add(Component.translatable(area(stack) ? "tooltip.fw_drills.area" : "tooltip.fw_drills.single").withStyle(ChatFormatting.AQUA));
        }
    }
}
