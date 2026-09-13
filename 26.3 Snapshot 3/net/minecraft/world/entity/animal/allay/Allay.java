package net.minecraft.world.entity.animal.allay;

import com.google.common.collect.ImmutableList;
import java.util.List;
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
import net.minecraft.world.entity.EntityTypes;
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
   private static final Brain.Provider<Allay> BRAIN_PROVIDER = Brain.provider(
      List.of(MemoryModuleType.LIKED_PLAYER, MemoryModuleType.LIKED_NOTEBLOCK_POSITION, MemoryModuleType.LIKED_NOTEBLOCK_COOLDOWN_TICKS),
      List.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.HURT_BY, SensorType.NEAREST_ITEMS),
      var0 -> AllayAi.getActivities()
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

   public Allay(final EntityType<? extends Allay> type, final Level level) {
      super(type, level);
      this.moveControl = new FlyingMoveControl<>(this, 20, true);
      this.setCanPickUpLoot(this.canPickUpLoot());
      this.vibrationUser = new Allay.VibrationUser();
      this.vibrationData = new VibrationSystem.Data();
      this.dynamicVibrationListener = new DynamicGameEventListener<>(new VibrationSystem.Listener(this));
      this.dynamicJukeboxListener = new DynamicGameEventListener<>(
         new Allay.JukeboxListener(this.vibrationUser.getPositionSource(), GameEvent.JUKEBOX_PLAY.value().notificationRadius())
      );
   }

   @Override
   protected Brain<Allay> makeBrain(final Brain.Packed packedBrain) {
      return BRAIN_PROVIDER.makeBrain(this, packedBrain);
   }

   @Override
   public Brain<Allay> getBrain() {
      return super.getBrain();
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 20.0)
         .add(Attributes.FLYING_SPEED, 0.1F)
         .add(Attributes.MOVEMENT_SPEED, 0.1F)
         .add(Attributes.ATTACK_DAMAGE, 2.0);
   }

   @Override
   protected PathNavigation createNavigation(final Level level) {
      FlyingPathNavigation flyingPathNavigation = new FlyingPathNavigation(this, level);
      flyingPathNavigation.setCanOpenDoors(false);
      flyingPathNavigation.setCanFloat(true);
      flyingPathNavigation.setRequiredPathLength(48.0F);
      return flyingPathNavigation;
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(DATA_DANCING, false);
      entityData.define(DATA_CAN_DUPLICATE, true);
   }

   @Override
   public void travel(final Vec3 input) {
      this.travelFlying(input, this.getSpeed());
   }

   @Override
   public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
      return this.isLikedPlayer(source.getEntity()) ? false : super.hurtServer(level, source, damage);
   }

   @Override
   protected boolean considersEntityAsAlly(final Entity other) {
      return this.isLikedPlayer(other) || super.considersEntityAsAlly(other);
   }

   private boolean isLikedPlayer(final @Nullable Entity other) {
      if (!(other instanceof Player player)) {
         return false;
      } else {
         Optional<UUID> likedPlayer = this.getBrain().getMemory(MemoryModuleType.LIKED_PLAYER);
         return likedPlayer.isPresent() && player.getUUID().equals(likedPlayer.get());
      }
   }

   @Override
   protected void playStepSound(final BlockPos pos, final BlockState blockState) {
   }

   @Override
   protected void checkFallDamage(final double ya, final boolean onGround, final BlockState onState, final BlockPos pos) {
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return this.hasItemInSlot(EquipmentSlot.MAINHAND) ? SoundEvents.ALLAY_AMBIENT_WITH_ITEM : SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
   }

   @Override
   protected SoundEvent getHurtSound(final DamageSource source) {
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
   protected void customServerAiStep(final ServerLevel level) {
      ProfilerFiller profiler = Profiler.get();
      profiler.push("allayBrain");
      this.getBrain().tick(level, this);
      profiler.pop();
      profiler.push("allayActivityUpdate");
      AllayAi.updateActivity(this);
      profiler.pop();
      super.customServerAiStep(level);
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
   protected boolean canDispenserEquipIntoSlot(final EquipmentSlot slot) {
      return false;
   }

   private boolean isOnPickupCooldown() {
      return this.getBrain().checkMemory(MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS, MemoryStatus.VALUE_PRESENT);
   }

   @Override
   protected InteractionResult mobInteract(final Player player, final InteractionHand hand) {
      ItemStack interactionItem = player.getItemInHand(hand);
      ItemStack itemInHand = this.getItemInHand(InteractionHand.MAIN_HAND);
      if (this.isDancing() && interactionItem.is(ItemTags.DUPLICATES_ALLAYS) && this.canDuplicate()) {
         this.duplicateAllay();
         this.level().broadcastEntityEvent(this, (byte)18);
         this.level().playSound(player, this, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 2.0F, 1.0F);
         this.removeInteractionItem(player, interactionItem);
         return InteractionResult.SUCCESS;
      }

      if (itemInHand.isEmpty() && !interactionItem.isEmpty()) {
         ItemStack itemToGive = interactionItem.copyWithCount(1);
         this.setItemInHand(InteractionHand.MAIN_HAND, itemToGive);
         this.removeInteractionItem(player, interactionItem);
         this.level().playSound(player, this, SoundEvents.ALLAY_ITEM_GIVEN, SoundSource.NEUTRAL, 2.0F, 1.0F);
         this.getBrain().setMemory(MemoryModuleType.LIKED_PLAYER, player.getUUID());
         return InteractionResult.SUCCESS;
      }

      if (!itemInHand.isEmpty() && hand == InteractionHand.MAIN_HAND && interactionItem.isEmpty()) {
         this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
         this.level().playSound(player, this, SoundEvents.ALLAY_ITEM_TAKEN, SoundSource.NEUTRAL, 2.0F, 1.0F);
         this.swing(InteractionHand.MAIN_HAND);

         for (ItemStack itemStack : this.getInventory().removeAllItems()) {
            BehaviorUtils.throwItem(this, itemStack, this.position());
         }

         this.getBrain().eraseMemory(MemoryModuleType.LIKED_PLAYER);
         player.addItem(itemInHand);
         return InteractionResult.SUCCESS;
      } else {
         return super.mobInteract(player, hand);
      }
   }

   public void setJukeboxPlaying(final BlockPos jukebox, final boolean isPlaying) {
      if (isPlaying) {
         if (!this.isDancing()) {
            this.jukeboxPos = jukebox;
            this.setDancing(true);
         }
      } else if (jukebox.equals(this.jukeboxPos) || this.jukeboxPos == null) {
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
   public boolean wantsToPickUp(final ServerLevel level, final ItemStack itemStack) {
      ItemStack itemInHand = this.getItemInHand(InteractionHand.MAIN_HAND);
      return !itemInHand.isEmpty()
         && level.getGameRules().get(GameRules.MOB_GRIEFING)
         && this.inventory.canAddItem(itemStack)
         && this.allayConsidersItemEqual(itemInHand, itemStack);
   }

   private boolean allayConsidersItemEqual(final ItemStack item1, final ItemStack item2) {
      return ItemStack.isSameItem(item1, item2) && !this.hasNonMatchingPotion(item1, item2);
   }

   private boolean hasNonMatchingPotion(final ItemStack itemInHand, final ItemStack pickupItem) {
      PotionContents potionInHand = itemInHand.get(DataComponents.POTION_CONTENTS);
      PotionContents potionInPickupItem = pickupItem.get(DataComponents.POTION_CONTENTS);
      return !Objects.equals(potionInHand, potionInPickupItem);
   }

   @Override
   protected void pickUpItem(final ServerLevel level, final ItemEntity entity) {
      InventoryCarrier.pickUpItem(level, this, this, entity);
   }

   @Override
   public boolean isFlapping() {
      return !this.onGround();
   }

   @Override
   public void updateDynamicGameEventListener(final BiConsumer<DynamicGameEventListener<?>, ServerLevel> action) {
      if (this.level() instanceof ServerLevel serverLevel) {
         action.accept(this.dynamicVibrationListener, serverLevel);
         action.accept(this.dynamicJukeboxListener, serverLevel);
      }
   }

   public boolean isDancing() {
      return this.entityData.get(DATA_DANCING);
   }

   public void setDancing(final boolean isDancing) {
      if (!this.level().isClientSide() && this.isEffectiveAi() && (!isDancing || !this.isPanicking())) {
         this.entityData.set(DATA_DANCING, isDancing);
      }
   }

   private boolean shouldStopDancing() {
      return this.jukeboxPos == null
         || !this.jukeboxPos.closerToCenterThan(this.position(), GameEvent.JUKEBOX_PLAY.value().notificationRadius())
         || !this.level().getBlockState(this.jukeboxPos).is(Blocks.JUKEBOX);
   }

   public float getHoldingItemAnimationProgress(final float a) {
      return Mth.lerp(a, this.holdingItemAnimationTicks0, this.holdingItemAnimationTicks) / 5.0F;
   }

   public boolean isSpinning() {
      float spinningProgress = this.dancingAnimationTicks % 55.0F;
      return spinningProgress < 15.0F;
   }

   public float getSpinningProgress(final float a) {
      return Mth.lerp(a, this.spinningAnimationTicks0, this.spinningAnimationTicks) / 15.0F;
   }

   @Override
   public boolean equipmentHasChanged(final ItemStack previous, final ItemStack current) {
      return !this.allayConsidersItemEqual(previous, current);
   }

   @Override
   protected void dropEquipment(final ServerLevel level) {
      super.dropEquipment(level);
      this.inventory.removeAllItems().forEach(stack -> this.spawnAtLocation(level, stack));
      ItemStack itemStack = this.getItemBySlot(EquipmentSlot.MAINHAND);
      if (!itemStack.isEmpty() && !EnchantmentHelper.has(itemStack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
         this.spawnAtLocation(level, itemStack);
         this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
      }
   }

   @Override
   public boolean removeWhenFarAway(final double distSqr) {
      return false;
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      this.writeInventoryToTag(output);
      output.store("listener", VibrationSystem.Data.CODEC, this.vibrationData);
      output.putLong("DuplicationCooldown", this.duplicationCooldown);
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.readInventoryFromTag(input);
      this.vibrationData = input.<VibrationSystem.Data>read("listener", VibrationSystem.Data.CODEC).orElseGet(VibrationSystem.Data::new);
      this.setDuplicationCooldown(input.getIntOr("DuplicationCooldown", 0));
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

   private void setDuplicationCooldown(final long duplicationCooldown) {
      this.duplicationCooldown = duplicationCooldown;
      this.entityData.set(DATA_CAN_DUPLICATE, duplicationCooldown == 0L);
   }

   private void duplicateAllay() {
      Allay allay = EntityTypes.ALLAY.create(this.level(), EntitySpawnReason.BREEDING);
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

   private void removeInteractionItem(final Player player, final ItemStack interactionItem) {
      interactionItem.consume(1, player);
   }

   @Override
   public Vec3 getLeashOffset() {
      return new Vec3(0.0, this.getEyeHeight() * 0.6, this.getBbWidth() * 0.1);
   }

   @Override
   public void handleEntityEvent(final byte id) {
      if (id == 18) {
         for (int i = 0; i < 3; i++) {
            this.spawnHeartParticle();
         }
      } else {
         super.handleEntityEvent(id);
      }
   }

   private void spawnHeartParticle() {
      double xd = this.random.nextGaussian() * 0.02;
      double yd = this.random.nextGaussian() * 0.02;
      double zd = this.random.nextGaussian() * 0.02;
      this.level().addParticle(ParticleTypes.HEART, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), xd, yd, zd);
   }

   @Override
   public VibrationSystem.Data getVibrationData() {
      return this.vibrationData;
   }

   @Override
   public VibrationSystem.User getVibrationUser() {
      return this.vibrationUser;
   }

   private class JukeboxListener implements GameEventListener {
      private final PositionSource listenerSource;
      private final int listenerRadius;

      public JukeboxListener(final PositionSource listenerSource, final int listenerRadius) {
         this.listenerSource = listenerSource;
         this.listenerRadius = listenerRadius;
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
      public boolean handleGameEvent(final ServerLevel level, final Holder<GameEvent> event, final GameEvent.Context context, final Vec3 sourcePosition) {
         if (event.is(GameEvent.JUKEBOX_PLAY)) {
            Allay.this.setJukeboxPlaying(BlockPos.containing(sourcePosition), true);
            return true;
         } else if (event.is(GameEvent.JUKEBOX_STOP_PLAY)) {
            Allay.this.setJukeboxPlaying(BlockPos.containing(sourcePosition), false);
            return true;
         } else {
            return false;
         }
      }
   }

   private class VibrationUser implements VibrationSystem.User {
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
      public boolean canReceiveVibration(final ServerLevel level, final BlockPos pos, final Holder<GameEvent> event, final GameEvent.Context context) {
         if (Allay.this.isNoAi()) {
            return false;
         }

         Optional<GlobalPos> maybeGlobalPos = Allay.this.getBrain().getMemory(MemoryModuleType.LIKED_NOTEBLOCK_POSITION);
         if (maybeGlobalPos.isEmpty()) {
            return true;
         }

         GlobalPos globalPos = maybeGlobalPos.get();
         return globalPos.isCloseEnough(level.dimension(), Allay.this.blockPosition(), 1024) && globalPos.pos().equals(pos);
      }

      @Override
      public void onReceiveVibration(
         final ServerLevel level,
         final BlockPos pos,
         final Holder<GameEvent> event,
         final @Nullable Entity sourceEntity,
         final @Nullable Entity projectileOwner,
         final float receivingDistance
      ) {
         if (event.is(GameEvent.NOTE_BLOCK_PLAY)) {
            AllayAi.hearNoteblock(Allay.this, pos.immutable());
         }
      }

      @Override
      public TagKey<GameEvent> getListenableEvents() {
         return GameEventTags.ALLAY_CAN_LISTEN;
      }
   }
}
