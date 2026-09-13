package net.minecraft.world.entity.animal.feline;

import java.util.List;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.variant.MoonBrightnessCheck;
import net.minecraft.world.entity.variant.PriorityProvider;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;
import net.minecraft.world.entity.variant.StructureCheck;
import net.minecraft.world.level.levelgen.structure.Structure;

public interface CatVariants {
   ResourceKey<CatVariant> TABBY = createKey("tabby");
   ResourceKey<CatVariant> BLACK = createKey("black");
   ResourceKey<CatVariant> RED = createKey("red");
   ResourceKey<CatVariant> SIAMESE = createKey("siamese");
   ResourceKey<CatVariant> BRITISH_SHORTHAIR = createKey("british_shorthair");
   ResourceKey<CatVariant> CALICO = createKey("calico");
   ResourceKey<CatVariant> PERSIAN = createKey("persian");
   ResourceKey<CatVariant> RAGDOLL = createKey("ragdoll");
   ResourceKey<CatVariant> WHITE = createKey("white");
   ResourceKey<CatVariant> JELLIE = createKey("jellie");
   ResourceKey<CatVariant> ALL_BLACK = createKey("all_black");

   private static ResourceKey<CatVariant> createKey(final String name) {
      return ResourceKey.create(Registries.CAT_VARIANT, Identifier.withDefaultNamespace(name));
   }

   static void bootstrap(final BootstrapContext<CatVariant> context) {
      HolderGetter<Structure> structures = context.lookup(Registries.STRUCTURE);
      registerForAnyConditions(context, TABBY, "entity/cat/cat_tabby", "entity/cat/cat_tabby_baby");
      registerForAnyConditions(context, BLACK, "entity/cat/cat_black", "entity/cat/cat_black_baby");
      registerForAnyConditions(context, RED, "entity/cat/cat_red", "entity/cat/cat_red_baby");
      registerForAnyConditions(context, SIAMESE, "entity/cat/cat_siamese", "entity/cat/cat_siamese_baby");
      registerForAnyConditions(context, BRITISH_SHORTHAIR, "entity/cat/cat_british_shorthair", "entity/cat/cat_british_shorthair_baby");
      registerForAnyConditions(context, CALICO, "entity/cat/cat_calico", "entity/cat/cat_calico_baby");
      registerForAnyConditions(context, PERSIAN, "entity/cat/cat_persian", "entity/cat/cat_persian_baby");
      registerForAnyConditions(context, RAGDOLL, "entity/cat/cat_ragdoll", "entity/cat/cat_ragdoll_baby");
      registerForAnyConditions(context, WHITE, "entity/cat/cat_white", "entity/cat/cat_white_baby");
      registerForAnyConditions(context, JELLIE, "entity/cat/cat_jellie", "entity/cat/cat_jellie_baby");
      register(
         context,
         ALL_BLACK,
         "entity/cat/cat_all_black",
         "entity/cat/cat_all_black_baby",
         new SpawnPrioritySelectors(
            List.of(
               new PriorityProvider.Selector<>(new StructureCheck(structures.getOrThrow(StructureTags.CATS_SPAWN_AS_BLACK)), 1),
               new PriorityProvider.Selector<>(new MoonBrightnessCheck(MinMaxBounds.Doubles.atLeast(0.9)), 0)
            )
         )
      );
   }

   private static void registerForAnyConditions(
      final BootstrapContext<CatVariant> context, final ResourceKey<CatVariant> name, final String adultTexture, final String babyTexture
   ) {
      register(context, name, adultTexture, babyTexture, SpawnPrioritySelectors.fallback(0));
   }

   private static void register(
      final BootstrapContext<CatVariant> context,
      final ResourceKey<CatVariant> name,
      final String adultTexture,
      final String babyTexture,
      final SpawnPrioritySelectors spawnConditions
   ) {
      context.register(
         name,
         new CatVariant(
            new ClientAsset.ResourceTexture(Identifier.withDefaultNamespace(adultTexture)),
            new ClientAsset.ResourceTexture(Identifier.withDefaultNamespace(babyTexture)),
            spawnConditions
         )
      );
   }
}
