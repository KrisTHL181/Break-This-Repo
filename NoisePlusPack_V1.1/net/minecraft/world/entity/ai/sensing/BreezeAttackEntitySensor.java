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

   protected void doTick(ServerLevel p_310391_, Breeze p_312097_) {
      super.doTick(p_310391_, p_312097_);
      p_312097_.getBrain()
         .getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES)
         .stream()
         .flatMap(Collection::stream)
         .filter(EntitySelector.NO_CREATIVE_OR_SPECTATOR)
         .filter(p_359103_ -> Sensor.isEntityAttackable(p_310391_, p_312097_, p_359103_))
         .findFirst()
         .ifPresentOrElse(
            p_310804_ -> p_312097_.getBrain().setMemory(MemoryModuleType.NEAREST_ATTACKABLE, p_310804_),
            () -> p_312097_.getBrain().eraseMemory(MemoryModuleType.NEAREST_ATTACKABLE)
         );
   }
}
