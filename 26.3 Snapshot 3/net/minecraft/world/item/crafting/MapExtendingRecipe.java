package net.minecraft.world.item.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class MapExtendingRecipe extends CustomRecipe {
   public static final MapCodec<MapExtendingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Ingredient.CODEC.fieldOf("map").forGetter(o -> o.map),
            Ingredient.CODEC.fieldOf("material").forGetter(o -> o.material),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result)
         )
         .apply(i, MapExtendingRecipe::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, MapExtendingRecipe> STREAM_CODEC = StreamCodec.composite(
      Ingredient.CONTENTS_STREAM_CODEC,
      o -> o.map,
      Ingredient.CONTENTS_STREAM_CODEC,
      o -> o.material,
      ItemStackTemplate.STREAM_CODEC,
      o -> o.result,
      MapExtendingRecipe::new
   );
   public static final RecipeSerializer<MapExtendingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
   private final ShapedRecipePattern pattern;
   private final Ingredient map;
   private final Ingredient material;
   private final ItemStackTemplate result;

   public MapExtendingRecipe(final Ingredient map, final Ingredient material, final ItemStackTemplate result) {
      this.map = map;
      this.material = material;
      this.result = result;
      this.pattern = ShapedRecipePattern.of(Map.of('#', material, 'x', map), "###", "#x#", "###");
   }

   public boolean matches(final CraftingInput input, final Level level) {
      if (!this.pattern.matches(input)) {
         return false;
      } else {
         ItemStack map = findFilledMap(input);
         if (map.isEmpty()) {
            return false;
         } else {
            MapItemSavedData data = MapItem.getSavedData(map, level);
            if (data == null) {
               return false;
            } else {
               return data.isExplorationMap() ? false : data.scale < 4;
            }
         }
      }
   }

   public ItemStack assemble(final CraftingInput input) {
      ItemStack sourceMap = findFilledMap(input);
      ItemStack map = TransmuteRecipe.createWithOriginalComponents(this.result, sourceMap);
      map.set(DataComponents.MAP_POST_PROCESSING, MapPostProcessing.SCALE);
      return map;
   }

   private static ItemStack findFilledMap(final CraftingInput input) {
      for (int i = 0; i < input.size(); i++) {
         ItemStack itemStack = input.getItem(i);
         if (itemStack.has(DataComponents.MAP_ID)) {
            return itemStack;
         }
      }

      return ItemStack.EMPTY;
   }

   @Override
   public RecipeSerializer<MapExtendingRecipe> getSerializer() {
      return SERIALIZER;
   }
}
