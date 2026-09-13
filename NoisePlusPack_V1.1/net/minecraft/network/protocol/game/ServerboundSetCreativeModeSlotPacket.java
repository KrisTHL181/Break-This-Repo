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

   public ServerboundSetCreativeModeSlotPacket(int p_134553_, ItemStack p_134554_) {
      this((short)p_134553_, p_134554_);
   }

   @Override
   public PacketType<ServerboundSetCreativeModeSlotPacket> type() {
      return GamePacketTypes.SERVERBOUND_SET_CREATIVE_MODE_SLOT;
   }

   public void handle(ServerGamePacketListener p_134560_) {
      p_134560_.handleSetCreativeModeSlot(this);
   }
}
