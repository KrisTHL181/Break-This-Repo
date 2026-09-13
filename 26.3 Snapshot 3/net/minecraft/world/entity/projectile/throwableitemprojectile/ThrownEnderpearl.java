package net.minecraft.world.entity.projectile.throwableitemprojectile;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ThrownEnderpearl extends ThrowableItemProjectile {
   private long ticketTimer = 0L;

   public ThrownEnderpearl(final EntityType<? extends ThrownEnderpearl> type, final Level level) {
      super(type, level);
   }

   public ThrownEnderpearl(final Level level, final LivingEntity mob, final ItemStack itemStack) {
      super(EntityTypes.ENDER_PEARL, mob, level, itemStack);
   }

   @Override
   protected Item getDefaultItem() {
      return Items.ENDER_PEARL;
   }

   @Override
   protected void setOwner(final @Nullable EntityReference<Entity> owner) {
      this.deregisterFromCurrentOwner();
      super.setOwner(owner);
      this.registerToCurrentOwner();
   }

   private void deregisterFromCurrentOwner() {
      if (this.getOwner() instanceof ServerPlayer serverPlayer) {
         serverPlayer.deregisterEnderPearl(this);
      }
   }

   private void registerToCurrentOwner() {
      if (this.getOwner() instanceof ServerPlayer serverPlayer) {
         serverPlayer.registerEnderPearl(this);
      }
   }

   @Override
   public @Nullable Entity getOwner() {
      return this.owner != null && this.level() instanceof ServerLevel serverLevel ? this.owner.getEntity(serverLevel, Entity.class) : super.getOwner();
   }

   private static @Nullable Entity findOwnerIncludingDeadPlayer(final ServerLevel serverLevel, final UUID uuid) {
      Entity owner = serverLevel.getEntityInAnyDimension(uuid);
      return owner != null ? owner : serverLevel.getServer().getPlayerList().getPlayer(uuid);
   }

   @Override
   protected void onHitEntity(final EntityHitResult hitResult) {
      super.onHitEntity(hitResult);
      hitResult.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), 0.0F);
   }

   @Override
   protected void onHit(final HitResult hitResult) {
      super.onHit(hitResult);

      for (int i = 0; i < 32; i++) {
         this.level()
            .addParticle(
               ParticleTypes.PORTAL,
               this.getX(),
               this.getY() + this.random.nextDouble() * 2.0,
               this.getZ(),
               this.random.nextGaussian(),
               0.0,
               this.random.nextGaussian()
            );
      }

      if (this.level() instanceof ServerLevel level && !this.isRemoved()) {
         Entity owner = this.getOwner();
         if (owner != null && isAllowedToTeleportOwner(owner, level)) {
            Vec3 teleportPos = this.oldPosition();
            if (owner instanceof ServerPlayer player) {
               if (player.connection.isAcceptingMessages()) {
                  if (this.random.nextFloat() < 0.05F && level.isSpawningMonsters() && level.getLevelData().getDifficulty() != Difficulty.PEACEFUL) {
                     Endermite endermite = EntityTypes.ENDERMITE.create(level, EntitySpawnReason.TRIGGERED);
                     if (endermite != null) {
                        endermite.snapTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
                        level.addFreshEntity(endermite);
                     }
                  }

                  if (this.isOnPortalCooldown()) {
                     owner.setPortalCooldown();
                  }

                  ServerPlayer newOwner = player.teleport(
                     new TeleportTransition(
                        level, teleportPos, Vec3.ZERO, 0.0F, 0.0F, Relative.union(Relative.ROTATION, Relative.DELTA), TeleportTransition.DO_NOTHING
                     )
                  );
                  if (newOwner != null) {
                     newOwner.resetFallDistance();
                     newOwner.resetCurrentImpulseContext();
                     newOwner.hurtServer(player.level(), this.damageSources().enderPearl(), 5.0F);
                  }

                  this.playSound(level, teleportPos);
               }
            } else {
               Entity newOwner = owner.teleport(
                  new TeleportTransition(level, teleportPos, owner.getDeltaMovement(), owner.getYRot(), owner.getXRot(), TeleportTransition.DO_NOTHING)
               );
               if (newOwner != null) {
                  newOwner.resetFallDistance();
               }

               if (newOwner instanceof LivingEntity livingEntity) {
                  livingEntity.resetCurrentImpulseContext();
               }

               this.playSound(level, teleportPos);
            }

            this.discard();
         } else {
            this.discard();
         }
      }
   }

   private static boolean isAllowedToTeleportOwner(final Entity owner, final Level newLevel) {
      if (owner.level().dimension() == newLevel.dimension()) {
         return !(owner instanceof LivingEntity livingOwner) ? owner.isAlive() : livingOwner.isAlive() && !livingOwner.isSleeping();
      } else {
         return owner.canUsePortal(true);
      }
   }

   @Override
   public void tick() {
      if (this.level() instanceof ServerLevel serverLevel) {
         int var7 = SectionPos.blockToSectionCoord(this.position().x());
         int previousChunkZ = SectionPos.blockToSectionCoord(this.position().z());
         Entity owner = this.owner != null ? findOwnerIncludingDeadPlayer(serverLevel, this.owner.getUUID()) : null;
         if (owner instanceof ServerPlayer serverPlayer
            && !owner.isAlive()
            && !serverPlayer.wonGame
            && serverPlayer.level().getGameRules().get(GameRules.ENDER_PEARLS_VANISH_ON_DEATH)) {
            this.discard();
         } else {
            super.tick();
         }

         if (this.isAlive()) {
            BlockPos currentPos = BlockPos.containing(this.position());
            if ((
                  --this.ticketTimer <= 0L
                     || var7 != SectionPos.blockToSectionCoord(currentPos.getX())
                     || previousChunkZ != SectionPos.blockToSectionCoord(currentPos.getZ())
               )
               && owner instanceof ServerPlayer serverPlayer) {
               this.ticketTimer = serverPlayer.registerAndUpdateEnderPearlTicket(this);
            }
         }
      } else {
         super.tick();
      }
   }

   private void playSound(final Level level, final Vec3 position) {
      level.playSound(null, position.x, position.y, position.z, SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS);
   }

   @Override
   public @Nullable Entity teleport(final TeleportTransition transition) {
      Entity newEntity = super.teleport(transition);
      if (newEntity != null) {
         newEntity.placePortalTicket(BlockPos.containing(newEntity.position()));
      }

      return newEntity;
   }

   @Override
   public boolean canTeleport(final Level from, final Level to) {
      return from.dimension() == Level.END && to.dimension() == Level.OVERWORLD && this.getOwner() instanceof ServerPlayer player
         ? super.canTeleport(from, to) && player.seenCredits
         : super.canTeleport(from, to);
   }

   @Override
   protected void onInsideBlock(final BlockState state) {
      super.onInsideBlock(state);
      if (state.is(Blocks.END_GATEWAY) && this.getOwner() instanceof ServerPlayer player) {
         player.onInsideBlock(state);
      }
   }

   @Override
   public void onRemoval(final Entity.RemovalReason reason) {
      if (reason != Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
         this.deregisterFromCurrentOwner();
      }

      super.onRemoval(reason);
   }

   @Override
   public void onAboveBubbleColumn(final boolean dragDown, final BlockPos pos) {
      Entity.handleOnAboveBubbleColumn(this, dragDown, pos);
   }

   @Override
   public void onInsideBubbleColumn(final boolean dragDown) {
      Entity.handleOnInsideBubbleColumn(this, dragDown);
   }
}
