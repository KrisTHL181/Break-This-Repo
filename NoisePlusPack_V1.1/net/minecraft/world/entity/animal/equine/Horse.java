package net.minecraft.world.entity.animal.equine;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
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
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class Horse extends AbstractHorse {
   private static final EntityDataAccessor<Integer> DATA_ID_TYPE_VARIANT = SynchedEntityData.defineId(Horse.class, EntityDataSerializers.INT);
   private static final EntityDimensions BABY_DIMENSIONS = EntityType.HORSE
      .getDimensions()
      .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, EntityType.HORSE.getHeight() + 0.125F, 0.0F))
      .scale(0.5F);
   private static final int DEFAULT_VARIANT = 0;

   public Horse(EntityType<? extends Horse> p_458271_, Level p_452236_) {
      super(p_458271_, p_452236_);
      this.setPathfindingMalus(PathType.DANGER_OTHER, -1.0F);
      this.setPathfindingMalus(PathType.DAMAGE_OTHER, -1.0F);
   }

   @Override
   protected void randomizeAttributes(RandomSource p_459630_) {
      this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(generateMaxHealth(p_459630_::nextInt));
      this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(generateSpeed(p_459630_::nextDouble));
      this.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(generateJumpStrength(p_459630_::nextDouble));
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_453967_) {
      super.defineSynchedData(p_453967_);
      p_453967_.define(DATA_ID_TYPE_VARIANT, 0);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_456830_) {
      super.addAdditionalSaveData(p_456830_);
      p_456830_.putInt("Variant", this.getTypeVariant());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_450935_) {
      super.readAdditionalSaveData(p_450935_);
      this.setTypeVariant(p_450935_.getIntOr("Variant", 0));
   }

   private void setTypeVariant(int p_451856_) {
      this.entityData.set(DATA_ID_TYPE_VARIANT, p_451856_);
   }

   private int getTypeVariant() {
      return this.entityData.get(DATA_ID_TYPE_VARIANT);
   }

   private void setVariantAndMarkings(Variant p_458984_, Markings p_452966_) {
      this.setTypeVariant(p_458984_.getId() & 0xFF | p_452966_.getId() << 8 & 0xFF00);
   }

   public Variant getVariant() {
      return Variant.byId(this.getTypeVariant() & 0xFF);
   }

   private void setVariant(Variant p_457220_) {
      this.setTypeVariant(p_457220_.getId() & 0xFF | this.getTypeVariant() & -256);
   }

   @Override
   public <T> @Nullable T get(DataComponentType<? extends T> p_450226_) {
      return p_450226_ == DataComponents.HORSE_VARIANT ? castComponentValue((DataComponentType<T>)p_450226_, this.getVariant()) : super.get(p_450226_);
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_450557_) {
      this.applyImplicitComponentIfPresent(p_450557_, DataComponents.HORSE_VARIANT);
      super.applyImplicitComponents(p_450557_);
   }

   @Override
   protected <T> boolean applyImplicitComponent(DataComponentType<T> p_457671_, T p_460202_) {
      if (p_457671_ == DataComponents.HORSE_VARIANT) {
         this.setVariant(castComponentValue(DataComponents.HORSE_VARIANT, p_460202_));
         return true;
      } else {
         return super.applyImplicitComponent(p_457671_, p_460202_);
      }
   }

   public Markings getMarkings() {
      return Markings.byId((this.getTypeVariant() & 0xFF00) >> 8);
   }

   @Override
   protected void playGallopSound(SoundType p_460978_) {
      super.playGallopSound(p_460978_);
      if (this.random.nextInt(10) == 0) {
         this.playSound(SoundEvents.HORSE_BREATHE, p_460978_.getVolume() * 0.6F, p_460978_.getPitch());
      }
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.HORSE_AMBIENT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.HORSE_DEATH;
   }

   @Override
   protected SoundEvent getEatingSound() {
      return SoundEvents.HORSE_EAT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_456189_) {
      return SoundEvents.HORSE_HURT;
   }

   @Override
   protected SoundEvent getAngrySound() {
      return SoundEvents.HORSE_ANGRY;
   }

   @Override
   public InteractionResult mobInteract(Player p_451801_, InteractionHand p_453106_) {
      boolean flag = !this.isBaby() && this.isTamed() && p_451801_.isSecondaryUseActive();
      if (!this.isVehicle() && !flag) {
         ItemStack itemstack = p_451801_.getItemInHand(p_453106_);
         if (!itemstack.isEmpty()) {
            if (this.isFood(itemstack)) {
               return this.fedFood(p_451801_, itemstack);
            }

            if (!this.isTamed()) {
               this.makeMad();
               return InteractionResult.SUCCESS;
            }
         }

         return super.mobInteract(p_451801_, p_453106_);
      } else {
         return super.mobInteract(p_451801_, p_453106_);
      }
   }

   @Override
   public boolean canMate(Animal p_459082_) {
      if (p_459082_ == this) {
         return false;
      } else {
         return !(p_459082_ instanceof Donkey) && !(p_459082_ instanceof Horse) ? false : this.canParent() && ((AbstractHorse)p_459082_).canParent();
      }
   }

   @Override
   public @Nullable AgeableMob getBreedOffspring(ServerLevel p_460751_, AgeableMob p_451439_) {
      if (p_451439_ instanceof Donkey) {
         Mule mule = EntityType.MULE.create(p_460751_, EntitySpawnReason.BREEDING);
         if (mule != null) {
            this.setOffspringAttributes(p_451439_, mule);
         }

         return mule;
      } else {
         Horse horse = (Horse)p_451439_;
         Horse horse1 = EntityType.HORSE.create(p_460751_, EntitySpawnReason.BREEDING);
         if (horse1 != null) {
            int i = this.random.nextInt(9);
            Variant variant;
            if (i < 4) {
               variant = this.getVariant();
            } else if (i < 8) {
               variant = horse.getVariant();
            } else {
               variant = Util.getRandom(Variant.values(), this.random);
            }

            int j = this.random.nextInt(5);
            Markings markings;
            if (j < 2) {
               markings = this.getMarkings();
            } else if (j < 4) {
               markings = horse.getMarkings();
            } else {
               markings = Util.getRandom(Markings.values(), this.random);
            }

            horse1.setVariantAndMarkings(variant, markings);
            this.setOffspringAttributes(p_451439_, horse1);
         }

         return horse1;
      }
   }

   @Override
   public boolean canUseSlot(EquipmentSlot p_453173_) {
      return true;
   }

   @Override
   protected void hurtArmor(DamageSource p_455276_, float p_457436_) {
      this.doHurtEquipment(p_455276_, p_457436_, EquipmentSlot.BODY);
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_452550_, DifficultyInstance p_450368_, EntitySpawnReason p_460449_, @Nullable SpawnGroupData p_450993_
   ) {
      RandomSource randomsource = p_452550_.getRandom();
      Variant variant;
      if (p_450993_ instanceof Horse.HorseGroupData) {
         variant = ((Horse.HorseGroupData)p_450993_).variant;
      } else {
         variant = Util.getRandom(Variant.values(), randomsource);
         p_450993_ = new Horse.HorseGroupData(variant);
      }

      this.setVariantAndMarkings(variant, Util.getRandom(Markings.values(), randomsource));
      return super.finalizeSpawn(p_452550_, p_450368_, p_460449_, p_450993_);
   }

   @Override
   public EntityDimensions getDefaultDimensions(Pose p_451537_) {
      return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(p_451537_);
   }

   public static class HorseGroupData extends AgeableMob.AgeableMobGroupData {
      public final Variant variant;

      public HorseGroupData(Variant p_458631_) {
         super(true);
         this.variant = p_458631_;
      }
   }
}
