package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class BonemealableFeaturePlacerBlock extends Block implements BonemealableBlock {
   public static final MapCodec<BonemealableFeaturePlacerBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422075_ -> p_422075_.group(
            ResourceKey.codec(Registries.CONFIGURED_FEATURE).fieldOf("feature").forGetter(p_368616_ -> p_368616_.feature), propertiesCodec()
         )
         .apply(p_422075_, BonemealableFeaturePlacerBlock::new)
   );
   private final ResourceKey<ConfiguredFeature<?, ?>> feature;

   @Override
   public MapCodec<BonemealableFeaturePlacerBlock> codec() {
      return CODEC;
   }

   public BonemealableFeaturePlacerBlock(ResourceKey<ConfiguredFeature<?, ?>> p_364303_, BlockBehaviour.Properties p_364734_) {
      super(p_364734_);
      this.feature = p_364303_;
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader p_368682_, BlockPos p_367106_, BlockState p_363602_) {
      return p_368682_.getBlockState(p_367106_.above()).isAir();
   }

   @Override
   public boolean isBonemealSuccess(Level p_364715_, RandomSource p_368684_, BlockPos p_368440_, BlockState p_362347_) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel p_366209_, RandomSource p_363050_, BlockPos p_367656_, BlockState p_368138_) {
      p_366209_.registryAccess()
         .lookup(Registries.CONFIGURED_FEATURE)
         .flatMap(p_363008_ -> p_363008_.get(this.feature))
         .ifPresent(p_369259_ -> p_369259_.value().place(p_366209_, p_366209_.getChunkSource().getGenerator(), p_363050_, p_367656_.above()));
   }

   @Override
   public BonemealableBlock.Type getType() {
      return BonemealableBlock.Type.NEIGHBOR_SPREADER;
   }
}
