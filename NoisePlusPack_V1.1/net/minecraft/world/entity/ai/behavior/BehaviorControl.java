package net.minecraft.world.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public interface BehaviorControl<E extends LivingEntity> {
   Behavior.Status getStatus();

   boolean tryStart(ServerLevel var1, E var2, long var3);

   void tickOrStop(ServerLevel var1, E var2, long var3);

   void doStop(ServerLevel var1, E var2, long var3);

   String debugString();
}
