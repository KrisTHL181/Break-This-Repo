package net.minecraft.world.entity.animal.feline;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.CatLieOnBedGoal;
import net.minecraft.world.entity.ai.goal.CatSitOnBlockGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OcelotAttackGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.variant.SpawnContext;
import net.minecraft.world.entity.variant.VariantUtils;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class Cat extends TamableAnimal {
   public static final double TEMPT_SPEED_MOD = 0.6;
   public static final double WALK_SPEED_MOD = 0.8;
   public static final double SPRINT_SPEED_MOD = 1.33;
   private static final EntityDataAccessor<Holder<CatVariant>> DATA_VARIANT_ID = SynchedEntityData.defineId(Cat.class, EntityDataSerializers.CAT_VARIANT);
   private static final EntityDataAccessor<Boolean> IS_LYING = SynchedEntityData.defineId(Cat.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> RELAX_STATE_ONE = SynchedEntityData.defineId(Cat.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR = SynchedEntityData.defineId(Cat.class, EntityDataSerializers.INT);
   private static final ResourceKey<CatVariant> DEFAULT_VARIANT = CatVariants.BLACK;
   private static final DyeColor DEFAULT_COLLAR_COLOR = DyeColor.RED;
   private Cat.@Nullable CatAvoidEntityGoal<Player> avoidPlayersGoal;
   private @Nullable TemptGoal temptGoal;
   private float lieDownAmount;
   private float lieDownAmountO;
   private float lieDownAmountTail;
   private float lieDownAmountOTail;
   private boolean isLyingOnTopOfSleepingPlayer;
   private float relaxStateOneAmount;
   private float relaxStateOneAmountO;

   public Cat(EntityType<? extends Cat> p_450475_, Level p_460125_) {
      super(p_450475_, p_460125_);
      this.reassessTameGoals();
   }

   @Override
   protected void registerGoals() {
      this.temptGoal = new Cat.CatTemptGoal(this, 0.6, p_458797_ -> p_458797_.is(ItemTags.CAT_FOOD), true);
      this.goalSelector.addGoal(1, new FloatGoal(this));
      this.goalSelector.addGoal(1, new TamableAnimal.TamableAnimalPanicGoal(1.5));
      this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
      this.goalSelector.addGoal(3, new Cat.CatRelaxOnOwnerGoal(this));
      this.goalSelector.addGoal(4, this.temptGoal);
      this.goalSelector.addGoal(5, new CatLieOnBedGoal(this, 1.1, 8));
      this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.0, 10.0F, 5.0F));
      this.goalSelector.addGoal(7, new CatSitOnBlockGoal(this, 0.8));
      this.goalSelector.addGoal(8, new LeapAtTargetGoal(this, 0.3F));
      this.goalSelector.addGoal(9, new OcelotAttackGoal(this));
      this.goalSelector.addGoal(10, new BreedGoal(this, 0.8));
      this.goalSelector.addGoal(11, new WaterAvoidingRandomStrollGoal(this, 0.8, 1.0000001E-5F));
      this.goalSelector.addGoal(12, new LookAtPlayerGoal(this, Player.class, 10.0F));
      this.targetSelector.addGoal(1, new NonTameRandomTargetGoal<>(this, Rabbit.class, false, null));
      this.targetSelector.addGoal(1, new NonTameRandomTargetGoal<>(this, Turtle.class, false, Turtle.BABY_ON_LAND_SELECTOR));
   }

   public Holder<CatVariant> getVariant() {
      return this.entityData.get(DATA_VARIANT_ID);
   }

   private void setVariant(Holder<CatVariant> p_458808_) {
      this.entityData.set(DATA_VARIANT_ID, p_458808_);
   }

   @Override
   public <T> @Nullable T get(DataComponentType<? extends T> p_453519_) {
      if (p_453519_ == DataComponents.CAT_VARIANT) {
         return castComponentValue((DataComponentType<T>)p_453519_, this.getVariant());
      } else {
         return p_453519_ == DataComponents.CAT_COLLAR ? castComponentValue((DataComponentType<T>)p_453519_, this.getCollarColor()) : super.get(p_453519_);
      }
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_453347_) {
      this.applyImplicitComponentIfPresent(p_453347_, DataComponents.CAT_VARIANT);
      this.applyImplicitComponentIfPresent(p_453347_, DataComponents.CAT_COLLAR);
      super.applyImplicitComponents(p_453347_);
   }

   @Override
   protected <T> boolean applyImplicitComponent(DataComponentType<T> p_459621_, T p_451752_) {
      if (p_459621_ == DataComponents.CAT_VARIANT) {
         this.setVariant(castComponentValue(DataComponents.CAT_VARIANT, p_451752_));
         return true;
      } else if (p_459621_ == DataComponents.CAT_COLLAR) {
         this.setCollarColor(castComponentValue(DataComponents.CAT_COLLAR, p_451752_));
         return true;
      } else {
         return super.applyImplicitComponent(p_459621_, p_451752_);
      }
   }

   public void setLying(boolean p_450189_) {
      this.entityData.set(IS_LYING, p_450189_);
   }

   public boolean isLying() {
      return this.entityData.get(IS_LYING);
   }

   void setRelaxStateOne(boolean p_459955_) {
      this.entityData.set(RELAX_STATE_ONE, p_459955_);
   }

   boolean isRelaxStateOne() {
      return this.entityData.get(RELAX_STATE_ONE);
   }

   public DyeColor getCollarColor() {
      return DyeColor.byId(this.entityData.get(DATA_COLLAR_COLOR));
   }

   private void setCollarColor(DyeColor p_453421_) {
      this.entityData.set(DATA_COLLAR_COLOR, p_453421_.getId());
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_455710_) {
      super.defineSynchedData(p_455710_);
      p_455710_.define(DATA_VARIANT_ID, VariantUtils.getDefaultOrAny(this.registryAccess(), DEFAULT_VARIANT));
      p_455710_.define(IS_LYING, false);
      p_455710_.define(RELAX_STATE_ONE, false);
      p_455710_.define(DATA_COLLAR_COLOR, DEFAULT_COLLAR_COLOR.getId());
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_460693_) {
      super.addAdditionalSaveData(p_460693_);
      VariantUtils.writeVariant(p_460693_, this.getVariant());
      p_460693_.store("CollarColor", DyeColor.LEGACY_ID_CODEC, this.getCollarColor());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_450552_) {
      super.readAdditionalSaveData(p_450552_);
      VariantUtils.readVariant(p_450552_, Registries.CAT_VARIANT).ifPresent(this::setVariant);
      this.setCollarColor(p_450552_.<DyeColor>read("CollarColor", DyeColor.LEGACY_ID_CODEC).orElse(DEFAULT_COLLAR_COLOR));
   }

   @Override
   public void customServerAiStep(ServerLevel p_457795_) {
      if (this.getMoveControl().hasWanted()) {
         double d0 = this.getMoveControl().getSpeedModifier();
         if (d0 == 0.6) {
            this.setPose(Pose.CROUCHING);
            this.setSprinting(false);
         } else if (d0 == 1.33) {
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
   protected @Nullable SoundEvent getAmbientSound() {
      if (this.isTame()) {
         if (this.isInLove()) {
            return SoundEvents.CAT_PURR;
         } else {
            return this.random.nextInt(4) == 0 ? SoundEvents.CAT_PURREOW : SoundEvents.CAT_AMBIENT;
         }
      } else {
         return SoundEvents.CAT_STRAY_AMBIENT;
      }
   }

   @Override
   public int getAmbientSoundInterval() {
      return 120;
   }

   public void hiss() {
      this.makeSound(SoundEvents.CAT_HISS);
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_451824_) {
      return SoundEvents.CAT_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.CAT_DEATH;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.3F).add(Attributes.ATTACK_DAMAGE, 3.0);
   }

   @Override
   protected void playEatingSound() {
      this.playSound(SoundEvents.CAT_EAT, 1.0F, 1.0F);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.temptGoal != null && this.temptGoal.isRunning() && !this.isTame() && this.tickCount % 100 == 0) {
         this.playSound(SoundEvents.CAT_BEG_FOR_FOOD, 1.0F, 1.0F);
      }

      this.handleLieDown();
   }

   private void handleLieDown() {
      if ((this.isLying() || this.isRelaxStateOne()) && this.tickCount % 5 == 0) {
         this.playSound(SoundEvents.CAT_PURR, 0.6F + 0.4F * (this.random.nextFloat() - this.random.nextFloat()), 1.0F);
      }

      this.updateLieDownAmount();
      this.updateRelaxStateOneAmount();
      this.isLyingOnTopOfSleepingPlayer = false;
      if (this.isLying()) {
         BlockPos blockpos = this.blockPosition();

         for (Player player : this.level().getEntitiesOfClass(Player.class, new AABB(blockpos).inflate(2.0, 2.0, 2.0))) {
            if (player.isSleeping()) {
               this.isLyingOnTopOfSleepingPlayer = true;
               break;
            }
         }
      }
   }

   public boolean isLyingOnTopOfSleepingPlayer() {
      return this.isLyingOnTopOfSleepingPlayer;
   }

   private void updateLieDownAmount() {
      this.lieDownAmountO = this.lieDownAmount;
      this.lieDownAmountOTail = this.lieDownAmountTail;
      if (this.isLying()) {
         this.lieDownAmount = Math.min(1.0F, this.lieDownAmount + 0.15F);
         this.lieDownAmountTail = Math.min(1.0F, this.lieDownAmountTail + 0.08F);
      } else {
         this.lieDownAmount = Math.max(0.0F, this.lieDownAmount - 0.22F);
         this.lieDownAmountTail = Math.max(0.0F, this.lieDownAmountTail - 0.13F);
      }
   }

   private void updateRelaxStateOneAmount() {
      this.relaxStateOneAmountO = this.relaxStateOneAmount;
      if (this.isRelaxStateOne()) {
         this.relaxStateOneAmount = Math.min(1.0F, this.relaxStateOneAmount + 0.1F);
      } else {
         this.relaxStateOneAmount = Math.max(0.0F, this.relaxStateOneAmount - 0.13F);
      }
   }

   public float getLieDownAmount(float p_459155_) {
      return Mth.lerp(p_459155_, this.lieDownAmountO, this.lieDownAmount);
   }

   public float getLieDownAmountTail(float p_451054_) {
      return Mth.lerp(p_451054_, this.lieDownAmountOTail, this.lieDownAmountTail);
   }

   public float getRelaxStateOneAmount(float p_456719_) {
      return Mth.lerp(p_456719_, this.relaxStateOneAmountO, this.relaxStateOneAmount);
   }

   public @Nullable Cat getBreedOffspring(ServerLevel p_455151_, AgeableMob p_460287_) {
      Cat cat = EntityType.CAT.create(p_455151_, EntitySpawnReason.BREEDING);
      if (cat != null && p_460287_ instanceof Cat cat1) {
         if (this.random.nextBoolean()) {
            cat.setVariant(this.getVariant());
         } else {
            cat.setVariant(cat1.getVariant());
         }

         if (this.isTame()) {
            cat.setOwnerReference(this.getOwnerReference());
            cat.setTame(true, true);
            DyeColor dyecolor = this.getCollarColor();
            DyeColor dyecolor1 = cat1.getCollarColor();
            cat.setCollarColor(DyeColor.getMixedColor(p_455151_, dyecolor, dyecolor1));
         }
      }

      return cat;
   }

   @Override
   public boolean canMate(Animal p_453545_) {
      if (!this.isTame()) {
         return false;
      } else {
         return p_453545_ instanceof Cat cat ? cat.isTame() && super.canMate(p_453545_) : false;
      }
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_460172_, DifficultyInstance p_452342_, EntitySpawnReason p_450503_, @Nullable SpawnGroupData p_459350_
   ) {
      p_459350_ = super.finalizeSpawn(p_460172_, p_452342_, p_450503_, p_459350_);
      VariantUtils.selectVariantToSpawn(SpawnContext.create(p_460172_, this.blockPosition()), Registries.CAT_VARIANT).ifPresent(this::setVariant);
      return p_459350_;
   }

   @Override
   public InteractionResult mobInteract(Player p_460113_, InteractionHand p_450900_) {
      ItemStack itemstack = p_460113_.getItemInHand(p_450900_);
      Item item = itemstack.getItem();
      if (this.isTame()) {
         if (this.isOwnedBy(p_460113_)) {
            if (item instanceof DyeItem dyeitem) {
               DyeColor dyecolor = dyeitem.getDyeColor();
               if (dyecolor != this.getCollarColor()) {
                  if (!this.level().isClientSide()) {
                     this.setCollarColor(dyecolor);
                     itemstack.consume(1, p_460113_);
                     this.setPersistenceRequired();
                  }

                  return InteractionResult.SUCCESS;
               }
            } else if (this.isFood(itemstack) && this.getHealth() < this.getMaxHealth()) {
               if (!this.level().isClientSide()) {
                  this.usePlayerItem(p_460113_, p_450900_, itemstack);
                  FoodProperties foodproperties = itemstack.get(DataComponents.FOOD);
                  this.heal(foodproperties != null ? foodproperties.nutrition() : 1.0F);
                  this.playEatingSound();
               }

               return InteractionResult.SUCCESS;
            }

            InteractionResult interactionresult = super.mobInteract(p_460113_, p_450900_);
            if (!interactionresult.consumesAction()) {
               this.setOrderedToSit(!this.isOrderedToSit());
               return InteractionResult.SUCCESS;
            }

            return interactionresult;
         }
      } else if (this.isFood(itemstack)) {
         if (!this.level().isClientSide()) {
            this.usePlayerItem(p_460113_, p_450900_, itemstack);
            this.tryToTame(p_460113_);
            this.setPersistenceRequired();
            this.playEatingSound();
         }

         return InteractionResult.SUCCESS;
      }

      InteractionResult interactionresult1 = super.mobInteract(p_460113_, p_450900_);
      if (interactionresult1.consumesAction()) {
         this.setPersistenceRequired();
      }

      return interactionresult1;
   }

   @Override
   public boolean isFood(ItemStack p_455899_) {
      return p_455899_.is(ItemTags.CAT_FOOD);
   }

   @Override
   public boolean removeWhenFarAway(double p_458083_) {
      return !this.isTame() && this.tickCount > 2400;
   }

   @Override
   public void setTame(boolean p_450192_, boolean p_457887_) {
      super.setTame(p_450192_, p_457887_);
      this.reassessTameGoals();
   }

   protected void reassessTameGoals() {
      if (this.avoidPlayersGoal == null) {
         this.avoidPlayersGoal = new Cat.CatAvoidEntityGoal<>(this, Player.class, 16.0F, 0.8, 1.33);
      }

      this.goalSelector.removeGoal(this.avoidPlayersGoal);
      if (!this.isTame()) {
         this.goalSelector.addGoal(4, this.avoidPlayersGoal);
      }
   }

   private void tryToTame(Player p_451247_) {
      if (this.random.nextInt(3) == 0) {
         this.tame(p_451247_);
         this.setOrderedToSit(true);
         this.level().broadcastEntityEvent(this, (byte)7);
      } else {
         this.level().broadcastEntityEvent(this, (byte)6);
      }
   }

   @Override
   public boolean isSteppingCarefully() {
      return this.isCrouching() || super.isSteppingCarefully();
   }

   static class CatAvoidEntityGoal<T extends LivingEntity> extends AvoidEntityGoal<T> {
      private final Cat cat;

      public CatAvoidEntityGoal(Cat p_458123_, Class<T> p_450532_, float p_457006_, double p_454471_, double p_450460_) {
         super(p_458123_, p_450532_, p_457006_, p_454471_, p_450460_, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
         this.cat = p_458123_;
      }

      @Override
      public boolean canUse() {
         return !this.cat.isTame() && super.canUse();
      }

      @Override
      public boolean canContinueToUse() {
         return !this.cat.isTame() && super.canContinueToUse();
      }
   }

   static class CatRelaxOnOwnerGoal extends Goal {
      private final Cat cat;
      private @Nullable Player ownerPlayer;
      private @Nullable BlockPos goalPos;
      private int onBedTicks;

      public CatRelaxOnOwnerGoal(Cat p_456002_) {
         this.cat = p_456002_;
      }

      @Override
      public boolean canUse() {
         if (!this.cat.isTame()) {
            return false;
         }

         if (this.cat.isOrderedToSit()) {
            return false;
         }

         LivingEntity livingentity = this.cat.getOwner();
         if (livingentity instanceof Player player) {
            this.ownerPlayer = player;
            if (!livingentity.isSleeping()) {
               return false;
            }

            if (this.cat.distanceToSqr(this.ownerPlayer) > 100.0) {
               return false;
            }

            BlockPos blockpos = this.ownerPlayer.blockPosition();
            BlockState blockstate = this.cat.level().getBlockState(blockpos);
            if (blockstate.is(BlockTags.BEDS)) {
               this.goalPos = blockstate.getOptionalValue(BedBlock.FACING)
                  .map(p_455309_ -> blockpos.relative(p_455309_.getOpposite()))
                  .orElseGet(() -> new BlockPos(blockpos));
               return !this.spaceIsOccupied();
            }
         }

         return false;
      }

      private boolean spaceIsOccupied() {
         for (Cat cat : this.cat.level().getEntitiesOfClass(Cat.class, new AABB(this.goalPos).inflate(2.0))) {
            if (cat != this.cat && (cat.isLying() || cat.isRelaxStateOne())) {
               return true;
            }
         }

         return false;
      }

      @Override
      public boolean canContinueToUse() {
         return this.cat.isTame()
            && !this.cat.isOrderedToSit()
            && this.ownerPlayer != null
            && this.ownerPlayer.isSleeping()
            && this.goalPos != null
            && !this.spaceIsOccupied();
      }

      @Override
      public void start() {
         if (this.goalPos != null) {
            this.cat.setInSittingPose(false);
            this.cat.getNavigation().moveTo(this.goalPos.getX(), this.goalPos.getY(), this.goalPos.getZ(), 1.1F);
         }
      }

      @Override
      public void stop() {
         this.cat.setLying(false);
         if (this.ownerPlayer.getSleepTimer() >= 100
            && this.cat.level().getRandom().nextFloat()
               < this.cat.level().environmentAttributes().getValue(EnvironmentAttributes.CAT_WAKING_UP_GIFT_CHANCE, this.cat.position())) {
            this.giveMorningGift();
         }

         this.onBedTicks = 0;
         this.cat.setRelaxStateOne(false);
         this.cat.getNavigation().stop();
      }

      private void giveMorningGift() {
         RandomSource randomsource = this.cat.getRandom();
         BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
         blockpos$mutableblockpos.set(this.cat.isLeashed() ? this.cat.getLeashHolder().blockPosition() : this.cat.blockPosition());
         this.cat
            .randomTeleport(
               blockpos$mutableblockpos.getX() + randomsource.nextInt(11) - 5,
               blockpos$mutableblockpos.getY() + randomsource.nextInt(5) - 2,
               blockpos$mutableblockpos.getZ() + randomsource.nextInt(11) - 5,
               false
            );
         blockpos$mutableblockpos.set(this.cat.blockPosition());
         this.cat
            .dropFromGiftLootTable(
               getServerLevel(this.cat),
               BuiltInLootTables.CAT_MORNING_GIFT,
               (p_455796_, p_451741_) -> p_455796_.addFreshEntity(
                  new ItemEntity(
                     p_455796_,
                     (double)blockpos$mutableblockpos.getX() - Mth.sin(this.cat.yBodyRot * (float) (Math.PI / 180.0)),
                     blockpos$mutableblockpos.getY(),
                     (double)blockpos$mutableblockpos.getZ() + Mth.cos(this.cat.yBodyRot * (float) (Math.PI / 180.0)),
                     p_451741_
                  )
               )
            );
      }

      @Override
      public void tick() {
         if (this.ownerPlayer != null && this.goalPos != null) {
            this.cat.setInSittingPose(false);
            this.cat.getNavigation().moveTo(this.goalPos.getX(), this.goalPos.getY(), this.goalPos.getZ(), 1.1F);
            if (this.cat.distanceToSqr(this.ownerPlayer) < 2.5) {
               this.onBedTicks++;
               if (this.onBedTicks > this.adjustedTickDelay(16)) {
                  this.cat.setLying(true);
                  this.cat.setRelaxStateOne(false);
               } else {
                  this.cat.lookAt(this.ownerPlayer, 45.0F, 45.0F);
                  this.cat.setRelaxStateOne(true);
               }
            } else {
               this.cat.setLying(false);
            }
         }
      }
   }

   static class CatTemptGoal extends TemptGoal {
      private @Nullable Player selectedPlayer;
      private final Cat cat;

      public CatTemptGoal(Cat p_452820_, double p_450936_, Predicate<ItemStack> p_450565_, boolean p_457956_) {
         super(p_452820_, p_450936_, p_450565_, p_457956_);
         this.cat = p_452820_;
      }

      @Override
      public void tick() {
         super.tick();
         if (this.selectedPlayer == null && this.mob.getRandom().nextInt(this.adjustedTickDelay(600)) == 0) {
            this.selectedPlayer = this.player;
         } else if (this.mob.getRandom().nextInt(this.adjustedTickDelay(500)) == 0) {
            this.selectedPlayer = null;
         }
      }

      @Override
      protected boolean canScare() {
         return this.selectedPlayer != null && this.selectedPlayer.equals(this.player) ? false : super.canScare();
      }

      @Override
      public boolean canUse() {
         return super.canUse() && !this.cat.isTame();
      }
   }
}
