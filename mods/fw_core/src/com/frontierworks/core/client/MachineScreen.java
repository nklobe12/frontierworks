package com.frontierworks.core.client;

import com.frontierworks.core.energy.EnergyUtil;
import com.frontierworks.core.machine.MachineBlockEntity;
import com.frontierworks.core.machine.MachineLayout;
import com.frontierworks.core.machine.MachineMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.List;

/** Draws any machine GUI from its MachineLayout. */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private final MachineLayout layout;
    private final List<Button> buttons = new ArrayList<>();
    private final List<Integer> buttonIds = new ArrayList<>();

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
            Button b = Button.builder(menu.be.buttonLabel(id, menu), btn -> {
                if (minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
                }
            }).bounds(leftPos + def.x, topPos + def.y, def.w, def.h).build();
            buttons.add(addRenderableWidget(b));
            buttonIds.add(id);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setMessage(menu.be.buttonLabel(buttonIds.get(i), menu));
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        GuiDraw.panel(g, x, y, imageWidth, imageHeight);
        for (Slot s : menu.slots) {
            if (s instanceof MachineMenu.MachineSlot && ((MachineMenu.MachineSlot) s).role == MachineLayout.Role.OUTPUT && layout.count(MachineLayout.Role.OUTPUT) == 1) {
                GuiDraw.bigSlot(g, x + s.x, y + s.y);
            } else {
                GuiDraw.slot(g, x + s.x, y + s.y);
            }
            if (s instanceof MachineMenu.MachineSlot && !s.hasItem()) {
                MachineLayout.Role role = ((MachineMenu.MachineSlot) s).role;
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
            GuiDraw.fluid(g, x + td.x, y + td.y, td.w, td.h, f, menu.tankAmount(t), td.capacity);
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
        if (layout.energyBar && GuiDraw.in(rx, ry, layout.energyX, layout.energyY, 12, layout.energyH)) {
            List<Component> tip = new ArrayList<>();
            tip.add(EnergyUtil.energyLine(menu.data(MachineBlockEntity.D_ENERGY), menu.data(MachineBlockEntity.D_MAX_ENERGY)));
            g.renderComponentTooltip(font, tip, mouseX, mouseY);
        }
        for (int t = 0; t < layout.tanks.size(); t++) {
            MachineLayout.TankDef td = layout.tanks.get(t);
            if (GuiDraw.in(rx, ry, td.x, td.y, td.w, td.h)) {
                List<Component> tip = new ArrayList<>();
                Fluid f = MachineBlockEntity.fluidById(menu.tankFluidId(t));
                if (f == null || menu.tankAmount(t) <= 0) {
                    tip.add(Component.translatable("gui.fw_core.empty").withStyle(ChatFormatting.GRAY));
                } else {
                    tip.add(f.getFluidType().getDescription());
                }
                tip.add(Component.literal(menu.tankAmount(t) + " / " + td.capacity + " mB").withStyle(ChatFormatting.GRAY));
                g.renderComponentTooltip(font, tip, mouseX, mouseY);
            }
        }
    }
}
