package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundSetHealthPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundSetHealthPacket> STREAM_CODEC = Packet.codec(
      ClientboundSetHealthPacket::write, ClientboundSetHealthPacket::new
   );
   private final float health;
   private final int food;
   private final float saturation;

   public ClientboundSetHealthPacket(float p_133238_, int p_133239_, float p_133240_) {
      this.health = p_133238_;
      this.food = p_133239_;
      this.saturation = p_133240_;
   }

   private ClientboundSetHealthPacket(FriendlyByteBuf p_179301_) {
      this.health = p_179301_.readFloat();
      this.food = p_179301_.readVarInt();
      this.saturation = p_179301_.readFloat();
   }

   private void write(FriendlyByteBuf p_133249_) {
      p_133249_.writeFloat(this.health);
      p_133249_.writeVarInt(this.food);
      p_133249_.writeFloat(this.saturation);
   }

   @Override
   public PacketType<ClientboundSetHealthPacket> type() {
      return GamePacketTypes.CLIENTBOUND_SET_HEALTH;
   }

   public void handle(ClientGamePacketListener p_133246_) {
      p_133246_.handleSetHealth(this);
   }

   public float getHealth() {
      return this.health;
   }

   public int getFood() {
      return this.food;
   }

   public float getSaturation() {
      return this.saturation;
   }
}
