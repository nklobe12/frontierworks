package com.lodestar.core.client;

import com.lodestar.core.energy.EnergyUtil;
import com.lodestar.core.machine.MachineBlockEntity;
import com.lodestar.core.machine.MachineLayout;
import com.lodestar.core.machine.MachineMenu;
import com.lodestar.core.machine.RedstoneMode;
import com.lodestar.core.machine.SideConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws any machine screen from its {@link MachineLayout}, plus the redstone-control and side-configuration
 * tabs on the right. Machines needing extra drawing register a subclass with {@link #register}.
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> implements ExtraAreas {
    public interface Factory {
        MachineScreen create(MachineMenu menu, Inventory inv, Component title);
    }

    private static final Map<BlockEntityType<?>, Factory> CUSTOM = new HashMap<>();

    /** Client setup: use a custom screen class for machines of this block entity type. */
    public static void register(BlockEntityType<?> type, Factory factory) {
        CUSTOM.put(type, factory);
    }

    public static MachineScreen create(MachineMenu menu, Inventory inv, Component title) {
        Factory f = CUSTOM.get(menu.be.getType());
        return f != null ? f.create(menu, inv, title) : new MachineScreen(menu, inv, title);
    }

    protected static final int TAB = 22;
    protected static final int PANEL_W = 64, PANEL_H = 76;
    protected static final int[][] FACE_POS = {{25, 22}, {41, 38}, {9, 22}, {41, 22}, {25, 6}, {25, 38}};
    protected static final String[] FACE_LETTER = {"F", "B", "L", "R", "T", "D"};
    protected static final int[] MODE_COLOR = {0xFF6E6E6E, 0xFF3F7FE0, 0xFFE08A2E, 0xFF4CB34C};

    protected final MachineLayout layout;
    private final List<Button> buttons = new ArrayList<>();
    private final List<Integer> buttonIds = new ArrayList<>();
    protected boolean configOpen;

    public MachineScreen(MachineMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.layout = menu.be.getLayout();
        this.imageWidth = 176;
        this.imageHeight = layout.height;
        this.inventoryLabelY = layout.invY - 11;
    }

    @Override
    protected void init() {
        super.init();
        buttons.clear();
        buttonIds.clear();
        for (MachineLayout.ButtonDef def : layout.buttons) {
            final int id = def.id;
            Button b = Button.builder(menu.be.buttonLabel(id, menu), btn -> click(id))
                    .bounds(leftPos + def.x, topPos + def.y, def.w, def.h).build();
            buttons.add(addRenderableWidget(b));
            buttonIds.add(id);
        }
    }

    protected void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setMessage(menu.be.buttonLabel(buttonIds.get(i), menu));
        }
    }

    protected boolean showConfig() {
        return menu.be.hasSideConfig();
    }

    @Override
    public List<Rect2i> extraAreas() {
        List<Rect2i> list = new ArrayList<>();
        if (showConfig()) {
            list.add(new Rect2i(leftPos + imageWidth, topPos + 4, configOpen ? PANEL_W : TAB, TAB * 2 + 2 + (configOpen ? PANEL_H : 0)));
        }
        return list;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        GuiDraw.panel(g, x, y, imageWidth, imageHeight);
        for (Slot s : menu.slots) {
            if (s instanceof MachineMenu.MachineSlot ms && ms.role == MachineLayout.Role.OUTPUT && layout.count(MachineLayout.Role.OUTPUT) == 1) {
                GuiDraw.bigSlot(g, x + s.x, y + s.y);
            } else {
                GuiDraw.slot(g, x + s.x, y + s.y);
            }
            if (s instanceof MachineMenu.MachineSlot ms && !s.hasItem()) {
                MachineLayout.Role role = ms.role;
                int marker = role == MachineLayout.Role.UPGRADE ? 0x6033AA33 : role == MachineLayout.Role.BATTERY || role == MachineLayout.Role.CHARGE ? 0x60CC2222
                        : role == MachineLayout.Role.FUEL ? 0x60CC7722 : 0;
                if (marker != 0) g.fill(x + s.x + 2, y + s.y + 2, x + s.x + 14, y + s.y + 14, marker);
            }
        }
        if (layout.energyBar) {
            GuiDraw.energyBar(g, x + layout.energyX, y + layout.energyY, 12, layout.energyH,
                    menu.data(MachineBlockEntity.D_ENERGY), menu.data(MachineBlockEntity.D_MAX_ENERGY));
        }
        for (int t = 0; t < layout.tanks.size(); t++) {
            MachineLayout.TankDef td = layout.tanks.get(t);
            Fluid f = MachineBlockEntity.fluidById(menu.tankFluidId(t));
            GuiDraw.fluid(g, x + td.x, y + td.y, td.w, td.h, f, menu.tankAmount(t), Math.max(1, menu.tankCapacity(t)));
        }
        if (layout.arrowX >= 0) {
            int max = menu.data(MachineBlockEntity.D_MAX_PROGRESS);
            float p = max > 0 ? (float) menu.data(MachineBlockEntity.D_PROGRESS) / max : 0;
            GuiDraw.arrow(g, x + layout.arrowX, y + layout.arrowY, p);
        }
        if (layout.flameX >= 0) {
            int max = menu.data(MachineBlockEntity.D_MAX_BURN);
            float b = max > 0 ? (float) menu.data(MachineBlockEntity.D_BURN) / max : 0;
            GuiDraw.flame(g, x + layout.flameX, y + layout.flameY, b);
        }
        drawExtraBg(g, x, y, mouseX, mouseY);
        if (showConfig()) drawConfig(g, mouseX, mouseY);
    }

    /** Hook for subclasses: extra background drawing (absolute coordinates of the panel origin). */
    protected void drawExtraBg(GuiGraphics g, int x, int y, int mouseX, int mouseY) {}

    protected void drawConfig(GuiGraphics g, int mouseX, int mouseY) {
        int tx = leftPos + imageWidth - 1, ty = topPos + 4;
        // redstone tab
        GuiDraw.tab(g, tx, ty, TAB, TAB, 0xFFB03030);
        g.renderItem(new ItemStack(Items.REDSTONE), tx + 3, ty + 3);
        RedstoneMode rm = menu.redstone();
        if (rm != RedstoneMode.IGNORE) {
            g.fill(tx + 15, ty + 15, tx + 20, ty + 20, rm == RedstoneMode.HIGH ? 0xFFFF3030 : 0xFF404040);
        }
        // config tab / panel
        int cy = ty + TAB + 2;
        if (!configOpen) {
            GuiDraw.tab(g, tx, cy, TAB, TAB, 0xFF3060B0);
            g.renderItem(new ItemStack(Items.HOPPER), tx + 3, cy + 3);
            return;
        }
        GuiDraw.tab(g, tx, cy, PANEL_W, PANEL_H, 0xFF3060B0);
        g.renderItem(new ItemStack(Items.HOPPER), tx + 3, cy + 3);
        SideConfig sc = menu.sides();
        for (int f = 0; f < 6; f++) {
            int bx = tx + FACE_POS[f][0], by = cy + FACE_POS[f][1];
            int col = MODE_COLOR[sc.get(f).ordinal()];
            g.fill(bx, by, bx + 14, by + 14, 0xFF000000);
            g.fill(bx + 1, by + 1, bx + 13, by + 13, col);
            g.drawString(font, FACE_LETTER[f], bx + 4, by + 3, 0xFFFFFFFF, true);
        }
        toggle(g, tx + 5, cy + 58, sc.autoEject, "gui.ls_core.eject");
        toggle(g, tx + 34, cy + 58, sc.autoInput, "gui.ls_core.input");
    }

    private void toggle(GuiGraphics g, int x, int y, boolean on, String key) {
        g.fill(x, y, x + 26, y + 12, 0xFF000000);
        g.fill(x + 1, y + 1, x + 25, y + 11, on ? 0xFF4CB34C : 0xFF555555);
        Component c = Component.translatable(key);
        g.drawString(font, c, x + 13 - font.width(c) / 2, y + 2, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (showConfig()) {
            int tx = leftPos + imageWidth - 1, ty = topPos + 4;
            int cy = ty + TAB + 2;
            if (GuiDraw.in((int) mx, (int) my, tx, ty, TAB, TAB)) {
                click(MachineBlockEntity.BTN_REDSTONE);
                return true;
            }
            if (configOpen) {
                SideConfig sc = menu.sides();
                for (int f = 0; f < 6; f++) {
                    int bx = tx + FACE_POS[f][0], by = cy + FACE_POS[f][1];
                    if (GuiDraw.in((int) mx, (int) my, bx, by, 14, 14)) {
                        click((button == 1 ? MachineBlockEntity.BTN_SIDE_BACK : MachineBlockEntity.BTN_SIDE) + f);
                        return true;
                    }
                }
                if (GuiDraw.in((int) mx, (int) my, tx + 5, cy + 58, 26, 12)) {
                    click(MachineBlockEntity.BTN_EJECT);
                    return true;
                }
                if (GuiDraw.in((int) mx, (int) my, tx + 34, cy + 58, 26, 12)) {
                    click(MachineBlockEntity.BTN_INPUT);
                    return true;
                }
                if (GuiDraw.in((int) mx, (int) my, tx, cy, 22, 22)) {
                    configOpen = false;
                    return true;
                }
                if (GuiDraw.in((int) mx, (int) my, tx, cy, PANEL_W, PANEL_H)) return true;
            } else if (GuiDraw.in((int) mx, (int) my, tx, cy, TAB, TAB)) {
                configOpen = true;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, (imageWidth - font.width(title)) / 2, 6, GuiDraw.TEXT, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, GuiDraw.TEXT, false);
        List<Component> info = new ArrayList<>();
        menu.be.addInfo(info, menu);
        if (!info.isEmpty()) {
            int ix = layout.infoX >= 0 ? layout.infoX : (layout.arrowX >= 0 ? layout.arrowX : 60);
            int iy = layout.infoY >= 0 ? layout.infoY : (layout.arrowY >= 0 ? layout.arrowY + 22 : 20);
            for (Component c : info) {
                g.drawString(font, c, ix, iy, GuiDraw.TEXT, false);
                iy += 10;
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        List<Component> tip = new ArrayList<>();
        if (layout.energyBar && GuiDraw.in(rx, ry, layout.energyX, layout.energyY, 12, layout.energyH)) {
            tip.add(EnergyUtil.energyLine(menu.data(MachineBlockEntity.D_ENERGY), menu.data(MachineBlockEntity.D_MAX_ENERGY)));
            int rate = menu.data(MachineBlockEntity.D_RATE);
            if (rate > 0) tip.add(Component.translatable("gui.ls_core.rate", rate).withStyle(ChatFormatting.GRAY));
        }
        for (int t = 0; t < layout.tanks.size(); t++) {
            MachineLayout.TankDef td = layout.tanks.get(t);
            if (GuiDraw.in(rx, ry, td.x, td.y, td.w, td.h)) {
                Fluid f = MachineBlockEntity.fluidById(menu.tankFluidId(t));
                if (f == null || menu.tankAmount(t) <= 0) {
                    tip.add(Component.translatable("gui.ls_core.empty").withStyle(ChatFormatting.GRAY));
                } else {
                    tip.add(f.getFluidType().getDescription());
                }
                tip.add(Component.literal(menu.tankAmount(t) + " / " + menu.tankCapacity(t) + " mB").withStyle(ChatFormatting.GRAY));
            }
        }
        if (showConfig()) addConfigTooltip(tip, mouseX, mouseY);
        addExtraTooltip(tip, rx, ry);
        if (!tip.isEmpty()) g.renderComponentTooltip(font, tip, mouseX, mouseY);
    }

    /** Hook for subclasses: tooltip lines for (panel-relative) mouse coordinates. */
    protected void addExtraTooltip(List<Component> tip, int rx, int ry) {}

    private void addConfigTooltip(List<Component> tip, int mx, int my) {
        int tx = leftPos + imageWidth - 1, ty = topPos + 4;
        int cy = ty + TAB + 2;
        if (GuiDraw.in(mx, my, tx, ty, TAB, TAB)) {
            tip.add(Component.translatable("gui.ls_core.redstone"));
            tip.add(Component.translatable("gui.ls_core.redstone." + menu.redstone().name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GRAY));
            return;
        }
        if (!configOpen) {
            if (GuiDraw.in(mx, my, tx, cy, TAB, TAB)) {
                tip.add(Component.translatable("gui.ls_core.config"));
                tip.add(Component.translatable("gui.ls_core.config.hint").withStyle(ChatFormatting.GRAY));
            }
            return;
        }
        SideConfig sc = menu.sides();
        for (int f = 0; f < 6; f++) {
            int bx = tx + FACE_POS[f][0], by = cy + FACE_POS[f][1];
            if (GuiDraw.in(mx, my, bx, by, 14, 14)) {
                tip.add(Component.translatable("gui.ls_core.face." + SideConfig.FACE_NAMES[f]));
                tip.add(Component.translatable("gui.ls_core.mode." + sc.get(f).name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GRAY));
                tip.add(Component.translatable("gui.ls_core.face.hint").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        if (GuiDraw.in(mx, my, tx + 5, cy + 58, 26, 12)) tip.add(Component.translatable("gui.ls_core.eject.hint"));
        if (GuiDraw.in(mx, my, tx + 34, cy + 58, 26, 12)) tip.add(Component.translatable("gui.ls_core.input.hint"));
    }
}
