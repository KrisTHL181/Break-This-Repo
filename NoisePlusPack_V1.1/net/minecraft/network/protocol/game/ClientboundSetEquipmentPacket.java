package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class ClientboundSetEquipmentPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSetEquipmentPacket> STREAM_CODEC = Packet.codec(
      ClientboundSetEquipmentPacket::write, ClientboundSetEquipmentPacket::new
   );
   private static final byte CONTINUE_MASK = -128;
   private final int entity;
   private final List<Pair<EquipmentSlot, ItemStack>> slots;

   public ClientboundSetEquipmentPacket(int p_133202_, List<Pair<EquipmentSlot, ItemStack>> p_133203_) {
      this.entity = p_133202_;
      this.slots = p_133203_;
   }

   private ClientboundSetEquipmentPacket(RegistryFriendlyByteBuf p_329444_) {
      this.entity = p_329444_.readVarInt();
      this.slots = Lists.newArrayList();

      int i;
      do {
         i = p_329444_.readByte();
         EquipmentSlot equipmentslot = EquipmentSlot.VALUES.get(i & 127);
         ItemStack itemstack = ItemStack.OPTIONAL_STREAM_CODEC.decode(p_329444_);
         this.slots.add(Pair.of(equipmentslot, itemstack));
      } while ((i & -128) != 0);
   }

   private void write(RegistryFriendlyByteBuf p_328455_) {
      p_328455_.writeVarInt(this.entity);
      int i = this.slots.size();

      for (int j = 0; j < i; j++) {
         Pair<EquipmentSlot, ItemStack> pair = this.slots.get(j);
         EquipmentSlot equipmentslot = (EquipmentSlot)pair.getFirst();
         boolean flag = j != i - 1;
         int k = equipmentslot.ordinal();
         p_328455_.writeByte(flag ? k | -128 : k);
         ItemStack.OPTIONAL_STREAM_CODEC.encode(p_328455_, (ItemStack)pair.getSecond());
      }
   }

   @Override
   public PacketType<ClientboundSetEquipmentPacket> type() {
      return GamePacketTypes.CLIENTBOUND_SET_EQUIPMENT;
   }

   public void handle(ClientGamePacketListener p_133209_) {
      p_133209_.handleSetEquipment(this);
   }

   public int getEntity() {
      return this.entity;
   }

   public List<Pair<EquipmentSlot, ItemStack>> getSlots() {
      return this.slots;
   }
}
