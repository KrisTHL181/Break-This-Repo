package net.minecraft.world.entity.animal.nautilus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class AbstractNautilus extends TamableAnimal implements HasCustomInventoryScreen, PlayerRideableJumping {
   public static final int INVENTORY_SLOT_OFFSET = 500;
   public static final int INVENTORY_ROWS = 3;
   public static final int SMALL_RESTRICTION_RADIUS = 16;
   public static final int LARGE_RESTRICTION_RADIUS = 32;
   public static final int RESTRICTION_RADIUS_BUFFER = 8;
   private static final int EFFECT_DURATION = 60;
   private static final int EFFECT_REFRESH_RATE = 40;
   private static final double NAUTILUS_WATER_RESISTANCE = 0.9;
   private static final float IN_WATER_SPEED_MODIFIER = 0.011F;
   private static final float RIDDEN_SPEED_MODIFIER_IN_WATER = 0.0325F;
   private static final float RIDDEN_SPEED_MODIFIER_ON_LAND = 0.02F;
   private static final EntityDataAccessor<Boolean> DASH = SynchedEntityData.defineId(AbstractNautilus.class, EntityDataSerializers.BOOLEAN);
   private static final int DASH_COOLDOWN_TICKS = 40;
   private static final int DASH_MINIMUM_DURATION_TICKS = 5;
   private static final float DASH_MOMENTUM_IN_WATER = 1.2F;
   private static final float DASH_MOMENTUM_ON_LAND = 0.5F;
   private int dashCooldown = 0;
   protected float playerJumpPendingScale;
   protected SimpleContainer inventory;
   private static final double BUBBLE_SPREAD_FACTOR = 0.8;
   private static final double BUBBLE_DIRECTION_SCALE = 1.1;
   private static final double BUBBLE_Y_OFFSET = 0.25;
   private static final double BUBBLE_PROBABILITY_MULTIPLIER = 2.0;
   private static final float BUBBLE_PROBABILITY_MIN = 0.15F;
   private static final float BUBBLE_PROBABILITY_MAX = 1.0F;

   protected AbstractNautilus(EntityType<? extends AbstractNautilus> p_455832_, Level p_452219_) {
      super(p_455832_, p_452219_);
      this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.011F, 0.0F, true);
      this.lookControl = new SmoothSwimmingLookControl(this, 10);
      this.setPathfindingMalus(PathType.WATER, 0.0F);
      this.createInventory();
   }

   @Override
   public boolean isFood(ItemStack p_453020_) {
      return !this.isTame() && !this.isBaby() ? p_453020_.is(ItemTags.NAUTILUS_TAMING_ITEMS) : p_453020_.is(ItemTags.NAUTILUS_FOOD);
   }

   @Override
   protected void usePlayerItem(Player p_450386_, InteractionHand p_450300_, ItemStack p_455954_) {
      if (p_455954_.is(ItemTags.NAUTILUS_BUCKET_FOOD)) {
         p_450386_.setItemInHand(p_450300_, ItemUtils.createFilledResult(p_455954_, p_450386_, new ItemStack(Items.WATER_BUCKET)));
      } else {
         super.usePlayerItem(p_450386_, p_450300_, p_455954_);
      }
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Animal.createAnimalAttributes()
         .add(Attributes.MAX_HEALTH, 15.0)
         .add(Attributes.MOVEMENT_SPEED, 1.0)
         .add(Attributes.ATTACK_DAMAGE, 3.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.3F);
   }

   @Override
   public boolean isPushedByFluid() {
      return false;
   }

   @Override
   protected PathNavigation createNavigation(Level p_450752_) {
      return new WaterBoundPathNavigation(this, p_450752_);
   }

   @Override
   public float getWalkTargetValue(BlockPos p_457176_, LevelReader p_458494_) {
      return 0.0F;
   }

   public static boolean checkNautilusSpawnRules(
      EntityType<? extends AbstractNautilus> p_456993_, LevelAccessor p_452931_, EntitySpawnReason p_458103_, BlockPos p_453882_, RandomSource p_458433_
   ) {
      int i = p_452931_.getSeaLevel();
      int j = i - 25;
      return p_453882_.getY() >= j
         && p_453882_.getY() <= i - 5
         && p_452931_.getFluidState(p_453882_.below()).is(FluidTags.WATER)
         && p_452931_.getBlockState(p_453882_.above()).is(Blocks.WATER);
   }

   @Override
   public boolean checkSpawnObstruction(LevelReader p_458655_) {
      return p_458655_.isUnobstructed(this);
   }

   @Override
   public boolean canUseSlot(EquipmentSlot p_453536_) {
      return p_453536_ != EquipmentSlot.SADDLE && p_453536_ != EquipmentSlot.BODY
         ? super.canUseSlot(p_453536_)
         : this.isAlive() && !this.isBaby() && this.isTame();
   }

   @Override
   protected boolean canDispenserEquipIntoSlot(EquipmentSlot p_459800_) {
      return p_459800_ == EquipmentSlot.BODY || p_459800_ == EquipmentSlot.SADDLE || super.canDispenserEquipIntoSlot(p_459800_);
   }

   @Override
   protected boolean canAddPassenger(Entity p_453027_) {
      return !this.isVehicle();
   }

   @Override
   public @Nullable LivingEntity getControllingPassenger() {
      return this.isSaddled() && this.getFirstPassenger() instanceof Player player ? player : super.getControllingPassenger();
   }

   @Override
   protected Vec3 getRiddenInput(Player p_454068_, Vec3 p_458490_) {
      float f = p_454068_.xxa;
      float f1 = 0.0F;
      float f2 = 0.0F;
      if (p_454068_.zza != 0.0F) {
         float f3 = Mth.cos(p_454068_.getXRot() * (float) (Math.PI / 180.0));
         float f4 = -Mth.sin(p_454068_.getXRot() * (float) (Math.PI / 180.0));
         if (p_454068_.zza < 0.0F) {
            f3 *= -0.5F;
            f4 *= -0.5F;
         }

         f2 = f4;
         f1 = f3;
      }

      return new Vec3(f, f2, f1);
   }

   protected Vec2 getRiddenRotation(LivingEntity p_456649_) {
      return new Vec2(p_456649_.getXRot() * 0.5F, p_456649_.getYRot());
   }

   @Override
   protected void tickRidden(Player p_450782_, Vec3 p_452110_) {
      super.tickRidden(p_450782_, p_452110_);
      Vec2 vec2 = this.getRiddenRotation(p_450782_);
      float f = this.getYRot();
      float f1 = Mth.wrapDegrees(vec2.y - f);
      float f2 = 0.5F;
      f += f1 * 0.5F;
      this.setRot(f, vec2.x);
      this.yRotO = this.yBodyRot = this.yHeadRot = f;
      if (this.isLocalInstanceAuthoritative()) {
         if (this.playerJumpPendingScale > 0.0F && !this.isJumping()) {
            this.executeRidersJump(this.playerJumpPendingScale, p_450782_);
         }

         this.playerJumpPendingScale = 0.0F;
      }
   }

   @Override
   protected void travelInWater(Vec3 p_460206_, double p_455904_, boolean p_451288_, double p_457106_) {
      float f = this.getSpeed();
      this.moveRelative(f, p_460206_);
      this.move(MoverType.SELF, this.getDeltaMovement());
      this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
   }

   @Override
   protected float getRiddenSpeed(Player p_460619_) {
      return this.isInWater()
         ? 0.0325F * (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED)
         : 0.02F * (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED);
   }

   protected void doPlayerRide(Player p_457064_) {
      if (!this.level().isClientSide()) {
         p_457064_.startRiding(this);
         if (!this.isVehicle()) {
            this.clearHome();
         }
      }
   }

   private int getNautilusRestrictionRadius() {
      return !this.isBaby() && this.getItemBySlot(EquipmentSlot.SADDLE).isEmpty() ? 32 : 16;
   }

   protected void checkRestriction() {
      if (!this.isLeashed() && !this.isVehicle() && this.isTame()) {
         int i = this.getNautilusRestrictionRadius();
         if (!this.hasHome() || !this.getHomePosition().closerThan(this.blockPosition(), i + 8) || i != this.getHomeRadius()) {
            this.setHomeTo(this.blockPosition(), i);
         }
      }
   }

   @Override
   protected void customServerAiStep(ServerLevel p_451717_) {
      this.checkRestriction();
      super.customServerAiStep(p_451717_);
   }

   private void applyEffects(Level p_456301_) {
      if (this.getFirstPassenger() instanceof Player player) {
         boolean flag = player.hasEffect(MobEffects.BREATH_OF_THE_NAUTILUS);
         boolean flag1 = p_456301_.getGameTime() % 40L == 0L;
         if (!flag || flag1) {
            player.addEffect(new MobEffectInstance(MobEffects.BREATH_OF_THE_NAUTILUS, 60, 0, true, true, true));
         }
      }
   }

   private void spawnBubbles() {
      double d0 = this.getDeltaMovement().length();
      double d1 = Mth.clamp(d0 * 2.0, 0.15F, 1.0);
      if (this.random.nextFloat() < d1) {
         float f = this.getYRot();
         float f1 = Mth.clamp(this.getXRot(), -10.0F, 10.0F);
         Vec3 vec3 = this.calculateViewVector(f1, f);
         double d2 = this.random.nextDouble() * 0.8 * (1.0 + d0);
         double d3 = (this.random.nextFloat() - 0.5) * d2;
         double d4 = (this.random.nextFloat() - 0.5) * d2;
         double d5 = (this.random.nextFloat() - 0.5) * d2;
         this.level().addParticle(ParticleTypes.BUBBLE, this.getX() - vec3.x * 1.1, this.getY() - vec3.y + 0.25, this.getZ() - vec3.z * 1.1, d3, d4, d5);
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         this.applyEffects(this.level());
      }

      if (this.isDashing() && this.dashCooldown < 35) {
         this.setDashing(false);
      }

      if (this.dashCooldown > 0) {
         this.dashCooldown--;
         if (this.dashCooldown == 0) {
            this.makeSound(this.getDashReadySound());
         }
      }

      if (this.isInWater()) {
         this.spawnBubbles();
      }
   }

   @Override
   public boolean canJump() {
      return this.isSaddled();
   }

   @Override
   public void onPlayerJump(int p_452581_) {
      if (this.isSaddled() && this.dashCooldown <= 0) {
         this.playerJumpPendingScale = this.getPlayerJumpPendingScale(p_452581_);
      }
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_454726_) {
      super.defineSynchedData(p_454726_);
      p_454726_.define(DASH, false);
   }

   public boolean isDashing() {
      return this.entityData.get(DASH);
   }

   public void setDashing(boolean p_459445_) {
      this.entityData.set(DASH, p_459445_);
   }

   protected void executeRidersJump(float p_452491_, Player p_455774_) {
      this.addDeltaMovement(
         p_455774_.getLookAngle()
            .scale((this.isInWater() ? 1.2F : 0.5F) * p_452491_ * this.getAttributeValue(Attributes.MOVEMENT_SPEED) * this.getBlockSpeedFactor())
      );
      this.dashCooldown = 40;
      this.setDashing(true);
      this.needsSync = true;
   }

   @Override
   public void handleStartJump(int p_450502_) {
      this.makeSound(this.getDashSound());
      this.gameEvent(GameEvent.ENTITY_ACTION);
      this.setDashing(true);
   }

   @Override
   public int getJumpCooldown() {
      return this.dashCooldown;
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> p_451265_) {
      if (!this.firstTick && DASH.equals(p_451265_)) {
         this.dashCooldown = this.dashCooldown == 0 ? 40 : this.dashCooldown;
      }

      super.onSyncedDataUpdated(p_451265_);
   }

   @Override
   public void handleStopJump() {
   }

   @Override
   protected void playStepSound(BlockPos p_452461_, BlockState p_455043_) {
   }

   protected @Nullable SoundEvent getDashSound() {
      return null;
   }

   protected @Nullable SoundEvent getDashReadySound() {
      return null;
   }

   @Override
   public InteractionResult interact(Player p_452852_, InteractionHand p_457312_) {
      this.setPersistenceRequired();
      return super.interact(p_452852_, p_457312_);
   }

   @Override
   public InteractionResult mobInteract(Player p_458952_, InteractionHand p_460677_) {
      ItemStack itemstack = p_458952_.getItemInHand(p_460677_);
      if (this.isBaby()) {
         return super.mobInteract(p_458952_, p_460677_);
      }

      if (this.isTame() && p_458952_.isSecondaryUseActive()) {
         this.openCustomInventoryScreen(p_458952_);
         return InteractionResult.SUCCESS;
      }

      if (!itemstack.isEmpty()) {
         if (!this.level().isClientSide() && !this.isTame() && this.isFood(itemstack)) {
            this.usePlayerItem(p_458952_, p_460677_, itemstack);
            this.tryToTame(p_458952_);
            return InteractionResult.SUCCESS_SERVER;
         }

         if (this.isFood(itemstack) && this.getHealth() < this.getMaxHealth()) {
            FoodProperties foodproperties = itemstack.get(DataComponents.FOOD);
            this.heal(foodproperties != null ? 2 * foodproperties.nutrition() : 1.0F);
            this.usePlayerItem(p_458952_, p_460677_, itemstack);
            this.playEatingSound();
            return InteractionResult.SUCCESS;
         }

         InteractionResult interactionresult = itemstack.interactLivingEntity(p_458952_, this, p_460677_);
         if (interactionresult.consumesAction()) {
            return interactionresult;
         }
      }

      if (this.isTame() && !p_458952_.isSecondaryUseActive() && !this.isFood(itemstack)) {
         this.doPlayerRide(p_458952_);
         return InteractionResult.SUCCESS;
      } else {
         return super.mobInteract(p_458952_, p_460677_);
      }
   }

   private void tryToTame(Player p_451972_) {
      if (this.random.nextInt(3) == 0) {
         this.tame(p_451972_);
         this.navigation.stop();
         this.level().broadcastEntityEvent(this, (byte)7);
      } else {
         this.level().broadcastEntityEvent(this, (byte)6);
      }

      this.playEatingSound();
   }

   @Override
   public boolean removeWhenFarAway(double p_451126_) {
      return true;
   }

   @Override
   public boolean hurtServer(ServerLevel p_456419_, DamageSource p_452812_, float p_452274_) {
      boolean flag = super.hurtServer(p_456419_, p_452812_, p_452274_);
      if (flag && p_452812_.getEntity() instanceof LivingEntity livingentity) {
         NautilusAi.setAngerTarget(p_456419_, this, livingentity);
      }

      return flag;
   }

   @Override
   public boolean canBeAffected(MobEffectInstance p_457976_) {
      return p_457976_.getEffect() == MobEffects.POISON ? false : super.canBeAffected(p_457976_);
   }

   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_461085_, DifficultyInstance p_450678_, EntitySpawnReason p_456724_, @Nullable SpawnGroupData p_456970_
   ) {
      RandomSource randomsource = p_461085_.getRandom();
      NautilusAi.initMemories(this, randomsource);
      return super.finalizeSpawn(p_461085_, p_450678_, p_456724_, p_456970_);
   }

   @Override
   protected Holder<SoundEvent> getEquipSound(EquipmentSlot p_456358_, ItemStack p_456297_, Equippable p_450719_) {
      if (p_456358_ == EquipmentSlot.SADDLE && this.isUnderWater()) {
         return SoundEvents.NAUTILUS_SADDLE_UNDERWATER_EQUIP;
      } else {
         return p_456358_ == EquipmentSlot.SADDLE ? SoundEvents.NAUTILUS_SADDLE_EQUIP : super.getEquipSound(p_456358_, p_456297_, p_450719_);
      }
   }

   public final int getInventorySize() {
      return AbstractMountInventoryMenu.getInventorySize(this.getInventoryColumns());
   }

   protected void createInventory() {
      SimpleContainer simplecontainer = this.inventory;
      this.inventory = new SimpleContainer(this.getInventorySize());
      if (simplecontainer != null) {
         int i = Math.min(simplecontainer.getContainerSize(), this.inventory.getContainerSize());

         for (int j = 0; j < i; j++) {
            ItemStack itemstack = simplecontainer.getItem(j);
            if (!itemstack.isEmpty()) {
               this.inventory.setItem(j, itemstack.copy());
            }
         }
      }
   }

   @Override
   public void openCustomInventoryScreen(Player p_460114_) {
      if (!this.level().isClientSide() && (!this.isVehicle() || this.hasPassenger(p_460114_)) && this.isTame()) {
         p_460114_.openNautilusInventory(this, this.inventory);
      }
   }

   @Override
   public @Nullable SlotAccess getSlot(int p_457574_) {
      int i = p_457574_ - 500;
      return i >= 0 && i < this.inventory.getContainerSize() ? this.inventory.getSlot(i) : super.getSlot(p_457574_);
   }

   public boolean hasInventoryChanged(Container p_455247_) {
      return this.inventory != p_455247_;
   }

   public int getInventoryColumns() {
      return 0;
   }

   protected boolean isMobControlled() {
      return this.getFirstPassenger() instanceof Mob;
   }

   protected boolean isAggravated() {
      return this.getBrain().hasMemoryValue(MemoryModuleType.ANGRY_AT) || this.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET);
   }
}
