package net.minecraft.world.entity.monster.piglin;

import java.util.List;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.hoglin.Hoglin;

public class StartHuntingHoglin {
   public static OneShot<Piglin> create() {
      return BehaviorBuilder.create(
         p_259791_ -> p_259791_.group(
               p_259791_.present(MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN),
               p_259791_.absent(MemoryModuleType.ANGRY_AT),
               p_259791_.absent(MemoryModuleType.HUNTED_RECENTLY),
               p_259791_.registered(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS)
            )
            .apply(
               p_259791_,
               (p_259255_, p_260214_, p_259562_, p_259156_) -> (p_359306_, p_359307_, p_359308_) -> {
                  if (!p_359307_.isBaby()
                     && !p_259791_.<List>tryGet(p_259156_).map(p_259958_ -> p_259958_.stream().anyMatch(StartHuntingHoglin::hasHuntedRecently)).isPresent()) {
                     Hoglin hoglin = p_259791_.get(p_259255_);
                     PiglinAi.setAngerTarget(p_359306_, p_359307_, hoglin);
                     PiglinAi.dontKillAnyMoreHoglinsForAWhile(p_359307_);
                     PiglinAi.broadcastAngerTarget(p_359306_, p_359307_, hoglin);
                     p_259791_.<List>tryGet(p_259156_).ifPresent(p_259760_ -> p_259760_.forEach(PiglinAi::dontKillAnyMoreHoglinsForAWhile));
                     return true;
                  } else {
                     return false;
                  }
               }
            )
      );
   }

   private static boolean hasHuntedRecently(AbstractPiglin p_260138_) {
      return p_260138_.getBrain().hasMemoryValue(MemoryModuleType.HUNTED_RECENTLY);
   }
}
