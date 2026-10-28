package com.lodestar.kinetics;

import com.lodestar.core.machine.MachineLayout;
import com.lodestar.core.machine.MachineMenu;
import com.lodestar.core.machine.MachineSpec;
import com.lodestar.core.machine.ProcessJob;
import com.lodestar.core.machine.ProcessingMachineBE;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;

/**
 * A machine worked by a shaft instead of energy. Work done per tick equals the shaft speed in RPM; a recipe's time is
 * its duration at 32 RPM, so a mill on a 64 RPM shaft grinds twice as fast. The machine loads its network with a fixed
 * torque while attached. The mixing vat also checks for heat below it for heated recipes.
 */
public class KineticMachineBE extends ProcessingMachineBE implements IKineticConsumer {
    public static final float REFERENCE_RPM = 32;
    public static final TagKey<net.minecraft.world.level.block.Block> HEATERS = BlockTags.create(new ResourceLocation("ls_kinetics", "vat_heaters"));

    public enum Kind {
        MILL(48), PRESS(64), VAT(64);
        public final float torque;

        Kind(float torque) {
            this.torque = torque;
        }
    }

    public final Kind kind;
    private float rpm;
    private byte driveStatus = KineticBE.OK;
    private long lastDrive = -10;
    private boolean heated;
    private ResourceLocation lastJob;
    private float syncedRpm;
    private int syncedFluid = -1;
    /** client: animation angle */
    public float clientAngle, clientAnglePrev;
    /** client: number of finished jobs seen (press stroke / mill puff) */
    public int strokes;

    public KineticMachineBE(BlockEntityType<?> type, BlockPos pos, BlockState state, MachineSpec spec, Kind kind) {
        super(type, pos, state, spec);
        this.kind = kind;
    }

    // ------------------------------------------------------------------ rotation

    @Override
    public boolean acceptsRotation(Direction side) {
        return switch (kind) {
            case MILL -> side.getAxis() == Direction.Axis.Y;
            case PRESS, VAT -> side == Direction.UP;
        };
    }

    @Override
    public float torqueDemand() {
        return kind.torque;
    }

    @Override
    public void setRotation(Direction side, float rpm, byte status) {
        this.rpm = rpm;
        this.driveStatus = status;
        if (level != null) lastDrive = level.getGameTime();
    }

    public float rpm() {
        return rpm;
    }

    public boolean heated() {
        return heated;
    }

    @Override
    public boolean hasSideConfig() {
        return true;
    }

    // ------------------------------------------------------------------ work

    private boolean checkHeat() {
        if (kind != Kind.VAT || level == null) return false;
        BlockState below = level.getBlockState(worldPosition.below());
        if (!below.is(HEATERS)) return false;
        return !below.hasProperty(BlockStateProperties.LIT) || below.getValue(BlockStateProperties.LIT);
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        long now = level.getGameTime();
        if (now - lastDrive > 1) {
            rpm = 0;
            driveStatus = KineticBE.OK;
        }
        if (kind == Kind.VAT && now % 20 == Math.floorMod(worldPosition.hashCode(), 20)) {
            boolean h = checkHeat();
            if (h != heated) {
                heated = h;
                inventoryDirty = true;
                sync();
            }
        }
        if (inventoryDirty) {
            inventoryDirty = false;
            ProcessJob found = spec.recipes.find(level, this, inputSlots);
            if (found == null) {
                job = null;
                progress = 0;
                lastJob = null;
            } else {
                if (lastJob == null || !lastJob.equals(found.recipeId)) progress = 0;
                job = found;
                lastJob = found.recipeId;
                maxProgress = Math.max(1, Math.round(found.time * REFERENCE_RPM));
            }
        }
        int work = Math.round(Math.abs(rpm));
        boolean active = false;
        if (job != null && work > 0 && canRun() && canFinish(job)) {
            progress += work;
            active = true;
            if (progress >= maxProgress) {
                finish(job);
                progress = 0;
                inventoryDirty = true;
                playFinish();
            }
            setChanged();
        }
        rate = 0;
        if (Math.abs(rpm - syncedRpm) > 0.5f || (kind == Kind.VAT && now % 10 == 0 && fluidHash() != syncedFluid)) {
            syncedRpm = rpm;
            syncedFluid = fluidHash();
            sync();
        }
    }

    private int fluidHash() {
        int h = 0;
        for (var t : tanks) h = h * 31 + t.getFluidAmount() / 50 + 7 * t.getFluid().getFluid().hashCode();
        return h;
    }

    private void playFinish() {
        switch (kind) {
            case MILL -> level.playSound(null, worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.4f, 1.1f);
            case PRESS -> level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.25f, 1.6f);
            case VAT -> level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.4f, 1.2f);
        }
        level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0);
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == 1) {
            strokes++;
            return true;
        }
        return super.triggerEvent(id, param);
    }

    @Override
    protected void onInventoryChanged(int slot) {
        super.onInventoryChanged(slot);
        if (layout.slots.get(slot).role == MachineLayout.Role.INPUT) sync();
    }

    @Override
    public void clientTick() {
        clientAnglePrev = clientAngle;
        clientAngle = (float) ((clientAngle + rpm * KineticBE.DEG_PER_TICK) % 360.0);
    }

    // ------------------------------------------------------------------ screen

    @Override
    public int getExtra(int i) {
        return switch (i) {
            case 0 -> Math.round(rpm * 10);
            case 1 -> driveStatus;
            case 2 -> heated ? 1 : 0;
            default -> 0;
        };
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        float r = Math.abs(menu.extra(0) / 10f);
        int st = menu.extra(1);
        MutableComponent c;
        if (st == KineticBE.OVERLOADED) c = Component.translatable("gui.ls_kinetics.overloaded").withStyle(ChatFormatting.RED);
        else if (st == KineticBE.JAMMED) c = Component.translatable("gui.ls_kinetics.jammed").withStyle(ChatFormatting.RED);
        else if (r <= 0) c = Component.translatable("gui.ls_kinetics.no_drive");
        else c = Component.translatable("gui.ls_kinetics.rpm", KineticBE.fmt(r));
        if (kind == Kind.VAT && menu.extra(2) == 1) c.append(Component.literal("  ")).append(Component.translatable("gui.ls_kinetics.heated"));
        lines.add(c);
    }

    // ------------------------------------------------------------------ sync & save

    /** Client copy of the input stacks, for the renderer. */
    public ItemStack shownInput(int i) {
        int[] in = layout.indices(MachineLayout.Role.INPUT);
        return i < in.length ? items.getStackInSlot(in[i]) : ItemStack.EMPTY;
    }

    @Override
    protected void saveSyncExtra(CompoundTag tag) {
        tag.putFloat("Rpm", rpm);
        tag.putBoolean("Heated", heated);
        ListTag l = new ListTag();
        for (int s : layout.indices(MachineLayout.Role.INPUT)) l.add(items.getStackInSlot(s).save(new CompoundTag()));
        tag.put("Shown", l);
    }

    @Override
    protected void loadSyncExtra(CompoundTag tag) {
        rpm = tag.getFloat("Rpm");
        heated = tag.getBoolean("Heated");
        ListTag l = tag.getList("Shown", 10);
        int[] in = layout.indices(MachineLayout.Role.INPUT);
        for (int i = 0; i < in.length && i < l.size(); i++) items.setStackInSlot(in[i], ItemStack.of(l.getCompound(i)));
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        super.saveExtra(tag);
        tag.putBoolean("Heated", heated);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        super.loadExtra(tag);
        heated = tag.getBoolean("Heated");
    }
}
