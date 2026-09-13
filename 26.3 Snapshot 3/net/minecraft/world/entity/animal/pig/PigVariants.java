package net.minecraft.world.entity.animal.pig;

import net.minecraft.core.ClientAsset;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.animal.TemperatureVariants;
import net.minecraft.world.entity.variant.BiomeCheck;
import net.minecraft.world.entity.variant.ModelAndTexture;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;
import net.minecraft.world.level.biome.Biome;

public class PigVariants {
   public static final ResourceKey<PigVariant> TEMPERATE = createKey(TemperatureVariants.TEMPERATE);
   public static final ResourceKey<PigVariant> WARM = createKey(TemperatureVariants.WARM);
   public static final ResourceKey<PigVariant> COLD = createKey(TemperatureVariants.COLD);
   public static final ResourceKey<PigVariant> DEFAULT = TEMPERATE;

   private static ResourceKey<PigVariant> createKey(final Identifier id) {
      return ResourceKey.create(Registries.PIG_VARIANT, id);
   }

   public static void bootstrap(final BootstrapContext<PigVariant> context) {
      register(context, TEMPERATE, PigVariant.ModelType.NORMAL, "pig_temperate", "pig_temperate_baby", SpawnPrioritySelectors.fallback(0));
      register(context, WARM, PigVariant.ModelType.NORMAL, "pig_warm", "pig_warm_baby", BiomeTags.SPAWNS_WARM_VARIANT_FARM_ANIMALS);
      register(context, COLD, PigVariant.ModelType.COLD, "pig_cold", "pig_cold_baby", BiomeTags.SPAWNS_COLD_VARIANT_FARM_ANIMALS);
   }

   private static void register(
      final BootstrapContext<PigVariant> context,
      final ResourceKey<PigVariant> name,
      final PigVariant.ModelType modelType,
      final String textureName,
      final String babyTextureName,
      final TagKey<Biome> spawnBiome
   ) {
      HolderSet<Biome> biomes = context.lookup(Registries.BIOME).getOrThrow(spawnBiome);
      register(context, name, modelType, textureName, babyTextureName, SpawnPrioritySelectors.single(new BiomeCheck(biomes), 1));
   }

   private static void register(
      final BootstrapContext<PigVariant> context,
      final ResourceKey<PigVariant> name,
      final PigVariant.ModelType modelType,
      final String textureName,
      final String babyTextureName,
      final SpawnPrioritySelectors selectors
   ) {
      Identifier textureId = Identifier.withDefaultNamespace("entity/pig/" + textureName);
      Identifier babyTextureId = Identifier.withDefaultNamespace("entity/pig/" + babyTextureName);
      context.register(name, new PigVariant(new ModelAndTexture<>(modelType, textureId), new ClientAsset.ResourceTexture(babyTextureId), selectors));
   }
}
