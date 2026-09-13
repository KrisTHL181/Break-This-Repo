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

   public MobSensor(int p_333366_, BiPredicate<T, LivingEntity> p_329126_, Predicate<T> p_334546_, MemoryModuleType<Boolean> p_334716_, int p_331675_) {
      super(p_333366_);
      this.mobTest = p_329126_;
      this.readyTest = p_334546_;
      this.toSet = p_334716_;
      this.memoryTimeToLive = p_331675_;
   }

   @Override
   protected void doTick(ServerLevel p_332587_, T p_336316_) {
      if (!this.readyTest.test(p_336316_)) {
         this.clearMemory(p_336316_);
      } else {
         this.checkForMobsNearby(p_336316_);
      }
   }

   @Override
   public Set<MemoryModuleType<?>> requires() {
      return Set.of(MemoryModuleType.NEAREST_LIVING_ENTITIES);
   }

   public void checkForMobsNearby(T p_333520_) {
      Optional<List<LivingEntity>> optional = p_333520_.getBrain().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES);
      if (!optional.isEmpty()) {
         boolean flag = optional.get().stream().anyMatch(p_329312_ -> this.mobTest.test(p_333520_, p_329312_));
         if (flag) {
            this.mobDetected(p_333520_);
         }
      }
   }

   public void mobDetected(T p_332120_) {
      p_332120_.getBrain().setMemoryWithExpiry(this.toSet, true, this.memoryTimeToLive);
   }

   public void clearMemory(T p_336340_) {
      p_336340_.getBrain().eraseMemory(this.toSet);
   }
}
