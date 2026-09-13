package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.LivingEntity;

public record ClientboundHurtAnimationPacket(int id, float yaw) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundHurtAnimationPacket> STREAM_CODEC = Packet.codec(
      ClientboundHurtAnimationPacket::write, ClientboundHurtAnimationPacket::new
   );

   public ClientboundHurtAnimationPacket(final LivingEntity entity) {
      this(entity.getId(), entity.getHurtDir());
   }

   private ClientboundHurtAnimationPacket(final FriendlyByteBuf input) {
      this(input.readVarInt(), input.readFloat());
   }

   private void write(final FriendlyByteBuf output) {
      output.writeVarInt(this.id);
      output.writeFloat(this.yaw);
   }

   @Override
   public PacketType<ClientboundHurtAnimationPacket> type() {
      return GamePacketTypes.CLIENTBOUND_HURT_ANIMATION;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleHurtAnimation(this);
   }
}
