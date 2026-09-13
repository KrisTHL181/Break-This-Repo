package net.minecraft.world.entity.ai.village.poi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Util;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.debug.DebugPoiInfo;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class PoiSection {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Short2ObjectMap<PoiRecord> records = new Short2ObjectOpenHashMap();
   private final Map<Holder<PoiType>, Set<PoiRecord>> byType = Maps.newHashMap();
   private final Runnable setDirty;
   private boolean isValid;

   public PoiSection(final Runnable setDirty) {
      this(setDirty, true, ImmutableList.of());
   }

   private PoiSection(final Runnable setDirty, final boolean isValid, final List<PoiRecord> records) {
      this.setDirty = setDirty;
      this.isValid = isValid;
      records.forEach(this::add);
   }

   public PoiSection.Packed pack() {
      return new PoiSection.Packed(this.isValid, this.records.values().stream().map(PoiRecord::pack).toList());
   }

   public Stream<PoiRecord> getRecords(final Predicate<Holder<PoiType>> predicate, final PoiManager.Occupancy occupancy) {
      return this.byType.entrySet().stream().filter(e -> predicate.test(e.getKey())).flatMap(e -> e.getValue().stream()).filter(occupancy.getTest());
   }

   public @Nullable PoiRecord add(final BlockPos blockPos, final Holder<PoiType> type) {
      PoiRecord record = new PoiRecord(blockPos, type, this.setDirty);
      if (this.add(record)) {
         LOGGER.debug("Added POI of type {} @ {}", type.getRegisteredName(), blockPos);
         this.setDirty.run();
         return record;
      } else {
         return null;
      }
   }

   private boolean add(final PoiRecord record) {
      BlockPos blockPos = record.getPos();
      Holder<PoiType> poiType = record.getPoiType();
      short key = SectionPos.sectionRelativePos(blockPos);
      PoiRecord oldRecord = (PoiRecord)this.records.get(key);
      if (oldRecord != null) {
         if (poiType.equals(oldRecord.getPoiType())) {
            return false;
         }

         Util.logAndPauseIfInIde("POI data mismatch: already registered at " + blockPos);
      }

      this.records.put(key, record);
      this.byType.computeIfAbsent(poiType, k -> Sets.newHashSet()).add(record);
      return true;
   }

   public void remove(final BlockPos pos) {
      PoiRecord poiRecord = (PoiRecord)this.records.remove(SectionPos.sectionRelativePos(pos));
      if (poiRecord == null) {
         LOGGER.error("POI data mismatch: never registered at {}", pos);
      } else {
         this.byType.get(poiRecord.getPoiType()).remove(poiRecord);
         LOGGER.debug("Removed POI of type {} @ {}", LogUtils.defer(poiRecord::getPoiType), LogUtils.defer(poiRecord::getPos));
         this.setDirty.run();
      }
   }

   @Deprecated
   @VisibleForDebug
   public int getFreeTickets(final BlockPos pos) {
      return this.getPoiRecord(pos).map(PoiRecord::getFreeTickets).orElse(0);
   }

   public boolean release(final BlockPos pos) {
      PoiRecord record = (PoiRecord)this.records.get(SectionPos.sectionRelativePos(pos));
      if (record == null) {
         throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("POI never registered at " + pos));
      }

      boolean success = record.releaseTicket();
      this.setDirty.run();
      return success;
   }

   public boolean exists(final BlockPos pos, final Predicate<Holder<PoiType>> predicate) {
      return this.getType(pos).filter(predicate).isPresent();
   }

   public Optional<Holder<PoiType>> getType(final BlockPos pos) {
      return this.getPoiRecord(pos).map(PoiRecord::getPoiType);
   }

   private Optional<PoiRecord> getPoiRecord(final BlockPos pos) {
      return Optional.ofNullable((PoiRecord)this.records.get(SectionPos.sectionRelativePos(pos)));
   }

   public Optional<DebugPoiInfo> getDebugPoiInfo(final BlockPos pos) {
      return this.getPoiRecord(pos).map(DebugPoiInfo::new);
   }

   public void refresh(final Consumer<BiConsumer<BlockPos, Holder<PoiType>>> updater) {
      if (!this.isValid) {
         Short2ObjectMap<PoiRecord> oldRecords = new Short2ObjectOpenHashMap(this.records);
         this.clear();
         updater.accept((blockPos, poiType) -> {
            short key = SectionPos.sectionRelativePos(blockPos);
            PoiRecord newRecord = (PoiRecord)oldRecords.computeIfAbsent(key, k -> new PoiRecord(blockPos, poiType, this.setDirty));
            this.add(newRecord);
         });
         this.isValid = true;
         this.setDirty.run();
      }
   }

   private void clear() {
      this.records.clear();
      this.byType.clear();
   }

   public boolean isValid() {
      return this.isValid;
   }

   public record Packed(boolean isValid, List<PoiRecord.Packed> records) {
      public static final Codec<PoiSection.Packed> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               Codec.BOOL.lenientOptionalFieldOf("Valid", false).forGetter(PoiSection.Packed::isValid),
               PoiRecord.Packed.CODEC.listOf().fieldOf("Records").forGetter(PoiSection.Packed::records)
            )
            .apply(i, PoiSection.Packed::new)
      );

      public PoiSection unpack(final Runnable setDirty) {
         return new PoiSection(setDirty, this.isValid, this.records.stream().map(record -> record.unpack(setDirty)).toList());
      }
   }
}
