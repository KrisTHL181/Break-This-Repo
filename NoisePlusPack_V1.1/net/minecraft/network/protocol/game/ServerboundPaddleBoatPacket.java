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

   public ServerboundPaddleBoatPacket(boolean p_134210_, boolean p_134211_) {
      this.left = p_134210_;
      this.right = p_134211_;
   }

   private ServerboundPaddleBoatPacket(FriendlyByteBuf p_179702_) {
      this.left = p_179702_.readBoolean();
      this.right = p_179702_.readBoolean();
   }

   private void write(FriendlyByteBuf p_134220_) {
      p_134220_.writeBoolean(this.left);
      p_134220_.writeBoolean(this.right);
   }

   public void handle(ServerGamePacketListener p_134217_) {
      p_134217_.handlePaddleBoat(this);
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
