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

   public ServerboundRenameItemPacket(String p_134396_) {
      this.name = p_134396_;
   }

   private ServerboundRenameItemPacket(FriendlyByteBuf p_179738_) {
      this.name = p_179738_.readUtf();
   }

   private void write(FriendlyByteBuf p_134405_) {
      p_134405_.writeUtf(this.name);
   }

   @Override
   public PacketType<ServerboundRenameItemPacket> type() {
      return GamePacketTypes.SERVERBOUND_RENAME_ITEM;
   }

   public void handle(ServerGamePacketListener p_134402_) {
      p_134402_.handleRenameItem(this);
   }

   public String getName() {
      return this.name;
   }
}
