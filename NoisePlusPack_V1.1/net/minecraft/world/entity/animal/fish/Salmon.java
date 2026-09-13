package net.minecraft.world.entity.animal.fish;

import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class Salmon extends AbstractSchoolingFish {
   private static final String TAG_TYPE = "type";
   private static final EntityDataAccessor<Integer> DATA_TYPE = SynchedEntityData.defineId(Salmon.class, EntityDataSerializers.INT);

   public Salmon(EntityType<? extends Salmon> p_451549_, Level p_459343_) {
      super(p_451549_, p_459343_);
      this.refreshDimensions();
   }

   @Override
   public int getMaxSchoolSize() {
      return 5;
   }

   @Override
   public ItemStack getBucketItemStack() {
      return new ItemStack(Items.SALMON_BUCKET);
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.SALMON_AMBIENT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.SALMON_DEATH;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_461050_) {
      return SoundEvents.SALMON_HURT;
   }

   @Override
   protected SoundEvent getFlopSound() {
      return SoundEvents.SALMON_FLOP;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_454451_) {
      super.defineSynchedData(p_454451_);
      p_454451_.define(DATA_TYPE, Salmon.Variant.DEFAULT.id());
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> p_454002_) {
      super.onSyncedDataUpdated(p_454002_);
      if (DATA_TYPE.equals(p_454002_)) {
         this.refreshDimensions();
      }
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_458418_) {
      super.addAdditionalSaveData(p_458418_);
      p_458418_.store("type", Salmon.Variant.CODEC, this.getVariant());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_450324_) {
      super.readAdditionalSaveData(p_450324_);
      this.setVariant(p_450324_.<Salmon.Variant>read("type", Salmon.Variant.CODEC).orElse(Salmon.Variant.DEFAULT));
   }

   @Override
   public void saveToBucketTag(ItemStack p_454325_) {
      Bucketable.saveDefaultDataToBucketTag(this, p_454325_);
      p_454325_.copyFrom(DataComponents.SALMON_SIZE, this);
   }

   private void setVariant(Salmon.Variant p_451459_) {
      this.entityData.set(DATA_TYPE, p_451459_.id);
   }

   public Salmon.Variant getVariant() {
      return Salmon.Variant.BY_ID.apply(this.entityData.get(DATA_TYPE));
   }

   @Override
   public <T> @Nullable T get(DataComponentType<? extends T> p_457412_) {
      return p_457412_ == DataComponents.SALMON_SIZE ? castComponentValue((DataComponentType<T>)p_457412_, this.getVariant()) : super.get(p_457412_);
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_455413_) {
      this.applyImplicitComponentIfPresent(p_455413_, DataComponents.SALMON_SIZE);
      super.applyImplicitComponents(p_455413_);
   }

   @Override
   protected <T> boolean applyImplicitComponent(DataComponentType<T> p_460906_, T p_461079_) {
      if (p_460906_ == DataComponents.SALMON_SIZE) {
         this.setVariant(castComponentValue(DataComponents.SALMON_SIZE, p_461079_));
         return true;
      } else {
         return super.applyImplicitComponent(p_460906_, p_461079_);
      }
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_459412_, DifficultyInstance p_458114_, EntitySpawnReason p_459699_, @Nullable SpawnGroupData p_456095_
   ) {
      WeightedList.Builder<Salmon.Variant> builder = WeightedList.builder();
      builder.add(Salmon.Variant.SMALL, 30);
      builder.add(Salmon.Variant.MEDIUM, 50);
      builder.add(Salmon.Variant.LARGE, 15);
      builder.build().getRandom(this.random).ifPresent(this::setVariant);
      return super.finalizeSpawn(p_459412_, p_458114_, p_459699_, p_456095_);
   }

   public float getSalmonScale() {
      return this.getVariant().boundingBoxScale;
   }

   @Override
   protected EntityDimensions getDefaultDimensions(Pose p_451707_) {
      return super.getDefaultDimensions(p_451707_).scale(this.getSalmonScale());
   }

   public enum Variant implements StringRepresentable {
      SMALL("small", 0, 0.5F),
      MEDIUM("medium", 1, 1.0F),
      LARGE("large", 2, 1.5F);

      public static final Salmon.Variant DEFAULT = MEDIUM;
      public static final StringRepresentable.EnumCodec<Salmon.Variant> CODEC = StringRepresentable.fromEnum(Salmon.Variant::values);
      static final IntFunction<Salmon.Variant> BY_ID = ByIdMap.continuous(Salmon.Variant::id, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
      public static final StreamCodec<ByteBuf, Salmon.Variant> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Salmon.Variant::id);
      private final String name;
      final int id;
      final float boundingBoxScale;

      Variant(final String p_455456_, final int p_456977_, final float p_455649_) {
         this.name = p_455456_;
         this.id = p_456977_;
         this.boundingBoxScale = p_455649_;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      int id() {
         return this.id;
      }
   }
}
