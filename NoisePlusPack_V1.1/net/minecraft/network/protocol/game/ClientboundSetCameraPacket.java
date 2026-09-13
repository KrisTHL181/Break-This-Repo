package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class ClientboundSetCameraPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundSetCameraPacket> STREAM_CODEC = Packet.codec(
      ClientboundSetCameraPacket::write, ClientboundSetCameraPacket::new
   );
   private final int cameraId;

   public ClientboundSetCameraPacket(Entity p_133058_) {
      this.cameraId = p_133058_.getId();
   }

   private ClientboundSetCameraPacket(FriendlyByteBuf p_179278_) {
      this.cameraId = p_179278_.readVarInt();
   }

   private void write(FriendlyByteBuf p_133068_) {
      p_133068_.writeVarInt(this.cameraId);
   }

   @Override
   public PacketType<ClientboundSetCameraPacket> type() {
      return GamePacketTypes.CLIENTBOUND_SET_CAMERA;
   }

   public void handle(ClientGamePacketListener p_133066_) {
      p_133066_.handleSetCamera(this);
   }

   public @Nullable Entity getEntity(Level p_133060_) {
      return p_133060_.getEntity(this.cameraId);
   }
}
