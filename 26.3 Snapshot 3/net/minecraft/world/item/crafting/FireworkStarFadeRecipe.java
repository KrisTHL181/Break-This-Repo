package net.minecraft.world.item.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.level.Level;

public class FireworkStarFadeRecipe extends CustomRecipe {
   public static final MapCodec<FireworkStarFadeRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Ingredient.CODEC.fieldOf("target").forGetter(o -> o.target),
            Ingredient.CODEC.fieldOf("dye").forGetter(o -> o.dye),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result)
         )
         .apply(i, FireworkStarFadeRecipe::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, FireworkStarFadeRecipe> STREAM_CODEC = StreamCodec.composite(
      Ingredient.CONTENTS_STREAM_CODEC,
      o -> o.target,
      Ingredient.CONTENTS_STREAM_CODEC,
      o -> o.dye,
      ItemStackTemplate.STREAM_CODEC,
      o -> o.result,
      FireworkStarFadeRecipe::new
   );
   public static final RecipeSerializer<FireworkStarFadeRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
   private final Ingredient target;
   private final Ingredient dye;
   private final ItemStackTemplate result;

   public FireworkStarFadeRecipe(final Ingredient target, final Ingredient dye, final ItemStackTemplate result) {
      this.target = target;
      this.dye = dye;
      this.result = result;
   }

   public boolean matches(final CraftingInput input, final Level level) {
      if (input.ingredientCount() < 2) {
         return false;
      }

      boolean hasDye = false;
      boolean hasTarget = false;

      for (int slot = 0; slot < input.size(); slot++) {
         ItemStack itemStack = input.getItem(slot);
         if (!itemStack.isEmpty()) {
            if (this.dye.test(itemStack) && itemStack.has(DataComponents.DYE)) {
               hasDye = true;
            } else {
               if (!this.target.test(itemStack)) {
                  return false;
               }

               if (hasTarget) {
                  return false;
               }

               hasTarget = true;
            }
         }
      }

      return hasTarget && hasDye;
   }

   public ItemStack assemble(final CraftingInput input) {
      IntList colors = new IntArrayList();
      ItemStack targetStack = null;

      for (int slot = 0; slot < input.size(); slot++) {
         ItemStack itemStack = input.getItem(slot);
         if (this.dye.test(itemStack)) {
            DyeColor dye = itemStack.getOrDefault(DataComponents.DYE, DyeColor.WHITE);
            colors.add(dye.getFireworkColor());
         } else if (this.target.test(itemStack)) {
            targetStack = itemStack;
         }
      }

      if (targetStack != null && !colors.isEmpty()) {
         ItemStack result = TransmuteRecipe.createWithOriginalComponents(this.result, targetStack);
         result.update(DataComponents.FIREWORK_EXPLOSION, FireworkExplosion.DEFAULT, colors, FireworkExplosion::withFadeColors);
         return result;
      } else {
         return ItemStack.EMPTY;
      }
   }

   @Override
   public RecipeSerializer<FireworkStarFadeRecipe> getSerializer() {
      return SERIALIZER;
   }
}
