package net.minecraft.world.entity.animal.nautilus;

import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.variant.SpawnContext;
import net.minecraft.world.entity.variant.VariantUtils;
import net.minecraft.world.item.EitherHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class ZombieNautilus extends AbstractNautilus {
   private static final EntityDataAccessor<Holder<ZombieNautilusVariant>> DATA_VARIANT_ID = SynchedEntityData.defineId(
      ZombieNautilus.class, EntityDataSerializers.ZOMBIE_NAUTILUS_VARIANT
   );

   public ZombieNautilus(EntityType<? extends ZombieNautilus> p_457129_, Level p_452024_) {
      super(p_457129_, p_452024_);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return AbstractNautilus.createAttributes().add(Attributes.MOVEMENT_SPEED, 1.1F);
   }

   public @Nullable ZombieNautilus getBreedOffspring(ServerLevel p_456201_, AgeableMob p_455778_) {
      return null;
   }

   @Override
   protected EquipmentSlot sunProtectionSlot() {
      return EquipmentSlot.BODY;
   }

   @Override
   protected Brain.Provider<ZombieNautilus> brainProvider() {
      return ZombieNautilusAi.brainProvider();
   }

   @Override
   protected Brain<?> makeBrain(Dynamic<?> p_453332_) {
      return ZombieNautilusAi.makeBrain(this.brainProvider().makeBrain(p_453332_));
   }

   @Override
   public Brain<ZombieNautilus> getBrain() {
      return (Brain<ZombieNautilus>)super.getBrain();
   }

   @Override
   protected void customServerAiStep(ServerLevel p_455575_) {
      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("zombieNautilusBrain");
      this.getBrain().tick(p_455575_, this);
      profilerfiller.pop();
      profilerfiller.push("zombieNautilusActivityUpdate");
      ZombieNautilusAi.updateActivity(this);
      profilerfiller.pop();
      super.customServerAiStep(p_455575_);
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return this.isUnderWater() ? SoundEvents.ZOMBIE_NAUTILUS_AMBIENT : SoundEvents.ZOMBIE_NAUTILUS_AMBIENT_ON_LAND;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_454717_) {
      return this.isUnderWater() ? SoundEvents.ZOMBIE_NAUTILUS_HURT : SoundEvents.ZOMBIE_NAUTILUS_HURT_ON_LAND;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return this.isUnderWater() ? SoundEvents.ZOMBIE_NAUTILUS_DEATH : SoundEvents.ZOMBIE_NAUTILUS_DEATH_ON_LAND;
   }

   @Override
   protected SoundEvent getDashSound() {
      return this.isUnderWater() ? SoundEvents.ZOMBIE_NAUTILUS_DASH : SoundEvents.ZOMBIE_NAUTILUS_DASH_ON_LAND;
   }

   @Override
   protected SoundEvent getDashReadySound() {
      return this.isUnderWater() ? SoundEvents.ZOMBIE_NAUTILUS_DASH_READY : SoundEvents.ZOMBIE_NAUTILUS_DASH_READY_ON_LAND;
   }

   @Override
   protected void playEatingSound() {
      this.makeSound(SoundEvents.ZOMBIE_NAUTILUS_EAT);
   }

   @Override
   protected SoundEvent getSwimSound() {
      return SoundEvents.ZOMBIE_NAUTILUS_SWIM;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_450929_) {
      super.defineSynchedData(p_450929_);
      p_450929_.define(DATA_VARIANT_ID, VariantUtils.getDefaultOrAny(this.registryAccess(), ZombieNautilusVariants.TEMPERATE));
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_460301_) {
      super.readAdditionalSaveData(p_460301_);
      VariantUtils.readVariant(p_460301_, Registries.ZOMBIE_NAUTILUS_VARIANT).ifPresent(this::setVariant);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_460414_) {
      super.addAdditionalSaveData(p_460414_);
      VariantUtils.writeVariant(p_460414_, this.getVariant());
   }

   public void setVariant(Holder<ZombieNautilusVariant> p_452334_) {
      this.entityData.set(DATA_VARIANT_ID, p_452334_);
   }

   public Holder<ZombieNautilusVariant> getVariant() {
      return this.entityData.get(DATA_VARIANT_ID);
   }

   @Override
   public <T> @Nullable T get(DataComponentType<? extends T> p_451258_) {
      return p_451258_ == DataComponents.ZOMBIE_NAUTILUS_VARIANT
         ? castComponentValue((DataComponentType<T>)p_451258_, new EitherHolder<>(this.getVariant()))
         : super.get(p_451258_);
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_455795_) {
      this.applyImplicitComponentIfPresent(p_455795_, DataComponents.ZOMBIE_NAUTILUS_VARIANT);
      super.applyImplicitComponents(p_455795_);
   }

   @Override
   protected <T> boolean applyImplicitComponent(DataComponentType<T> p_453765_, T p_457488_) {
      if (p_453765_ == DataComponents.ZOMBIE_NAUTILUS_VARIANT) {
         Optional<Holder<ZombieNautilusVariant>> optional = castComponentValue(DataComponents.ZOMBIE_NAUTILUS_VARIANT, p_457488_).unwrap(this.registryAccess());
         if (optional.isPresent()) {
            this.setVariant(optional.get());
            return true;
         } else {
            return false;
         }
      } else {
         return super.applyImplicitComponent(p_453765_, p_457488_);
      }
   }

   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_458159_, DifficultyInstance p_458323_, EntitySpawnReason p_455232_, @Nullable SpawnGroupData p_454792_
   ) {
      VariantUtils.selectVariantToSpawn(SpawnContext.create(p_458159_, this.blockPosition()), Registries.ZOMBIE_NAUTILUS_VARIANT).ifPresent(this::setVariant);
      return super.finalizeSpawn(p_458159_, p_458323_, p_455232_, p_454792_);
   }

   @Override
   public boolean canBeLeashed() {
      return !this.isAggravated() && !this.isMobControlled();
   }

   @Override
   public boolean isBaby() {
      return false;
   }
}
