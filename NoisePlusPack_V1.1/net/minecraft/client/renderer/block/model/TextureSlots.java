package net.minecraft.client.renderer.block.model;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class TextureSlots {
   public static final TextureSlots EMPTY = new TextureSlots(Map.of());
   private static final char REFERENCE_CHAR = '#';
   private final Map<String, Material> resolvedValues;

   TextureSlots(Map<String, Material> p_376574_) {
      this.resolvedValues = p_376574_;
   }

   public @Nullable Material getMaterial(String p_375817_) {
      if (isTextureReference(p_375817_)) {
         p_375817_ = p_375817_.substring(1);
      }

      return this.resolvedValues.get(p_375817_);
   }

   private static boolean isTextureReference(String p_376119_) {
      return p_376119_.charAt(0) == '#';
   }

   public static TextureSlots.Data parseTextureMap(JsonObject p_376737_) {
      TextureSlots.Data.Builder textureslots$data$builder = new TextureSlots.Data.Builder();

      for (Entry<String, JsonElement> entry : p_376737_.entrySet()) {
         parseEntry(entry.getKey(), entry.getValue().getAsString(), textureslots$data$builder);
      }

      return textureslots$data$builder.build();
   }

   private static void parseEntry(String p_376233_, String p_378647_, TextureSlots.Data.Builder p_376019_) {
      if (isTextureReference(p_378647_)) {
         p_376019_.addReference(p_376233_, p_378647_.substring(1));
      } else {
         Identifier identifier = Identifier.tryParse(p_378647_);
         if (identifier == null) {
            throw new JsonParseException(p_378647_ + " is not valid resource location");
         }

         p_376019_.addTexture(p_376233_, new Material(ModelManager.BLOCK_OR_ITEM, identifier));
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record Data(Map<String, TextureSlots.SlotContents> values) {
      public static final TextureSlots.Data EMPTY = new TextureSlots.Data(Map.of());

      @OnlyIn(Dist.CLIENT)
      public static class Builder {
         private final Map<String, TextureSlots.SlotContents> textureMap = new HashMap<>();

         public TextureSlots.Data.Builder addReference(String p_375509_, String p_378243_) {
            this.textureMap.put(p_375509_, new TextureSlots.Reference(p_378243_));
            return this;
         }

         public TextureSlots.Data.Builder addTexture(String p_376424_, Material p_377345_) {
            this.textureMap.put(p_376424_, new TextureSlots.Value(p_377345_));
            return this;
         }

         public TextureSlots.Data build() {
            return this.textureMap.isEmpty() ? TextureSlots.Data.EMPTY : new TextureSlots.Data(Map.copyOf(this.textureMap));
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   record Reference(String target) implements TextureSlots.SlotContents {
   }

   @OnlyIn(Dist.CLIENT)
   public static class Resolver {
      private static final Logger LOGGER = LogUtils.getLogger();
      private final List<TextureSlots.Data> entries = new ArrayList<>();

      public TextureSlots.Resolver addLast(TextureSlots.Data p_375723_) {
         this.entries.addLast(p_375723_);
         return this;
      }

      public TextureSlots.Resolver addFirst(TextureSlots.Data p_377819_) {
         this.entries.addFirst(p_377819_);
         return this;
      }

      public TextureSlots resolve(ModelDebugName p_375822_) {
         if (this.entries.isEmpty()) {
            return TextureSlots.EMPTY;
         }

         Object2ObjectMap<String, Material> object2objectmap = new Object2ObjectArrayMap();
         Object2ObjectMap<String, TextureSlots.Reference> object2objectmap1 = new Object2ObjectArrayMap();

         for (TextureSlots.Data textureslots$data : Lists.reverse(this.entries)) {
            textureslots$data.values.forEach((p_376922_, p_376306_) -> {
               switch (p_376306_) {
                  case TextureSlots.Value textureslots$value:
                     object2objectmap1.remove(p_376922_);
                     object2objectmap.put(p_376922_, textureslots$value.material());
                     break;
                  case TextureSlots.Reference textureslots$reference:
                     object2objectmap.remove(p_376922_);
                     object2objectmap1.put(p_376922_, textureslots$reference);
                     break;
                  default:
                     throw new MatchException(null, null);
               }
            });
         }

         if (object2objectmap1.isEmpty()) {
            return new TextureSlots(object2objectmap);
         }

         boolean flag = true;

         while (flag) {
            flag = false;
            ObjectIterator<it.unimi.dsi.fastutil.objects.Object2ObjectMap.Entry<String, TextureSlots.Reference>> objectiterator = Object2ObjectMaps.fastIterator(
               object2objectmap1
            );

            while (objectiterator.hasNext()) {
               it.unimi.dsi.fastutil.objects.Object2ObjectMap.Entry<String, TextureSlots.Reference> entry = (it.unimi.dsi.fastutil.objects.Object2ObjectMap.Entry<String, TextureSlots.Reference>)objectiterator.next();
               Material material = (Material)object2objectmap.get(((TextureSlots.Reference)entry.getValue()).target);
               if (material != null) {
                  object2objectmap.put((String)entry.getKey(), material);
                  objectiterator.remove();
                  flag = true;
               }
            }
         }

         if (!object2objectmap1.isEmpty()) {
            LOGGER.warn(
               "Unresolved texture references in {}:\n{}",
               p_375822_.debugName(),
               object2objectmap1.entrySet()
                  .stream()
                  .map(p_378552_ -> "\t#" + (String)p_378552_.getKey() + "-> #" + ((TextureSlots.Reference)p_378552_.getValue()).target + "\n")
                  .collect(Collectors.joining())
            );
         }

         return new TextureSlots(object2objectmap);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public sealed interface SlotContents permits TextureSlots.Value, TextureSlots.Reference {
   }

   @OnlyIn(Dist.CLIENT)
   record Value(Material material) implements TextureSlots.SlotContents {
   }
}
