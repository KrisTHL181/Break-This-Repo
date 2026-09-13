package net.minecraft.world.item.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

public class ShieldDecorationRecipe extends CustomRecipe {
   public static final MapCodec<ShieldDecorationRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Ingredient.CODEC.fieldOf("banner").forGetter(o -> o.banner),
            Ingredient.CODEC.fieldOf("target").forGetter(o -> o.target),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result)
         )
         .apply(i, ShieldDecorationRecipe::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ShieldDecorationRecipe> STREAM_CODEC = StreamCodec.composite(
      Ingredient.CONTENTS_STREAM_CODEC,
      o -> o.banner,
      Ingredient.CONTENTS_STREAM_CODEC,
      o -> o.target,
      ItemStackTemplate.STREAM_CODEC,
      o -> o.result,
      ShieldDecorationRecipe::new
   );
   public static final RecipeSerializer<ShieldDecorationRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
   private final Ingredient banner;
   private final Ingredient target;
   private final ItemStackTemplate result;

   public ShieldDecorationRecipe(final Ingredient banner, final Ingredient target, final ItemStackTemplate result) {
      this.banner = banner;
      this.target = target;
      this.result = result;
   }

   public boolean matches(final CraftingInput input, final Level level) {
      if (input.ingredientCount() != 2) {
         return false;
      }

      boolean hasClearTarget = false;
      boolean hasPatternBanner = false;

      for (int slot = 0; slot < input.size(); slot++) {
         ItemStack itemStack = input.getItem(slot);
         if (!itemStack.isEmpty()) {
            if (this.banner.test(itemStack) && itemStack.getItem() instanceof BannerItem) {
               if (hasPatternBanner) {
                  return false;
               }

               hasPatternBanner = true;
            } else {
               if (!this.target.test(itemStack)) {
                  return false;
               }

               if (hasClearTarget) {
                  return false;
               }

               BannerPatternLayers patterns = itemStack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
               if (!patterns.layers().isEmpty()) {
                  return false;
               }

               hasClearTarget = true;
            }
         }
      }

      return hasClearTarget && hasPatternBanner;
   }

   public ItemStack assemble(final CraftingInput input) {
      BannerPatternLayers patterns = null;
      DyeColor baseColor = DyeColor.WHITE;
      ItemStack target = ItemStack.EMPTY;

      for (int slot = 0; slot < input.size(); slot++) {
         ItemStack itemStack = input.getItem(slot);
         if (!itemStack.isEmpty()) {
            if (this.banner.test(itemStack) && itemStack.getItem() instanceof BannerItem bannerItem) {
               patterns = itemStack.get(DataComponents.BANNER_PATTERNS);
               baseColor = bannerItem.getColor();
            } else if (this.target.test(itemStack)) {
               target = itemStack;
            }
         }
      }

      ItemStack result = TransmuteRecipe.createWithOriginalComponents(this.result, target);
      result.set(DataComponents.BANNER_PATTERNS, patterns);
      result.set(DataComponents.BASE_COLOR, baseColor);
      return result;
   }

   @Override
   public RecipeSerializer<ShieldDecorationRecipe> getSerializer() {
      return SERIALIZER;
   }
}
