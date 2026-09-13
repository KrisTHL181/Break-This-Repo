package net.minecraft.network.protocol.game;

import java.util.BitSet;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jspecify.annotations.Nullable;

public class ClientboundLevelChunkWithLightPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundLevelChunkWithLightPacket> STREAM_CODEC = Packet.codec(
      ClientboundLevelChunkWithLightPacket::write, ClientboundLevelChunkWithLightPacket::new
   );
   private final int x;
   private final int z;
   private final ClientboundLevelChunkPacketData chunkData;
   private final ClientboundLightUpdatePacketData lightData;

   public ClientboundLevelChunkWithLightPacket(
      final LevelChunk levelChunk,
      final LevelLightEngine lightEngine,
      final @Nullable BitSet skyChangedLightSectionFilter,
      final @Nullable BitSet blockChangedLightSectionFilter
   ) {
      ChunkPos chunkPos = levelChunk.getPos();
      this.x = chunkPos.x();
      this.z = chunkPos.z();
      this.chunkData = new ClientboundLevelChunkPacketData(levelChunk);
      this.lightData = new ClientboundLightUpdatePacketData(chunkPos, lightEngine, skyChangedLightSectionFilter, blockChangedLightSectionFilter);
   }

   private ClientboundLevelChunkWithLightPacket(final RegistryFriendlyByteBuf input) {
      this.x = input.readInt();
      this.z = input.readInt();
      this.chunkData = new ClientboundLevelChunkPacketData(input, this.x, this.z);
      this.lightData = new ClientboundLightUpdatePacketData(input, this.x, this.z);
   }

   private void write(final RegistryFriendlyByteBuf output) {
      output.writeInt(this.x);
      output.writeInt(this.z);
      this.chunkData.write(output);
      this.lightData.write(output);
   }

   @Override
   public PacketType<ClientboundLevelChunkWithLightPacket> type() {
      return GamePacketTypes.CLIENTBOUND_LEVEL_CHUNK_WITH_LIGHT;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleLevelChunkWithLight(this);
   }

   public int getX() {
      return this.x;
   }

   public int getZ() {
      return this.z;
   }

   public ClientboundLevelChunkPacketData getChunkData() {
      return this.chunkData;
   }

   public ClientboundLightUpdatePacketData getLightData() {
      return this.lightData;
   }
}
