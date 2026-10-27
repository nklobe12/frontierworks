package com.lodestar.metalwork;

import com.lodestar.core.machine.MachineBlockEntity;
import com.lodestar.core.machine.MachineLayout;
import com.lodestar.core.machine.MachineMenu;
import com.lodestar.core.multiblock.MultiblockHelper;
import com.lodestar.core.multiblock.MultiblockPattern;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Bloomery: a chimney of fire bricks with this controller at its foot. Charcoal and iron ore smoulder inside for
 * half a minute and leave a spongy steel bloom, which must be hammered on the smithing anvil while hot to drive
 * out the slag. Bellows facing the controller speed it up.
 */
public class BloomeryBE extends MachineBlockEntity {
    public static final MachineLayout LAYOUT = MachineLayout.builder()
            .slot(44, 17, MachineLayout.Role.INPUT).slot(44, 53, MachineLayout.Role.FUEL).flame(45, 36).arrow(76, 34)
            .slot(116, 35, MachineLayout.Role.OUTPUT).info(8, 72).height(176).build();
    public static final TagKey<Item> ORE = ItemTags.create(new net.minecraft.resources.ResourceLocation("ls_metalwork", "bloomery_ore"));
    public static final TagKey<Item> FUEL = ItemTags.create(new net.minecraft.resources.ResourceLocation("ls_metalwork", "bloomery_fuel"));
    public static final int TIME = 600;

    private static MultiblockPattern pattern;
    private final MultiblockHelper mb;
    private int boost;

    public static MultiblockPattern pattern() {
        if (pattern == null) {
            pattern = MultiblockPattern.builder("ls_metalwork:bloomery")
                    .layer("BBB", "BBB", "BCB")
                    .layer("BBB", "B_B", "BBB")
                    .layer("BBB", "B_B", "BBB")
                    .layer(" B ", "B_B", " B ")
                    .key('B', () -> Content.FIRE_BRICKS.get())
                    .build();
        }
        return pattern;
    }

    public BloomeryBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, LAYOUT, 0, 0, 0);
        this.mb = new MultiblockHelper(pattern(), 40);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        MachineLayout.Role r = layout.slots.get(slot).role;
        if (r == MachineLayout.Role.INPUT) return stack.is(ORE);
        if (r == MachineLayout.Role.FUEL) return stack.is(FUEL);
        return super.isItemValid(slot, stack);
    }

    public void blow() {
        boost = Math.min(300, boost + 60);
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        mb.tick(level, worldPosition, facing());
        maxProgress = TIME;
        boolean active = false;
        if (mb.formed() && canRun()) {
            ItemStack ore = items.getStackInSlot(0);
            ItemStack fuel = items.getStackInSlot(1);
            if (progress == 0 && ore.is(ORE) && fuel.is(FUEL) && fuel.getCount() >= 2 && insertOutput(new ItemStack(Content.STEEL_BLOOM.get()), true).isEmpty()) {
                items.extractItem(0, 1, false);
                items.extractItem(1, 2, false);
                progress = 1;
                burn = maxBurn = TIME;
            } else if (progress > 0) {
                progress += boost > 0 ? 2 : 1;
                burn = Math.max(0, TIME - progress);
                active = true;
                if (progress >= TIME) {
                    ItemStack bloom = new ItemStack(Content.STEEL_BLOOM.get());
                    Heat.set(bloom, 1250);
                    insertOutput(bloom, false);
                    progress = 0;
                    burn = 0;
                }
            }
        }
        if (boost > 0) boost--;
        if (active && level instanceof ServerLevel sl && level.random.nextInt(3) == 0) {
            BlockPos top = mb.match() != null ? worldPosition.relative(facing().getOpposite()).above(3) : worldPosition.above();
            sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, top.getX() + 0.5, top.getY() + 1.0, top.getZ() + 0.5, 1, 0.1, 0.1, 0.1, 0.01);
            sl.sendParticles(ParticleTypes.FLAME, top.getX() + 0.5, top.getY() + 0.5, top.getZ() + 0.5, 1, 0.1, 0.1, 0.1, 0.005);
        }
        setLit(active);
        setChanged();
    }

    @Override
    public int getExtra(int i) {
        return mb.formed() ? 1 : 0;
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        if (menu.extra(0) == 0) {
            lines.add(Component.translatable("gui.ls_metalwork.bloomery.incomplete").withStyle(ChatFormatting.DARK_RED));
        } else {
            lines.add(Component.translatable("gui.ls_metalwork.bloomery.ready").withStyle(ChatFormatting.DARK_GREEN));
        }
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        mb.save(tag);
        tag.putInt("Boost", boost);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        mb.load(tag);
        boost = tag.getInt("Boost");
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) mb.release(level);
        super.setRemoved();
    }
}
