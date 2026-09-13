package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class GrassBlock extends SpreadingSnowyDirtBlock implements BonemealableBlock {
   public static final MapCodec<GrassBlock> CODEC = simpleCodec(GrassBlock::new);

   @Override
   public MapCodec<GrassBlock> codec() {
      return CODEC;
   }

   public GrassBlock(BlockBehaviour.Properties p_53685_) {
      super(p_53685_);
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader p_256229_, BlockPos p_256432_, BlockState p_255677_) {
      return p_256229_.getBlockState(p_256432_.above()).isAir();
   }

   @Override
   public boolean isBonemealSuccess(Level p_221275_, RandomSource p_221276_, BlockPos p_221277_, BlockState p_221278_) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel p_221270_, RandomSource p_221271_, BlockPos p_221272_, BlockState p_221273_) {
      BlockPos blockpos = p_221272_.above();
      BlockState blockstate = Blocks.SHORT_GRASS.defaultBlockState();
      Optional<Holder.Reference<PlacedFeature>> optional = p_221270_.registryAccess()
         .lookupOrThrow(Registries.PLACED_FEATURE)
         .get(VegetationPlacements.GRASS_BONEMEAL);

      label51:
      for (int i = 0; i < 128; i++) {
         BlockPos blockpos1 = blockpos;

         for (int j = 0; j < i / 16; j++) {
            blockpos1 = blockpos1.offset(p_221271_.nextInt(3) - 1, (p_221271_.nextInt(3) - 1) * p_221271_.nextInt(3) / 2, p_221271_.nextInt(3) - 1);
            if (!p_221270_.getBlockState(blockpos1.below()).is(this) || p_221270_.getBlockState(blockpos1).isCollisionShapeFullBlock(p_221270_, blockpos1)) {
               continue label51;
            }
         }

         BlockState blockstate1 = p_221270_.getBlockState(blockpos1);
         if (blockstate1.is(blockstate.getBlock()) && p_221271_.nextInt(10) == 0) {
            BonemealableBlock bonemealableblock = (BonemealableBlock)blockstate.getBlock();
            if (bonemealableblock.isValidBonemealTarget(p_221270_, blockpos1, blockstate1)) {
               bonemealableblock.performBonemeal(p_221270_, p_221271_, blockpos1, blockstate1);
            }
         }

         if (blockstate1.isAir()) {
            Holder<PlacedFeature> holder;
            if (p_221271_.nextInt(8) == 0) {
               List<ConfiguredFeature<?, ?>> list = p_221270_.getBiome(blockpos1).value().getGenerationSettings().getFlowerFeatures();
               if (list.isEmpty()) {
                  continue;
               }

               int k = p_221271_.nextInt(list.size());
               holder = ((RandomPatchConfiguration)list.get(k).config()).feature();
            } else {
               if (!optional.isPresent()) {
                  continue;
               }

               holder = optional.get();
            }

            holder.value().place(p_221270_, p_221270_.getChunkSource().getGenerator(), p_221271_, blockpos1);
         }
      }
   }

   @Override
   public BonemealableBlock.Type getType() {
      return BonemealableBlock.Type.NEIGHBOR_SPREADER;
   }
}
