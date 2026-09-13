package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class LongJumpToRandomPos<E extends Mob> extends Behavior<E> {
   protected static final int FIND_JUMP_TRIES = 20;
   private static final int PREPARE_JUMP_DURATION = 40;
   protected static final int MIN_PATHFIND_DISTANCE_TO_VALID_JUMP = 8;
   private static final int TIME_OUT_DURATION = 200;
   private static final List<Integer> ALLOWED_ANGLES = Lists.newArrayList(new Integer[]{65, 70, 75, 80});
   private final UniformInt timeBetweenLongJumps;
   protected final int maxLongJumpHeight;
   protected final int maxLongJumpWidth;
   protected final float maxJumpVelocityMultiplier;
   protected List<LongJumpToRandomPos.PossibleJump> jumpCandidates = Lists.newArrayList();
   protected Optional<Vec3> initialPosition = Optional.empty();
   protected @Nullable Vec3 chosenJump;
   protected int findJumpTries;
   protected long prepareJumpStart;
   private final Function<E, SoundEvent> getJumpSound;
   private final BiPredicate<E, BlockPos> acceptableLandingSpot;

   public LongJumpToRandomPos(
      final UniformInt timeBetweenLongJumps,
      final int maxLongJumpHeight,
      final int maxLongJumpWidth,
      final float maxJumpVelocityMultiplier,
      final Function<E, SoundEvent> getJumpSound
   ) {
      this(
         timeBetweenLongJumps, maxLongJumpHeight, maxLongJumpWidth, maxJumpVelocityMultiplier, getJumpSound, LongJumpToRandomPos::defaultAcceptableLandingSpot
      );
   }

   public static <E extends Mob> boolean defaultAcceptableLandingSpot(final E body, final BlockPos targetPos) {
      Level level = body.level();
      BlockPos below = targetPos.below();
      return level.getBlockState(below).isSolidRender() && body.getPathfindingMalus(WalkNodeEvaluator.getPathTypeStatic(body, targetPos)) == 0.0F;
   }

   public LongJumpToRandomPos(
      final UniformInt timeBetweenLongJumps,
      final int maxLongJumpHeight,
      final int maxLongJumpWidth,
      final float maxJumpVelocityMultiplier,
      final Function<E, SoundEvent> getJumpSound,
      final BiPredicate<E, BlockPos> acceptableLandingSpot
   ) {
      super(
         ImmutableMap.of(
            MemoryModuleType.LOOK_TARGET,
            MemoryStatus.REGISTERED,
            MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.LONG_JUMP_MID_JUMP,
            MemoryStatus.VALUE_ABSENT
         ),
         200
      );
      this.timeBetweenLongJumps = timeBetweenLongJumps;
      this.maxLongJumpHeight = maxLongJumpHeight;
      this.maxLongJumpWidth = maxLongJumpWidth;
      this.maxJumpVelocityMultiplier = maxJumpVelocityMultiplier;
      this.getJumpSound = getJumpSound;
      this.acceptableLandingSpot = acceptableLandingSpot;
   }

   protected boolean checkExtraStartConditions(final ServerLevel level, final Mob body) {
      boolean canStart = body.onGround() && !body.isInWater() && !body.isInLava() && !level.getBlockState(body.blockPosition()).is(Blocks.HONEY_BLOCK);
      if (!canStart) {
         body.getBrain().setMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, this.timeBetweenLongJumps.sample(level.getRandom()) / 2);
      }

      return canStart;
   }

   protected boolean canStillUse(final ServerLevel level, final Mob body, final long timestamp) {
      boolean isValid = this.initialPosition.isPresent()
         && this.initialPosition.get().equals(body.position())
         && this.findJumpTries > 0
         && !body.isInWater()
         && (this.chosenJump != null || !this.jumpCandidates.isEmpty());
      if (!isValid && body.getBrain().getMemory(MemoryModuleType.LONG_JUMP_MID_JUMP).isEmpty()) {
         body.getBrain().setMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, this.timeBetweenLongJumps.sample(level.getRandom()) / 2);
         body.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
      }

      return isValid;
   }

   protected void start(final ServerLevel level, final E body, final long timestamp) {
      this.chosenJump = null;
      this.findJumpTries = 20;
      this.initialPosition = Optional.of(body.position());
      BlockPos mobPos = body.blockPosition();
      int mobX = mobPos.getX();
      int mobY = mobPos.getY();
      int mobZ = mobPos.getZ();
      this.jumpCandidates = BlockPos.betweenClosedStream(
            mobX - this.maxLongJumpWidth,
            mobY - this.maxLongJumpHeight,
            mobZ - this.maxLongJumpWidth,
            mobX + this.maxLongJumpWidth,
            mobY + this.maxLongJumpHeight,
            mobZ + this.maxLongJumpWidth
         )
         .filter(pos -> !pos.equals(mobPos))
         .map(pos -> new LongJumpToRandomPos.PossibleJump(pos.immutable(), Mth.ceil(mobPos.distSqr(pos))))
         .collect(Collectors.toCollection(Lists::newArrayList));
   }

   protected void tick(final ServerLevel level, final E body, final long timestamp) {
      if (this.chosenJump != null) {
         if (timestamp - this.prepareJumpStart >= 40L) {
            body.setYRot(body.yBodyRot);
            body.setDiscardFriction(true);
            double orgLength = this.chosenJump.length();
            double lengthWithJumpBoost = orgLength + body.getJumpBoostPower();
            body.setDeltaMovement(this.chosenJump.scale(lengthWithJumpBoost / orgLength));
            body.getBrain().setMemory(MemoryModuleType.LONG_JUMP_MID_JUMP, true);
            level.playSound(null, body, this.getJumpSound.apply(body), SoundSource.NEUTRAL, 1.0F, 1.0F);
         }
      } else {
         this.findJumpTries--;
         this.pickCandidate(level, body, timestamp);
      }
   }

   protected void pickCandidate(final ServerLevel level, final E body, final long timestamp) {
      while (!this.jumpCandidates.isEmpty()) {
         Optional<LongJumpToRandomPos.PossibleJump> optionalPosition = this.getJumpCandidate(level);
         if (!optionalPosition.isEmpty()) {
            LongJumpToRandomPos.PossibleJump position = optionalPosition.get();
            BlockPos targetPos = position.targetPos();
            if (this.isAcceptableLandingPosition(level, body, targetPos)) {
               Vec3 targetPosition = Vec3.atCenterOf(targetPos);
               Vec3 jumpVector = this.calculateOptimalJumpVector(body, targetPosition);
               if (jumpVector != null) {
                  body.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(targetPos));
                  PathNavigation navigation = body.getNavigation();
                  Path path = navigation.createPath(targetPos, 0, 8);
                  if (path == null || !path.canReach()) {
                     this.chosenJump = jumpVector;
                     this.prepareJumpStart = timestamp;
                     return;
                  }
               }
            }
         }
      }
   }

   protected Optional<LongJumpToRandomPos.PossibleJump> getJumpCandidate(final ServerLevel level) {
      Optional<LongJumpToRandomPos.PossibleJump> randomItem = WeightedRandom.getRandomItem(
         level.getRandom(), this.jumpCandidates, LongJumpToRandomPos.PossibleJump::weight
      );
      randomItem.ifPresent(this.jumpCandidates::remove);
      return randomItem;
   }

   private boolean isAcceptableLandingPosition(final ServerLevel level, final E body, final BlockPos targetPos) {
      BlockPos bodyPos = body.blockPosition();
      int mobX = bodyPos.getX();
      int mobZ = bodyPos.getZ();
      return mobX == targetPos.getX() && mobZ == targetPos.getZ() ? false : this.acceptableLandingSpot.test(body, targetPos);
   }

   protected @Nullable Vec3 calculateOptimalJumpVector(final Mob body, final Vec3 targetPos) {
      List<Integer> allowedAngles = Lists.newArrayList(ALLOWED_ANGLES);
      Collections.shuffle(allowedAngles);
      float maxJumpVelocity = (float)(body.getAttributeValue(Attributes.JUMP_STRENGTH) * this.maxJumpVelocityMultiplier);

      for (int angle : allowedAngles) {
         Optional<Vec3> velocityVector = LongJumpUtil.calculateJumpVectorForAngle(body, targetPos, maxJumpVelocity, angle, true);
         if (velocityVector.isPresent()) {
            return velocityVector.get();
         }
      }

      return null;
   }

   public record PossibleJump(BlockPos targetPos, int weight) {
   }
}
