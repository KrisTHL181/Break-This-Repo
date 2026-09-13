package net.minecraft.world.entity.animal.equine;

import java.util.function.DoubleSupplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ZombieHorse extends AbstractHorse {
   private static final float SPEED_FACTOR = 42.16F;
   private static final double BASE_JUMP_STRENGTH = 0.5;
   private static final double PER_RANDOM_JUMP_STRENGTH = 0.06666666666666667;
   private static final double BASE_SPEED = 9.0;
   private static final double PER_RANDOM_SPEED = 1.0;
   private static final EntityDimensions BABY_DIMENSIONS = EntityType.ZOMBIE_HORSE
      .getDimensions()
      .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, EntityType.ZOMBIE_HORSE.getHeight() - 0.03125F, 0.0F))
      .scale(0.5F);

   public ZombieHorse(EntityType<? extends ZombieHorse> p_453640_, Level p_453829_) {
      super(p_453640_, p_453829_);
      this.setPathfindingMalus(PathType.DANGER_OTHER, -1.0F);
      this.setPathfindingMalus(PathType.DAMAGE_OTHER, -1.0F);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return createBaseHorseAttributes().add(Attributes.MAX_HEALTH, 25.0);
   }

   @Override
   public InteractionResult interact(Player p_457345_, InteractionHand p_451894_) {
      this.setPersistenceRequired();
      return super.interact(p_457345_, p_451894_);
   }

   @Override
   public boolean removeWhenFarAway(double p_457691_) {
      return true;
   }

   @Override
   public boolean isMobControlled() {
      return this.getFirstPassenger() instanceof Mob;
   }

   @Override
   protected void randomizeAttributes(RandomSource p_457453_) {
      this.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(generateZombieHorseJumpStrength(p_457453_::nextDouble));
      this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(generateZombieHorseSpeed(p_457453_::nextDouble));
   }

   private static double generateZombieHorseJumpStrength(DoubleSupplier p_460522_) {
      return 0.5
         + p_460522_.getAsDouble() * 0.06666666666666667
         + p_460522_.getAsDouble() * 0.06666666666666667
         + p_460522_.getAsDouble() * 0.06666666666666667;
   }

   private static double generateZombieHorseSpeed(DoubleSupplier p_450664_) {
      return (9.0 + p_450664_.getAsDouble() * 1.0 + p_450664_.getAsDouble() * 1.0 + p_450664_.getAsDouble() * 1.0) / 42.16F;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.ZOMBIE_HORSE_AMBIENT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.ZOMBIE_HORSE_DEATH;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_460926_) {
      return SoundEvents.ZOMBIE_HORSE_HURT;
   }

   @Override
   protected SoundEvent getAngrySound() {
      return SoundEvents.ZOMBIE_HORSE_ANGRY;
   }

   @Override
   protected SoundEvent getEatingSound() {
      return SoundEvents.ZOMBIE_HORSE_EAT;
   }

   @Override
   public @Nullable AgeableMob getBreedOffspring(ServerLevel p_454469_, AgeableMob p_453799_) {
      return null;
   }

   @Override
   public boolean canFallInLove() {
      return false;
   }

   @Override
   protected void addBehaviourGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(3, new TemptGoal(this, 1.25, p_453910_ -> p_453910_.is(ItemTags.ZOMBIE_HORSE_FOOD), false));
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_456676_, DifficultyInstance p_451114_, EntitySpawnReason p_454151_, @Nullable SpawnGroupData p_451840_
   ) {
      if (p_454151_ == EntitySpawnReason.NATURAL) {
         Zombie zombie = EntityType.ZOMBIE.create(this.level(), EntitySpawnReason.JOCKEY);
         if (zombie != null) {
            zombie.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
            zombie.finalizeSpawn(p_456676_, p_451114_, p_454151_, null);
            zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SPEAR));
            zombie.startRiding(this, false, false);
         }
      }

      return super.finalizeSpawn(p_456676_, p_451114_, p_454151_, p_451840_);
   }

   @Override
   public InteractionResult mobInteract(Player p_459296_, InteractionHand p_457303_) {
      boolean flag = !this.isBaby() && this.isTamed() && p_459296_.isSecondaryUseActive();
      if (!this.isVehicle() && !flag) {
         ItemStack itemstack = p_459296_.getItemInHand(p_457303_);
         if (!itemstack.isEmpty()) {
            if (this.isFood(itemstack)) {
               return this.fedFood(p_459296_, itemstack);
            }

            if (!this.isTamed()) {
               this.makeMad();
               return InteractionResult.SUCCESS;
            }
         }

         return super.mobInteract(p_459296_, p_457303_);
      } else {
         return super.mobInteract(p_459296_, p_457303_);
      }
   }

   @Override
   public boolean canUseSlot(EquipmentSlot p_457843_) {
      return true;
   }

   @Override
   public boolean canBeLeashed() {
      return this.isTamed() || !this.isMobControlled();
   }

   @Override
   public boolean isFood(ItemStack p_459077_) {
      return p_459077_.is(ItemTags.ZOMBIE_HORSE_FOOD);
   }

   @Override
   protected EquipmentSlot sunProtectionSlot() {
      return EquipmentSlot.BODY;
   }

   @Override
   public Vec3[] getQuadLeashOffsets() {
      return Leashable.createQuadLeashOffsets(this, 0.04, 0.41, 0.18, 0.73);
   }

   @Override
   public EntityDimensions getDefaultDimensions(Pose p_453061_) {
      return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(p_453061_);
   }

   @Override
   public float chargeSpeedModifier() {
      return 1.4F;
   }
}
