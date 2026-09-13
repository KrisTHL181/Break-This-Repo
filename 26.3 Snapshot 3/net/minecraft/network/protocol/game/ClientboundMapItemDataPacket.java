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
      final MapId mapId,
      final byte scale,
      final boolean locked,
      final @Nullable Collection<MapDecoration> decorations,
      final MapItemSavedData.@Nullable MapPatch colorPatch
   ) {
      this(mapId, scale, locked, decorations != null ? Optional.of(List.copyOf(decorations)) : Optional.empty(), Optional.ofNullable(colorPatch));
   }

   @Override
   public PacketType<ClientboundMapItemDataPacket> type() {
      return GamePacketTypes.CLIENTBOUND_MAP_ITEM_DATA;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleMapItemData(this);
   }

   public void applyToMap(final MapItemSavedData map) {
      this.decorations.ifPresent(map::addClientSideDecorations);
      this.colorPatch.ifPresent(patch -> patch.applyToMap(map));
   }
}
