package com.lodestar.lapidary.client;

import com.lodestar.core.client.GuiDraw;
import com.lodestar.core.client.MachineScreen;
import com.lodestar.core.machine.MachineMenu;
import com.lodestar.lapidary.GemData;
import com.lodestar.lapidary.GemEffect;
import com.lodestar.lapidary.JewelerTableBE;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Jeweler's table: the tool's sockets drawn as settings between the tool and gem slots; click one to set or unset a gem. */
public class JewelerScreen extends MachineScreen {
    public JewelerScreen(MachineMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    private ItemStack tool() {
        return menu.slots.get(JewelerTableBE.SLOT_TOOL).getItem();
    }

    private int sx(int i) {
        return JewelerTableBE.SOCKET_X + i * JewelerTableBE.SOCKET_STEP;
    }

    @Override
    protected void drawExtraBg(GuiGraphics g, int x, int y, int mx, int my) {
        ItemStack tool = tool();
        List<GemData.Socket> sockets = GemData.sockets(tool);
        int max = GemData.socketable(tool) ? GemData.maxSockets(tool) : 3;
        for (int i = 0; i < 3; i++) {
            int bx = x + sx(i), by = y + JewelerTableBE.SOCKET_Y;
            if (i >= max) continue;
            boolean exists = i < sockets.size();
            int ring = exists ? (i == 0 ? 0xFFDCE3EA : i == 1 ? 0xFFF2C94C : 0xFFA9D8E6) : 0xFF8B8B8B;
            g.fill(bx + 2, by, bx + 18, by + 20, 0xFF373737);
            g.fill(bx, by + 2, bx + 20, by + 18, 0xFF373737);
            g.fill(bx + 3, by + 1, bx + 17, by + 19, ring);
            g.fill(bx + 1, by + 3, bx + 19, by + 17, ring);
            g.fill(bx + 4, by + 4, bx + 16, by + 16, exists ? 0xFF1E1E22 : 0xFF6E6E6E);
            if (exists && !sockets.get(i).empty()) {
                g.renderItem(sockets.get(i).toStack(), bx + 2, by + 2);
            } else if (!exists) {
                g.renderItem(new ItemStack(JewelerTableBE.settingFor(i)), bx + 2, by + 2);
                g.fill(bx + 2, by + 2, bx + 18, by + 18, 0x80C6C6C6);
            }
        }
        // arrows hinting the flow: tool -> sockets <- gem
        g.fill(x + 45, y + 31, x + 60, y + 33, 0xFF8B8B8B);
        g.fill(x + 132, y + 31, x + 138, y + 33, 0xFF8B8B8B);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int n = GemData.sockets(tool()).size();
        for (int i = 0; i < n; i++) {
            if (GuiDraw.in((int) mx, (int) my, leftPos + sx(i), topPos + JewelerTableBE.SOCKET_Y, 20, 20)) {
                click(JewelerTableBE.BTN_SOCKET + i);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    protected void addExtraTooltip(List<Component> tip, int rx, int ry) {
        ItemStack tool = tool();
        List<GemData.Socket> sockets = GemData.sockets(tool);
        boolean armor = GemData.isArmor(tool);
        int max = GemData.socketable(tool) ? GemData.maxSockets(tool) : 3;
        for (int i = 0; i < max; i++) {
            if (!GuiDraw.in(rx, ry, sx(i), JewelerTableBE.SOCKET_Y, 20, 20)) continue;
            if (i >= sockets.size()) {
                tip.add(Component.translatable("gui.ls_lapidary.socket_locked", new ItemStack(JewelerTableBE.settingFor(i)).getHoverName()).withStyle(ChatFormatting.GRAY));
            } else if (sockets.get(i).empty()) {
                tip.add(Component.translatable("gui.ls_lapidary.socket_empty").withStyle(ChatFormatting.GRAY));
            } else {
                GemData.Socket s = sockets.get(i);
                GemEffect ef = s.gem().effect(armor);
                tip.add(s.toStack().getHoverName());
                tip.add(ef.describe(ef.perScale * s.scale()).withStyle(ChatFormatting.BLUE));
                tip.add(Component.translatable("gui.ls_lapidary.socket_filled").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        if (tool.isEmpty() && GuiDraw.in(rx, ry, 26, 24, 16, 16)) tip.add(Component.translatable("gui.ls_lapidary.tool_slot").withStyle(ChatFormatting.GRAY));
        if (menu.slots.get(JewelerTableBE.SLOT_SETTING).getItem().isEmpty() && GuiDraw.in(rx, ry, 26, 52, 16, 16)) {
            tip.add(Component.translatable("gui.ls_lapidary.setting_slot").withStyle(ChatFormatting.GRAY));
        }
        if (menu.slots.get(JewelerTableBE.SLOT_GEM).getItem().isEmpty() && GuiDraw.in(rx, ry, 140, 24, 16, 16)) {
            tip.add(Component.translatable("gui.ls_lapidary.gem_slot").withStyle(ChatFormatting.GRAY));
        }
    }
}
