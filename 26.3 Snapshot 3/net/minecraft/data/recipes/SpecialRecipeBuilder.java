package net.minecraft.data.recipes;

import java.util.function.Supplier;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import org.jspecify.annotations.Nullable;

public class SpecialRecipeBuilder {
   private @Nullable RecipeUnlockAdvancementBuilder advancementBuilder;
   private final Supplier<Recipe<?>> factory;

   public SpecialRecipeBuilder(final Supplier<Recipe<?>> factory) {
      this.factory = factory;
   }

   public static SpecialRecipeBuilder special(final Supplier<Recipe<?>> factory) {
      return new SpecialRecipeBuilder(factory);
   }

   public SpecialRecipeBuilder unlockedBy(final String name, final Criterion<?> criterion) {
      if (this.advancementBuilder == null) {
         this.advancementBuilder = new RecipeUnlockAdvancementBuilder();
      }

      this.advancementBuilder.unlockedBy(name, criterion);
      return this;
   }

   public void save(final RecipeOutput output, final String name) {
      this.save(output, ResourceKey.create(Registries.RECIPE, Identifier.parse(name)));
   }

   public void save(final RecipeOutput output, final ResourceKey<Recipe<?>> id) {
      AdvancementHolder advancement;
      if (this.advancementBuilder != null) {
         advancement = this.advancementBuilder.build(output, id, RecipeCategory.MISC);
      } else {
         advancement = null;
      }

      output.accept(id, this.factory.get(), advancement);
   }
}
