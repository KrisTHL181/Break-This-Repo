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

   private static ResourceKey<ZombieNautilusVariant> createKey(final Identifier id) {
      return ResourceKey.create(Registries.ZOMBIE_NAUTILUS_VARIANT, id);
   }

   public static void bootstrap(final BootstrapContext<ZombieNautilusVariant> context) {
      register(context, TEMPERATE, ZombieNautilusVariant.ModelType.NORMAL, "zombie_nautilus", SpawnPrioritySelectors.fallback(0));
      register(context, WARM, ZombieNautilusVariant.ModelType.WARM, "zombie_nautilus_coral", BiomeTags.SPAWNS_CORAL_VARIANT_ZOMBIE_NAUTILUS);
   }

   private static void register(
      final BootstrapContext<ZombieNautilusVariant> context,
      final ResourceKey<ZombieNautilusVariant> name,
      final ZombieNautilusVariant.ModelType modelType,
      final String textureName,
      final TagKey<Biome> spawnBiome
   ) {
      HolderSet<Biome> biomes = context.lookup(Registries.BIOME).getOrThrow(spawnBiome);
      register(context, name, modelType, textureName, SpawnPrioritySelectors.single(new BiomeCheck(biomes), 1));
   }

   private static void register(
      final BootstrapContext<ZombieNautilusVariant> context,
      final ResourceKey<ZombieNautilusVariant> name,
      final ZombieNautilusVariant.ModelType modelType,
      final String textureName,
      final SpawnPrioritySelectors selectors
   ) {
      Identifier textureId = Identifier.withDefaultNamespace("entity/nautilus/" + textureName);
      context.register(name, new ZombieNautilusVariant(new ModelAndTexture<>(modelType, textureId), selectors));
   }
}
