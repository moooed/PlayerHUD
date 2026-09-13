package com.vipowlmc.armorsheart;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public class ArmorsHeartHui implements ModInitializer {
    public static final String MOD_ID = "armors_heart_hui";

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(TotemCountPayload.ID, TotemCountPayload.CODEC);

        // Send the exact Totem count every 10 server ticks (twice per second).
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 10 != 0) return;

            for (ServerPlayerEntity viewer : server.getPlayerManager().getPlayerList()) {
                for (ServerPlayerEntity target : server.getPlayerManager().getPlayerList()) {
                    int count = countTotems(target.getInventory());
                    ServerPlayNetworking.send(viewer, new TotemCountPayload(target.getUuid(), count));
                }
            }
        });
    }

    private static int countTotems(PlayerInventory inventory) {
        int total = 0;
        for (int i = 0; i < inventory.size(); i++) {
            var stack = inventory.getStack(i);
            if (stack.isOf(Items.TOTEM_OF_UNDYING)) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
