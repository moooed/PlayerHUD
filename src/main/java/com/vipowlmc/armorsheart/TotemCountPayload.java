package com.vipowlmc.armorsheart;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record TotemCountPayload(UUID playerUuid, int count) implements CustomPayload {
    public static final Identifier ID_VALUE = Identifier.of(ArmorsHeartHui.MOD_ID, "totem_count");
    public static final CustomPayload.Id<TotemCountPayload> ID = new CustomPayload.Id<>(ID_VALUE);

    public static final PacketCodec<RegistryByteBuf, TotemCountPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, TotemCountPayload::playerUuid,
            PacketCodecs.VAR_INT, TotemCountPayload::count,
            TotemCountPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
