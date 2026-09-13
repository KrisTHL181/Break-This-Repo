package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.breeze.Breeze;

public class BreezeAttackEntitySensor extends NearestLivingEntitySensor<Breeze> {
   @Override
   public Set<MemoryModuleType<?>> requires() {
      return ImmutableSet.copyOf(Iterables.concat(super.requires(), List.of(MemoryModuleType.NEAREST_ATTACKABLE)));
   }

   protected void doTick(final ServerLevel level, final Breeze breeze) {
      super.doTick(level, breeze);
      breeze.getBrain()
         .getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES)
         .stream()
         .flatMap(Collection::stream)
         .filter(EntitySelector.NO_CREATIVE_OR_SPECTATOR)
         .filter(entity -> Sensor.isEntityAttackable(level, breeze, entity))
         .findFirst()
         .ifPresentOrElse(
            entity -> breeze.getBrain().setMemory(MemoryModuleType.NEAREST_ATTACKABLE, entity),
            () -> breeze.getBrain().eraseMemory(MemoryModuleType.NEAREST_ATTACKABLE)
         );
   }
}
