package net.minecraft.world.entity.animal.pig;

import net.minecraft.core.BlockPos;
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
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ItemBasedSteering;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.variant.SpawnContext;
import net.minecraft.world.entity.variant.VariantUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Pig extends Animal implements ItemSteerable {
   private static final EntityDataAccessor<Integer> DATA_BOOST_TIME = SynchedEntityData.defineId(Pig.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Holder<PigVariant>> DATA_VARIANT_ID = SynchedEntityData.defineId(Pig.class, EntityDataSerializers.PIG_VARIANT);
   private final ItemBasedSteering steering = new ItemBasedSteering(this.entityData, DATA_BOOST_TIME);

   public Pig(EntityType<? extends Pig> p_457021_, Level p_460038_) {
      super(p_457021_, p_460038_);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new PanicGoal(this, 1.25));
      this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
      this.goalSelector.addGoal(4, new TemptGoal(this, 1.2, p_453748_ -> p_453748_.is(Items.CARROT_ON_A_STICK), false));
      this.goalSelector.addGoal(4, new TemptGoal(this, 1.2, p_460021_ -> p_460021_.is(ItemTags.PIG_FOOD), false));
      this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
      this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
      this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.25);
   }

   @Override
   public @Nullable LivingEntity getControllingPassenger() {
      return this.isSaddled() && this.getFirstPassenger() instanceof Player player && player.isHolding(Items.CARROT_ON_A_STICK)
         ? player
         : super.getControllingPassenger();
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> p_455511_) {
      if (DATA_BOOST_TIME.equals(p_455511_) && this.level().isClientSide()) {
         this.steering.onSynced();
      }

      super.onSyncedDataUpdated(p_455511_);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_454847_) {
      super.defineSynchedData(p_454847_);
      p_454847_.define(DATA_BOOST_TIME, 0);
      p_454847_.define(DATA_VARIANT_ID, VariantUtils.getDefaultOrAny(this.registryAccess(), PigVariants.DEFAULT));
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_455183_) {
      super.addAdditionalSaveData(p_455183_);
      VariantUtils.writeVariant(p_455183_, this.getVariant());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_454547_) {
      super.readAdditionalSaveData(p_454547_);
      VariantUtils.readVariant(p_454547_, Registries.PIG_VARIANT).ifPresent(this::setVariant);
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.PIG_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_460687_) {
      return SoundEvents.PIG_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.PIG_DEATH;
   }

   @Override
   protected void playStepSound(BlockPos p_450547_, BlockState p_451548_) {
      this.playSound(SoundEvents.PIG_STEP, 0.15F, 1.0F);
   }

   @Override
   public InteractionResult mobInteract(Player p_458345_, InteractionHand p_459637_) {
      boolean flag = this.isFood(p_458345_.getItemInHand(p_459637_));
      if (!flag && this.isSaddled() && !this.isVehicle() && !p_458345_.isSecondaryUseActive()) {
         if (!this.level().isClientSide()) {
            p_458345_.startRiding(this);
         }

         return InteractionResult.SUCCESS;
      } else {
         InteractionResult interactionresult = super.mobInteract(p_458345_, p_459637_);
         if (!interactionresult.consumesAction()) {
            ItemStack itemstack = p_458345_.getItemInHand(p_459637_);
            return this.isEquippableInSlot(itemstack, EquipmentSlot.SADDLE)
               ? itemstack.interactLivingEntity(p_458345_, this, p_459637_)
               : InteractionResult.PASS;
         } else {
            return interactionresult;
         }
      }
   }

   @Override
   public boolean canUseSlot(EquipmentSlot p_460048_) {
      return p_460048_ != EquipmentSlot.SADDLE ? super.canUseSlot(p_460048_) : this.isAlive() && !this.isBaby();
   }

   @Override
   protected boolean canDispenserEquipIntoSlot(EquipmentSlot p_455663_) {
      return p_455663_ == EquipmentSlot.SADDLE || super.canDispenserEquipIntoSlot(p_455663_);
   }

   @Override
   protected Holder<SoundEvent> getEquipSound(EquipmentSlot p_452116_, ItemStack p_459901_, Equippable p_450790_) {
      return p_452116_ == EquipmentSlot.SADDLE ? SoundEvents.PIG_SADDLE : super.getEquipSound(p_452116_, p_459901_, p_450790_);
   }

   @Override
   public void thunderHit(ServerLevel p_452901_, LightningBolt p_454063_) {
      if (p_452901_.getDifficulty() != Difficulty.PEACEFUL) {
         ZombifiedPiglin zombifiedpiglin = this.convertTo(EntityType.ZOMBIFIED_PIGLIN, ConversionParams.single(this, false, true), p_456410_ -> {
            p_456410_.populateDefaultEquipmentSlots(this.getRandom(), p_452901_.getCurrentDifficultyAt(this.blockPosition()));
            p_456410_.setPersistenceRequired();
         });
         if (zombifiedpiglin == null) {
            super.thunderHit(p_452901_, p_454063_);
         }
      } else {
         super.thunderHit(p_452901_, p_454063_);
      }
   }

   @Override
   protected void tickRidden(Player p_450623_, Vec3 p_460651_) {
      super.tickRidden(p_450623_, p_460651_);
      this.setRot(p_450623_.getYRot(), p_450623_.getXRot() * 0.5F);
      this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
      this.steering.tickBoost();
   }

   @Override
   protected Vec3 getRiddenInput(Player p_451056_, Vec3 p_452064_) {
      return new Vec3(0.0, 0.0, 1.0);
   }

   @Override
   protected float getRiddenSpeed(Player p_450758_) {
      return (float)(this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.225 * this.steering.boostFactor());
   }

   @Override
   public boolean boost() {
      return this.steering.boost(this.getRandom());
   }

   public @Nullable Pig getBreedOffspring(ServerLevel p_451358_, AgeableMob p_450433_) {
      Pig pig = EntityType.PIG.create(p_451358_, EntitySpawnReason.BREEDING);
      if (pig != null && p_450433_ instanceof Pig pig1) {
         pig.setVariant(this.random.nextBoolean() ? this.getVariant() : pig1.getVariant());
      }

      return pig;
   }

   @Override
   public boolean isFood(ItemStack p_452387_) {
      return p_452387_.is(ItemTags.PIG_FOOD);
   }

   @Override
   public Vec3 getLeashOffset() {
      return new Vec3(0.0, 0.6F * this.getEyeHeight(), this.getBbWidth() * 0.4F);
   }

   private void setVariant(Holder<PigVariant> p_454852_) {
      this.entityData.set(DATA_VARIANT_ID, p_454852_);
   }

   public Holder<PigVariant> getVariant() {
      return this.entityData.get(DATA_VARIANT_ID);
   }

   @Override
   public <T> @Nullable T get(DataComponentType<? extends T> p_453052_) {
      return p_453052_ == DataComponents.PIG_VARIANT ? castComponentValue((DataComponentType<T>)p_453052_, this.getVariant()) : super.get(p_453052_);
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_455337_) {
      this.applyImplicitComponentIfPresent(p_455337_, DataComponents.PIG_VARIANT);
      super.applyImplicitComponents(p_455337_);
   }

   @Override
   protected <T> boolean applyImplicitComponent(DataComponentType<T> p_456478_, T p_455633_) {
      if (p_456478_ == DataComponents.PIG_VARIANT) {
         this.setVariant(castComponentValue(DataComponents.PIG_VARIANT, p_455633_));
         return true;
      } else {
         return super.applyImplicitComponent(p_456478_, p_455633_);
      }
   }

   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_451627_, DifficultyInstance p_452316_, EntitySpawnReason p_451929_, @Nullable SpawnGroupData p_461007_
   ) {
      VariantUtils.selectVariantToSpawn(SpawnContext.create(p_451627_, this.blockPosition()), Registries.PIG_VARIANT).ifPresent(this::setVariant);
      return super.finalizeSpawn(p_451627_, p_452316_, p_451929_, p_461007_);
   }
}
