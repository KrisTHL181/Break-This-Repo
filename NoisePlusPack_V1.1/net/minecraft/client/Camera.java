package net.minecraft.client;

import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class Camera implements TrackedWaypoint.Camera {
   private static final float DEFAULT_CAMERA_DISTANCE = 4.0F;
   private static final Vector3f FORWARDS = new Vector3f(0.0F, 0.0F, -1.0F);
   private static final Vector3f UP = new Vector3f(0.0F, 1.0F, 0.0F);
   private static final Vector3f LEFT = new Vector3f(-1.0F, 0.0F, 0.0F);
   private boolean initialized;
   private Level level;
   private Entity entity;
   private Vec3 position = Vec3.ZERO;
   private final BlockPos.MutableBlockPos blockPosition = new BlockPos.MutableBlockPos();
   private final Vector3f forwards = new Vector3f(FORWARDS);
   private final Vector3f up = new Vector3f(UP);
   private final Vector3f left = new Vector3f(LEFT);
   private float xRot;
   private float yRot;
   private final Quaternionf rotation = new Quaternionf();
   private boolean detached;
   private float eyeHeight;
   private float eyeHeightOld;
   private float partialTickTime;
   private final EnvironmentAttributeProbe attributeProbe = new EnvironmentAttributeProbe();

   public void setup(Level p_458249_, Entity p_90577_, boolean p_90578_, boolean p_90579_, float p_90580_) {
      this.initialized = true;
      this.level = p_458249_;
      this.entity = p_90577_;
      this.detached = p_90578_;
      this.partialTickTime = p_90580_;
      if (p_90577_.isPassenger()
         && p_90577_.getVehicle() instanceof Minecart minecart
         && minecart.getBehavior() instanceof NewMinecartBehavior newminecartbehavior
         && newminecartbehavior.cartHasPosRotLerp()) {
         Vec3 vec3 = minecart.getPassengerRidingPosition(p_90577_)
            .subtract(minecart.position())
            .subtract(p_90577_.getVehicleAttachmentPoint(minecart))
            .add(new Vec3(0.0, Mth.lerp(p_90580_, this.eyeHeightOld, this.eyeHeight), 0.0));
         this.setRotation(p_90577_.getViewYRot(p_90580_), p_90577_.getViewXRot(p_90580_));
         this.setPosition(newminecartbehavior.getCartLerpPosition(p_90580_).add(vec3));
      } else {
         this.setRotation(p_90577_.getViewYRot(p_90580_), p_90577_.getViewXRot(p_90580_));
         this.setPosition(
            Mth.lerp(p_90580_, p_90577_.xo, p_90577_.getX()),
            Mth.lerp(p_90580_, p_90577_.yo, p_90577_.getY()) + Mth.lerp(p_90580_, this.eyeHeightOld, this.eyeHeight),
            Mth.lerp(p_90580_, p_90577_.zo, p_90577_.getZ())
         );
      }

      if (p_90578_) {
         if (p_90579_) {
            this.setRotation(this.yRot + 180.0F, -this.xRot);
         }

         float f1 = 4.0F;
         float f2 = 1.0F;
         if (p_90577_ instanceof LivingEntity livingentity1) {
            f2 = livingentity1.getScale();
            f1 = (float)livingentity1.getAttributeValue(Attributes.CAMERA_DISTANCE);
         }

         float f3 = f2;
         float f = f1;
         if (p_90577_.isPassenger() && p_90577_.getVehicle() instanceof LivingEntity livingentity) {
            f3 = livingentity.getScale();
            f = (float)livingentity.getAttributeValue(Attributes.CAMERA_DISTANCE);
         }

         this.move(-this.getMaxZoom(Math.max(f2 * f1, f3 * f)), 0.0F, 0.0F);
      } else if (p_90577_ instanceof LivingEntity && ((LivingEntity)p_90577_).isSleeping()) {
         Direction direction = ((LivingEntity)p_90577_).getBedOrientation();
         this.setRotation(direction != null ? direction.toYRot() - 180.0F : 0.0F, 0.0F);
         this.move(0.0F, 0.3F, 0.0F);
      }
   }

   public void tick() {
      if (this.entity != null) {
         this.eyeHeightOld = this.eyeHeight;
         this.eyeHeight = this.eyeHeight + (this.entity.getEyeHeight() - this.eyeHeight) * 0.5F;
         this.attributeProbe.tick(this.level, this.position);
      }
   }

   private float getMaxZoom(float p_345111_) {
      float f = 0.1F;

      for (int i = 0; i < 8; i++) {
         float f1 = (i & 1) * 2 - 1;
         float f2 = (i >> 1 & 1) * 2 - 1;
         float f3 = (i >> 2 & 1) * 2 - 1;
         Vec3 vec3 = this.position.add(f1 * 0.1F, f2 * 0.1F, f3 * 0.1F);
         Vec3 vec31 = vec3.add(new Vec3(this.forwards).scale(-p_345111_));
         HitResult hitresult = this.level.clip(new ClipContext(vec3, vec31, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, this.entity));
         if (hitresult.getType() != HitResult.Type.MISS) {
            float f4 = (float)hitresult.getLocation().distanceToSqr(this.position);
            if (f4 < Mth.square(p_345111_)) {
               p_345111_ = Mth.sqrt(f4);
            }
         }
      }

      return p_345111_;
   }

   protected void move(float p_343871_, float p_343008_, float p_343953_) {
      Vector3f vector3f = new Vector3f(p_343953_, p_343008_, -p_343871_).rotate(this.rotation);
      this.setPosition(new Vec3(this.position.x + vector3f.x, this.position.y + vector3f.y, this.position.z + vector3f.z));
   }

   protected void setRotation(float p_90573_, float p_90574_) {
      this.xRot = p_90574_;
      this.yRot = p_90573_;
      this.rotation.rotationYXZ((float) Math.PI - p_90573_ * (float) (Math.PI / 180.0), -p_90574_ * (float) (Math.PI / 180.0), 0.0F);
      FORWARDS.rotate(this.rotation, this.forwards);
      UP.rotate(this.rotation, this.up);
      LEFT.rotate(this.rotation, this.left);
   }

   protected void setPosition(double p_90585_, double p_90586_, double p_90587_) {
      this.setPosition(new Vec3(p_90585_, p_90586_, p_90587_));
   }

   protected void setPosition(Vec3 p_90582_) {
      this.position = p_90582_;
      this.blockPosition.set(p_90582_.x, p_90582_.y, p_90582_.z);
   }

   @Override
   public Vec3 position() {
      return this.position;
   }

   public BlockPos blockPosition() {
      return this.blockPosition;
   }

   public float xRot() {
      return this.xRot;
   }

   public float yRot() {
      return this.yRot;
   }

   @Override
   public float yaw() {
      return Mth.wrapDegrees(this.yRot());
   }

   public Quaternionf rotation() {
      return this.rotation;
   }

   public Entity entity() {
      return this.entity;
   }

   public boolean isInitialized() {
      return this.initialized;
   }

   public boolean isDetached() {
      return this.detached;
   }

   public EnvironmentAttributeProbe attributeProbe() {
      return this.attributeProbe;
   }

   public Camera.NearPlane getNearPlane() {
      Minecraft minecraft = Minecraft.getInstance();
      double d0 = (double)minecraft.getWindow().getWidth() / minecraft.getWindow().getHeight();
      double d1 = Math.tan(minecraft.options.fov().get().intValue() * (float) (Math.PI / 180.0) / 2.0) * 0.05F;
      double d2 = d1 * d0;
      Vec3 vec3 = new Vec3(this.forwards).scale(0.05F);
      Vec3 vec31 = new Vec3(this.left).scale(d2);
      Vec3 vec32 = new Vec3(this.up).scale(d1);
      return new Camera.NearPlane(vec3, vec31, vec32);
   }

   public FogType getFluidInCamera() {
      if (!this.initialized) {
         return FogType.NONE;
      }

      FluidState fluidstate = this.level.getFluidState(this.blockPosition);
      if (fluidstate.is(FluidTags.WATER) && this.position.y < this.blockPosition.getY() + fluidstate.getHeight(this.level, this.blockPosition)) {
         return FogType.WATER;
      }

      Camera.NearPlane camera$nearplane = this.getNearPlane();

      for (Vec3 vec3 : Arrays.asList(
         camera$nearplane.forward,
         camera$nearplane.getTopLeft(),
         camera$nearplane.getTopRight(),
         camera$nearplane.getBottomLeft(),
         camera$nearplane.getBottomRight()
      )) {
         Vec3 vec31 = this.position.add(vec3);
         BlockPos blockpos = BlockPos.containing(vec31);
         FluidState fluidstate1 = this.level.getFluidState(blockpos);
         if (fluidstate1.is(FluidTags.LAVA)) {
            if (vec31.y <= fluidstate1.getHeight(this.level, blockpos) + blockpos.getY()) {
               return FogType.LAVA;
            }
         } else {
            BlockState blockstate = this.level.getBlockState(blockpos);
            if (blockstate.is(Blocks.POWDER_SNOW)) {
               return FogType.POWDER_SNOW;
            }
         }
      }

      return FogType.NONE;
   }

   public Vector3fc forwardVector() {
      return this.forwards;
   }

   public Vector3fc upVector() {
      return this.up;
   }

   public Vector3fc leftVector() {
      return this.left;
   }

   public void reset() {
      this.level = null;
      this.entity = null;
      this.attributeProbe.reset();
      this.initialized = false;
   }

   public float getPartialTickTime() {
      return this.partialTickTime;
   }

   @OnlyIn(Dist.CLIENT)
   public static class NearPlane {
      final Vec3 forward;
      private final Vec3 left;
      private final Vec3 up;

      NearPlane(Vec3 p_167691_, Vec3 p_167692_, Vec3 p_167693_) {
         this.forward = p_167691_;
         this.left = p_167692_;
         this.up = p_167693_;
      }

      public Vec3 getTopLeft() {
         return this.forward.add(this.up).add(this.left);
      }

      public Vec3 getTopRight() {
         return this.forward.add(this.up).subtract(this.left);
      }

      public Vec3 getBottomLeft() {
         return this.forward.subtract(this.up).add(this.left);
      }

      public Vec3 getBottomRight() {
         return this.forward.subtract(this.up).subtract(this.left);
      }

      public Vec3 getPointOnPlane(float p_167696_, float p_167697_) {
         return this.forward.add(this.up.scale(p_167697_)).subtract(this.left.scale(p_167696_));
      }
   }
}
