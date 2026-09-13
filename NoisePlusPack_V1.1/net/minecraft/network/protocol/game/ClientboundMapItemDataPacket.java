package net.minecraft.network.protocol.game;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jspecify.annotations.Nullable;

public record ClientboundMapItemDataPacket(
   MapId mapId, byte scale, boolean locked, Optional<List<MapDecoration>> decorations, Optional<MapItemSavedData.MapPatch> colorPatch
) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundMapItemDataPacket> STREAM_CODEC = StreamCodec.composite(
      MapId.STREAM_CODEC,
      ClientboundMapItemDataPacket::mapId,
      ByteBufCodecs.BYTE,
      ClientboundMapItemDataPacket::scale,
      ByteBufCodecs.BOOL,
      ClientboundMapItemDataPacket::locked,
      MapDecoration.STREAM_CODEC.apply(ByteBufCodecs.list()).apply(ByteBufCodecs::optional),
      ClientboundMapItemDataPacket::decorations,
      MapItemSavedData.MapPatch.STREAM_CODEC,
      ClientboundMapItemDataPacket::colorPatch,
      ClientboundMapItemDataPacket::new
   );

   public ClientboundMapItemDataPacket(
      MapId p_332536_, byte p_327887_, boolean p_335452_, @Nullable Collection<MapDecoration> p_328950_, MapItemSavedData.@Nullable MapPatch p_329006_
   ) {
      this(p_332536_, p_327887_, p_335452_, p_328950_ != null ? Optional.of(List.copyOf(p_328950_)) : Optional.empty(), Optional.ofNullable(p_329006_));
   }

   @Override
   public PacketType<ClientboundMapItemDataPacket> type() {
      return GamePacketTypes.CLIENTBOUND_MAP_ITEM_DATA;
   }

   public void handle(ClientGamePacketListener p_132444_) {
      p_132444_.handleMapItemData(this);
   }

   public void applyToMap(MapItemSavedData p_132438_) {
      this.decorations.ifPresent(p_132438_::addClientSideDecorations);
      this.colorPatch.ifPresent(p_326099_ -> p_326099_.applyToMap(p_132438_));
   }
}
