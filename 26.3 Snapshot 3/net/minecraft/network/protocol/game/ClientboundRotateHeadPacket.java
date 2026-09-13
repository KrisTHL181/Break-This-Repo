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

   public ClientboundRotateHeadPacket(final Entity entity, final byte yHeadRot) {
      this.entityId = entity.getId();
      this.yHeadRot = yHeadRot;
   }

   private ClientboundRotateHeadPacket(final FriendlyByteBuf input) {
      this.entityId = input.readVarInt();
      this.yHeadRot = input.readByte();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeVarInt(this.entityId);
      output.writeByte(this.yHeadRot);
   }

   @Override
   public PacketType<ClientboundRotateHeadPacket> type() {
      return GamePacketTypes.CLIENTBOUND_ROTATE_HEAD;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleRotateMob(this);
   }

   public @Nullable Entity getEntity(final Level level) {
      return level.getEntity(this.entityId);
   }

   public float getYHeadRot() {
      return Mth.unpackDegrees(this.yHeadRot);
   }
}
