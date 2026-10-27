package com.lodestar.veins;

import com.lodestar.core.machine.MachineBlockEntity;
import com.lodestar.core.machine.MachineLayout;
import com.lodestar.core.machine.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.common.Tags;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Seismic Surveyor: sends charges into the ground and listens. Scans every 16-block section of the 5x5 chunks
 * around it, one section per tick while it has power, then prints a Survey Report on a sheet of paper showing
 * how much of each ore lies at each depth.
 */
public class SurveyorBE extends MachineBlockEntity {
    public static final MachineLayout LAYOUT = MachineLayout.builder()
            .energy(8, 17, 52).slot(44, 35, MachineLayout.Role.INPUT).arrow(70, 34).slot(108, 35, MachineLayout.Role.OUTPUT)
            .slot(152, 17, MachineLayout.Role.UPGRADE).slot(152, 35, MachineLayout.Role.UPGRADE).info(36, 58).build();
    public static final int RADIUS = 2;
    public static final int COST_PER_SECTION = 80;

    private boolean scanning;
    private int index;
    private int sectionsPerColumn;
    private final Map<String, int[]> counts = new HashMap<>();

    public SurveyorBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, LAYOUT, 40000, 2000, 0);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (layout.slots.get(slot).role == MachineLayout.Role.INPUT) return stack.is(Items.PAPER);
        return super.isItemValid(slot, stack);
    }

    private int totalSections() {
        int side = RADIUS * 2 + 1;
        return side * side * Math.max(1, sectionsPerColumn);
    }

    @Override
    public void serverTick() {
        if (level == null || energy == null) return;
        sectionsPerColumn = level.getSectionsCount();
        if (!scanning) {
            if (canRun() && items.getStackInSlot(0).is(Items.PAPER) && items.getStackInSlot(1).isEmpty()) {
                scanning = true;
                index = 0;
                counts.clear();
                setChanged();
            }
            progress = 0;
            maxProgress = totalSections();
            setLit(false);
            return;
        }
        maxProgress = totalSections();
        int cost = (int) Math.round(COST_PER_SECTION * energyMultiplier());
        int steps = (int) Math.max(1, Math.round(1 / speedMultiplier()));
        boolean worked = false;
        for (int s = 0; s < steps && scanning; s++) {
            if (!canRun() || energy.getEnergyStored() < cost) break;
            energy.consume(cost);
            scanSection(index++);
            worked = true;
            if (index >= totalSections()) finish();
        }
        rate = worked ? cost * steps : 0;
        progress = index;
        setLit(worked);
        setChanged();
    }

    private void scanSection(int i) {
        int side = RADIUS * 2 + 1;
        int column = i / sectionsPerColumn, sec = i % sectionsPerColumn;
        int cx = (worldPosition.getX() >> 4) - RADIUS + column % side;
        int cz = (worldPosition.getZ() >> 4) - RADIUS + column / side;
        ChunkAccess chunk = level.getChunk(cx, cz);
        LevelChunkSection section = chunk.getSections()[sec];
        if (section.hasOnlyAir() || !section.maybeHas(st -> st.is(Tags.Blocks.ORES))) return;
        for (int y = 0; y < 16; y++)
            for (int z = 0; z < 16; z++)
                for (int x = 0; x < 16; x++) {
                    String n = OreIndex.oreName(section.getBlockState(x, y, z));
                    if (n != null) counts.computeIfAbsent(n, k -> new int[sectionsPerColumn])[sec]++;
                }
    }

    private void finish() {
        scanning = false;
        ItemStack report = new ItemStack(Content.SURVEY_REPORT.get());
        CompoundTag tag = report.getOrCreateTag();
        tag.putInt("X", worldPosition.getX());
        tag.putInt("Z", worldPosition.getZ());
        tag.putInt("MinY", level.getMinBuildHeight());
        tag.putString("Dim", level.dimension().location().toString());
        CompoundTag ores = new CompoundTag();
        counts.forEach((k, v) -> ores.putIntArray(k, v));
        tag.put("Ores", ores);
        items.extractItem(0, 1, false);
        items.setStackInSlot(1, report);
        counts.clear();
        index = 0;
    }

    @Override
    public void addInfo(List<Component> lines, MachineMenu menu) {
        int max = menu.data(D_MAX_PROGRESS), p = menu.data(D_PROGRESS);
        if (max > 0 && p > 0) lines.add(Component.translatable("gui.ls_veins.scanning", p * 100 / max));
        else lines.add(Component.translatable("gui.ls_veins.insert_paper"));
    }

    @Override
    protected void saveExtra(CompoundTag tag) {
        tag.putBoolean("Scanning", scanning);
        tag.putInt("Index", index);
        CompoundTag c = new CompoundTag();
        counts.forEach((k, v) -> c.putIntArray(k, v));
        tag.put("Counts", c);
    }

    @Override
    protected void loadExtra(CompoundTag tag) {
        scanning = tag.getBoolean("Scanning");
        index = tag.getInt("Index");
        counts.clear();
        CompoundTag c = tag.getCompound("Counts");
        for (String k : c.getAllKeys()) counts.put(k, c.getIntArray(k));
    }
}
