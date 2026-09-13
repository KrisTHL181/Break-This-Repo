package net.minecraft.world.entity.animal.nautilus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
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

public abstract class AbstractNautilus extends TamableAnimal implements PlayerRideableJumping, HasCustomInventoryScreen {
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

   protected AbstractNautilus(final EntityType<? extends AbstractNautilus> type, final Level level) {
      super(type, level);
      this.moveControl = new SmoothSwimmingMoveControl<>(this, 85, 10, 0.011F, 0.0F, true);
      this.lookControl = new SmoothSwimmingLookControl(this, 10);
      this.setPathfindingMalus(PathType.WATER, 0.0F);
      this.createInventory();
   }

   @Override
   public boolean isFood(final ItemStack itemStack) {
      return !this.isTame() && !this.isBaby() ? itemStack.is(ItemTags.NAUTILUS_TAMING_ITEMS) : itemStack.is(ItemTags.NAUTILUS_FOOD);
   }

   @Override
   protected void usePlayerItem(final Player player, final InteractionHand hand, final ItemStack itemStack) {
      if (itemStack.is(ItemTags.NAUTILUS_BUCKET_FOOD)) {
         player.setItemInHand(hand, ItemUtils.createFilledResult(itemStack, player, new ItemStack(Items.WATER_BUCKET)));
      } else {
         super.usePlayerItem(player, hand, itemStack);
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
   protected PathNavigation createNavigation(final Level level) {
      return new WaterBoundPathNavigation(this, level);
   }

   @Override
   public float getWalkTargetValue(final BlockPos pos, final LevelReader level) {
      return 0.0F;
   }

   public static boolean checkNautilusSpawnRules(
      final EntityType<? extends AbstractNautilus> type,
      final LevelAccessor level,
      final EntitySpawnReason spawnReason,
      final BlockPos pos,
      final RandomSource random
   ) {
      int seaLevel = level.getSeaLevel();
      int minSpawnLevel = seaLevel - 25;
      return pos.getY() >= minSpawnLevel
         && pos.getY() <= seaLevel - 5
         && level.getFluidState(pos.below()).is(FluidTags.WATER)
         && level.getBlockState(pos.above()).is(Blocks.WATER);
   }

   @Override
   public boolean checkSpawnObstruction(final LevelReader level) {
      return level.isUnobstructed(this);
   }

   @Override
   public boolean canUseSlot(final EquipmentSlot slot) {
      return slot != EquipmentSlot.SADDLE && slot != EquipmentSlot.BODY ? super.canUseSlot(slot) : this.isAlive() && !this.isBaby() && this.isTame();
   }

   @Override
   protected boolean canDispenserEquipIntoSlot(final EquipmentSlot slot) {
      return slot == EquipmentSlot.BODY || slot == EquipmentSlot.SADDLE || super.canDispenserEquipIntoSlot(slot);
   }

   @Override
   protected boolean canAddPassenger(final Entity passenger) {
      return !this.isVehicle();
   }

   @Override
   public @Nullable LivingEntity getControllingPassenger() {
      return this.isSaddled() && this.getFirstPassenger() instanceof Player player ? player : super.getControllingPassenger();
   }

   @Override
   protected Vec3 getRiddenInput(final Player controller, final Vec3 selfInput) {
      float strafe = controller.xxa;
      float forward = 0.0F;
      float up = 0.0F;
      if (controller.zza != 0.0F) {
         float forwardLook = Mth.cos(controller.getXRot() * (float) (Math.PI / 180.0));
         float upLook = -Mth.sin(controller.getXRot() * (float) (Math.PI / 180.0));
         if (controller.zza < 0.0F) {
            forwardLook *= -0.5F;
            upLook *= -0.5F;
         }

         up = upLook;
         forward = forwardLook;
      }

      return new Vec3(strafe, up, forward);
   }

   protected Vec2 getRiddenRotation(final LivingEntity controller) {
      return new Vec2(controller.getXRot() * 0.5F, controller.getYRot());
   }

   @Override
   protected void tickRidden(final Player controller, final Vec3 riddenInput) {
      super.tickRidden(controller, riddenInput);
      Vec2 rotation = this.getRiddenRotation(controller);
      float yRot = this.getYRot();
      float diff = Mth.wrapDegrees(rotation.y - yRot);
      float turnSpeed = 0.5F;
      yRot += diff * 0.5F;
      this.setRot(yRot, rotation.x);
      this.yRotO = this.yBodyRot = this.yHeadRot = yRot;
      if (this.isLocalInstanceAuthoritative()) {
         if (this.playerJumpPendingScale > 0.0F && !this.isJumping()) {
            this.executeRidersJump(this.playerJumpPendingScale, controller);
         }

         this.playerJumpPendingScale = 0.0F;
      }
   }

   @Override
   protected void travelInWater(final Vec3 input, final double baseGravity, final boolean isFalling, final double oldY) {
      float speed = this.getSpeed();
      this.moveRelative(speed, input);
      this.move(MoverType.SELF, this.getDeltaMovement());
      this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
   }

   @Override
   protected float getRiddenSpeed(final Player controller) {
      return this.isInWater()
         ? 0.0325F * (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED)
         : 0.02F * (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED);
   }

   protected void doPlayerRide(final Player player) {
      if (!this.level().isClientSide()) {
         player.startRiding(this);
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
         int radius = this.getNautilusRestrictionRadius();
         if (!this.hasHome() || !this.getHomePosition().closerThan(this.blockPosition(), radius + 8) || radius != this.getHomeRadius()) {
            this.setHomeTo(this.blockPosition(), radius);
         }
      }
   }

   @Override
   protected void customServerAiStep(final ServerLevel level) {
      this.checkRestriction();
      super.customServerAiStep(level);
   }

   private void applyEffects(final Level level) {
      if (this.getFirstPassenger() instanceof Player player) {
         boolean hasEffect = player.hasEffect(MobEffects.BREATH_OF_THE_NAUTILUS);
         boolean shouldRefresh = level.getGameTime() % 40L == 0L;
         if (!hasEffect || shouldRefresh) {
            player.addEffect(new MobEffectInstance(MobEffects.BREATH_OF_THE_NAUTILUS, 60, 0, true, true, true));
         }
      }
   }

   private void spawnBubbles() {
      double speed = this.getDeltaMovement().length();
      double bubbleProbability = Mth.clamp(speed * 2.0, 0.15F, 1.0);
      if (this.random.nextFloat() < bubbleProbability) {
         float yRot = this.getYRot();
         float xRot = Mth.clamp(this.getXRot(), -10.0F, 10.0F);
         Vec3 mouthDirectionVector = this.calculateViewVector(xRot, yRot);
         double spread = this.random.nextDouble() * 0.8 * (1.0 + speed);
         double dx = (this.random.nextFloat() - 0.5) * spread;
         double dy = (this.random.nextFloat() - 0.5) * spread;
         double dz = (this.random.nextFloat() - 0.5) * spread;
         this.level()
            .addParticle(
               ParticleTypes.BUBBLE,
               this.getX() - mouthDirectionVector.x * 1.1,
               this.getY() - mouthDirectionVector.y + 0.25,
               this.getZ() - mouthDirectionVector.z * 1.1,
               dx,
               dy,
               dz
            );
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
   public void onPlayerJump(final int jumpAmount) {
      if (this.isSaddled() && this.dashCooldown <= 0) {
         this.playerJumpPendingScale = this.getPlayerJumpPendingScale(jumpAmount);
      }
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(DASH, false);
   }

   public boolean isDashing() {
      return this.entityData.get(DASH);
   }

   public void setDashing(final boolean isDashing) {
      this.entityData.set(DASH, isDashing);
   }

   protected void executeRidersJump(final float amount, final Player controller) {
      this.addDeltaMovement(
         controller.getLookAngle()
            .scale((this.isInWater() ? 1.2F : 0.5F) * amount * this.getAttributeValue(Attributes.MOVEMENT_SPEED) * this.getBlockSpeedFactor())
      );
      this.dashCooldown = 40;
      this.setDashing(true);
      this.needsSync = true;
   }

   @Override
   public void handleStartJump(final int jumpScale) {
      this.makeSound(this.getDashSound());
      this.gameEvent(GameEvent.ENTITY_ACTION);
      this.setDashing(true);
   }

   @Override
   public int getJumpCooldown() {
      return this.dashCooldown;
   }

   @Override
   public void onSyncedDataUpdated(final EntityDataAccessor<?> accessor) {
      if (!this.firstTick && DASH.equals(accessor)) {
         this.dashCooldown = this.dashCooldown == 0 ? 40 : this.dashCooldown;
      }

      super.onSyncedDataUpdated(accessor);
   }

   @Override
   public void handleStopJump() {
   }

   @Override
   protected void playStepSound(final BlockPos pos, final BlockState blockState) {
   }

   protected @Nullable SoundEvent getDashSound() {
      return null;
   }

   protected @Nullable SoundEvent getDashReadySound() {
      return null;
   }

   @Override
   public InteractionResult interact(final Player player, final InteractionHand hand, final Vec3 location) {
      this.setPersistenceRequired();
      return super.interact(player, hand, location);
   }

   @Override
   public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      if (this.isBaby()) {
         return super.mobInteract(player, hand);
      }

      if (this.isTame() && player.isSecondaryUseActive()) {
         this.openCustomInventoryScreen(player);
         return InteractionResult.SUCCESS;
      }

      if (!itemStack.isEmpty()) {
         if (!this.level().isClientSide() && !this.isTame() && this.isFood(itemStack)) {
            this.usePlayerItem(player, hand, itemStack);
            this.tryToTame(player);
            return InteractionResult.SUCCESS_SERVER;
         }

         if (this.isFood(itemStack) && this.getHealth() < this.getMaxHealth()) {
            this.feed(player, hand, itemStack, 2.0F, 1.0F);
            return InteractionResult.SUCCESS;
         }

         InteractionResult interactionResult = itemStack.interactLivingEntity(player, this, hand);
         if (interactionResult.consumesAction()) {
            return interactionResult;
         }
      }

      if (this.isTame() && !player.isSecondaryUseActive() && !this.isFood(itemStack)) {
         this.doPlayerRide(player);
         return InteractionResult.SUCCESS;
      } else {
         return super.mobInteract(player, hand);
      }
   }

   private void tryToTame(final Player player) {
      if (this.random.nextInt(3) == 0) {
         this.tame(player);
         this.navigation.stop();
         this.level().broadcastEntityEvent(this, (byte)7);
      } else {
         this.level().broadcastEntityEvent(this, (byte)6);
      }

      this.playEatingSound();
   }

   @Override
   public boolean removeWhenFarAway(final double distSqr) {
      return true;
   }

   @Override
   public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
      boolean wasHurt = super.hurtServer(level, source, damage);
      if (wasHurt && source.getEntity() instanceof LivingEntity sourceEntity) {
         NautilusAi.setAngerTarget(level, this, sourceEntity);
      }

      return wasHurt;
   }

   @Override
   public boolean canBeAffected(final MobEffectInstance newEffect) {
      return newEffect.getEffect() == MobEffects.POISON ? false : super.canBeAffected(newEffect);
   }

   @Override
   public SpawnGroupData finalizeSpawn(
      final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, final @Nullable SpawnGroupData groupData
   ) {
      RandomSource random = level.getRandom();
      NautilusAi.initMemories(this, random);
      return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
   }

   @Override
   protected Holder<SoundEvent> getEquipSound(final EquipmentSlot slot, final ItemStack stack, final Equippable equippable) {
      if (slot == EquipmentSlot.SADDLE && this.isUnderWater()) {
         return SoundEvents.NAUTILUS_SADDLE_UNDERWATER_EQUIP;
      } else {
         return slot == EquipmentSlot.SADDLE ? SoundEvents.NAUTILUS_SADDLE_EQUIP : super.getEquipSound(slot, stack, equippable);
      }
   }

   public final int getInventorySize() {
      return AbstractMountInventoryMenu.getInventorySize(this.getInventoryColumns());
   }

   protected void createInventory() {
      SimpleContainer old = this.inventory;
      this.inventory = new SimpleContainer(this.getInventorySize());
      if (old != null) {
         int max = Math.min(old.getContainerSize(), this.inventory.getContainerSize());

         for (int slot = 0; slot < max; slot++) {
            ItemStack itemStack = old.getItem(slot);
            if (!itemStack.isEmpty()) {
               this.inventory.setItem(slot, itemStack.copy());
            }
         }
      }
   }

   @Override
   public void openCustomInventoryScreen(final Player player) {
      if (!this.level().isClientSide() && (!this.isVehicle() || this.hasPassenger(player)) && this.isTame()) {
         player.openNautilusInventory(this, this.inventory);
      }
   }

   @Override
   public @Nullable SlotAccess getSlot(final int slot) {
      int inventorySlot = slot - 500;
      return inventorySlot >= 0 && inventorySlot < this.inventory.getContainerSize() ? this.inventory.getSlot(inventorySlot) : super.getSlot(slot);
   }

   public boolean hasInventoryChanged(final Container oldInventory) {
      return this.inventory != oldInventory;
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

   @Override
   public boolean requiresCustomPersistence() {
      return super.requiresCustomPersistence() || this.isTame();
   }
}
