package net.minecraft.world.entity.animal.cow;

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

public class CowVariants {
   public static final ResourceKey<CowVariant> TEMPERATE = createKey(TemperatureVariants.TEMPERATE);
   public static final ResourceKey<CowVariant> WARM = createKey(TemperatureVariants.WARM);
   public static final ResourceKey<CowVariant> COLD = createKey(TemperatureVariants.COLD);
   public static final ResourceKey<CowVariant> DEFAULT = TEMPERATE;

   private static ResourceKey<CowVariant> createKey(Identifier p_454463_) {
      return ResourceKey.create(Registries.COW_VARIANT, p_454463_);
   }

   public static void bootstrap(BootstrapContext<CowVariant> p_454398_) {
      register(p_454398_, TEMPERATE, CowVariant.ModelType.NORMAL, "temperate_cow", SpawnPrioritySelectors.fallback(0));
      register(p_454398_, WARM, CowVariant.ModelType.WARM, "warm_cow", BiomeTags.SPAWNS_WARM_VARIANT_FARM_ANIMALS);
      register(p_454398_, COLD, CowVariant.ModelType.COLD, "cold_cow", BiomeTags.SPAWNS_COLD_VARIANT_FARM_ANIMALS);
   }

   private static void register(
      BootstrapContext<CowVariant> p_459725_, ResourceKey<CowVariant> p_455507_, CowVariant.ModelType p_460893_, String p_451583_, TagKey<Biome> p_458461_
   ) {
      HolderSet<Biome> holderset = p_459725_.lookup(Registries.BIOME).getOrThrow(p_458461_);
      register(p_459725_, p_455507_, p_460893_, p_451583_, SpawnPrioritySelectors.single(new BiomeCheck(holderset), 1));
   }

   private static void register(
      BootstrapContext<CowVariant> p_454568_,
      ResourceKey<CowVariant> p_459488_,
      CowVariant.ModelType p_455755_,
      String p_459305_,
      SpawnPrioritySelectors p_453632_
   ) {
      Identifier identifier = Identifier.withDefaultNamespace("entity/cow/" + p_459305_);
      p_454568_.register(p_459488_, new CowVariant(new ModelAndTexture<>(p_455755_, identifier), p_453632_));
   }
}
