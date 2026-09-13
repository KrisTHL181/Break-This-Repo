package net.minecraft.client.renderer.block.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.math.Quadrant;
import java.lang.reflect.Type;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record BlockElementFace(@Nullable Direction cullForDirection, int tintIndex, String texture, BlockElementFace.@Nullable UVs uvs, Quadrant rotation) {
   public static final int NO_TINT = -1;

   public static float getU(BlockElementFace.UVs p_396140_, Quadrant p_396737_, int p_395337_) {
      return p_396140_.getVertexU(p_396737_.rotateVertexIndex(p_395337_)) / 16.0F;
   }

   public static float getV(BlockElementFace.UVs p_397165_, Quadrant p_391897_, int p_393770_) {
      return p_397165_.getVertexV(p_391897_.rotateVertexIndex(p_393770_)) / 16.0F;
   }

   @OnlyIn(Dist.CLIENT)
   protected static class Deserializer implements JsonDeserializer<BlockElementFace> {
      private static final int DEFAULT_TINT_INDEX = -1;
      private static final int DEFAULT_ROTATION = 0;

      public BlockElementFace deserialize(JsonElement p_111365_, Type p_111366_, JsonDeserializationContext p_111367_) throws JsonParseException {
         JsonObject jsonobject = p_111365_.getAsJsonObject();
         Direction direction = getCullFacing(jsonobject);
         int i = getTintIndex(jsonobject);
         String s = getTexture(jsonobject);
         BlockElementFace.UVs blockelementface$uvs = getUVs(jsonobject);
         Quadrant quadrant = getRotation(jsonobject);
         return new BlockElementFace(direction, i, s, blockelementface$uvs, quadrant);
      }

      private static int getTintIndex(JsonObject p_111369_) {
         return GsonHelper.getAsInt(p_111369_, "tintindex", -1);
      }

      private static String getTexture(JsonObject p_111371_) {
         return GsonHelper.getAsString(p_111371_, "texture");
      }

      private static @Nullable Direction getCullFacing(JsonObject p_111373_) {
         String s = GsonHelper.getAsString(p_111373_, "cullface", "");
         return Direction.byName(s);
      }

      private static Quadrant getRotation(JsonObject p_396944_) {
         int i = GsonHelper.getAsInt(p_396944_, "rotation", 0);
         return Quadrant.parseJson(i);
      }

      private static BlockElementFace.@Nullable UVs getUVs(JsonObject p_395346_) {
         if (!p_395346_.has("uv")) {
            return null;
         }

         JsonArray jsonarray = GsonHelper.getAsJsonArray(p_395346_, "uv");
         if (jsonarray.size() != 4) {
            throw new JsonParseException("Expected 4 uv values, found: " + jsonarray.size());
         }

         float f = GsonHelper.convertToFloat(jsonarray.get(0), "minU");
         float f1 = GsonHelper.convertToFloat(jsonarray.get(1), "minV");
         float f2 = GsonHelper.convertToFloat(jsonarray.get(2), "maxU");
         float f3 = GsonHelper.convertToFloat(jsonarray.get(3), "maxV");
         return new BlockElementFace.UVs(f, f1, f2, f3);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record UVs(float minU, float minV, float maxU, float maxV) {
      public float getVertexU(int p_393086_) {
         return p_393086_ != 0 && p_393086_ != 1 ? this.maxU : this.minU;
      }

      public float getVertexV(int p_396767_) {
         return p_396767_ != 0 && p_396767_ != 3 ? this.maxV : this.minV;
      }
   }
}
