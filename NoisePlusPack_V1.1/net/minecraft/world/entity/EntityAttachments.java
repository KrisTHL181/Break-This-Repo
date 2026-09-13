package net.minecraft.world.entity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EntityAttachments {
   private final Map<EntityAttachment, List<Vec3>> attachments;

   EntityAttachments(Map<EntityAttachment, List<Vec3>> p_331204_) {
      this.attachments = p_331204_;
   }

   public static EntityAttachments createDefault(float p_329032_, float p_333755_) {
      return builder().build(p_329032_, p_333755_);
   }

   public static EntityAttachments.Builder builder() {
      return new EntityAttachments.Builder();
   }

   public EntityAttachments scale(float p_332347_, float p_335416_, float p_329295_) {
      return new EntityAttachments(Util.makeEnumMap(EntityAttachment.class, p_395775_ -> {
         List<Vec3> list = new ArrayList<>();

         for (Vec3 vec3 : this.attachments.get(p_395775_)) {
            list.add(vec3.multiply(p_332347_, p_335416_, p_329295_));
         }

         return list;
      }));
   }

   public @Nullable Vec3 getNullable(EntityAttachment p_327874_, int p_334745_, float p_333621_) {
      List<Vec3> list = this.attachments.get(p_327874_);
      return p_334745_ >= 0 && p_334745_ < list.size() ? transformPoint(list.get(p_334745_), p_333621_) : null;
   }

   public Vec3 get(EntityAttachment p_329241_, int p_328790_, float p_333537_) {
      Vec3 vec3 = this.getNullable(p_329241_, p_328790_, p_333537_);
      if (vec3 == null) {
         throw new IllegalStateException("Had no attachment point of type: " + p_329241_ + " for index: " + p_328790_);
      } else {
         return vec3;
      }
   }

   public Vec3 getAverage(EntityAttachment p_405988_) {
      List<Vec3> list = this.attachments.get(p_405988_);
      if (list != null && !list.isEmpty()) {
         Vec3 vec3 = Vec3.ZERO;

         for (Vec3 vec31 : list) {
            vec3 = vec3.add(vec31);
         }

         return vec3.scale(1.0F / list.size());
      } else {
         throw new IllegalStateException("No attachment points of type: PASSENGER");
      }
   }

   public Vec3 getClamped(EntityAttachment p_332337_, int p_333181_, float p_335290_) {
      List<Vec3> list = this.attachments.get(p_332337_);
      if (list.isEmpty()) {
         throw new IllegalStateException("Had no attachment points of type: " + p_332337_);
      }

      Vec3 vec3 = list.get(Mth.clamp(p_333181_, 0, list.size() - 1));
      return transformPoint(vec3, p_335290_);
   }

   private static Vec3 transformPoint(Vec3 p_329033_, float p_331796_) {
      return p_329033_.yRot(-p_331796_ * (float) (Math.PI / 180.0));
   }

   public static class Builder {
      private final Map<EntityAttachment, List<Vec3>> attachments = new EnumMap<>(EntityAttachment.class);

      Builder() {
      }

      public EntityAttachments.Builder attach(EntityAttachment p_333943_, float p_333061_, float p_333157_, float p_328995_) {
         return this.attach(p_333943_, new Vec3(p_333061_, p_333157_, p_328995_));
      }

      public EntityAttachments.Builder attach(EntityAttachment p_328839_, Vec3 p_328743_) {
         this.attachments.computeIfAbsent(p_328839_, p_333992_ -> new ArrayList<>(1)).add(p_328743_);
         return this;
      }

      public EntityAttachments build(float p_334466_, float p_334856_) {
         Map<EntityAttachment, List<Vec3>> map = Util.makeEnumMap(EntityAttachment.class, p_390488_ -> {
            List<Vec3> list = this.attachments.get(p_390488_);
            return list == null ? p_390488_.createFallbackPoints(p_334466_, p_334856_) : List.copyOf(list);
         });
         return new EntityAttachments(map);
      }
   }
}
