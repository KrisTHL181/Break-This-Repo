package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundMountScreenOpenPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundMountScreenOpenPacket> STREAM_CODEC = Packet.codec(
      ClientboundMountScreenOpenPacket::write, ClientboundMountScreenOpenPacket::new
   );
   private final int containerId;
   private final int inventoryColumns;
   private final int entityId;

   public ClientboundMountScreenOpenPacket(int p_455855_, int p_454920_, int p_459100_) {
      this.containerId = p_455855_;
      this.inventoryColumns = p_454920_;
      this.entityId = p_459100_;
   }

   private ClientboundMountScreenOpenPacket(FriendlyByteBuf p_453492_) {
      this.containerId = p_453492_.readContainerId();
      this.inventoryColumns = p_453492_.readVarInt();
      this.entityId = p_453492_.readInt();
   }

   private void write(FriendlyByteBuf p_452661_) {
      p_452661_.writeContainerId(this.containerId);
      p_452661_.writeVarInt(this.inventoryColumns);
      p_452661_.writeInt(this.entityId);
   }

   @Override
   public PacketType<ClientboundMountScreenOpenPacket> type() {
      return GamePacketTypes.CLIENTBOUND_MOUNT_SCREEN_OPEN;
   }

   public void handle(ClientGamePacketListener p_457099_) {
      p_457099_.handleMountScreenOpen(this);
   }

   public int getContainerId() {
      return this.containerId;
   }

   public int getInventoryColumns() {
      return this.inventoryColumns;
   }

   public int getEntityId() {
      return this.entityId;
   }
}
