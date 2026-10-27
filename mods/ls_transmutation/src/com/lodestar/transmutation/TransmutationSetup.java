package com.lodestar.transmutation;

import com.lodestar.core.data.PlayerData;
import com.lodestar.core.net.CoreNet;
import com.lodestar.core.recipe.display.LensRegistry;
import com.lodestar.core.recipe.display.RecipeCategory;
import com.lodestar.core.recipe.display.RecipeView;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/** Hand-written registrations and events for Transmutation. */
public final class TransmutationSetup {
    private TransmutationSetup() {}

    public static final RegistryObject<MenuType<ExchangeMenu>> EXCHANGE_MENU = Content.REG.menu("exchange", ExchangeMenu::new);

    public static void init(IEventBus bus) {
        PlayerData.autoSync(Knowledge.KEY);
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent e) -> AetherValues.compute(e.getServer()));
        MinecraftForge.EVENT_BUS.addListener(TransmutationSetup::datapackSync);
        MinecraftForge.EVENT_BUS.addListener(TransmutationSetup::playerTick);
        CoreNet.registerTag("ls_transmutation:withdraw", TransmutationSetup::withdraw);
        CoreNet.register("ls_transmutation:charge", (p, d) -> MatterTools.cycleCharge(p, p.getMainHandItem()));
        ResourceLocation cat = new ResourceLocation("ls_transmutation", "world_transmutation");
        LensRegistry.register(new RecipeCategory(cat, Component.translatable("lens.category.ls_transmutation.world_transmutation"),
                () -> new ItemStack(Content.TRANSMUTERS_STONE.get()), () -> List.of(new ItemStack(Content.TRANSMUTERS_STONE.get())), 400), level -> {
            List<RecipeView> out = new ArrayList<>();
            for (Block[] cycle : TransmuterStoneItem.CYCLES) {
                for (int i = 0; i < cycle.length; i++) {
                    Block to = cycle[(i + 1) % cycle.length];
                    if (cycle[i].asItem() == Items.AIR || to.asItem() == Items.AIR) continue;
                    out.add(RecipeView.builder(cat, 110, 24).input(4, 4, new ItemStack(cycle[i]))
                            .catalyst(28, 4, List.of(new ItemStack(Content.TRANSMUTERS_STONE.get())), Component.translatable("lens.ls_core.not_consumed"))
                            .arrow(50, 4).output(82, 4, new ItemStack(to)).build());
                }
            }
            return out;
        });
    }

    private static void datapackSync(OnDatapackSyncEvent e) {
        if (e.getPlayer() == null) {
            AetherValues.compute(e.getPlayerList().getServer());
            for (ServerPlayer p : e.getPlayerList().getPlayers()) AetherValues.send(p);
        } else {
            if (AetherValues.size() == 0) AetherValues.compute(e.getPlayerList().getServer());
            AetherValues.send(e.getPlayer());
        }
    }

    private static void playerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase == TickEvent.Phase.END && !e.player.level().isClientSide && e.player.tickCount % 20 == 0) MatterArmor.tick(e.player);
    }

    private static void withdraw(ServerPlayer p, net.minecraft.nbt.CompoundTag tag) {
        if (!(p.containerMenu instanceof ExchangeMenu)) return;
        Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(tag.getString("Item")));
        if (item == Items.AIR || !Knowledge.knows(p, item)) return;
        long v = AetherValues.get(item);
        if (v <= 0) return;
        int want = Math.max(1, Math.min(64, tag.getInt("Count")));
        int n = (int) Math.min(want, Math.min(item.getMaxStackSize(), Knowledge.aether(p) / v));
        if (n <= 0) {
            p.displayClientMessage(Component.translatable("msg.ls_transmutation.too_poor").withStyle(ChatFormatting.RED), true);
            return;
        }
        ItemStack st = new ItemStack(item, n);
        Knowledge.setAether(p, Knowledge.aether(p) - v * n);
        if (!p.getInventory().add(st)) p.drop(st, false);
        Knowledge.sync(p);
    }
}
