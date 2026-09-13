package net.minecraft.world.entity.animal.sheep;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.jspecify.annotations.Nullable;

public class Sheep extends Animal implements Shearable {
   private static final int EAT_ANIMATION_TICKS = 40;
   private static final EntityDataAccessor<Byte> DATA_WOOL_ID = SynchedEntityData.defineId(Sheep.class, EntityDataSerializers.BYTE);
   private static final DyeColor DEFAULT_COLOR = DyeColor.WHITE;
   private static final boolean DEFAULT_SHEARED = false;
   private int eatAnimationTick;
   private EatBlockGoal eatBlockGoal;

   public Sheep(EntityType<? extends Sheep> p_394325_, Level p_393450_) {
      super(p_394325_, p_393450_);
   }

   @Override
   protected void registerGoals() {
      this.eatBlockGoal = new EatBlockGoal(this);
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new PanicGoal(this, 1.25));
      this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
      this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, p_395225_ -> p_395225_.is(ItemTags.SHEEP_FOOD), false));
      this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
      this.goalSelector.addGoal(5, this.eatBlockGoal);
      this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
      this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
   }

   @Override
   public boolean isFood(ItemStack p_397197_) {
      return p_397197_.is(ItemTags.SHEEP_FOOD);
   }

   @Override
   protected void customServerAiStep(ServerLevel p_393554_) {
      this.eatAnimationTick = this.eatBlockGoal.getEatAnimationTick();
      super.customServerAiStep(p_393554_);
   }

   @Override
   public void aiStep() {
      if (this.level().isClientSide()) {
         this.eatAnimationTick = Math.max(0, this.eatAnimationTick - 1);
      }

      super.aiStep();
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.23F);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_397053_) {
      super.defineSynchedData(p_397053_);
      p_397053_.define(DATA_WOOL_ID, (byte)0);
   }

   @Override
   public void handleEntityEvent(byte p_397414_) {
      if (p_397414_ == 10) {
         this.eatAnimationTick = 40;
      } else {
         super.handleEntityEvent(p_397414_);
      }
   }

   public float getHeadEatPositionScale(float p_396232_) {
      if (this.eatAnimationTick <= 0) {
         return 0.0F;
      } else if (this.eatAnimationTick >= 4 && this.eatAnimationTick <= 36) {
         return 1.0F;
      } else {
         return this.eatAnimationTick < 4 ? (this.eatAnimationTick - p_396232_) / 4.0F : -(this.eatAnimationTick - 40 - p_396232_) / 4.0F;
      }
   }

   public float getHeadEatAngleScale(float p_397037_) {
      if (this.eatAnimationTick > 4 && this.eatAnimationTick <= 36) {
         float f = (this.eatAnimationTick - 4 - p_397037_) / 32.0F;
         return (float) (Math.PI / 5) + 0.21991149F * Mth.sin(f * 28.7F);
      } else {
         return this.eatAnimationTick > 0 ? (float) (Math.PI / 5) : this.getXRot(p_397037_) * (float) (Math.PI / 180.0);
      }
   }

   @Override
   public InteractionResult mobInteract(Player p_397056_, InteractionHand p_391211_) {
      ItemStack itemstack = p_397056_.getItemInHand(p_391211_);
      if (itemstack.is(Items.SHEARS)) {
         if (this.level() instanceof ServerLevel serverlevel && this.readyForShearing()) {
            this.shear(serverlevel, SoundSource.PLAYERS, itemstack);
            this.gameEvent(GameEvent.SHEAR, p_397056_);
            itemstack.hurtAndBreak(1, p_397056_, p_391211_.asEquipmentSlot());
            return InteractionResult.SUCCESS_SERVER;
         } else {
            return InteractionResult.CONSUME;
         }
      } else {
         return super.mobInteract(p_397056_, p_391211_);
      }
   }

   @Override
   public void shear(ServerLevel p_397708_, SoundSource p_395553_, ItemStack p_396000_) {
      p_397708_.playSound(null, this, SoundEvents.SHEEP_SHEAR, p_395553_, 1.0F, 1.0F);
      this.dropFromShearingLootTable(
         p_397708_,
         BuiltInLootTables.SHEAR_SHEEP,
         p_396000_,
         (p_397779_, p_393350_) -> {
            for (int i = 0; i < p_393350_.getCount(); i++) {
               ItemEntity itementity = this.spawnAtLocation(p_397779_, p_393350_.copyWithCount(1), 1.0F);
               if (itementity != null) {
                  itementity.setDeltaMovement(
                     itementity.getDeltaMovement()
                        .add(
                           (this.random.nextFloat() - this.random.nextFloat()) * 0.1F,
                           this.random.nextFloat() * 0.05F,
                           (this.random.nextFloat() - this.random.nextFloat()) * 0.1F
                        )
                  );
               }
            }
         }
      );
      this.setSheared(true);
   }

   @Override
   public boolean readyForShearing() {
      return this.isAlive() && !this.isSheared() && !this.isBaby();
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_408777_) {
      super.addAdditionalSaveData(p_408777_);
      p_408777_.putBoolean("Sheared", this.isSheared());
      p_408777_.store("Color", DyeColor.LEGACY_ID_CODEC, this.getColor());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_409895_) {
      super.readAdditionalSaveData(p_409895_);
      this.setSheared(p_409895_.getBooleanOr("Sheared", false));
      this.setColor(p_409895_.<DyeColor>read("Color", DyeColor.LEGACY_ID_CODEC).orElse(DEFAULT_COLOR));
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.SHEEP_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_395295_) {
      return SoundEvents.SHEEP_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.SHEEP_DEATH;
   }

   @Override
   protected void playStepSound(BlockPos p_393951_, BlockState p_392498_) {
      this.playSound(SoundEvents.SHEEP_STEP, 0.15F, 1.0F);
   }

   public DyeColor getColor() {
      return DyeColor.byId(this.entityData.get(DATA_WOOL_ID) & 15);
   }

   public void setColor(DyeColor p_394530_) {
      byte b0 = this.entityData.get(DATA_WOOL_ID);
      this.entityData.set(DATA_WOOL_ID, (byte)(b0 & 240 | p_394530_.getId() & 15));
   }

   @Override
   public <T> @Nullable T get(DataComponentType<? extends T> p_392235_) {
      return p_392235_ == DataComponents.SHEEP_COLOR ? castComponentValue((DataComponentType<T>)p_392235_, this.getColor()) : super.get(p_392235_);
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_393376_) {
      this.applyImplicitComponentIfPresent(p_393376_, DataComponents.SHEEP_COLOR);
      super.applyImplicitComponents(p_393376_);
   }

   @Override
   protected <T> boolean applyImplicitComponent(DataComponentType<T> p_397580_, T p_395303_) {
      if (p_397580_ == DataComponents.SHEEP_COLOR) {
         this.setColor(castComponentValue(DataComponents.SHEEP_COLOR, p_395303_));
         return true;
      } else {
         return super.applyImplicitComponent(p_397580_, p_395303_);
      }
   }

   public boolean isSheared() {
      return (this.entityData.get(DATA_WOOL_ID) & 16) != 0;
   }

   public void setSheared(boolean p_397988_) {
      byte b0 = this.entityData.get(DATA_WOOL_ID);
      if (p_397988_) {
         this.entityData.set(DATA_WOOL_ID, (byte)(b0 | 16));
      } else {
         this.entityData.set(DATA_WOOL_ID, (byte)(b0 & -17));
      }
   }

   public static DyeColor getRandomSheepColor(ServerLevelAccessor p_396281_, BlockPos p_397425_) {
      Holder<Biome> holder = p_396281_.getBiome(p_397425_);
      return SheepColorSpawnRules.getSheepColor(holder, p_396281_.getRandom());
   }

   public @Nullable Sheep getBreedOffspring(ServerLevel p_393667_, AgeableMob p_391579_) {
      Sheep sheep = EntityType.SHEEP.create(p_393667_, EntitySpawnReason.BREEDING);
      if (sheep != null) {
         DyeColor dyecolor = this.getColor();
         DyeColor dyecolor1 = ((Sheep)p_391579_).getColor();
         sheep.setColor(DyeColor.getMixedColor(p_393667_, dyecolor, dyecolor1));
      }

      return sheep;
   }

   @Override
   public void ate() {
      super.ate();
      this.setSheared(false);
      if (this.isBaby()) {
         this.ageUp(60);
      }
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_397762_, DifficultyInstance p_391689_, EntitySpawnReason p_395930_, @Nullable SpawnGroupData p_392555_
   ) {
      this.setColor(getRandomSheepColor(p_397762_, this.blockPosition()));
      return super.finalizeSpawn(p_397762_, p_391689_, p_395930_, p_392555_);
   }
}
