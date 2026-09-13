package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.pathfinder.Path;

public class VillagerMakeLove extends Behavior<Villager> {
   private long birthTimestamp;

   public VillagerMakeLove() {
      super(
         ImmutableMap.of(
            MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT
         ),
         350,
         350
      );
   }

   protected boolean checkExtraStartConditions(ServerLevel p_24623_, Villager p_453703_) {
      return this.isBreedingPossible(p_453703_);
   }

   protected boolean canStillUse(ServerLevel p_24626_, Villager p_455512_, long p_24628_) {
      return p_24628_ <= this.birthTimestamp && this.isBreedingPossible(p_455512_);
   }

   protected void start(ServerLevel p_24652_, Villager p_460422_, long p_24654_) {
      AgeableMob ageablemob = p_460422_.getBrain().getMemory(MemoryModuleType.BREED_TARGET).get();
      BehaviorUtils.lockGazeAndWalkToEachOther(p_460422_, ageablemob, 0.5F, 2);
      p_24652_.broadcastEntityEvent(ageablemob, (byte)18);
      p_24652_.broadcastEntityEvent(p_460422_, (byte)18);
      int i = 275 + p_460422_.getRandom().nextInt(50);
      this.birthTimestamp = p_24654_ + i;
   }

   protected void tick(ServerLevel p_24667_, Villager p_460907_, long p_24669_) {
      Villager villager = (Villager)p_460907_.getBrain().getMemory(MemoryModuleType.BREED_TARGET).get();
      if (!(p_460907_.distanceToSqr(villager) > 5.0)) {
         BehaviorUtils.lockGazeAndWalkToEachOther(p_460907_, villager, 0.5F, 2);
         if (p_24669_ >= this.birthTimestamp) {
            p_460907_.eatAndDigestFood();
            villager.eatAndDigestFood();
            this.tryToGiveBirth(p_24667_, p_460907_, villager);
         } else if (p_460907_.getRandom().nextInt(35) == 0) {
            p_24667_.broadcastEntityEvent(villager, (byte)12);
            p_24667_.broadcastEntityEvent(p_460907_, (byte)12);
         }
      }
   }

   private void tryToGiveBirth(ServerLevel p_24630_, Villager p_456511_, Villager p_454345_) {
      Optional<BlockPos> optional = this.takeVacantBed(p_24630_, p_456511_);
      if (optional.isEmpty()) {
         p_24630_.broadcastEntityEvent(p_454345_, (byte)13);
         p_24630_.broadcastEntityEvent(p_456511_, (byte)13);
      } else {
         Optional<Villager> optional1 = this.breed(p_24630_, p_456511_, p_454345_);
         if (optional1.isPresent()) {
            this.giveBedToChild(p_24630_, optional1.get(), optional.get());
         } else {
            p_24630_.getPoiManager().release(optional.get());
            p_24630_.debugSynchronizers().updatePoi(optional.get());
         }
      }
   }

   protected void stop(ServerLevel p_24675_, Villager p_458018_, long p_24677_) {
      p_458018_.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
   }

   private boolean isBreedingPossible(Villager p_460158_) {
      Brain<Villager> brain = p_460158_.getBrain();
      Optional<AgeableMob> optional = brain.getMemory(MemoryModuleType.BREED_TARGET).filter(p_449568_ -> p_449568_.getType() == EntityType.VILLAGER);
      return optional.isEmpty()
         ? false
         : BehaviorUtils.targetIsValid(brain, MemoryModuleType.BREED_TARGET, EntityType.VILLAGER) && p_460158_.canBreed() && optional.get().canBreed();
   }

   private Optional<BlockPos> takeVacantBed(ServerLevel p_24649_, Villager p_456388_) {
      return p_24649_.getPoiManager()
         .take(
            p_217509_ -> p_217509_.is(PoiTypes.HOME), (p_449570_, p_449571_) -> this.canReach(p_456388_, p_449571_, p_449570_), p_456388_.blockPosition(), 48
         );
   }

   private boolean canReach(Villager p_461015_, BlockPos p_217502_, Holder<PoiType> p_217503_) {
      Path path = p_461015_.getNavigation().createPath(p_217502_, p_217503_.value().validRange());
      return path != null && path.canReach();
   }

   private Optional<Villager> breed(ServerLevel p_24656_, Villager p_452828_, Villager p_451807_) {
      Villager villager = p_452828_.getBreedOffspring(p_24656_, p_451807_);
      if (villager == null) {
         return Optional.empty();
      }

      p_452828_.setAge(6000);
      p_451807_.setAge(6000);
      villager.setAge(-24000);
      villager.snapTo(p_452828_.getX(), p_452828_.getY(), p_452828_.getZ(), 0.0F, 0.0F);
      p_24656_.addFreshEntityWithPassengers(villager);
      p_24656_.broadcastEntityEvent(villager, (byte)12);
      return Optional.of(villager);
   }

   private void giveBedToChild(ServerLevel p_24634_, Villager p_455612_, BlockPos p_24636_) {
      GlobalPos globalpos = GlobalPos.of(p_24634_.dimension(), p_24636_);
      p_455612_.getBrain().setMemory(MemoryModuleType.HOME, globalpos);
   }
}
