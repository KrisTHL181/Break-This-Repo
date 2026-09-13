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

   public ClientboundSetHealthPacket(final float health, final int food, final float saturation) {
      this.health = health;
      this.food = food;
      this.saturation = saturation;
   }

   private ClientboundSetHealthPacket(final FriendlyByteBuf input) {
      this.health = input.readFloat();
      this.food = input.readVarInt();
      this.saturation = input.readFloat();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeFloat(this.health);
      output.writeVarInt(this.food);
      output.writeFloat(this.saturation);
   }

   @Override
   public PacketType<ClientboundSetHealthPacket> type() {
      return GamePacketTypes.CLIENTBOUND_SET_HEALTH;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleSetHealth(this);
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
