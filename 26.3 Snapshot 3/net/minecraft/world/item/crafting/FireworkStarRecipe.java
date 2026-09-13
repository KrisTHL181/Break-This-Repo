package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class FireworkStarRecipe extends CustomRecipe {
   public static final MapCodec<FireworkStarRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Codec.simpleMap(FireworkExplosion.Shape.CODEC, Ingredient.CODEC, StringRepresentable.keys(FireworkExplosion.Shape.values()))
               .fieldOf("shapes")
               .forGetter(o -> o.shapes),
            Ingredient.CODEC.fieldOf("trail").forGetter(o -> o.trail),
            Ingredient.CODEC.fieldOf("twinkle").forGetter(o -> o.twinkle),
            Ingredient.CODEC.fieldOf("fuel").forGetter(o -> o.fuel),
            Ingredient.CODEC.fieldOf("dye").forGetter(o -> o.dye),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result)
         )
         .apply(i, FireworkStarRecipe::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, FireworkStarRecipe> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.map(HashMap::new, FireworkExplosion.Shape.STREAM_CODEC, Ingredient.CONTENTS_STREAM_CODEC),
      o -> o.shapes,
      Ingredient.CONTENTS_STREAM_CODEC,
      o -> o.trail,
      Ingredient.CONTENTS_STREAM_CODEC,
      o -> o.twinkle,
      Ingredient.CONTENTS_STREAM_CODEC,
      o -> o.fuel,
      Ingredient.CONTENTS_STREAM_CODEC,
      o -> o.dye,
      ItemStackTemplate.STREAM_CODEC,
      o -> o.result,
      FireworkStarRecipe::new
   );
   public static final RecipeSerializer<FireworkStarRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
   private final Map<FireworkExplosion.Shape, Ingredient> shapes;
   private final Ingredient trail;
   private final Ingredient twinkle;
   private final Ingredient fuel;
   private final Ingredient dye;
   private final ItemStackTemplate result;

   public FireworkStarRecipe(
      final Map<FireworkExplosion.Shape, Ingredient> shapes,
      final Ingredient trail,
      final Ingredient twinkle,
      final Ingredient fuel,
      final Ingredient dye,
      final ItemStackTemplate result
   ) {
      this.shapes = shapes;
      this.trail = trail;
      this.twinkle = twinkle;
      this.fuel = fuel;
      this.dye = dye;
      this.result = result;
   }

   private FireworkExplosion.@Nullable Shape findShape(final ItemStack itemStack) {
      for (Entry<FireworkExplosion.Shape, Ingredient> e : this.shapes.entrySet()) {
         if (e.getValue().test(itemStack)) {
            return e.getKey();
         }
      }

      return null;
   }

   public boolean matches(final CraftingInput input, final Level level) {
      if (input.ingredientCount() < 2) {
         return false;
      }

      boolean hasFuel = false;
      boolean hasDye = false;
      boolean hasShape = false;
      boolean hasTrail = false;
      boolean hasTwinkle = false;

      for (int slot = 0; slot < input.size(); slot++) {
         ItemStack itemStack = input.getItem(slot);
         if (!itemStack.isEmpty()) {
            if (this.twinkle.test(itemStack)) {
               if (hasTwinkle) {
                  return false;
               }

               hasTwinkle = true;
            } else if (this.trail.test(itemStack)) {
               if (hasTrail) {
                  return false;
               }

               hasTrail = true;
            } else if (this.fuel.test(itemStack)) {
               if (hasFuel) {
                  return false;
               }

               hasFuel = true;
            } else if (this.dye.test(itemStack) && itemStack.has(DataComponents.DYE)) {
               hasDye = true;
            } else {
               FireworkExplosion.Shape shape = this.findShape(itemStack);
               if (shape == null) {
                  return false;
               }

               if (hasShape) {
                  return false;
               }

               hasShape = true;
            }
         }
      }

      return hasFuel && hasDye;
   }

   public ItemStack assemble(final CraftingInput input) {
      FireworkExplosion.Shape shape = FireworkExplosion.Shape.SMALL_BALL;
      boolean hasTwinkle = false;
      boolean hasTrail = false;
      IntList colors = new IntArrayList();

      for (int slot = 0; slot < input.size(); slot++) {
         ItemStack itemStack = input.getItem(slot);
         if (!itemStack.isEmpty()) {
            FireworkExplosion.Shape maybeShape = this.findShape(itemStack);
            if (maybeShape != null) {
               shape = maybeShape;
            } else if (this.twinkle.test(itemStack)) {
               hasTwinkle = true;
            } else if (this.trail.test(itemStack)) {
               hasTrail = true;
            } else if (this.dye.test(itemStack)) {
               DyeColor dye = itemStack.getOrDefault(DataComponents.DYE, DyeColor.WHITE);
               colors.add(dye.getFireworkColor());
            }
         }
      }

      ItemStack star = this.result.create();
      star.set(DataComponents.FIREWORK_EXPLOSION, new FireworkExplosion(shape, colors, IntList.of(), hasTrail, hasTwinkle));
      return star;
   }

   @Override
   public RecipeSerializer<FireworkStarRecipe> getSerializer() {
      return SERIALIZER;
   }
}
