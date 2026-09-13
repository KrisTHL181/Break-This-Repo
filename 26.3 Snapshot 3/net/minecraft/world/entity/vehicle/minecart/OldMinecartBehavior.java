package net.minecraft.world.entity.vehicle.minecart;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class OldMinecartBehavior extends MinecartBehavior {
   private static final double MINECART_RIDABLE_THRESHOLD = 0.01;
   private static final double MAX_SPEED_IN_WATER = 0.2;
   private static final double MAX_SPEED_ON_LAND = 0.4;
   private static final double ABSOLUTE_MAX_SPEED = 0.4;
   private final InterpolationHandler interpolation;
   private Vec3 targetDeltaMovement = Vec3.ZERO;

   public OldMinecartBehavior(final AbstractMinecart minecart) {
      super(minecart);
      this.interpolation = new LinearInterpolationHandler(minecart);
   }

   @Override
   public InterpolationHandler getInterpolation() {
      return this.interpolation;
   }

   @Override
   public void onInterpolationStart(final InterpolationHandler interpolation) {
      this.setDeltaMovement(this.targetDeltaMovement);
   }

   @Override
   public void lerpMotion(final Vec3 movement) {
      this.targetDeltaMovement = movement;
      this.setDeltaMovement(this.targetDeltaMovement);
   }

   @Override
   public void tick() {
      if (this.level() instanceof ServerLevel level) {
         this.minecart.applyGravity();
         BlockPos var11 = this.minecart.getCurrentBlockPosOrRailBelow();
         BlockState state = this.level().getBlockState(var11);
         boolean onRails = BaseRailBlock.isRail(state);
         this.minecart.setOnRails(onRails);
         if (onRails) {
            this.moveAlongTrack(level);
            if (state.is(Blocks.ACTIVATOR_RAIL)) {
               this.minecart.activateMinecart(level, var11.getX(), var11.getY(), var11.getZ(), state.getValue(PoweredRailBlock.POWERED));
            }
         } else {
            this.minecart.comeOffTrack(level);
         }

         this.minecart.applyEffectsFromBlocks();
         this.setXRot(0.0F);
         double xDiff = this.minecart.xo - this.getX();
         double zDiff = this.minecart.zo - this.getZ();
         if (xDiff * xDiff + zDiff * zDiff > 0.001) {
            this.setYRot((float)(Mth.atan2(zDiff, xDiff) * 180.0 / Math.PI));
            if (this.minecart.isFlipped()) {
               this.setYRot(this.getYRot() + 180.0F);
            }
         }

         double rotDiff = Mth.wrapDegrees(this.getYRot() - this.minecart.yRotO);
         if (rotDiff < -170.0 || rotDiff >= 170.0) {
            this.setYRot(this.getYRot() + 180.0F);
            this.minecart.setFlipped(!this.minecart.isFlipped());
         }

         this.setXRot(this.getXRot() % 360.0F);
         this.setYRot(this.getYRot() % 360.0F);
         this.pushAndPickupEntities();
      } else {
         if (this.interpolation.hasActiveInterpolation()) {
            this.interpolation.interpolate();
         } else {
            this.minecart.reapplyPosition();
            this.setXRot(this.getXRot() % 360.0F);
            this.setYRot(this.getYRot() % 360.0F);
         }
      }
   }

   @Override
   public void moveAlongTrack(final ServerLevel level) {
      BlockPos pos = this.minecart.getCurrentBlockPosOrRailBelow();
      BlockState state = this.level().getBlockState(pos);
      this.minecart.resetFallDistance();
      double x = this.minecart.getX();
      double y = this.minecart.getY();
      double z = this.minecart.getZ();
      Vec3 oldPos = this.getPos(x, y, z);
      y = pos.getY();
      boolean powerTrack = false;
      boolean haltTrack = false;
      if (state.is(Blocks.POWERED_RAIL)) {
         powerTrack = state.getValue(PoweredRailBlock.POWERED);
         haltTrack = !powerTrack;
      }

      double slideSpeed = 0.0078125;
      if (this.minecart.isInWater()) {
         slideSpeed *= 0.2;
      }

      Vec3 movement = this.getDeltaMovement();
      RailShape shape = state.getValue(((BaseRailBlock)state.getBlock()).getShapeProperty());
      switch (shape) {
         case ASCENDING_EAST:
            this.setDeltaMovement(movement.add(-slideSpeed, 0.0, 0.0));
            y++;
            break;
         case ASCENDING_WEST:
            this.setDeltaMovement(movement.add(slideSpeed, 0.0, 0.0));
            y++;
            break;
         case ASCENDING_NORTH:
            this.setDeltaMovement(movement.add(0.0, 0.0, slideSpeed));
            y++;
            break;
         case ASCENDING_SOUTH:
            this.setDeltaMovement(movement.add(0.0, 0.0, -slideSpeed));
            y++;
      }

      movement = this.getDeltaMovement();
      Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(shape);
      Vec3i exit0 = (Vec3i)exits.getFirst();
      Vec3i exit1 = (Vec3i)exits.getSecond();
      double xD = exit1.getX() - exit0.getX();
      double zD = exit1.getZ() - exit0.getZ();
      double length = Math.sqrt(xD * xD + zD * zD);
      double flip = movement.x * xD + movement.z * zD;
      if (flip < 0.0) {
         xD = -xD;
         zD = -zD;
      }

      double pow = Math.min(2.0, movement.horizontalDistance());
      movement = new Vec3(pow * xD / length, movement.y, pow * zD / length);
      this.setDeltaMovement(movement);
      Entity controllingPassenger = this.minecart.getFirstPassenger();
      Vec3 moveIntent;
      if (this.minecart.getFirstPassenger() instanceof ServerPlayer player) {
         moveIntent = player.getLastClientMoveIntent();
      } else {
         moveIntent = Vec3.ZERO;
      }

      if (controllingPassenger instanceof Player && moveIntent.lengthSqr() > 0.0) {
         Vec3 riderMovement = moveIntent.normalize();
         double ownDist = this.getDeltaMovement().horizontalDistanceSqr();
         if (riderMovement.lengthSqr() > 0.0 && ownDist < 0.01) {
            this.setDeltaMovement(this.getDeltaMovement().add(moveIntent.x * 0.001, 0.0, moveIntent.z * 0.001));
            haltTrack = false;
         }
      }

      if (haltTrack) {
         double speedLength = this.getDeltaMovement().horizontalDistance();
         if (speedLength < 0.03) {
            this.setDeltaMovement(Vec3.ZERO);
         } else {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.0, 0.5));
         }
      }

      double x0 = pos.getX() + 0.5 + exit0.getX() * 0.5;
      double z0 = pos.getZ() + 0.5 + exit0.getZ() * 0.5;
      double x1 = pos.getX() + 0.5 + exit1.getX() * 0.5;
      double z1 = pos.getZ() + 0.5 + exit1.getZ() * 0.5;
      xD = x1 - x0;
      zD = z1 - z0;
      double progress;
      if (xD == 0.0) {
         progress = z - pos.getZ();
      } else if (zD == 0.0) {
         progress = x - pos.getX();
      } else {
         double xx = x - x0;
         double zz = z - z0;
         progress = (xx * xD + zz * zD) * 2.0;
      }

      x = x0 + xD * progress;
      z = z0 + zD * progress;
      this.setPos(x, y, z);
      double scale = this.minecart.isVehicle() ? 0.75 : 1.0;
      double maxSpeed = this.minecart.getMaxSpeed(level);
      movement = this.getDeltaMovement();
      this.minecart.move(MoverType.SELF, new Vec3(Mth.clamp(scale * movement.x, -maxSpeed, maxSpeed), 0.0, Mth.clamp(scale * movement.z, -maxSpeed, maxSpeed)));
      if (exit0.getY() != 0 && Mth.floor(this.minecart.getX()) - pos.getX() == exit0.getX() && Mth.floor(this.minecart.getZ()) - pos.getZ() == exit0.getZ()) {
         this.setPos(this.minecart.getX(), this.minecart.getY() + exit0.getY(), this.minecart.getZ());
      } else if (exit1.getY() != 0
         && Mth.floor(this.minecart.getX()) - pos.getX() == exit1.getX()
         && Mth.floor(this.minecart.getZ()) - pos.getZ() == exit1.getZ()) {
         this.setPos(this.minecart.getX(), this.minecart.getY() + exit1.getY(), this.minecart.getZ());
      }

      this.setDeltaMovement(this.minecart.applyNaturalSlowdown(this.getDeltaMovement()));
      Vec3 newPos = this.getPos(this.minecart.getX(), this.minecart.getY(), this.minecart.getZ());
      if (newPos != null && oldPos != null) {
         double speed = (oldPos.y - newPos.y) * 0.05;
         Vec3 vec3 = this.getDeltaMovement();
         double otherPow = vec3.horizontalDistance();
         if (otherPow > 0.0) {
            this.setDeltaMovement(vec3.multiply((otherPow + speed) / otherPow, 1.0, (otherPow + speed) / otherPow));
         }

         this.setPos(this.minecart.getX(), newPos.y, this.minecart.getZ());
      }

      int xn = Mth.floor(this.minecart.getX());
      int zn = Mth.floor(this.minecart.getZ());
      if (xn != pos.getX() || zn != pos.getZ()) {
         Vec3 vec3 = this.getDeltaMovement();
         double otherPow = vec3.horizontalDistance();
         this.setDeltaMovement(otherPow * (xn - pos.getX()), vec3.y, otherPow * (zn - pos.getZ()));
      }

      if (powerTrack) {
         Vec3 vec3 = this.getDeltaMovement();
         double speedLength = vec3.horizontalDistance();
         if (speedLength > 0.01) {
            double speed = 0.06;
            this.setDeltaMovement(vec3.add(vec3.x / speedLength * 0.06, 0.0, vec3.z / speedLength * 0.06));
         } else {
            Vec3 deltaMovement = this.getDeltaMovement();
            double dx = deltaMovement.x;
            double dz = deltaMovement.z;
            if (shape == RailShape.EAST_WEST) {
               if (this.minecart.isRedstoneConductor(pos.west())) {
                  dx = 0.02;
               } else if (this.minecart.isRedstoneConductor(pos.east())) {
                  dx = -0.02;
               }
            } else {
               if (shape != RailShape.NORTH_SOUTH) {
                  return;
               }

               if (this.minecart.isRedstoneConductor(pos.north())) {
                  dz = 0.02;
               } else if (this.minecart.isRedstoneConductor(pos.south())) {
                  dz = -0.02;
               }
            }

            this.setDeltaMovement(dx, deltaMovement.y, dz);
         }
      }
   }

   public @Nullable Vec3 getPosOffs(double x, double y, double z, final double offs) {
      int xt = Mth.floor(x);
      int yt = Mth.floor(y);
      int zt = Mth.floor(z);
      if (this.level().getBlockState(new BlockPos(xt, yt - 1, zt)).is(BlockTags.RAILS)) {
         yt--;
      }

      BlockState state = this.level().getBlockState(new BlockPos(xt, yt, zt));
      if (BaseRailBlock.isRail(state)) {
         RailShape shape = state.getValue(((BaseRailBlock)state.getBlock()).getShapeProperty());
         y = yt;
         if (shape.isSlope()) {
            y = yt + 1;
         }

         Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(shape);
         Vec3i exit0 = (Vec3i)exits.getFirst();
         Vec3i exit1 = (Vec3i)exits.getSecond();
         double xD = exit1.getX() - exit0.getX();
         double zD = exit1.getZ() - exit0.getZ();
         double dd = Math.sqrt(xD * xD + zD * zD);
         xD /= dd;
         zD /= dd;
         x += xD * offs;
         z += zD * offs;
         if (exit0.getY() != 0 && Mth.floor(x) - xt == exit0.getX() && Mth.floor(z) - zt == exit0.getZ()) {
            y += exit0.getY();
         } else if (exit1.getY() != 0 && Mth.floor(x) - xt == exit1.getX() && Mth.floor(z) - zt == exit1.getZ()) {
            y += exit1.getY();
         }

         return this.getPos(x, y, z);
      } else {
         return null;
      }
   }

   public @Nullable Vec3 getPos(double x, double y, double z) {
      int xt = Mth.floor(x);
      int yt = Mth.floor(y);
      int zt = Mth.floor(z);
      if (this.level().getBlockState(new BlockPos(xt, yt - 1, zt)).is(BlockTags.RAILS)) {
         yt--;
      }

      BlockState state = this.level().getBlockState(new BlockPos(xt, yt, zt));
      if (BaseRailBlock.isRail(state)) {
         RailShape shape = state.getValue(((BaseRailBlock)state.getBlock()).getShapeProperty());
         Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(shape);
         Vec3i exit0 = (Vec3i)exits.getFirst();
         Vec3i exit1 = (Vec3i)exits.getSecond();
         double x0 = xt + 0.5 + exit0.getX() * 0.5;
         double y0 = yt + 0.0625 + exit0.getY() * 0.5;
         double z0 = zt + 0.5 + exit0.getZ() * 0.5;
         double x1 = xt + 0.5 + exit1.getX() * 0.5;
         double y1 = yt + 0.0625 + exit1.getY() * 0.5;
         double z1 = zt + 0.5 + exit1.getZ() * 0.5;
         double xD = x1 - x0;
         double yD = (y1 - y0) * 2.0;
         double zD = z1 - z0;
         double progress;
         if (xD == 0.0) {
            progress = z - zt;
         } else if (zD == 0.0) {
            progress = x - xt;
         } else {
            double xx = x - x0;
            double zz = z - z0;
            progress = (xx * xD + zz * zD) * 2.0;
         }

         x = x0 + xD * progress;
         y = y0 + yD * progress;
         z = z0 + zD * progress;
         if (yD < 0.0) {
            y++;
         } else if (yD > 0.0) {
            y += 0.5;
         }

         return new Vec3(x, y, z);
      } else {
         return null;
      }
   }

   @Override
   public double stepAlongTrack(final BlockPos pos, final RailShape shape, final double movementLeft) {
      return 0.0;
   }

   @Override
   public boolean pushAndPickupEntities() {
      AABB hitbox = this.minecart.getBoundingBox().inflate(0.2F, 0.0, 0.2F);
      if (this.minecart.isRideable() && this.getDeltaMovement().horizontalDistanceSqr() >= 0.01) {
         List<Entity> entities = this.level().getEntities(this.minecart, hitbox, EntitySelector.pushableBy(this.minecart));
         if (!entities.isEmpty()) {
            for (Entity entity : entities) {
               if (!(entity instanceof Player)
                  && !(entity instanceof IronGolem)
                  && !(entity instanceof AbstractMinecart)
                  && !this.minecart.isVehicle()
                  && !entity.isPassenger()) {
                  entity.startRiding(this.minecart);
               } else {
                  entity.push(this.minecart);
               }
            }
         }
      } else {
         for (Entity entity : this.level().getEntities(this.minecart, hitbox)) {
            if (!this.minecart.hasPassenger(entity) && entity.isPushable() && entity instanceof AbstractMinecart) {
               entity.push(this.minecart);
            }
         }
      }

      return false;
   }

   @Override
   public Direction getMotionDirection() {
      return this.minecart.isFlipped() ? this.minecart.getDirection().getOpposite().getClockWise() : this.minecart.getDirection().getClockWise();
   }

   @Override
   public Vec3 getKnownMovement(final Vec3 knownMovement) {
      return !Double.isNaN(knownMovement.x) && !Double.isNaN(knownMovement.y) && !Double.isNaN(knownMovement.z)
         ? new Vec3(Mth.clamp(knownMovement.x, -0.4, 0.4), knownMovement.y, Mth.clamp(knownMovement.z, -0.4, 0.4))
         : Vec3.ZERO;
   }

   @Override
   public double getMaxSpeed(final ServerLevel level) {
      return this.minecart.isInWater() ? 0.2 : 0.4;
   }

   @Override
   public double getSlowdownFactor() {
      return this.minecart.isVehicle() ? 0.997 : 0.96;
   }
}
