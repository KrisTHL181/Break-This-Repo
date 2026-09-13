package net.minecraft.world.entity.animal.pig;

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

   private static ResourceKey<PigVariant> createKey(Identifier p_452224_) {
      return ResourceKey.create(Registries.PIG_VARIANT, p_452224_);
   }

   public static void bootstrap(BootstrapContext<PigVariant> p_453053_) {
      register(p_453053_, TEMPERATE, PigVariant.ModelType.NORMAL, "temperate_pig", SpawnPrioritySelectors.fallback(0));
      register(p_453053_, WARM, PigVariant.ModelType.NORMAL, "warm_pig", BiomeTags.SPAWNS_WARM_VARIANT_FARM_ANIMALS);
      register(p_453053_, COLD, PigVariant.ModelType.COLD, "cold_pig", BiomeTags.SPAWNS_COLD_VARIANT_FARM_ANIMALS);
   }

   private static void register(
      BootstrapContext<PigVariant> p_458733_, ResourceKey<PigVariant> p_456729_, PigVariant.ModelType p_458915_, String p_459530_, TagKey<Biome> p_453568_
   ) {
      HolderSet<Biome> holderset = p_458733_.lookup(Registries.BIOME).getOrThrow(p_453568_);
      register(p_458733_, p_456729_, p_458915_, p_459530_, SpawnPrioritySelectors.single(new BiomeCheck(holderset), 1));
   }

   private static void register(
      BootstrapContext<PigVariant> p_451140_,
      ResourceKey<PigVariant> p_450943_,
      PigVariant.ModelType p_459209_,
      String p_458396_,
      SpawnPrioritySelectors p_457995_
   ) {
      Identifier identifier = Identifier.withDefaultNamespace("entity/pig/" + p_458396_);
      p_451140_.register(p_450943_, new PigVariant(new ModelAndTexture<>(p_459209_, identifier), p_457995_));
   }
}
