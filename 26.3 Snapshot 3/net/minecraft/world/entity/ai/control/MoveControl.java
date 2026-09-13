package net.minecraft.world.entity.ai.control;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MoveControl<T extends Mob> implements Control {
   public static final float MIN_SPEED = 5.0E-4F;
   public static final float MIN_SPEED_SQR = 2.5000003E-7F;
   protected static final int MAX_TURN = 90;
   protected final T mob;
   protected double wantedX;
   protected double wantedY;
   protected double wantedZ;
   protected double speedModifier;
   protected float strafeForwards;
   protected float strafeRight;
   protected MoveControl.Operation operation = MoveControl.Operation.WAIT;

   public MoveControl(final T mob) {
      this.mob = mob;
   }

   public boolean hasWanted() {
      return this.operation == MoveControl.Operation.MOVE_TO;
   }

   public double getSpeedModifier() {
      return this.speedModifier;
   }

   public void setWantedPosition(final double x, final double y, final double z, final double speedModifier) {
      this.wantedX = x;
      this.wantedY = y;
      this.wantedZ = z;
      this.speedModifier = speedModifier;
      if (this.operation != MoveControl.Operation.JUMPING) {
         this.operation = MoveControl.Operation.MOVE_TO;
      }
   }

   public void strafe(final float forwards, final float right) {
      this.operation = MoveControl.Operation.STRAFE;
      this.strafeForwards = forwards;
      this.strafeRight = right;
      this.speedModifier = 0.25;
   }

   public void tick() {
      if (this.operation == MoveControl.Operation.STRAFE) {
         float speed = (float)this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
         float speedModified = (float)this.speedModifier * speed;
         float xa = this.strafeForwards;
         float za = this.strafeRight;
         float dist = Mth.sqrt(xa * xa + za * za);
         if (dist < 1.0F) {
            dist = 1.0F;
         }

         dist = speedModified / dist;
         xa *= dist;
         za *= dist;
         float sin = Mth.sin(this.mob.getYRot() * (float) (Math.PI / 180.0));
         float cos = Mth.cos(this.mob.getYRot() * (float) (Math.PI / 180.0));
         float dx = xa * cos - za * sin;
         float dz = za * cos + xa * sin;
         if (!this.isWalkable(dx, dz)) {
            this.strafeForwards = 1.0F;
            this.strafeRight = 0.0F;
         }

         this.mob.setSpeed(speedModified);
         this.mob.setZza(this.strafeForwards);
         this.mob.setXxa(this.strafeRight);
         this.operation = MoveControl.Operation.WAIT;
      } else if (this.operation == MoveControl.Operation.MOVE_TO) {
         this.operation = MoveControl.Operation.WAIT;
         double xd = this.wantedX - this.mob.getX();
         double zd = this.wantedZ - this.mob.getZ();
         double yd = this.wantedY - this.mob.getY();
         double dd = xd * xd + yd * yd + zd * zd;
         if (dd < 2.5000003E-7F) {
            this.mob.setZza(0.0F);
            return;
         }

         float yRotD = (float)(Mth.atan2(zd, xd) * 180.0F / (float)Math.PI) - 90.0F;
         this.mob.setYRot(this.rotlerp(this.mob.getYRot(), yRotD, 90.0F));
         this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
         BlockPos pos = this.mob.blockPosition();
         BlockState blockState = this.mob.level().getBlockState(pos);
         VoxelShape shape = blockState.getCollisionShape(this.mob.level(), pos);
         if (yd > this.mob.maxUpStep() && xd * xd + zd * zd < Math.max(1.0F, this.mob.getBbWidth())
            || !shape.isEmpty()
               && this.mob.getY() < shape.max(Direction.Axis.Y) + pos.getY()
               && !blockState.is(BlockTags.DOORS)
               && !blockState.is(BlockTags.FENCES)) {
            this.mob.getJumpControl().jump();
            this.operation = MoveControl.Operation.JUMPING;
         }
      } else if (this.operation == MoveControl.Operation.JUMPING) {
         this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
         if (this.mob.onGround() || this.mob.isInLiquid() && this.mob.isAffectedByFluids()) {
            this.operation = MoveControl.Operation.WAIT;
         }
      } else {
         this.mob.setZza(0.0F);
      }
   }

   private boolean isWalkable(final float dx, final float dz) {
      PathNavigation pathNavigation = this.mob.getNavigation();
      if (pathNavigation != null) {
         NodeEvaluator nodeEvaluator = pathNavigation.getNodeEvaluator();
         if (nodeEvaluator != null
            && nodeEvaluator.getPathType(this.mob, BlockPos.containing(this.mob.getX() + dx, this.mob.getBlockY(), this.mob.getZ() + dz)) != PathType.WALKABLE) {
            return false;
         }
      }

      return true;
   }

   protected float rotlerp(final float a, final float b, final float max) {
      float diff = Mth.wrapDegrees(b - a);
      if (diff > max) {
         diff = max;
      }

      if (diff < -max) {
         diff = -max;
      }

      float result = a + diff;
      if (result < 0.0F) {
         result += 360.0F;
      } else if (result > 360.0F) {
         result -= 360.0F;
      }

      return result;
   }

   public double getWantedX() {
      return this.wantedX;
   }

   public double getWantedY() {
      return this.wantedY;
   }

   public double getWantedZ() {
      return this.wantedZ;
   }

   public void setWait() {
      this.operation = MoveControl.Operation.WAIT;
   }

   protected enum Operation {
      WAIT,
      MOVE_TO,
      STRAFE,
      JUMPING;
   }
}
