package net.minecraft.client.renderer.block.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record BlockModel(
   @Nullable UnbakedGeometry geometry,
   UnbakedModel.@Nullable GuiLight guiLight,
   @Nullable Boolean ambientOcclusion,
   @Nullable ItemTransforms transforms,
   TextureSlots.Data textureSlots,
   @Nullable Identifier parent
) implements UnbakedModel {
   @VisibleForTesting
   static final Gson GSON = new GsonBuilder()
      .registerTypeAdapter(BlockModel.class, new BlockModel.Deserializer())
      .registerTypeAdapter(BlockElement.class, new BlockElement.Deserializer())
      .registerTypeAdapter(BlockElementFace.class, new BlockElementFace.Deserializer())
      .registerTypeAdapter(ItemTransform.class, new ItemTransform.Deserializer())
      .registerTypeAdapter(ItemTransforms.class, new ItemTransforms.Deserializer())
      .create();

   public static BlockModel fromStream(Reader p_111462_) {
      return GsonHelper.fromJson(GSON, p_111462_, BlockModel.class);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Deserializer implements JsonDeserializer<BlockModel> {
      public BlockModel deserialize(JsonElement p_111498_, Type p_111499_, JsonDeserializationContext p_111500_) throws JsonParseException {
         JsonObject jsonobject = p_111498_.getAsJsonObject();
         UnbakedGeometry unbakedgeometry = this.getElements(p_111500_, jsonobject);
         String s = this.getParentName(jsonobject);
         TextureSlots.Data textureslots$data = this.getTextureMap(jsonobject);
         Boolean obool = this.getAmbientOcclusion(jsonobject);
         ItemTransforms itemtransforms = null;
         if (jsonobject.has("display")) {
            JsonObject jsonobject1 = GsonHelper.getAsJsonObject(jsonobject, "display");
            itemtransforms = (ItemTransforms)p_111500_.deserialize(jsonobject1, ItemTransforms.class);
         }

         UnbakedModel.GuiLight unbakedmodel$guilight = null;
         if (jsonobject.has("gui_light")) {
            unbakedmodel$guilight = UnbakedModel.GuiLight.getByName(GsonHelper.getAsString(jsonobject, "gui_light"));
         }

         Identifier identifier = s.isEmpty() ? null : Identifier.parse(s);
         return new BlockModel(unbakedgeometry, unbakedmodel$guilight, obool, itemtransforms, textureslots$data, identifier);
      }

      private TextureSlots.Data getTextureMap(JsonObject p_111510_) {
         if (p_111510_.has("textures")) {
            JsonObject jsonobject = GsonHelper.getAsJsonObject(p_111510_, "textures");
            return TextureSlots.parseTextureMap(jsonobject);
         } else {
            return TextureSlots.Data.EMPTY;
         }
      }

      private String getParentName(JsonObject p_111512_) {
         return GsonHelper.getAsString(p_111512_, "parent", "");
      }

      protected @Nullable Boolean getAmbientOcclusion(JsonObject p_273052_) {
         return p_273052_.has("ambientocclusion") ? GsonHelper.getAsBoolean(p_273052_, "ambientocclusion") : null;
      }

      protected @Nullable UnbakedGeometry getElements(JsonDeserializationContext p_111507_, JsonObject p_111508_) {
         if (!p_111508_.has("elements")) {
            return null;
         }

         List<BlockElement> list = new ArrayList<>();

         for (JsonElement jsonelement : GsonHelper.getAsJsonArray(p_111508_, "elements")) {
            list.add((BlockElement)p_111507_.deserialize(jsonelement, BlockElement.class));
         }

         return new SimpleUnbakedGeometry(list);
      }
   }
}
