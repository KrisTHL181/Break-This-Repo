package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SporeBlossomBlock extends Block {
   public static final MapCodec<SporeBlossomBlock> CODEC = simpleCodec(SporeBlossomBlock::new);
   private static final VoxelShape SHAPE = Block.column(12.0, 13.0, 16.0);
   private static final int ADD_PARTICLE_ATTEMPTS = 14;
   private static final int PARTICLE_XZ_RADIUS = 10;
   private static final int PARTICLE_Y_MAX = 10;

   @Override
   public MapCodec<SporeBlossomBlock> codec() {
      return CODEC;
   }

   public SporeBlossomBlock(BlockBehaviour.Properties p_154697_) {
      super(p_154697_);
   }

   @Override
   protected boolean canSurvive(BlockState p_154709_, LevelReader p_154710_, BlockPos p_154711_) {
      return Block.canSupportCenter(p_154710_, p_154711_.above(), Direction.DOWN) && !p_154710_.isWaterAt(p_154711_);
   }

   @Override
   protected BlockState updateShape(
      BlockState p_154713_,
      LevelReader p_364689_,
      ScheduledTickAccess p_361752_,
      BlockPos p_154717_,
      Direction p_154714_,
      BlockPos p_154718_,
      BlockState p_154715_,
      RandomSource p_368691_
   ) {
      return p_154714_ == Direction.UP && !this.canSurvive(p_154713_, p_364689_, p_154717_)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(p_154713_, p_364689_, p_361752_, p_154717_, p_154714_, p_154718_, p_154715_, p_368691_);
   }

   @Override
   public void animateTick(BlockState p_222503_, Level p_222504_, BlockPos p_222505_, RandomSource p_222506_) {
      int i = p_222505_.getX();
      int j = p_222505_.getY();
      int k = p_222505_.getZ();
      double d0 = i + p_222506_.nextDouble();
      double d1 = j + 0.7;
      double d2 = k + p_222506_.nextDouble();
      p_222504_.addParticle(ParticleTypes.FALLING_SPORE_BLOSSOM, d0, d1, d2, 0.0, 0.0, 0.0);
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for (int l = 0; l < 14; l++) {
         blockpos$mutableblockpos.set(i + Mth.nextInt(p_222506_, -10, 10), j - p_222506_.nextInt(10), k + Mth.nextInt(p_222506_, -10, 10));
         BlockState blockstate = p_222504_.getBlockState(blockpos$mutableblockpos);
         if (!blockstate.isCollisionShapeFullBlock(p_222504_, blockpos$mutableblockpos)) {
            p_222504_.addParticle(
               ParticleTypes.SPORE_BLOSSOM_AIR,
               blockpos$mutableblockpos.getX() + p_222506_.nextDouble(),
               blockpos$mutableblockpos.getY() + p_222506_.nextDouble(),
               blockpos$mutableblockpos.getZ() + p_222506_.nextDouble(),
               0.0,
               0.0,
               0.0
            );
         }
      }
   }

   @Override
   protected VoxelShape getShape(BlockState p_154699_, BlockGetter p_154700_, BlockPos p_154701_, CollisionContext p_154702_) {
      return SHAPE;
   }
}
