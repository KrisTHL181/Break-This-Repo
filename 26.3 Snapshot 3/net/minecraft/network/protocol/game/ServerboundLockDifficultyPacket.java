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

   public ServerboundLockDifficultyPacket(final boolean locked) {
      this.locked = locked;
   }

   private ServerboundLockDifficultyPacket(final FriendlyByteBuf input) {
      this.locked = input.readBoolean();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeBoolean(this.locked);
   }

   @Override
   public PacketType<ServerboundLockDifficultyPacket> type() {
      return GamePacketTypes.SERVERBOUND_LOCK_DIFFICULTY;
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handleLockDifficulty(this);
   }

   public boolean isLocked() {
      return this.locked;
   }
}
