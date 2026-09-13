package net.minecraft.world.entity.ai.behavior;

import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public interface BehaviorControl<E extends LivingEntity> {
   Behavior.Status getStatus();

   Set<MemoryModuleType<?>> getRequiredMemories();

   boolean tryStart(ServerLevel level, E body, long timestamp);

   void tickOrStop(ServerLevel level, E body, long timestamp);

   void doStop(ServerLevel level, E body, long timestamp);

   String debugString();
}
