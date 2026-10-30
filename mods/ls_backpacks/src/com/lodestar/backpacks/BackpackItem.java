package com.lodestar.backpacks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * A backpack. Use it to open; sneak-use on the ground to set it down as a block; wear it in the chest slot and press
 * the backpack key. Contents live server side under the id stored in the stack, so they survive placing, picking up
 * and upgrading to the next tier.
 */
public class BackpackItem extends Item implements Equipable, DyeableLeatherItem {
    public static final String TAG_ID = "BackpackId";
    public final BackpackTier tier;

    public BackpackItem(Properties props, BackpackTier tier) {
        super(props.stacksTo(1));
        this.tier = tier;
    }

    public static UUID id(ItemStack stack) {
        if (!stack.getOrCreateTag().hasUUID(TAG_ID)) stack.getOrCreateTag().putUUID(TAG_ID, UUID.randomUUID());
        return stack.getOrCreateTag().getUUID(TAG_ID);
    }

    @Nullable
    public static UUID peekId(ItemStack stack) {
        return stack.hasTag() && stack.getTag().hasUUID(TAG_ID) ? stack.getTag().getUUID(TAG_ID) : null;
    }

    @Override
    public int getColor(ItemStack stack) {
        CompoundTagHelper h = new CompoundTagHelper(stack);
        return h.color(defaultColor());
    }

    public int defaultColor() {
        return switch (tier) {
            case LEATHER -> 0x8B5A2B;
            case IRON -> 0x9A9A9A;
            case GOLD -> 0xC9A63A;
            case DIAMOND -> 0x3FB8B0;
            case NETHERITE -> 0x4A3F44;
        };
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.CHEST;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            BackpackMenu.Source src = hand == InteractionHand.MAIN_HAND ? BackpackMenu.Source.hotbar(sp.getInventory().selected) : BackpackMenu.Source.offhand();
            BackpackMenu.open(sp, stack, src);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;
        Level level = ctx.getLevel();
        BlockPlaceContext pc = new BlockPlaceContext(ctx);
        if (!pc.canPlace()) return InteractionResult.FAIL;
        BlockPos pos = pc.getClickedPos();
        if (!level.isClientSide) {
            ItemStack stack = ctx.getItemInHand();
            BlockState st = Content.PLACED_BACKPACK.get().getStateForPlacement(pc);
            if (st == null) return InteractionResult.FAIL;
            id(stack);
            level.setBlock(pos, st.setValue(BackpackBlock.TIER, BackpackBlock.TierProp.of(tier)), 3);
            if (level.getBlockEntity(pos) instanceof BackpackBE be) be.fromItem(stack);
            level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_LEATHER, net.minecraft.sounds.SoundSource.BLOCKS, 0.8f, 0.9f);
            stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.ls_backpacks.size", tier.slots(), tier.upgrades).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("tooltip.ls_backpacks.use").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    /** Reads the dye colour like leather armour does. */
    private record CompoundTagHelper(ItemStack stack) {
        int color(int def) {
            net.minecraft.nbt.CompoundTag d = stack.getTagElement(TAG_DISPLAY);
            return d != null && d.contains(TAG_COLOR, 99) ? d.getInt(TAG_COLOR) : def;
        }
    }
}
