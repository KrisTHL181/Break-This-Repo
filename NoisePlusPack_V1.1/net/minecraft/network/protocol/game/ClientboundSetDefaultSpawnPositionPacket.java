package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.level.storage.LevelData;

public record ClientboundSetDefaultSpawnPositionPacket(LevelData.RespawnData respawnData) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundSetDefaultSpawnPositionPacket> STREAM_CODEC = StreamCodec.composite(
      LevelData.RespawnData.STREAM_CODEC, ClientboundSetDefaultSpawnPositionPacket::respawnData, ClientboundSetDefaultSpawnPositionPacket::new
   );

   @Override
   public PacketType<ClientboundSetDefaultSpawnPositionPacket> type() {
      return GamePacketTypes.CLIENTBOUND_SET_DEFAULT_SPAWN_POSITION;
   }

   public void handle(ClientGamePacketListener p_133122_) {
      p_133122_.handleSetSpawn(this);
   }
}
