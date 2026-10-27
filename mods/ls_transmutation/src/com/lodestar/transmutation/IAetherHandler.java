package com.lodestar.transmutation;

/** Block entities that store aether and pass it between each other (collectors, relays, condensers). */
public interface IAetherHandler {
    long aether();

    long aetherCapacity();

    /** Accepts up to amount; returns how much was taken. */
    long receiveAether(long amount, boolean simulate);

    /** Whether this block hands aether on to neighbours (collectors and relays do, condensers keep it). */
    default boolean providesAether() {
        return false;
    }
}
