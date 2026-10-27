package com.frontierworks.food;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

public final class FoodHooks {
    private FoodHooks() {}

    public static void init(IEventBus bus) {
        bus.addListener(FoodHooks::setup);
    }

    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            for (String crop : new String[]{"tomato", "corn", "onion", "chili_pepper"}) {
                compost(crop + "_seeds", 0.3f);
                compost(crop, 0.65f);
            }
        });
    }

    private static void compost(String name, float chance) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(FoodMod.MODID, name));
        if (item != null) ComposterBlock.COMPOSTABLES.put(item, chance);
    }
}
