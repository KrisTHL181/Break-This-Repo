package net.minecraft.network.protocol.game;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.syncher.SynchedEntityData;

public record ClientboundSetEntityDataPacket(int id, List<SynchedEntityData.DataValue<?>> packedItems) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSetEntityDataPacket> STREAM_CODEC = Packet.codec(
      ClientboundSetEntityDataPacket::write, ClientboundSetEntityDataPacket::new
   );
   public static final int EOF_MARKER = 255;

   private ClientboundSetEntityDataPacket(RegistryFriendlyByteBuf p_335656_) {
      this(p_335656_.readVarInt(), unpack(p_335656_));
   }

   private static void pack(List<SynchedEntityData.DataValue<?>> p_253940_, RegistryFriendlyByteBuf p_331850_) {
      for (SynchedEntityData.DataValue<?> datavalue : p_253940_) {
         datavalue.write(p_331850_);
      }

      p_331850_.writeByte(255);
   }

   private static List<SynchedEntityData.DataValue<?>> unpack(RegistryFriendlyByteBuf p_330932_) {
      List<SynchedEntityData.DataValue<?>> list = new ArrayList<>();

      int i;
      while ((i = p_330932_.readUnsignedByte()) != 255) {
         list.add(SynchedEntityData.DataValue.read(p_330932_, i));
      }

      return list;
   }

   private void write(RegistryFriendlyByteBuf p_333245_) {
      p_333245_.writeVarInt(this.id);
      pack(this.packedItems, p_333245_);
   }

   @Override
   public PacketType<ClientboundSetEntityDataPacket> type() {
      return GamePacketTypes.CLIENTBOUND_SET_ENTITY_DATA;
   }

   public void handle(ClientGamePacketListener p_133155_) {
      p_133155_.handleSetEntityData(this);
   }
}
