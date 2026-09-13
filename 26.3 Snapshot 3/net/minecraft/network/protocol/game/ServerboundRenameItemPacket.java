package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundRenameItemPacket implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundRenameItemPacket> STREAM_CODEC = Packet.codec(
      ServerboundRenameItemPacket::write, ServerboundRenameItemPacket::new
   );
   private final String name;

   public ServerboundRenameItemPacket(final String name) {
      this.name = name;
   }

   private ServerboundRenameItemPacket(final FriendlyByteBuf input) {
      this.name = input.readUtf();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeUtf(this.name);
   }

   @Override
   public PacketType<ServerboundRenameItemPacket> type() {
      return GamePacketTypes.SERVERBOUND_RENAME_ITEM;
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handleRenameItem(this);
   }

   public String getName() {
      return this.name;
   }
}
