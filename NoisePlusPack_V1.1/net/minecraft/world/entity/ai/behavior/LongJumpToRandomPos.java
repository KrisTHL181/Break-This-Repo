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

   public LongJumpToRandomPos(UniformInt p_147637_, int p_147638_, int p_147639_, float p_147640_, Function<E, SoundEvent> p_147641_) {
      this(p_147637_, p_147638_, p_147639_, p_147640_, p_147641_, LongJumpToRandomPos::defaultAcceptableLandingSpot);
   }

   public static <E extends Mob> boolean defaultAcceptableLandingSpot(E p_251540_, BlockPos p_248879_) {
      Level level = p_251540_.level();
      BlockPos blockpos = p_248879_.below();
      return level.getBlockState(blockpos).isSolidRender() && p_251540_.getPathfindingMalus(WalkNodeEvaluator.getPathTypeStatic(p_251540_, p_248879_)) == 0.0F;
   }

   public LongJumpToRandomPos(
      UniformInt p_251244_, int p_248763_, int p_251698_, float p_250165_, Function<E, SoundEvent> p_249738_, BiPredicate<E, BlockPos> p_249945_
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
      this.timeBetweenLongJumps = p_251244_;
      this.maxLongJumpHeight = p_248763_;
      this.maxLongJumpWidth = p_251698_;
      this.maxJumpVelocityMultiplier = p_250165_;
      this.getJumpSound = p_249738_;
      this.acceptableLandingSpot = p_249945_;
   }

   protected boolean checkExtraStartConditions(ServerLevel p_147650_, Mob p_147651_) {
      boolean flag = p_147651_.onGround()
         && !p_147651_.isInWater()
         && !p_147651_.isInLava()
         && !p_147650_.getBlockState(p_147651_.blockPosition()).is(Blocks.HONEY_BLOCK);
      if (!flag) {
         p_147651_.getBrain().setMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, this.timeBetweenLongJumps.sample(p_147650_.random) / 2);
      }

      return flag;
   }

   protected boolean canStillUse(ServerLevel p_147653_, Mob p_147654_, long p_147655_) {
      boolean flag = this.initialPosition.isPresent()
         && this.initialPosition.get().equals(p_147654_.position())
         && this.findJumpTries > 0
         && !p_147654_.isInWater()
         && (this.chosenJump != null || !this.jumpCandidates.isEmpty());
      if (!flag && p_147654_.getBrain().getMemory(MemoryModuleType.LONG_JUMP_MID_JUMP).isEmpty()) {
         p_147654_.getBrain().setMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, this.timeBetweenLongJumps.sample(p_147653_.random) / 2);
         p_147654_.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
      }

      return flag;
   }

   protected void start(ServerLevel p_147676_, E p_147677_, long p_147678_) {
      this.chosenJump = null;
      this.findJumpTries = 20;
      this.initialPosition = Optional.of(p_147677_.position());
      BlockPos blockpos = p_147677_.blockPosition();
      int i = blockpos.getX();
      int j = blockpos.getY();
      int k = blockpos.getZ();
      this.jumpCandidates = BlockPos.betweenClosedStream(
            i - this.maxLongJumpWidth,
            j - this.maxLongJumpHeight,
            k - this.maxLongJumpWidth,
            i + this.maxLongJumpWidth,
            j + this.maxLongJumpHeight,
            k + this.maxLongJumpWidth
         )
         .filter(p_217317_ -> !p_217317_.equals(blockpos))
         .map(p_217314_ -> new LongJumpToRandomPos.PossibleJump(p_217314_.immutable(), Mth.ceil(blockpos.distSqr(p_217314_))))
         .collect(Collectors.toCollection(Lists::newArrayList));
   }

   protected void tick(ServerLevel p_147680_, E p_147681_, long p_147682_) {
      if (this.chosenJump != null) {
         if (p_147682_ - this.prepareJumpStart >= 40L) {
            p_147681_.setYRot(p_147681_.yBodyRot);
            p_147681_.setDiscardFriction(true);
            double d0 = this.chosenJump.length();
            double d1 = d0 + p_147681_.getJumpBoostPower();
            p_147681_.setDeltaMovement(this.chosenJump.scale(d1 / d0));
            p_147681_.getBrain().setMemory(MemoryModuleType.LONG_JUMP_MID_JUMP, true);
            p_147680_.playSound(null, p_147681_, this.getJumpSound.apply(p_147681_), SoundSource.NEUTRAL, 1.0F, 1.0F);
         }
      } else {
         this.findJumpTries--;
         this.pickCandidate(p_147680_, p_147681_, p_147682_);
      }
   }

   protected void pickCandidate(ServerLevel p_217319_, E p_217320_, long p_217321_) {
      while (!this.jumpCandidates.isEmpty()) {
         Optional<LongJumpToRandomPos.PossibleJump> optional = this.getJumpCandidate(p_217319_);
         if (!optional.isEmpty()) {
            LongJumpToRandomPos.PossibleJump longjumptorandompos$possiblejump = optional.get();
            BlockPos blockpos = longjumptorandompos$possiblejump.targetPos();
            if (this.isAcceptableLandingPosition(p_217319_, p_217320_, blockpos)) {
               Vec3 vec3 = Vec3.atCenterOf(blockpos);
               Vec3 vec31 = this.calculateOptimalJumpVector(p_217320_, vec3);
               if (vec31 != null) {
                  p_217320_.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(blockpos));
                  PathNavigation pathnavigation = p_217320_.getNavigation();
                  Path path = pathnavigation.createPath(blockpos, 0, 8);
                  if (path == null || !path.canReach()) {
                     this.chosenJump = vec31;
                     this.prepareJumpStart = p_217321_;
                     return;
                  }
               }
            }
         }
      }
   }

   protected Optional<LongJumpToRandomPos.PossibleJump> getJumpCandidate(ServerLevel p_217299_) {
      Optional<LongJumpToRandomPos.PossibleJump> optional = WeightedRandom.getRandomItem(
         p_217299_.random, this.jumpCandidates, LongJumpToRandomPos.PossibleJump::weight
      );
      optional.ifPresent(this.jumpCandidates::remove);
      return optional;
   }

   private boolean isAcceptableLandingPosition(ServerLevel p_217300_, E p_217301_, BlockPos p_217302_) {
      BlockPos blockpos = p_217301_.blockPosition();
      int i = blockpos.getX();
      int j = blockpos.getZ();
      return i == p_217302_.getX() && j == p_217302_.getZ() ? false : this.acceptableLandingSpot.test(p_217301_, p_217302_);
   }

   protected @Nullable Vec3 calculateOptimalJumpVector(Mob p_217304_, Vec3 p_217305_) {
      List<Integer> list = Lists.newArrayList(ALLOWED_ANGLES);
      Collections.shuffle(list);
      float f = (float)(p_217304_.getAttributeValue(Attributes.JUMP_STRENGTH) * this.maxJumpVelocityMultiplier);

      for (int i : list) {
         Optional<Vec3> optional = LongJumpUtil.calculateJumpVectorForAngle(p_217304_, p_217305_, f, i, true);
         if (optional.isPresent()) {
            return optional.get();
         }
      }

      return null;
   }

   public record PossibleJump(BlockPos targetPos, int weight) {
   }
}
