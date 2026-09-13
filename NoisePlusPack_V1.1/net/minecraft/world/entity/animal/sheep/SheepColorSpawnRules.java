package net.minecraft.world.entity.animal.sheep;

import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.biome.Biome;

public class SheepColorSpawnRules {
   private static final SheepColorSpawnRules.SheepColorSpawnConfiguration TEMPERATE_SPAWN_CONFIGURATION = new SheepColorSpawnRules.SheepColorSpawnConfiguration(
      weighted(
         builder()
            .add(single(DyeColor.BLACK), 5)
            .add(single(DyeColor.GRAY), 5)
            .add(single(DyeColor.LIGHT_GRAY), 5)
            .add(single(DyeColor.BROWN), 3)
            .add(commonColors(DyeColor.WHITE), 82)
            .build()
      )
   );
   private static final SheepColorSpawnRules.SheepColorSpawnConfiguration WARM_SPAWN_CONFIGURATION = new SheepColorSpawnRules.SheepColorSpawnConfiguration(
      weighted(
         builder()
            .add(single(DyeColor.GRAY), 5)
            .add(single(DyeColor.LIGHT_GRAY), 5)
            .add(single(DyeColor.WHITE), 5)
            .add(single(DyeColor.BLACK), 3)
            .add(commonColors(DyeColor.BROWN), 82)
            .build()
      )
   );
   private static final SheepColorSpawnRules.SheepColorSpawnConfiguration COLD_SPAWN_CONFIGURATION = new SheepColorSpawnRules.SheepColorSpawnConfiguration(
      weighted(
         builder()
            .add(single(DyeColor.LIGHT_GRAY), 5)
            .add(single(DyeColor.GRAY), 5)
            .add(single(DyeColor.WHITE), 5)
            .add(single(DyeColor.BROWN), 3)
            .add(commonColors(DyeColor.BLACK), 82)
            .build()
      )
   );

   private static SheepColorSpawnRules.SheepColorProvider commonColors(DyeColor p_392935_) {
      return weighted(builder().add(single(p_392935_), 499).add(single(DyeColor.PINK), 1).build());
   }

   public static DyeColor getSheepColor(Holder<Biome> p_397560_, RandomSource p_397669_) {
      SheepColorSpawnRules.SheepColorSpawnConfiguration sheepcolorspawnrules$sheepcolorspawnconfiguration = getSheepColorConfiguration(p_397560_);
      return sheepcolorspawnrules$sheepcolorspawnconfiguration.colors().get(p_397669_);
   }

   private static SheepColorSpawnRules.SheepColorSpawnConfiguration getSheepColorConfiguration(Holder<Biome> p_397141_) {
      if (p_397141_.is(BiomeTags.SPAWNS_WARM_VARIANT_FARM_ANIMALS)) {
         return WARM_SPAWN_CONFIGURATION;
      } else {
         return p_397141_.is(BiomeTags.SPAWNS_COLD_VARIANT_FARM_ANIMALS) ? COLD_SPAWN_CONFIGURATION : TEMPERATE_SPAWN_CONFIGURATION;
      }
   }

   private static SheepColorSpawnRules.SheepColorProvider weighted(WeightedList<SheepColorSpawnRules.SheepColorProvider> p_391917_) {
      if (p_391917_.isEmpty()) {
         throw new IllegalArgumentException("List must be non-empty");
      } else {
         return p_396158_ -> p_391917_.getRandomOrThrow(p_396158_).get(p_396158_);
      }
   }

   private static SheepColorSpawnRules.SheepColorProvider single(DyeColor p_397733_) {
      return p_394954_ -> p_397733_;
   }

   private static WeightedList.Builder<SheepColorSpawnRules.SheepColorProvider> builder() {
      return WeightedList.builder();
   }

   @FunctionalInterface
   interface SheepColorProvider {
      DyeColor get(RandomSource var1);
   }

   record SheepColorSpawnConfiguration(SheepColorSpawnRules.SheepColorProvider colors) {
   }
}
