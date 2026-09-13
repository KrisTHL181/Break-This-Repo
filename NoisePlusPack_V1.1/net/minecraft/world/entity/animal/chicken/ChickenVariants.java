package net.minecraft.world.entity.animal.chicken;

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

public class ChickenVariants {
   public static final ResourceKey<ChickenVariant> TEMPERATE = createKey(TemperatureVariants.TEMPERATE);
   public static final ResourceKey<ChickenVariant> WARM = createKey(TemperatureVariants.WARM);
   public static final ResourceKey<ChickenVariant> COLD = createKey(TemperatureVariants.COLD);
   public static final ResourceKey<ChickenVariant> DEFAULT = TEMPERATE;

   private static ResourceKey<ChickenVariant> createKey(Identifier p_451653_) {
      return ResourceKey.create(Registries.CHICKEN_VARIANT, p_451653_);
   }

   public static void bootstrap(BootstrapContext<ChickenVariant> p_458649_) {
      register(p_458649_, TEMPERATE, ChickenVariant.ModelType.NORMAL, "temperate_chicken", SpawnPrioritySelectors.fallback(0));
      register(p_458649_, WARM, ChickenVariant.ModelType.NORMAL, "warm_chicken", BiomeTags.SPAWNS_WARM_VARIANT_FARM_ANIMALS);
      register(p_458649_, COLD, ChickenVariant.ModelType.COLD, "cold_chicken", BiomeTags.SPAWNS_COLD_VARIANT_FARM_ANIMALS);
   }

   private static void register(
      BootstrapContext<ChickenVariant> p_453254_,
      ResourceKey<ChickenVariant> p_452107_,
      ChickenVariant.ModelType p_457973_,
      String p_457893_,
      TagKey<Biome> p_452455_
   ) {
      HolderSet<Biome> holderset = p_453254_.lookup(Registries.BIOME).getOrThrow(p_452455_);
      register(p_453254_, p_452107_, p_457973_, p_457893_, SpawnPrioritySelectors.single(new BiomeCheck(holderset), 1));
   }

   private static void register(
      BootstrapContext<ChickenVariant> p_459186_,
      ResourceKey<ChickenVariant> p_458899_,
      ChickenVariant.ModelType p_454899_,
      String p_454140_,
      SpawnPrioritySelectors p_460415_
   ) {
      Identifier identifier = Identifier.withDefaultNamespace("entity/chicken/" + p_454140_);
      p_459186_.register(p_458899_, new ChickenVariant(new ModelAndTexture<>(p_454899_, identifier), p_460415_));
   }
}
