package net.minecraft.world.entity.ai;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryMap;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemorySlot;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Brain<E extends LivingEntity> {
   private static final int SCHEDULE_UPDATE_DELAY = 20;
   private final Map<MemoryModuleType<?>, MemorySlot<?>> memories = Maps.newHashMap();
   private final Map<SensorType<? extends Sensor<? super E>>, Sensor<? super E>> sensors = Maps.newLinkedHashMap();
   private final Map<Integer, Map<Activity, Set<BehaviorControl<? super E>>>> availableBehaviorsByPriority = Maps.newTreeMap();
   private @Nullable EnvironmentAttribute<Activity> schedule;
   private final Map<Activity, Set<Pair<MemoryModuleType<?>, MemoryStatus>>> activityRequirements = Maps.newHashMap();
   private final Map<Activity, Set<MemoryModuleType<?>>> activityMemoriesToEraseWhenStopped = Maps.newHashMap();
   private Set<Activity> coreActivities = Sets.newHashSet();
   private final Set<Activity> activeActivities = Sets.newHashSet();
   private Activity defaultActivity = Activity.IDLE;
   private long lastScheduleUpdate = -9999L;

   public static <E extends LivingEntity> Brain.Provider<E> provider(final Collection<? extends SensorType<? extends Sensor<? super E>>> sensorTypes) {
      return new Brain.Provider<>(ImmutableList.of(), sensorTypes, var0 -> List.of());
   }

   public static <E extends LivingEntity> Brain.Provider<E> provider(
      final Collection<? extends SensorType<? extends Sensor<? super E>>> sensorTypes, final Brain.ActivitySupplier<E> activities
   ) {
      return new Brain.Provider<>(ImmutableList.of(), sensorTypes, activities);
   }

   @Deprecated
   public static <E extends LivingEntity> Brain.Provider<E> provider(
      final Collection<? extends MemoryModuleType<?>> memoryTypes,
      final Collection<? extends SensorType<? extends Sensor<? super E>>> sensorTypes,
      final Brain.ActivitySupplier<E> activities
   ) {
      return new Brain.Provider<>(memoryTypes, sensorTypes, activities);
   }

   @VisibleForTesting
   protected Brain(
      final Collection<? extends MemoryModuleType<?>> memoryTypes,
      final Collection<? extends SensorType<? extends Sensor<? super E>>> sensorTypes,
      final List<ActivityData<E>> activities,
      final MemoryMap memories,
      final RandomSource randomSource
   ) {
      for (MemoryModuleType<?> memoryType : memoryTypes) {
         this.registerMemory(memoryType);
      }

      for (SensorType<? extends Sensor<? super E>> sensorType : sensorTypes) {
         Sensor<? super E> newSensor = (Sensor<? super E>)sensorType.create();
         newSensor.randomlyDelayStart(randomSource);
         this.sensors.put(sensorType, newSensor);

         for (MemoryModuleType<?> type : newSensor.requires()) {
            this.registerMemory(type);
         }
      }

      for (ActivityData<E> activity : activities) {
         this.addActivity(activity.activityType(), activity.behaviorPriorityPairs(), activity.conditions(), activity.memoriesToEraseWhenStopped());
      }

      for (MemoryMap.Value<?> memory : memories) {
         this.setMemoryInternal(memory);
      }

      this.setCoreActivities(ImmutableSet.of(Activity.CORE));
      this.useDefaultActivity();
   }

   private void registerMemory(final MemoryModuleType<?> memoryType) {
      this.memories.putIfAbsent(memoryType, MemorySlot.create());
   }

   public Brain() {
      this.setCoreActivities(ImmutableSet.of(Activity.CORE));
      this.useDefaultActivity();
   }

   public Brain.Packed pack() {
      final MemoryMap.Builder builder = new MemoryMap.Builder();
      this.forEach(new Brain.Visitor() {
         @Override
         public <U> void acceptEmpty(final MemoryModuleType<U> type) {
         }

         @Override
         public <U> void accept(final MemoryModuleType<U> type, final U value, final long timeToLive) {
            if (type.canSerialize()) {
               builder.add(type, ExpirableValue.of(value, timeToLive));
            }
         }

         @Override
         public <U> void accept(final MemoryModuleType<U> type, final U value) {
            if (type.canSerialize()) {
               builder.add(type, ExpirableValue.of(value));
            }
         }
      });
      return new Brain.Packed(builder.build());
   }

   private <T> @Nullable MemorySlot<T> getMemorySlotIfPresent(final MemoryModuleType<T> memoryType) {
      return (MemorySlot<T>)this.memories.get(memoryType);
   }

   private <T> MemorySlot<T> getMemorySlot(final MemoryModuleType<T> memoryType) {
      MemorySlot<T> result = this.getMemorySlotIfPresent(memoryType);
      if (result == null) {
         throw new IllegalStateException("Unregistered memory fetched: " + memoryType);
      } else {
         return result;
      }
   }

   public boolean hasMemoryValue(final MemoryModuleType<?> type) {
      return this.checkMemory(type, MemoryStatus.VALUE_PRESENT);
   }

   public void clearMemories() {
      this.memories.values().forEach(MemorySlot::clear);
   }

   public <U> void eraseMemory(final MemoryModuleType<U> type) {
      MemorySlot<U> slot = this.getMemorySlotIfPresent(type);
      if (slot != null) {
         slot.clear();
      }
   }

   public <U> void setMemory(final MemoryModuleType<U> type, final @Nullable U value) {
      this.setMemoryInternal(type, value);
   }

   public <U> void setMemoryWithExpiry(final MemoryModuleType<U> type, final U value, final long timeToLive) {
      this.setMemoryInternal(type, value, timeToLive);
   }

   public <U> void setMemory(final MemoryModuleType<U> type, final Optional<? extends U> optionalValue) {
      this.setMemoryInternal(type, (U)optionalValue.orElse(null));
   }

   private <U> void setMemoryInternal(final MemoryMap.Value<U> value) {
      ExpirableValue<U> expirableValue = value.value();
      if (expirableValue.timeToLive().isPresent()) {
         this.setMemoryInternal(value.type(), expirableValue.value(), expirableValue.timeToLive().get());
      } else {
         this.setMemoryInternal(value.type(), expirableValue.value());
      }
   }

   private <U> void setMemoryInternal(final MemoryModuleType<U> type, U value, final long tileToLive) {
      MemorySlot<U> slot = this.getMemorySlotIfPresent(type);
      if (slot != null) {
         if (isEmptyCollection(value)) {
            value = null;
         }

         if (value == null) {
            slot.clear();
         } else {
            slot.set(value, tileToLive);
         }
      }
   }

   private <U> void setMemoryInternal(final MemoryModuleType<U> type, @Nullable U value) {
      MemorySlot<U> slot = this.getMemorySlotIfPresent(type);
      if (slot != null) {
         if (value != null && isEmptyCollection(value)) {
            value = null;
         }

         if (value == null) {
            slot.clear();
         } else {
            slot.set(value);
         }
      }
   }

   public <U> Optional<U> getMemory(final MemoryModuleType<U> type) {
      return Optional.ofNullable(this.getMemorySlot(type).value());
   }

   public <U> @Nullable Optional<U> getMemoryInternal(final MemoryModuleType<U> type) {
      MemorySlot<U> slot = this.getMemorySlotIfPresent(type);
      return slot == null ? null : Optional.ofNullable(slot.value());
   }

   public <U> long getTimeUntilExpiry(final MemoryModuleType<U> type) {
      return this.getMemorySlot(type).timeToLive();
   }

   public void forEach(final Brain.Visitor visitor) {
      this.memories.forEach((memoryModuleType, slot) -> callVisitor(visitor, (MemoryModuleType<?>)memoryModuleType, (MemorySlot<?>)slot));
   }

   private static <U> void callVisitor(final Brain.Visitor visitor, final MemoryModuleType<U> memoryModuleType, final MemorySlot<?> slot) {
      slot.visit(memoryModuleType, visitor);
   }

   public <U> boolean isMemoryValue(final MemoryModuleType<U> memoryType, final U value) {
      MemorySlot<U> slot = this.getMemorySlotIfPresent(memoryType);
      return slot != null && Objects.equals(value, slot.value());
   }

   public boolean checkMemory(final MemoryModuleType<?> type, final MemoryStatus status) {
      MemorySlot<?> slot = this.getMemorySlotIfPresent(type);
      return slot == null
         ? false
         : status == MemoryStatus.REGISTERED
            || status == MemoryStatus.VALUE_PRESENT && slot.hasValue()
            || status == MemoryStatus.VALUE_ABSENT && !slot.hasValue();
   }

   public void setSchedule(final EnvironmentAttribute<Activity> schedule) {
      this.schedule = schedule;
   }

   public void setCoreActivities(final Set<Activity> activities) {
      this.coreActivities = activities;
   }

   @Deprecated
   @VisibleForDebug
   public Set<Activity> getActiveActivities() {
      return this.activeActivities;
   }

   @Deprecated
   @VisibleForDebug
   public List<BehaviorControl<? super E>> getRunningBehaviors() {
      List<BehaviorControl<? super E>> runningBehaviours = new ObjectArrayList();

      for (Map<Activity, Set<BehaviorControl<? super E>>> behavioursByActivities : this.availableBehaviorsByPriority.values()) {
         for (Set<BehaviorControl<? super E>> behaviors : behavioursByActivities.values()) {
            for (BehaviorControl<? super E> behavior : behaviors) {
               if (behavior.getStatus() == Behavior.Status.RUNNING) {
                  runningBehaviours.add(behavior);
               }
            }
         }
      }

      return runningBehaviours;
   }

   public void useDefaultActivity() {
      this.setActiveActivity(this.defaultActivity);
   }

   public Optional<Activity> getActiveNonCoreActivity() {
      for (Activity activity : this.activeActivities) {
         if (!this.coreActivities.contains(activity)) {
            return Optional.of(activity);
         }
      }

      return Optional.empty();
   }

   public void setActiveActivityIfPossible(final Activity activity) {
      if (this.activityRequirementsAreMet(activity)) {
         this.setActiveActivity(activity);
      } else {
         this.useDefaultActivity();
      }
   }

   private void setActiveActivity(final Activity activity) {
      if (!this.isActive(activity)) {
         this.eraseMemoriesForOtherActivitesThan(activity);
         this.activeActivities.clear();
         this.activeActivities.addAll(this.coreActivities);
         this.activeActivities.add(activity);
      }
   }

   private void eraseMemoriesForOtherActivitesThan(final Activity activity) {
      for (Activity oldActivity : this.activeActivities) {
         if (oldActivity != activity) {
            Set<MemoryModuleType<?>> memoryModuleTypes = this.activityMemoriesToEraseWhenStopped.get(oldActivity);
            if (memoryModuleTypes != null) {
               for (MemoryModuleType<?> memoryModuleType : memoryModuleTypes) {
                  this.eraseMemory(memoryModuleType);
               }
            }
         }
      }
   }

   public void updateActivityFromSchedule(final EnvironmentAttributeSystem environmentAttributes, final long gameTime, final Vec3 pos) {
      if (gameTime - this.lastScheduleUpdate > 20L) {
         this.lastScheduleUpdate = gameTime;
         Activity scheduledActivity = this.schedule != null ? environmentAttributes.getValue(this.schedule, pos) : Activity.IDLE;
         if (!this.activeActivities.contains(scheduledActivity)) {
            this.setActiveActivityIfPossible(scheduledActivity);
         }
      }
   }

   public void setActiveActivityToFirstValid(final List<Activity> activities) {
      for (Activity activity : activities) {
         if (this.activityRequirementsAreMet(activity)) {
            this.setActiveActivity(activity);
            break;
         }
      }
   }

   public void setDefaultActivity(final Activity activity) {
      this.defaultActivity = activity;
   }

   public void addActivity(
      final Activity activity,
      final ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super E>>> behaviorPriorityPairs,
      final Set<Pair<MemoryModuleType<?>, MemoryStatus>> conditions,
      final Set<MemoryModuleType<?>> memoriesToEraseWhenStopped
   ) {
      this.activityRequirements.put(activity, conditions);
      if (!memoriesToEraseWhenStopped.isEmpty()) {
         this.activityMemoriesToEraseWhenStopped.put(activity, memoriesToEraseWhenStopped);
      }

      UnmodifiableIterator var5 = behaviorPriorityPairs.iterator();

      while (var5.hasNext()) {
         Pair<Integer, ? extends BehaviorControl<? super E>> pair = (Pair<Integer, ? extends BehaviorControl<? super E>>)var5.next();
         BehaviorControl<? super E> behavior = (BehaviorControl<? super E>)pair.getSecond();

         for (MemoryModuleType<?> requiredMemory : behavior.getRequiredMemories()) {
            this.registerMemory(requiredMemory);
         }

         this.availableBehaviorsByPriority
            .computeIfAbsent((Integer)pair.getFirst(), key -> Maps.newHashMap())
            .computeIfAbsent(activity, key -> Sets.newLinkedHashSet())
            .add(behavior);
      }
   }

   @VisibleForTesting
   public void removeAllBehaviors() {
      this.availableBehaviorsByPriority.clear();
   }

   public boolean isActive(final Activity activity) {
      return this.activeActivities.contains(activity);
   }

   public void tick(final ServerLevel level, final E body) {
      this.forgetOutdatedMemories();
      this.tickSensors(level, body);
      this.startEachNonRunningBehavior(level, body);
      this.tickEachRunningBehavior(level, body);
   }

   private void tickSensors(final ServerLevel level, final E body) {
      for (Sensor<? super E> sensor : this.sensors.values()) {
         sensor.tick(level, body);
      }
   }

   private void forgetOutdatedMemories() {
      this.memories.values().forEach(MemorySlot::tick);
   }

   public void stopAll(final ServerLevel level, final E body) {
      long timestamp = body.level().getGameTime();

      for (BehaviorControl<? super E> behavior : this.getRunningBehaviors()) {
         behavior.doStop(level, body, timestamp);
      }
   }

   private void startEachNonRunningBehavior(final ServerLevel level, final E body) {
      long time = level.getGameTime();

      for (Map<Activity, Set<BehaviorControl<? super E>>> behavioursByActivities : this.availableBehaviorsByPriority.values()) {
         for (Entry<Activity, Set<BehaviorControl<? super E>>> behavioursForActivity : behavioursByActivities.entrySet()) {
            Activity activity = behavioursForActivity.getKey();
            if (this.activeActivities.contains(activity)) {
               for (BehaviorControl<? super E> behavior : behavioursForActivity.getValue()) {
                  if (behavior.getStatus() == Behavior.Status.STOPPED) {
                     behavior.tryStart(level, body, time);
                  }
               }
            }
         }
      }
   }

   private void tickEachRunningBehavior(final ServerLevel level, final E body) {
      long timestamp = level.getGameTime();

      for (BehaviorControl<? super E> behavior : this.getRunningBehaviors()) {
         behavior.tickOrStop(level, body, timestamp);
      }
   }

   private boolean activityRequirementsAreMet(final Activity activity) {
      if (!this.activityRequirements.containsKey(activity)) {
         return false;
      }

      for (Pair<MemoryModuleType<?>, MemoryStatus> memoryRequirement : this.activityRequirements.get(activity)) {
         MemoryModuleType<?> memoryType = (MemoryModuleType<?>)memoryRequirement.getFirst();
         MemoryStatus memoryStatus = (MemoryStatus)memoryRequirement.getSecond();
         if (!this.checkMemory(memoryType, memoryStatus)) {
            return false;
         }
      }

      return true;
   }

   private static boolean isEmptyCollection(final Object object) {
      return object instanceof Collection<?> collection && collection.isEmpty();
   }

   public boolean isBrainDead() {
      return this.memories.isEmpty() && this.sensors.isEmpty() && this.availableBehaviorsByPriority.isEmpty();
   }

   @FunctionalInterface
   public interface ActivitySupplier<E extends LivingEntity> {
      List<ActivityData<E>> createActivities(E body);
   }

   public record Packed(MemoryMap memories) {
      public static final Brain.Packed EMPTY = new Brain.Packed(MemoryMap.EMPTY);
      public static final Codec<Brain.Packed> CODEC = RecordCodecBuilder.create(
         i -> i.group(MemoryMap.CODEC.fieldOf("memories").forGetter(Brain.Packed::memories)).apply(i, Brain.Packed::new)
      );
   }

   public static final class Provider<E extends LivingEntity> {
      private final Collection<? extends MemoryModuleType<?>> memoryTypes;
      private final Collection<? extends SensorType<? extends Sensor<? super E>>> sensorTypes;
      private final Brain.ActivitySupplier<E> activities;

      private Provider(
         final Collection<? extends MemoryModuleType<?>> memoryTypes,
         final Collection<? extends SensorType<? extends Sensor<? super E>>> sensorTypes,
         final Brain.ActivitySupplier<E> activities
      ) {
         this.memoryTypes = memoryTypes;
         this.sensorTypes = sensorTypes;
         this.activities = activities;
      }

      public Brain<E> makeBrain(final E body, final Brain.Packed packed) {
         List<ActivityData<E>> activities = this.activities.createActivities(body);
         return new Brain<>(this.memoryTypes, this.sensorTypes, activities, packed.memories, body.getRandom());
      }
   }

   public interface Visitor {
      <U> void acceptEmpty(MemoryModuleType<U> type);

      <U> void accept(MemoryModuleType<U> type, U value);

      <U> void accept(MemoryModuleType<U> type, U value, long timeToLive);
   }
}
