package com.lodestar.quarry.client;

import com.lodestar.core.client.GuiDraw;
import com.lodestar.core.client.MachineScreen;
import com.lodestar.core.machine.MachineLayout;
import com.lodestar.core.machine.MachineMenu;
import com.lodestar.quarry.AbstractQuarryBE;
import com.lodestar.quarry.Content;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Quarry screen: ghost icons in the bit, sieve and fill slots and a depth gauge between the outputs and the augments. */
public class QuarryScreen extends MachineScreen {
    private static final int GX = 138, GY = 17, GW = 8, GH = 52;

    public QuarryScreen(MachineMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    private ItemStack ghost(int slot) {
        if (slot == AbstractQuarryBE.S_BIT) return new ItemStack(Content.FORTUNE_DRILL_BIT_1.get());
        if (slot == AbstractQuarryBE.S_SIEVE) return new ItemStack(Content.ORE_SIEVE.get());
        if (slot >= AbstractQuarryBE.S_FILL && slot < AbstractQuarryBE.S_FILL + AbstractQuarryBE.FILLS) return new ItemStack(Items.COBBLESTONE);
        return ItemStack.EMPTY;
    }

    private float depthFraction() {
        int top = menu.extra(AbstractQuarryBE.X_TOP), floor = menu.extra(AbstractQuarryBE.X_FLOOR), y = menu.extra(AbstractQuarryBE.X_Y);
        int ph = menu.extra(AbstractQuarryBE.X_PHASE);
        if (ph == AbstractQuarryBE.Phase.NO_AREA.ordinal() || ph == AbstractQuarryBE.Phase.BUILDING.ordinal() || top <= floor) return 0f;
        if (ph == AbstractQuarryBE.Phase.DONE.ordinal()) return 1f;
        return Math.max(0f, Math.min(1f, (float) (top - y) / (top - floor)));
    }

    @Override
    protected void drawExtraBg(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        for (int i = 0; i < AbstractQuarryBE.S_OUT; i++) {
            if (menu.getSlot(i).hasItem()) continue;
            MachineLayout.SlotDef d = layout.slots.get(i);
            g.renderFakeItem(ghost(i), x + d.x, y + d.y);
            g.fill(x + d.x, y + d.y, x + d.x + 16, y + d.y + 16, 0xA08B8B8B);
        }
        boolean restoring = menu.extra(AbstractQuarryBE.X_PHASE) == AbstractQuarryBE.Phase.RESTORING.ordinal();
        GuiDraw.vbar(g, x + GX, y + GY, GW, GH, 1f - depthFraction(), restoring ? 0xFF5FB04A : 0xFF8A6A42);
        // the drill: a marker line at the current depth
        int my = y + GY + Math.round(GH * depthFraction());
        g.fill(x + GX - 1, Math.min(my, y + GY + GH - 1), x + GX + GW + 1, Math.min(my, y + GY + GH - 1) + 1, 0xFFE0B020);
    }

    @Override
    protected void addExtraTooltip(List<Component> tip, int rx, int ry) {
        if (GuiDraw.in(rx, ry, GX, GY, GW, GH)) {
            tip.add(Component.translatable("gui.ls_quarry.depth", menu.extra(AbstractQuarryBE.X_Y), menu.extra(AbstractQuarryBE.X_TOP),
                    menu.extra(AbstractQuarryBE.X_FLOOR)));
            if ((menu.extra(AbstractQuarryBE.X_FLAGS) & 1) != 0) {
                tip.add(Component.translatable("gui.ls_quarry.spoil", menu.extra(AbstractQuarryBE.X_RUBBLE), menu.extra(AbstractQuarryBE.X_SOIL))
                        .withStyle(ChatFormatting.GRAY));
            }
            tip.add(Component.translatable("gui.ls_quarry.mined", menu.extra(AbstractQuarryBE.X_MINED), menu.extra(AbstractQuarryBE.X_COST))
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        for (int i = 0; i < AbstractQuarryBE.S_OUT; i++) {
            MachineLayout.SlotDef d = layout.slots.get(i);
            if (!menu.getSlot(i).hasItem() && GuiDraw.in(rx, ry, d.x, d.y, 16, 16)) {
                String key = i == AbstractQuarryBE.S_BIT ? "gui.ls_quarry.slot.bit" : i == AbstractQuarryBE.S_SIEVE ? "gui.ls_quarry.slot.sieve" : "gui.ls_quarry.slot.fill";
                tip.add(Component.translatable(key));
            }
        }
    }
}
