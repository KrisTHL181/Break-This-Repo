package net.minecraft.world.entity.animal.cow;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
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

   public Cow(EntityType<? extends Cow> p_451234_, Level p_453544_) {
      super(p_451234_, p_453544_);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_450423_) {
      super.defineSynchedData(p_450423_);
      p_450423_.define(DATA_VARIANT_ID, VariantUtils.getDefaultOrAny(this.registryAccess(), CowVariants.TEMPERATE));
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_456867_) {
      super.addAdditionalSaveData(p_456867_);
      VariantUtils.writeVariant(p_456867_, this.getVariant());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_451164_) {
      super.readAdditionalSaveData(p_451164_);
      VariantUtils.readVariant(p_451164_, Registries.COW_VARIANT).ifPresent(this::setVariant);
   }

   public @Nullable Cow getBreedOffspring(ServerLevel p_457056_, AgeableMob p_453337_) {
      Cow cow = EntityType.COW.create(p_457056_, EntitySpawnReason.BREEDING);
      if (cow != null && p_453337_ instanceof Cow cow1) {
         cow.setVariant(this.random.nextBoolean() ? this.getVariant() : cow1.getVariant());
      }

      return cow;
   }

   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_456110_, DifficultyInstance p_451480_, EntitySpawnReason p_450581_, @Nullable SpawnGroupData p_457604_
   ) {
      VariantUtils.selectVariantToSpawn(SpawnContext.create(p_456110_, this.blockPosition()), Registries.COW_VARIANT).ifPresent(this::setVariant);
      return super.finalizeSpawn(p_456110_, p_451480_, p_450581_, p_457604_);
   }

   public void setVariant(Holder<CowVariant> p_458158_) {
      this.entityData.set(DATA_VARIANT_ID, p_458158_);
   }

   public Holder<CowVariant> getVariant() {
      return this.entityData.get(DATA_VARIANT_ID);
   }

   @Override
   public <T> @Nullable T get(DataComponentType<? extends T> p_459836_) {
      return p_459836_ == DataComponents.COW_VARIANT ? castComponentValue((DataComponentType<T>)p_459836_, this.getVariant()) : super.get(p_459836_);
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_455152_) {
      this.applyImplicitComponentIfPresent(p_455152_, DataComponents.COW_VARIANT);
      super.applyImplicitComponents(p_455152_);
   }

   @Override
   protected <T> boolean applyImplicitComponent(DataComponentType<T> p_454606_, T p_453925_) {
      if (p_454606_ == DataComponents.COW_VARIANT) {
         this.setVariant(castComponentValue(DataComponents.COW_VARIANT, p_453925_));
         return true;
      } else {
         return super.applyImplicitComponent(p_454606_, p_453925_);
      }
   }
}
