package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundLockDifficultyPacket implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundLockDifficultyPacket> STREAM_CODEC = Packet.codec(
      ServerboundLockDifficultyPacket::write, ServerboundLockDifficultyPacket::new
   );
   private final boolean locked;

   public ServerboundLockDifficultyPacket(boolean p_134108_) {
      this.locked = p_134108_;
   }

   private ServerboundLockDifficultyPacket(FriendlyByteBuf p_179673_) {
      this.locked = p_179673_.readBoolean();
   }

   private void write(FriendlyByteBuf p_134117_) {
      p_134117_.writeBoolean(this.locked);
   }

   @Override
   public PacketType<ServerboundLockDifficultyPacket> type() {
      return GamePacketTypes.SERVERBOUND_LOCK_DIFFICULTY;
   }

   public void handle(ServerGamePacketListener p_134114_) {
      p_134114_.handleLockDifficulty(this);
   }

   public boolean isLocked() {
      return this.locked;
   }
}
