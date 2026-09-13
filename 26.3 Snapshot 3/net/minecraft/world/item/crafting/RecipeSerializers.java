package net.minecraft.world.item.crafting;

import net.minecraft.core.Registry;

public class RecipeSerializers {
   public static Object bootstrap(final Registry<RecipeSerializer<?>> registry) {
      Registry.register(registry, "crafting_shaped", ShapedRecipe.SERIALIZER);
      Registry.register(registry, "crafting_shapeless", ShapelessRecipe.SERIALIZER);
      Registry.register(registry, "crafting_dye", DyeRecipe.SERIALIZER);
      Registry.register(registry, "crafting_imbue", ImbueRecipe.SERIALIZER);
      Registry.register(registry, "crafting_transmute", TransmuteRecipe.SERIALIZER);
      Registry.register(registry, "crafting_decorated_pot", DecoratedPotRecipe.SERIALIZER);
      Registry.register(registry, "crafting_special_bookcloning", BookCloningRecipe.SERIALIZER);
      Registry.register(registry, "crafting_special_mapextending", MapExtendingRecipe.SERIALIZER);
      Registry.register(registry, "crafting_special_firework_rocket", FireworkRocketRecipe.SERIALIZER);
      Registry.register(registry, "crafting_special_firework_star", FireworkStarRecipe.SERIALIZER);
      Registry.register(registry, "crafting_special_firework_star_fade", FireworkStarFadeRecipe.SERIALIZER);
      Registry.register(registry, "crafting_special_bannerduplicate", BannerDuplicateRecipe.SERIALIZER);
      Registry.register(registry, "crafting_special_shielddecoration", ShieldDecorationRecipe.SERIALIZER);
      Registry.register(registry, "crafting_special_repairitem", RepairItemRecipe.SERIALIZER);
      Registry.register(registry, "smelting", SmeltingRecipe.SERIALIZER);
      Registry.register(registry, "blasting", BlastingRecipe.SERIALIZER);
      Registry.register(registry, "smoking", SmokingRecipe.SERIALIZER);
      Registry.register(registry, "campfire_cooking", CampfireCookingRecipe.SERIALIZER);
      Registry.register(registry, "stonecutting", StonecutterRecipe.SERIALIZER);
      Registry.register(registry, "smithing_transform", SmithingTransformRecipe.SERIALIZER);
      Registry.register(registry, "brewing", BrewingRecipe.SERIALIZER);
      return Registry.register(registry, "smithing_trim", SmithingTrimRecipe.SERIALIZER);
   }
}
