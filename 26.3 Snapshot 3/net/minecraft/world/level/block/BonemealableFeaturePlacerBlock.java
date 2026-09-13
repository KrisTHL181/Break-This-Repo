package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;

public class BonemealableFeaturePlacerBlock extends Block implements BonemealableBlock {
   private final ResourceKey<Feature> feature;

   public BonemealableFeaturePlacerBlock(final ResourceKey<Feature> feature, final BlockBehaviour.Properties properties) {
      super(properties);
      this.feature = feature;
   }

   @Override
   public boolean isValidBonemealTarget(final LevelReader level, final BlockPos pos, final BlockState state) {
      return level.getBlockState(pos.above()).isAir();
   }

   @Override
   public boolean isBonemealSuccess(final Level level, final RandomSource random, final BlockPos pos, final BlockState state) {
      return true;
   }

   @Override
   public void performBonemeal(final ServerLevel level, final RandomSource random, final BlockPos pos, final BlockState state) {
      level.registryAccess()
         .lookup(Registries.FEATURE)
         .flatMap(registry -> registry.get(this.feature))
         .ifPresent(mossPatch -> mossPatch.value().place(level, level.getChunkSource().getGenerator(), random, pos.above()));
   }

   @Override
   public BonemealableBlock.Type getType() {
      return BonemealableBlock.Type.NEIGHBOR_SPREADER;
   }
}
