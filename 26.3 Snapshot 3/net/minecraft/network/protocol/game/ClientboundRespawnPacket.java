package net.minecraft.network.protocol.game;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ClientboundRespawnPacket(CommonPlayerSpawnInfo commonPlayerSpawnInfo, byte dataToKeep) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundRespawnPacket> STREAM_CODEC = Packet.codec(
      ClientboundRespawnPacket::write, ClientboundRespawnPacket::new
   );
   public static final byte KEEP_ATTRIBUTE_MODIFIERS = 1;
   public static final byte KEEP_ENTITY_DATA = 2;
   public static final byte KEEP_ALL_DATA = 3;

   private ClientboundRespawnPacket(final RegistryFriendlyByteBuf input) {
      this(new CommonPlayerSpawnInfo(input), input.readByte());
   }

   private void write(final RegistryFriendlyByteBuf output) {
      this.commonPlayerSpawnInfo.write(output);
      output.writeByte(this.dataToKeep);
   }

   @Override
   public PacketType<ClientboundRespawnPacket> type() {
      return GamePacketTypes.CLIENTBOUND_RESPAWN;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleRespawn(this);
   }

   public boolean shouldKeep(final byte mask) {
      return (this.dataToKeep & mask) != 0;
   }
}
