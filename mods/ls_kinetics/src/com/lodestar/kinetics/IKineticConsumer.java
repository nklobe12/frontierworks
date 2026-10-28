package com.lodestar.kinetics;

import net.minecraft.core.Direction;

/**
 * A machine turned by a shaft that ends against one of its faces. Consumers are not network members themselves: the
 * network finds them at its shaft ends when it is built, adds their torque to its load and turns them every tick.
 */
public interface IKineticConsumer {
    /** Whether a shaft ending against this face (the machine's own side) drives the machine. */
    boolean acceptsRotation(Direction side);

    /** Torque in Nm the machine loads its network with while it is attached (at its own shaft speed). */
    float torqueDemand();

    /**
     * Called every tick by the driving network. rpm is the shaft speed, signed along the direction pointing into the
     * machine (right-hand rule); status is one of the {@link KineticBE} status codes.
     */
    void setRotation(Direction side, float rpm, byte status);
}
