package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public abstract class SingleItemRecipe implements Recipe<SingleRecipeInput> {
   private final Ingredient input;
   private final ItemStack result;
   private final String group;
   private @Nullable PlacementInfo placementInfo;

   public SingleItemRecipe(String p_44419_, Ingredient p_44420_, ItemStack p_44421_) {
      this.group = p_44419_;
      this.input = p_44420_;
      this.result = p_44421_;
   }

   @Override
   public abstract RecipeSerializer<? extends SingleItemRecipe> getSerializer();

   @Override
   public abstract RecipeType<? extends SingleItemRecipe> getType();

   public boolean matches(SingleRecipeInput p_365360_, Level p_370085_) {
      return this.input.test(p_365360_.item());
   }

   @Override
   public String group() {
      return this.group;
   }

   public Ingredient input() {
      return this.input;
   }

   protected ItemStack result() {
      return this.result;
   }

   @Override
   public PlacementInfo placementInfo() {
      if (this.placementInfo == null) {
         this.placementInfo = PlacementInfo.create(this.input);
      }

      return this.placementInfo;
   }

   public ItemStack assemble(SingleRecipeInput p_343460_, HolderLookup.Provider p_330907_) {
      return this.result.copy();
   }

   @FunctionalInterface
   public interface Factory<T extends SingleItemRecipe> {
      T create(String var1, Ingredient var2, ItemStack var3);
   }

   public static class Serializer<T extends SingleItemRecipe> implements RecipeSerializer<T> {
      private final MapCodec<T> codec;
      private final StreamCodec<RegistryFriendlyByteBuf, T> streamCodec;

      protected Serializer(SingleItemRecipe.Factory<T> p_311205_) {
         this.codec = RecordCodecBuilder.mapCodec(
            p_359863_ -> p_359863_.group(
                  Codec.STRING.optionalFieldOf("group", "").forGetter(SingleItemRecipe::group),
                  Ingredient.CODEC.fieldOf("ingredient").forGetter(SingleItemRecipe::input),
                  ItemStack.STRICT_CODEC.fieldOf("result").forGetter(SingleItemRecipe::result)
               )
               .apply(p_359863_, p_311205_::create)
         );
         this.streamCodec = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            SingleItemRecipe::group,
            Ingredient.CONTENTS_STREAM_CODEC,
            SingleItemRecipe::input,
            ItemStack.STREAM_CODEC,
            SingleItemRecipe::result,
            p_311205_::create
         );
      }

      @Override
      public MapCodec<T> codec() {
         return this.codec;
      }

      @Override
      public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
         return this.streamCodec;
      }
   }
}
