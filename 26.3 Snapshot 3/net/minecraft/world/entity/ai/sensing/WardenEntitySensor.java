package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.Warden;

public class WardenEntitySensor extends NearestLivingEntitySensor<Warden> {
   @Override
   public Set<MemoryModuleType<?>> requires() {
      return ImmutableSet.copyOf(Iterables.concat(super.requires(), List.of(MemoryModuleType.NEAREST_ATTACKABLE)));
   }

   protected void doTick(final ServerLevel level, final Warden body) {
      super.doTick(level, body);
      getClosest(body, e -> e.is(EntityTypes.PLAYER))
         .or(() -> getClosest(body, e -> !e.is(EntityTypes.PLAYER)))
         .ifPresentOrElse(
            entity -> body.getBrain().setMemory(MemoryModuleType.NEAREST_ATTACKABLE, entity),
            () -> body.getBrain().eraseMemory(MemoryModuleType.NEAREST_ATTACKABLE)
         );
   }

   private static Optional<LivingEntity> getClosest(final Warden body, final Predicate<LivingEntity> test) {
      return body.getBrain()
         .getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES)
         .stream()
         .flatMap(Collection::stream)
         .filter(body::canTargetEntity)
         .filter(test)
         .findFirst();
   }
}
