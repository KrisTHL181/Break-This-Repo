package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.ClimbOnTopOfPowderSnowGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;

public class Silverfish extends Monster {
   private Silverfish.@Nullable SilverfishWakeUpFriendsGoal friendsGoal;

   public Silverfish(EntityType<? extends Silverfish> p_33523_, Level p_33524_) {
      super(p_33523_, p_33524_);
   }

   @Override
   protected void registerGoals() {
      this.friendsGoal = new Silverfish.SilverfishWakeUpFriendsGoal(this);
      this.goalSelector.addGoal(1, new FloatGoal(this));
      this.goalSelector.addGoal(1, new ClimbOnTopOfPowderSnowGoal(this, this.level()));
      this.goalSelector.addGoal(3, this.friendsGoal);
      this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, false));
      this.goalSelector.addGoal(5, new Silverfish.SilverfishMergeWithStoneGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.ATTACK_DAMAGE, 1.0);
   }

   @Override
   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.EVENTS;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.SILVERFISH_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_33549_) {
      return SoundEvents.SILVERFISH_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.SILVERFISH_DEATH;
   }

   @Override
   protected void playStepSound(BlockPos p_33543_, BlockState p_33544_) {
      this.playSound(SoundEvents.SILVERFISH_STEP, 0.15F, 1.0F);
   }

   @Override
   public boolean hurtServer(ServerLevel p_367867_, DamageSource p_363178_, float p_365184_) {
      if (this.isInvulnerableTo(p_367867_, p_363178_)) {
         return false;
      }

      if ((p_363178_.getEntity() != null || p_363178_.is(DamageTypeTags.ALWAYS_TRIGGERS_SILVERFISH)) && this.friendsGoal != null) {
         this.friendsGoal.notifyHurt();
      }

      return super.hurtServer(p_367867_, p_363178_, p_365184_);
   }

   @Override
   public void tick() {
      this.yBodyRot = this.getYRot();
      super.tick();
   }

   @Override
   public void setYBodyRot(float p_33553_) {
      this.setYRot(p_33553_);
      super.setYBodyRot(p_33553_);
   }

   @Override
   public float getWalkTargetValue(BlockPos p_33530_, LevelReader p_33531_) {
      return InfestedBlock.isCompatibleHostBlock(p_33531_.getBlockState(p_33530_.below())) ? 10.0F : super.getWalkTargetValue(p_33530_, p_33531_);
   }

   public static boolean checkSilverfishSpawnRules(
      EntityType<Silverfish> p_219077_, LevelAccessor p_219078_, EntitySpawnReason p_360856_, BlockPos p_219080_, RandomSource p_219081_
   ) {
      if (!checkAnyLightMonsterSpawnRules(p_219077_, p_219078_, p_360856_, p_219080_, p_219081_)) {
         return false;
      }

      if (EntitySpawnReason.isSpawner(p_360856_)) {
         return true;
      }

      Player player = p_219078_.getNearestPlayer(p_219080_.getX() + 0.5, p_219080_.getY() + 0.5, p_219080_.getZ() + 0.5, 5.0, true);
      return player == null;
   }

   static class SilverfishMergeWithStoneGoal extends RandomStrollGoal {
      private @Nullable Direction selectedDirection;
      private boolean doMerge;

      public SilverfishMergeWithStoneGoal(Silverfish p_33558_) {
         super(p_33558_, 1.0, 10);
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public boolean canUse() {
         if (this.mob.getTarget() != null) {
            return false;
         }

         if (!this.mob.getNavigation().isDone()) {
            return false;
         }

         RandomSource randomsource = this.mob.getRandom();
         if (getServerLevel(this.mob).getGameRules().get(GameRules.MOB_GRIEFING) && randomsource.nextInt(reducedTickDelay(10)) == 0) {
            this.selectedDirection = Direction.getRandom(randomsource);
            BlockPos blockpos = BlockPos.containing(this.mob.getX(), this.mob.getY() + 0.5, this.mob.getZ()).relative(this.selectedDirection);
            BlockState blockstate = this.mob.level().getBlockState(blockpos);
            if (InfestedBlock.isCompatibleHostBlock(blockstate)) {
               this.doMerge = true;
               return true;
            }
         }

         this.doMerge = false;
         return super.canUse();
      }

      @Override
      public boolean canContinueToUse() {
         return this.doMerge ? false : super.canContinueToUse();
      }

      @Override
      public void start() {
         if (!this.doMerge) {
            super.start();
         } else {
            LevelAccessor levelaccessor = this.mob.level();
            BlockPos blockpos = BlockPos.containing(this.mob.getX(), this.mob.getY() + 0.5, this.mob.getZ()).relative(this.selectedDirection);
            BlockState blockstate = levelaccessor.getBlockState(blockpos);
            if (InfestedBlock.isCompatibleHostBlock(blockstate)) {
               levelaccessor.setBlock(blockpos, InfestedBlock.infestedStateByHost(blockstate), 3);
               this.mob.spawnAnim();
               this.mob.discard();
            }
         }
      }
   }

   static class SilverfishWakeUpFriendsGoal extends Goal {
      private final Silverfish silverfish;
      private int lookForFriends;

      public SilverfishWakeUpFriendsGoal(Silverfish p_33565_) {
         this.silverfish = p_33565_;
      }

      public void notifyHurt() {
         if (this.lookForFriends == 0) {
            this.lookForFriends = this.adjustedTickDelay(20);
         }
      }

      @Override
      public boolean canUse() {
         return this.lookForFriends > 0;
      }

      @Override
      public void tick() {
         this.lookForFriends--;
         if (this.lookForFriends <= 0) {
            Level level = this.silverfish.level();
            RandomSource randomsource = this.silverfish.getRandom();
            BlockPos blockpos = this.silverfish.blockPosition();

            for (int i = 0; i <= 5 && i >= -5; i = (i <= 0 ? 1 : 0) - i) {
               for (int j = 0; j <= 10 && j >= -10; j = (j <= 0 ? 1 : 0) - j) {
                  for (int k = 0; k <= 10 && k >= -10; k = (k <= 0 ? 1 : 0) - k) {
                     BlockPos blockpos1 = blockpos.offset(j, i, k);
                     BlockState blockstate = level.getBlockState(blockpos1);
                     Block block = blockstate.getBlock();
                     if (block instanceof InfestedBlock) {
                        if (getServerLevel(level).getGameRules().get(GameRules.MOB_GRIEFING)) {
                           level.destroyBlock(blockpos1, true, this.silverfish);
                        } else {
                           level.setBlock(blockpos1, ((InfestedBlock)block).hostStateByInfested(level.getBlockState(blockpos1)), 3);
                        }

                        if (randomsource.nextBoolean()) {
                           return;
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
