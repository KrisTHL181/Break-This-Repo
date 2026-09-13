package net.minecraft.world.entity.ai.sensing;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class MobSensor<T extends LivingEntity> extends Sensor<T> {
   private final BiPredicate<T, LivingEntity> mobTest;
   private final Predicate<T> readyTest;
   private final MemoryModuleType<Boolean> toSet;
   private final int memoryTimeToLive;

   public MobSensor(
      final int scanRate,
      final BiPredicate<T, LivingEntity> mobTest,
      final Predicate<T> readyTest,
      final MemoryModuleType<Boolean> toSet,
      final int memoryTimeToLive
   ) {
      super(scanRate);
      this.mobTest = mobTest;
      this.readyTest = readyTest;
      this.toSet = toSet;
      this.memoryTimeToLive = memoryTimeToLive;
   }

   @Override
   protected void doTick(final ServerLevel level, final T body) {
      if (!this.readyTest.test(body)) {
         this.clearMemory(body);
      } else {
         this.checkForMobsNearby(body);
      }
   }

   @Override
   public Set<MemoryModuleType<?>> requires() {
      return Set.of(MemoryModuleType.NEAREST_LIVING_ENTITIES);
   }

   public void checkForMobsNearby(final T body) {
      Optional<List<LivingEntity>> livingEntitiesMemory = body.getBrain().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES);
      if (!livingEntitiesMemory.isEmpty()) {
         boolean mobPresent = livingEntitiesMemory.get().stream().anyMatch(entity -> this.mobTest.test(body, entity));
         if (mobPresent) {
            this.mobDetected(body);
         }
      }
   }

   public void mobDetected(final T body) {
      body.getBrain().setMemoryWithExpiry(this.toSet, true, this.memoryTimeToLive);
   }

   public void clearMemory(final T body) {
      body.getBrain().eraseMemory(this.toSet);
   }
}
