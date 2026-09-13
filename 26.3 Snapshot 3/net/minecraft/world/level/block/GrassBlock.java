package net.minecraft.world.level.block;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.references.BlockItemIds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class GrassBlock extends SpreadingSnowyBlock implements BonemealableBlock {
   private static final int ATTEMPT_COUNT = 128;
   private static final float GROW_TALL_GRASS_CHANCE = 0.1F;
   private static final float PLACE_FLOWER_CHANCE = 0.125F;

   public GrassBlock(final BlockBehaviour.Properties properties) {
      super(properties, BlockItemIds.DIRT.block());
   }

   @Override
   public boolean isValidBonemealTarget(final LevelReader level, final BlockPos pos, final BlockState state) {
      return level.getBlockState(pos.above()).isAir() && level.isInsideBuildHeight(pos.above());
   }

   @Override
   public boolean isBonemealSuccess(final Level level, final RandomSource random, final BlockPos pos, final BlockState state) {
      return true;
   }

   @Override
   public void performBonemeal(final ServerLevel level, final RandomSource random, final BlockPos pos, final BlockState state) {
      BlockPos above = pos.above();

      label24:
      for (int attempt = 0; attempt < 128; attempt++) {
         BlockPos testPos = above;
         int randomizeCount = attempt / 16;

         for (int i = 0; i < randomizeCount; i++) {
            int dx = random.nextIntBetweenInclusive(-1, 1);
            int dy = random.nextIntBetweenInclusive(-1, 1) * random.nextInt(3) / 2;
            int dz = random.nextIntBetweenInclusive(-1, 1);
            testPos = testPos.offset(dx, dy, dz);
            if (this.stopBonemealSpread(level, testPos)) {
               continue label24;
            }
         }

         placeBonemealEffect(level, random, testPos);
      }
   }

   private static void placeBonemealEffect(final ServerLevel level, final RandomSource random, final BlockPos testPos) {
      BlockState grass = Blocks.SHORT_GRASS.defaultBlockState();
      Optional<Holder.Reference<PlacedFeature>> grassFeature = level.registryAccess()
         .lookupOrThrow(Registries.PLACED_FEATURE)
         .get(VegetationPlacements.GRASS_BONEMEAL);
      BlockState testState = level.getBlockState(testPos);
      if (testState.is(grass.getBlock()) && random.nextFloat() < 0.1F) {
         BonemealableBlock bonemealableBlock = (BonemealableBlock)grass.getBlock();
         if (bonemealableBlock.isValidBonemealTarget(level, testPos, testState)) {
            bonemealableBlock.performBonemeal(level, random, testPos, testState);
         }
      }

      if (testState.isAir() && !level.isOutsideBuildHeight(testPos)) {
         if (random.nextFloat() < 0.125F) {
            List<Feature> features = level.getBiome(testPos).value().getGenerationSettings().getBoneMealFeatures();
            if (features.isEmpty()) {
               return;
            }

            Feature placementFeature = Util.getRandom(features, random);
            placementFeature.place(level, level.getChunkSource().getGenerator(), random, testPos);
         } else if (grassFeature.isPresent()) {
            grassFeature.get().value().place(level, level.getChunkSource().getGenerator(), random, testPos);
         }
      }
   }

   private boolean stopBonemealSpread(final ServerLevel level, final BlockPos testPos) {
      return !level.getBlockState(testPos.below()).is(this) || level.getBlockState(testPos).isCollisionShapeFullBlock(level, testPos);
   }

   @Override
   public BonemealableBlock.Type getType() {
      return BonemealableBlock.Type.NEIGHBOR_SPREADER;
   }
}
