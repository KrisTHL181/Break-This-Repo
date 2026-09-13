package net.minecraft.world.entity;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class OminousItemSpawner extends Entity {
   private static final int SPAWN_ITEM_DELAY_MIN = 60;
   private static final int SPAWN_ITEM_DELAY_MAX = 120;
   private static final String TAG_SPAWN_ITEM_AFTER_TICKS = "spawn_item_after_ticks";
   private static final String TAG_ITEM = "item";
   private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(OminousItemSpawner.class, EntityDataSerializers.ITEM_STACK);
   public static final int TICKS_BEFORE_ABOUT_TO_SPAWN_SOUND = 36;
   private long spawnItemAfterTicks;

   public OminousItemSpawner(final EntityType<? extends OminousItemSpawner> type, final Level level) {
      super(type, level);
      this.noPhysics = true;
   }

   public static OminousItemSpawner create(final Level level, final ItemStack item) {
      OminousItemSpawner itemSpawner = new OminousItemSpawner(EntityTypes.OMINOUS_ITEM_SPAWNER, level);
      itemSpawner.spawnItemAfterTicks = level.getRandom().nextIntBetweenInclusive(60, 120);
      itemSpawner.setItem(item);
      return itemSpawner;
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level() instanceof ServerLevel serverLevel) {
         this.tickServer(serverLevel);
      } else {
         this.tickClient();
      }
   }

   private void tickServer(final ServerLevel level) {
      if (this.tickCount == this.spawnItemAfterTicks - 36L) {
         level.playSound(null, this.blockPosition(), SoundEvents.TRIAL_SPAWNER_ABOUT_TO_SPAWN_ITEM, SoundSource.NEUTRAL);
      }

      if (this.tickCount >= this.spawnItemAfterTicks) {
         this.spawnItem();
         this.kill(level);
      }
   }

   private void tickClient() {
      if (this.level().getGameTime() % 5L == 0L) {
         this.addParticles();
      }
   }

   private void spawnItem() {
      if (this.level() instanceof ServerLevel level) {
         ItemStack item = this.getItem();
         if (!item.isEmpty()) {
            Entity spawnedEntity;
            if (item.getItem() instanceof ProjectileItem projectileItem) {
               spawnedEntity = this.spawnProjectile(level, projectileItem, item);
            } else {
               spawnedEntity = new ItemEntity(level, this.getX(), this.getY(), this.getZ(), item);
               level.addFreshEntity(spawnedEntity);
            }

            level.levelEvent(3021, this.blockPosition(), 1);
            level.gameEvent(spawnedEntity, GameEvent.ENTITY_PLACE, this.position());
            this.setItem(ItemStack.EMPTY);
         }
      }
   }

   private Entity spawnProjectile(final ServerLevel level, final ProjectileItem projectileItem, final ItemStack item) {
      ProjectileItem.DispenseConfig dispenseConfig = projectileItem.createDispenseConfig();
      dispenseConfig.overrideDispenseEvent().ifPresent(event -> level.levelEvent(event, this.blockPosition(), 0));
      Direction direction = Direction.DOWN;
      Projectile projectile = Projectile.spawnProjectileUsingShoot(
         projectileItem.asProjectile(level, this.position(), item, direction),
         level,
         item,
         direction.getStepX(),
         direction.getStepY(),
         direction.getStepZ(),
         dispenseConfig.power(),
         dispenseConfig.uncertainty()
      );
      projectile.setOwner(this);
      return projectile;
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      entityData.define(DATA_ITEM, ItemStack.EMPTY);
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      this.setItem(input.<ItemStack>read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY));
      this.spawnItemAfterTicks = input.getLongOr("spawn_item_after_ticks", 0L);
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      if (!this.getItem().isEmpty()) {
         output.store("item", ItemStack.CODEC, this.getItem());
      }

      output.putLong("spawn_item_after_ticks", this.spawnItemAfterTicks);
   }

   @Override
   protected boolean canAddPassenger(final Entity passenger) {
      return false;
   }

   @Override
   protected boolean couldAcceptPassenger() {
      return false;
   }

   @Override
   protected void addPassenger(final Entity passenger) {
      throw new IllegalStateException("Should never addPassenger without checking couldAcceptPassenger()");
   }

   @Override
   public PushReaction getPistonPushReaction() {
      return PushReaction.IGNORE;
   }

   @Override
   public boolean isIgnoringBlockTriggers() {
      return true;
   }

   public void addParticles() {
      Vec3 flyTowards = this.position();
      int particleCount = this.random.nextIntBetweenInclusive(1, 3);

      for (int i = 0; i < particleCount; i++) {
         double radius = 0.4;
         Vec3 flyFrom = new Vec3(
            this.getX() + 0.4 * (this.random.nextGaussian() - this.random.nextGaussian()),
            this.getY() + 0.4 * (this.random.nextGaussian() - this.random.nextGaussian()),
            this.getZ() + 0.4 * (this.random.nextGaussian() - this.random.nextGaussian())
         );
         Vec3 randomDirection = flyTowards.vectorTo(flyFrom);
         this.level()
            .addParticle(
               ParticleTypes.OMINOUS_SPAWNING, flyTowards.x(), flyTowards.y(), flyTowards.z(), randomDirection.x(), randomDirection.y(), randomDirection.z()
            );
      }
   }

   public ItemStack getItem() {
      return this.getEntityData().get(DATA_ITEM);
   }

   private void setItem(final ItemStack itemStack) {
      this.getEntityData().set(DATA_ITEM, itemStack);
   }

   @Override
   public final boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
      return false;
   }
}
