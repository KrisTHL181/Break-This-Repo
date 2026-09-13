package net.minecraft.world.entity.vehicle.minecart;

import com.mojang.datafixers.util.Pair;
import io.netty.buffer.ByteBuf;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class NewMinecartBehavior extends MinecartBehavior {
   public static final int POS_ROT_LERP_TICKS = 3;
   public static final double ON_RAIL_Y_OFFSET = 0.1;
   public static final double OPPOSING_SLOPES_REST_AT_SPEED_THRESHOLD = 0.005;
   private NewMinecartBehavior.@Nullable StepPartialTicks cacheIndexAlpha;
   private int cachedLerpDelay;
   private float cachedPartialTick;
   private int lerpDelay = 0;
   public final List<NewMinecartBehavior.MinecartStep> lerpSteps = new LinkedList<>();
   public final List<NewMinecartBehavior.MinecartStep> currentLerpSteps = new LinkedList<>();
   public double currentLerpStepsTotalWeight = 0.0;
   public NewMinecartBehavior.MinecartStep oldLerp = NewMinecartBehavior.MinecartStep.ZERO;

   public NewMinecartBehavior(AbstractMinecart p_455785_) {
      super(p_455785_);
   }

   @Override
   public void tick() {
      if (this.level() instanceof ServerLevel serverlevel) {
         BlockPos blockpos = this.minecart.getCurrentBlockPosOrRailBelow();
         BlockState $$4 = this.level().getBlockState(blockpos);
         if (this.minecart.isFirstTick()) {
            this.minecart.setOnRails(BaseRailBlock.isRail($$4));
            this.adjustToRails(blockpos, $$4, true);
         }

         this.minecart.applyGravity();
         this.minecart.moveAlongTrack(serverlevel);
      } else {
         this.lerpClientPositionAndRotation();
         boolean flag = BaseRailBlock.isRail(this.level().getBlockState(this.minecart.getCurrentBlockPosOrRailBelow()));
         this.minecart.setOnRails(flag);
      }
   }

   private void lerpClientPositionAndRotation() {
      if (--this.lerpDelay <= 0) {
         this.setOldLerpValues();
         this.currentLerpSteps.clear();
         if (!this.lerpSteps.isEmpty()) {
            this.currentLerpSteps.addAll(this.lerpSteps);
            this.lerpSteps.clear();
            this.currentLerpStepsTotalWeight = 0.0;

            for (NewMinecartBehavior.MinecartStep newminecartbehavior$minecartstep : this.currentLerpSteps) {
               this.currentLerpStepsTotalWeight = this.currentLerpStepsTotalWeight + newminecartbehavior$minecartstep.weight;
            }

            this.lerpDelay = this.currentLerpStepsTotalWeight == 0.0 ? 0 : 3;
         }
      }

      if (this.cartHasPosRotLerp()) {
         this.setPos(this.getCartLerpPosition(1.0F));
         this.setDeltaMovement(this.getCartLerpMovements(1.0F));
         this.setXRot(this.getCartLerpXRot(1.0F));
         this.setYRot(this.getCartLerpYRot(1.0F));
      }
   }

   public void setOldLerpValues() {
      this.oldLerp = new NewMinecartBehavior.MinecartStep(this.position(), this.getDeltaMovement(), this.getYRot(), this.getXRot(), 0.0F);
   }

   public boolean cartHasPosRotLerp() {
      return !this.currentLerpSteps.isEmpty();
   }

   public float getCartLerpXRot(float p_457347_) {
      NewMinecartBehavior.StepPartialTicks newminecartbehavior$steppartialticks = this.getCurrentLerpStep(p_457347_);
      return Mth.rotLerp(
         newminecartbehavior$steppartialticks.partialTicksInStep,
         newminecartbehavior$steppartialticks.previousStep.xRot,
         newminecartbehavior$steppartialticks.currentStep.xRot
      );
   }

   public float getCartLerpYRot(float p_459384_) {
      NewMinecartBehavior.StepPartialTicks newminecartbehavior$steppartialticks = this.getCurrentLerpStep(p_459384_);
      return Mth.rotLerp(
         newminecartbehavior$steppartialticks.partialTicksInStep,
         newminecartbehavior$steppartialticks.previousStep.yRot,
         newminecartbehavior$steppartialticks.currentStep.yRot
      );
   }

   public Vec3 getCartLerpPosition(float p_450921_) {
      NewMinecartBehavior.StepPartialTicks newminecartbehavior$steppartialticks = this.getCurrentLerpStep(p_450921_);
      return Mth.lerp(
         newminecartbehavior$steppartialticks.partialTicksInStep,
         newminecartbehavior$steppartialticks.previousStep.position,
         newminecartbehavior$steppartialticks.currentStep.position
      );
   }

   public Vec3 getCartLerpMovements(float p_450290_) {
      NewMinecartBehavior.StepPartialTicks newminecartbehavior$steppartialticks = this.getCurrentLerpStep(p_450290_);
      return Mth.lerp(
         newminecartbehavior$steppartialticks.partialTicksInStep,
         newminecartbehavior$steppartialticks.previousStep.movement,
         newminecartbehavior$steppartialticks.currentStep.movement
      );
   }

   private NewMinecartBehavior.StepPartialTicks getCurrentLerpStep(float p_456868_) {
      if (p_456868_ == this.cachedPartialTick && this.lerpDelay == this.cachedLerpDelay && this.cacheIndexAlpha != null) {
         return this.cacheIndexAlpha;
      }

      float f = (3 - this.lerpDelay + p_456868_) / 3.0F;
      float f1 = 0.0F;
      float f2 = 1.0F;
      boolean flag = false;

      int i;
      for (i = 0; i < this.currentLerpSteps.size(); i++) {
         float f3 = this.currentLerpSteps.get(i).weight;
         if (!(f3 <= 0.0F)) {
            f1 += f3;
            if (f1 >= this.currentLerpStepsTotalWeight * f) {
               float f4 = f1 - f3;
               f2 = (float)((f * this.currentLerpStepsTotalWeight - f4) / f3);
               flag = true;
               break;
            }
         }
      }

      if (!flag) {
         i = this.currentLerpSteps.size() - 1;
      }

      NewMinecartBehavior.MinecartStep newminecartbehavior$minecartstep = this.currentLerpSteps.get(i);
      NewMinecartBehavior.MinecartStep newminecartbehavior$minecartstep1 = i > 0 ? this.currentLerpSteps.get(i - 1) : this.oldLerp;
      this.cacheIndexAlpha = new NewMinecartBehavior.StepPartialTicks(f2, newminecartbehavior$minecartstep, newminecartbehavior$minecartstep1);
      this.cachedLerpDelay = this.lerpDelay;
      this.cachedPartialTick = p_456868_;
      return this.cacheIndexAlpha;
   }

   public void adjustToRails(BlockPos p_451133_, BlockState p_453781_, boolean p_453463_) {
      if (BaseRailBlock.isRail(p_453781_)) {
         RailShape railshape = p_453781_.getValue(((BaseRailBlock)p_453781_.getBlock()).getShapeProperty());
         Pair<Vec3i, Vec3i> pair = AbstractMinecart.exits(railshape);
         Vec3 vec3 = new Vec3((Vec3i)pair.getFirst()).scale(0.5);
         Vec3 vec31 = new Vec3((Vec3i)pair.getSecond()).scale(0.5);
         Vec3 vec32 = vec3.horizontal();
         Vec3 vec33 = vec31.horizontal();
         if (this.getDeltaMovement().length() > 1.0E-5F && this.getDeltaMovement().dot(vec32) < this.getDeltaMovement().dot(vec33)
            || this.isDecending(vec33, railshape)) {
            Vec3 vec34 = vec32;
            vec32 = vec33;
            vec33 = vec34;
         }

         float f = 180.0F - (float)(Math.atan2(vec32.z, vec32.x) * 180.0 / Math.PI);
         f += this.minecart.isFlipped() ? 180.0F : 0.0F;
         Vec3 vec35 = this.position();
         boolean flag = vec3.x() != vec31.x() && vec3.z() != vec31.z();
         Vec3 vec36;
         if (flag) {
            Vec3 vec37 = vec31.subtract(vec3);
            Vec3 vec38 = vec35.subtract(p_451133_.getBottomCenter()).subtract(vec3);
            Vec3 vec39 = vec37.scale(vec37.dot(vec38) / vec37.dot(vec37));
            vec36 = p_451133_.getBottomCenter().add(vec3).add(vec39);
            f = 180.0F - (float)(Math.atan2(vec39.z, vec39.x) * 180.0 / Math.PI);
            f += this.minecart.isFlipped() ? 180.0F : 0.0F;
         } else {
            boolean flag1 = vec3.subtract(vec31).x != 0.0;
            boolean flag2 = vec3.subtract(vec31).z != 0.0;
            vec36 = new Vec3(flag2 ? p_451133_.getCenter().x : vec35.x, p_451133_.getY(), flag1 ? p_451133_.getCenter().z : vec35.z);
         }

         Vec3 vec311 = vec36.subtract(vec35);
         this.setPos(vec35.add(vec311));
         float f1 = 0.0F;
         boolean flag3 = vec3.y() != vec31.y();
         if (flag3) {
            Vec3 vec310 = p_451133_.getBottomCenter().add(vec33);
            double d0 = vec310.distanceTo(this.position());
            this.setPos(this.position().add(0.0, d0 + 0.1, 0.0));
            f1 = this.minecart.isFlipped() ? 45.0F : -45.0F;
         } else {
            this.setPos(this.position().add(0.0, 0.1, 0.0));
         }

         this.setRotation(f, f1);
         double d1 = vec35.distanceTo(this.position());
         if (d1 > 0.0) {
            this.lerpSteps
               .add(
                  new NewMinecartBehavior.MinecartStep(this.position(), this.getDeltaMovement(), this.getYRot(), this.getXRot(), p_453463_ ? 0.0F : (float)d1)
               );
         }
      }
   }

   private void setRotation(float p_459636_, float p_452340_) {
      double d0 = Math.abs(p_459636_ - this.getYRot());
      if (d0 >= 175.0 && d0 <= 185.0) {
         this.minecart.setFlipped(!this.minecart.isFlipped());
         p_459636_ -= 180.0F;
         p_452340_ *= -1.0F;
      }

      p_452340_ = Math.clamp(p_452340_, -45.0F, 45.0F);
      this.setXRot(p_452340_ % 360.0F);
      this.setYRot(p_459636_ % 360.0F);
   }

   @Override
   public void moveAlongTrack(ServerLevel p_450194_) {
      for (NewMinecartBehavior.TrackIteration newminecartbehavior$trackiteration = new NewMinecartBehavior.TrackIteration();
         newminecartbehavior$trackiteration.shouldIterate() && this.minecart.isAlive();
         newminecartbehavior$trackiteration.firstIteration = false
      ) {
         Vec3 vec3 = this.getDeltaMovement();
         BlockPos blockpos = this.minecart.getCurrentBlockPosOrRailBelow();
         BlockState blockstate = this.level().getBlockState(blockpos);
         boolean flag = BaseRailBlock.isRail(blockstate);
         if (this.minecart.isOnRails() != flag) {
            this.minecart.setOnRails(flag);
            this.adjustToRails(blockpos, blockstate, false);
         }

         if (flag) {
            this.minecart.resetFallDistance();
            this.minecart.setOldPosAndRot();
            if (blockstate.is(Blocks.ACTIVATOR_RAIL)) {
               this.minecart.activateMinecart(p_450194_, blockpos.getX(), blockpos.getY(), blockpos.getZ(), blockstate.getValue(PoweredRailBlock.POWERED));
            }

            RailShape railshape = blockstate.getValue(((BaseRailBlock)blockstate.getBlock()).getShapeProperty());
            Vec3 vec31 = this.calculateTrackSpeed(p_450194_, vec3.horizontal(), newminecartbehavior$trackiteration, blockpos, blockstate, railshape);
            if (newminecartbehavior$trackiteration.firstIteration) {
               newminecartbehavior$trackiteration.movementLeft = vec31.horizontalDistance();
            } else {
               newminecartbehavior$trackiteration.movementLeft = newminecartbehavior$trackiteration.movementLeft
                  + (vec31.horizontalDistance() - vec3.horizontalDistance());
            }

            this.setDeltaMovement(vec31);
            newminecartbehavior$trackiteration.movementLeft = this.minecart
               .makeStepAlongTrack(blockpos, railshape, newminecartbehavior$trackiteration.movementLeft);
         } else {
            this.minecart.comeOffTrack(p_450194_);
            newminecartbehavior$trackiteration.movementLeft = 0.0;
         }

         Vec3 vec32 = this.position();
         Vec3 vec33 = vec32.subtract(this.minecart.oldPosition());
         double d0 = vec33.length();
         if (d0 > 1.0E-5F) {
            if (!(vec33.horizontalDistanceSqr() > 1.0E-5F)) {
               if (!this.minecart.isOnRails()) {
                  this.setXRot(this.minecart.onGround() ? 0.0F : Mth.rotLerp(0.2F, this.getXRot(), 0.0F));
               }
            } else {
               float f = 180.0F - (float)(Math.atan2(vec33.z, vec33.x) * 180.0 / Math.PI);
               float f1 = this.minecart.onGround() && !this.minecart.isOnRails()
                  ? 0.0F
                  : 90.0F - (float)(Math.atan2(vec33.horizontalDistance(), vec33.y) * 180.0 / Math.PI);
               f += this.minecart.isFlipped() ? 180.0F : 0.0F;
               f1 *= this.minecart.isFlipped() ? -1.0F : 1.0F;
               this.setRotation(f, f1);
            }

            this.lerpSteps
               .add(
                  new NewMinecartBehavior.MinecartStep(
                     vec32, this.getDeltaMovement(), this.getYRot(), this.getXRot(), (float)Math.min(d0, this.getMaxSpeed(p_450194_))
                  )
               );
         } else if (vec3.horizontalDistanceSqr() > 0.0) {
            this.lerpSteps.add(new NewMinecartBehavior.MinecartStep(vec32, this.getDeltaMovement(), this.getYRot(), this.getXRot(), 1.0F));
         }

         if (d0 > 1.0E-5F || newminecartbehavior$trackiteration.firstIteration) {
            this.minecart.applyEffectsFromBlocks();
            this.minecart.applyEffectsFromBlocks();
         }
      }
   }

   private Vec3 calculateTrackSpeed(
      ServerLevel p_450938_, Vec3 p_457096_, NewMinecartBehavior.TrackIteration p_457086_, BlockPos p_456146_, BlockState p_452152_, RailShape p_459639_
   ) {
      Vec3 vec3 = p_457096_;
      if (!p_457086_.hasGainedSlopeSpeed) {
         Vec3 vec31 = this.calculateSlopeSpeed(p_457096_, p_459639_);
         if (vec31.horizontalDistanceSqr() != p_457096_.horizontalDistanceSqr()) {
            p_457086_.hasGainedSlopeSpeed = true;
            vec3 = vec31;
         }
      }

      if (p_457086_.firstIteration) {
         Vec3 vec32 = this.calculatePlayerInputSpeed(vec3);
         if (vec32.horizontalDistanceSqr() != vec3.horizontalDistanceSqr()) {
            p_457086_.hasHalted = true;
            vec3 = vec32;
         }
      }

      if (!p_457086_.hasHalted) {
         Vec3 vec33 = this.calculateHaltTrackSpeed(vec3, p_452152_);
         if (vec33.horizontalDistanceSqr() != vec3.horizontalDistanceSqr()) {
            p_457086_.hasHalted = true;
            vec3 = vec33;
         }
      }

      if (p_457086_.firstIteration) {
         vec3 = this.minecart.applyNaturalSlowdown(vec3);
         if (vec3.lengthSqr() > 0.0) {
            double d0 = Math.min(vec3.length(), this.minecart.getMaxSpeed(p_450938_));
            vec3 = vec3.normalize().scale(d0);
         }
      }

      if (!p_457086_.hasBoosted) {
         Vec3 vec34 = this.calculateBoostTrackSpeed(vec3, p_456146_, p_452152_);
         if (vec34.horizontalDistanceSqr() != vec3.horizontalDistanceSqr()) {
            p_457086_.hasBoosted = true;
            vec3 = vec34;
         }
      }

      return vec3;
   }

   private Vec3 calculateSlopeSpeed(Vec3 p_454343_, RailShape p_456315_) {
      double d0 = Math.max(0.0078125, p_454343_.horizontalDistance() * 0.02);
      if (this.minecart.isInWater()) {
         d0 *= 0.2;
      }
      return switch (p_456315_) {
         case ASCENDING_EAST -> p_454343_.add(-d0, 0.0, 0.0);
         case ASCENDING_WEST -> p_454343_.add(d0, 0.0, 0.0);
         case ASCENDING_NORTH -> p_454343_.add(0.0, 0.0, d0);
         case ASCENDING_SOUTH -> p_454343_.add(0.0, 0.0, -d0);
         default -> p_454343_;
      };
   }

   private Vec3 calculatePlayerInputSpeed(Vec3 p_455045_) {
      if (this.minecart.getFirstPassenger() instanceof ServerPlayer serverplayer) {
         Vec3 vec31 = serverplayer.getLastClientMoveIntent();
         if (vec31.lengthSqr() > 0.0) {
            Vec3 vec3 = vec31.normalize();
            double d0 = p_455045_.horizontalDistanceSqr();
            if (vec3.lengthSqr() > 0.0 && d0 < 0.01) {
               return p_455045_.add(new Vec3(vec3.x, 0.0, vec3.z).normalize().scale(0.001));
            }
         }

         return p_455045_;
      } else {
         return p_455045_;
      }
   }

   private Vec3 calculateHaltTrackSpeed(Vec3 p_460405_, BlockState p_457203_) {
      if (p_457203_.is(Blocks.POWERED_RAIL) && !p_457203_.getValue(PoweredRailBlock.POWERED)) {
         return p_460405_.length() < 0.03 ? Vec3.ZERO : p_460405_.scale(0.5);
      } else {
         return p_460405_;
      }
   }

   private Vec3 calculateBoostTrackSpeed(Vec3 p_451672_, BlockPos p_453710_, BlockState p_457216_) {
      if (p_457216_.is(Blocks.POWERED_RAIL) && p_457216_.getValue(PoweredRailBlock.POWERED)) {
         if (p_451672_.length() > 0.01) {
            return p_451672_.normalize().scale(p_451672_.length() + 0.06);
         }

         Vec3 vec3 = this.minecart.getRedstoneDirection(p_453710_);
         return vec3.lengthSqr() <= 0.0 ? p_451672_ : vec3.scale(p_451672_.length() + 0.2);
      } else {
         return p_451672_;
      }
   }

   @Override
   public double stepAlongTrack(BlockPos p_453796_, RailShape p_452428_, double p_459888_) {
      if (p_459888_ < 1.0E-5F) {
         return 0.0;
      }

      Vec3 vec3 = this.position();
      Pair<Vec3i, Vec3i> pair = AbstractMinecart.exits(p_452428_);
      Vec3i vec3i = (Vec3i)pair.getFirst();
      Vec3i vec3i1 = (Vec3i)pair.getSecond();
      Vec3 vec31 = this.getDeltaMovement().horizontal();
      if (vec31.length() < 1.0E-5F) {
         this.setDeltaMovement(Vec3.ZERO);
         return 0.0;
      }

      boolean flag = vec3i.getY() != vec3i1.getY();
      Vec3 vec32 = new Vec3(vec3i1).scale(0.5).horizontal();
      Vec3 vec33 = new Vec3(vec3i).scale(0.5).horizontal();
      if (vec31.dot(vec33) < vec31.dot(vec32)) {
         vec33 = vec32;
      }

      Vec3 vec34 = p_453796_.getBottomCenter().add(vec33).add(0.0, 0.1, 0.0).add(vec33.normalize().scale(1.0E-5F));
      if (flag && !this.isDecending(vec31, p_452428_)) {
         vec34 = vec34.add(0.0, 1.0, 0.0);
      }

      Vec3 vec35 = vec34.subtract(this.position()).normalize();
      vec31 = vec35.scale(vec31.length() / vec35.horizontalDistance());
      Vec3 vec36 = vec3.add(vec31.normalize().scale(p_459888_ * (flag ? Mth.SQRT_OF_TWO : 1.0F)));
      if (vec3.distanceToSqr(vec34) <= vec3.distanceToSqr(vec36)) {
         p_459888_ = vec34.subtract(vec36).horizontalDistance();
         vec36 = vec34;
      } else {
         p_459888_ = 0.0;
      }

      this.minecart.move(MoverType.SELF, vec36.subtract(vec3));
      BlockState blockstate = this.level().getBlockState(BlockPos.containing(vec36));
      if (flag) {
         if (BaseRailBlock.isRail(blockstate)) {
            RailShape railshape = blockstate.getValue(((BaseRailBlock)blockstate.getBlock()).getShapeProperty());
            if (this.restAtVShape(p_452428_, railshape)) {
               return 0.0;
            }
         }

         double d1 = vec34.horizontal().distanceTo(this.position().horizontal());
         double d0 = vec34.y + (this.isDecending(vec31, p_452428_) ? d1 : -d1);
         if (this.position().y < d0) {
            this.setPos(this.position().x, d0, this.position().z);
         }
      }

      if (this.position().distanceTo(vec3) < 1.0E-5F && vec36.distanceTo(vec3) > 1.0E-5F) {
         this.setDeltaMovement(Vec3.ZERO);
         return 0.0;
      } else {
         this.setDeltaMovement(vec31);
         return p_459888_;
      }
   }

   private boolean restAtVShape(RailShape p_452620_, RailShape p_458030_) {
      if (this.getDeltaMovement().lengthSqr() < 0.005
         && p_458030_.isSlope()
         && this.isDecending(this.getDeltaMovement(), p_452620_)
         && !this.isDecending(this.getDeltaMovement(), p_458030_)) {
         this.setDeltaMovement(Vec3.ZERO);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public double getMaxSpeed(ServerLevel p_452151_) {
      return p_452151_.getGameRules().get(GameRules.MAX_MINECART_SPEED).intValue() * (this.minecart.isInWater() ? 0.5 : 1.0) / 20.0;
   }

   private boolean isDecending(Vec3 p_453777_, RailShape p_456662_) {
      return switch (p_456662_) {
         case ASCENDING_EAST -> p_453777_.x < 0.0;
         case ASCENDING_WEST -> p_453777_.x > 0.0;
         case ASCENDING_NORTH -> p_453777_.z > 0.0;
         case ASCENDING_SOUTH -> p_453777_.z < 0.0;
         default -> false;
      };
   }

   @Override
   public double getSlowdownFactor() {
      return this.minecart.isVehicle() ? 0.997 : 0.975;
   }

   @Override
   public boolean pushAndPickupEntities() {
      boolean flag = this.pickupEntities(this.minecart.getBoundingBox().inflate(0.2, 0.0, 0.2));
      if (!this.minecart.horizontalCollision && !this.minecart.verticalCollision) {
         return false;
      }

      boolean flag1 = this.pushEntities(this.minecart.getBoundingBox().inflate(1.0E-7));
      return flag && !flag1;
   }

   public boolean pickupEntities(AABB p_453454_) {
      if (this.minecart.isRideable() && !this.minecart.isVehicle()) {
         List<Entity> list = this.level().getEntities(this.minecart, p_453454_, EntitySelector.pushableBy(this.minecart));
         if (!list.isEmpty()) {
            for (Entity entity : list) {
               if (!(entity instanceof Player)
                  && !(entity instanceof IronGolem)
                  && !(entity instanceof AbstractMinecart)
                  && !this.minecart.isVehicle()
                  && !entity.isPassenger()) {
                  boolean flag = entity.startRiding(this.minecart);
                  if (flag) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   public boolean pushEntities(AABB p_457159_) {
      boolean flag = false;
      if (this.minecart.isRideable()) {
         List<Entity> list = this.level().getEntities(this.minecart, p_457159_, EntitySelector.pushableBy(this.minecart));
         if (!list.isEmpty()) {
            for (Entity entity : list) {
               if (entity instanceof Player
                  || entity instanceof IronGolem
                  || entity instanceof AbstractMinecart
                  || this.minecart.isVehicle()
                  || entity.isPassenger()) {
                  entity.push(this.minecart);
                  flag = true;
               }
            }
         }
      } else {
         for (Entity entity1 : this.level().getEntities(this.minecart, p_457159_)) {
            if (!this.minecart.hasPassenger(entity1) && entity1.isPushable() && entity1 instanceof AbstractMinecart) {
               entity1.push(this.minecart);
               flag = true;
            }
         }
      }

      return flag;
   }

   public record MinecartStep(Vec3 position, Vec3 movement, float yRot, float xRot, float weight) {
      public static final StreamCodec<ByteBuf, NewMinecartBehavior.MinecartStep> STREAM_CODEC = StreamCodec.composite(
         Vec3.STREAM_CODEC,
         NewMinecartBehavior.MinecartStep::position,
         Vec3.STREAM_CODEC,
         NewMinecartBehavior.MinecartStep::movement,
         ByteBufCodecs.ROTATION_BYTE,
         NewMinecartBehavior.MinecartStep::yRot,
         ByteBufCodecs.ROTATION_BYTE,
         NewMinecartBehavior.MinecartStep::xRot,
         ByteBufCodecs.FLOAT,
         NewMinecartBehavior.MinecartStep::weight,
         NewMinecartBehavior.MinecartStep::new
      );
      public static NewMinecartBehavior.MinecartStep ZERO = new NewMinecartBehavior.MinecartStep(Vec3.ZERO, Vec3.ZERO, 0.0F, 0.0F, 0.0F);
   }

   record StepPartialTicks(float partialTicksInStep, NewMinecartBehavior.MinecartStep currentStep, NewMinecartBehavior.MinecartStep previousStep) {
   }

   static class TrackIteration {
      double movementLeft = 0.0;
      boolean firstIteration = true;
      boolean hasGainedSlopeSpeed = false;
      boolean hasHalted = false;
      boolean hasBoosted = false;

      public boolean shouldIterate() {
         return this.firstIteration || this.movementLeft > 1.0E-5F;
      }
   }
}
