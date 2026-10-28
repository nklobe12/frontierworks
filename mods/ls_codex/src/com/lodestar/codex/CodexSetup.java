package com.lodestar.codex;

import com.lodestar.core.data.PlayerData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class CodexSetup {
    public static final String KEY = "ls_codex";

    private CodexSetup() {}

    public static void init(IEventBus bus) {
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> grantOnce(e.getEntity()));
        if (com.lodestar.core.test.SelfTest.enabled()) CodexTests.register();
    }

    /**
     * Gives the player a Field Codex the first time they join a world (tracked in their player data, so it survives
     * death and is not repeated). Returns true when a codex was given.
     */
    public static boolean grantOnce(Player player) {
        if (player.level().isClientSide) return false;
        CompoundTag data = PlayerData.get(player, KEY);
        if (data.getBoolean("Granted")) return false;
        data.putBoolean("Granted", true);
        ItemStack book = new ItemStack(Content.FIELD_CODEX.get());
        if (!player.getInventory().add(book)) player.drop(book, false);
        if (!(player instanceof FakePlayer)) {
            player.sendSystemMessage(Component.translatable("msg.ls_codex.granted").withStyle(ChatFormatting.GOLD));
        }
        return true;
    }
}
