package com.lodestar.toolsmith;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/** Lookup of tool materials by id (the table itself is generated into MaterialTable). */
public final class Materials {
    private static final Map<String, ToolMaterial> BY_ID = new LinkedHashMap<>();
    public static final ToolMaterial FALLBACK = new ToolMaterial("unknown", 0x888888, 0, 50, 1f, 0f, 1f, 0, "none", "", "");

    static {
        for (ToolMaterial m : MaterialTable.ALL) BY_ID.put(m.id(), m);
    }

    private Materials() {}

    @Nullable
    public static ToolMaterial get(String id) {
        return BY_ID.get(id);
    }

    public static Iterable<ToolMaterial> all() {
        return BY_ID.values();
    }

    /** Material whose molten fluid id matches, or null. */
    @Nullable
    public static ToolMaterial byFluid(String fluidId) {
        for (ToolMaterial m : BY_ID.values()) if (m.fluid().equals(fluidId)) return m;
        return null;
    }
}
