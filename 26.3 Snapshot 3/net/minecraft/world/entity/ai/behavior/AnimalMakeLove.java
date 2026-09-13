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

   public AnimalMakeLove(final EntityType<? extends Animal> partnerType) {
      this(partnerType, 1.0F, 2);
   }

   public AnimalMakeLove(final EntityType<? extends Animal> partnerType, final float speedModifier, final int closeEnoughDistance) {
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
      this.partnerType = partnerType;
      this.speedModifier = speedModifier;
      this.closeEnoughDistance = closeEnoughDistance;
   }

   protected boolean checkExtraStartConditions(final ServerLevel level, final Animal body) {
      return body.isInLove() && this.findValidBreedPartner(body).isPresent();
   }

   protected void start(final ServerLevel level, final Animal body, final long timestamp) {
      Animal partner = this.findValidBreedPartner(body).get();
      body.getBrain().setMemory(MemoryModuleType.BREED_TARGET, partner);
      partner.getBrain().setMemory(MemoryModuleType.BREED_TARGET, body);
      BehaviorUtils.lockGazeAndWalkToEachOther(body, partner, this.speedModifier, this.closeEnoughDistance);
      int duration = 60 + body.getRandom().nextInt(50);
      this.spawnChildAtTime = timestamp + duration;
   }

   protected boolean canStillUse(final ServerLevel level, final Animal body, final long timestamp) {
      if (!this.hasBreedTargetOfRightType(body)) {
         return false;
      }

      Animal partner = this.getBreedTarget(body);
      return partner.isAlive()
         && body.canMate(partner)
         && BehaviorUtils.entityIsVisible(body.getBrain(), partner)
         && timestamp <= this.spawnChildAtTime
         && !body.isPanicking()
         && !partner.isPanicking();
   }

   protected void tick(final ServerLevel level, final Animal body, final long timestamp) {
      Animal partner = this.getBreedTarget(body);
      BehaviorUtils.lockGazeAndWalkToEachOther(body, partner, this.speedModifier, this.closeEnoughDistance);
      if (body.closerThan(partner, 3.0)) {
         if (timestamp >= this.spawnChildAtTime) {
            body.spawnChildFromBreeding(level, partner);
            body.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
            partner.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
         }
      }
   }

   protected void stop(final ServerLevel level, final Animal body, final long timestamp) {
      body.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
      body.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      body.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
      this.spawnChildAtTime = 0L;
   }

   private Animal getBreedTarget(final Animal body) {
      return (Animal)body.getBrain().getMemory(MemoryModuleType.BREED_TARGET).get();
   }

   private boolean hasBreedTargetOfRightType(final Animal body) {
      Brain<?> brain = body.getBrain();
      return brain.hasMemoryValue(MemoryModuleType.BREED_TARGET) && brain.getMemory(MemoryModuleType.BREED_TARGET).get().is(this.partnerType);
   }

   private Optional<? extends Animal> findValidBreedPartner(final Animal body) {
      return body.getBrain()
         .getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
         .get()
         .findClosest(entity -> entity.is(this.partnerType) && entity instanceof Animal animal && body.canMate(animal) && !animal.isPanicking())
         .map(Animal.class::cast);
   }
}
