package com.lodestar.toolsmith;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

import java.util.List;
import java.util.Set;

/** The kinds of modular tools: which parts they take and how they behave. */
public enum ToolType {
    PICKAXE("pickaxe", List.of(PartType.PICK_HEAD, PartType.TOOL_HANDLE, PartType.TOOL_BINDING), false, 1.0f, -2.8f,
            List.of(BlockTags.MINEABLE_WITH_PICKAXE), ToolActions.DEFAULT_PICKAXE_ACTIONS, 1.0f, 1.0f),
    AXE("axe", List.of(PartType.AXE_HEAD, PartType.TOOL_HANDLE, PartType.TOOL_BINDING), false, 5.0f, -3.0f,
            List.of(BlockTags.MINEABLE_WITH_AXE), ToolActions.DEFAULT_AXE_ACTIONS, 1.0f, 1.0f),
    SHOVEL("shovel", List.of(PartType.SHOVEL_HEAD, PartType.TOOL_HANDLE, PartType.TOOL_BINDING), false, 1.5f, -3.0f,
            List.of(BlockTags.MINEABLE_WITH_SHOVEL), ToolActions.DEFAULT_SHOVEL_ACTIONS, 1.0f, 1.0f),
    SWORD("sword", List.of(PartType.SWORD_BLADE, PartType.TOOL_HANDLE, PartType.CROSS_GUARD), false, 3.0f, -2.4f,
            List.of(), ToolActions.DEFAULT_SWORD_ACTIONS, 1.1f, 1.0f),
    KAMA("kama", List.of(PartType.KAMA_HEAD, PartType.TOOL_HANDLE, PartType.TOOL_BINDING), false, 1.0f, -1.0f,
            List.of(BlockTags.MINEABLE_WITH_HOE, BlockTags.LEAVES), ToolActions.DEFAULT_HOE_ACTIONS, 1.0f, 1.0f),
    HAMMER("hammer", List.of(PartType.HAMMER_HEAD, PartType.TOUGH_HANDLE, PartType.TOUGH_BINDING), true, 6.0f, -3.4f,
            List.of(BlockTags.MINEABLE_WITH_PICKAXE), ToolActions.DEFAULT_PICKAXE_ACTIONS, 2.5f, 0.45f),
    BROAD_AXE("broad_axe", List.of(PartType.BROAD_AXE_HEAD, PartType.TOUGH_HANDLE, PartType.TOUGH_BINDING), true, 8.0f, -3.5f,
            List.of(BlockTags.MINEABLE_WITH_AXE), ToolActions.DEFAULT_AXE_ACTIONS, 2.5f, 0.6f),
    EXCAVATOR("excavator", List.of(PartType.EXCAVATOR_HEAD, PartType.TOUGH_HANDLE, PartType.TOUGH_BINDING), true, 3.0f, -3.4f,
            List.of(BlockTags.MINEABLE_WITH_SHOVEL), ToolActions.DEFAULT_SHOVEL_ACTIONS, 2.5f, 0.5f),
    SCYTHE("scythe", List.of(PartType.SCYTHE_BLADE, PartType.TOUGH_HANDLE, PartType.TOUGH_BINDING), true, 4.0f, -3.0f,
            List.of(BlockTags.MINEABLE_WITH_HOE, BlockTags.LEAVES), ToolActions.DEFAULT_HOE_ACTIONS, 2.5f, 0.8f);

    public final String id;
    public final List<PartType> parts;
    public final boolean large;
    public final float baseAttack;
    public final float attackSpeed;
    public final List<TagKey<Block>> mineable;
    public final Set<ToolAction> actions;
    public final float durabilityFactor;
    public final float speedFactor;

    ToolType(String id, List<PartType> parts, boolean large, float baseAttack, float attackSpeed, List<TagKey<Block>> mineable,
             Set<ToolAction> actions, float durabilityFactor, float speedFactor) {
        this.id = id;
        this.parts = parts;
        this.large = large;
        this.baseAttack = baseAttack;
        this.attackSpeed = attackSpeed;
        this.mineable = mineable;
        this.actions = actions;
        this.durabilityFactor = durabilityFactor;
        this.speedFactor = speedFactor;
    }

    /** Tool type whose part list matches the given parts in any order, or null. */
    public static ToolType forParts(List<PartType> given) {
        for (ToolType t : values()) {
            if (t.parts.size() != given.size()) continue;
            List<PartType> rest = new java.util.ArrayList<>(given);
            boolean ok = true;
            for (PartType p : t.parts) {
                if (!rest.remove(p)) {
                    ok = false;
                    break;
                }
            }
            if (ok) return t;
        }
        return null;
    }
}
