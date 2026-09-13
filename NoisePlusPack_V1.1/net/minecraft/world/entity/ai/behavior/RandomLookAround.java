package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;

public class RandomLookAround extends Behavior<Mob> {
   private final IntProvider interval;
   private final float maxYaw;
   private final float minPitch;
   private final float pitchRange;

   public RandomLookAround(IntProvider p_248963_, float p_251826_, float p_251456_, float p_249962_) {
      super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.GAZE_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT));
      if (p_251456_ > p_249962_) {
         throw new IllegalArgumentException("Minimum pitch is larger than maximum pitch! " + p_251456_ + " > " + p_249962_);
      }

      this.interval = p_248963_;
      this.maxYaw = p_251826_;
      this.minPitch = p_251456_;
      this.pitchRange = p_249962_ - p_251456_;
   }

   protected void start(ServerLevel p_250941_, Mob p_248765_, long p_251801_) {
      RandomSource randomsource = p_248765_.getRandom();
      float f = Mth.clamp(randomsource.nextFloat() * this.pitchRange + this.minPitch, -90.0F, 90.0F);
      float f1 = Mth.wrapDegrees(p_248765_.getYRot() + 2.0F * randomsource.nextFloat() * this.maxYaw - this.maxYaw);
      Vec3 vec3 = Vec3.directionFromRotation(f, f1);
      p_248765_.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(p_248765_.getEyePosition().add(vec3)));
      p_248765_.getBrain().setMemory(MemoryModuleType.GAZE_COOLDOWN_TICKS, this.interval.sample(randomsource));
   }
}
