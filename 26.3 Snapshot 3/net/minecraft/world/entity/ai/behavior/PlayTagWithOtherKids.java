package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.Maps;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

public class PlayTagWithOtherKids {
   private static final int MAX_FLEE_XZ_DIST = 20;
   private static final int MAX_FLEE_Y_DIST = 8;
   private static final float FLEE_SPEED_MODIFIER = 0.6F;
   private static final float CHASE_SPEED_MODIFIER = 0.6F;
   private static final int MAX_CHASERS_PER_TARGET = 5;
   private static final int AVERAGE_WAIT_TIME_BETWEEN_RUNS = 10;

   public static BehaviorControl<PathfinderMob> create() {
      return BehaviorBuilder.create(
         i -> i.group(
               i.present(MemoryModuleType.VISIBLE_VILLAGER_BABIES),
               i.absent(MemoryModuleType.WALK_TARGET),
               i.registered(MemoryModuleType.LOOK_TARGET),
               i.registered(MemoryModuleType.INTERACTION_TARGET)
            )
            .apply(i, (babies, walkTarget, lookTarget, interactionTarget) -> (level, me, timestamp) -> {
               if (level.getRandom().nextInt(10) != 0) {
                  return false;
               }

               List<LivingEntity> friendsNearby = i.get(babies);
               Optional<LivingEntity> otherKidChasingMe = friendsNearby.stream().filter(friend -> isFriendChasingMe(me, friend)).findAny();
               if (!otherKidChasingMe.isPresent()) {
                  Optional<LivingEntity> otherKidBeingChased = findSomeoneBeingChased(friendsNearby);
                  if (otherKidBeingChased.isPresent()) {
                     chaseKid(interactionTarget, lookTarget, walkTarget, otherKidBeingChased.get());
                     return true;
                  } else {
                     friendsNearby.stream().findAny().ifPresent(entity -> chaseKid(interactionTarget, lookTarget, walkTarget, entity));
                     return true;
                  }
               } else {
                  for (int j = 0; j < 10; j++) {
                     Vec3 pos = LandRandomPos.getPos(me, 20, 8);
                     if (pos != null && level.isVillage(BlockPos.containing(pos))) {
                        walkTarget.set(new WalkTarget(pos, 0.6F, 0));
                        break;
                     }
                  }

                  return true;
               }
            })
      );
   }

   private static void chaseKid(
      final MemoryAccessor<?, LivingEntity> interactionTarget,
      final MemoryAccessor<?, PositionTracker> lookTarget,
      final MemoryAccessor<?, WalkTarget> walkTarget,
      final LivingEntity kidToChase
   ) {
      interactionTarget.set(kidToChase);
      lookTarget.set(new EntityTracker(kidToChase, true));
      walkTarget.set(new WalkTarget(new EntityTracker(kidToChase, false), 0.6F, 1));
   }

   private static Optional<LivingEntity> findSomeoneBeingChased(final List<LivingEntity> friendsNearby) {
      Map<LivingEntity, Integer> chasedKids = checkHowManyChasersEachFriendHas(friendsNearby);
      return chasedKids.entrySet()
         .stream()
         .sorted(Comparator.comparingInt(Entry::getValue))
         .filter(entry -> entry.getValue() > 0 && entry.getValue() <= 5)
         .map(Entry::getKey)
         .findFirst();
   }

   private static Map<LivingEntity, Integer> checkHowManyChasersEachFriendHas(final List<LivingEntity> friendsNearby) {
      Map<LivingEntity, Integer> chasedKids = Maps.newHashMap();
      friendsNearby.stream()
         .filter(PlayTagWithOtherKids::isChasingSomeone)
         .forEach(chaser -> chasedKids.compute(whoAreYouChasing(chaser), (k, count) -> count == null ? 1 : count + 1));
      return chasedKids;
   }

   private static LivingEntity whoAreYouChasing(final LivingEntity friend) {
      return friend.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).get();
   }

   private static boolean isChasingSomeone(final LivingEntity friend) {
      return friend.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).isPresent();
   }

   private static boolean isFriendChasingMe(final LivingEntity me, final LivingEntity friend) {
      return friend.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).filter(mob -> mob == me).isPresent();
   }
}
