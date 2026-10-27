package com.lodestar.toolsmith;

import net.minecraft.network.chat.Component;

/** A tool part shape and how many units of material it costs (1 unit = 1 item or 144 mB of molten metal). */
public enum PartType {
    PICK_HEAD("pick_head", 2, Role.HEAD),
    AXE_HEAD("axe_head", 2, Role.HEAD),
    SHOVEL_HEAD("shovel_head", 1, Role.HEAD),
    SWORD_BLADE("sword_blade", 2, Role.HEAD),
    KAMA_HEAD("kama_head", 2, Role.HEAD),
    HAMMER_HEAD("hammer_head", 8, Role.HEAD),
    BROAD_AXE_HEAD("broad_axe_head", 8, Role.HEAD),
    EXCAVATOR_HEAD("excavator_head", 8, Role.HEAD),
    SCYTHE_BLADE("scythe_blade", 8, Role.HEAD),
    TOOL_HANDLE("tool_handle", 1, Role.HANDLE),
    TOUGH_HANDLE("tough_handle", 3, Role.HANDLE),
    TOOL_BINDING("tool_binding", 1, Role.BINDING),
    TOUGH_BINDING("tough_binding", 3, Role.BINDING),
    CROSS_GUARD("cross_guard", 1, Role.BINDING);

    public enum Role { HEAD, HANDLE, BINDING }

    public final String id;
    public final int cost;
    public final Role role;

    PartType(String id, int cost, Role role) {
        this.id = id;
        this.cost = cost;
        this.role = role;
    }

    public Component displayName() {
        return Component.translatable("part.ls_toolsmith." + id);
    }

    public static PartType byId(String id) {
        for (PartType p : values()) if (p.id.equals(id)) return p;
        return null;
    }
}
