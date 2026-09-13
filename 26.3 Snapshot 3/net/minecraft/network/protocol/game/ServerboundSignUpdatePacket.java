package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundSignUpdatePacket implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundSignUpdatePacket> STREAM_CODEC = Packet.codec(
      ServerboundSignUpdatePacket::write, ServerboundSignUpdatePacket::new
   );
   private static final int MAX_STRING_LENGTH = 384;
   private final BlockPos pos;
   private final String[] lines;
   private final boolean isFrontText;

   public ServerboundSignUpdatePacket(
      final BlockPos pos, final boolean isFrontText, final String line0, final String line1, final String line2, final String line3
   ) {
      this.pos = pos;
      this.isFrontText = isFrontText;
      this.lines = new String[]{line0, line1, line2, line3};
   }

   private ServerboundSignUpdatePacket(final FriendlyByteBuf input) {
      this.pos = input.readBlockPos();
      this.isFrontText = input.readBoolean();
      this.lines = new String[4];

      for (int i = 0; i < 4; i++) {
         this.lines[i] = input.readUtf(384);
      }
   }

   private void write(final FriendlyByteBuf output) {
      output.writeBlockPos(this.pos);
      output.writeBoolean(this.isFrontText);

      for (int i = 0; i < 4; i++) {
         output.writeUtf(this.lines[i]);
      }
   }

   @Override
   public PacketType<ServerboundSignUpdatePacket> type() {
      return GamePacketTypes.SERVERBOUND_SIGN_UPDATE;
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handleSignUpdate(this);
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public boolean isFrontText() {
      return this.isFrontText;
   }

   public String[] getLines() {
      return this.lines;
   }
}
