package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundContainerSetDataPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundContainerSetDataPacket> STREAM_CODEC = Packet.codec(
      ClientboundContainerSetDataPacket::write, ClientboundContainerSetDataPacket::new
   );
   private final int containerId;
   private final int id;
   private final int value;

   public ClientboundContainerSetDataPacket(int p_131963_, int p_131964_, int p_131965_) {
      this.containerId = p_131963_;
      this.id = p_131964_;
      this.value = p_131965_;
   }

   private ClientboundContainerSetDataPacket(FriendlyByteBuf p_178825_) {
      this.containerId = p_178825_.readContainerId();
      this.id = p_178825_.readShort();
      this.value = p_178825_.readShort();
   }

   private void write(FriendlyByteBuf p_131974_) {
      p_131974_.writeContainerId(this.containerId);
      p_131974_.writeShort(this.id);
      p_131974_.writeShort(this.value);
   }

   @Override
   public PacketType<ClientboundContainerSetDataPacket> type() {
      return GamePacketTypes.CLIENTBOUND_CONTAINER_SET_DATA;
   }

   public void handle(ClientGamePacketListener p_131971_) {
      p_131971_.handleContainerSetData(this);
   }

   public int getContainerId() {
      return this.containerId;
   }

   public int getId() {
      return this.id;
   }

   public int getValue() {
      return this.value;
   }
}
