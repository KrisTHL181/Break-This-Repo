package net.minecraft.data.worldgen;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.placement.TreePlacements;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class AbandonedCampStructurePools {
   public static final AbandonedCampStructurePools.AbandonedCampStructure BAMBOO_JUNGLE = new AbandonedCampStructurePools.AbandonedCampStructure(
      Pools.createKey("abandoned_camp/camp/bamboo_jungle"), Pools.createKey("abandoned_camp/tent/bamboo_jungle"), "bamboo_jungle"
   );
   public static final AbandonedCampStructurePools.AbandonedCampStructure BIRCH_FOREST = new AbandonedCampStructurePools.AbandonedCampStructure(
      Pools.createKey("abandoned_camp/camp/birch_forest"), Pools.createKey("abandoned_camp/tent/birch_forest"), "birch_forest"
   );
   public static final AbandonedCampStructurePools.AbandonedCampStructure CHERRY_GROVE = new AbandonedCampStructurePools.AbandonedCampStructure(
      Pools.createKey("abandoned_camp/camp/cherry_grove"), Pools.createKey("abandoned_camp/tent/cherry_grove"), "cherry_grove"
   );
   public static final AbandonedCampStructurePools.AbandonedCampStructure DAPPLED_FOREST = new AbandonedCampStructurePools.AbandonedCampStructure(
      Pools.createKey("abandoned_camp/camp/dappled_forest"), Pools.createKey("abandoned_camp/tent/dappled_forest"), "dappled_forest"
   );
   public static final AbandonedCampStructurePools.AbandonedCampStructure FLOWER_FOREST = new AbandonedCampStructurePools.AbandonedCampStructure(
      Pools.createKey("abandoned_camp/camp/flower_forest"), Pools.createKey("abandoned_camp/tent/flower_forest"), "flower_forest"
   );
   public static final AbandonedCampStructurePools.AbandonedCampStructure FOREST = new AbandonedCampStructurePools.AbandonedCampStructure(
      Pools.createKey("abandoned_camp/camp/forest"), Pools.createKey("abandoned_camp/tent/forest"), "forest"
   );
   public static final AbandonedCampStructurePools.AbandonedCampStructure MEADOW = new AbandonedCampStructurePools.AbandonedCampStructure(
      Pools.createKey("abandoned_camp/camp/meadow"), Pools.createKey("abandoned_camp/tent/meadow"), "meadow"
   );
   public static final AbandonedCampStructurePools.AbandonedCampStructure OLD_GROWTH_BIRCH_FOREST = new AbandonedCampStructurePools.AbandonedCampStructure(
      Pools.createKey("abandoned_camp/camp/old_growth_birch_forest"), Pools.createKey("abandoned_camp/tent/old_growth_birch_forest"), "old_growth_birch_forest"
   );
   public static final AbandonedCampStructurePools.AbandonedCampStructure OLD_GROWTH_PINE_TAIGA = new AbandonedCampStructurePools.AbandonedCampStructure(
      Pools.createKey("abandoned_camp/camp/old_growth_pine_taiga"), Pools.createKey("abandoned_camp/tent/old_growth_pine_taiga"), "old_growth_pine_taiga"
   );
   public static final AbandonedCampStructurePools.AbandonedCampStructure OLD_GROWTH_SPRUCE_TAIGA = new AbandonedCampStructurePools.AbandonedCampStructure(
      Pools.createKey("abandoned_camp/camp/old_growth_spruce_taiga"), Pools.createKey("abandoned_camp/tent/old_growth_spruce_taiga"), "old_growth_spruce_taiga"
   );
   public static final AbandonedCampStructurePools.AbandonedCampStructure PALE_GARDEN = new AbandonedCampStructurePools.AbandonedCampStructure(
      Pools.createKey("abandoned_camp/camp/pale_garden"), Pools.createKey("abandoned_camp/tent/pale_garden"), "pale_garden"
   );
   public static final AbandonedCampStructurePools.AbandonedCampStructure SAVANNA = new AbandonedCampStructurePools.AbandonedCampStructure(
      Pools.createKey("abandoned_camp/camp/savanna"), Pools.createKey("abandoned_camp/tent/savanna"), "savanna"
   );
   public static final AbandonedCampStructurePools.AbandonedCampStructure SNOWY_TAIGA = new AbandonedCampStructurePools.AbandonedCampStructure(
      Pools.createKey("abandoned_camp/camp/snowy_taiga"), Pools.createKey("abandoned_camp/tent/snowy_taiga"), "snowy_taiga"
   );
   public static final AbandonedCampStructurePools.AbandonedCampStructure SPARSE_JUNGLE = new AbandonedCampStructurePools.AbandonedCampStructure(
      Pools.createKey("abandoned_camp/camp/sparse_jungle"), Pools.createKey("abandoned_camp/tent/sparse_jungle"), "sparse_jungle"
   );
   public static final AbandonedCampStructurePools.AbandonedCampStructure SWAMP = new AbandonedCampStructurePools.AbandonedCampStructure(
      Pools.createKey("abandoned_camp/camp/swamp"), Pools.createKey("abandoned_camp/tent/swamp"), "swamp"
   );
   public static final AbandonedCampStructurePools.AbandonedCampStructure TAIGA = new AbandonedCampStructurePools.AbandonedCampStructure(
      Pools.createKey("abandoned_camp/camp/taiga"), Pools.createKey("abandoned_camp/tent/taiga"), "taiga"
   );
   public static final AbandonedCampStructurePools.AbandonedCampStructure WINDSWEPT_FOREST = new AbandonedCampStructurePools.AbandonedCampStructure(
      Pools.createKey("abandoned_camp/camp/windswept_forest"), Pools.createKey("abandoned_camp/tent/windswept_forest"), "windswept_forest"
   );
   public static final AbandonedCampStructurePools.AbandonedCampStructure WOODED_BADLANDS = new AbandonedCampStructurePools.AbandonedCampStructure(
      Pools.createKey("abandoned_camp/camp/wooded_badlands"), Pools.createKey("abandoned_camp/tent/wooded_badlands"), "wooded_badlands"
   );
   private static final String TENT_IDENTIFIER_TEMPLATE = "abandoned_camp/tent/%s/tent_%s_%d";
   private static final String CAMP_BIOME_IDENTIFIER_TEMPLATE = "abandoned_camp/camp/%s/campsite_%s_%d";
   private static final String CAMP_DEFAULT_IDENTIFIER_TEMPLATE = "abandoned_camp/camp/default/campsite_default_%s_%d";
   private static final int NUM_OF_BIOME_SPECIFIC_CAMPSITES = 3;
   private static final int NUM_OF_DEFAULT_CAMPSITES = 15;
   private static final int NUM_OF_TENTS = 10;
   private static final List<AbandonedCampStructurePools.AbandonedCampStructure> BIOME_VARIANTS = List.of(
      SAVANNA,
      FLOWER_FOREST,
      BIRCH_FOREST,
      FOREST,
      SNOWY_TAIGA,
      BAMBOO_JUNGLE,
      SPARSE_JUNGLE,
      CHERRY_GROVE,
      MEADOW,
      OLD_GROWTH_BIRCH_FOREST,
      OLD_GROWTH_SPRUCE_TAIGA,
      OLD_GROWTH_PINE_TAIGA,
      SWAMP,
      TAIGA,
      WINDSWEPT_FOREST,
      DAPPLED_FOREST,
      WOODED_BADLANDS,
      PALE_GARDEN
   );
   private static final List<String> DEFAULT_CAMP_TYPES = List.of("chest", "barrel", "special");

   public static void bootstrap(final BootstrapContext<StructureTemplatePool> context) {
      HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
      Holder<StructureTemplatePool> empty = pools.getOrThrow(Pools.EMPTY);
      bootstrapTrees(context, empty);
      bootstrapCampsitePools(context, empty);
   }

   private static void bootstrapTrees(final BootstrapContext<StructureTemplatePool> context, final Holder<StructureTemplatePool> empty) {
      registerTrees(
         context,
         empty,
         ImmutableList.of(
            Pair.of("acacia", TreePlacements.ACACIA_CHECKED),
            Pair.of("birch", TreePlacements.BIRCH_CHECKED),
            Pair.of("fancy_oak", TreePlacements.FANCY_OAK_CHECKED),
            Pair.of("oak", TreePlacements.OAK_CHECKED),
            Pair.of("spruce", TreePlacements.SPRUCE_CHECKED),
            Pair.of("thick_spruce", TreePlacements.MEGA_SPRUCE_CHECKED),
            Pair.of("yellow_poplar", TreePlacements.YELLOW_POPLAR),
            Pair.of("orange_poplar", TreePlacements.ORANGE_POPLAR),
            Pair.of("red_poplar", TreePlacements.RED_POPLAR),
            Pair.of("super_birch_bees", TreePlacements.SUPER_BIRCH_BEES_0002),
            Pair.of("spruce_on_snow", TreePlacements.SPRUCE_ON_SNOW),
            Pair.of("fancy_oak_bees", TreePlacements.FANCY_OAK_BEES_002),
            new Pair[]{
               Pair.of("birch_bees", TreePlacements.BIRCH_BEES_002),
               Pair.of("pale_oak", TreePlacements.PALE_OAK_CHECKED),
               Pair.of("bamboo", VegetationPlacements.BAMBOO_IN_STRUCTURE),
               Pair.of("jungle", TreePlacements.JUNGLE_TREE_CHECKED),
               Pair.of("pine", TreePlacements.PINE_CHECKED),
               Pair.of("mega_pine", TreePlacements.MEGA_PINE_CHECKED),
               Pair.of("mega_jungle", TreePlacements.MEGA_JUNGLE_TREE_CHECKED),
               Pair.of("cherry", TreePlacements.CHERRY_CHECKED),
               Pair.of("cherry_bees", TreePlacements.CHERRY_BEES_005)
            }
         )
      );
   }

   private static void registerTrees(
      final BootstrapContext<StructureTemplatePool> context,
      final Holder<StructureTemplatePool> empty,
      final Collection<Pair<String, ResourceKey<PlacedFeature>>> list
   ) {
      HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);

      for (Pair<String, ResourceKey<PlacedFeature>> pair : list) {
         Pools.register(
            context,
            "abandoned_camp/trees/" + (String)pair.getFirst(),
            new StructureTemplatePool(
               empty,
               ImmutableList.of(Pair.of(StructurePoolElement.feature(placedFeatures.getOrThrow((ResourceKey<PlacedFeature>)pair.getSecond())), 1)),
               StructureTemplatePool.Projection.RIGID
            )
         );
      }
   }

   private static void bootstrapCampsitePools(final BootstrapContext<StructureTemplatePool> context, final Holder<StructureTemplatePool> empty) {
      for (AbandonedCampStructurePools.AbandonedCampStructure biomeVariant : BIOME_VARIANTS) {
         registerTentPool(context, empty, biomeVariant);
         registerCampsitePool(context, empty, biomeVariant);
      }
   }

   private static void registerTentPool(
      final BootstrapContext<StructureTemplatePool> context,
      final Holder<StructureTemplatePool> empty,
      final AbandonedCampStructurePools.AbandonedCampStructure biomeVariant
   ) {
      List<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>> tentStructures = new ArrayList<>();

      for (int numOfTentVariants = 1; numOfTentVariants <= 10; numOfTentVariants++) {
         tentStructures.add(
            Pair.of(StructurePoolElement.legacy(getBiomeSpecificStructureName(biomeVariant, "abandoned_camp/tent/%s/tent_%s_%d", numOfTentVariants)), 1)
         );
      }

      Pools.register(
         context,
         biomeVariant.tentStructureDirectory.identifier().toShortString(),
         new StructureTemplatePool(empty, tentStructures, StructureTemplatePool.Projection.RIGID)
      );
   }

   private static void registerCampsitePool(
      final BootstrapContext<StructureTemplatePool> context,
      final Holder<StructureTemplatePool> empty,
      final AbandonedCampStructurePools.AbandonedCampStructure biomeVariant
   ) {
      List<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>> campsiteStructures = new ArrayList<>();

      for (String defaultCampSiteTypes : DEFAULT_CAMP_TYPES) {
         for (int numOfDefaultCampVariants = 1; numOfDefaultCampVariants <= 15; numOfDefaultCampVariants++) {
            campsiteStructures.add(Pair.of(StructurePoolElement.legacy(getDefaultCampsiteStructureName(defaultCampSiteTypes, numOfDefaultCampVariants)), 1));
         }
      }

      for (int numOfBiomeCampVariants = 1; numOfBiomeCampVariants <= 3; numOfBiomeCampVariants++) {
         campsiteStructures.add(
            Pair.of(
               StructurePoolElement.legacy(getBiomeSpecificStructureName(biomeVariant, "abandoned_camp/camp/%s/campsite_%s_%d", numOfBiomeCampVariants)), 1
            )
         );
      }

      Pools.register(
         context,
         biomeVariant.campStructureDirectory.identifier().toShortString(),
         new StructureTemplatePool(empty, campsiteStructures, StructureTemplatePool.Projection.RIGID)
      );
   }

   private static String getBiomeSpecificStructureName(
      final AbandonedCampStructurePools.AbandonedCampStructure biomeVariant, final String identifierTemplate, final int numOfTentVariants
   ) {
      return String.format(Locale.ROOT, identifierTemplate, biomeVariant.name, biomeVariant.name, numOfTentVariants);
   }

   private static String getDefaultCampsiteStructureName(final String defaultCampSiteTypes, final int numOfDefaultCampVariants) {
      return String.format(Locale.ROOT, "abandoned_camp/camp/default/campsite_default_%s_%d", defaultCampSiteTypes, numOfDefaultCampVariants);
   }

   public record AbandonedCampStructure(
      ResourceKey<StructureTemplatePool> campStructureDirectory, ResourceKey<StructureTemplatePool> tentStructureDirectory, String name
   ) {
   }
}
