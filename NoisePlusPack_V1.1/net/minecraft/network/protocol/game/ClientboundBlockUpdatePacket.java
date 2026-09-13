package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ClientboundBlockUpdatePacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundBlockUpdatePacket> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      ClientboundBlockUpdatePacket::getPos,
      ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY),
      ClientboundBlockUpdatePacket::getBlockState,
      ClientboundBlockUpdatePacket::new
   );
   private final BlockPos pos;
   private final BlockState blockState;

   public ClientboundBlockUpdatePacket(BlockPos p_131738_, BlockState p_131739_) {
      this.pos = p_131738_;
      this.blockState = p_131739_;
   }

   public ClientboundBlockUpdatePacket(BlockGetter p_131735_, BlockPos p_131736_) {
      this(p_131736_, p_131735_.getBlockState(p_131736_));
   }

   @Override
   public PacketType<ClientboundBlockUpdatePacket> type() {
      return GamePacketTypes.CLIENTBOUND_BLOCK_UPDATE;
   }

   public void handle(ClientGamePacketListener p_131745_) {
      p_131745_.handleBlockUpdate(this);
   }

   public BlockState getBlockState() {
      return this.blockState;
   }

   public BlockPos getPos() {
      return this.pos;
   }
}
