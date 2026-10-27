package com.lodestar.core.client;

import com.lodestar.core.recipe.display.RecipeView;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Draws {@link RecipeView}s (Lens, Codex) and finds what the mouse is over. */
public final class RecipeDraw {
    private RecipeDraw() {}

    /** What the mouse is over in a recipe view: an item slot (with the stack currently shown) or a tank. */
    public record Hit(@Nullable RecipeView.SlotView slot, ItemStack stack, @Nullable RecipeView.TankView tank) {
        public FluidStack fluid() {
            return tank == null ? FluidStack.EMPTY : tank.fluid();
        }
    }

    /** The stack a slot shows right now (alternatives cycle once a second). */
    public static ItemStack shown(RecipeView.SlotView s) {
        if (s.stacks().isEmpty()) return ItemStack.EMPTY;
        int i = (int) ((System.currentTimeMillis() / 1000) % s.stacks().size());
        return s.stacks().get(i);
    }

    public static void draw(GuiGraphics g, RecipeView v, int x, int y, int mx, int my) {
        Font font = Minecraft.getInstance().font;
        for (RecipeView.TankView t : v.tanks) {
            GuiDraw.fluid(g, x + t.x(), y + t.y(), t.w(), t.h(), t.fluid().getFluid(), t.fluid().getAmount(), t.capacity());
        }
        for (RecipeView.SlotView s : v.slots) {
            GuiDraw.slot(g, x + s.x(), y + s.y());
            if (s.output()) g.fill(x + s.x(), y + s.y(), x + s.x() + 16, y + s.y() + 16, 0x30FFD86B);
            if (s.note() != null) g.fill(x + s.x(), y + s.y(), x + s.x() + 16, y + s.y() + 16, 0x306BB8FF);
        }
        if (v.arrowX >= 0) GuiDraw.arrow(g, x + v.arrowX, y + v.arrowY, (System.currentTimeMillis() % 2000) / 2000f);
        for (RecipeView.SlotView s : v.slots) {
            ItemStack st = shown(s);
            if (st.isEmpty()) continue;
            g.renderItem(st, x + s.x(), y + s.y());
            g.renderItemDecorations(font, st, x + s.x(), y + s.y());
            if (s.chance() < 1f) {
                String c = Math.round(s.chance() * 100) + "%";
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                g.pose().scale(0.5f, 0.5f, 1f);
                g.drawString(font, c, (x + s.x()) * 2, (y + s.y()) * 2, 0xFFFFE070, true);
                g.pose().popPose();
            }
        }
        for (RecipeView.TextView t : v.texts) {
            int tx = t.centered() ? x + t.x() - font.width(t.text()) / 2 : x + t.x();
            g.drawString(font, t.text(), tx, y + t.y(), t.color(), false);
        }
        Hit h = hit(v, x, y, mx, my);
        if (h != null && h.slot() != null) {
            g.fill(x + h.slot().x(), y + h.slot().y(), x + h.slot().x() + 16, y + h.slot().y() + 16, 0x80FFFFFF);
        }
    }

    @Nullable
    public static Hit hit(RecipeView v, int x, int y, int mx, int my) {
        for (RecipeView.SlotView s : v.slots) {
            if (GuiDraw.in(mx, my, x + s.x(), y + s.y(), 16, 16)) return new Hit(s, shown(s), null);
        }
        for (RecipeView.TankView t : v.tanks) {
            if (GuiDraw.in(mx, my, x + t.x(), y + t.y(), t.w(), t.h())) return new Hit(null, ItemStack.EMPTY, t);
        }
        return null;
    }

    /** Tooltip for a hit: the item's tooltip plus chance and note lines, or the fluid's name and amount. */
    public static void tooltip(GuiGraphics g, Hit h, int mx, int my) {
        Font font = Minecraft.getInstance().font;
        List<Component> lines = new ArrayList<>();
        if (h.slot() != null && !h.stack().isEmpty()) {
            lines.addAll(net.minecraft.client.gui.screens.Screen.getTooltipFromItem(Minecraft.getInstance(), h.stack()));
            if (h.slot().chance() < 1f) lines.add(Component.translatable("lens.ls_core.chance", Math.round(h.slot().chance() * 100)).withStyle(ChatFormatting.GOLD));
            if (h.slot().note() != null) lines.add(h.slot().note().copy().withStyle(ChatFormatting.AQUA));
            if (h.slot().stacks().size() > 1) lines.add(Component.translatable("lens.ls_core.alternatives", h.slot().stacks().size()).withStyle(ChatFormatting.DARK_GRAY));
        } else if (h.tank() != null) {
            FluidStack f = h.tank().fluid();
            lines.add(f.getDisplayName());
            lines.add(Component.literal(String.format("%,d mB", f.getAmount())).withStyle(ChatFormatting.GRAY));
        }
        if (lines.isEmpty()) return;
        List<FormattedCharSequence> seq = new ArrayList<>();
        for (Component c : lines) seq.add(c.getVisualOrderText());
        g.renderTooltip(font, seq, mx, my);
    }

    static TooltipFlag flag() {
        return Minecraft.getInstance().options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;
    }
}
