package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import org.jspecify.annotations.Nullable;

public class JumpOnBed extends Behavior<Mob> {
   private static final int MAX_TIME_TO_REACH_BED = 100;
   private static final int MIN_JUMPS = 3;
   private static final int MAX_JUMPS = 6;
   private static final int COOLDOWN_BETWEEN_JUMPS = 5;
   private final float speedModifier;
   private @Nullable BlockPos targetBed;
   private int remainingTimeToReachBed;
   private int remainingJumps;
   private int remainingCooldownUntilNextJump;

   public JumpOnBed(final float speedModifier) {
      super(ImmutableMap.of(MemoryModuleType.NEAREST_BED, MemoryStatus.VALUE_PRESENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
      this.speedModifier = speedModifier;
   }

   protected boolean checkExtraStartConditions(final ServerLevel level, final Mob body) {
      return body.isBaby() && this.nearBed(level, body);
   }

   protected void start(final ServerLevel level, final Mob body, final long timestamp) {
      super.start(level, body, timestamp);
      this.getNearestBed(body).ifPresent(targetBed -> {
         this.targetBed = targetBed;
         this.remainingTimeToReachBed = 100;
         this.remainingJumps = 3 + level.getRandom().nextInt(4);
         this.remainingCooldownUntilNextJump = 0;
         this.startWalkingTowardsBed(body, targetBed);
      });
   }

   protected void stop(final ServerLevel level, final Mob body, final long timestamp) {
      super.stop(level, body, timestamp);
      this.targetBed = null;
      this.remainingTimeToReachBed = 0;
      this.remainingJumps = 0;
      this.remainingCooldownUntilNextJump = 0;
   }

   protected boolean canStillUse(final ServerLevel level, final Mob body, final long timestamp) {
      return body.isBaby()
         && this.targetBed != null
         && this.isJumpable(level, this.targetBed)
         && !this.tiredOfWalking(level, body)
         && !this.tiredOfJumping(level, body);
   }

   @Override
   protected boolean timedOut(final long timestamp) {
      return false;
   }

   protected void tick(final ServerLevel level, final Mob body, final long timestamp) {
      if (!this.onOrOverBed(level, body)) {
         this.remainingTimeToReachBed--;
      } else if (this.remainingCooldownUntilNextJump > 0) {
         this.remainingCooldownUntilNextJump--;
      } else {
         if (this.onBedSurface(level, body)) {
            body.getJumpControl().jump();
            this.remainingJumps--;
            this.remainingCooldownUntilNextJump = 5;
         }
      }
   }

   private void startWalkingTowardsBed(final Mob body, final BlockPos bedPos) {
      body.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(bedPos, this.speedModifier, 0));
   }

   private boolean nearBed(final ServerLevel level, final Mob body) {
      return this.onOrOverBed(level, body) || this.getNearestBed(body).isPresent();
   }

   private boolean onOrOverBed(final ServerLevel level, final Mob body) {
      BlockPos bodyPos = body.blockPosition();
      BlockPos oneBelow = bodyPos.below();
      return this.isJumpable(level, bodyPos) || this.isJumpable(level, oneBelow);
   }

   private boolean onBedSurface(final ServerLevel level, final Mob body) {
      return this.isJumpable(level, body.blockPosition());
   }

   private boolean isJumpable(final ServerLevel level, final BlockPos bodyPos) {
      return level.getBlockState(bodyPos).is(BlockTags.VILLAGER_BABIES_CAN_JUMP_ON_BED);
   }

   private Optional<BlockPos> getNearestBed(final Mob body) {
      return body.getBrain().getMemory(MemoryModuleType.NEAREST_BED);
   }

   private boolean tiredOfWalking(final ServerLevel level, final Mob body) {
      return !this.onOrOverBed(level, body) && this.remainingTimeToReachBed <= 0;
   }

   private boolean tiredOfJumping(final ServerLevel level, final Mob body) {
      return this.onOrOverBed(level, body) && this.remainingJumps <= 0;
   }
}
