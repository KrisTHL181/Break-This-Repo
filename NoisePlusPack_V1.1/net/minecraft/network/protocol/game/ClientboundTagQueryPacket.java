package net.minecraft.network.protocol.game;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import org.jspecify.annotations.Nullable;

public class ClientboundTagQueryPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundTagQueryPacket> STREAM_CODEC = Packet.codec(
      ClientboundTagQueryPacket::write, ClientboundTagQueryPacket::new
   );
   private final int transactionId;
   private final @Nullable CompoundTag tag;

   public ClientboundTagQueryPacket(int p_133497_, @Nullable CompoundTag p_133498_) {
      this.transactionId = p_133497_;
      this.tag = p_133498_;
   }

   private ClientboundTagQueryPacket(FriendlyByteBuf p_179433_) {
      this.transactionId = p_179433_.readVarInt();
      this.tag = p_179433_.readNbt();
   }

   private void write(FriendlyByteBuf p_133508_) {
      p_133508_.writeVarInt(this.transactionId);
      p_133508_.writeNbt(this.tag);
   }

   @Override
   public PacketType<ClientboundTagQueryPacket> type() {
      return GamePacketTypes.CLIENTBOUND_TAG_QUERY;
   }

   public void handle(ClientGamePacketListener p_133505_) {
      p_133505_.handleTagQueryPacket(this);
   }

   public int getTransactionId() {
      return this.transactionId;
   }

   public @Nullable CompoundTag getTag() {
      return this.tag;
   }

   @Override
   public boolean isSkippable() {
      return true;
   }
}
