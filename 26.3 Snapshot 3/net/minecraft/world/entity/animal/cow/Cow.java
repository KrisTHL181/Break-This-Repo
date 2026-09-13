package net.minecraft.world.entity.animal.cow;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.variant.SpawnContext;
import net.minecraft.world.entity.variant.VariantUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class Cow extends AbstractCow {
   private static final EntityDataAccessor<Holder<CowVariant>> DATA_VARIANT_ID = SynchedEntityData.defineId(Cow.class, EntityDataSerializers.COW_VARIANT);
   private static final EntityDataAccessor<Holder<CowSoundVariant>> DATA_SOUND_VARIANT_ID = SynchedEntityData.defineId(
      Cow.class, EntityDataSerializers.COW_SOUND_VARIANT
   );
   private static final EntityDimensions BABY_DIMENSIONS = EntityDimensions.scalable(0.45F, 0.7F)
      .withEyeHeight(0.69F)
      .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, 0.75F, 0.0F));

   public Cow(final EntityType<? extends Cow> type, final Level level) {
      super(type, level);
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      Registry<CowSoundVariant> cowSoundVariants = this.registryAccess().lookupOrThrow(Registries.COW_SOUND_VARIANT);
      entityData.define(DATA_VARIANT_ID, VariantUtils.getDefaultOrAny(this.registryAccess(), CowVariants.TEMPERATE));
      entityData.define(DATA_SOUND_VARIANT_ID, cowSoundVariants.get(CowSoundVariants.CLASSIC).or(cowSoundVariants::getAny).orElseThrow());
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      super.addAdditionalSaveData(output);
      VariantUtils.writeVariant(output, this.getVariant());
      this.getSoundVariant()
         .unwrapKey()
         .ifPresent(soundVariant -> output.store("sound_variant", ResourceKey.codec(Registries.COW_SOUND_VARIANT), soundVariant));
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      VariantUtils.readVariant(input, Registries.COW_VARIANT).ifPresent(this::setVariant);
      input.<ResourceKey>read("sound_variant", ResourceKey.codec(Registries.COW_SOUND_VARIANT))
         .flatMap(soundVariant -> this.registryAccess().lookupOrThrow(Registries.COW_SOUND_VARIANT).get((ResourceKey<CowSoundVariant>)soundVariant))
         .ifPresent(this::setSoundVariant);
   }

   public @Nullable Cow getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
      Cow baby = EntityTypes.COW.create(level, EntitySpawnReason.BREEDING);
      if (baby != null && partner instanceof Cow partnerCow) {
         baby.setVariant(this.random.nextBoolean() ? this.getVariant() : partnerCow.getVariant());
      }

      return baby;
   }

   @Override
   public SpawnGroupData finalizeSpawn(
      final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, final @Nullable SpawnGroupData groupData
   ) {
      VariantUtils.selectVariantToSpawn(SpawnContext.create(level, this.blockPosition()), Registries.COW_VARIANT).ifPresent(this::setVariant);
      this.setSoundVariant(CowSoundVariants.pickRandomSoundVariant(this.registryAccess(), level.getRandom()));
      return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
   }

   public void setVariant(final Holder<CowVariant> variant) {
      this.entityData.set(DATA_VARIANT_ID, variant);
   }

   public Holder<CowVariant> getVariant() {
      return this.entityData.get(DATA_VARIANT_ID);
   }

   private Holder<CowSoundVariant> getSoundVariant() {
      return this.entityData.get(DATA_SOUND_VARIANT_ID);
   }

   private void setSoundVariant(final Holder<CowSoundVariant> soundVariant) {
      this.entityData.set(DATA_SOUND_VARIANT_ID, soundVariant);
   }

   @Override
   protected CowSoundVariant getSoundSet() {
      return this.getSoundVariant().value();
   }

   @Override
   public EntityDimensions getDefaultDimensions(final Pose pose) {
      return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
   }

   @Override
   public <T> @Nullable T get(final DataComponentType<? extends T> type) {
      if (type == DataComponents.COW_VARIANT) {
         return castComponentValue((DataComponentType<T>)type, this.getVariant());
      } else {
         return type == DataComponents.COW_SOUND_VARIANT ? castComponentValue((DataComponentType<T>)type, this.getSoundVariant()) : super.get(type);
      }
   }

   @Override
   protected void applyImplicitComponents(final DataComponentGetter components) {
      this.applyImplicitComponentIfPresent(components, DataComponents.COW_VARIANT);
      this.applyImplicitComponentIfPresent(components, DataComponents.COW_SOUND_VARIANT);
      super.applyImplicitComponents(components);
   }

   @Override
   protected <T> boolean applyImplicitComponent(final DataComponentType<T> type, final T value) {
      if (type == DataComponents.COW_VARIANT) {
         this.setVariant(castComponentValue(DataComponents.COW_VARIANT, value));
         return true;
      } else if (type == DataComponents.COW_SOUND_VARIANT) {
         this.setSoundVariant(castComponentValue(DataComponents.COW_SOUND_VARIANT, value));
         return true;
      } else {
         return super.applyImplicitComponent(type, value);
      }
   }
}
