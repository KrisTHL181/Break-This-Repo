package net.minecraft.client.renderer.block.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Type;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public record ItemTransform(Vector3fc rotation, Vector3fc translation, Vector3fc scale) {
   public static final ItemTransform NO_TRANSFORM = new ItemTransform(new Vector3f(), new Vector3f(), new Vector3f(1.0F, 1.0F, 1.0F));

   public void apply(boolean p_111764_, PoseStack.Pose p_397991_) {
      if (this == NO_TRANSFORM) {
         p_397991_.translate(-0.5F, -0.5F, -0.5F);
      } else {
         float f;
         float f1;
         float f2;
         if (p_111764_) {
            f = -this.translation.x();
            f1 = -this.rotation.y();
            f2 = -this.rotation.z();
         } else {
            f = this.translation.x();
            f1 = this.rotation.y();
            f2 = this.rotation.z();
         }

         p_397991_.translate(f, this.translation.y(), this.translation.z());
         p_397991_.rotate(
            new Quaternionf().rotationXYZ(this.rotation.x() * (float) (Math.PI / 180.0), f1 * (float) (Math.PI / 180.0), f2 * (float) (Math.PI / 180.0))
         );
         p_397991_.scale(this.scale.x(), this.scale.y(), this.scale.z());
         p_397991_.translate(-0.5F, -0.5F, -0.5F);
      }
   }

   @OnlyIn(Dist.CLIENT)
   protected static class Deserializer implements JsonDeserializer<ItemTransform> {
      private static final Vector3f DEFAULT_ROTATION = new Vector3f(0.0F, 0.0F, 0.0F);
      private static final Vector3f DEFAULT_TRANSLATION = new Vector3f(0.0F, 0.0F, 0.0F);
      private static final Vector3f DEFAULT_SCALE = new Vector3f(1.0F, 1.0F, 1.0F);
      public static final float MAX_TRANSLATION = 5.0F;
      public static final float MAX_SCALE = 4.0F;

      public ItemTransform deserialize(JsonElement p_111775_, Type p_111776_, JsonDeserializationContext p_111777_) throws JsonParseException {
         JsonObject jsonobject = p_111775_.getAsJsonObject();
         Vector3f vector3f = this.getVector3f(jsonobject, "rotation", DEFAULT_ROTATION);
         Vector3f vector3f1 = this.getVector3f(jsonobject, "translation", DEFAULT_TRANSLATION);
         vector3f1.mul(0.0625F);
         vector3f1.set(Mth.clamp(vector3f1.x, -5.0F, 5.0F), Mth.clamp(vector3f1.y, -5.0F, 5.0F), Mth.clamp(vector3f1.z, -5.0F, 5.0F));
         Vector3f vector3f2 = this.getVector3f(jsonobject, "scale", DEFAULT_SCALE);
         vector3f2.set(Mth.clamp(vector3f2.x, -4.0F, 4.0F), Mth.clamp(vector3f2.y, -4.0F, 4.0F), Mth.clamp(vector3f2.z, -4.0F, 4.0F));
         return new ItemTransform(vector3f, vector3f1, vector3f2);
      }

      private Vector3f getVector3f(JsonObject p_111779_, String p_111780_, Vector3f p_253777_) {
         if (!p_111779_.has(p_111780_)) {
            return p_253777_;
         }

         JsonArray jsonarray = GsonHelper.getAsJsonArray(p_111779_, p_111780_);
         if (jsonarray.size() != 3) {
            throw new JsonParseException("Expected 3 " + p_111780_ + " values, found: " + jsonarray.size());
         }

         float[] afloat = new float[3];

         for (int i = 0; i < afloat.length; i++) {
            afloat[i] = GsonHelper.convertToFloat(jsonarray.get(i), p_111780_ + "[" + i + "]");
         }

         return new Vector3f(afloat[0], afloat[1], afloat[2]);
      }
   }
}
