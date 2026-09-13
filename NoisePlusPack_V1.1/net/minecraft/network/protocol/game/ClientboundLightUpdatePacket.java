package net.minecraft.network.protocol.game;

import java.util.BitSet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jspecify.annotations.Nullable;

public class ClientboundLightUpdatePacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundLightUpdatePacket> STREAM_CODEC = Packet.codec(
      ClientboundLightUpdatePacket::write, ClientboundLightUpdatePacket::new
   );
   private final int x;
   private final int z;
   private final ClientboundLightUpdatePacketData lightData;

   public ClientboundLightUpdatePacket(ChunkPos p_285255_, LevelLightEngine p_285409_, @Nullable BitSet p_285387_, @Nullable BitSet p_285074_) {
      this.x = p_285255_.x;
      this.z = p_285255_.z;
      this.lightData = new ClientboundLightUpdatePacketData(p_285255_, p_285409_, p_285387_, p_285074_);
   }

   private ClientboundLightUpdatePacket(FriendlyByteBuf p_178918_) {
      this.x = p_178918_.readVarInt();
      this.z = p_178918_.readVarInt();
      this.lightData = new ClientboundLightUpdatePacketData(p_178918_, this.x, this.z);
   }

   private void write(FriendlyByteBuf p_132351_) {
      p_132351_.writeVarInt(this.x);
      p_132351_.writeVarInt(this.z);
      this.lightData.write(p_132351_);
   }

   @Override
   public PacketType<ClientboundLightUpdatePacket> type() {
      return GamePacketTypes.CLIENTBOUND_LIGHT_UPDATE;
   }

   public void handle(ClientGamePacketListener p_132348_) {
      p_132348_.handleLightUpdatePacket(this);
   }

   public int getX() {
      return this.x;
   }

   public int getZ() {
      return this.z;
   }

   public ClientboundLightUpdatePacketData getLightData() {
      return this.lightData;
   }
}
