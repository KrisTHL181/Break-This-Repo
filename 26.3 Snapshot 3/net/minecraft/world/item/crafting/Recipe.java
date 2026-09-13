package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

public interface Recipe<T extends RecipeInput> {
   Codec<Recipe<?>> CODEC = BuiltInRegistries.RECIPE_SERIALIZER.byNameCodec().dispatch(Recipe::getSerializer, RecipeSerializer::codec);
   Codec<ResourceKey<Recipe<?>>> KEY_CODEC = ResourceKey.codec(Registries.RECIPE);
   StreamCodec<RegistryFriendlyByteBuf, Recipe<?>> STREAM_CODEC = ByteBufCodecs.registry(Registries.RECIPE_SERIALIZER)
      .dispatch(Recipe::getSerializer, RecipeSerializer::streamCodec);

   boolean matches(T input, Level level);

   ItemStack assemble(T input);

   default boolean isSpecial() {
      return false;
   }

   boolean showNotification();

   String group();

   RecipeSerializer<? extends Recipe<T>> getSerializer();

   RecipeType<? extends Recipe<T>> getType();

   PlacementInfo placementInfo();

   default List<RecipeDisplay> display() {
      return List.of();
   }

   RecipeBookCategory recipeBookCategory();

   interface BookInfo<CategoryType> {
      CategoryType category();

      String group();

      static <CategoryType, SelfType extends Recipe.BookInfo<CategoryType>> MapCodec<SelfType> mapCodec(
         final Codec<CategoryType> categoryCodec, final CategoryType defaultCategory, final Recipe.BookInfo.Constructor<CategoryType, SelfType> constructor
      ) {
         return RecordCodecBuilder.mapCodec(
            i -> i.group(
                  categoryCodec.optionalFieldOf("category", defaultCategory).forGetter(Recipe.BookInfo::category),
                  Codec.STRING.optionalFieldOf("group", "").forGetter(Recipe.BookInfo::group)
               )
               .apply(i, constructor)
         );
      }

      static <CategoryType, SelfType extends Recipe.BookInfo<CategoryType>> StreamCodec<RegistryFriendlyByteBuf, SelfType> streamCodec(
         final StreamCodec<? super RegistryFriendlyByteBuf, CategoryType> categoryCodec, final Recipe.BookInfo.Constructor<CategoryType, SelfType> constructor
      ) {
         return StreamCodec.composite(categoryCodec, Recipe.BookInfo::category, ByteBufCodecs.STRING_UTF8, Recipe.BookInfo::group, constructor);
      }

      @FunctionalInterface
      interface Constructor<CategoryType, SelfType extends Recipe.BookInfo<CategoryType>> extends BiFunction<CategoryType, String, SelfType> {
      }
   }

   record CommonInfo(boolean showNotification) {
      public static final MapCodec<Recipe.CommonInfo> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(Recipe.CommonInfo::showNotification)).apply(i, Recipe.CommonInfo::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, Recipe.CommonInfo> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.BOOL, Recipe.CommonInfo::showNotification, Recipe.CommonInfo::new
      );
   }
}
