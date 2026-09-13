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

   public ServerboundCommandSuggestionPacket(int p_133893_, String p_133894_) {
      this.id = p_133893_;
      this.command = p_133894_;
   }

   private ServerboundCommandSuggestionPacket(FriendlyByteBuf p_179565_) {
      this.id = p_179565_.readVarInt();
      this.command = p_179565_.readUtf(32500);
   }

   private void write(FriendlyByteBuf p_133903_) {
      p_133903_.writeVarInt(this.id);
      p_133903_.writeUtf(this.command, 32500);
   }

   @Override
   public PacketType<ServerboundCommandSuggestionPacket> type() {
      return GamePacketTypes.SERVERBOUND_COMMAND_SUGGESTION;
   }

   public void handle(ServerGamePacketListener p_133900_) {
      p_133900_.handleCustomCommandSuggestions(this);
   }

   public int getId() {
      return this.id;
   }

   public String getCommand() {
      return this.command;
   }
}
