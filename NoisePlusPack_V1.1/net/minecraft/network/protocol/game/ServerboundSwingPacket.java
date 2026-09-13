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

   public ServerboundSwingPacket(InteractionHand p_134667_) {
      this.hand = p_134667_;
   }

   private ServerboundSwingPacket(FriendlyByteBuf p_179792_) {
      this.hand = p_179792_.readEnum(InteractionHand.class);
   }

   private void write(FriendlyByteBuf p_134676_) {
      p_134676_.writeEnum(this.hand);
   }

   @Override
   public PacketType<ServerboundSwingPacket> type() {
      return GamePacketTypes.SERVERBOUND_SWING;
   }

   public void handle(ServerGamePacketListener p_134673_) {
      p_134673_.handleAnimate(this);
   }

   public InteractionHand getHand() {
      return this.hand;
   }
}
