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

   private EntityAttachments(final Map<EntityAttachment, List<Vec3>> attachments) {
      this.attachments = attachments;
   }

   public static EntityAttachments createDefault(final float width, final float height) {
      return builder().build(width, height);
   }

   public static EntityAttachments.Builder builder() {
      return new EntityAttachments.Builder();
   }

   public EntityAttachments scale(final float x, final float y, final float z) {
      return new EntityAttachments(Util.makeEnumMap(EntityAttachment.class, attachment -> {
         List<Vec3> list = new ArrayList<>();

         for (Vec3 vec3 : this.attachments.get(attachment)) {
            list.add(vec3.multiply(x, y, z));
         }

         return list;
      }));
   }

   public @Nullable Vec3 getNullable(final EntityAttachment attachment, final int index, final float rotY) {
      List<Vec3> points = this.attachments.get(attachment);
      return index >= 0 && index < points.size() ? transformPoint(points.get(index), rotY) : null;
   }

   public Vec3 get(final EntityAttachment attachment, final int index, final float rotY) {
      Vec3 point = this.getNullable(attachment, index, rotY);
      if (point == null) {
         throw new IllegalStateException("Had no attachment point of type: " + attachment + " for index: " + index);
      } else {
         return point;
      }
   }

   public Vec3 getAverage(final EntityAttachment attachment) {
      List<Vec3> points = this.attachments.get(attachment);
      if (points != null && !points.isEmpty()) {
         Vec3 sum = Vec3.ZERO;

         for (Vec3 point : points) {
            sum = sum.add(point);
         }

         return sum.scale(1.0F / points.size());
      } else {
         throw new IllegalStateException("No attachment points of type: PASSENGER");
      }
   }

   public Vec3 getClamped(final EntityAttachment attachment, final int index, final float rotY) {
      List<Vec3> points = this.attachments.get(attachment);
      if (points.isEmpty()) {
         throw new IllegalStateException("Had no attachment points of type: " + attachment);
      }

      Vec3 point = points.get(Mth.clamp(index, 0, points.size() - 1));
      return transformPoint(point, rotY);
   }

   private static Vec3 transformPoint(final Vec3 point, final float rotY) {
      return point.yRot(-rotY * (float) (Math.PI / 180.0));
   }

   public static class Builder {
      private final Map<EntityAttachment, List<Vec3>> attachments = new EnumMap<>(EntityAttachment.class);

      private Builder() {
      }

      public EntityAttachments.Builder attach(final EntityAttachment attachment, final float x, final float y, final float z) {
         return this.attach(attachment, new Vec3(x, y, z));
      }

      public EntityAttachments.Builder attach(final EntityAttachment attachment, final Vec3 point) {
         this.attachments.computeIfAbsent(attachment, a -> new ArrayList<>(1)).add(point);
         return this;
      }

      public EntityAttachments build(final float width, final float height) {
         Map<EntityAttachment, List<Vec3>> attachments = Util.makeEnumMap(EntityAttachment.class, attachment -> {
            List<Vec3> points = this.attachments.get(attachment);
            return points == null ? attachment.createFallbackPoints(width, height) : List.copyOf(points);
         });
         return new EntityAttachments(attachments);
      }
   }
}
