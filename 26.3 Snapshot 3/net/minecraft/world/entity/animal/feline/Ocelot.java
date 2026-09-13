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
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
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
   private static final EntityDimensions BABY_DIMENSIONS = EntityDimensions.scalable(0.3F, 0.35F)
      .withEyeHeight(0.34375F)
      .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, 0.3125F, 0.0F));
   private Ocelot.@Nullable OcelotAvoidEntityGoal<Player> ocelotAvoidPlayersGoal;
   private Ocelot.@Nullable OcelotTemptGoal temptGoal;

   public Ocelot(final EntityType<? extends Ocelot> type, final Level level) {
      super(type, level);
      this.reassessTrustingGoals();
   }

   private boolean isTrusting() {
      return this.entityData.get(DATA_TRUSTING);
   }

   private void setTrusting(final boolean trusting) {
      this.entityData.set(DATA_TRUSTING, trusting);
      this.reassessTrustingGoals();
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putBoolean("Trusting", this.isTrusting());
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.setTrusting(input.getBooleanOr("Trusting", false));
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(DATA_TRUSTING, false);
   }

   @Override
   protected void registerGoals() {
      this.temptGoal = new Ocelot.OcelotTemptGoal(this, 0.6, i -> i.is(ItemTags.OCELOT_FOOD), true);
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
   public void customServerAiStep(final ServerLevel level) {
      if (this.getMoveControl().hasWanted()) {
         double speed = this.getMoveControl().getSpeedModifier();
         if (speed == 0.6) {
            this.setPose(Pose.CROUCHING);
            this.setSprinting(false);
         } else if (speed == 1.33) {
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
   public boolean removeWhenFarAway(final double distSqr) {
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
   protected SoundEvent getHurtSound(final DamageSource source) {
      return SoundEvents.OCELOT_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.OCELOT_DEATH;
   }

   @Override
   public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      if ((this.temptGoal == null || this.temptGoal.isRunning()) && !this.isTrusting() && this.isFood(itemStack) && player.distanceToSqr(this) < 9.0) {
         this.usePlayerItem(player, hand, itemStack);
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
         return super.mobInteract(player, hand);
      }
   }

   @Override
   public void handleEntityEvent(final byte id) {
      if (id == 41) {
         this.spawnTrustingParticles(true);
      } else if (id == 40) {
         this.spawnTrustingParticles(false);
      } else {
         super.handleEntityEvent(id);
      }
   }

   private void spawnTrustingParticles(final boolean success) {
      ParticleOptions particle = ParticleTypes.HEART;
      if (!success) {
         particle = ParticleTypes.SMOKE;
      }

      for (int i = 0; i < 7; i++) {
         double xa = this.random.nextGaussian() * 0.02;
         double ya = this.random.nextGaussian() * 0.02;
         double za = this.random.nextGaussian() * 0.02;
         this.level().addParticle(particle, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), xa, ya, za);
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

   @Override
   public EntityDimensions getDefaultDimensions(final Pose pose) {
      return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
   }

   public @Nullable Ocelot getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
      return EntityTypes.OCELOT.create(level, EntitySpawnReason.BREEDING);
   }

   @Override
   public boolean isFood(final ItemStack itemStack) {
      return itemStack.is(ItemTags.OCELOT_FOOD);
   }

   public static boolean checkOcelotSpawnRules(
      final EntityType<Ocelot> type, final LevelAccessor level, final EntitySpawnReason spawnReason, final BlockPos pos, final RandomSource random
   ) {
      return random.nextInt(3) != 0;
   }

   @Override
   public boolean checkSpawnObstruction(final LevelReader level) {
      if (level.isUnobstructed(this) && !level.containsAnyLiquid(this.getBoundingBox())) {
         BlockPos pos = this.blockPosition();
         if (pos.getY() < level.getSeaLevel()) {
            return false;
         }

         BlockState state = level.getBlockState(pos.below());
         if (state.is(Blocks.GRASS_BLOCK) || state.is(BlockTags.LEAVES)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData
   ) {
      if (groupData == null) {
         groupData = new AgeableMob.AgeableMobGroupData(1.0F);
      }

      return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
   }

   @Override
   public Vec3 getLeashOffset() {
      return new Vec3(0.0, 0.5F * this.getEyeHeight(), this.getBbWidth() * 0.4F);
   }

   @Override
   public boolean isSteppingCarefully() {
      return this.isCrouching() || super.isSteppingCarefully();
   }

   private static class OcelotAvoidEntityGoal<T extends LivingEntity> extends AvoidEntityGoal<T> {
      private final Ocelot ocelot;

      public OcelotAvoidEntityGoal(
         final Ocelot ocelot, final Class<T> avoidClass, final float maxDist, final double walkSpeedModifier, final double sprintSpeedModifier
      ) {
         super(ocelot, avoidClass, maxDist, walkSpeedModifier, sprintSpeedModifier, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
         this.ocelot = ocelot;
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

   private static class OcelotTemptGoal extends TemptGoal {
      private final Ocelot ocelot;

      public OcelotTemptGoal(final Ocelot ocelot, final double speedModifier, final Predicate<ItemStack> items, final boolean canScare) {
         super(ocelot, speedModifier, items, canScare);
         this.ocelot = ocelot;
      }

      @Override
      protected boolean canScare() {
         return super.canScare() && !this.ocelot.isTrusting();
      }
   }
}
