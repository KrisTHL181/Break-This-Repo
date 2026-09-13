package net.minecraft.world.entity;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record EntityDimensions(float width, float height, float eyeHeight, EntityAttachments attachments, boolean fixed) {
   private EntityDimensions(final float width, final float height, final boolean fixed) {
      this(width, height, defaultEyeHeight(height), EntityAttachments.createDefault(width, height), fixed);
   }

   private static float defaultEyeHeight(final float height) {
      return height * 0.85F;
   }

   public AABB makeBoundingBox(final Vec3 pos) {
      return this.makeBoundingBox(pos.x, pos.y, pos.z);
   }

   public AABB makeBoundingBox(final double x, final double y, final double z) {
      float w = this.width / 2.0F;
      float h = this.height;
      return new AABB(x - w, y, z - w, x + w, y + h, z + w);
   }

   public EntityDimensions scale(final float scaleFactor) {
      return this.scale(scaleFactor, scaleFactor);
   }

   public EntityDimensions scale(final float widthScaleFactor, final float heightScaleFactor) {
      return !this.fixed && (widthScaleFactor != 1.0F || heightScaleFactor != 1.0F)
         ? new EntityDimensions(
            this.width * widthScaleFactor,
            this.height * heightScaleFactor,
            this.eyeHeight * heightScaleFactor,
            this.attachments.scale(widthScaleFactor, heightScaleFactor, widthScaleFactor),
            false
         )
         : this;
   }

   public static EntityDimensions scalable(final float width, final float height) {
      return new EntityDimensions(width, height, false);
   }

   public static EntityDimensions fixed(final float width, final float height) {
      return new EntityDimensions(width, height, true);
   }

   public EntityDimensions withEyeHeight(final float eyeHeight) {
      return new EntityDimensions(this.width, this.height, eyeHeight, this.attachments, this.fixed);
   }

   public EntityDimensions withAttachments(final EntityAttachments.Builder attachments) {
      return new EntityDimensions(this.width, this.height, this.eyeHeight, attachments.build(this.width, this.height), this.fixed);
   }
}
