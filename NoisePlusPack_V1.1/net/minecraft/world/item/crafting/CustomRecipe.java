package net.minecraft.world.item.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public abstract class CustomRecipe implements CraftingRecipe {
   private final CraftingBookCategory category;

   public CustomRecipe(CraftingBookCategory p_249010_) {
      this.category = p_249010_;
   }

   @Override
   public boolean isSpecial() {
      return true;
   }

   @Override
   public CraftingBookCategory category() {
      return this.category;
   }

   @Override
   public PlacementInfo placementInfo() {
      return PlacementInfo.NOT_PLACEABLE;
   }

   @Override
   public abstract RecipeSerializer<? extends CustomRecipe> getSerializer();

   public static class Serializer<T extends CraftingRecipe> implements RecipeSerializer<T> {
      private final MapCodec<T> codec;
      private final StreamCodec<RegistryFriendlyByteBuf, T> streamCodec;

      public Serializer(CustomRecipe.Serializer.Factory<T> p_361018_) {
         this.codec = RecordCodecBuilder.mapCodec(
            p_362686_ -> p_362686_.group(CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(CraftingRecipe::category))
               .apply(p_362686_, p_361018_::create)
         );
         this.streamCodec = StreamCodec.composite(CraftingBookCategory.STREAM_CODEC, CraftingRecipe::category, p_361018_::create);
      }

      @Override
      public MapCodec<T> codec() {
         return this.codec;
      }

      @Override
      public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
         return this.streamCodec;
      }

      @FunctionalInterface
      public interface Factory<T extends CraftingRecipe> {
         T create(CraftingBookCategory var1);
      }
   }
}
