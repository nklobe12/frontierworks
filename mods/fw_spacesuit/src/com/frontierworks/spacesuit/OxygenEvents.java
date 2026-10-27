package com.frontierworks.spacesuit;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.Set;

public final class OxygenEvents {
    public static final Set<String> AIRLESS = Set.of("fw_moon:moon", "fw_mars:mars");
    public static final int SUIT_MAX = 1800;

    private OxygenEvents() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(OxygenEvents::onTick);
        MinecraftForge.EVENT_BUS.addListener(OxygenEvents::onTooltip);
    }

    public static boolean isSuitChest(ItemStack s) {
        return s.getItem() instanceof ArmorItem a && a.getMaterial() == Content.SUIT_MAT && a.getType() == ArmorItem.Type.CHESTPLATE;
    }

    public static boolean fullSuit(Player p) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!(p.getItemBySlot(slot).getItem() instanceof ArmorItem a) || a.getMaterial() != Content.SUIT_MAT) return false;
        }
        return true;
    }

    public static boolean airless(Player p) {
        return AIRLESS.contains(p.level().dimension().location().toString());
    }

    private static void onTick(TickEvent.PlayerTickEvent event) {
        Player p = event.player;
        if (event.phase != TickEvent.Phase.END || p.level().isClientSide || p.tickCount % 20 != 0) return;
        if (!airless(p) || p.isCreative() || p.isSpectator()) return;
        for (OxygenGeneratorBE g : new ArrayList<>(OxygenGeneratorBE.LOADED)) {
            if (!g.isRemoved() && g.getLevel() == p.level() && g.getBlockPos().distSqr(p.blockPosition()) <= 64 && g.breathe()) return;
        }
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        if (fullSuit(p)) {
            int o = chest.getOrCreateTag().getInt("Oxygen");
            if (o > 0) {
                chest.getOrCreateTag().putInt("Oxygen", o - 1);
                if (o - 1 == 60 || o - 1 == 30 || o - 1 == 10) p.displayClientMessage(Component.translatable("msg.fw_spacesuit.low", o - 1), true);
                return;
            }
        }
        p.hurt(p.damageSources().drown(), 2.0f);
        p.displayClientMessage(Component.translatable("msg.fw_spacesuit.no_air"), true);
    }

    private static void onTooltip(ItemTooltipEvent event) {
        if (isSuitChest(event.getItemStack())) {
            int o = event.getItemStack().hasTag() ? event.getItemStack().getTag().getInt("Oxygen") : 0;
            event.getToolTip().add(Component.translatable("tooltip.fw_spacesuit.oxygen", o, SUIT_MAX));
        }
    }
}
