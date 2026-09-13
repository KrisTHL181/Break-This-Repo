package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class ClientboundSetEntityMotionPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundSetEntityMotionPacket> STREAM_CODEC = Packet.codec(
      ClientboundSetEntityMotionPacket::write, ClientboundSetEntityMotionPacket::new
   );
   private final int id;
   private final Vec3 movement;

   public ClientboundSetEntityMotionPacket(Entity p_133185_) {
      this(p_133185_.getId(), p_133185_.getDeltaMovement());
   }

   public ClientboundSetEntityMotionPacket(int p_133182_, Vec3 p_133183_) {
      this.id = p_133182_;
      this.movement = p_133183_;
   }

   private ClientboundSetEntityMotionPacket(FriendlyByteBuf p_179294_) {
      this.id = p_179294_.readVarInt();
      this.movement = p_179294_.readLpVec3();
   }

   private void write(FriendlyByteBuf p_133194_) {
      p_133194_.writeVarInt(this.id);
      p_133194_.writeLpVec3(this.movement);
   }

   @Override
   public PacketType<ClientboundSetEntityMotionPacket> type() {
      return GamePacketTypes.CLIENTBOUND_SET_ENTITY_MOTION;
   }

   public void handle(ClientGamePacketListener p_133191_) {
      p_133191_.handleSetEntityMotion(this);
   }

   public int getId() {
      return this.id;
   }

   public Vec3 getMovement() {
      return this.movement;
   }
}
