package com.lodestar.toolsmith.station;

import com.lodestar.toolsmith.Modifiers;
import com.lodestar.toolsmith.ModularToolItem;
import com.lodestar.toolsmith.PartItem;
import com.lodestar.toolsmith.PartType;
import com.lodestar.toolsmith.ToolData;
import com.lodestar.toolsmith.ToolMaterial;
import com.lodestar.toolsmith.ToolType;
import com.lodestar.toolsmith.ToolsmithSetup;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Tool station and tool forge. Put parts in the slots to assemble a tool (large tools need the forge). Put a tool in
 * the centre slot with its head material around it to repair it, or with a modifier item to add a modifier.
 */
public class ToolStationMenu extends AbstractContainerMenu {
    public static final int[][] POS = {{48, 35}, {24, 35}, {48, 11}, {72, 35}, {48, 59}};
    public final StationBE be;
    private final ContainerLevelAccess access;
    private final Container result = new SimpleContainer(1);
    private final int[] use = new int[5];
    @Nullable
    public Component error;

    public ToolStationMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (StationBE) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public ToolStationMenu(int id, Inventory inv, StationBE be) {
        super(ToolsmithSetup.TOOL_STATION_MENU.get(), id);
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        for (int i = 0; i < 5; i++) {
            addSlot(new SlotItemHandler(be.items, i, POS[i][0], POS[i][1]) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    updateResult();
                }
            });
        }
        addSlot(new Slot(result, 0, 134, 35) {
            @Override
            public boolean mayPlace(@Nonnull ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(@Nonnull Player player, @Nonnull ItemStack stack) {
                for (int i = 0; i < 5; i++) if (use[i] > 0) be.items.extractItem(i, use[i], false);
                player.level().playSound(null, be.getBlockPos(), SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.7f, 1.1f);
                updateResult();
                super.onTake(player, stack);
            }
        });
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
        updateResult();
    }

    public boolean isForge() {
        return be.kind == StationBE.Kind.TOOL_FORGE;
    }

    private void updateResult() {
        java.util.Arrays.fill(use, 0);
        error = null;
        result.setItem(0, compute());
    }

    private ItemStack compute() {
        ItemStack main = be.items.getStackInSlot(0);
        if (main.getItem() instanceof ModularToolItem && ToolData.has(main)) return modify(main);
        List<PartType> types = new ArrayList<>();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ItemStack st = be.items.getStackInSlot(i);
            if (st.isEmpty()) continue;
            if (!(st.getItem() instanceof PartItem pi)) return ItemStack.EMPTY;
            types.add(pi.part);
            slots.add(i);
        }
        if (types.isEmpty()) return ItemStack.EMPTY;
        ToolType t = ToolType.forParts(types);
        if (t == null) return ItemStack.EMPTY;
        if (t.large && !isForge()) {
            error = Component.translatable("msg.ls_toolsmith.needs_forge");
            return ItemStack.EMPTY;
        }
        List<ToolMaterial> mats = new ArrayList<>();
        boolean[] taken = new boolean[5];
        for (PartType p : t.parts) {
            for (int s : slots) {
                ItemStack st = be.items.getStackInSlot(s);
                if (!taken[s] && st.getItem() instanceof PartItem pi && pi.part == p) {
                    taken[s] = true;
                    mats.add(PartItem.material(st));
                    use[s] = 1;
                    break;
                }
            }
        }
        return ToolData.build(t, mats);
    }

    private ItemStack modify(ItemStack main) {
        ItemStack out = main.copy();
        boolean changed = false;
        List<ToolMaterial> mats = ToolData.materials(out);
        ToolMaterial head = mats.isEmpty() ? null : mats.get(0);
        for (int i = 1; i < 5; i++) {
            ItemStack st = be.items.getStackInSlot(i);
            if (st.isEmpty()) continue;
            if (head != null && head.matches(st)) {
                if (out.getDamageValue() <= 0) continue;
                int per = Math.max(1, (int) Math.ceil(out.getMaxDamage() * 0.25));
                int n = Math.min(st.getCount(), (int) Math.ceil((double) out.getDamageValue() / per));
                out.setDamageValue(Math.max(0, out.getDamageValue() - n * per));
                use[i] = n;
                changed = true;
                continue;
            }
            Modifiers.Def d = Modifiers.forItem(st);
            if (d == null) {
                error = Component.translatable("msg.ls_toolsmith.not_usable");
                return ItemStack.EMPTY;
            }
            Component why = Modifiers.check(out, d);
            if (why != null) {
                error = why;
                return ItemStack.EMPTY;
            }
            ToolData.setModifier(out, d.id(), ToolData.modifier(out, d.id()) + 1);
            use[i] = 1;
            changed = true;
        }
        return changed ? out : ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, be.getBlockState().getBlock());
    }

    @Nonnull
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack st = slot.getItem();
        ItemStack copy = st.copy();
        if (index == 5) {
            if (!moveItemStackTo(st, 6, slots.size(), true)) return ItemStack.EMPTY;
            slot.onTake(player, copy);
            return copy;
        }
        if (index < 5) {
            if (!moveItemStackTo(st, 6, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(st, 0, 5, false)) {
            return ItemStack.EMPTY;
        }
        if (st.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
