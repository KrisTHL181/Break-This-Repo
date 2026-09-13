package net.minecraft.world.item.crafting;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;

public record RecipeHolder<T extends Recipe<?>>(ResourceKey<Recipe<?>> id, T value) {
   public static final StreamCodec<RegistryFriendlyByteBuf, RecipeHolder<?>> STREAM_CODEC = StreamCodec.composite(
      ResourceKey.streamCodec(Registries.RECIPE), RecipeHolder::id, Recipe.STREAM_CODEC, RecipeHolder::value, RecipeHolder::new
   );

   @Override
   public boolean equals(Object p_298053_) {
      return this == p_298053_ ? true : p_298053_ instanceof RecipeHolder<?> recipeholder && this.id == recipeholder.id;
   }

   @Override
   public int hashCode() {
      return this.id.hashCode();
   }

   @Override
   public String toString() {
      return this.id.toString();
   }
}
