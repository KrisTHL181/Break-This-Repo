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
   private int conversionDelay = Mth.randomBetweenInclusive(RandomSource.createThreadLocalInstance(), 0, 2);
   private int highestAnger;
   private static final Codec<Pair<UUID, Integer>> SUSPECT_ANGER_PAIR = RecordCodecBuilder.create(
      i -> i.group(UUIDUtil.CODEC.fieldOf("uuid").forGetter(Pair::getFirst), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("anger").forGetter(Pair::getSecond))
         .apply(i, Pair::of)
   );
   private final Predicate<Entity> filter;
   @VisibleForTesting
   protected final ArrayList<Entity> suspects;
   private final AngerManagement.Sorter suspectSorter;
   @VisibleForTesting
   protected final Object2IntMap<Entity> angerBySuspect;
   @VisibleForTesting
   protected final Object2IntMap<UUID> angerByUuid;

   public static Codec<AngerManagement> codec(final Predicate<Entity> filter) {
      return RecordCodecBuilder.create(
         i -> i.group(SUSPECT_ANGER_PAIR.listOf().lenientOptionalFieldOf("suspects", List.of()).forGetter(AngerManagement::createUuidAngerPairs))
            .apply(i, list -> new AngerManagement(filter, list))
      );
   }

   public AngerManagement(final Predicate<Entity> filter, final List<Pair<UUID, Integer>> angerByUuid) {
      this.filter = filter;
      this.suspects = new ArrayList<>();
      this.suspectSorter = new AngerManagement.Sorter(this);
      this.angerBySuspect = new Object2IntOpenHashMap();
      this.angerByUuid = new Object2IntOpenHashMap(angerByUuid.size());
      angerByUuid.forEach(pair -> this.angerByUuid.put((UUID)pair.getFirst(), (Integer)pair.getSecond()));
   }

   private List<Pair<UUID, Integer>> createUuidAngerPairs() {
      return Streams.concat(
            new Stream[]{
               this.suspects.stream().map(e -> Pair.of(e.getUUID(), this.angerBySuspect.getInt(e))),
               this.angerByUuid.object2IntEntrySet().stream().map(e -> Pair.of((UUID)e.getKey(), e.getIntValue()))
            }
         )
         .collect(Collectors.toList());
   }

   public void tick(final ServerLevel level, final Predicate<Entity> validEntity) {
      this.conversionDelay--;
      if (this.conversionDelay <= 0) {
         this.convertFromUuids(level);
         this.conversionDelay = 2;
      }

      ObjectIterator<Entry<UUID>> serializedIterator = this.angerByUuid.object2IntEntrySet().iterator();

      while (serializedIterator.hasNext()) {
         Entry<UUID> entry = (Entry<UUID>)serializedIterator.next();
         int anger = entry.getIntValue();
         if (anger <= 1) {
            serializedIterator.remove();
         } else {
            entry.setValue(anger - 1);
         }
      }

      ObjectIterator<Entry<Entity>> iterator = this.angerBySuspect.object2IntEntrySet().iterator();

      while (iterator.hasNext()) {
         Entry<Entity> entry = (Entry<Entity>)iterator.next();
         int anger = entry.getIntValue();
         Entity entity = (Entity)entry.getKey();
         Entity.RemovalReason removalReason = entity.getRemovalReason();
         if (anger > 1 && validEntity.test(entity) && removalReason == null) {
            entry.setValue(anger - 1);
         } else {
            this.suspects.remove(entity);
            iterator.remove();
            if (anger > 1 && removalReason != null) {
               switch (removalReason) {
                  case CHANGED_DIMENSION:
                  case UNLOADED_TO_CHUNK:
                  case UNLOADED_WITH_PLAYER:
                     this.angerByUuid.put(entity.getUUID(), anger - 1);
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

   private void convertFromUuids(final ServerLevel level) {
      ObjectIterator<Entry<UUID>> iterator = this.angerByUuid.object2IntEntrySet().iterator();

      while (iterator.hasNext()) {
         Entry<UUID> entry = (Entry<UUID>)iterator.next();
         int anger = entry.getIntValue();
         Entity entity = level.getEntity((UUID)entry.getKey());
         if (entity != null) {
            this.angerBySuspect.put(entity, anger);
            this.suspects.add(entity);
            iterator.remove();
         }
      }
   }

   public int increaseAnger(final Entity entity, final int increment) {
      boolean newSuspect = !this.angerBySuspect.containsKey(entity);
      int currentAnger = this.angerBySuspect.computeInt(entity, (k, anger) -> Math.min(150, (anger == null ? 0 : anger) + increment));
      if (newSuspect) {
         int serializedAnger = this.angerByUuid.removeInt(entity.getUUID());
         currentAnger += serializedAnger;
         this.angerBySuspect.put(entity, currentAnger);
         this.suspects.add(entity);
      }

      this.sortAndUpdateHighestAnger();
      return currentAnger;
   }

   public void clearAnger(final Entity entity) {
      this.angerBySuspect.removeInt(entity);
      this.suspects.remove(entity);
      this.sortAndUpdateHighestAnger();
   }

   private @Nullable Entity getTopSuspect() {
      return this.suspects.stream().filter(this.filter).findFirst().orElse(null);
   }

   public int getActiveAnger(final @Nullable Entity currentTarget) {
      return currentTarget == null ? this.highestAnger : this.angerBySuspect.getInt(currentTarget);
   }

   public Optional<LivingEntity> getActiveEntity() {
      return Optional.ofNullable(this.getTopSuspect()).filter(e -> e instanceof LivingEntity).map(e -> (LivingEntity)e);
   }

   @VisibleForTesting
   protected record Sorter(AngerManagement angerManagement) implements Comparator<Entity> {
      public int compare(final Entity entity1, final Entity entity2) {
         if (entity1.equals(entity2)) {
            return 0;
         } else {
            int anger1 = this.angerManagement.angerBySuspect.getOrDefault(entity1, 0);
            int anger2 = this.angerManagement.angerBySuspect.getOrDefault(entity2, 0);
            this.angerManagement.highestAnger = Math.max(this.angerManagement.highestAnger, Math.max(anger1, anger2));
            boolean angryAt1 = AngerLevel.byAnger(anger1).isAngry();
            boolean angryAt2 = AngerLevel.byAnger(anger2).isAngry();
            if (angryAt1 != angryAt2) {
               return angryAt1 ? -1 : 1;
            } else {
               boolean isPlayer1 = entity1 instanceof Player;
               boolean isPlayer2 = entity2 instanceof Player;
               if (isPlayer1 != isPlayer2) {
                  return isPlayer1 ? -1 : 1;
               } else {
                  return Integer.compare(anger2, anger1);
               }
            }
         }
      }
   }
}
