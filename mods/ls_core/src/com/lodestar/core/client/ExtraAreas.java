package com.lodestar.core.client;

import net.minecraft.client.renderer.Rect2i;

import java.util.List;

/** Implemented by screens that draw outside their main panel, so overlays (Lens) can avoid those areas. */
public interface ExtraAreas {
    List<Rect2i> extraAreas();
}
