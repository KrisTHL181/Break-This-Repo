package net.minecraft.world.entity;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record EntityDimensions(float width, float height, float eyeHeight, EntityAttachments attachments, boolean fixed) {
   private EntityDimensions(float p_20381_, float p_20382_, boolean p_20383_) {
      this(p_20381_, p_20382_, defaultEyeHeight(p_20382_), EntityAttachments.createDefault(p_20381_, p_20382_), p_20383_);
   }

   private static float defaultEyeHeight(float p_331315_) {
      return p_331315_ * 0.85F;
   }

   public AABB makeBoundingBox(Vec3 p_20394_) {
      return this.makeBoundingBox(p_20394_.x, p_20394_.y, p_20394_.z);
   }

   public AABB makeBoundingBox(double p_20385_, double p_20386_, double p_20387_) {
      float f = this.width / 2.0F;
      float f1 = this.height;
      return new AABB(p_20385_ - f, p_20386_, p_20387_ - f, p_20385_ + f, p_20386_ + f1, p_20387_ + f);
   }

   public EntityDimensions scale(float p_20389_) {
      return this.scale(p_20389_, p_20389_);
   }

   public EntityDimensions scale(float p_20391_, float p_20392_) {
      return this.fixed || p_20391_ == 1.0F && p_20392_ == 1.0F
         ? this
         : new EntityDimensions(
            this.width * p_20391_, this.height * p_20392_, this.eyeHeight * p_20392_, this.attachments.scale(p_20391_, p_20392_, p_20391_), false
         );
   }

   public static EntityDimensions scalable(float p_20396_, float p_20397_) {
      return new EntityDimensions(p_20396_, p_20397_, false);
   }

   public static EntityDimensions fixed(float p_20399_, float p_20400_) {
      return new EntityDimensions(p_20399_, p_20400_, true);
   }

   public EntityDimensions withEyeHeight(float p_333362_) {
      return new EntityDimensions(this.width, this.height, p_333362_, this.attachments, this.fixed);
   }

   public EntityDimensions withAttachments(EntityAttachments.Builder p_328127_) {
      return new EntityDimensions(this.width, this.height, this.eyeHeight, p_328127_.build(this.width, this.height), this.fixed);
   }
}
