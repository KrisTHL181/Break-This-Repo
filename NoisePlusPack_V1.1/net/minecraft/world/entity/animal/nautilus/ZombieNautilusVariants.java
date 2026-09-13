package net.minecraft.world.entity.animal.nautilus;

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

public class ZombieNautilusVariants {
   public static final ResourceKey<ZombieNautilusVariant> TEMPERATE = createKey(TemperatureVariants.TEMPERATE);
   public static final ResourceKey<ZombieNautilusVariant> WARM = createKey(TemperatureVariants.WARM);
   public static final ResourceKey<ZombieNautilusVariant> DEFAULT = TEMPERATE;

   private static ResourceKey<ZombieNautilusVariant> createKey(Identifier p_454687_) {
      return ResourceKey.create(Registries.ZOMBIE_NAUTILUS_VARIANT, p_454687_);
   }

   public static void bootstrap(BootstrapContext<ZombieNautilusVariant> p_458683_) {
      register(p_458683_, TEMPERATE, ZombieNautilusVariant.ModelType.NORMAL, "zombie_nautilus", SpawnPrioritySelectors.fallback(0));
      register(p_458683_, WARM, ZombieNautilusVariant.ModelType.WARM, "zombie_nautilus_coral", BiomeTags.SPAWNS_CORAL_VARIANT_ZOMBIE_NAUTILUS);
   }

   private static void register(
      BootstrapContext<ZombieNautilusVariant> p_452335_,
      ResourceKey<ZombieNautilusVariant> p_450375_,
      ZombieNautilusVariant.ModelType p_453489_,
      String p_453615_,
      TagKey<Biome> p_459380_
   ) {
      HolderSet<Biome> holderset = p_452335_.lookup(Registries.BIOME).getOrThrow(p_459380_);
      register(p_452335_, p_450375_, p_453489_, p_453615_, SpawnPrioritySelectors.single(new BiomeCheck(holderset), 1));
   }

   private static void register(
      BootstrapContext<ZombieNautilusVariant> p_450835_,
      ResourceKey<ZombieNautilusVariant> p_451268_,
      ZombieNautilusVariant.ModelType p_450275_,
      String p_459332_,
      SpawnPrioritySelectors p_451308_
   ) {
      Identifier identifier = Identifier.withDefaultNamespace("entity/nautilus/" + p_459332_);
      p_450835_.register(p_451268_, new ZombieNautilusVariant(new ModelAndTexture<>(p_450275_, identifier), p_451308_));
   }
}
