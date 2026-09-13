package net.minecraft.network.protocol.configuration;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.Identifier;

public record ClientboundUpdateEnabledFeaturesPacket(Set<Identifier> features) implements Packet<ClientConfigurationPacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundUpdateEnabledFeaturesPacket> STREAM_CODEC = Packet.codec(
      ClientboundUpdateEnabledFeaturesPacket::write, ClientboundUpdateEnabledFeaturesPacket::new
   );

   private ClientboundUpdateEnabledFeaturesPacket(FriendlyByteBuf p_299340_) {
      this(p_299340_.readCollection(HashSet::new, FriendlyByteBuf::readIdentifier));
   }

   private void write(FriendlyByteBuf p_297257_) {
      p_297257_.writeCollection(this.features, FriendlyByteBuf::writeIdentifier);
   }

   @Override
   public PacketType<ClientboundUpdateEnabledFeaturesPacket> type() {
      return ConfigurationPacketTypes.CLIENTBOUND_UPDATE_ENABLED_FEATURES;
   }

   public void handle(ClientConfigurationPacketListener p_301161_) {
      p_301161_.handleEnabledFeatures(this);
   }
}
