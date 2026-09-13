package net.minecraft.world.entity;

import java.util.List;
import net.minecraft.world.phys.Vec3;

public enum EntityAttachment {
   PASSENGER(EntityAttachment.Fallback.AT_HEIGHT),
   VEHICLE(EntityAttachment.Fallback.AT_FEET),
   NAME_TAG(EntityAttachment.Fallback.AT_HEIGHT),
   WARDEN_CHEST(EntityAttachment.Fallback.AT_CENTER);

   private final EntityAttachment.Fallback fallback;

   EntityAttachment(final EntityAttachment.Fallback p_333642_) {
      this.fallback = p_333642_;
   }

   public List<Vec3> createFallbackPoints(float p_330294_, float p_328764_) {
      return this.fallback.create(p_330294_, p_328764_);
   }

   public interface Fallback {
      List<Vec3> ZERO = List.of(Vec3.ZERO);
      EntityAttachment.Fallback AT_FEET = (p_331269_, p_331409_) -> ZERO;
      EntityAttachment.Fallback AT_HEIGHT = (p_331649_, p_328299_) -> List.of(new Vec3(0.0, p_328299_, 0.0));
      EntityAttachment.Fallback AT_CENTER = (p_331512_, p_335776_) -> List.of(new Vec3(0.0, p_335776_ / 2.0, 0.0));

      List<Vec3> create(float var1, float var2);
   }
}
