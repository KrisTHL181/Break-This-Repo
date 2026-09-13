package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.debug.DebugSubscription;

public record ClientboundDebugBlockValuePacket(BlockPos blockPos, DebugSubscription.Update<?> update) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundDebugBlockValuePacket> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      ClientboundDebugBlockValuePacket::blockPos,
      DebugSubscription.Update.STREAM_CODEC,
      ClientboundDebugBlockValuePacket::update,
      ClientboundDebugBlockValuePacket::new
   );

   @Override
   public PacketType<ClientboundDebugBlockValuePacket> type() {
      return GamePacketTypes.CLIENTBOUND_DEBUG_BLOCK_VALUE;
   }

   public void handle(ClientGamePacketListener p_426087_) {
      p_426087_.handleDebugBlockValue(this);
   }
}
