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

   public ClientboundBlockEventPacket(final BlockPos pos, final Block block, final int b0, final int b1) {
      this.pos = pos;
      this.block = block;
      this.b0 = b0;
      this.b1 = b1;
   }

   private ClientboundBlockEventPacket(final RegistryFriendlyByteBuf input) {
      this.pos = input.readBlockPos();
      this.b0 = input.readUnsignedByte();
      this.b1 = input.readUnsignedByte();
      this.block = ByteBufCodecs.registry(Registries.BLOCK).decode(input);
   }

   private void write(final RegistryFriendlyByteBuf output) {
      output.writeBlockPos(this.pos);
      output.writeByte(this.b0);
      output.writeByte(this.b1);
      ByteBufCodecs.registry(Registries.BLOCK).encode(output, this.block);
   }

   @Override
   public PacketType<ClientboundBlockEventPacket> type() {
      return GamePacketTypes.CLIENTBOUND_BLOCK_EVENT;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleBlockEvent(this);
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
