package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ServerboundSelectBundleItemPacket(int slotId, int selectedItemIndex) implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundSelectBundleItemPacket> STREAM_CODEC = Packet.codec(
      ServerboundSelectBundleItemPacket::write, ServerboundSelectBundleItemPacket::new
   );

   private ServerboundSelectBundleItemPacket(FriendlyByteBuf p_361056_) {
      this(p_361056_.readVarInt(), p_361056_.readVarInt());
      if (this.selectedItemIndex < 0 && this.selectedItemIndex != -1) {
         throw new IllegalArgumentException("Invalid selectedItemIndex: " + this.selectedItemIndex);
      }
   }

   private void write(FriendlyByteBuf p_364570_) {
      p_364570_.writeVarInt(this.slotId);
      p_364570_.writeVarInt(this.selectedItemIndex);
   }

   @Override
   public PacketType<ServerboundSelectBundleItemPacket> type() {
      return GamePacketTypes.SERVERBOUND_BUNDLE_ITEM_SELECTED;
   }

   public void handle(ServerGamePacketListener p_361569_) {
      p_361569_.handleBundleItemSelectedPacket(this);
   }
}
