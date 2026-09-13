package net.minecraft.network.protocol.common;

import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagNetworkSerialization;

public class ClientboundUpdateTagsPacket implements Packet<ClientCommonPacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundUpdateTagsPacket> STREAM_CODEC = Packet.codec(
      ClientboundUpdateTagsPacket::write, ClientboundUpdateTagsPacket::new
   );
   private final Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> tags;

   public ClientboundUpdateTagsPacket(Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> p_300911_) {
      this.tags = p_300911_;
   }

   private ClientboundUpdateTagsPacket(FriendlyByteBuf p_298277_) {
      this.tags = p_298277_.readMap(FriendlyByteBuf::readRegistryKey, TagNetworkSerialization.NetworkPayload::read);
   }

   private void write(FriendlyByteBuf p_299422_) {
      p_299422_.writeMap(this.tags, FriendlyByteBuf::writeResourceKey, (p_297824_, p_298178_) -> p_298178_.write(p_297824_));
   }

   @Override
   public PacketType<ClientboundUpdateTagsPacket> type() {
      return CommonPacketTypes.CLIENTBOUND_UPDATE_TAGS;
   }

   public void handle(ClientCommonPacketListener p_297999_) {
      p_297999_.handleUpdateTags(this);
   }

   public Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> getTags() {
      return this.tags;
   }
}
