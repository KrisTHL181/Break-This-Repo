package net.minecraft.network.protocol.common;

import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.Identifier;

public record ServerboundCustomClickActionPacket(Identifier id, Optional<Tag> payload) implements Packet<ServerCommonPacketListener> {
   private static final StreamCodec<ByteBuf, Optional<Tag>> UNTRUSTED_TAG_CODEC = ByteBufCodecs.optionalTagCodec(() -> new NbtAccounter(32768L, 16))
      .apply(ByteBufCodecs.lengthPrefixed(65536));
   public static final StreamCodec<ByteBuf, ServerboundCustomClickActionPacket> STREAM_CODEC = StreamCodec.composite(
      Identifier.STREAM_CODEC,
      ServerboundCustomClickActionPacket::id,
      UNTRUSTED_TAG_CODEC,
      ServerboundCustomClickActionPacket::payload,
      ServerboundCustomClickActionPacket::new
   );

   @Override
   public PacketType<ServerboundCustomClickActionPacket> type() {
      return CommonPacketTypes.SERVERBOUND_CUSTOM_CLICK_ACTION;
   }

   public void handle(ServerCommonPacketListener p_407494_) {
      p_407494_.handleCustomClickAction(this);
   }
}
