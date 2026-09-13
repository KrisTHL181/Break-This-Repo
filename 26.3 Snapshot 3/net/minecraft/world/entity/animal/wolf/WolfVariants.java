package net.minecraft.world.entity.animal.wolf;

import net.minecraft.core.ClientAsset;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.variant.BiomeCheck;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public class WolfVariants {
   public static final ResourceKey<WolfVariant> PALE = createKey("pale");
   public static final ResourceKey<WolfVariant> SPOTTED = createKey("spotted");
   public static final ResourceKey<WolfVariant> SNOWY = createKey("snowy");
   public static final ResourceKey<WolfVariant> BLACK = createKey("black");
   public static final ResourceKey<WolfVariant> ASHEN = createKey("ashen");
   public static final ResourceKey<WolfVariant> RUSTY = createKey("rusty");
   public static final ResourceKey<WolfVariant> WOODS = createKey("woods");
   public static final ResourceKey<WolfVariant> CHESTNUT = createKey("chestnut");
   public static final ResourceKey<WolfVariant> STRIPED = createKey("striped");
   public static final ResourceKey<WolfVariant> DEFAULT = PALE;

   private static ResourceKey<WolfVariant> createKey(final String name) {
      return ResourceKey.create(Registries.WOLF_VARIANT, Identifier.withDefaultNamespace(name));
   }

   private static void register(
      final BootstrapContext<WolfVariant> context, final ResourceKey<WolfVariant> name, final String fileName, final ResourceKey<Biome> spawnBiome
   ) {
      register(context, name, fileName, highPrioBiome(HolderSet.direct(context.lookup(Registries.BIOME).getOrThrow(spawnBiome))));
   }

   private static void register(
      final BootstrapContext<WolfVariant> context, final ResourceKey<WolfVariant> name, final String fileName, final TagKey<Biome> spawnBiome
   ) {
      register(context, name, fileName, highPrioBiome(context.lookup(Registries.BIOME).getOrThrow(spawnBiome)));
   }

   private static SpawnPrioritySelectors highPrioBiome(final HolderSet<Biome> biomes) {
      return SpawnPrioritySelectors.single(new BiomeCheck(biomes), 1);
   }

   private static void register(
      final BootstrapContext<WolfVariant> context, final ResourceKey<WolfVariant> name, final String fileName, final SpawnPrioritySelectors selectors
   ) {
      Identifier wildTexture = Identifier.withDefaultNamespace("entity/wolf/" + fileName);
      Identifier tameTexture = Identifier.withDefaultNamespace("entity/wolf/" + fileName + "_tame");
      Identifier angryTexture = Identifier.withDefaultNamespace("entity/wolf/" + fileName + "_angry");
      Identifier babyTexture = Identifier.withDefaultNamespace("entity/wolf/" + fileName + "_baby");
      Identifier tameBabyTexture = Identifier.withDefaultNamespace("entity/wolf/" + fileName + "_tame_baby");
      Identifier angryBabyTexture = Identifier.withDefaultNamespace("entity/wolf/" + fileName + "_angry_baby");
      context.register(
         name,
         new WolfVariant(
            new WolfVariant.AssetInfo(
               new ClientAsset.ResourceTexture(wildTexture), new ClientAsset.ResourceTexture(tameTexture), new ClientAsset.ResourceTexture(angryTexture)
            ),
            new WolfVariant.AssetInfo(
               new ClientAsset.ResourceTexture(babyTexture),
               new ClientAsset.ResourceTexture(tameBabyTexture),
               new ClientAsset.ResourceTexture(angryBabyTexture)
            ),
            selectors
         )
      );
   }

   public static void bootstrap(final BootstrapContext<WolfVariant> context) {
      register(context, PALE, "wolf", SpawnPrioritySelectors.fallback(0));
      register(context, SPOTTED, "wolf_spotted", BiomeTags.IS_SAVANNA);
      register(context, SNOWY, "wolf_snowy", Biomes.GROVE);
      register(context, BLACK, "wolf_black", Biomes.OLD_GROWTH_PINE_TAIGA);
      register(context, ASHEN, "wolf_ashen", Biomes.SNOWY_TAIGA);
      register(context, RUSTY, "wolf_rusty", BiomeTags.IS_JUNGLE);
      register(context, WOODS, "wolf_woods", Biomes.FOREST);
      register(context, CHESTNUT, "wolf_chestnut", Biomes.OLD_GROWTH_SPRUCE_TAIGA);
      register(context, STRIPED, "wolf_striped", BiomeTags.IS_BADLANDS);
   }
}
