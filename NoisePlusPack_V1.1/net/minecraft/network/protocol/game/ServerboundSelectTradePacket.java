package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundSelectTradePacket implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundSelectTradePacket> STREAM_CODEC = Packet.codec(
      ServerboundSelectTradePacket::write, ServerboundSelectTradePacket::new
   );
   private final int item;

   public ServerboundSelectTradePacket(int p_134462_) {
      this.item = p_134462_;
   }

   private ServerboundSelectTradePacket(FriendlyByteBuf p_179747_) {
      this.item = p_179747_.readVarInt();
   }

   private void write(FriendlyByteBuf p_134471_) {
      p_134471_.writeVarInt(this.item);
   }

   @Override
   public PacketType<ServerboundSelectTradePacket> type() {
      return GamePacketTypes.SERVERBOUND_SELECT_TRADE;
   }

   public void handle(ServerGamePacketListener p_134468_) {
      p_134468_.handleSelectTrade(this);
   }

   public int getItem() {
      return this.item;
   }
}
