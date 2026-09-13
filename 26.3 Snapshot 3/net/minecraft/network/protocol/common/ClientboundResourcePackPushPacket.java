package net.minecraft.network.protocol.common;

import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ClientboundResourcePackPushPacket(UUID id, String url, String hash, boolean required, Optional<Component> prompt)
   implements Packet<ClientCommonPacketListener> {
   public static final int MAX_HASH_LENGTH = 40;
   public static final StreamCodec<ByteBuf, ClientboundResourcePackPushPacket> STREAM_CODEC = StreamCodec.composite(
      UUIDUtil.STREAM_CODEC,
      ClientboundResourcePackPushPacket::id,
      ByteBufCodecs.STRING_UTF8,
      ClientboundResourcePackPushPacket::url,
      ByteBufCodecs.stringUtf8(40),
      ClientboundResourcePackPushPacket::hash,
      ByteBufCodecs.BOOL,
      ClientboundResourcePackPushPacket::required,
      ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.apply(ByteBufCodecs::optional),
      ClientboundResourcePackPushPacket::prompt,
      ClientboundResourcePackPushPacket::new
   );

   public ClientboundResourcePackPushPacket {
      if (hash.length() > 40) {
         throw new IllegalArgumentException("Hash is too long (max 40, was " + hash.length() + ")");
      }
   }

   @Override
   public PacketType<ClientboundResourcePackPushPacket> type() {
      return CommonPacketTypes.CLIENTBOUND_RESOURCE_PACK_PUSH;
   }

   public void handle(final ClientCommonPacketListener listener) {
      listener.handleResourcePackPush(this);
   }
}
