package net.minecraft.world.entity.projectile.throwableitemprojectile;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
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

   public ThrownEnderpearl(EntityType<? extends ThrownEnderpearl> p_451775_, Level p_454048_) {
      super(p_451775_, p_454048_);
   }

   public ThrownEnderpearl(Level p_460742_, LivingEntity p_458699_, ItemStack p_460872_) {
      super(EntityType.ENDER_PEARL, p_458699_, p_460742_, p_460872_);
   }

   @Override
   protected Item getDefaultItem() {
      return Items.ENDER_PEARL;
   }

   @Override
   protected void setOwner(@Nullable EntityReference<Entity> p_452724_) {
      this.deregisterFromCurrentOwner();
      super.setOwner(p_452724_);
      this.registerToCurrentOwner();
   }

   private void deregisterFromCurrentOwner() {
      if (this.getOwner() instanceof ServerPlayer serverplayer) {
         serverplayer.deregisterEnderPearl(this);
      }
   }

   private void registerToCurrentOwner() {
      if (this.getOwner() instanceof ServerPlayer serverplayer) {
         serverplayer.registerEnderPearl(this);
      }
   }

   @Override
   public @Nullable Entity getOwner() {
      return this.owner != null && this.level() instanceof ServerLevel serverlevel ? this.owner.getEntity(serverlevel, Entity.class) : super.getOwner();
   }

   private static @Nullable Entity findOwnerIncludingDeadPlayer(ServerLevel p_460869_, UUID p_460652_) {
      Entity entity = p_460869_.getEntityInAnyDimension(p_460652_);
      return entity != null ? entity : p_460869_.getServer().getPlayerList().getPlayer(p_460652_);
   }

   @Override
   protected void onHitEntity(EntityHitResult p_454744_) {
      super.onHitEntity(p_454744_);
      p_454744_.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), 0.0F);
   }

   @Override
   protected void onHit(HitResult p_451483_) {
      super.onHit(p_451483_);

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

      if (this.level() instanceof ServerLevel serverlevel && !this.isRemoved()) {
         Entity entity = this.getOwner();
         if (entity != null && isAllowedToTeleportOwner(entity, serverlevel)) {
            Vec3 vec3 = this.oldPosition();
            if (entity instanceof ServerPlayer serverplayer) {
               if (serverplayer.connection.isAcceptingMessages()) {
                  if (this.random.nextFloat() < 0.05F && serverlevel.isSpawningMonsters()) {
                     Endermite endermite = EntityType.ENDERMITE.create(serverlevel, EntitySpawnReason.TRIGGERED);
                     if (endermite != null) {
                        endermite.snapTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
                        serverlevel.addFreshEntity(endermite);
                     }
                  }

                  if (this.isOnPortalCooldown()) {
                     entity.setPortalCooldown();
                  }

                  ServerPlayer serverplayer1 = serverplayer.teleport(
                     new TeleportTransition(
                        serverlevel, vec3, Vec3.ZERO, 0.0F, 0.0F, Relative.union(Relative.ROTATION, Relative.DELTA), TeleportTransition.DO_NOTHING
                     )
                  );
                  if (serverplayer1 != null) {
                     serverplayer1.resetFallDistance();
                     serverplayer1.resetCurrentImpulseContext();
                     serverplayer1.hurtServer(serverplayer.level(), this.damageSources().enderPearl(), 5.0F);
                  }

                  this.playSound(serverlevel, vec3);
               }
            } else {
               Entity entity1 = entity.teleport(
                  new TeleportTransition(serverlevel, vec3, entity.getDeltaMovement(), entity.getYRot(), entity.getXRot(), TeleportTransition.DO_NOTHING)
               );
               if (entity1 != null) {
                  entity1.resetFallDistance();
               }

               this.playSound(serverlevel, vec3);
            }

            this.discard();
         } else {
            this.discard();
         }
      }
   }

   private static boolean isAllowedToTeleportOwner(Entity p_455145_, Level p_450595_) {
      if (p_455145_.level().dimension() != p_450595_.dimension()) {
         return p_455145_.canUsePortal(true);
      } else {
         return p_455145_ instanceof LivingEntity livingentity ? livingentity.isAlive() && !livingentity.isSleeping() : p_455145_.isAlive();
      }
   }

   @Override
   public void tick() {
      if (this.level() instanceof ServerLevel serverlevel) {
         int j = SectionPos.blockToSectionCoord(this.position().x());
         int $$3 = SectionPos.blockToSectionCoord(this.position().z());
         Entity entity = this.owner != null ? findOwnerIncludingDeadPlayer(serverlevel, this.owner.getUUID()) : null;
         if (entity instanceof ServerPlayer serverplayer
            && !entity.isAlive()
            && !serverplayer.wonGame
            && serverplayer.level().getGameRules().get(GameRules.ENDER_PEARLS_VANISH_ON_DEATH)) {
            this.discard();
         } else {
            super.tick();
         }

         if (this.isAlive()) {
            BlockPos blockpos = BlockPos.containing(this.position());
            if ((--this.ticketTimer <= 0L || j != SectionPos.blockToSectionCoord(blockpos.getX()) || $$3 != SectionPos.blockToSectionCoord(blockpos.getZ()))
               && entity instanceof ServerPlayer serverplayer1) {
               this.ticketTimer = serverplayer1.registerAndUpdateEnderPearlTicket(this);
            }
         }
      } else {
         super.tick();
      }
   }

   private void playSound(Level p_453382_, Vec3 p_453690_) {
      p_453382_.playSound(null, p_453690_.x, p_453690_.y, p_453690_.z, SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS);
   }

   @Override
   public @Nullable Entity teleport(TeleportTransition p_454458_) {
      Entity entity = super.teleport(p_454458_);
      if (entity != null) {
         entity.placePortalTicket(BlockPos.containing(entity.position()));
      }

      return entity;
   }

   @Override
   public boolean canTeleport(Level p_450310_, Level p_450589_) {
      return p_450310_.dimension() == Level.END && p_450589_.dimension() == Level.OVERWORLD && this.getOwner() instanceof ServerPlayer serverplayer
         ? super.canTeleport(p_450310_, p_450589_) && serverplayer.seenCredits
         : super.canTeleport(p_450310_, p_450589_);
   }

   @Override
   protected void onInsideBlock(BlockState p_453185_) {
      super.onInsideBlock(p_453185_);
      if (p_453185_.is(Blocks.END_GATEWAY) && this.getOwner() instanceof ServerPlayer serverplayer) {
         serverplayer.onInsideBlock(p_453185_);
      }
   }

   @Override
   public void onRemoval(Entity.RemovalReason p_460801_) {
      if (p_460801_ != Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
         this.deregisterFromCurrentOwner();
      }

      super.onRemoval(p_460801_);
   }

   @Override
   public void onAboveBubbleColumn(boolean p_455548_, BlockPos p_460769_) {
      Entity.handleOnAboveBubbleColumn(this, p_455548_, p_460769_);
   }

   @Override
   public void onInsideBubbleColumn(boolean p_455880_) {
      Entity.handleOnInsideBubbleColumn(this, p_455880_);
   }
}
