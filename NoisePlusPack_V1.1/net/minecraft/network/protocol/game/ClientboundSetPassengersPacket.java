package net.minecraft.network.protocol.game;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.Entity;

public class ClientboundSetPassengersPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundSetPassengersPacket> STREAM_CODEC = Packet.codec(
      ClientboundSetPassengersPacket::write, ClientboundSetPassengersPacket::new
   );
   private final int vehicle;
   private final int[] passengers;

   public ClientboundSetPassengersPacket(Entity p_133276_) {
      this.vehicle = p_133276_.getId();
      List<Entity> list = p_133276_.getPassengers();
      this.passengers = new int[list.size()];

      for (int i = 0; i < list.size(); i++) {
         this.passengers[i] = list.get(i).getId();
      }
   }

   private ClientboundSetPassengersPacket(FriendlyByteBuf p_179308_) {
      this.vehicle = p_179308_.readVarInt();
      this.passengers = p_179308_.readVarIntArray();
   }

   private void write(FriendlyByteBuf p_133285_) {
      p_133285_.writeVarInt(this.vehicle);
      p_133285_.writeVarIntArray(this.passengers);
   }

   @Override
   public PacketType<ClientboundSetPassengersPacket> type() {
      return GamePacketTypes.CLIENTBOUND_SET_PASSENGERS;
   }

   public void handle(ClientGamePacketListener p_133282_) {
      p_133282_.handleSetEntityPassengersPacket(this);
   }

   public int[] getPassengers() {
      return this.passengers;
   }

   public int getVehicle() {
      return this.vehicle;
   }
}
