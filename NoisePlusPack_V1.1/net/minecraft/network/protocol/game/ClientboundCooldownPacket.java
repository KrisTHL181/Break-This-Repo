package net.minecraft.network.protocol.game;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.Identifier;

public record ClientboundCooldownPacket(Identifier cooldownGroup, int duration) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundCooldownPacket> STREAM_CODEC = StreamCodec.composite(
      Identifier.STREAM_CODEC,
      ClientboundCooldownPacket::cooldownGroup,
      ByteBufCodecs.VAR_INT,
      ClientboundCooldownPacket::duration,
      ClientboundCooldownPacket::new
   );

   @Override
   public PacketType<ClientboundCooldownPacket> type() {
      return GamePacketTypes.CLIENTBOUND_COOLDOWN;
   }

   public void handle(ClientGamePacketListener p_132007_) {
      p_132007_.handleItemCooldown(this);
   }
}
