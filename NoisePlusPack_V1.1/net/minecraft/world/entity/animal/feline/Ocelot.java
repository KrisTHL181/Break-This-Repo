package net.minecraft.world.entity.animal.feline;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OcelotAttackGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Ocelot extends Animal {
   public static final double CROUCH_SPEED_MOD = 0.6;
   public static final double WALK_SPEED_MOD = 0.8;
   public static final double SPRINT_SPEED_MOD = 1.33;
   private static final EntityDataAccessor<Boolean> DATA_TRUSTING = SynchedEntityData.defineId(Ocelot.class, EntityDataSerializers.BOOLEAN);
   private static final boolean DEFAULT_TRUSTING = false;
   private Ocelot.@Nullable OcelotAvoidEntityGoal<Player> ocelotAvoidPlayersGoal;
   private Ocelot.@Nullable OcelotTemptGoal temptGoal;

   public Ocelot(EntityType<? extends Ocelot> p_460539_, Level p_456452_) {
      super(p_460539_, p_456452_);
      this.reassessTrustingGoals();
   }

   boolean isTrusting() {
      return this.entityData.get(DATA_TRUSTING);
   }

   private void setTrusting(boolean p_456166_) {
      this.entityData.set(DATA_TRUSTING, p_456166_);
      this.reassessTrustingGoals();
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_456902_) {
      super.addAdditionalSaveData(p_456902_);
      p_456902_.putBoolean("Trusting", this.isTrusting());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_451382_) {
      super.readAdditionalSaveData(p_451382_);
      this.setTrusting(p_451382_.getBooleanOr("Trusting", false));
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_459358_) {
      super.defineSynchedData(p_459358_);
      p_459358_.define(DATA_TRUSTING, false);
   }

   @Override
   protected void registerGoals() {
      this.temptGoal = new Ocelot.OcelotTemptGoal(this, 0.6, p_456888_ -> p_456888_.is(ItemTags.OCELOT_FOOD), true);
      this.goalSelector.addGoal(1, new FloatGoal(this));
      this.goalSelector.addGoal(3, this.temptGoal);
      this.goalSelector.addGoal(7, new LeapAtTargetGoal(this, 0.3F));
      this.goalSelector.addGoal(8, new OcelotAttackGoal(this));
      this.goalSelector.addGoal(9, new BreedGoal(this, 0.8));
      this.goalSelector.addGoal(10, new WaterAvoidingRandomStrollGoal(this, 0.8, 1.0000001E-5F));
      this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 10.0F));
      this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Chicken.class, false));
      this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Turtle.class, 10, false, false, Turtle.BABY_ON_LAND_SELECTOR));
   }

   @Override
   public void customServerAiStep(ServerLevel p_452718_) {
      if (this.getMoveControl().hasWanted()) {
         double d0 = this.getMoveControl().getSpeedModifier();
         if (d0 == 0.6) {
            this.setPose(Pose.CROUCHING);
            this.setSprinting(false);
         } else if (d0 == 1.33) {
            this.setPose(Pose.STANDING);
            this.setSprinting(true);
         } else {
            this.setPose(Pose.STANDING);
            this.setSprinting(false);
         }
      } else {
         this.setPose(Pose.STANDING);
         this.setSprinting(false);
      }
   }

   @Override
   public boolean removeWhenFarAway(double p_453677_) {
      return !this.isTrusting() && this.tickCount > 2400;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.3F).add(Attributes.ATTACK_DAMAGE, 3.0);
   }

   @Override
   protected @Nullable SoundEvent getAmbientSound() {
      return SoundEvents.OCELOT_AMBIENT;
   }

   @Override
   public int getAmbientSoundInterval() {
      return 900;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_456559_) {
      return SoundEvents.OCELOT_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.OCELOT_DEATH;
   }

   @Override
   public InteractionResult mobInteract(Player p_454842_, InteractionHand p_450856_) {
      ItemStack itemstack = p_454842_.getItemInHand(p_450856_);
      if ((this.temptGoal == null || this.temptGoal.isRunning()) && !this.isTrusting() && this.isFood(itemstack) && p_454842_.distanceToSqr(this) < 9.0) {
         this.usePlayerItem(p_454842_, p_450856_, itemstack);
         if (!this.level().isClientSide()) {
            if (this.random.nextInt(3) == 0) {
               this.setTrusting(true);
               this.spawnTrustingParticles(true);
               this.level().broadcastEntityEvent(this, (byte)41);
            } else {
               this.spawnTrustingParticles(false);
               this.level().broadcastEntityEvent(this, (byte)40);
            }
         }

         return InteractionResult.SUCCESS;
      } else {
         return super.mobInteract(p_454842_, p_450856_);
      }
   }

   @Override
   public void handleEntityEvent(byte p_455513_) {
      if (p_455513_ == 41) {
         this.spawnTrustingParticles(true);
      } else if (p_455513_ == 40) {
         this.spawnTrustingParticles(false);
      } else {
         super.handleEntityEvent(p_455513_);
      }
   }

   private void spawnTrustingParticles(boolean p_451811_) {
      ParticleOptions particleoptions = ParticleTypes.HEART;
      if (!p_451811_) {
         particleoptions = ParticleTypes.SMOKE;
      }

      for (int i = 0; i < 7; i++) {
         double d0 = this.random.nextGaussian() * 0.02;
         double d1 = this.random.nextGaussian() * 0.02;
         double d2 = this.random.nextGaussian() * 0.02;
         this.level().addParticle(particleoptions, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), d0, d1, d2);
      }
   }

   protected void reassessTrustingGoals() {
      if (this.ocelotAvoidPlayersGoal == null) {
         this.ocelotAvoidPlayersGoal = new Ocelot.OcelotAvoidEntityGoal<>(this, Player.class, 16.0F, 0.8, 1.33);
      }

      this.goalSelector.removeGoal(this.ocelotAvoidPlayersGoal);
      if (!this.isTrusting()) {
         this.goalSelector.addGoal(4, this.ocelotAvoidPlayersGoal);
      }
   }

   public @Nullable Ocelot getBreedOffspring(ServerLevel p_455814_, AgeableMob p_451458_) {
      return EntityType.OCELOT.create(p_455814_, EntitySpawnReason.BREEDING);
   }

   @Override
   public boolean isFood(ItemStack p_459174_) {
      return p_459174_.is(ItemTags.OCELOT_FOOD);
   }

   public static boolean checkOcelotSpawnRules(
      EntityType<Ocelot> p_456164_, LevelAccessor p_450266_, EntitySpawnReason p_460453_, BlockPos p_456781_, RandomSource p_451468_
   ) {
      return p_451468_.nextInt(3) != 0;
   }

   @Override
   public boolean checkSpawnObstruction(LevelReader p_455881_) {
      if (p_455881_.isUnobstructed(this) && !p_455881_.containsAnyLiquid(this.getBoundingBox())) {
         BlockPos blockpos = this.blockPosition();
         if (blockpos.getY() < p_455881_.getSeaLevel()) {
            return false;
         }

         BlockState blockstate = p_455881_.getBlockState(blockpos.below());
         if (blockstate.is(Blocks.GRASS_BLOCK) || blockstate.is(BlockTags.LEAVES)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_450717_, DifficultyInstance p_452608_, EntitySpawnReason p_455808_, @Nullable SpawnGroupData p_452742_
   ) {
      if (p_452742_ == null) {
         p_452742_ = new AgeableMob.AgeableMobGroupData(1.0F);
      }

      return super.finalizeSpawn(p_450717_, p_452608_, p_455808_, p_452742_);
   }

   @Override
   public Vec3 getLeashOffset() {
      return new Vec3(0.0, 0.5F * this.getEyeHeight(), this.getBbWidth() * 0.4F);
   }

   @Override
   public boolean isSteppingCarefully() {
      return this.isCrouching() || super.isSteppingCarefully();
   }

   static class OcelotAvoidEntityGoal<T extends LivingEntity> extends AvoidEntityGoal<T> {
      private final Ocelot ocelot;

      public OcelotAvoidEntityGoal(Ocelot p_451267_, Class<T> p_456084_, float p_452149_, double p_458190_, double p_459265_) {
         super(p_451267_, p_456084_, p_452149_, p_458190_, p_459265_, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
         this.ocelot = p_451267_;
      }

      @Override
      public boolean canUse() {
         return !this.ocelot.isTrusting() && super.canUse();
      }

      @Override
      public boolean canContinueToUse() {
         return !this.ocelot.isTrusting() && super.canContinueToUse();
      }
   }

   static class OcelotTemptGoal extends TemptGoal {
      private final Ocelot ocelot;

      public OcelotTemptGoal(Ocelot p_456742_, double p_453434_, Predicate<ItemStack> p_452663_, boolean p_456177_) {
         super(p_456742_, p_453434_, p_452663_, p_456177_);
         this.ocelot = p_456742_;
      }

      @Override
      protected boolean canScare() {
         return super.canScare() && !this.ocelot.isTrusting();
      }
   }
}
