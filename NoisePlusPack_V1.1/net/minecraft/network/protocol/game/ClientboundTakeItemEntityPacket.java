package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundTakeItemEntityPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundTakeItemEntityPacket> STREAM_CODEC = Packet.codec(
      ClientboundTakeItemEntityPacket::write, ClientboundTakeItemEntityPacket::new
   );
   private final int itemId;
   private final int playerId;
   private final int amount;

   public ClientboundTakeItemEntityPacket(int p_133515_, int p_133516_, int p_133517_) {
      this.itemId = p_133515_;
      this.playerId = p_133516_;
      this.amount = p_133517_;
   }

   private ClientboundTakeItemEntityPacket(FriendlyByteBuf p_179435_) {
      this.itemId = p_179435_.readVarInt();
      this.playerId = p_179435_.readVarInt();
      this.amount = p_179435_.readVarInt();
   }

   private void write(FriendlyByteBuf p_133526_) {
      p_133526_.writeVarInt(this.itemId);
      p_133526_.writeVarInt(this.playerId);
      p_133526_.writeVarInt(this.amount);
   }

   @Override
   public PacketType<ClientboundTakeItemEntityPacket> type() {
      return GamePacketTypes.CLIENTBOUND_TAKE_ITEM_ENTITY;
   }

   public void handle(ClientGamePacketListener p_133523_) {
      p_133523_.handleTakeItemEntity(this);
   }

   public int getItemId() {
      return this.itemId;
   }

   public int getPlayerId() {
      return this.playerId;
   }

   public int getAmount() {
      return this.amount;
   }
}
