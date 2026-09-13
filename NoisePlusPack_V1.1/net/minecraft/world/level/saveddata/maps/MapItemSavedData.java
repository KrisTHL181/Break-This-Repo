package net.minecraft.world.level.saveddata.maps;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class MapItemSavedData extends SavedData {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAP_SIZE = 128;
   private static final int HALF_MAP_SIZE = 64;
   public static final int MAX_SCALE = 4;
   public static final int TRACKED_DECORATION_LIMIT = 256;
   private static final String FRAME_PREFIX = "frame-";
   public static final Codec<MapItemSavedData> CODEC = RecordCodecBuilder.create(
      p_391106_ -> p_391106_.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(p_391098_ -> p_391098_.dimension),
            Codec.INT.fieldOf("xCenter").forGetter(p_391097_ -> p_391097_.centerX),
            Codec.INT.fieldOf("zCenter").forGetter(p_391096_ -> p_391096_.centerZ),
            Codec.BYTE.optionalFieldOf("scale", (byte)0).forGetter(p_391102_ -> p_391102_.scale),
            Codec.BYTE_BUFFER.fieldOf("colors").forGetter(p_391100_ -> ByteBuffer.wrap(p_391100_.colors)),
            Codec.BOOL.optionalFieldOf("trackingPosition", true).forGetter(p_391101_ -> p_391101_.trackingPosition),
            Codec.BOOL.optionalFieldOf("unlimitedTracking", false).forGetter(p_391099_ -> p_391099_.unlimitedTracking),
            Codec.BOOL.optionalFieldOf("locked", false).forGetter(p_391104_ -> p_391104_.locked),
            MapBanner.CODEC.listOf().optionalFieldOf("banners", List.of()).forGetter(p_391103_ -> List.copyOf(p_391103_.bannerMarkers.values())),
            MapFrame.CODEC.listOf().optionalFieldOf("frames", List.of()).forGetter(p_391105_ -> List.copyOf(p_391105_.frameMarkers.values()))
         )
         .apply(p_391106_, MapItemSavedData::new)
   );
   public final int centerX;
   public final int centerZ;
   public final ResourceKey<Level> dimension;
   private final boolean trackingPosition;
   private final boolean unlimitedTracking;
   public final byte scale;
   public byte[] colors = new byte[16384];
   public final boolean locked;
   private final List<MapItemSavedData.HoldingPlayer> carriedBy = Lists.newArrayList();
   private final Map<Player, MapItemSavedData.HoldingPlayer> carriedByPlayers = Maps.newHashMap();
   private final Map<String, MapBanner> bannerMarkers = Maps.newHashMap();
   final Map<String, MapDecoration> decorations = Maps.newLinkedHashMap();
   private final Map<String, MapFrame> frameMarkers = Maps.newHashMap();
   private int trackedDecorationCount;

   public static SavedDataType<MapItemSavedData> type(MapId p_392603_) {
      return new SavedDataType<>(p_392603_.key(), () -> {
         throw new IllegalStateException("Should never create an empty map saved data");
      }, CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);
   }

   private MapItemSavedData(int p_164768_, int p_164769_, byte p_164770_, boolean p_164771_, boolean p_164772_, boolean p_164773_, ResourceKey<Level> p_164774_) {
      this.scale = p_164770_;
      this.centerX = p_164768_;
      this.centerZ = p_164769_;
      this.dimension = p_164774_;
      this.trackingPosition = p_164771_;
      this.unlimitedTracking = p_164772_;
      this.locked = p_164773_;
   }

   private MapItemSavedData(
      ResourceKey<Level> p_392020_,
      int p_393271_,
      int p_395708_,
      byte p_397666_,
      ByteBuffer p_397898_,
      boolean p_394192_,
      boolean p_397535_,
      boolean p_395624_,
      List<MapBanner> p_397829_,
      List<MapFrame> p_394048_
   ) {
      this(p_393271_, p_395708_, (byte)Mth.clamp(p_397666_, 0, 4), p_394192_, p_397535_, p_395624_, p_392020_);
      if (p_397898_.array().length == 16384) {
         this.colors = p_397898_.array();
      }

      for (MapBanner mapbanner : p_397829_) {
         this.bannerMarkers.put(mapbanner.getId(), mapbanner);
         this.addDecoration(
            mapbanner.getDecoration(), null, mapbanner.getId(), mapbanner.pos().getX(), mapbanner.pos().getZ(), 180.0, mapbanner.name().orElse(null)
         );
      }

      for (MapFrame mapframe : p_394048_) {
         this.frameMarkers.put(mapframe.getId(), mapframe);
         this.addDecoration(
            MapDecorationTypes.FRAME, null, getFrameKey(mapframe.entityId()), mapframe.pos().getX(), mapframe.pos().getZ(), mapframe.rotation(), null
         );
      }
   }

   public static MapItemSavedData createFresh(
      double p_164781_, double p_164782_, byte p_164783_, boolean p_164784_, boolean p_164785_, ResourceKey<Level> p_164786_
   ) {
      int i = 128 * (1 << p_164783_);
      int j = Mth.floor((p_164781_ + 64.0) / i);
      int k = Mth.floor((p_164782_ + 64.0) / i);
      int l = j * i + i / 2 - 64;
      int i1 = k * i + i / 2 - 64;
      return new MapItemSavedData(l, i1, p_164783_, p_164784_, p_164785_, false, p_164786_);
   }

   public static MapItemSavedData createForClient(byte p_164777_, boolean p_164778_, ResourceKey<Level> p_164779_) {
      return new MapItemSavedData(0, 0, p_164777_, false, false, p_164778_, p_164779_);
   }

   public MapItemSavedData locked() {
      MapItemSavedData mapitemsaveddata = new MapItemSavedData(
         this.centerX, this.centerZ, this.scale, this.trackingPosition, this.unlimitedTracking, true, this.dimension
      );
      mapitemsaveddata.bannerMarkers.putAll(this.bannerMarkers);
      mapitemsaveddata.decorations.putAll(this.decorations);
      mapitemsaveddata.trackedDecorationCount = this.trackedDecorationCount;
      System.arraycopy(this.colors, 0, mapitemsaveddata.colors, 0, this.colors.length);
      return mapitemsaveddata;
   }

   public MapItemSavedData scaled() {
      return createFresh(this.centerX, this.centerZ, (byte)Mth.clamp(this.scale + 1, 0, 4), this.trackingPosition, this.unlimitedTracking, this.dimension);
   }

   private static Predicate<ItemStack> mapMatcher(ItemStack p_331084_) {
      MapId mapid = p_331084_.get(DataComponents.MAP_ID);
      return p_327526_ -> p_327526_ == p_331084_ ? true : p_327526_.is(p_331084_.getItem()) && Objects.equals(mapid, p_327526_.get(DataComponents.MAP_ID));
   }

   public void tickCarriedBy(Player p_77919_, ItemStack p_77920_) {
      if (!this.carriedByPlayers.containsKey(p_77919_)) {
         MapItemSavedData.HoldingPlayer mapitemsaveddata$holdingplayer = new MapItemSavedData.HoldingPlayer(p_77919_);
         this.carriedByPlayers.put(p_77919_, mapitemsaveddata$holdingplayer);
         this.carriedBy.add(mapitemsaveddata$holdingplayer);
      }

      Predicate<ItemStack> predicate = mapMatcher(p_77920_);
      if (!p_77919_.getInventory().contains(predicate)) {
         this.removeDecoration(p_77919_.getPlainTextName());
      }

      for (int i = 0; i < this.carriedBy.size(); i++) {
         MapItemSavedData.HoldingPlayer mapitemsaveddata$holdingplayer1 = this.carriedBy.get(i);
         Player player = mapitemsaveddata$holdingplayer1.player;
         String s = player.getPlainTextName();
         if (!player.isRemoved() && (player.getInventory().contains(predicate) || p_77920_.isFramed())) {
            if (!p_77920_.isFramed() && player.level().dimension() == this.dimension && this.trackingPosition) {
               this.addDecoration(MapDecorationTypes.PLAYER, player.level(), s, player.getX(), player.getZ(), player.getYRot(), null);
            }
         } else {
            this.carriedByPlayers.remove(player);
            this.carriedBy.remove(mapitemsaveddata$holdingplayer1);
            this.removeDecoration(s);
         }

         if (!player.equals(p_77919_) && hasMapInvisibilityItemEquipped(player)) {
            this.removeDecoration(s);
         }
      }

      if (p_77920_.isFramed() && this.trackingPosition) {
         ItemFrame itemframe = p_77920_.getFrame();
         BlockPos blockpos = itemframe.getPos();
         MapFrame mapframe1 = this.frameMarkers.get(MapFrame.frameId(blockpos));
         if (mapframe1 != null && itemframe.getId() != mapframe1.entityId() && this.frameMarkers.containsKey(mapframe1.getId())) {
            this.removeDecoration(getFrameKey(mapframe1.entityId()));
         }

         MapFrame mapframe2 = new MapFrame(blockpos, itemframe.getDirection().get2DDataValue() * 90, itemframe.getId());
         this.addDecoration(
            MapDecorationTypes.FRAME,
            p_77919_.level(),
            getFrameKey(itemframe.getId()),
            blockpos.getX(),
            blockpos.getZ(),
            itemframe.getDirection().get2DDataValue() * 90,
            null
         );
         MapFrame mapframe = this.frameMarkers.put(mapframe2.getId(), mapframe2);
         if (!mapframe2.equals(mapframe)) {
            this.setDirty();
         }
      }

      MapDecorations mapdecorations = p_77920_.getOrDefault(DataComponents.MAP_DECORATIONS, MapDecorations.EMPTY);
      if (!this.decorations.keySet().containsAll(mapdecorations.decorations().keySet())) {
         mapdecorations.decorations().forEach((p_450058_, p_450059_) -> {
            if (!this.decorations.containsKey(p_450058_)) {
               this.addDecoration(p_450059_.type(), p_77919_.level(), p_450058_, p_450059_.x(), p_450059_.z(), p_450059_.rotation(), null);
            }
         });
      }
   }

   private static boolean hasMapInvisibilityItemEquipped(Player p_367828_) {
      for (EquipmentSlot equipmentslot : EquipmentSlot.values()) {
         if (equipmentslot != EquipmentSlot.MAINHAND
            && equipmentslot != EquipmentSlot.OFFHAND
            && p_367828_.getItemBySlot(equipmentslot).is(ItemTags.MAP_INVISIBILITY_EQUIPMENT)) {
            return true;
         }
      }

      return false;
   }

   private void removeDecoration(String p_164800_) {
      MapDecoration mapdecoration = this.decorations.remove(p_164800_);
      if (mapdecoration != null && mapdecoration.type().value().trackCount()) {
         this.trackedDecorationCount--;
      }

      this.setDecorationsDirty();
   }

   public static void addTargetDecoration(ItemStack p_77926_, BlockPos p_77927_, String p_77928_, Holder<MapDecorationType> p_335418_) {
      MapDecorations.Entry mapdecorations$entry = new MapDecorations.Entry(p_335418_, p_77927_.getX(), p_77927_.getZ(), 180.0F);
      p_77926_.update(DataComponents.MAP_DECORATIONS, MapDecorations.EMPTY, p_327532_ -> p_327532_.withDecoration(p_77928_, mapdecorations$entry));
      if (p_335418_.value().hasMapColor()) {
         p_77926_.set(DataComponents.MAP_COLOR, new MapItemColor(p_335418_.value().mapColor()));
      }
   }

   private void addDecoration(
      Holder<MapDecorationType> p_333390_,
      @Nullable LevelAccessor p_77939_,
      String p_77940_,
      double p_77941_,
      double p_77942_,
      double p_77943_,
      @Nullable Component p_77944_
   ) {
      int i = 1 << this.scale;
      float f = (float)(p_77941_ - this.centerX) / i;
      float f1 = (float)(p_77942_ - this.centerZ) / i;
      MapItemSavedData.MapDecorationLocation mapitemsaveddata$mapdecorationlocation = this.calculateDecorationLocationAndType(
         p_333390_, p_77939_, p_77943_, f, f1
      );
      if (mapitemsaveddata$mapdecorationlocation == null) {
         this.removeDecoration(p_77940_);
      } else {
         MapDecoration mapdecoration = new MapDecoration(
            mapitemsaveddata$mapdecorationlocation.type(),
            mapitemsaveddata$mapdecorationlocation.x(),
            mapitemsaveddata$mapdecorationlocation.y(),
            mapitemsaveddata$mapdecorationlocation.rot(),
            Optional.ofNullable(p_77944_)
         );
         MapDecoration mapdecoration1 = this.decorations.put(p_77940_, mapdecoration);
         if (!mapdecoration.equals(mapdecoration1)) {
            if (mapdecoration1 != null && mapdecoration1.type().value().trackCount()) {
               this.trackedDecorationCount--;
            }

            if (mapitemsaveddata$mapdecorationlocation.type().value().trackCount()) {
               this.trackedDecorationCount++;
            }

            this.setDecorationsDirty();
         }
      }
   }

   private MapItemSavedData.@Nullable MapDecorationLocation calculateDecorationLocationAndType(
      Holder<MapDecorationType> p_361847_, @Nullable LevelAccessor p_361669_, double p_364097_, float p_366348_, float p_369890_
   ) {
      byte b0 = clampMapCoordinate(p_366348_);
      byte b1 = clampMapCoordinate(p_369890_);
      if (p_361847_.is(MapDecorationTypes.PLAYER)) {
         Pair<Holder<MapDecorationType>, Byte> pair = this.playerDecorationTypeAndRotation(p_361847_, p_361669_, p_364097_, p_366348_, p_369890_);
         return pair == null ? null : new MapItemSavedData.MapDecorationLocation((Holder<MapDecorationType>)pair.getFirst(), b0, b1, (Byte)pair.getSecond());
      } else {
         return !isInsideMap(p_366348_, p_369890_) && !this.unlimitedTracking
            ? null
            : new MapItemSavedData.MapDecorationLocation(p_361847_, b0, b1, this.calculateRotation(p_361669_, p_364097_));
      }
   }

   private @Nullable Pair<Holder<MapDecorationType>, Byte> playerDecorationTypeAndRotation(
      Holder<MapDecorationType> p_363889_, @Nullable LevelAccessor p_361689_, double p_367676_, float p_364470_, float p_361732_
   ) {
      if (isInsideMap(p_364470_, p_361732_)) {
         return Pair.of(p_363889_, this.calculateRotation(p_361689_, p_367676_));
      }

      Holder<MapDecorationType> holder = this.decorationTypeForPlayerOutsideMap(p_364470_, p_361732_);
      return holder == null ? null : Pair.of(holder, (byte)0);
   }

   private byte calculateRotation(@Nullable LevelAccessor p_366972_, double p_368862_) {
      if (this.dimension == Level.NETHER && p_366972_ != null) {
         int i = (int)(p_366972_.getGameTime() / 10L);
         return (byte)(i * i * 34187121 + i * 121 >> 15 & 15);
      } else {
         double d0 = p_368862_ < 0.0 ? p_368862_ - 8.0 : p_368862_ + 8.0;
         return (byte)(d0 * 16.0 / 360.0);
      }
   }

   private static boolean isInsideMap(float p_365691_, float p_362576_) {
      int i = 63;
      return p_365691_ >= -63.0F && p_362576_ >= -63.0F && p_365691_ <= 63.0F && p_362576_ <= 63.0F;
   }

   private @Nullable Holder<MapDecorationType> decorationTypeForPlayerOutsideMap(float p_361505_, float p_369187_) {
      int i = 320;
      boolean flag = Math.abs(p_361505_) < 320.0F && Math.abs(p_369187_) < 320.0F;
      if (flag) {
         return MapDecorationTypes.PLAYER_OFF_MAP;
      } else {
         return this.unlimitedTracking ? MapDecorationTypes.PLAYER_OFF_LIMITS : null;
      }
   }

   private static byte clampMapCoordinate(float p_365103_) {
      int i = 63;
      if (p_365103_ <= -63.0F) {
         return -128;
      } else {
         return p_365103_ >= 63.0F ? 127 : (byte)(p_365103_ * 2.0F + 0.5);
      }
   }

   public @Nullable Packet<?> getUpdatePacket(MapId p_328547_, Player p_164798_) {
      MapItemSavedData.HoldingPlayer mapitemsaveddata$holdingplayer = this.carriedByPlayers.get(p_164798_);
      return mapitemsaveddata$holdingplayer == null ? null : mapitemsaveddata$holdingplayer.nextUpdatePacket(p_328547_);
   }

   private void setColorsDirty(int p_164790_, int p_164791_) {
      this.setDirty();

      for (MapItemSavedData.HoldingPlayer mapitemsaveddata$holdingplayer : this.carriedBy) {
         mapitemsaveddata$holdingplayer.markColorsDirty(p_164790_, p_164791_);
      }
   }

   private void setDecorationsDirty() {
      this.carriedBy.forEach(MapItemSavedData.HoldingPlayer::markDecorationsDirty);
   }

   public MapItemSavedData.HoldingPlayer getHoldingPlayer(Player p_77917_) {
      MapItemSavedData.HoldingPlayer mapitemsaveddata$holdingplayer = this.carriedByPlayers.get(p_77917_);
      if (mapitemsaveddata$holdingplayer == null) {
         mapitemsaveddata$holdingplayer = new MapItemSavedData.HoldingPlayer(p_77917_);
         this.carriedByPlayers.put(p_77917_, mapitemsaveddata$holdingplayer);
         this.carriedBy.add(mapitemsaveddata$holdingplayer);
      }

      return mapitemsaveddata$holdingplayer;
   }

   public boolean toggleBanner(LevelAccessor p_77935_, BlockPos p_77936_) {
      double d0 = p_77936_.getX() + 0.5;
      double d1 = p_77936_.getZ() + 0.5;
      int i = 1 << this.scale;
      double d2 = (d0 - this.centerX) / i;
      double d3 = (d1 - this.centerZ) / i;
      int j = 63;
      if (d2 >= -63.0 && d3 >= -63.0 && d2 <= 63.0 && d3 <= 63.0) {
         MapBanner mapbanner = MapBanner.fromWorld(p_77935_, p_77936_);
         if (mapbanner == null) {
            return false;
         }

         if (this.bannerMarkers.remove(mapbanner.getId(), mapbanner)) {
            this.removeDecoration(mapbanner.getId());
            this.setDirty();
            return true;
         }

         if (!this.isTrackedCountOverLimit(256)) {
            this.bannerMarkers.put(mapbanner.getId(), mapbanner);
            this.addDecoration(mapbanner.getDecoration(), p_77935_, mapbanner.getId(), d0, d1, 180.0, mapbanner.name().orElse(null));
            this.setDirty();
            return true;
         }
      }

      return false;
   }

   public void checkBanners(BlockGetter p_77931_, int p_77932_, int p_77933_) {
      Iterator<MapBanner> iterator = this.bannerMarkers.values().iterator();

      while (iterator.hasNext()) {
         MapBanner mapbanner = iterator.next();
         if (mapbanner.pos().getX() == p_77932_ && mapbanner.pos().getZ() == p_77933_) {
            MapBanner mapbanner1 = MapBanner.fromWorld(p_77931_, mapbanner.pos());
            if (!mapbanner.equals(mapbanner1)) {
               iterator.remove();
               this.removeDecoration(mapbanner.getId());
               this.setDirty();
            }
         }
      }
   }

   public Collection<MapBanner> getBanners() {
      return this.bannerMarkers.values();
   }

   public void removedFromFrame(BlockPos p_77948_, int p_77949_) {
      this.removeDecoration(getFrameKey(p_77949_));
      this.frameMarkers.remove(MapFrame.frameId(p_77948_));
      this.setDirty();
   }

   public boolean updateColor(int p_164793_, int p_164794_, byte p_164795_) {
      byte b0 = this.colors[p_164793_ + p_164794_ * 128];
      if (b0 != p_164795_) {
         this.setColor(p_164793_, p_164794_, p_164795_);
         return true;
      } else {
         return false;
      }
   }

   public void setColor(int p_164804_, int p_164805_, byte p_164806_) {
      this.colors[p_164804_ + p_164805_ * 128] = p_164806_;
      this.setColorsDirty(p_164804_, p_164805_);
   }

   public boolean isExplorationMap() {
      for (MapDecoration mapdecoration : this.decorations.values()) {
         if (mapdecoration.type().value().explorationMapElement()) {
            return true;
         }
      }

      return false;
   }

   public void addClientSideDecorations(List<MapDecoration> p_164802_) {
      this.decorations.clear();
      this.trackedDecorationCount = 0;

      for (int i = 0; i < p_164802_.size(); i++) {
         MapDecoration mapdecoration = p_164802_.get(i);
         this.decorations.put("icon-" + i, mapdecoration);
         if (mapdecoration.type().value().trackCount()) {
            this.trackedDecorationCount++;
         }
      }
   }

   public Iterable<MapDecoration> getDecorations() {
      return this.decorations.values();
   }

   public boolean isTrackedCountOverLimit(int p_181313_) {
      return this.trackedDecorationCount >= p_181313_;
   }

   private static String getFrameKey(int p_342097_) {
      return "frame-" + p_342097_;
   }

   public class HoldingPlayer {
      public final Player player;
      private boolean dirtyData = true;
      private int minDirtyX;
      private int minDirtyY;
      private int maxDirtyX = 127;
      private int maxDirtyY = 127;
      private boolean dirtyDecorations = true;
      private int tick;
      public int step;

      HoldingPlayer(final Player p_77970_) {
         this.player = p_77970_;
      }

      private MapItemSavedData.MapPatch createPatch() {
         int i = this.minDirtyX;
         int j = this.minDirtyY;
         int k = this.maxDirtyX + 1 - this.minDirtyX;
         int l = this.maxDirtyY + 1 - this.minDirtyY;
         byte[] abyte = new byte[k * l];

         for (int i1 = 0; i1 < k; i1++) {
            for (int j1 = 0; j1 < l; j1++) {
               abyte[i1 + j1 * k] = MapItemSavedData.this.colors[i + i1 + (j + j1) * 128];
            }
         }

         return new MapItemSavedData.MapPatch(i, j, k, l, abyte);
      }

      @Nullable Packet<?> nextUpdatePacket(MapId p_331779_) {
         MapItemSavedData.MapPatch mapitemsaveddata$mappatch;
         if (this.dirtyData) {
            this.dirtyData = false;
            mapitemsaveddata$mappatch = this.createPatch();
         } else {
            mapitemsaveddata$mappatch = null;
         }

         Collection<MapDecoration> collection;
         if (this.dirtyDecorations && this.tick++ % 5 == 0) {
            this.dirtyDecorations = false;
            collection = MapItemSavedData.this.decorations.values();
         } else {
            collection = null;
         }

         return collection == null && mapitemsaveddata$mappatch == null
            ? null
            : new ClientboundMapItemDataPacket(p_331779_, MapItemSavedData.this.scale, MapItemSavedData.this.locked, collection, mapitemsaveddata$mappatch);
      }

      void markColorsDirty(int p_164818_, int p_164819_) {
         if (this.dirtyData) {
            this.minDirtyX = Math.min(this.minDirtyX, p_164818_);
            this.minDirtyY = Math.min(this.minDirtyY, p_164819_);
            this.maxDirtyX = Math.max(this.maxDirtyX, p_164818_);
            this.maxDirtyY = Math.max(this.maxDirtyY, p_164819_);
         } else {
            this.dirtyData = true;
            this.minDirtyX = p_164818_;
            this.minDirtyY = p_164819_;
            this.maxDirtyX = p_164818_;
            this.maxDirtyY = p_164819_;
         }
      }

      private void markDecorationsDirty() {
         this.dirtyDecorations = true;
      }
   }

   record MapDecorationLocation(Holder<MapDecorationType> type, byte x, byte y, byte rot) {
   }

   public record MapPatch(int startX, int startY, int width, int height, byte[] mapColors) {
      public static final StreamCodec<ByteBuf, Optional<MapItemSavedData.MapPatch>> STREAM_CODEC = StreamCodec.of(
         MapItemSavedData.MapPatch::write, MapItemSavedData.MapPatch::read
      );

      private static void write(ByteBuf p_334846_, Optional<MapItemSavedData.MapPatch> p_333957_) {
         if (p_333957_.isPresent()) {
            MapItemSavedData.MapPatch mapitemsaveddata$mappatch = p_333957_.get();
            p_334846_.writeByte(mapitemsaveddata$mappatch.width);
            p_334846_.writeByte(mapitemsaveddata$mappatch.height);
            p_334846_.writeByte(mapitemsaveddata$mappatch.startX);
            p_334846_.writeByte(mapitemsaveddata$mappatch.startY);
            FriendlyByteBuf.writeByteArray(p_334846_, mapitemsaveddata$mappatch.mapColors);
         } else {
            p_334846_.writeByte(0);
         }
      }

      private static Optional<MapItemSavedData.MapPatch> read(ByteBuf p_332582_) {
         int i = p_332582_.readUnsignedByte();
         if (i > 0) {
            int j = p_332582_.readUnsignedByte();
            int k = p_332582_.readUnsignedByte();
            int l = p_332582_.readUnsignedByte();
            byte[] abyte = FriendlyByteBuf.readByteArray(p_332582_);
            return Optional.of(new MapItemSavedData.MapPatch(k, l, i, j, abyte));
         } else {
            return Optional.empty();
         }
      }

      public void applyToMap(MapItemSavedData p_164833_) {
         for (int i = 0; i < this.width; i++) {
            for (int j = 0; j < this.height; j++) {
               p_164833_.setColor(this.startX + i, this.startY + j, this.mapColors[i + j * this.width]);
            }
         }
      }
   }
}
