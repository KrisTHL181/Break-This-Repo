package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundPaddleBoatPacket implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundPaddleBoatPacket> STREAM_CODEC = Packet.codec(
      ServerboundPaddleBoatPacket::write, ServerboundPaddleBoatPacket::new
   );
   private final boolean left;
   private final boolean right;

   public ServerboundPaddleBoatPacket(final boolean left, final boolean right) {
      this.left = left;
      this.right = right;
   }

   private ServerboundPaddleBoatPacket(final FriendlyByteBuf input) {
      this.left = input.readBoolean();
      this.right = input.readBoolean();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeBoolean(this.left);
      output.writeBoolean(this.right);
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handlePaddleBoat(this);
   }

   @Override
   public PacketType<ServerboundPaddleBoatPacket> type() {
      return GamePacketTypes.SERVERBOUND_PADDLE_BOAT;
   }

   public boolean getLeft() {
      return this.left;
   }

   public boolean getRight() {
      return this.right;
   }
}
