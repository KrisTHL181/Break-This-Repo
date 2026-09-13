package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class RunOne<E extends LivingEntity> extends GateBehavior<E> {
   public RunOne(final List<Pair<? extends BehaviorControl<? super E>, Integer>> weightedBehaviors) {
      this(ImmutableMap.of(), weightedBehaviors);
   }

   public RunOne(final Map<MemoryModuleType<?>, MemoryStatus> entryCondition, final List<Pair<? extends BehaviorControl<? super E>, Integer>> weightedBehaviors) {
      super(entryCondition, ImmutableSet.of(), GateBehavior.OrderPolicy.SHUFFLED, GateBehavior.RunningPolicy.RUN_ONE, weightedBehaviors);
   }
}
