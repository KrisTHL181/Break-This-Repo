package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.InteractionHand;

public class ClientboundOpenBookPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundOpenBookPacket> STREAM_CODEC = Packet.codec(
      ClientboundOpenBookPacket::write, ClientboundOpenBookPacket::new
   );
   private final InteractionHand hand;

   public ClientboundOpenBookPacket(InteractionHand p_132601_) {
      this.hand = p_132601_;
   }

   private ClientboundOpenBookPacket(FriendlyByteBuf p_179009_) {
      this.hand = p_179009_.readEnum(InteractionHand.class);
   }

   private void write(FriendlyByteBuf p_132610_) {
      p_132610_.writeEnum(this.hand);
   }

   @Override
   public PacketType<ClientboundOpenBookPacket> type() {
      return GamePacketTypes.CLIENTBOUND_OPEN_BOOK;
   }

   public void handle(ClientGamePacketListener p_132607_) {
      p_132607_.handleOpenBook(this);
   }

   public InteractionHand getHand() {
      return this.hand;
   }
}
