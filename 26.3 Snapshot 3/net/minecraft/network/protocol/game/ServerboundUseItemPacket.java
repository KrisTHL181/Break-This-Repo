package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.InteractionHand;

public class ServerboundUseItemPacket implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundUseItemPacket> STREAM_CODEC = Packet.codec(
      ServerboundUseItemPacket::write, ServerboundUseItemPacket::new
   );
   private final InteractionHand hand;
   private final int sequence;
   private final float yRot;
   private final float xRot;

   public ServerboundUseItemPacket(final InteractionHand hand, final int sequence, final float yRot, final float xRot) {
      this.hand = hand;
      this.sequence = sequence;
      this.yRot = yRot;
      this.xRot = xRot;
   }

   private ServerboundUseItemPacket(final FriendlyByteBuf input) {
      this.hand = input.readEnum(InteractionHand.class);
      this.sequence = input.readVarInt();
      this.yRot = input.readFloat();
      this.xRot = input.readFloat();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeEnum(this.hand);
      output.writeVarInt(this.sequence);
      output.writeFloat(this.yRot);
      output.writeFloat(this.xRot);
   }

   @Override
   public PacketType<ServerboundUseItemPacket> type() {
      return GamePacketTypes.SERVERBOUND_USE_ITEM;
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handleUseItem(this);
   }

   public InteractionHand getHand() {
      return this.hand;
   }

   public int getSequence() {
      return this.sequence;
   }

   public float getYRot() {
      return this.yRot;
   }

   public float getXRot() {
      return this.xRot;
   }
}
