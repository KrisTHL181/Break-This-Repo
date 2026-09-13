package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.Trigger;

public class TriggerGate {
   public static <E extends LivingEntity> OneShot<E> triggerOneShuffled(List<Pair<? extends Trigger<? super E>, Integer>> p_259551_) {
      return triggerGate(p_259551_, GateBehavior.OrderPolicy.SHUFFLED, GateBehavior.RunningPolicy.RUN_ONE);
   }

   public static <E extends LivingEntity> OneShot<E> triggerGate(
      List<Pair<? extends Trigger<? super E>, Integer>> p_259442_, GateBehavior.OrderPolicy p_259823_, GateBehavior.RunningPolicy p_259632_
   ) {
      ShufflingList<Trigger<? super E>> shufflinglist = new ShufflingList<>();
      p_259442_.forEach(p_260333_ -> shufflinglist.add((Trigger)p_260333_.getFirst(), (Integer)p_260333_.getSecond()));
      return BehaviorBuilder.create(p_259457_ -> p_259457_.point((p_260107_, p_259505_, p_259999_) -> {
         if (p_259823_ == GateBehavior.OrderPolicy.SHUFFLED) {
            shufflinglist.shuffle();
         }

         for (Trigger<? super E> trigger : shufflinglist) {
            if (trigger.trigger(p_260107_, p_259505_, p_259999_) && p_259632_ == GateBehavior.RunningPolicy.RUN_ONE) {
               break;
            }
         }

         return true;
      }));
   }
}
