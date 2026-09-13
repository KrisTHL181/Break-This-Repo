package net.minecraft.world.entity.monster.warden;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Streams;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

public class AngerManagement {
   @VisibleForTesting
   protected static final int CONVERSION_DELAY = 2;
   @VisibleForTesting
   protected static final int MAX_ANGER = 150;
   private static final int DEFAULT_ANGER_DECREASE = 1;
   private int conversionDelay = Mth.randomBetweenInclusive(RandomSource.create(), 0, 2);
   int highestAnger;
   private static final Codec<Pair<UUID, Integer>> SUSPECT_ANGER_PAIR = RecordCodecBuilder.create(
      p_253580_ -> p_253580_.group(
            UUIDUtil.CODEC.fieldOf("uuid").forGetter(Pair::getFirst), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("anger").forGetter(Pair::getSecond)
         )
         .apply(p_253580_, Pair::of)
   );
   private final Predicate<Entity> filter;
   @VisibleForTesting
   protected final ArrayList<Entity> suspects;
   private final AngerManagement.Sorter suspectSorter;
   @VisibleForTesting
   protected final Object2IntMap<Entity> angerBySuspect;
   @VisibleForTesting
   protected final Object2IntMap<UUID> angerByUuid;

   public static Codec<AngerManagement> codec(Predicate<Entity> p_219278_) {
      return RecordCodecBuilder.create(
         p_219281_ -> p_219281_.group(
               SUSPECT_ANGER_PAIR.listOf().fieldOf("suspects").orElse(Collections.emptyList()).forGetter(AngerManagement::createUuidAngerPairs)
            )
            .apply(p_219281_, p_219284_ -> new AngerManagement(p_219278_, p_219284_))
      );
   }

   public AngerManagement(Predicate<Entity> p_219254_, List<Pair<UUID, Integer>> p_219255_) {
      this.filter = p_219254_;
      this.suspects = new ArrayList<>();
      this.suspectSorter = new AngerManagement.Sorter(this);
      this.angerBySuspect = new Object2IntOpenHashMap();
      this.angerByUuid = new Object2IntOpenHashMap(p_219255_.size());
      p_219255_.forEach(p_219272_ -> this.angerByUuid.put((UUID)p_219272_.getFirst(), (Integer)p_219272_.getSecond()));
   }

   private List<Pair<UUID, Integer>> createUuidAngerPairs() {
      return Streams.concat(
            new Stream[]{
               this.suspects.stream().map(p_219295_ -> Pair.of(p_219295_.getUUID(), this.angerBySuspect.getInt(p_219295_))),
               this.angerByUuid.object2IntEntrySet().stream().map(p_219276_ -> Pair.of((UUID)p_219276_.getKey(), p_219276_.getIntValue()))
            }
         )
         .collect(Collectors.toList());
   }

   public void tick(ServerLevel p_219264_, Predicate<Entity> p_219265_) {
      this.conversionDelay--;
      if (this.conversionDelay <= 0) {
         this.convertFromUuids(p_219264_);
         this.conversionDelay = 2;
      }

      ObjectIterator<Entry<UUID>> objectiterator = this.angerByUuid.object2IntEntrySet().iterator();

      while (objectiterator.hasNext()) {
         Entry<UUID> entry = (Entry<UUID>)objectiterator.next();
         int i = entry.getIntValue();
         if (i <= 1) {
            objectiterator.remove();
         } else {
            entry.setValue(i - 1);
         }
      }

      ObjectIterator<Entry<Entity>> objectiterator1 = this.angerBySuspect.object2IntEntrySet().iterator();

      while (objectiterator1.hasNext()) {
         Entry<Entity> entry1 = (Entry<Entity>)objectiterator1.next();
         int j = entry1.getIntValue();
         Entity entity = (Entity)entry1.getKey();
         Entity.RemovalReason entity$removalreason = entity.getRemovalReason();
         if (j > 1 && p_219265_.test(entity) && entity$removalreason == null) {
            entry1.setValue(j - 1);
         } else {
            this.suspects.remove(entity);
            objectiterator1.remove();
            if (j > 1 && entity$removalreason != null) {
               switch (entity$removalreason) {
                  case CHANGED_DIMENSION:
                  case UNLOADED_TO_CHUNK:
                  case UNLOADED_WITH_PLAYER:
                     this.angerByUuid.put(entity.getUUID(), j - 1);
               }
            }
         }
      }

      this.sortAndUpdateHighestAnger();
   }

   private void sortAndUpdateHighestAnger() {
      this.highestAnger = 0;
      this.suspects.sort(this.suspectSorter);
      if (this.suspects.size() == 1) {
         this.highestAnger = this.angerBySuspect.getInt(this.suspects.get(0));
      }
   }

   private void convertFromUuids(ServerLevel p_219262_) {
      ObjectIterator<Entry<UUID>> objectiterator = this.angerByUuid.object2IntEntrySet().iterator();

      while (objectiterator.hasNext()) {
         Entry<UUID> entry = (Entry<UUID>)objectiterator.next();
         int i = entry.getIntValue();
         Entity entity = p_219262_.getEntity((UUID)entry.getKey());
         if (entity != null) {
            this.angerBySuspect.put(entity, i);
            this.suspects.add(entity);
            objectiterator.remove();
         }
      }
   }

   public int increaseAnger(Entity p_219269_, int p_219270_) {
      boolean flag = !this.angerBySuspect.containsKey(p_219269_);
      int i = this.angerBySuspect.computeInt(p_219269_, (p_219259_, p_219260_) -> Math.min(150, (p_219260_ == null ? 0 : p_219260_) + p_219270_));
      if (flag) {
         int j = this.angerByUuid.removeInt(p_219269_.getUUID());
         i += j;
         this.angerBySuspect.put(p_219269_, i);
         this.suspects.add(p_219269_);
      }

      this.sortAndUpdateHighestAnger();
      return i;
   }

   public void clearAnger(Entity p_219267_) {
      this.angerBySuspect.removeInt(p_219267_);
      this.suspects.remove(p_219267_);
      this.sortAndUpdateHighestAnger();
   }

   private @Nullable Entity getTopSuspect() {
      return this.suspects.stream().filter(this.filter).findFirst().orElse(null);
   }

   public int getActiveAnger(@Nullable Entity p_219287_) {
      return p_219287_ == null ? this.highestAnger : this.angerBySuspect.getInt(p_219287_);
   }

   public Optional<LivingEntity> getActiveEntity() {
      return Optional.ofNullable(this.getTopSuspect()).filter(p_219293_ -> p_219293_ instanceof LivingEntity).map(p_219290_ -> (LivingEntity)p_219290_);
   }

   @VisibleForTesting
   protected record Sorter(AngerManagement angerManagement) implements Comparator<Entity> {
      public int compare(Entity p_219303_, Entity p_219304_) {
         if (p_219303_.equals(p_219304_)) {
            return 0;
         } else {
            int i = this.angerManagement.angerBySuspect.getOrDefault(p_219303_, 0);
            int j = this.angerManagement.angerBySuspect.getOrDefault(p_219304_, 0);
            this.angerManagement.highestAnger = Math.max(this.angerManagement.highestAnger, Math.max(i, j));
            boolean flag = AngerLevel.byAnger(i).isAngry();
            boolean flag1 = AngerLevel.byAnger(j).isAngry();
            if (flag != flag1) {
               return flag ? -1 : 1;
            } else {
               boolean flag2 = p_219303_ instanceof Player;
               boolean flag3 = p_219304_ instanceof Player;
               if (flag2 != flag3) {
                  return flag2 ? -1 : 1;
               } else {
                  return Integer.compare(j, i);
               }
            }
         }
      }
   }
}
