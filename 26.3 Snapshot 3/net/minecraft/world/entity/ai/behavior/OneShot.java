package net.minecraft.world.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.Trigger;

public abstract class OneShot<E extends LivingEntity> implements BehaviorControl<E>, Trigger<E> {
   private Behavior.Status status = Behavior.Status.STOPPED;

   @Override
   public final Behavior.Status getStatus() {
      return this.status;
   }

   @Override
   public final boolean tryStart(final ServerLevel level, final E body, final long timestamp) {
      if (this.trigger(level, body, timestamp)) {
         this.status = Behavior.Status.RUNNING;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final void tickOrStop(final ServerLevel level, final E body, final long timestamp) {
      this.doStop(level, body, timestamp);
   }

   @Override
   public final void doStop(final ServerLevel level, final E body, final long timestamp) {
      this.status = Behavior.Status.STOPPED;
   }

   @Override
   public String debugString() {
      return this.getClass().getSimpleName();
   }
}
