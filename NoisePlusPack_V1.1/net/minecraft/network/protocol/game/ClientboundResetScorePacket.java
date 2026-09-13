package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import org.jspecify.annotations.Nullable;

public record ClientboundResetScorePacket(String owner, @Nullable String objectiveName) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundResetScorePacket> STREAM_CODEC = Packet.codec(
      ClientboundResetScorePacket::write, ClientboundResetScorePacket::new
   );

   private ClientboundResetScorePacket(FriendlyByteBuf p_312061_) {
      this(p_312061_.readUtf(), p_312061_.readNullable(FriendlyByteBuf::readUtf));
   }

   private void write(FriendlyByteBuf p_310951_) {
      p_310951_.writeUtf(this.owner);
      p_310951_.writeNullable(this.objectiveName, FriendlyByteBuf::writeUtf);
   }

   @Override
   public PacketType<ClientboundResetScorePacket> type() {
      return GamePacketTypes.CLIENTBOUND_RESET_SCORE;
   }

   public void handle(ClientGamePacketListener p_310650_) {
      p_310650_.handleResetScore(this);
   }
}
