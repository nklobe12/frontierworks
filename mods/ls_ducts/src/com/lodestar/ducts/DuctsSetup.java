package com.lodestar.ducts;

import com.lodestar.core.net.CoreNet;
import com.lodestar.ducts.transit.Transit;
import com.lodestar.ducts.transit.TransitPod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

public final class DuctsSetup {
    public static final RegistryObject<MenuType<FilterMenu>> FILTER_MENU = Content.REG.menu("filter", FilterMenu::new);
    public static final RegistryObject<EntityType<TransitPod>> TRANSIT_POD = Content.REG.entity("transit_pod",
            () -> EntityType.Builder.<TransitPod>of(TransitPod::new, MobCategory.MISC).sized(0.8f, 0.8f).clientTrackingRange(8).updateInterval(1).fireImmune());

    private DuctsSetup() {}

    public static void init(IEventBus bus) {
        CoreNet.registerTag("ls_ducts:travel", Transit::travel);
        if (com.lodestar.core.test.SelfTest.enabled()) DuctsTests.register();
        // riders cannot climb out of a pod between stations
        MinecraftForge.EVENT_BUS.addListener((EntityMountEvent e) -> {
            if (e.isDismounting() && e.getEntityBeingMounted() instanceof TransitPod pod && !pod.arrived && pod.isAlive()) e.setCanceled(true);
        });
    }

    public static Item attachmentItem(Attachment.Type type, boolean fluid) {
        return switch (type) {
            case SERVO -> fluid ? Content.FLUID_SERVO.get() : Content.SERVO.get();
            case FILTER -> fluid ? Content.FLUID_FILTER.get() : Content.FILTER.get();
            case RETRIEVER -> Content.RETRIEVER.get();
        };
    }
}
