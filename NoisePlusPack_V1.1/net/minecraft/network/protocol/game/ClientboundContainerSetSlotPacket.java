package net.minecraft.network.protocol.game;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.item.ItemStack;

public class ClientboundContainerSetSlotPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundContainerSetSlotPacket> STREAM_CODEC = Packet.codec(
      ClientboundContainerSetSlotPacket::write, ClientboundContainerSetSlotPacket::new
   );
   private final int containerId;
   private final int stateId;
   private final int slot;
   private final ItemStack itemStack;

   public ClientboundContainerSetSlotPacket(int p_131982_, int p_182713_, int p_131983_, ItemStack p_131984_) {
      this.containerId = p_131982_;
      this.stateId = p_182713_;
      this.slot = p_131983_;
      this.itemStack = p_131984_.copy();
   }

   private ClientboundContainerSetSlotPacket(RegistryFriendlyByteBuf p_334368_) {
      this.containerId = p_334368_.readContainerId();
      this.stateId = p_334368_.readVarInt();
      this.slot = p_334368_.readShort();
      this.itemStack = ItemStack.OPTIONAL_STREAM_CODEC.decode(p_334368_);
   }

   private void write(RegistryFriendlyByteBuf p_330631_) {
      p_330631_.writeContainerId(this.containerId);
      p_330631_.writeVarInt(this.stateId);
      p_330631_.writeShort(this.slot);
      ItemStack.OPTIONAL_STREAM_CODEC.encode(p_330631_, this.itemStack);
   }

   @Override
   public PacketType<ClientboundContainerSetSlotPacket> type() {
      return GamePacketTypes.CLIENTBOUND_CONTAINER_SET_SLOT;
   }

   public void handle(ClientGamePacketListener p_131990_) {
      p_131990_.handleContainerSetSlot(this);
   }

   public int getContainerId() {
      return this.containerId;
   }

   public int getSlot() {
      return this.slot;
   }

   public ItemStack getItem() {
      return this.itemStack;
   }

   public int getStateId() {
      return this.stateId;
   }
}
