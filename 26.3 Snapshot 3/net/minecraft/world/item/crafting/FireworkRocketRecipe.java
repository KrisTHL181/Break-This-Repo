package net.minecraft.world.item.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;

public class FireworkRocketRecipe extends CustomRecipe {
   public static final MapCodec<FireworkRocketRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Ingredient.CODEC.fieldOf("shell").forGetter(o -> o.shell),
            Ingredient.CODEC.fieldOf("fuel").forGetter(o -> o.fuel),
            Ingredient.CODEC.fieldOf("star").forGetter(o -> o.star),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result)
         )
         .apply(i, FireworkRocketRecipe::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, FireworkRocketRecipe> STREAM_CODEC = StreamCodec.composite(
      Ingredient.CONTENTS_STREAM_CODEC,
      o -> o.shell,
      Ingredient.CONTENTS_STREAM_CODEC,
      o -> o.fuel,
      Ingredient.CONTENTS_STREAM_CODEC,
      o -> o.star,
      ItemStackTemplate.STREAM_CODEC,
      o -> o.result,
      FireworkRocketRecipe::new
   );
   public static final RecipeSerializer<FireworkRocketRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
   private final Ingredient shell;
   private final Ingredient fuel;
   private final Ingredient star;
   private final ItemStackTemplate result;

   public FireworkRocketRecipe(final Ingredient shell, final Ingredient fuel, final Ingredient star, final ItemStackTemplate result) {
      this.shell = shell;
      this.fuel = fuel;
      this.star = star;
      this.result = result;
   }

   public boolean matches(final CraftingInput input, final Level level) {
      if (input.ingredientCount() < 2) {
         return false;
      }

      boolean hasShell = false;
      int fuelCount = 0;

      for (int slot = 0; slot < input.size(); slot++) {
         ItemStack itemStack = input.getItem(slot);
         if (!itemStack.isEmpty()) {
            if (this.shell.test(itemStack)) {
               if (hasShell) {
                  return false;
               }

               hasShell = true;
            } else if (this.fuel.test(itemStack)) {
               if (++fuelCount > 3) {
                  return false;
               }
            } else if (!this.star.test(itemStack)) {
               return false;
            }
         }
      }

      return hasShell && fuelCount >= 1;
   }

   public ItemStack assemble(final CraftingInput input) {
      List<FireworkExplosion> explosions = new ArrayList<>();
      int fuelCount = 0;

      for (int slot = 0; slot < input.size(); slot++) {
         ItemStack itemStack = input.getItem(slot);
         if (!itemStack.isEmpty()) {
            if (this.fuel.test(itemStack)) {
               fuelCount++;
            } else if (this.star.test(itemStack)) {
               FireworkExplosion explosion = itemStack.get(DataComponents.FIREWORK_EXPLOSION);
               if (explosion != null) {
                  explosions.add(explosion);
               }
            }
         }
      }

      DataComponentPatch components = DataComponentPatch.builder().set(DataComponents.FIREWORKS, new Fireworks(fuelCount, explosions)).build();
      return this.result.apply(components);
   }

   @Override
   public RecipeSerializer<FireworkRocketRecipe> getSerializer() {
      return SERIALIZER;
   }
}
