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
   public final boolean tryStart(ServerLevel p_260083_, E p_259643_, long p_259226_) {
      if (this.trigger(p_260083_, p_259643_, p_259226_)) {
         this.status = Behavior.Status.RUNNING;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final void tickOrStop(ServerLevel p_259112_, E p_259594_, long p_259046_) {
      this.doStop(p_259112_, p_259594_, p_259046_);
   }

   @Override
   public final void doStop(ServerLevel p_260215_, E p_259970_, long p_260273_) {
      this.status = Behavior.Status.STOPPED;
   }

   @Override
   public String debugString() {
      return this.getClass().getSimpleName();
   }
}
