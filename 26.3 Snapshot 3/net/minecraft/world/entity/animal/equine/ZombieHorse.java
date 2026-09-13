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
import net.minecraft.world.entity.EntityTypes;
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
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ZombieHorse extends AbstractHorse {
   private static final float SPEED_FACTOR = 42.16F;
   private static final double BASE_JUMP_STRENGTH = 0.5;
   private static final double PER_RANDOM_JUMP_STRENGTH = 0.06666666666666667;
   private static final double BASE_SPEED = 9.0;
   private static final double PER_RANDOM_SPEED = 1.0;
   private static final EntityDimensions BABY_DIMENSIONS = EntityTypes.ZOMBIE_HORSE
      .getDimensions()
      .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, EntityTypes.ZOMBIE_HORSE.getHeight() - 0.25F, 0.0F))
      .scale(0.7F);

   public ZombieHorse(final EntityType<? extends ZombieHorse> type, final Level level) {
      super(type, level);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return createBaseHorseAttributes().add(Attributes.MAX_HEALTH, 25.0);
   }

   @Override
   public InteractionResult interact(final Player player, final InteractionHand hand, final Vec3 location) {
      this.setPersistenceRequired();
      return super.interact(player, hand, location);
   }

   @Override
   public boolean removeWhenFarAway(final double distSqr) {
      return true;
   }

   @Override
   public boolean isMobControlled() {
      return this.getFirstPassenger() instanceof Mob;
   }

   @Override
   protected void randomizeAttributes(final RandomSource random) {
      this.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(generateZombieHorseJumpStrength(random::nextDouble));
      this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(generateZombieHorseSpeed(random::nextDouble));
   }

   private static double generateZombieHorseJumpStrength(final DoubleSupplier probabilityProvider) {
      return 0.5
         + probabilityProvider.getAsDouble() * 0.06666666666666667
         + probabilityProvider.getAsDouble() * 0.06666666666666667
         + probabilityProvider.getAsDouble() * 0.06666666666666667;
   }

   private static double generateZombieHorseSpeed(final DoubleSupplier probabilityProvider) {
      return (9.0 + probabilityProvider.getAsDouble() * 1.0 + probabilityProvider.getAsDouble() * 1.0 + probabilityProvider.getAsDouble() * 1.0) / 42.16F;
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
   protected SoundEvent getHurtSound(final DamageSource source) {
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
   public @Nullable AgeableMob getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
      return EntityTypes.ZOMBIE_HORSE.create(level, EntitySpawnReason.BREEDING);
   }

   @Override
   public boolean canFallInLove() {
      return false;
   }

   @Override
   protected void addBehaviourGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(3, new TemptGoal(this, 1.25, i -> i.is(ItemTags.ZOMBIE_HORSE_FOOD), false));
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, final @Nullable SpawnGroupData groupData
   ) {
      if (spawnReason == EntitySpawnReason.NATURAL) {
         Zombie zombie = EntityTypes.ZOMBIE.create(this.level(), EntitySpawnReason.JOCKEY);
         if (zombie != null) {
            zombie.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
            zombie.finalizeSpawn(level, difficulty, spawnReason, null);
            zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SPEAR));
            zombie.startRiding(this, false, false);
         }
      }

      return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
   }

   @Override
   public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
      boolean shouldOpenInventory = !this.isBaby() && this.isTamed() && player.isSecondaryUseActive();
      if (!this.isVehicle() && !shouldOpenInventory) {
         ItemStack itemStack = player.getItemInHand(hand);
         if (!itemStack.isEmpty()) {
            if (this.isFood(itemStack)) {
               return this.fedFood(player, itemStack);
            }

            if (!this.isTamed()) {
               this.makeMad();
               return InteractionResult.SUCCESS;
            }
         }

         return super.mobInteract(player, hand);
      } else {
         return super.mobInteract(player, hand);
      }
   }

   @Override
   public boolean canUseSlot(final EquipmentSlot slot) {
      return true;
   }

   @Override
   public boolean canBeLeashed() {
      return this.isTamed() || !this.isMobControlled();
   }

   @Override
   public boolean isFood(final ItemStack itemStack) {
      return itemStack.is(ItemTags.ZOMBIE_HORSE_FOOD);
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
   public EntityDimensions getDefaultDimensions(final Pose pose) {
      return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
   }

   @Override
   public float chargeSpeedModifier() {
      return 1.4F;
   }

   @Override
   public boolean canAgeUp() {
      return false;
   }
}
