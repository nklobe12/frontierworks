package com.frontierworks.scanner;

import com.frontierworks.core.energy.EnergyItem;
import com.frontierworks.core.energy.ItemEnergy;
import com.frontierworks.core.net.CoreNet;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/** Right-click: highlights every ore within range through walls for 15 seconds. Uses energy. */
public class OreScannerItem extends EnergyItem {
    public final int radius;
    public final int cost;

    public OreScannerItem(Properties props, int capacity, int radius, int cost) {
        super(props, capacity, Math.max(1000, capacity / 50), false);
        this.radius = radius;
        this.cost = cost;
    }

    public static void init() {
        CoreNet.register("fw_scanner:scan", (player, data) -> {
            ItemStack st = player.getMainHandItem();
            if (st.getItem() instanceof OreScannerItem) ItemEnergy.use(st, ((OreScannerItem) st.getItem()).cost);
        });
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
        if (!ItemEnergy.has(stack, cost)) {
            if (level.isClientSide) player.displayClientMessage(Component.translatable("msg.fw_scanner.no_power"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide) {
            final int r = radius;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.frontierworks.scanner.client.ScannerClient.scan(r));
            CoreNet.send("fw_scanner:scan", 0);
        }
        player.getCooldowns().addCooldown(this, 60);
        return InteractionResultHolder.success(stack);
    }
}
