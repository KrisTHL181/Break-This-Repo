package net.minecraft.world.entity.monster.piglin;

import java.util.List;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.hoglin.Hoglin;

public class StartHuntingHoglin {
    public static OneShot<Piglin> create() {
        return BehaviorBuilder.create(
            i -> i.group(
                    i.present(MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN),
                    i.absent(MemoryModuleType.ANGRY_AT),
                    i.absent(MemoryModuleType.HUNTED_RECENTLY),
                    i.registered(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS)
                )
                .apply(i, (huntable, angryAt, huntedRecently, nearestPiglins) -> (level, body, timestamp) -> {
                    if (!body.isBaby() && !i.tryGet(nearestPiglins).filter(p -> p.stream().anyMatch(StartHuntingHoglin::hasHuntedRecently)).isPresent()) {
                        Hoglin target = i.get(huntable);
                        PiglinAi.setAngerTarget(level, body, target);
                        PiglinAi.dontKillAnyMoreHoglinsForAWhile(body);
                        PiglinAi.broadcastAngerTarget(level, body, target);
                        i.tryGet(nearestPiglins).ifPresent(p -> p.forEach(PiglinAi::dontKillAnyMoreHoglinsForAWhile));
                        return true;
                    } else {
                        return false;
                    }
                })
        );
    }

    private static boolean hasHuntedRecently(final AbstractPiglin otherPiglin) {
        return otherPiglin.getBrain().hasMemoryValue(MemoryModuleType.HUNTED_RECENTLY);
    }
}
