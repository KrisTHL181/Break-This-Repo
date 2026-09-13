package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.Trigger;

public class TriggerGate {
   public static <E extends LivingEntity> OneShot<E> triggerOneShuffled(final List<Pair<? extends Trigger<? super E>, Integer>> weightedTriggers) {
      return triggerGate(weightedTriggers, GateBehavior.OrderPolicy.SHUFFLED, GateBehavior.RunningPolicy.RUN_ONE);
   }

   public static <E extends LivingEntity> OneShot<E> triggerGate(
      final List<Pair<? extends Trigger<? super E>, Integer>> weightedBehaviors,
      final GateBehavior.OrderPolicy orderPolicy,
      final GateBehavior.RunningPolicy runningPolicy
   ) {
      ShufflingList<Trigger<? super E>> behaviors = new ShufflingList<>();
      weightedBehaviors.forEach(entry -> behaviors.add((Trigger)entry.getFirst(), (Integer)entry.getSecond()));
      return BehaviorBuilder.create(i -> i.point((level, body, timestamp) -> {
         if (orderPolicy == GateBehavior.OrderPolicy.SHUFFLED) {
            behaviors.shuffle();
         }

         for (Trigger<? super E> behavior : behaviors) {
            if (behavior.trigger(level, body, timestamp) && runningPolicy == GateBehavior.RunningPolicy.RUN_ONE) {
               break;
            }
         }

         return true;
      }));
   }
}
