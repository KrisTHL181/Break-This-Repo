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

   public PoiSection(Runnable p_27267_) {
      this(p_27267_, true, ImmutableList.of());
   }

   PoiSection(Runnable p_27269_, boolean p_27270_, List<PoiRecord> p_27271_) {
      this.setDirty = p_27269_;
      this.isValid = p_27270_;
      p_27271_.forEach(this::add);
   }

   public PoiSection.Packed pack() {
      return new PoiSection.Packed(this.isValid, this.records.values().stream().map(PoiRecord::pack).toList());
   }

   public Stream<PoiRecord> getRecords(Predicate<Holder<PoiType>> p_27305_, PoiManager.Occupancy p_27306_) {
      return this.byType
         .entrySet()
         .stream()
         .filter(p_27309_ -> p_27305_.test(p_27309_.getKey()))
         .flatMap(p_27301_ -> p_27301_.getValue().stream())
         .filter(p_27306_.getTest());
   }

   public @Nullable PoiRecord add(BlockPos p_218022_, Holder<PoiType> p_218023_) {
      PoiRecord poirecord = new PoiRecord(p_218022_, p_218023_, this.setDirty);
      if (this.add(poirecord)) {
         LOGGER.debug("Added POI of type {} @ {}", p_218023_.getRegisteredName(), p_218022_);
         this.setDirty.run();
         return poirecord;
      } else {
         return null;
      }
   }

   private boolean add(PoiRecord p_27274_) {
      BlockPos blockpos = p_27274_.getPos();
      Holder<PoiType> holder = p_27274_.getPoiType();
      short short1 = SectionPos.sectionRelativePos(blockpos);
      PoiRecord poirecord = (PoiRecord)this.records.get(short1);
      if (poirecord != null) {
         if (holder.equals(poirecord.getPoiType())) {
            return false;
         }

         Util.logAndPauseIfInIde("POI data mismatch: already registered at " + blockpos);
      }

      this.records.put(short1, p_27274_);
      this.byType.computeIfAbsent(holder, p_218029_ -> Sets.newHashSet()).add(p_27274_);
      return true;
   }

   public void remove(BlockPos p_27280_) {
      PoiRecord poirecord = (PoiRecord)this.records.remove(SectionPos.sectionRelativePos(p_27280_));
      if (poirecord == null) {
         LOGGER.error("POI data mismatch: never registered at {}", p_27280_);
      } else {
         this.byType.get(poirecord.getPoiType()).remove(poirecord);
         LOGGER.debug("Removed POI of type {} @ {}", LogUtils.defer(poirecord::getPoiType), LogUtils.defer(poirecord::getPos));
         this.setDirty.run();
      }
   }

   @Deprecated
   @VisibleForDebug
   public int getFreeTickets(BlockPos p_148683_) {
      return this.getPoiRecord(p_148683_).map(PoiRecord::getFreeTickets).orElse(0);
   }

   public boolean release(BlockPos p_27318_) {
      PoiRecord poirecord = (PoiRecord)this.records.get(SectionPos.sectionRelativePos(p_27318_));
      if (poirecord == null) {
         throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("POI never registered at " + p_27318_));
      }

      boolean flag = poirecord.releaseTicket();
      this.setDirty.run();
      return flag;
   }

   public boolean exists(BlockPos p_27289_, Predicate<Holder<PoiType>> p_27290_) {
      return this.getType(p_27289_).filter(p_27290_).isPresent();
   }

   public Optional<Holder<PoiType>> getType(BlockPos p_27320_) {
      return this.getPoiRecord(p_27320_).map(PoiRecord::getPoiType);
   }

   private Optional<PoiRecord> getPoiRecord(BlockPos p_148685_) {
      return Optional.ofNullable((PoiRecord)this.records.get(SectionPos.sectionRelativePos(p_148685_)));
   }

   public Optional<DebugPoiInfo> getDebugPoiInfo(BlockPos p_424814_) {
      return this.getPoiRecord(p_424814_).map(DebugPoiInfo::new);
   }

   public void refresh(Consumer<BiConsumer<BlockPos, Holder<PoiType>>> p_27303_) {
      if (!this.isValid) {
         Short2ObjectMap<PoiRecord> short2objectmap = new Short2ObjectOpenHashMap(this.records);
         this.clear();
         p_27303_.accept((p_218032_, p_218033_) -> {
            short short1 = SectionPos.sectionRelativePos(p_218032_);
            PoiRecord poirecord = (PoiRecord)short2objectmap.computeIfAbsent(short1, p_218027_ -> new PoiRecord(p_218032_, p_218033_, this.setDirty));
            this.add(poirecord);
         });
         this.isValid = true;
         this.setDirty.run();
      }
   }

   private void clear() {
      this.records.clear();
      this.byType.clear();
   }

   boolean isValid() {
      return this.isValid;
   }

   public record Packed(boolean isValid, List<PoiRecord.Packed> records) {
      public static final Codec<PoiSection.Packed> CODEC = RecordCodecBuilder.create(
         p_365286_ -> p_365286_.group(
               Codec.BOOL.lenientOptionalFieldOf("Valid", false).forGetter(PoiSection.Packed::isValid),
               PoiRecord.Packed.CODEC.listOf().fieldOf("Records").forGetter(PoiSection.Packed::records)
            )
            .apply(p_365286_, PoiSection.Packed::new)
      );

      public PoiSection unpack(Runnable p_366213_) {
         return new PoiSection(p_366213_, this.isValid, this.records.stream().map(p_365161_ -> p_365161_.unpack(p_366213_)).toList());
      }
   }
}
