package com.lodestar.lapidary.client;

import com.lodestar.core.client.GuiDraw;
import com.lodestar.core.client.MachineScreen;
import com.lodestar.core.machine.MachineMenu;
import com.lodestar.lapidary.Cut;
import com.lodestar.lapidary.CuttingWheelBE;
import com.lodestar.lapidary.GemData;
import com.lodestar.lapidary.GemType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

/** Cutting wheel screen: cut choice buttons, abrasive charge gauge and a bar showing the last cut's quality. */
public class CuttingWheelScreen extends MachineScreen {
    private static final int BTN_Y = 17;

    public CuttingWheelScreen(MachineMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    private Cut selected() {
        return Cut.byIndex(menu.extra(0));
    }

    private ItemStack icon(Cut c) {
        ItemStack in = menu.slots.get(CuttingWheelBE.SLOT_IN).getItem();
        GemType g = GemType.RUBY;
        for (GemType t : GemType.values()) if (!in.isEmpty() && in.getItem() == t.roughItem()) g = t;
        return GemData.setCut(new ItemStack(g.cutItem()), c, 60);
    }

    private static int gradeColor(int q) {
        if (q < 0) return 0xFFC03030;
        return switch (GemData.gradeKey(q)) {
            case "chipped" -> 0xFF606060;
            case "flawed" -> 0xFFA8A8A8;
            case "fine" -> 0xFFF0F0F0;
            case "flawless" -> 0xFF55E0FF;
            default -> 0xFFFFB020;
        };
    }

    @Override
    protected void drawExtraBg(GuiGraphics g, int x, int y, int mx, int my) {
        Cut sel = selected();
        for (Cut c : Cut.values()) {
            int bx = x + CuttingWheelBE.CUT_X, by = y + BTN_Y + c.ordinal() * 18;
            boolean hover = GuiDraw.in(mx, my, bx, by, 18, 18);
            GuiDraw.button(g, bx, by, 18, 18, hover, true);
            if (c == sel) {
                g.fill(bx + 1, by + 1, bx + 17, by + 17, 0xFF7FB2FF);
            }
            g.renderItem(icon(c), bx + 1, by + 1);
        }
        // abrasive charge gauge beside the abrasive slot
        int charge = menu.extra(2);
        GuiDraw.vbar(g, x + 58, y + 51, 3, 16, charge / 8f, 0xFFD8C8A0);
        // last result
        int last = menu.extra(1);
        if (last != 0) {
            GuiDraw.hbar(g, x + 62, y + 56, 24, 3, last < 0 ? 1f : last / 100f, gradeColor(last));
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (Cut c : Cut.values()) {
            if (GuiDraw.in((int) mx, (int) my, leftPos + CuttingWheelBE.CUT_X, topPos + BTN_Y + c.ordinal() * 18, 18, 18)) {
                click(c.ordinal());
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    protected void addExtraTooltip(List<Component> tip, int rx, int ry) {
        Cut sel = selected();
        int packed = menu.extra(3);
        int expected = packed / 1000;
        int shatterPct = packed % 1000;
        for (Cut c : Cut.values()) {
            if (!GuiDraw.in(rx, ry, CuttingWheelBE.CUT_X, BTN_Y + c.ordinal() * 18, 18, 18)) continue;
            int q = expected - sel.base + c.base;
            int sh = Math.max(0, Math.round(c.shatter * 100) - (Math.round(sel.shatter * 100) - shatterPct));
            tip.add(c.displayName().copy().withStyle(c == sel ? ChatFormatting.AQUA : ChatFormatting.WHITE));
            tip.add(Component.translatable("gui.ls_lapidary.expected", Math.max(1, Math.min(100, q)), c.spread).withStyle(ChatFormatting.GRAY));
            tip.add(Component.translatable("gui.ls_lapidary.shatter", sh).withStyle(sh > 0 ? ChatFormatting.RED : ChatFormatting.GRAY));
            tip.add(Component.translatable("gui.ls_lapidary.cut_stats", String.format(Locale.ROOT, "%.1f", c.mult),
                    String.format(Locale.ROOT, "%.1f", c.time)).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (GuiDraw.in(rx, ry, 56, 50, 6, 18)) {
            tip.add(Component.translatable("gui.ls_lapidary.charge", menu.extra(2)));
        }
        if (GuiDraw.in(rx, ry, 62, 34, 24, 26)) {
            int last = menu.extra(1);
            if (last < 0) tip.add(Component.translatable("gui.ls_lapidary.shattered").withStyle(ChatFormatting.RED));
            else if (last > 0) tip.add(Component.translatable("gui.ls_lapidary.last", GemData.grade(last), last));
        }
        if (menu.slots.get(CuttingWheelBE.SLOT_IN).getItem().isEmpty() && GuiDraw.in(rx, ry, 38, 21, 16, 16)) {
            tip.add(Component.translatable("gui.ls_lapidary.rough_slot").withStyle(ChatFormatting.GRAY));
        }
        if (menu.slots.get(CuttingWheelBE.SLOT_GRIT).getItem().isEmpty() && GuiDraw.in(rx, ry, 38, 51, 16, 16)) {
            tip.add(Component.translatable("gui.ls_lapidary.abrasive_slot").withStyle(ChatFormatting.GRAY));
        }
    }
}
