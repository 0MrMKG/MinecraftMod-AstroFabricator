package com.astrofabricator.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AstroPayload(String action, String data1, String data2) implements CustomPacketPayload {

    public static final Type<AstroPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("astrofabricator", "main_payload"));

    // 定义如何将数据写入字节流以及如何读取
    public static final StreamCodec<FriendlyByteBuf, AstroPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, AstroPayload::action,
            ByteBufCodecs.STRING_UTF8, AstroPayload::data1,
            ByteBufCodecs.STRING_UTF8, AstroPayload::data2,
            AstroPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}