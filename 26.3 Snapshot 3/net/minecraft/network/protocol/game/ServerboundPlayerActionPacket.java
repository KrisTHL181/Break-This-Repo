package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundPlayerActionPacket implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundPlayerActionPacket> STREAM_CODEC = Packet.codec(
      ServerboundPlayerActionPacket::write, ServerboundPlayerActionPacket::new
   );
   private final BlockPos pos;
   private final Direction direction;
   private final ServerboundPlayerActionPacket.Action action;
   private final int sequence;

   public ServerboundPlayerActionPacket(final ServerboundPlayerActionPacket.Action action, final BlockPos pos, final Direction direction, final int sequence) {
      this.action = action;
      this.pos = pos.immutable();
      this.direction = direction;
      this.sequence = sequence;
   }

   public ServerboundPlayerActionPacket(final ServerboundPlayerActionPacket.Action action, final BlockPos pos, final Direction direction) {
      this(action, pos, direction, 0);
   }

   private ServerboundPlayerActionPacket(final FriendlyByteBuf input) {
      this.action = input.readEnum(ServerboundPlayerActionPacket.Action.class);
      this.pos = input.readBlockPos();
      this.direction = Direction.from3DDataValue(input.readUnsignedByte());
      this.sequence = input.readVarInt();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeEnum(this.action);
      output.writeBlockPos(this.pos);
      output.writeByte(this.direction.get3DDataValue());
      output.writeVarInt(this.sequence);
   }

   @Override
   public PacketType<ServerboundPlayerActionPacket> type() {
      return GamePacketTypes.SERVERBOUND_PLAYER_ACTION;
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handlePlayerAction(this);
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public Direction getDirection() {
      return this.direction;
   }

   public ServerboundPlayerActionPacket.Action getAction() {
      return this.action;
   }

   public int getSequence() {
      return this.sequence;
   }

   public enum Action {
      START_DESTROY_BLOCK,
      ABORT_DESTROY_BLOCK,
      STOP_DESTROY_BLOCK,
      DROP_ALL_ITEMS,
      DROP_ITEM,
      RELEASE_USE_ITEM,
      SWAP_ITEM_WITH_OFFHAND,
      STAB;
   }
}
