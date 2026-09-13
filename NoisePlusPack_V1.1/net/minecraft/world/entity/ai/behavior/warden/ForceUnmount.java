package net.minecraft.world.entity.ai.behavior.warden;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;

public class ForceUnmount extends Behavior<LivingEntity> {
   public ForceUnmount() {
      super(ImmutableMap.of());
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel p_238424_, LivingEntity p_238425_) {
      return p_238425_.isPassenger();
   }

   @Override
   protected void start(ServerLevel p_238410_, LivingEntity p_238411_, long p_238412_) {
      p_238411_.unRide();
   }
}
