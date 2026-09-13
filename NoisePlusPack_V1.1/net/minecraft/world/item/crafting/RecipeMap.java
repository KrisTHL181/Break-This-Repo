package net.minecraft.world.item.crafting;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class RecipeMap {
   public static final RecipeMap EMPTY = new RecipeMap(ImmutableMultimap.of(), Map.of());
   private final Multimap<RecipeType<?>, RecipeHolder<?>> byType;
   private final Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey;

   private RecipeMap(Multimap<RecipeType<?>, RecipeHolder<?>> p_370021_, Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> p_362655_) {
      this.byType = p_370021_;
      this.byKey = p_362655_;
   }

   public static RecipeMap create(Iterable<RecipeHolder<?>> p_369802_) {
      Builder<RecipeType<?>, RecipeHolder<?>> builder = ImmutableMultimap.builder();
      com.google.common.collect.ImmutableMap.Builder<ResourceKey<Recipe<?>>, RecipeHolder<?>> builder1 = ImmutableMap.builder();

      for (RecipeHolder<?> recipeholder : p_369802_) {
         builder.put(recipeholder.value().getType(), recipeholder);
         builder1.put(recipeholder.id(), recipeholder);
      }

      return new RecipeMap(builder.build(), builder1.build());
   }

   public <I extends RecipeInput, T extends Recipe<I>> Collection<RecipeHolder<T>> byType(RecipeType<T> p_365239_) {
      return this.byType.get(p_365239_);
   }

   public Collection<RecipeHolder<?>> values() {
      return this.byKey.values();
   }

   public @Nullable RecipeHolder<?> byKey(ResourceKey<Recipe<?>> p_363032_) {
      return this.byKey.get(p_363032_);
   }

   public <I extends RecipeInput, T extends Recipe<I>> Stream<RecipeHolder<T>> getRecipesFor(RecipeType<T> p_365973_, I p_364991_, Level p_364879_) {
      return p_364991_.isEmpty() ? Stream.empty() : this.byType(p_365973_).stream().filter(p_363912_ -> p_363912_.value().matches(p_364991_, p_364879_));
   }
}
