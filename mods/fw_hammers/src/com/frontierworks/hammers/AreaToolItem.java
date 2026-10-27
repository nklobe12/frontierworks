package com.frontierworks.hammers;

import com.frontierworks.core.util.AreaMining;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/** Hammer (pickaxe) or excavator (shovel) that mines a 3x3 area. Sneak to mine a single block. */
public class AreaToolItem extends DiggerItem {
    private final boolean shovel;

    public AreaToolItem(Tier tier, boolean shovel, Properties props) {
        super(shovel ? 2.0f : 4.0f, shovel ? -3.2f : -3.4f, tier, shovel ? BlockTags.MINEABLE_WITH_SHOVEL : BlockTags.MINEABLE_WITH_PICKAXE,
                props.durability(tier.getUses() * 3));
        this.shovel = shovel;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return super.getDestroySpeed(stack, state) * 0.55f;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        boolean r = super.mineBlock(stack, level, state, pos, entity);
        if (!level.isClientSide && entity instanceof ServerPlayer && !entity.isShiftKeyDown() && !AreaMining.isBusy()
                && isCorrectToolForDrops(stack, state)) {
            ServerPlayer player = (ServerPlayer) entity;
            Direction face = AreaMining.face(player);
            float maxHardness = state.getDestroySpeed(level, pos) * 2.0f + 1.5f;
            AreaMining.breakAll(player, AreaMining.plane(pos, face, 1), st -> isCorrectToolForDrops(stack, st), maxHardness);
        }
        return r;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(shovel ? "tooltip.fw_hammers.excavator" : "tooltip.fw_hammers.hammer").withStyle(ChatFormatting.GRAY));
    }
}
