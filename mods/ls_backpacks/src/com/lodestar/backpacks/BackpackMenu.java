package com.lodestar.backpacks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Backpack screen: the storage grid, upgrade slots, and a side page for each fitted crafting, smelting or tank
 * upgrade. The backpack being viewed is locked in place while open.
 */
public class BackpackMenu extends AbstractContainerMenu {
    public static final int SIDE_X = 180, PAGE_X = 206;
    public static final int PAGE_NONE = 0, PAGE_CRAFT = 1, PAGE_SMELT = 2, PAGE_TANK = 3;

    /** Where the open backpack is. */
    public record Source(int kind, int index, BlockPos pos) {
        static final int INV = 0, OFFHAND = 1, CHEST = 2, BLOCK = 3;

        public static Source hotbar(int i) {
            return new Source(INV, i, BlockPos.ZERO);
        }

        public static Source inventory(int i) {
            return new Source(INV, i, BlockPos.ZERO);
        }

        public static Source offhand() {
            return new Source(OFFHAND, 0, BlockPos.ZERO);
        }

        public static Source chest() {
            return new Source(CHEST, 0, BlockPos.ZERO);
        }

        public static Source block(BlockPos p) {
            return new Source(BLOCK, 0, p);
        }

        void write(FriendlyByteBuf buf) {
            buf.writeByte(kind);
            buf.writeVarInt(index);
            buf.writeBlockPos(pos);
        }

        static Source read(FriendlyByteBuf buf) {
            return new Source(buf.readByte(), buf.readVarInt(), buf.readBlockPos());
        }
    }

    public final BackpackTier tier;
    public final Source source;
    private final Player player;
    @Nullable
    private final UUID id;
    @Nullable
    private final Contents contents;
    private final TransientCraftingContainer craftGrid;
    private final ResultContainer craftResult = new ResultContainer();
    private final ContainerData data;
    public int page = PAGE_NONE;
    public final int mainStart, mainEnd, upgradeStart, craftStart, resultSlot, smeltStart, bucketStart, invStart;

    /** Client constructor. */
    public BackpackMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, BackpackTier.values()[buf.readByte()], Source.read(buf), null, null);
    }

    public BackpackMenu(int windowId, Inventory inv, BackpackTier tier, Source source, @Nullable UUID id, @Nullable Contents contents) {
        super(BackpacksSetup.MENU.get(), windowId);
        this.tier = tier;
        this.source = source;
        this.player = inv.player;
        this.id = id;
        this.contents = contents;
        IItemHandler items = contents != null ? contents.items : new ItemStackHandler(tier.slots());
        ItemStackHandler upgrades = contents != null ? contents.upgrades : new ItemStackHandler(5);
        ItemStackHandler smelt = contents != null ? contents.smelt : new ItemStackHandler(3);
        ItemStackHandler buckets = contents != null ? contents.buckets : new ItemStackHandler(2);
        this.craftGrid = new TransientCraftingContainer(this, 3, 3);
        if (contents != null) for (int i = 0; i < 9; i++) craftGrid.setItem(i, contents.craft.getStackInSlot(i).copy());

        mainStart = slots.size();
        for (int r = 0; r < tier.rows; r++)
            for (int c = 0; c < 9; c++) addSlot(new SlotItemHandler(items, c + r * 9, 8 + c * 18, 18 + r * 18));
        mainEnd = slots.size();
        upgradeStart = slots.size();
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            addSlot(new SlotItemHandler(upgrades, i, SIDE_X + 4, 18 + i * 18) {
                @Override
                public boolean isActive() {
                    return idx < tier.upgrades;
                }

                @Override
                public boolean mayPlace(@Nonnull ItemStack stack) {
                    if (idx >= tier.upgrades || !(stack.getItem() instanceof UpgradeItem u)) return false;
                    for (int j = 0; j < upgrades.getSlots(); j++) {
                        if (j != idx && upgrades.getStackInSlot(j).getItem() instanceof UpgradeItem o && o.kind == u.kind) return false;
                    }
                    return true;
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    if (page != PAGE_NONE && !pageAvailable(page)) page = firstPage();
                }
            });
        }
        craftStart = slots.size();
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++) addSlot(new PageSlot(craftGrid, c + r * 3, PAGE_X + c * 18, 18 + r * 18, PAGE_CRAFT));
        resultSlot = slots.size();
        addSlot(new ResultSlot(player, craftGrid, craftResult, 0, PAGE_X + 18, 82) {
            @Override
            public boolean isActive() {
                return page == PAGE_CRAFT;
            }
        });
        smeltStart = slots.size();
        addSlot(new PageItemSlot(smelt, 0, PAGE_X, 18, PAGE_SMELT));
        addSlot(new PageItemSlot(smelt, 1, PAGE_X, 54, PAGE_SMELT) {
            @Override
            public boolean mayPlace(@Nonnull ItemStack stack) {
                return ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0;
            }
        });
        addSlot(new PageItemSlot(smelt, 2, PAGE_X + 48, 36, PAGE_SMELT) {
            @Override
            public boolean mayPlace(@Nonnull ItemStack stack) {
                return false;
            }
        });
        bucketStart = slots.size();
        addSlot(new PageItemSlot(buckets, 0, PAGE_X + 26, 18, PAGE_TANK) {
            @Override
            public void setChanged() {
                super.setChanged();
                handleBuckets();
            }
        });
        addSlot(new PageItemSlot(buckets, 1, PAGE_X + 26, 64, PAGE_TANK) {
            @Override
            public boolean mayPlace(@Nonnull ItemStack stack) {
                return false;
            }
        });
        invStart = slots.size();
        int y0 = 18 + tier.rows * 18 + 14;
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(lockable(inv, c + r * 9 + 9, 8 + c * 18, y0 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(lockable(inv, c, 8 + c * 18, y0 + 58));
        data = contents == null ? new SimpleContainerData(5) : new ContainerData() {
            public int get(int i) {
                return switch (i) {
                    case 0 -> contents.burn;
                    case 1 -> contents.burnMax;
                    case 2 -> contents.cook;
                    case 3 -> contents.tank.getFluidAmount();
                    case 4 -> contents.tank.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(contents.tank.getFluid().getFluid());
                    default -> 0;
                };
            }

            public void set(int i, int v) {}

            public int getCount() {
                return 5;
            }
        };
        addDataSlots(data);
        page = firstPage();
    }

    private Slot lockable(Inventory inv, int index, int x, int y) {
        boolean locked = source.kind() == Source.INV && source.index() == index;
        return new Slot(inv, index, x, y) {
            @Override
            public boolean mayPickup(@Nonnull Player p) {
                return !locked && super.mayPickup(p);
            }

            @Override
            public boolean mayPlace(@Nonnull ItemStack stack) {
                return !locked && super.mayPlace(stack);
            }
        };
    }

    private class PageSlot extends Slot {
        private final int pageId;

        PageSlot(Container c, int i, int x, int y, int pageId) {
            super(c, i, x, y);
            this.pageId = pageId;
        }

        @Override
        public boolean isActive() {
            return page == pageId;
        }
    }

    private class PageItemSlot extends SlotItemHandler {
        private final int pageId;

        PageItemSlot(IItemHandler h, int i, int x, int y, int pageId) {
            super(h, i, x, y);
            this.pageId = pageId;
        }

        @Override
        public boolean isActive() {
            return page == pageId;
        }
    }

    // ------------------------------------------------------------------ pages

    public boolean hasUpgrade(UpgradeItem.Kind k) {
        for (int i = 0; i < tier.upgrades; i++) {
            if (slots.get(upgradeStart + i).getItem().getItem() instanceof UpgradeItem u && u.kind == k) return true;
        }
        return false;
    }

    public boolean pageAvailable(int p) {
        return switch (p) {
            case PAGE_CRAFT -> hasUpgrade(UpgradeItem.Kind.CRAFTING);
            case PAGE_SMELT -> hasUpgrade(UpgradeItem.Kind.SMELTING);
            case PAGE_TANK -> hasUpgrade(UpgradeItem.Kind.TANK);
            default -> true;
        };
    }

    public int firstPage() {
        for (int p = PAGE_CRAFT; p <= PAGE_TANK; p++) if (pageAvailable(p)) return p;
        return PAGE_NONE;
    }

    public int burn() {
        return data.get(0);
    }

    public int burnMax() {
        return data.get(1);
    }

    public int cook() {
        return data.get(2);
    }

    public int tankAmount() {
        return data.get(3);
    }

    @Nullable
    public Fluid tankFluid() {
        int f = data.get(4);
        return f < 0 ? null : BuiltInRegistries.FLUID.byId(f);
    }

    @Override
    public boolean clickMenuButton(@Nonnull Player p, int id) {
        if (id >= PAGE_CRAFT && id <= PAGE_TANK && pageAvailable(id)) {
            page = id;
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ crafting

    @Override
    public void slotsChanged(@Nonnull Container c) {
        if (c == craftGrid && contents != null && player instanceof ServerPlayer sp) {
            for (int i = 0; i < 9; i++) contents.craft.setStackInSlot(i, craftGrid.getItem(i).copy());
            contents.changed();
            ItemStack out = ItemStack.EMPTY;
            Optional<CraftingRecipe> r = sp.server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftGrid, sp.level());
            if (r.isPresent() && craftResult.setRecipeUsed(sp.level(), sp, r.get())) out = r.get().assemble(craftGrid, sp.level().registryAccess());
            craftResult.setItem(0, out);
        }
        super.slotsChanged(c);
    }

    private void handleBuckets() {
        if (contents == null) return;
        ItemStack in = contents.buckets.getStackInSlot(0);
        if (in.isEmpty() || !contents.buckets.getStackInSlot(1).isEmpty()) return;
        ItemStack one = in.copyWithCount(1);
        FluidActionResult r = FluidUtil.tryEmptyContainer(one, contents.tank, Integer.MAX_VALUE, null, true);
        if (!r.isSuccess()) r = FluidUtil.tryFillContainer(one, contents.tank, Integer.MAX_VALUE, null, true);
        if (r.isSuccess()) {
            in.shrink(1);
            contents.buckets.setStackInSlot(0, in.isEmpty() ? ItemStack.EMPTY : in);
            contents.buckets.setStackInSlot(1, r.getResult());
            contents.changed();
        }
    }

    // ------------------------------------------------------------------ open / validity

    @Nullable
    private ItemStack sourceStack(Player p) {
        return switch (source.kind()) {
            case Source.INV -> p.getInventory().getItem(source.index());
            case Source.OFFHAND -> p.getOffhandItem();
            case Source.CHEST -> p.getItemBySlot(EquipmentSlot.CHEST);
            default -> null;
        };
    }

    @Override
    public boolean stillValid(@Nonnull Player p) {
        if (id == null) return true;
        if (source.kind() == Source.BLOCK) {
            return p.level().getBlockEntity(source.pos()) instanceof BackpackBE be && id.equals(be.id())
                    && p.distanceToSqr(source.pos().getX() + 0.5, source.pos().getY() + 0.5, source.pos().getZ() + 0.5) < 64;
        }
        ItemStack s = sourceStack(p);
        return s != null && s.getItem() instanceof BackpackItem && id.equals(BackpackItem.peekId(s));
    }

    @Override
    public void clicked(int slot, int button, @Nonnull ClickType type, @Nonnull Player p) {
        // number-key swaps could move the open backpack out of its slot
        if (type == ClickType.SWAP && source.kind() == Source.INV && button == source.index()) return;
        if (type == ClickType.SWAP && source.kind() == Source.OFFHAND && button == 40) return;
        super.clicked(slot, button, type, p);
    }

    public static void open(ServerPlayer sp, ItemStack stack, Source src) {
        if (!(stack.getItem() instanceof BackpackItem bi)) return;
        UUID id = BackpackItem.id(stack);
        Contents c = BackpackStore.get(sp.server).contents(id, bi.tier.slots());
        Component title = stack.hasCustomHoverName() ? stack.getHoverName() : Component.translatable(stack.getDescriptionId());
        NetworkHooks.openScreen(sp, new SimpleMenuProvider((wid, inv, p) -> new BackpackMenu(wid, inv, bi.tier, src, id, c), title), buf -> {
            buf.writeByte(bi.tier.ordinal());
            src.write(buf);
        });
    }

    public static void openBlock(ServerPlayer sp, BackpackBE be) {
        Contents c = BackpackStore.get(sp.server).contents(be.id(), be.tier().slots());
        Source src = Source.block(be.getBlockPos());
        Component title = be.name() != null ? be.name() : Component.translatable("item.ls_backpacks." + be.tier().id() + "_backpack");
        NetworkHooks.openScreen(sp, new SimpleMenuProvider((wid, inv, p) -> new BackpackMenu(wid, inv, be.tier(), src, be.id(), c), title), buf -> {
            buf.writeByte(be.tier().ordinal());
            src.write(buf);
        });
    }

    // ------------------------------------------------------------------ shift-click

    @Nonnull
    @Override
    public ItemStack quickMoveStack(@Nonnull Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(p)) return ItemStack.EMPTY;
        ItemStack st = slot.getItem();
        ItemStack copy = st.copy();
        int invEnd = slots.size();
        if (index == resultSlot) {
            st.getItem().onCraftedBy(st, p.level(), p);
            if (!moveItemStackTo(st, invStart, invEnd, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(st, copy);
        } else if (index >= invStart) {
            boolean moved = false;
            if (st.getItem() instanceof UpgradeItem) moved = moveItemStackTo(st, upgradeStart, upgradeStart + tier.upgrades, false);
            if (!moved && page == PAGE_SMELT && ForgeHooks.getBurnTime(st, RecipeType.SMELTING) > 0) moved = moveItemStackTo(st, smeltStart + 1, smeltStart + 2, false);
            if (!moved) moved = moveItemStackTo(st, mainStart, mainEnd, false);
            if (!moved) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(st, invStart, invEnd, true)) return ItemStack.EMPTY;
        }
        if (st.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (st.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(p, st);
        return copy;
    }

    @Override
    public void removed(@Nonnull Player p) {
        super.removed(p);
        if (contents != null) {
            for (int i = 0; i < 9; i++) contents.craft.setStackInSlot(i, craftGrid.getItem(i).copy());
            contents.changed();
        }
    }
}
