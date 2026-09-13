package net.minecraft.world.entity;

import java.util.Optional;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.scores.PlayerTeam;
import org.jspecify.annotations.Nullable;

public abstract class TamableAnimal extends Animal implements OwnableEntity {
   public static final int TELEPORT_WHEN_DISTANCE_IS_SQ = 144;
   private static final int MIN_HORIZONTAL_DISTANCE_FROM_TARGET_AFTER_TELEPORTING = 2;
   private static final int MAX_HORIZONTAL_DISTANCE_FROM_TARGET_AFTER_TELEPORTING = 3;
   private static final int MAX_VERTICAL_DISTANCE_FROM_TARGET_AFTER_TELEPORTING = 1;
   private static final boolean DEFAULT_ORDERED_TO_SIT = false;
   protected static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(TamableAnimal.class, EntityDataSerializers.BYTE);
   protected static final EntityDataAccessor<Optional<EntityReference<LivingEntity>>> DATA_OWNERUUID_ID = SynchedEntityData.defineId(
      TamableAnimal.class, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE
   );
   private boolean orderedToSit = false;

   protected TamableAnimal(final EntityType<? extends TamableAnimal> type, final Level level) {
      super(type, level);
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(DATA_FLAGS_ID, (byte)0);
      entityData.define(DATA_OWNERUUID_ID, Optional.empty());
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      EntityReference<LivingEntity> owner = this.getOwnerReference();
      EntityReference.store(owner, output, "Owner");
      output.putBoolean("Sitting", this.orderedToSit);
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      EntityReference<LivingEntity> owner = EntityReference.readWithOldOwnerConversion(input, "Owner", this.level());
      if (owner != null) {
         try {
            this.entityData.set(DATA_OWNERUUID_ID, Optional.of(owner));
            this.setTame(true, false);
         } catch (Throwable ignored) {
            this.setTame(false, true);
         }
      } else {
         this.entityData.set(DATA_OWNERUUID_ID, Optional.empty());
         this.setTame(false, true);
      }

      this.orderedToSit = input.getBooleanOr("Sitting", false);
      this.setInSittingPose(this.orderedToSit);
   }

   @Override
   public boolean canBeLeashed() {
      return true;
   }

   protected void spawnTamingParticles(final boolean success) {
      ParticleOptions particle = ParticleTypes.HEART;
      if (!success) {
         particle = ParticleTypes.SMOKE;
      }

      for (int i = 0; i < 7; i++) {
         double xa = this.random.nextGaussian() * 0.02;
         double ya = this.random.nextGaussian() * 0.02;
         double za = this.random.nextGaussian() * 0.02;
         this.level().addParticle(particle, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), xa, ya, za);
      }
   }

   @Override
   public void handleEntityEvent(final byte id) {
      if (id == 7) {
         this.spawnTamingParticles(true);
      } else if (id == 6) {
         this.spawnTamingParticles(false);
      } else {
         super.handleEntityEvent(id);
      }
   }

   public boolean isTame() {
      return (this.entityData.get(DATA_FLAGS_ID) & 4) != 0;
   }

   public void setTame(final boolean isTame, final boolean includeSideEffects) {
      byte current = this.entityData.get(DATA_FLAGS_ID);
      if (isTame) {
         this.entityData.set(DATA_FLAGS_ID, (byte)(current | 4));
      } else {
         this.entityData.set(DATA_FLAGS_ID, (byte)(current & -5));
      }

      if (includeSideEffects) {
         this.applyTamingSideEffects();
      }
   }

   protected void applyTamingSideEffects() {
   }

   protected void feed(final Player player, final InteractionHand hand, final ItemStack itemStack, final float healingFactor, final float defaultHeal) {
      FoodProperties foodProperties = itemStack.get(DataComponents.FOOD);
      this.usePlayerItem(player, hand, itemStack);
      this.heal(foodProperties != null ? healingFactor * foodProperties.nutrition() : defaultHeal);
      this.playEatingSound();
   }

   public boolean isInSittingPose() {
      return (this.entityData.get(DATA_FLAGS_ID) & 1) != 0;
   }

   public void setInSittingPose(final boolean value) {
      byte current = this.entityData.get(DATA_FLAGS_ID);
      if (value) {
         this.entityData.set(DATA_FLAGS_ID, (byte)(current | 1));
      } else {
         this.entityData.set(DATA_FLAGS_ID, (byte)(current & -2));
      }
   }

   @Override
   public @Nullable EntityReference<LivingEntity> getOwnerReference() {
      return this.entityData.get(DATA_OWNERUUID_ID).orElse(null);
   }

   public void setOwner(final @Nullable LivingEntity owner) {
      this.entityData.set(DATA_OWNERUUID_ID, Optional.ofNullable(owner).map(EntityReference::of));
   }

   public void setOwnerReference(final @Nullable EntityReference<LivingEntity> owner) {
      this.entityData.set(DATA_OWNERUUID_ID, Optional.ofNullable(owner));
   }

   public void tame(final Player player) {
      this.setTame(true, true);
      this.setOwner(player);
      if (player instanceof ServerPlayer serverPlayer) {
         CriteriaTriggers.TAME_ANIMAL.trigger(serverPlayer, this);
      }
   }

   @Override
   public boolean canAttack(final LivingEntity target) {
      return this.isOwnedBy(target) ? false : super.canAttack(target);
   }

   public boolean isOwnedBy(final LivingEntity entity) {
      return entity == this.getOwner();
   }

   public boolean wantsToAttack(final LivingEntity target, final LivingEntity owner) {
      return true;
   }

   @Override
   public @Nullable PlayerTeam getTeam() {
      PlayerTeam ownTeam = super.getTeam();
      if (ownTeam != null) {
         return ownTeam;
      }

      if (this.isTame()) {
         LivingEntity owner = this.getRootOwner();
         if (owner != null) {
            return owner.getTeam();
         }
      }

      return null;
   }

   @Override
   protected boolean considersEntityAsAlly(final Entity other) {
      if (this.isTame()) {
         LivingEntity owner = this.getRootOwner();
         if (other == owner) {
            return true;
         }

         if (owner != null) {
            return owner.considersEntityAsAlly(other);
         }
      }

      return super.considersEntityAsAlly(other);
   }

   @Override
   public void die(final DamageSource source) {
      if (this.level() instanceof ServerLevel serverLevel
         && serverLevel.getGameRules().get(GameRules.SHOW_DEATH_MESSAGES)
         && this.getOwner() instanceof ServerPlayer serverPlayer) {
         serverPlayer.sendSystemMessage(this.getCombatTracker().getDeathMessage());
      }

      super.die(source);
   }

   public boolean isOrderedToSit() {
      return this.orderedToSit;
   }

   public void setOrderedToSit(final boolean orderedToSit) {
      this.orderedToSit = orderedToSit;
   }

   public void tryToTeleportToOwner() {
      LivingEntity owner = this.getOwner();
      if (owner != null) {
         this.teleportToAroundBlockPos(owner.blockPosition());
      }
   }

   public boolean shouldTryTeleportToOwner() {
      LivingEntity owner = this.getOwner();
      return owner != null && this.distanceToSqr(this.getOwner()) >= 144.0;
   }

   private void teleportToAroundBlockPos(final BlockPos targetPos) {
      for (int attempt = 0; attempt < 10; attempt++) {
         int xd = this.random.nextIntBetweenInclusive(-3, 3);
         int zd = this.random.nextIntBetweenInclusive(-3, 3);
         if (Math.abs(xd) >= 2 || Math.abs(zd) >= 2) {
            int yd = this.random.nextIntBetweenInclusive(-1, 1);
            if (this.maybeTeleportTo(targetPos.getX() + xd, targetPos.getY() + yd, targetPos.getZ() + zd)) {
               return;
            }
         }
      }
   }

   private boolean maybeTeleportTo(final int x, final int y, final int z) {
      if (!this.canTeleportTo(new BlockPos(x, y, z))) {
         return false;
      }

      this.snapTo(x + 0.5, y, z + 0.5, this.getYRot(), this.getXRot());
      this.navigation.stop();
      return true;
   }

   private boolean canTeleportTo(final BlockPos pos) {
      PathType pathType = WalkNodeEvaluator.getPathTypeStatic(this, pos);
      if (pathType != PathType.WALKABLE) {
         return false;
      }

      BlockState blockStateBelow = this.level().getBlockState(pos.below());
      if (!this.canFlyToOwner() && blockStateBelow.getBlock() instanceof LeavesBlock) {
         return false;
      }

      BlockPos delta = pos.subtract(this.blockPosition());
      return this.level().noCollision(this, this.getBoundingBox().move(delta));
   }

   public final boolean unableToMoveToOwner() {
      return this.isOrderedToSit() || this.isPassenger() || this.mayBeLeashed() || this.getOwner() != null && this.getOwner().isSpectator();
   }

   protected boolean canFlyToOwner() {
      return false;
   }

   public class TamableAnimalPanicGoal extends PanicGoal {
      public TamableAnimalPanicGoal(final double speedModifier, final TagKey<DamageType> panicCausingDamageTypes) {
         super(TamableAnimal.this, speedModifier, panicCausingDamageTypes);
      }

      public TamableAnimalPanicGoal(final double speedModifier) {
         super(TamableAnimal.this, speedModifier);
      }

      @Override
      public void tick() {
         if (!TamableAnimal.this.unableToMoveToOwner() && TamableAnimal.this.shouldTryTeleportToOwner()) {
            TamableAnimal.this.tryToTeleportToOwner();
         }

         super.tick();
      }
   }
}
