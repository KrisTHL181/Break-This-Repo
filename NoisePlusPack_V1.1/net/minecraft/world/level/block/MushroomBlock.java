package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MushroomBlock extends VegetationBlock implements BonemealableBlock {
   public static final MapCodec<MushroomBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422119_ -> p_422119_.group(
            ResourceKey.codec(Registries.CONFIGURED_FEATURE).fieldOf("feature").forGetter(p_310622_ -> p_310622_.feature), propertiesCodec()
         )
         .apply(p_422119_, MushroomBlock::new)
   );
   private static final VoxelShape SHAPE = Block.column(6.0, 0.0, 6.0);
   private final ResourceKey<ConfiguredFeature<?, ?>> feature;

   @Override
   public MapCodec<MushroomBlock> codec() {
      return CODEC;
   }

   public MushroomBlock(ResourceKey<ConfiguredFeature<?, ?>> p_256049_, BlockBehaviour.Properties p_256027_) {
      super(p_256027_);
      this.feature = p_256049_;
   }

   @Override
   protected VoxelShape getShape(BlockState p_54889_, BlockGetter p_54890_, BlockPos p_54891_, CollisionContext p_54892_) {
      return SHAPE;
   }

   @Override
   protected void randomTick(BlockState p_221784_, ServerLevel p_221785_, BlockPos p_221786_, RandomSource p_221787_) {
      if (p_221787_.nextInt(25) == 0) {
         int i = 5;
         int j = 4;

         for (BlockPos blockpos : BlockPos.betweenClosed(p_221786_.offset(-4, -1, -4), p_221786_.offset(4, 1, 4))) {
            if (p_221785_.getBlockState(blockpos).is(this)) {
               if (--i <= 0) {
                  return;
               }
            }
         }

         BlockPos blockpos1 = p_221786_.offset(p_221787_.nextInt(3) - 1, p_221787_.nextInt(2) - p_221787_.nextInt(2), p_221787_.nextInt(3) - 1);

         for (int k = 0; k < 4; k++) {
            if (p_221785_.isEmptyBlock(blockpos1) && p_221784_.canSurvive(p_221785_, blockpos1)) {
               p_221786_ = blockpos1;
            }

            blockpos1 = p_221786_.offset(p_221787_.nextInt(3) - 1, p_221787_.nextInt(2) - p_221787_.nextInt(2), p_221787_.nextInt(3) - 1);
         }

         if (p_221785_.isEmptyBlock(blockpos1) && p_221784_.canSurvive(p_221785_, blockpos1)) {
            p_221785_.setBlock(blockpos1, p_221784_, 2);
         }
      }
   }

   @Override
   protected boolean mayPlaceOn(BlockState p_54894_, BlockGetter p_54895_, BlockPos p_54896_) {
      return p_54894_.isSolidRender();
   }

   @Override
   protected boolean canSurvive(BlockState p_54880_, LevelReader p_54881_, BlockPos p_54882_) {
      BlockPos blockpos = p_54882_.below();
      BlockState blockstate = p_54881_.getBlockState(blockpos);
      return blockstate.is(BlockTags.MUSHROOM_GROW_BLOCK)
         ? true
         : p_54881_.getRawBrightness(p_54882_, 0) < 13 && this.mayPlaceOn(blockstate, p_54881_, blockpos);
   }

   public boolean growMushroom(ServerLevel p_221774_, BlockPos p_221775_, BlockState p_221776_, RandomSource p_221777_) {
      Optional<? extends Holder<ConfiguredFeature<?, ?>>> optional = p_221774_.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(this.feature);
      if (optional.isEmpty()) {
         return false;
      }

      p_221774_.removeBlock(p_221775_, false);
      if (optional.get().value().place(p_221774_, p_221774_.getChunkSource().getGenerator(), p_221777_, p_221775_)) {
         return true;
      }

      p_221774_.setBlock(p_221775_, p_221776_, 3);
      return false;
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader p_255904_, BlockPos p_54871_, BlockState p_54872_) {
      return true;
   }

   @Override
   public boolean isBonemealSuccess(Level p_221779_, RandomSource p_221780_, BlockPos p_221781_, BlockState p_221782_) {
      return p_221780_.nextFloat() < 0.4;
   }

   @Override
   public void performBonemeal(ServerLevel p_221769_, RandomSource p_221770_, BlockPos p_221771_, BlockState p_221772_) {
      this.growMushroom(p_221769_, p_221771_, p_221772_, p_221770_);
   }
}
