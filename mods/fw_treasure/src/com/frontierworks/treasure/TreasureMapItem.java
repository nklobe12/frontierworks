package com.frontierworks.treasure;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/**
 * An unread treasure map. Using it (in the overworld) searches for the nearest unexplored structure in
 * its structure tag and turns into a filled explorer map marking the spot.
 */
public class TreasureMapItem extends Item {
    private final TagKey<Structure> target;
    private final MapDecoration.Type marker;

    public TreasureMapItem(Properties props, String tag, MapDecoration.Type marker) {
        super(props);
        this.target = TagKey.create(Registries.STRUCTURE, new ResourceLocation(tag));
        this.marker = marker;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel)) return InteractionResultHolder.success(stack);
        ServerLevel sl = (ServerLevel) level;
        BlockPos found = sl.findNearestMapStructure(target, player.blockPosition(), 100, true);
        if (found == null) {
            player.displayClientMessage(Component.translatable("msg.fw_treasure.nothing").withStyle(ChatFormatting.GRAY), true);
            player.getCooldowns().addCooldown(this, 60);
            return InteractionResultHolder.fail(stack);
        }
        ItemStack map = MapItem.create(sl, found.getX(), found.getZ(), (byte) 2, true, true);
        MapItem.renderBiomePreviewMap(sl, map);
        MapItemSavedData.addTargetDecoration(map, found, "+", marker);
        map.setHoverName(Component.translatable(getDescriptionId() + ".filled"));
        level.playSound(null, player.blockPosition(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 1.0f, 1.0f);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        if (stack.isEmpty()) return InteractionResultHolder.success(map);
        if (!player.getInventory().add(map)) player.drop(map, false);
        return InteractionResultHolder.success(stack);
    }
}
