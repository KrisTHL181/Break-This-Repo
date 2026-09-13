package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.InteractionHand;

public class ServerboundSwingPacket implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundSwingPacket> STREAM_CODEC = Packet.codec(
      ServerboundSwingPacket::write, ServerboundSwingPacket::new
   );
   private final InteractionHand hand;

   public ServerboundSwingPacket(final InteractionHand hand) {
      this.hand = hand;
   }

   private ServerboundSwingPacket(final FriendlyByteBuf input) {
      this.hand = input.readEnum(InteractionHand.class);
   }

   private void write(final FriendlyByteBuf output) {
      output.writeEnum(this.hand);
   }

   @Override
   public PacketType<ServerboundSwingPacket> type() {
      return GamePacketTypes.SERVERBOUND_SWING;
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handleAnimate(this);
   }

   public InteractionHand getHand() {
      return this.hand;
   }
}
