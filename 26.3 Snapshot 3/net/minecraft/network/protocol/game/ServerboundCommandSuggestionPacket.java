package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundCommandSuggestionPacket implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundCommandSuggestionPacket> STREAM_CODEC = Packet.codec(
      ServerboundCommandSuggestionPacket::write, ServerboundCommandSuggestionPacket::new
   );
   private final int id;
   private final String command;

   public ServerboundCommandSuggestionPacket(final int id, final String command) {
      this.id = id;
      this.command = command;
   }

   private ServerboundCommandSuggestionPacket(final FriendlyByteBuf input) {
      this.id = input.readVarInt();
      this.command = input.readUtf(32500);
   }

   private void write(final FriendlyByteBuf output) {
      output.writeVarInt(this.id);
      output.writeUtf(this.command, 32500);
   }

   @Override
   public PacketType<ServerboundCommandSuggestionPacket> type() {
      return GamePacketTypes.SERVERBOUND_COMMAND_SUGGESTION;
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handleCustomCommandSuggestions(this);
   }

   public int getId() {
      return this.id;
   }

   public String getCommand() {
      return this.command;
   }
}
