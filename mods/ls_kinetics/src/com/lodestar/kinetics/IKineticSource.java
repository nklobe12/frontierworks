package com.lodestar.kinetics;

/** A network member that drives its network: a hand wheel, paddle wheel, windmill or motor. */
public interface IKineticSource {
    /** The speed this source wants to turn at, RPM in its own frame (0 while idle). */
    float sourceSpeed();

    /** Torque in Nm this source can deliver at its own shaft. */
    float sourceTorque();

    /** Called every tick after the network solved: the fraction of its torque in use and whether the network turns. */
    default void networkLoad(float utilization, boolean running) {}
}
