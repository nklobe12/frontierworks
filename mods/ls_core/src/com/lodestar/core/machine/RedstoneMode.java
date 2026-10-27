package com.lodestar.core.machine;

/** Whether a machine runs regardless of redstone, only when powered, or only when unpowered. */
public enum RedstoneMode {
    IGNORE, HIGH, LOW;

    public boolean allows(boolean powered) {
        return this == IGNORE || (this == HIGH) == powered;
    }

    public RedstoneMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
