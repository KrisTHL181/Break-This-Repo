package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundClearTitlesPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundClearTitlesPacket> STREAM_CODEC = Packet.codec(
      ClientboundClearTitlesPacket::write, ClientboundClearTitlesPacket::new
   );
   private final boolean resetTimes;

   public ClientboundClearTitlesPacket(boolean p_178781_) {
      this.resetTimes = p_178781_;
   }

   private ClientboundClearTitlesPacket(FriendlyByteBuf p_178779_) {
      this.resetTimes = p_178779_.readBoolean();
   }

   private void write(FriendlyByteBuf p_178783_) {
      p_178783_.writeBoolean(this.resetTimes);
   }

   @Override
   public PacketType<ClientboundClearTitlesPacket> type() {
      return GamePacketTypes.CLIENTBOUND_CLEAR_TITLES;
   }

   public void handle(ClientGamePacketListener p_178787_) {
      p_178787_.handleTitlesClear(this);
   }

   public boolean shouldResetTimes() {
      return this.resetTimes;
   }
}
