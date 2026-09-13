package net.minecraft.world.entity.animal.allay;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Allay extends PathfinderMob implements InventoryCarrier, VibrationSystem {
   private static final Vec3i ITEM_PICKUP_REACH = new Vec3i(1, 1, 1);
   private static final int LIFTING_ITEM_ANIMATION_DURATION = 5;
   private static final float DANCING_LOOP_DURATION = 55.0F;
   private static final float SPINNING_ANIMATION_DURATION = 15.0F;
   private static final int DEFAULT_DUPLICATION_COOLDOWN = 0;
   private static final int DUPLICATION_COOLDOWN_TICKS = 6000;
   private static final int NUM_OF_DUPLICATION_HEARTS = 3;
   public static final int MAX_NOTEBLOCK_DISTANCE = 1024;
   private static final EntityDataAccessor<Boolean> DATA_DANCING = SynchedEntityData.defineId(Allay.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_CAN_DUPLICATE = SynchedEntityData.defineId(Allay.class, EntityDataSerializers.BOOLEAN);
   protected static final ImmutableList<SensorType<? extends Sensor<? super Allay>>> SENSOR_TYPES = ImmutableList.of(
      SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.HURT_BY, SensorType.NEAREST_ITEMS
   );
   protected static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
      MemoryModuleType.PATH,
      MemoryModuleType.LOOK_TARGET,
      MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
      MemoryModuleType.WALK_TARGET,
      MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
      MemoryModuleType.HURT_BY,
      MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM,
      MemoryModuleType.LIKED_PLAYER,
      MemoryModuleType.LIKED_NOTEBLOCK_POSITION,
      MemoryModuleType.LIKED_NOTEBLOCK_COOLDOWN_TICKS,
      MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS,
      MemoryModuleType.IS_PANICKING,
      new MemoryModuleType[0]
   );
   public static final ImmutableList<Float> THROW_SOUND_PITCHES = ImmutableList.of(
      0.5625F, 0.625F, 0.75F, 0.9375F, 1.0F, 1.0F, 1.125F, 1.25F, 1.5F, 1.875F, 2.0F, 2.25F, new Float[]{2.5F, 3.0F, 3.75F, 4.0F}
   );
   private final DynamicGameEventListener<VibrationSystem.Listener> dynamicVibrationListener;
   private VibrationSystem.Data vibrationData;
   private final VibrationSystem.User vibrationUser;
   private final DynamicGameEventListener<Allay.JukeboxListener> dynamicJukeboxListener;
   private final SimpleContainer inventory = new SimpleContainer(1);
   private @Nullable BlockPos jukeboxPos;
   private long duplicationCooldown = 0L;
   private float holdingItemAnimationTicks;
   private float holdingItemAnimationTicks0;
   private float dancingAnimationTicks;
   private float spinningAnimationTicks;
   private float spinningAnimationTicks0;

   public Allay(EntityType<? extends Allay> p_218310_, Level p_218311_) {
      super(p_218310_, p_218311_);
      this.moveControl = new FlyingMoveControl(this, 20, true);
      this.setCanPickUpLoot(this.canPickUpLoot());
      this.vibrationUser = new Allay.VibrationUser();
      this.vibrationData = new VibrationSystem.Data();
      this.dynamicVibrationListener = new DynamicGameEventListener<>(new VibrationSystem.Listener(this));
      this.dynamicJukeboxListener = new DynamicGameEventListener<>(
         new Allay.JukeboxListener(this.vibrationUser.getPositionSource(), GameEvent.JUKEBOX_PLAY.value().notificationRadius())
      );
   }

   @Override
   protected Brain.Provider<Allay> brainProvider() {
      return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
   }

   @Override
   protected Brain<?> makeBrain(Dynamic<?> p_218344_) {
      return AllayAi.makeBrain(this.brainProvider().makeBrain(p_218344_));
   }

   @Override
   public Brain<Allay> getBrain() {
      return (Brain<Allay>)super.getBrain();
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 20.0)
         .add(Attributes.FLYING_SPEED, 0.1F)
         .add(Attributes.MOVEMENT_SPEED, 0.1F)
         .add(Attributes.ATTACK_DAMAGE, 2.0);
   }

   @Override
   protected PathNavigation createNavigation(Level p_218342_) {
      FlyingPathNavigation flyingpathnavigation = new FlyingPathNavigation(this, p_218342_);
      flyingpathnavigation.setCanOpenDoors(false);
      flyingpathnavigation.setCanFloat(true);
      flyingpathnavigation.setRequiredPathLength(48.0F);
      return flyingpathnavigation;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_332593_) {
      super.defineSynchedData(p_332593_);
      p_332593_.define(DATA_DANCING, false);
      p_332593_.define(DATA_CAN_DUPLICATE, true);
   }

   @Override
   public void travel(Vec3 p_218382_) {
      this.travelFlying(p_218382_, this.getSpeed());
   }

   @Override
   public boolean hurtServer(ServerLevel p_365948_, DamageSource p_369485_, float p_365274_) {
      return this.isLikedPlayer(p_369485_.getEntity()) ? false : super.hurtServer(p_365948_, p_369485_, p_365274_);
   }

   @Override
   protected boolean considersEntityAsAlly(Entity p_376126_) {
      return this.isLikedPlayer(p_376126_) || super.considersEntityAsAlly(p_376126_);
   }

   private boolean isLikedPlayer(@Nullable Entity p_377947_) {
      if (!(p_377947_ instanceof Player player)) {
         return false;
      } else {
         Optional<UUID> optional = this.getBrain().getMemory(MemoryModuleType.LIKED_PLAYER);
         return optional.isPresent() && player.getUUID().equals(optional.get());
      }
   }

   @Override
   protected void playStepSound(BlockPos p_218364_, BlockState p_218365_) {
   }

   @Override
   protected void checkFallDamage(double p_218316_, boolean p_218317_, BlockState p_218318_, BlockPos p_218319_) {
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return this.hasItemInSlot(EquipmentSlot.MAINHAND) ? SoundEvents.ALLAY_AMBIENT_WITH_ITEM : SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_218369_) {
      return SoundEvents.ALLAY_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.ALLAY_DEATH;
   }

   @Override
   protected float getSoundVolume() {
      return 0.4F;
   }

   @Override
   protected void customServerAiStep(ServerLevel p_367271_) {
      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("allayBrain");
      this.getBrain().tick(p_367271_, this);
      profilerfiller.pop();
      profilerfiller.push("allayActivityUpdate");
      AllayAi.updateActivity(this);
      profilerfiller.pop();
      super.customServerAiStep(p_367271_);
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (!this.level().isClientSide() && this.isAlive() && this.tickCount % 10 == 0) {
         this.heal(1.0F);
      }

      if (this.isDancing() && this.shouldStopDancing() && this.tickCount % 20 == 0) {
         this.setDancing(false);
         this.jukeboxPos = null;
      }

      this.updateDuplicationCooldown();
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level().isClientSide()) {
         this.holdingItemAnimationTicks0 = this.holdingItemAnimationTicks;
         if (this.hasItemInHand()) {
            this.holdingItemAnimationTicks = Mth.clamp(this.holdingItemAnimationTicks + 1.0F, 0.0F, 5.0F);
         } else {
            this.holdingItemAnimationTicks = Mth.clamp(this.holdingItemAnimationTicks - 1.0F, 0.0F, 5.0F);
         }

         if (this.isDancing()) {
            this.dancingAnimationTicks++;
            this.spinningAnimationTicks0 = this.spinningAnimationTicks;
            if (this.isSpinning()) {
               this.spinningAnimationTicks++;
            } else {
               this.spinningAnimationTicks--;
            }

            this.spinningAnimationTicks = Mth.clamp(this.spinningAnimationTicks, 0.0F, 15.0F);
         } else {
            this.dancingAnimationTicks = 0.0F;
            this.spinningAnimationTicks = 0.0F;
            this.spinningAnimationTicks0 = 0.0F;
         }
      } else {
         VibrationSystem.Ticker.tick(this.level(), this.vibrationData, this.vibrationUser);
         if (this.isPanicking()) {
            this.setDancing(false);
         }
      }
   }

   @Override
   public boolean canPickUpLoot() {
      return !this.isOnPickupCooldown() && this.hasItemInHand();
   }

   public boolean hasItemInHand() {
      return !this.getItemInHand(InteractionHand.MAIN_HAND).isEmpty();
   }

   @Override
   protected boolean canDispenserEquipIntoSlot(EquipmentSlot p_360851_) {
      return false;
   }

   private boolean isOnPickupCooldown() {
      return this.getBrain().checkMemory(MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS, MemoryStatus.VALUE_PRESENT);
   }

   @Override
   protected InteractionResult mobInteract(Player p_218361_, InteractionHand p_218362_) {
      ItemStack itemstack = p_218361_.getItemInHand(p_218362_);
      ItemStack itemstack1 = this.getItemInHand(InteractionHand.MAIN_HAND);
      if (this.isDancing() && itemstack.is(ItemTags.DUPLICATES_ALLAYS) && this.canDuplicate()) {
         this.duplicateAllay();
         this.level().broadcastEntityEvent(this, (byte)18);
         this.level().playSound(p_218361_, this, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 2.0F, 1.0F);
         this.removeInteractionItem(p_218361_, itemstack);
         return InteractionResult.SUCCESS;
      }

      if (itemstack1.isEmpty() && !itemstack.isEmpty()) {
         ItemStack itemstack3 = itemstack.copyWithCount(1);
         this.setItemInHand(InteractionHand.MAIN_HAND, itemstack3);
         this.removeInteractionItem(p_218361_, itemstack);
         this.level().playSound(p_218361_, this, SoundEvents.ALLAY_ITEM_GIVEN, SoundSource.NEUTRAL, 2.0F, 1.0F);
         this.getBrain().setMemory(MemoryModuleType.LIKED_PLAYER, p_218361_.getUUID());
         return InteractionResult.SUCCESS;
      }

      if (!itemstack1.isEmpty() && p_218362_ == InteractionHand.MAIN_HAND && itemstack.isEmpty()) {
         this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
         this.level().playSound(p_218361_, this, SoundEvents.ALLAY_ITEM_TAKEN, SoundSource.NEUTRAL, 2.0F, 1.0F);
         this.swing(InteractionHand.MAIN_HAND);

         for (ItemStack itemstack2 : this.getInventory().removeAllItems()) {
            BehaviorUtils.throwItem(this, itemstack2, this.position());
         }

         this.getBrain().eraseMemory(MemoryModuleType.LIKED_PLAYER);
         p_218361_.addItem(itemstack1);
         return InteractionResult.SUCCESS;
      } else {
         return super.mobInteract(p_218361_, p_218362_);
      }
   }

   public void setJukeboxPlaying(BlockPos p_240102_, boolean p_240103_) {
      if (p_240103_) {
         if (!this.isDancing()) {
            this.jukeboxPos = p_240102_;
            this.setDancing(true);
         }
      } else if (p_240102_.equals(this.jukeboxPos) || this.jukeboxPos == null) {
         this.jukeboxPos = null;
         this.setDancing(false);
      }
   }

   @Override
   public SimpleContainer getInventory() {
      return this.inventory;
   }

   @Override
   protected Vec3i getPickupReach() {
      return ITEM_PICKUP_REACH;
   }

   @Override
   public boolean wantsToPickUp(ServerLevel p_363882_, ItemStack p_218387_) {
      ItemStack itemstack = this.getItemInHand(InteractionHand.MAIN_HAND);
      return !itemstack.isEmpty()
         && p_363882_.getGameRules().get(GameRules.MOB_GRIEFING)
         && this.inventory.canAddItem(p_218387_)
         && this.allayConsidersItemEqual(itemstack, p_218387_);
   }

   private boolean allayConsidersItemEqual(ItemStack p_252278_, ItemStack p_250405_) {
      return ItemStack.isSameItem(p_252278_, p_250405_) && !this.hasNonMatchingPotion(p_252278_, p_250405_);
   }

   private boolean hasNonMatchingPotion(ItemStack p_248762_, ItemStack p_250839_) {
      PotionContents potioncontents = p_248762_.get(DataComponents.POTION_CONTENTS);
      PotionContents potioncontents1 = p_250839_.get(DataComponents.POTION_CONTENTS);
      return !Objects.equals(potioncontents, potioncontents1);
   }

   @Override
   protected void pickUpItem(ServerLevel p_363025_, ItemEntity p_218359_) {
      InventoryCarrier.pickUpItem(p_363025_, this, this, p_218359_);
   }

   @Override
   public boolean isFlapping() {
      return !this.onGround();
   }

   @Override
   public void updateDynamicGameEventListener(BiConsumer<DynamicGameEventListener<?>, ServerLevel> p_218348_) {
      if (this.level() instanceof ServerLevel serverlevel) {
         p_218348_.accept(this.dynamicVibrationListener, serverlevel);
         p_218348_.accept(this.dynamicJukeboxListener, serverlevel);
      }
   }

   public boolean isDancing() {
      return this.entityData.get(DATA_DANCING);
   }

   public void setDancing(boolean p_240178_) {
      if (!this.level().isClientSide() && this.isEffectiveAi() && (!p_240178_ || !this.isPanicking())) {
         this.entityData.set(DATA_DANCING, p_240178_);
      }
   }

   private boolean shouldStopDancing() {
      return this.jukeboxPos == null
         || !this.jukeboxPos.closerToCenterThan(this.position(), GameEvent.JUKEBOX_PLAY.value().notificationRadius())
         || !this.level().getBlockState(this.jukeboxPos).is(Blocks.JUKEBOX);
   }

   public float getHoldingItemAnimationProgress(float p_218395_) {
      return Mth.lerp(p_218395_, this.holdingItemAnimationTicks0, this.holdingItemAnimationTicks) / 5.0F;
   }

   public boolean isSpinning() {
      float f = this.dancingAnimationTicks % 55.0F;
      return f < 15.0F;
   }

   public float getSpinningProgress(float p_240057_) {
      return Mth.lerp(p_240057_, this.spinningAnimationTicks0, this.spinningAnimationTicks) / 15.0F;
   }

   @Override
   public boolean equipmentHasChanged(ItemStack p_249825_, ItemStack p_251595_) {
      return !this.allayConsidersItemEqual(p_249825_, p_251595_);
   }

   @Override
   protected void dropEquipment(ServerLevel p_362296_) {
      super.dropEquipment(p_362296_);
      this.inventory.removeAllItems().forEach(p_359201_ -> this.spawnAtLocation(p_362296_, p_359201_));
      ItemStack itemstack = this.getItemBySlot(EquipmentSlot.MAINHAND);
      if (!itemstack.isEmpty() && !EnchantmentHelper.has(itemstack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
         this.spawnAtLocation(p_362296_, itemstack);
         this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
      }
   }

   @Override
   public boolean removeWhenFarAway(double p_218384_) {
      return false;
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_406889_) {
      super.addAdditionalSaveData(p_406889_);
      this.writeInventoryToTag(p_406889_);
      p_406889_.store("listener", VibrationSystem.Data.CODEC, this.vibrationData);
      p_406889_.putLong("DuplicationCooldown", this.duplicationCooldown);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_406830_) {
      super.readAdditionalSaveData(p_406830_);
      this.readInventoryFromTag(p_406830_);
      this.vibrationData = p_406830_.<VibrationSystem.Data>read("listener", VibrationSystem.Data.CODEC).orElseGet(VibrationSystem.Data::new);
      this.setDuplicationCooldown(p_406830_.getIntOr("DuplicationCooldown", 0));
   }

   @Override
   protected boolean shouldStayCloseToLeashHolder() {
      return false;
   }

   private void updateDuplicationCooldown() {
      if (!this.level().isClientSide() && this.duplicationCooldown > 0L) {
         this.setDuplicationCooldown(this.duplicationCooldown - 1L);
      }
   }

   private void setDuplicationCooldown(long p_396336_) {
      this.duplicationCooldown = p_396336_;
      this.entityData.set(DATA_CAN_DUPLICATE, p_396336_ == 0L);
   }

   private void duplicateAllay() {
      Allay allay = EntityType.ALLAY.create(this.level(), EntitySpawnReason.BREEDING);
      if (allay != null) {
         allay.snapTo(this.position());
         allay.setPersistenceRequired();
         allay.resetDuplicationCooldown();
         this.resetDuplicationCooldown();
         this.level().addFreshEntity(allay);
      }
   }

   private void resetDuplicationCooldown() {
      this.setDuplicationCooldown(6000L);
   }

   private boolean canDuplicate() {
      return this.entityData.get(DATA_CAN_DUPLICATE);
   }

   private void removeInteractionItem(Player p_239359_, ItemStack p_239360_) {
      p_239360_.consume(1, p_239359_);
   }

   @Override
   public Vec3 getLeashOffset() {
      return new Vec3(0.0, this.getEyeHeight() * 0.6, this.getBbWidth() * 0.1);
   }

   @Override
   public void handleEntityEvent(byte p_239347_) {
      if (p_239347_ == 18) {
         for (int i = 0; i < 3; i++) {
            this.spawnHeartParticle();
         }
      } else {
         super.handleEntityEvent(p_239347_);
      }
   }

   private void spawnHeartParticle() {
      double d0 = this.random.nextGaussian() * 0.02;
      double d1 = this.random.nextGaussian() * 0.02;
      double d2 = this.random.nextGaussian() * 0.02;
      this.level().addParticle(ParticleTypes.HEART, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), d0, d1, d2);
   }

   @Override
   public VibrationSystem.Data getVibrationData() {
      return this.vibrationData;
   }

   @Override
   public VibrationSystem.User getVibrationUser() {
      return this.vibrationUser;
   }

   class JukeboxListener implements GameEventListener {
      private final PositionSource listenerSource;
      private final int listenerRadius;

      public JukeboxListener(final PositionSource p_239448_, final int p_239449_) {
         this.listenerSource = p_239448_;
         this.listenerRadius = p_239449_;
      }

      @Override
      public PositionSource getListenerSource() {
         return this.listenerSource;
      }

      @Override
      public int getListenerRadius() {
         return this.listenerRadius;
      }

      @Override
      public boolean handleGameEvent(ServerLevel p_250009_, Holder<GameEvent> p_333132_, GameEvent.Context p_249478_, Vec3 p_250852_) {
         if (p_333132_.is(GameEvent.JUKEBOX_PLAY)) {
            Allay.this.setJukeboxPlaying(BlockPos.containing(p_250852_), true);
            return true;
         } else if (p_333132_.is(GameEvent.JUKEBOX_STOP_PLAY)) {
            Allay.this.setJukeboxPlaying(BlockPos.containing(p_250852_), false);
            return true;
         } else {
            return false;
         }
      }
   }

   class VibrationUser implements VibrationSystem.User {
      private static final int VIBRATION_EVENT_LISTENER_RANGE = 16;
      private final PositionSource positionSource = new EntityPositionSource(Allay.this, Allay.this.getEyeHeight());

      @Override
      public int getListenerRadius() {
         return 16;
      }

      @Override
      public PositionSource getPositionSource() {
         return this.positionSource;
      }

      @Override
      public boolean canReceiveVibration(ServerLevel p_282038_, BlockPos p_283385_, Holder<GameEvent> p_334911_, GameEvent.Context p_282208_) {
         if (Allay.this.isNoAi()) {
            return false;
         }

         Optional<GlobalPos> optional = Allay.this.getBrain().getMemory(MemoryModuleType.LIKED_NOTEBLOCK_POSITION);
         if (optional.isEmpty()) {
            return true;
         }

         GlobalPos globalpos = optional.get();
         return globalpos.isCloseEnough(p_282038_.dimension(), Allay.this.blockPosition(), 1024) && globalpos.pos().equals(p_283385_);
      }

      @Override
      public void onReceiveVibration(
         ServerLevel p_281422_, BlockPos p_281449_, Holder<GameEvent> p_333452_, @Nullable Entity p_281794_, @Nullable Entity p_281864_, float p_281642_
      ) {
         if (p_333452_.is(GameEvent.NOTE_BLOCK_PLAY)) {
            AllayAi.hearNoteblock(Allay.this, new BlockPos(p_281449_));
         }
      }

      @Override
      public TagKey<GameEvent> getListenableEvents() {
         return GameEventTags.ALLAY_CAN_LISTEN;
      }
   }
}
