package net.minecraft.data.worldgen.features;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.BlockPileFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RotatedBlockProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;

public class PileFeatures {
   public static final ResourceKey<Feature> PILE_HAY = FeatureUtils.createKey("pile_hay");
   public static final ResourceKey<Feature> PILE_MELON = FeatureUtils.createKey("pile_melon");
   public static final ResourceKey<Feature> PILE_SNOW = FeatureUtils.createKey("pile_snow");
   public static final ResourceKey<Feature> PILE_ICE = FeatureUtils.createKey("pile_ice");
   public static final ResourceKey<Feature> PILE_PUMPKIN = FeatureUtils.createKey("pile_pumpkin");

   public static void bootstrap(final BootstrapContext<Feature> context) {
      context.register(PILE_HAY, new BlockPileFeature(new RotatedBlockProvider(BlockStateProvider.simple(Blocks.HAY_BLOCK))));
      context.register(PILE_MELON, new BlockPileFeature(BlockStateProvider.simple(Blocks.MELON)));
      context.register(PILE_SNOW, new BlockPileFeature(BlockStateProvider.simple(Blocks.SNOW)));
      context.register(
         PILE_ICE,
         new BlockPileFeature(
            new WeightedStateProvider(
               WeightedList.<BlockState>builder().add(Blocks.BLUE_ICE.defaultBlockState(), 1).add(Blocks.PACKED_ICE.defaultBlockState(), 5)
            )
         )
      );
      context.register(
         PILE_PUMPKIN,
         new BlockPileFeature(
            new WeightedStateProvider(
               WeightedList.<BlockState>builder().add(Blocks.PUMPKIN.defaultBlockState(), 19).add(Blocks.JACK_O_LANTERN.defaultBlockState(), 1)
            )
         )
      );
   }
}
