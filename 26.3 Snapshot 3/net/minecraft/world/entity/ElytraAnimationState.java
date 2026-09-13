package net.minecraft.world.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class ElytraAnimationState {
   private static final float DEFAULT_X_ROT = (float) (Math.PI / 12);
   private static final float DEFAULT_Z_ROT = (float) (-Math.PI / 12);
   private float rotX;
   private float rotY;
   private float rotZ;
   private float rotXOld;
   private float rotYOld;
   private float rotZOld;
   private final LivingEntity entity;

   public ElytraAnimationState(final LivingEntity entity) {
      this.entity = entity;
   }

   public void tick() {
      this.rotXOld = this.rotX;
      this.rotYOld = this.rotY;
      this.rotZOld = this.rotZ;
      float targetXRot;
      float targetZRot;
      float targetYRot;
      if (this.entity.isFallFlying()) {
         float ratio = 1.0F;
         Vec3 movement = this.entity.getDeltaMovement();
         if (movement.y < 0.0) {
            Vec3 vec = movement.normalize();
            ratio = 1.0F - (float)Math.pow(-vec.y, 1.5);
         }

         targetXRot = Mth.lerp(ratio, (float) (Math.PI / 12), (float) (Math.PI / 9));
         targetZRot = Mth.lerp(ratio, (float) (-Math.PI / 12), (float) (-Math.PI / 2));
         targetYRot = 0.0F;
      } else if (this.entity.isCrouching()) {
         targetXRot = (float) (Math.PI * 2.0 / 9.0);
         targetZRot = (float) (-Math.PI / 4);
         targetYRot = 0.08726646F;
      } else {
         targetXRot = (float) (Math.PI / 12);
         targetZRot = (float) (-Math.PI / 12);
         targetYRot = 0.0F;
      }

      this.rotX = this.rotX + (targetXRot - this.rotX) * 0.3F;
      this.rotY = this.rotY + (targetYRot - this.rotY) * 0.3F;
      this.rotZ = this.rotZ + (targetZRot - this.rotZ) * 0.3F;
   }

   public float getRotX(final float partialTicks) {
      return Mth.lerp(partialTicks, this.rotXOld, this.rotX);
   }

   public float getRotY(final float partialTicks) {
      return Mth.lerp(partialTicks, this.rotYOld, this.rotY);
   }

   public float getRotZ(final float partialTicks) {
      return Mth.lerp(partialTicks, this.rotZOld, this.rotZ);
   }
}
