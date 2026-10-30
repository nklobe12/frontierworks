package com.lodestar.datastore;

import com.lodestar.datastore.menu.TerminalMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;

/** A hand-held access terminal linked to one core; works within {@link #RANGE} blocks of it in the same dimension. */
public class RemoteTerminalItem extends Item {
    public static final int RANGE = 64;

    public RemoteTerminalItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player p = ctx.getPlayer();
        if (p == null || !p.isShiftKeyDown() || !(ctx.getLevel().getBlockEntity(ctx.getClickedPos()) instanceof CoreBE)) return InteractionResult.PASS;
        if (!ctx.getLevel().isClientSide) {
            CompoundTag t = ctx.getItemInHand().getOrCreateTag();
            t.put("Core", NbtUtils.writeBlockPos(ctx.getClickedPos()));
            t.putString("Dim", ctx.getLevel().dimension().location().toString());
            p.displayClientMessage(Component.translatable("msg.ls_datastore.linked"), true);
        }
        return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
    }

    @Nullable
    public static BlockPos core(ItemStack s) {
        CompoundTag t = s.getTag();
        return t != null && t.contains("Core") ? NbtUtils.readBlockPos(t.getCompound("Core")) : null;
    }

    public static boolean sameDim(ItemStack s, Level level) {
        CompoundTag t = s.getTag();
        return t != null && level.dimension().location().toString().equals(t.getString("Dim"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.sidedSuccess(s, level.isClientSide);
        BlockPos core = core(s);
        if (core == null) {
            player.displayClientMessage(Component.translatable("msg.ls_datastore.unlinked"), true);
            return InteractionResultHolder.fail(s);
        }
        if (!sameDim(s, level) || player.distanceToSqr(core.getX() + 0.5, core.getY() + 0.5, core.getZ() + 0.5) > RANGE * RANGE) {
            player.displayClientMessage(Component.translatable("msg.ls_datastore.range"), true);
            return InteractionResultHolder.fail(s);
        }
        if (!level.isLoaded(core) || !(level.getBlockEntity(core) instanceof CoreBE)) {
            player.displayClientMessage(Component.translatable("msg.ls_datastore.no_core"), true);
            return InteractionResultHolder.fail(s);
        }
        NetworkHooks.openScreen(sp, new SimpleMenuProvider((id, inv, p) -> TerminalMenu.remote(id, inv, core), s.getHoverName()),
                buf -> TerminalMenu.writeOpen(buf, false, true));
        return InteractionResultHolder.consume(s);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        BlockPos p = core(stack);
        if (p == null) tip.add(Component.translatable("tooltip.ls_datastore.remote.unlinked").withStyle(ChatFormatting.DARK_GRAY));
        else tip.add(Component.translatable("tooltip.ls_datastore.remote.linked", p.getX(), p.getY(), p.getZ()).withStyle(ChatFormatting.AQUA));
    }
}
