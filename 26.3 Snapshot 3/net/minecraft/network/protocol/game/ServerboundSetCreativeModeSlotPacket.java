package net.minecraft.network.protocol.game;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.item.ItemStack;

public record ServerboundSetCreativeModeSlotPacket(short slotNum, ItemStack itemStack) implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundSetCreativeModeSlotPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.SHORT,
      ServerboundSetCreativeModeSlotPacket::slotNum,
      ItemStack.validatedStreamCodec(ItemStack.OPTIONAL_UNTRUSTED_STREAM_CODEC),
      ServerboundSetCreativeModeSlotPacket::itemStack,
      ServerboundSetCreativeModeSlotPacket::new
   );

   public ServerboundSetCreativeModeSlotPacket(final int slotNum, final ItemStack itemStack) {
      this((short)slotNum, itemStack);
   }

   @Override
   public PacketType<ServerboundSetCreativeModeSlotPacket> type() {
      return GamePacketTypes.SERVERBOUND_SET_CREATIVE_MODE_SLOT;
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handleSetCreativeModeSlot(this);
   }
}
