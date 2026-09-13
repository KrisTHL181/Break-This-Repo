package net.minecraft.world.entity.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.ClimbOnTopOfPowderSnowGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class Endermite extends Monster {
   private static final int MAX_LIFE = 2400;
   private static final int DEFAULT_LIFE = 0;
   private int life = 0;

   public Endermite(EntityType<? extends Endermite> p_32591_, Level p_32592_) {
      super(p_32591_, p_32592_);
      this.xpReward = 3;
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(1, new FloatGoal(this));
      this.goalSelector.addGoal(1, new ClimbOnTopOfPowderSnowGoal(this, this.level()));
      this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
      this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
      this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.ATTACK_DAMAGE, 2.0);
   }

   @Override
   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.EVENTS;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.ENDERMITE_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_32615_) {
      return SoundEvents.ENDERMITE_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.ENDERMITE_DEATH;
   }

   @Override
   protected void playStepSound(BlockPos p_32607_, BlockState p_32608_) {
      this.playSound(SoundEvents.ENDERMITE_STEP, 0.15F, 1.0F);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_407027_) {
      super.readAdditionalSaveData(p_407027_);
      this.life = p_407027_.getIntOr("Lifetime", 0);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_410669_) {
      super.addAdditionalSaveData(p_410669_);
      p_410669_.putInt("Lifetime", this.life);
   }

   @Override
   public void tick() {
      this.yBodyRot = this.getYRot();
      super.tick();
   }

   @Override
   public void setYBodyRot(float p_32621_) {
      this.setYRot(p_32621_);
      super.setYBodyRot(p_32621_);
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (this.level().isClientSide()) {
         for (int i = 0; i < 2; i++) {
            this.level()
               .addParticle(
                  ParticleTypes.PORTAL,
                  this.getRandomX(0.5),
                  this.getRandomY(),
                  this.getRandomZ(0.5),
                  (this.random.nextDouble() - 0.5) * 2.0,
                  -this.random.nextDouble(),
                  (this.random.nextDouble() - 0.5) * 2.0
               );
         }
      } else {
         if (!this.isPersistenceRequired()) {
            this.life++;
         }

         if (this.life >= 2400) {
            this.discard();
         }
      }
   }

   public static boolean checkEndermiteSpawnRules(
      EntityType<Endermite> p_218969_, LevelAccessor p_218970_, EntitySpawnReason p_364641_, BlockPos p_218972_, RandomSource p_218973_
   ) {
      if (!checkAnyLightMonsterSpawnRules(p_218969_, p_218970_, p_364641_, p_218972_, p_218973_)) {
         return false;
      }

      if (EntitySpawnReason.isSpawner(p_364641_)) {
         return true;
      }

      Player player = p_218970_.getNearestPlayer(p_218972_.getX() + 0.5, p_218972_.getY() + 0.5, p_218972_.getZ() + 0.5, 5.0, true);
      return player == null;
   }
}
