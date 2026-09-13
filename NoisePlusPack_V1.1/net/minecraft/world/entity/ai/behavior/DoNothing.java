package net.minecraft.world.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public class DoNothing implements BehaviorControl<LivingEntity> {
   private final int minDuration;
   private final int maxDuration;
   private Behavior.Status status = Behavior.Status.STOPPED;
   private long endTimestamp;

   public DoNothing(int p_22840_, int p_22841_) {
      this.minDuration = p_22840_;
      this.maxDuration = p_22841_;
   }

   @Override
   public Behavior.Status getStatus() {
      return this.status;
   }

   @Override
   public final boolean tryStart(ServerLevel p_259135_, LivingEntity p_259195_, long p_259189_) {
      this.status = Behavior.Status.RUNNING;
      int i = this.minDuration + p_259135_.getRandom().nextInt(this.maxDuration + 1 - this.minDuration);
      this.endTimestamp = p_259189_ + i;
      return true;
   }

   @Override
   public final void tickOrStop(ServerLevel p_259225_, LivingEntity p_259218_, long p_259803_) {
      if (p_259803_ > this.endTimestamp) {
         this.doStop(p_259225_, p_259218_, p_259803_);
      }
   }

   @Override
   public final void doStop(ServerLevel p_260265_, LivingEntity p_259336_, long p_259826_) {
      this.status = Behavior.Status.STOPPED;
   }

   @Override
   public String debugString() {
      return this.getClass().getSimpleName();
   }
}
