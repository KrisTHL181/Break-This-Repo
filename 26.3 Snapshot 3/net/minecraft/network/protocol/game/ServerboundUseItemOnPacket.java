package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;

public class ServerboundUseItemOnPacket implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundUseItemOnPacket> STREAM_CODEC = Packet.codec(
      ServerboundUseItemOnPacket::write, ServerboundUseItemOnPacket::new
   );
   private final BlockHitResult blockHit;
   private final InteractionHand hand;
   private final int sequence;

   public ServerboundUseItemOnPacket(final InteractionHand hand, final BlockHitResult blockHit, final int sequence) {
      this.hand = hand;
      this.blockHit = blockHit;
      this.sequence = sequence;
   }

   private ServerboundUseItemOnPacket(final FriendlyByteBuf input) {
      this.hand = input.readEnum(InteractionHand.class);
      this.blockHit = input.readBlockHitResult();
      this.sequence = input.readVarInt();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeEnum(this.hand);
      output.writeBlockHitResult(this.blockHit);
      output.writeVarInt(this.sequence);
   }

   @Override
   public PacketType<ServerboundUseItemOnPacket> type() {
      return GamePacketTypes.SERVERBOUND_USE_ITEM_ON;
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handleUseItemOn(this);
   }

   public InteractionHand getHand() {
      return this.hand;
   }

   public BlockHitResult getHitResult() {
      return this.blockHit;
   }

   public int getSequence() {
      return this.sequence;
   }
}
