package net.minecraft.world.item.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public abstract class SingleItemRecipe implements Recipe<SingleRecipeInput> {
   protected final Recipe.CommonInfo commonInfo;
   private final Ingredient input;
   private final ItemStackTemplate result;
   private @Nullable PlacementInfo placementInfo;

   public SingleItemRecipe(final Recipe.CommonInfo commonInfo, final Ingredient input, final ItemStackTemplate result) {
      this.commonInfo = commonInfo;
      this.input = input;
      this.result = result;
   }

   @Override
   public abstract RecipeSerializer<? extends SingleItemRecipe> getSerializer();

   @Override
   public abstract RecipeType<? extends SingleItemRecipe> getType();

   public boolean matches(final SingleRecipeInput input, final Level level) {
      return this.input.test(input.item());
   }

   @Override
   public boolean showNotification() {
      return this.commonInfo.showNotification();
   }

   public Ingredient input() {
      return this.input;
   }

   protected ItemStackTemplate result() {
      return this.result;
   }

   @Override
   public PlacementInfo placementInfo() {
      if (this.placementInfo == null) {
         this.placementInfo = PlacementInfo.create(this.input);
      }

      return this.placementInfo;
   }

   public ItemStack assemble(final SingleRecipeInput input) {
      return this.result.create();
   }

   public static <T extends SingleItemRecipe> MapCodec<T> simpleMapCodec(final SingleItemRecipe.Factory<T> factory) {
      return RecordCodecBuilder.mapCodec(
         i -> i.group(
               Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
               Ingredient.CODEC.fieldOf("ingredient").forGetter(SingleItemRecipe::input),
               ItemStackTemplate.CODEC.fieldOf("result").forGetter(SingleItemRecipe::result)
            )
            .apply(i, factory::create)
      );
   }

   public static <T extends SingleItemRecipe> StreamCodec<RegistryFriendlyByteBuf, T> simpleStreamCodec(final SingleItemRecipe.Factory<T> factory) {
      return StreamCodec.composite(
         Recipe.CommonInfo.STREAM_CODEC,
         o -> o.commonInfo,
         Ingredient.CONTENTS_STREAM_CODEC,
         SingleItemRecipe::input,
         ItemStackTemplate.STREAM_CODEC,
         SingleItemRecipe::result,
         factory::create
      );
   }

   @FunctionalInterface
   public interface Factory<T extends SingleItemRecipe> {
      T create(Recipe.CommonInfo commonInfo, Ingredient ingredient, ItemStackTemplate result);
   }
}
