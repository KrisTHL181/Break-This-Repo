package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.frog.Frog;

public class Croak extends Behavior<Frog> {
   private static final int CROAK_TICKS = 60;
   private static final int TIME_OUT_DURATION = 100;
   private int croakCounter;

   public Croak() {
      super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT), 100);
   }

   protected boolean checkExtraStartConditions(ServerLevel p_217151_, Frog p_217152_) {
      return p_217152_.getPose() == Pose.STANDING;
   }

   protected boolean canStillUse(ServerLevel p_217154_, Frog p_217155_, long p_217156_) {
      return this.croakCounter < 60;
   }

   protected void start(ServerLevel p_217162_, Frog p_217163_, long p_217164_) {
      if (!p_217163_.isInLiquid()) {
         p_217163_.setPose(Pose.CROAKING);
         this.croakCounter = 0;
      }
   }

   protected void stop(ServerLevel p_217170_, Frog p_217171_, long p_217172_) {
      p_217171_.setPose(Pose.STANDING);
   }

   protected void tick(ServerLevel p_217178_, Frog p_217179_, long p_217180_) {
      this.croakCounter++;
   }
}
