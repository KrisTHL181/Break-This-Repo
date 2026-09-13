package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundProjectilePowerPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundProjectilePowerPacket> STREAM_CODEC = Packet.codec(
      ClientboundProjectilePowerPacket::write, ClientboundProjectilePowerPacket::new
   );
   private final int id;
   private final double accelerationPower;

   public ClientboundProjectilePowerPacket(final int id, final double accelerationPower) {
      this.id = id;
      this.accelerationPower = accelerationPower;
   }

   private ClientboundProjectilePowerPacket(final FriendlyByteBuf input) {
      this.id = input.readVarInt();
      this.accelerationPower = input.readDouble();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeVarInt(this.id);
      output.writeDouble(this.accelerationPower);
   }

   @Override
   public PacketType<ClientboundProjectilePowerPacket> type() {
      return GamePacketTypes.CLIENTBOUND_PROJECTILE_POWER;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleProjectilePowerPacket(this);
   }

   public int getId() {
      return this.id;
   }

   public double getAccelerationPower() {
      return this.accelerationPower;
   }
}
