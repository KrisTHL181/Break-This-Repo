package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class LongJumpMidJump extends Behavior<Mob> {
   public static final int TIME_OUT_DURATION = 100;
   private final UniformInt timeBetweenLongJumps;
   private final SoundEvent landingSound;

   public LongJumpMidJump(final UniformInt timeBetweenLongJumps, final SoundEvent landingSound) {
      super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.LONG_JUMP_MID_JUMP, MemoryStatus.VALUE_PRESENT), 100);
      this.timeBetweenLongJumps = timeBetweenLongJumps;
      this.landingSound = landingSound;
   }

   protected boolean canStillUse(final ServerLevel level, final Mob body, final long timestamp) {
      return !body.onGround();
   }

   protected void start(final ServerLevel level, final Mob body, final long timestamp) {
      body.setDiscardFriction(true);
      body.setPose(Pose.LONG_JUMPING);
   }

   protected void stop(final ServerLevel level, final Mob body, final long timestamp) {
      if (body.onGround()) {
         body.setDeltaMovement(body.getDeltaMovement().multiply(0.1F, 1.0, 0.1F));
         level.playSound(null, body, this.landingSound, SoundSource.NEUTRAL, 2.0F, 1.0F);
      }

      body.setDiscardFriction(false);
      body.setPose(Pose.STANDING);
      body.getBrain().eraseMemory(MemoryModuleType.LONG_JUMP_MID_JUMP);
      body.getBrain().setMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, this.timeBetweenLongJumps.sample(level.getRandom()));
   }
}
