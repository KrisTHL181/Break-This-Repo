package net.minecraft.network.protocol.common;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.Identifier;

public record ClientboundPostEffectsPacket(List<Identifier> postEffects) implements Packet<ClientCommonPacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundPostEffectsPacket> STREAM_CODEC = Packet.codec(
      ClientboundPostEffectsPacket::write, ClientboundPostEffectsPacket::new
   );

   public ClientboundPostEffectsPacket(final FriendlyByteBuf input) {
      this(input.readList(FriendlyByteBuf::readIdentifier));
   }

   private void write(final FriendlyByteBuf output) {
      output.writeCollection(this.postEffects, FriendlyByteBuf::writeIdentifier);
   }

   @Override
   public PacketType<ClientboundPostEffectsPacket> type() {
      return CommonPacketTypes.CLIENTBOUND_POST_EFFECTS;
   }

   public void handle(final ClientCommonPacketListener listener) {
      listener.handlePostEffects(this);
   }
}
