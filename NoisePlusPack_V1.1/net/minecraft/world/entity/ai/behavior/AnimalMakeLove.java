package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.Animal;

public class AnimalMakeLove extends Behavior<Animal> {
   private static final int BREED_RANGE = 3;
   private static final int MIN_DURATION = 60;
   private static final int MAX_DURATION = 110;
   private final EntityType<? extends Animal> partnerType;
   private final float speedModifier;
   private final int closeEnoughDistance;
   private static final int DEFAULT_CLOSE_ENOUGH_DISTANCE = 2;
   private long spawnChildAtTime;

   public AnimalMakeLove(EntityType<? extends Animal> p_22391_) {
      this(p_22391_, 1.0F, 2);
   }

   public AnimalMakeLove(EntityType<? extends Animal> p_329113_, float p_335591_, int p_332468_) {
      super(
         ImmutableMap.of(
            MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
            MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.BREED_TARGET,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.WALK_TARGET,
            MemoryStatus.REGISTERED,
            MemoryModuleType.LOOK_TARGET,
            MemoryStatus.REGISTERED,
            MemoryModuleType.IS_PANICKING,
            MemoryStatus.VALUE_ABSENT
         ),
         110
      );
      this.partnerType = p_329113_;
      this.speedModifier = p_335591_;
      this.closeEnoughDistance = p_332468_;
   }

   protected boolean checkExtraStartConditions(ServerLevel p_22401_, Animal p_22402_) {
      return p_22402_.isInLove() && this.findValidBreedPartner(p_22402_).isPresent();
   }

   protected void start(ServerLevel p_22404_, Animal p_22405_, long p_22406_) {
      Animal animal = this.findValidBreedPartner(p_22405_).get();
      p_22405_.getBrain().setMemory(MemoryModuleType.BREED_TARGET, animal);
      animal.getBrain().setMemory(MemoryModuleType.BREED_TARGET, p_22405_);
      BehaviorUtils.lockGazeAndWalkToEachOther(p_22405_, animal, this.speedModifier, this.closeEnoughDistance);
      int i = 60 + p_22405_.getRandom().nextInt(50);
      this.spawnChildAtTime = p_22406_ + i;
   }

   protected boolean canStillUse(ServerLevel p_22416_, Animal p_22417_, long p_22418_) {
      if (!this.hasBreedTargetOfRightType(p_22417_)) {
         return false;
      }

      Animal animal = this.getBreedTarget(p_22417_);
      return animal.isAlive()
         && p_22417_.canMate(animal)
         && BehaviorUtils.entityIsVisible(p_22417_.getBrain(), animal)
         && p_22418_ <= this.spawnChildAtTime
         && !p_22417_.isPanicking()
         && !animal.isPanicking();
   }

   protected void tick(ServerLevel p_22428_, Animal p_22429_, long p_22430_) {
      Animal animal = this.getBreedTarget(p_22429_);
      BehaviorUtils.lockGazeAndWalkToEachOther(p_22429_, animal, this.speedModifier, this.closeEnoughDistance);
      if (p_22429_.closerThan(animal, 3.0) && p_22430_ >= this.spawnChildAtTime) {
         p_22429_.spawnChildFromBreeding(p_22428_, animal);
         p_22429_.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
         animal.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
      }
   }

   protected void stop(ServerLevel p_22438_, Animal p_22439_, long p_22440_) {
      p_22439_.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
      p_22439_.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      p_22439_.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
      this.spawnChildAtTime = 0L;
   }

   private Animal getBreedTarget(Animal p_22410_) {
      return (Animal)p_22410_.getBrain().getMemory(MemoryModuleType.BREED_TARGET).get();
   }

   private boolean hasBreedTargetOfRightType(Animal p_22422_) {
      Brain<?> brain = p_22422_.getBrain();
      return brain.hasMemoryValue(MemoryModuleType.BREED_TARGET) && brain.getMemory(MemoryModuleType.BREED_TARGET).get().getType() == this.partnerType;
   }

   private Optional<? extends Animal> findValidBreedPartner(Animal p_22432_) {
      return p_22432_.getBrain()
         .getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
         .get()
         .findClosest(
            p_449455_ -> p_449455_.getType() == this.partnerType && p_449455_ instanceof Animal animal && p_22432_.canMate(animal) && !animal.isPanicking()
         )
         .map(Animal.class::cast);
   }
}
