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

   public ClientboundClearTitlesPacket(final boolean resetTimes) {
      this.resetTimes = resetTimes;
   }

   private ClientboundClearTitlesPacket(final FriendlyByteBuf input) {
      this.resetTimes = input.readBoolean();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeBoolean(this.resetTimes);
   }

   @Override
   public PacketType<ClientboundClearTitlesPacket> type() {
      return GamePacketTypes.CLIENTBOUND_CLEAR_TITLES;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleTitlesClear(this);
   }

   public boolean shouldResetTimes() {
      return this.resetTimes;
   }
}
