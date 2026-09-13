package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.level.block.Block;

public class ClientboundBlockEventPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundBlockEventPacket> STREAM_CODEC = Packet.codec(
      ClientboundBlockEventPacket::write, ClientboundBlockEventPacket::new
   );
   private final BlockPos pos;
   private final int b0;
   private final int b1;
   private final Block block;

   public ClientboundBlockEventPacket(BlockPos p_131715_, Block p_131716_, int p_131717_, int p_131718_) {
      this.pos = p_131715_;
      this.block = p_131716_;
      this.b0 = p_131717_;
      this.b1 = p_131718_;
   }

   private ClientboundBlockEventPacket(RegistryFriendlyByteBuf p_332473_) {
      this.pos = p_332473_.readBlockPos();
      this.b0 = p_332473_.readUnsignedByte();
      this.b1 = p_332473_.readUnsignedByte();
      this.block = ByteBufCodecs.registry(Registries.BLOCK).decode(p_332473_);
   }

   private void write(RegistryFriendlyByteBuf p_331626_) {
      p_331626_.writeBlockPos(this.pos);
      p_331626_.writeByte(this.b0);
      p_331626_.writeByte(this.b1);
      ByteBufCodecs.registry(Registries.BLOCK).encode(p_331626_, this.block);
   }

   @Override
   public PacketType<ClientboundBlockEventPacket> type() {
      return GamePacketTypes.CLIENTBOUND_BLOCK_EVENT;
   }

   public void handle(ClientGamePacketListener p_131724_) {
      p_131724_.handleBlockEvent(this);
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public int getB0() {
      return this.b0;
   }

   public int getB1() {
      return this.b1;
   }

   public Block getBlock() {
      return this.block;
   }
}
