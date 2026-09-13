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

   public OldMinecartBehavior(AbstractMinecart p_453164_) {
      super(p_453164_);
      this.interpolation = new InterpolationHandler(p_453164_, this::onInterpolation);
   }

   @Override
   public InterpolationHandler getInterpolation() {
      return this.interpolation;
   }

   public void onInterpolation(InterpolationHandler p_454417_) {
      this.setDeltaMovement(this.targetDeltaMovement);
   }

   @Override
   public void lerpMotion(Vec3 p_450882_) {
      this.targetDeltaMovement = p_450882_;
      this.setDeltaMovement(this.targetDeltaMovement);
   }

   @Override
   public void tick() {
      if (this.level() instanceof ServerLevel serverlevel) {
         this.minecart.applyGravity();
         BlockPos blockpos = this.minecart.getCurrentBlockPosOrRailBelow();
         BlockState blockstate = this.level().getBlockState(blockpos);
         boolean $$4 = BaseRailBlock.isRail(blockstate);
         this.minecart.setOnRails($$4);
         if ($$4) {
            this.moveAlongTrack(serverlevel);
            if (blockstate.is(Blocks.ACTIVATOR_RAIL)) {
               this.minecart.activateMinecart(serverlevel, blockpos.getX(), blockpos.getY(), blockpos.getZ(), blockstate.getValue(PoweredRailBlock.POWERED));
            }
         } else {
            this.minecart.comeOffTrack(serverlevel);
         }

         this.minecart.applyEffectsFromBlocks();
         this.setXRot(0.0F);
         double d0 = this.minecart.xo - this.getX();
         double d1 = this.minecart.zo - this.getZ();
         if (d0 * d0 + d1 * d1 > 0.001) {
            this.setYRot((float)(Mth.atan2(d1, d0) * 180.0 / Math.PI));
            if (this.minecart.isFlipped()) {
               this.setYRot(this.getYRot() + 180.0F);
            }
         }

         double d2 = Mth.wrapDegrees(this.getYRot() - this.minecart.yRotO);
         if (d2 < -170.0 || d2 >= 170.0) {
            this.setYRot(this.getYRot() + 180.0F);
            this.minecart.setFlipped(!this.minecart.isFlipped());
         }

         this.setXRot(this.getXRot() % 360.0F);
         this.setYRot(this.getYRot() % 360.0F);
         this.pushAndPickupEntities();
      } else if (this.interpolation.hasActiveInterpolation()) {
         this.interpolation.interpolate();
      } else {
         this.minecart.reapplyPosition();
         this.setXRot(this.getXRot() % 360.0F);
         this.setYRot(this.getYRot() % 360.0F);
      }
   }

   @Override
   public void moveAlongTrack(ServerLevel p_458640_) {
      BlockPos blockpos = this.minecart.getCurrentBlockPosOrRailBelow();
      BlockState blockstate = this.level().getBlockState(blockpos);
      this.minecart.resetFallDistance();
      double d0 = this.minecart.getX();
      double d1 = this.minecart.getY();
      double d2 = this.minecart.getZ();
      Vec3 vec3 = this.getPos(d0, d1, d2);
      d1 = blockpos.getY();
      boolean flag = false;
      boolean flag1 = false;
      if (blockstate.is(Blocks.POWERED_RAIL)) {
         flag = blockstate.getValue(PoweredRailBlock.POWERED);
         flag1 = !flag;
      }

      double d3 = 0.0078125;
      if (this.minecart.isInWater()) {
         d3 *= 0.2;
      }

      Vec3 vec31 = this.getDeltaMovement();
      RailShape railshape = blockstate.getValue(((BaseRailBlock)blockstate.getBlock()).getShapeProperty());
      switch (railshape) {
         case ASCENDING_EAST:
            this.setDeltaMovement(vec31.add(-d3, 0.0, 0.0));
            d1++;
            break;
         case ASCENDING_WEST:
            this.setDeltaMovement(vec31.add(d3, 0.0, 0.0));
            d1++;
            break;
         case ASCENDING_NORTH:
            this.setDeltaMovement(vec31.add(0.0, 0.0, d3));
            d1++;
            break;
         case ASCENDING_SOUTH:
            this.setDeltaMovement(vec31.add(0.0, 0.0, -d3));
            d1++;
      }

      vec31 = this.getDeltaMovement();
      Pair<Vec3i, Vec3i> pair = AbstractMinecart.exits(railshape);
      Vec3i vec3i = (Vec3i)pair.getFirst();
      Vec3i vec3i1 = (Vec3i)pair.getSecond();
      double d4 = vec3i1.getX() - vec3i.getX();
      double d5 = vec3i1.getZ() - vec3i.getZ();
      double d6 = Math.sqrt(d4 * d4 + d5 * d5);
      double d7 = vec31.x * d4 + vec31.z * d5;
      if (d7 < 0.0) {
         d4 = -d4;
         d5 = -d5;
      }

      double d8 = Math.min(2.0, vec31.horizontalDistance());
      vec31 = new Vec3(d8 * d4 / d6, vec31.y, d8 * d5 / d6);
      this.setDeltaMovement(vec31);
      Entity entity = this.minecart.getFirstPassenger();
      Vec3 vec32;
      if (this.minecart.getFirstPassenger() instanceof ServerPlayer serverplayer) {
         vec32 = serverplayer.getLastClientMoveIntent();
      } else {
         vec32 = Vec3.ZERO;
      }

      if (entity instanceof Player && vec32.lengthSqr() > 0.0) {
         Vec3 vec35 = vec32.normalize();
         double d22 = this.getDeltaMovement().horizontalDistanceSqr();
         if (vec35.lengthSqr() > 0.0 && d22 < 0.01) {
            this.setDeltaMovement(this.getDeltaMovement().add(vec32.x * 0.001, 0.0, vec32.z * 0.001));
            flag1 = false;
         }
      }

      if (flag1) {
         double d20 = this.getDeltaMovement().horizontalDistance();
         if (d20 < 0.03) {
            this.setDeltaMovement(Vec3.ZERO);
         } else {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.0, 0.5));
         }
      }

      double d21 = blockpos.getX() + 0.5 + vec3i.getX() * 0.5;
      double d9 = blockpos.getZ() + 0.5 + vec3i.getZ() * 0.5;
      double d10 = blockpos.getX() + 0.5 + vec3i1.getX() * 0.5;
      double d11 = blockpos.getZ() + 0.5 + vec3i1.getZ() * 0.5;
      d4 = d10 - d21;
      d5 = d11 - d9;
      double d12;
      if (d4 == 0.0) {
         d12 = d2 - blockpos.getZ();
      } else if (d5 == 0.0) {
         d12 = d0 - blockpos.getX();
      } else {
         double d13 = d0 - d21;
         double d14 = d2 - d9;
         d12 = (d13 * d4 + d14 * d5) * 2.0;
      }

      d0 = d21 + d4 * d12;
      d2 = d9 + d5 * d12;
      this.setPos(d0, d1, d2);
      double d23 = this.minecart.isVehicle() ? 0.75 : 1.0;
      double d24 = this.minecart.getMaxSpeed(p_458640_);
      vec31 = this.getDeltaMovement();
      this.minecart.move(MoverType.SELF, new Vec3(Mth.clamp(d23 * vec31.x, -d24, d24), 0.0, Mth.clamp(d23 * vec31.z, -d24, d24)));
      if (vec3i.getY() != 0
         && Mth.floor(this.minecart.getX()) - blockpos.getX() == vec3i.getX()
         && Mth.floor(this.minecart.getZ()) - blockpos.getZ() == vec3i.getZ()) {
         this.setPos(this.minecart.getX(), this.minecart.getY() + vec3i.getY(), this.minecart.getZ());
      } else if (vec3i1.getY() != 0
         && Mth.floor(this.minecart.getX()) - blockpos.getX() == vec3i1.getX()
         && Mth.floor(this.minecart.getZ()) - blockpos.getZ() == vec3i1.getZ()) {
         this.setPos(this.minecart.getX(), this.minecart.getY() + vec3i1.getY(), this.minecart.getZ());
      }

      this.setDeltaMovement(this.minecart.applyNaturalSlowdown(this.getDeltaMovement()));
      Vec3 vec33 = this.getPos(this.minecart.getX(), this.minecart.getY(), this.minecart.getZ());
      if (vec33 != null && vec3 != null) {
         double d15 = (vec3.y - vec33.y) * 0.05;
         Vec3 vec34 = this.getDeltaMovement();
         double d16 = vec34.horizontalDistance();
         if (d16 > 0.0) {
            this.setDeltaMovement(vec34.multiply((d16 + d15) / d16, 1.0, (d16 + d15) / d16));
         }

         this.setPos(this.minecart.getX(), vec33.y, this.minecart.getZ());
      }

      int j = Mth.floor(this.minecart.getX());
      int i = Mth.floor(this.minecart.getZ());
      if (j != blockpos.getX() || i != blockpos.getZ()) {
         Vec3 vec36 = this.getDeltaMovement();
         double d25 = vec36.horizontalDistance();
         this.setDeltaMovement(d25 * (j - blockpos.getX()), vec36.y, d25 * (i - blockpos.getZ()));
      }

      if (flag) {
         Vec3 vec37 = this.getDeltaMovement();
         double d26 = vec37.horizontalDistance();
         if (d26 > 0.01) {
            double d17 = 0.06;
            this.setDeltaMovement(vec37.add(vec37.x / d26 * 0.06, 0.0, vec37.z / d26 * 0.06));
         } else {
            Vec3 vec38 = this.getDeltaMovement();
            double d18 = vec38.x;
            double d19 = vec38.z;
            if (railshape == RailShape.EAST_WEST) {
               if (this.minecart.isRedstoneConductor(blockpos.west())) {
                  d18 = 0.02;
               } else if (this.minecart.isRedstoneConductor(blockpos.east())) {
                  d18 = -0.02;
               }
            } else {
               if (railshape != RailShape.NORTH_SOUTH) {
                  return;
               }

               if (this.minecart.isRedstoneConductor(blockpos.north())) {
                  d19 = 0.02;
               } else if (this.minecart.isRedstoneConductor(blockpos.south())) {
                  d19 = -0.02;
               }
            }

            this.setDeltaMovement(d18, vec38.y, d19);
         }
      }
   }

   public @Nullable Vec3 getPosOffs(double p_451118_, double p_458616_, double p_456337_, double p_451592_) {
      int i = Mth.floor(p_451118_);
      int j = Mth.floor(p_458616_);
      int k = Mth.floor(p_456337_);
      if (this.level().getBlockState(new BlockPos(i, j - 1, k)).is(BlockTags.RAILS)) {
         j--;
      }

      BlockState blockstate = this.level().getBlockState(new BlockPos(i, j, k));
      if (BaseRailBlock.isRail(blockstate)) {
         RailShape railshape = blockstate.getValue(((BaseRailBlock)blockstate.getBlock()).getShapeProperty());
         p_458616_ = j;
         if (railshape.isSlope()) {
            p_458616_ = j + 1;
         }

         Pair<Vec3i, Vec3i> pair = AbstractMinecart.exits(railshape);
         Vec3i vec3i = (Vec3i)pair.getFirst();
         Vec3i vec3i1 = (Vec3i)pair.getSecond();
         double d0 = vec3i1.getX() - vec3i.getX();
         double d1 = vec3i1.getZ() - vec3i.getZ();
         double d2 = Math.sqrt(d0 * d0 + d1 * d1);
         d0 /= d2;
         d1 /= d2;
         p_451118_ += d0 * p_451592_;
         p_456337_ += d1 * p_451592_;
         if (vec3i.getY() != 0 && Mth.floor(p_451118_) - i == vec3i.getX() && Mth.floor(p_456337_) - k == vec3i.getZ()) {
            p_458616_ += vec3i.getY();
         } else if (vec3i1.getY() != 0 && Mth.floor(p_451118_) - i == vec3i1.getX() && Mth.floor(p_456337_) - k == vec3i1.getZ()) {
            p_458616_ += vec3i1.getY();
         }

         return this.getPos(p_451118_, p_458616_, p_456337_);
      } else {
         return null;
      }
   }

   public @Nullable Vec3 getPos(double p_451903_, double p_459361_, double p_450954_) {
      int i = Mth.floor(p_451903_);
      int j = Mth.floor(p_459361_);
      int k = Mth.floor(p_450954_);
      if (this.level().getBlockState(new BlockPos(i, j - 1, k)).is(BlockTags.RAILS)) {
         j--;
      }

      BlockState blockstate = this.level().getBlockState(new BlockPos(i, j, k));
      if (BaseRailBlock.isRail(blockstate)) {
         RailShape railshape = blockstate.getValue(((BaseRailBlock)blockstate.getBlock()).getShapeProperty());
         Pair<Vec3i, Vec3i> pair = AbstractMinecart.exits(railshape);
         Vec3i vec3i = (Vec3i)pair.getFirst();
         Vec3i vec3i1 = (Vec3i)pair.getSecond();
         double d0 = i + 0.5 + vec3i.getX() * 0.5;
         double d1 = j + 0.0625 + vec3i.getY() * 0.5;
         double d2 = k + 0.5 + vec3i.getZ() * 0.5;
         double d3 = i + 0.5 + vec3i1.getX() * 0.5;
         double d4 = j + 0.0625 + vec3i1.getY() * 0.5;
         double d5 = k + 0.5 + vec3i1.getZ() * 0.5;
         double d6 = d3 - d0;
         double d7 = (d4 - d1) * 2.0;
         double d8 = d5 - d2;
         double d9;
         if (d6 == 0.0) {
            d9 = p_450954_ - k;
         } else if (d8 == 0.0) {
            d9 = p_451903_ - i;
         } else {
            double d10 = p_451903_ - d0;
            double d11 = p_450954_ - d2;
            d9 = (d10 * d6 + d11 * d8) * 2.0;
         }

         p_451903_ = d0 + d6 * d9;
         p_459361_ = d1 + d7 * d9;
         p_450954_ = d2 + d8 * d9;
         if (d7 < 0.0) {
            p_459361_++;
         } else if (d7 > 0.0) {
            p_459361_ += 0.5;
         }

         return new Vec3(p_451903_, p_459361_, p_450954_);
      } else {
         return null;
      }
   }

   @Override
   public double stepAlongTrack(BlockPos p_457697_, RailShape p_453398_, double p_457117_) {
      return 0.0;
   }

   @Override
   public boolean pushAndPickupEntities() {
      AABB aabb = this.minecart.getBoundingBox().inflate(0.2F, 0.0, 0.2F);
      if (this.minecart.isRideable() && this.getDeltaMovement().horizontalDistanceSqr() >= 0.01) {
         List<Entity> list = this.level().getEntities(this.minecart, aabb, EntitySelector.pushableBy(this.minecart));
         if (!list.isEmpty()) {
            for (Entity entity1 : list) {
               if (!(entity1 instanceof Player)
                  && !(entity1 instanceof IronGolem)
                  && !(entity1 instanceof AbstractMinecart)
                  && !this.minecart.isVehicle()
                  && !entity1.isPassenger()) {
                  entity1.startRiding(this.minecart);
               } else {
                  entity1.push(this.minecart);
               }
            }
         }
      } else {
         for (Entity entity : this.level().getEntities(this.minecart, aabb)) {
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
   public Vec3 getKnownMovement(Vec3 p_458443_) {
      return !Double.isNaN(p_458443_.x) && !Double.isNaN(p_458443_.y) && !Double.isNaN(p_458443_.z)
         ? new Vec3(Mth.clamp(p_458443_.x, -0.4, 0.4), p_458443_.y, Mth.clamp(p_458443_.z, -0.4, 0.4))
         : Vec3.ZERO;
   }

   @Override
   public double getMaxSpeed(ServerLevel p_455315_) {
      return this.minecart.isInWater() ? 0.2 : 0.4;
   }

   @Override
   public double getSlowdownFactor() {
      return this.minecart.isVehicle() ? 0.997 : 0.96;
   }
}
