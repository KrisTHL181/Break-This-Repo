package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
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

public class FungusBlock extends VegetationBlock implements BonemealableBlock {
   public static final MapCodec<FungusBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422112_ -> p_422112_.group(
            ResourceKey.codec(Registries.CONFIGURED_FEATURE).fieldOf("feature").forGetter(p_309283_ -> p_309283_.feature),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("grows_on").forGetter(p_309285_ -> p_309285_.requiredBlock),
            propertiesCodec()
         )
         .apply(p_422112_, FungusBlock::new)
   );
   private static final double BONEMEAL_SUCCESS_PROBABILITY = 0.4;
   private static final VoxelShape SHAPE = Block.column(8.0, 0.0, 9.0);
   private final Block requiredBlock;
   private final ResourceKey<ConfiguredFeature<?, ?>> feature;

   @Override
   public MapCodec<FungusBlock> codec() {
      return CODEC;
   }

   protected FungusBlock(ResourceKey<ConfiguredFeature<?, ?>> p_259087_, Block p_260223_, BlockBehaviour.Properties p_259749_) {
      super(p_259749_);
      this.feature = p_259087_;
      this.requiredBlock = p_260223_;
   }

   @Override
   protected VoxelShape getShape(BlockState p_53618_, BlockGetter p_53619_, BlockPos p_53620_, CollisionContext p_53621_) {
      return SHAPE;
   }

   @Override
   protected boolean mayPlaceOn(BlockState p_53623_, BlockGetter p_53624_, BlockPos p_53625_) {
      return p_53623_.is(BlockTags.NYLIUM) || p_53623_.is(Blocks.MYCELIUM) || p_53623_.is(Blocks.SOUL_SOIL) || super.mayPlaceOn(p_53623_, p_53624_, p_53625_);
   }

   private Optional<? extends Holder<ConfiguredFeature<?, ?>>> getFeature(LevelReader p_256589_) {
      return p_256589_.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(this.feature);
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader p_256655_, BlockPos p_256553_, BlockState p_256213_) {
      BlockState blockstate = p_256655_.getBlockState(p_256553_.below());
      return blockstate.is(this.requiredBlock);
   }

   @Override
   public boolean isBonemealSuccess(Level p_221248_, RandomSource p_221249_, BlockPos p_221250_, BlockState p_221251_) {
      return p_221249_.nextFloat() < 0.4;
   }

   @Override
   public void performBonemeal(ServerLevel p_221243_, RandomSource p_221244_, BlockPos p_221245_, BlockState p_221246_) {
      this.getFeature(p_221243_).ifPresent(p_256352_ -> p_256352_.value().place(p_221243_, p_221243_.getChunkSource().getGenerator(), p_221244_, p_221245_));
   }
}
