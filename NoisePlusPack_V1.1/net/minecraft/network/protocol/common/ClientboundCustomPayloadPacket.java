package net.minecraft.network.protocol.common;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.util.Util;

public record ClientboundCustomPayloadPacket(CustomPacketPayload payload) implements Packet<ClientCommonPacketListener> {
   private static final int MAX_PAYLOAD_SIZE = 1048576;
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundCustomPayloadPacket> GAMEPLAY_STREAM_CODEC = CustomPacketPayload.<RegistryFriendlyByteBuf>codec(
         p_448776_ -> DiscardedPayload.codec(p_448776_, 1048576),
         Util.make(
            Lists.newArrayList(new CustomPacketPayload.TypeAndCodec[]{new CustomPacketPayload.TypeAndCodec<>(BrandPayload.TYPE, BrandPayload.STREAM_CODEC)}),
            p_333496_ -> {}
         )
      )
      .map(ClientboundCustomPayloadPacket::new, ClientboundCustomPayloadPacket::payload);
   public static final StreamCodec<FriendlyByteBuf, ClientboundCustomPayloadPacket> CONFIG_STREAM_CODEC = CustomPacketPayload.<FriendlyByteBuf>codec(
         p_448777_ -> DiscardedPayload.codec(p_448777_, 1048576), List.of(new CustomPacketPayload.TypeAndCodec<>(BrandPayload.TYPE, BrandPayload.STREAM_CODEC))
      )
      .map(ClientboundCustomPayloadPacket::new, ClientboundCustomPayloadPacket::payload);

   @Override
   public PacketType<ClientboundCustomPayloadPacket> type() {
      return CommonPacketTypes.CLIENTBOUND_CUSTOM_PAYLOAD;
   }

   public void handle(ClientCommonPacketListener p_299773_) {
      p_299773_.handleCustomPayload(this);
   }
}
