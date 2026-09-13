package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class ClientboundRotateHeadPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundRotateHeadPacket> STREAM_CODEC = Packet.codec(
      ClientboundRotateHeadPacket::write, ClientboundRotateHeadPacket::new
   );
   private final int entityId;
   private final byte yHeadRot;

   public ClientboundRotateHeadPacket(Entity p_132967_, byte p_132968_) {
      this.entityId = p_132967_.getId();
      this.yHeadRot = p_132968_;
   }

   private ClientboundRotateHeadPacket(FriendlyByteBuf p_179193_) {
      this.entityId = p_179193_.readVarInt();
      this.yHeadRot = p_179193_.readByte();
   }

   private void write(FriendlyByteBuf p_132979_) {
      p_132979_.writeVarInt(this.entityId);
      p_132979_.writeByte(this.yHeadRot);
   }

   @Override
   public PacketType<ClientboundRotateHeadPacket> type() {
      return GamePacketTypes.CLIENTBOUND_ROTATE_HEAD;
   }

   public void handle(ClientGamePacketListener p_132976_) {
      p_132976_.handleRotateMob(this);
   }

   public @Nullable Entity getEntity(Level p_132970_) {
      return p_132970_.getEntity(this.entityId);
   }

   public float getYHeadRot() {
      return Mth.unpackDegrees(this.yHeadRot);
   }
}
