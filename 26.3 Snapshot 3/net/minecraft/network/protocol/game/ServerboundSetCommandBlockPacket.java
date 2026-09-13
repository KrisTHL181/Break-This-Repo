package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.level.block.entity.CommandBlockEntity;

public class ServerboundSetCommandBlockPacket implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundSetCommandBlockPacket> STREAM_CODEC = Packet.codec(
      ServerboundSetCommandBlockPacket::write, ServerboundSetCommandBlockPacket::new
   );
   private static final int FLAG_TRACK_OUTPUT = 1;
   private static final int FLAG_CONDITIONAL = 2;
   private static final int FLAG_AUTOMATIC = 4;
   private final BlockPos pos;
   private final String command;
   private final boolean trackOutput;
   private final boolean conditional;
   private final boolean automatic;
   private final CommandBlockEntity.Mode mode;

   public ServerboundSetCommandBlockPacket(
      final BlockPos pos,
      final String command,
      final CommandBlockEntity.Mode mode,
      final boolean trackOutput,
      final boolean conditional,
      final boolean automatic
   ) {
      this.pos = pos;
      this.command = command;
      this.trackOutput = trackOutput;
      this.conditional = conditional;
      this.automatic = automatic;
      this.mode = mode;
   }

   private ServerboundSetCommandBlockPacket(final FriendlyByteBuf input) {
      this.pos = input.readBlockPos();
      this.command = input.readUtf();
      this.mode = input.readEnum(CommandBlockEntity.Mode.class);
      int flags = input.readByte();
      this.trackOutput = (flags & 1) != 0;
      this.conditional = (flags & 2) != 0;
      this.automatic = (flags & 4) != 0;
   }

   private void write(final FriendlyByteBuf output) {
      output.writeBlockPos(this.pos);
      output.writeUtf(this.command);
      output.writeEnum(this.mode);
      int flags = 0;
      if (this.trackOutput) {
         flags |= 1;
      }

      if (this.conditional) {
         flags |= 2;
      }

      if (this.automatic) {
         flags |= 4;
      }

      output.writeByte(flags);
   }

   @Override
   public PacketType<ServerboundSetCommandBlockPacket> type() {
      return GamePacketTypes.SERVERBOUND_SET_COMMAND_BLOCK;
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handleSetCommandBlock(this);
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public String getCommand() {
      return this.command;
   }

   public boolean isTrackOutput() {
      return this.trackOutput;
   }

   public boolean isConditional() {
      return this.conditional;
   }

   public boolean isAutomatic() {
      return this.automatic;
   }

   public CommandBlockEntity.Mode getMode() {
      return this.mode;
   }
}
