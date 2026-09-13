package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.player.Abilities;

public class ServerboundPlayerAbilitiesPacket implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundPlayerAbilitiesPacket> STREAM_CODEC = Packet.codec(
      ServerboundPlayerAbilitiesPacket::write, ServerboundPlayerAbilitiesPacket::new
   );
   private static final int FLAG_FLYING = 2;
   private final boolean isFlying;

   public ServerboundPlayerAbilitiesPacket(final Abilities abilities) {
      this.isFlying = abilities.flying;
   }

   private ServerboundPlayerAbilitiesPacket(final FriendlyByteBuf input) {
      byte bitfield = input.readByte();
      this.isFlying = (bitfield & 2) != 0;
   }

   private void write(final FriendlyByteBuf output) {
      byte bitfield = 0;
      if (this.isFlying) {
         bitfield = (byte)(bitfield | 2);
      }

      output.writeByte(bitfield);
   }

   @Override
   public PacketType<ServerboundPlayerAbilitiesPacket> type() {
      return GamePacketTypes.SERVERBOUND_PLAYER_ABILITIES;
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handlePlayerAbilities(this);
   }

   public boolean isFlying() {
      return this.isFlying;
   }
}
