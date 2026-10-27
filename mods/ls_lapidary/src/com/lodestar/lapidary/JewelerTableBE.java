package com.lodestar.lapidary;

import com.lodestar.core.machine.MachineBlockEntity;
import com.lodestar.core.machine.MachineLayout;
import com.lodestar.core.machine.MachineMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Jeweler's table: adds sockets to a tool or piece of armor (a silver setting for the first, gold for the second,
 * platinum for the third) and sets cut gems into them, or takes them back out.
 */
public class JewelerTableBE extends MachineBlockEntity {
    public static final int SLOT_TOOL = 0, SLOT_SETTING = 1, SLOT_GEM = 2;
    public static final int BTN_ADD = 0, BTN_SOCKET = 1;
    public static final int SOCKET_X = 62, SOCKET_Y = 24, SOCKET_STEP = 24;
    public static final MachineLayout LAYOUT = MachineLayout.builder()
            .slot(26, 35, MachineLayout.Role.STORAGE)
            .slot(26, 60, MachineLayout.Role.INPUT)
            .slot(140, 35, MachineLayout.Role.INPUT)
            .button(BTN_ADD, 52, 56, 76, 14, "gui.ls_lapidary.add_socket").build();

    public JewelerTableBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, LAYOUT, 0, 0, 0);
    }

    @Override
    public boolean hasSideConfig() {
        return false;
    }

    public static Item settingFor(int socketIndex) {
        return switch (socketIndex) {
            case 0 -> Content.SILVER_SETTING.get();
            case 1 -> Content.GOLD_SETTING.get();
            default -> Content.PLATINUM_SETTING.get();
        };
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_TOOL -> GemData.socketable(stack);
            case SLOT_SETTING -> stack.is(Content.SILVER_SETTING.get()) || stack.is(Content.GOLD_SETTING.get()) || stack.is(Content.PLATINUM_SETTING.get());
            case SLOT_GEM -> stack.getItem() instanceof CutGemItem;
            default -> super.isItemValid(slot, stack);
        };
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot == SLOT_TOOL ? 1 : 64;
    }

    @Override
    public void serverTick() {}

    /** Why a socket can't be added right now, or null if it can. */
    @Nullable
    public Component addProblem() {
        ItemStack tool = items.getStackInSlot(SLOT_TOOL);
        if (!GemData.socketable(tool)) return Component.translatable("gui.ls_lapidary.need_tool");
        int n = GemData.sockets(tool).size();
        if (n >= GemData.maxSockets(tool)) return Component.translatable("gui.ls_lapidary.full");
        if (!items.getStackInSlot(SLOT_SETTING).is(settingFor(n))) {
            return Component.translatable("gui.ls_lapidary.need_setting", new ItemStack(settingFor(n)).getHoverName());
        }
        return null;
    }

    /** Adds one socket using the setting in its slot. */
    public boolean addSocket() {
        if (addProblem() != null) return false;
        ItemStack tool = items.getStackInSlot(SLOT_TOOL).copy();
        List<GemData.Socket> s = new ArrayList<>(GemData.sockets(tool));
        s.add(GemData.Socket.EMPTY);
        GemData.setSockets(tool, s);
        items.extractItem(SLOT_SETTING, 1, false);
        items.setStackInSlot(SLOT_TOOL, tool);
        if (level != null) level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.4f, 1.6f);
        return true;
    }

    /** Sets the gem from the gem slot into an empty socket, or takes a set gem out into the empty gem slot. */
    public boolean toggleSocket(int index) {
        ItemStack tool = items.getStackInSlot(SLOT_TOOL);
        List<GemData.Socket> s = new ArrayList<>(GemData.sockets(tool));
        if (index < 0 || index >= s.size()) return false;
        ItemStack gem = items.getStackInSlot(SLOT_GEM);
        GemData.Socket cur = s.get(index);
        if (cur.empty()) {
            if (!(gem.getItem() instanceof CutGemItem)) return false;
            s.set(index, GemData.Socket.of(gem));
            items.extractItem(SLOT_GEM, 1, false);
        } else {
            ItemStack back = cur.toStack();
            if (!gem.isEmpty() && !(ItemStack.isSameItemSameTags(gem, back) && gem.getCount() < gem.getMaxStackSize())) return false;
            s.set(index, GemData.Socket.EMPTY);
            if (gem.isEmpty()) items.setStackInSlot(SLOT_GEM, back);
            else {
                ItemStack g = gem.copy();
                g.grow(1);
                items.setStackInSlot(SLOT_GEM, g);
            }
        }
        ItemStack copy = tool.copy();
        GemData.setSockets(copy, s);
        items.setStackInSlot(SLOT_TOOL, copy);
        if (level != null) level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.7f, cur.empty() ? 1.4f : 0.9f);
        return true;
    }

    @Override
    public void onCustomButton(int id, Player player) {
        if (id == BTN_ADD) {
            Component why = addProblem();
            if (why != null) player.displayClientMessage(why.copy().withStyle(ChatFormatting.RED), true);
            else addSocket();
        } else if (id >= BTN_SOCKET && id < BTN_SOCKET + 3) {
            toggleSocket(id - BTN_SOCKET);
        }
    }

    @Override
    public Component buttonLabel(int id, MachineMenu menu) {
        return Component.translatable("gui.ls_lapidary.add_socket");
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {}
}
