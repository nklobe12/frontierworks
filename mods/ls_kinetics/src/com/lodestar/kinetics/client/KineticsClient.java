package com.lodestar.kinetics.client;

import com.lodestar.core.client.RenderUtil;
import com.lodestar.kinetics.Content;
import com.lodestar.kinetics.KineticBE;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.List;

public final class KineticsClient {
    private static final List<String> MODELS = List.of("axle_rotor", "spur_gear_rotor", "pulley_rotor", "shaft_stub", "hand_wheel_rotor",
            "paddle_wheel_rotor", "windmill_rotor", "windmill_vane", "grist_mill_stone", "stamping_press_ram", "mixing_vat_paddle", "air_blower_rotor");

    private KineticsClient() {}

    public static void init(IEventBus bus) {
        for (String m : MODELS) RenderUtil.extraModel(KRender.model(m));
        bus.addListener(KineticsClient::renderers);
    }

    private static <T extends KineticBE> void rotor(EntityRenderersEvent.RegisterRenderers e, BlockEntityType<T> type, String model) {
        e.registerBlockEntityRenderer(type, (BlockEntityRendererProvider.Context ctx) -> new NodeRenderers.Rotor<T>(ctx, model));
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        rotor(e, Content.AXLE_BE.get(), "axle_rotor");
        rotor(e, Content.COUPLING_BE.get(), "axle_rotor");
        rotor(e, Content.TACHOMETER_BE.get(), "axle_rotor");
        rotor(e, Content.DYNAMOMETER_BE.get(), "axle_rotor");
        rotor(e, Content.SPUR_GEAR_BE.get(), "spur_gear_rotor");
        rotor(e, Content.PADDLE_WHEEL_BE.get(), "paddle_wheel_rotor");
        e.registerBlockEntityRenderer(Content.BEVEL_JUNCTION_BE.get(), NodeRenderers.Stubs::new);
        e.registerBlockEntityRenderer(Content.GEAR_TRAIN_BE.get(), NodeRenderers.Stubs::new);
        e.registerBlockEntityRenderer(Content.KINETIC_MOTOR_BE.get(), NodeRenderers.Stubs::new);
        e.registerBlockEntityRenderer(Content.HAND_WHEEL_BE.get(), NodeRenderers.HandWheel::new);
        e.registerBlockEntityRenderer(Content.PULLEY_BE.get(), NodeRenderers.Pulley::new);
        e.registerBlockEntityRenderer(Content.WINDMILL_HUB_BE.get(), NodeRenderers.Windmill::new);
        e.registerBlockEntityRenderer(Content.GRIST_MILL_BE.get(), MachineRenderers.Machine::new);
        e.registerBlockEntityRenderer(Content.STAMPING_PRESS_BE.get(), MachineRenderers.Machine::new);
        e.registerBlockEntityRenderer(Content.MIXING_VAT_BE.get(), MachineRenderers.Machine::new);
        e.registerBlockEntityRenderer(Content.AIR_BLOWER_BE.get(), MachineRenderers.Blower::new);
    }
}
