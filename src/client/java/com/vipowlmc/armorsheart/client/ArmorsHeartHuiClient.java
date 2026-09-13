package com.vipowlmc.armorsheart.client;

import com.vipowlmc.armorsheart.TotemCountPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ArmorsHeartHuiClient implements ClientModInitializer {
    private static final Map<UUID, Integer> TOTEMS = new ConcurrentHashMap<>();

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(TotemCountPayload.ID, (payload, context) ->
                TOTEMS.put(payload.playerUuid(), payload.count()));

        HudElementRegistry.addLast(
                Identifier.of("armors_heart_hui", "target_hud"),
                ArmorsHeartHuiClient::renderTargetHud
        );
    }

    private static void renderTargetHud(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.currentScreen != null) return;
        if (!(client.crosshairTarget instanceof EntityHitResult hit)) return;
        if (!(hit.getEntity() instanceof AbstractClientPlayerEntity target)) return;
        if (target == client.player) return;

        final int x = 8;
        final int y = 8;
        final int width = 238;
        final int height = 102;

        // Background + border.
        context.fill(x, y, x + width, y + height, 0xD0101018);
        context.fill(x, y, x + width, y + 1, 0xFFF0A6FF);
        context.fill(x, y + height - 1, x + width, y + height, 0xFFF0A6FF);
        context.fill(x, y, x + 1, y + height, 0xFFF0A6FF);
        context.fill(x + width - 1, y, x + width, y + height, 0xFFF0A6FF);

        TextRenderer tr = client.textRenderer;
        int tx = x + 6;

        context.drawTextWithShadow(tr, target.getName(), tx, y + 5, 0xFFFFB7F7);

        float health = Math.max(0.0f, target.getHealth());
        float maxHealth = Math.max(1.0f, target.getMaxHealth());
        context.drawTextWithShadow(
                tr,
                String.format("HP: %.1f / %.1f", health, maxHealth),
                tx, y + 18, 0xFFFF5555
        );

        int armor = target.getArmor();
        float toughness = (float) target.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS);

        // "Armor HP" here is the effective health against a 10-damage hit.
        // Armor reduction depends on incoming damage, so the reference damage is shown.
        float armorReduction = getArmorReduction(armor, toughness, 10.0f);
        float armorEhp = health / Math.max(0.01f, 1.0f - armorReduction);
        context.drawTextWithShadow(
                tr,
                String.format("Armor: %d  |  EHP: %.1f", armor, armorEhp),
                tx, y + 31, 0xFFD7E7FF
        );

        int totems = TOTEMS.getOrDefault(target.getUuid(), -1);
        String totemText = totems >= 0 ? "Totems: " + totems : "Totems: ?";
        context.drawTextWithShadow(tr, totemText, x + 135, y + 18, 0xFFFFD45C);

        context.drawTextWithShadow(
                tr,
                String.format("Armor reduction: %.1f%% (vs 10 dmg)", armorReduction * 100.0f),
                x + 135, y + 31, 0xFFBFD8FF
        );

        drawArmorSlots(context, target, x + 6, y + 44);

        context.drawTextWithShadow(tr, "Made By VipOwlMC", x + 135, y + 82, 0xFFBFA7C7);
    }

    private static float getArmorReduction(int armor, float toughness, float incomingDamage) {
        float reduction = Math.min(
                20.0f,
                Math.max(
                        armor / 5.0f,
                        armor - incomingDamage / (2.0f + toughness / 4.0f)
                )
        );
        return reduction / 25.0f;
    }

    private static void drawArmorSlots(
            DrawContext context,
            AbstractClientPlayerEntity player,
            int x,
            int y
    ) {
        EquipmentSlot[] slots = {
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        };

        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = player.getEquippedStack(slots[i]);
            int sx = x + i * 54;

            // Slot background.
            context.fill(sx, y, sx + 50, y + 34, 0x80181820);

            if (stack.isEmpty()) {
                context.fill(sx + 20, y + 14, sx + 30, y + 16, 0xFF555555);
                context.drawTextWithShadow(
                        MinecraftClient.getInstance().textRenderer,
                        "EMPTY",
                        sx + 7, y + 19, 0xFF777777
                );
                continue;
            }

            context.drawItemWithoutEntity(stack, sx + 17, y + 1);

            if (stack.isDamageable()) {
                int max = stack.getMaxDamage();
                int damage = stack.getDamage();
                int current = Math.max(0, max - damage);
                float ratio = max > 0 ? current / (float) max : 1.0f;

                // Durability bar.
                int barLeft = sx + 4;
                int barRight = sx + 46;
                int barY = y + 21;
                context.fill(barLeft, barY, barRight, barY + 4, 0xFF303030);
                int filled = Math.round((barRight - barLeft) * ratio);
                if (filled > 0) {
                    int durabilityColor = ratio > 0.50f
                            ? 0xFF55FF55
                            : ratio > 0.20f ? 0xFFFFFF55 : 0xFFFF5555;
                    context.fill(barLeft, barY, barLeft + filled, barY + 4, durabilityColor);
                }

                context.drawTextWithShadow(
                        MinecraftClient.getInstance().textRenderer,
                        current + "/" + max,
                        sx + 5, y + 25, 0xFFFFFFFF
                );
            } else {
                context.drawTextWithShadow(
                        MinecraftClient.getInstance().textRenderer,
                        "∞",
                        sx + 22, y + 23, 0xFFFFFFFF
                );
            }
        }
    }
}
