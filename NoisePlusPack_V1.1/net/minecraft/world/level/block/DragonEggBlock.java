package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DragonEggBlock extends FallingBlock {
   public static final MapCodec<DragonEggBlock> CODEC = simpleCodec(DragonEggBlock::new);
   private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 16.0);

   @Override
   public MapCodec<DragonEggBlock> codec() {
      return CODEC;
   }

   public DragonEggBlock(BlockBehaviour.Properties p_52911_) {
      super(p_52911_);
   }

   @Override
   protected VoxelShape getShape(BlockState p_52930_, BlockGetter p_52931_, BlockPos p_52932_, CollisionContext p_52933_) {
      return SHAPE;
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_52923_, Level p_52924_, BlockPos p_52925_, Player p_52926_, BlockHitResult p_52928_) {
      this.teleport(p_52923_, p_52924_, p_52925_);
      return InteractionResult.SUCCESS;
   }

   @Override
   protected void attack(BlockState p_52918_, Level p_52919_, BlockPos p_52920_, Player p_52921_) {
      this.teleport(p_52918_, p_52919_, p_52920_);
   }

   private void teleport(BlockState p_52936_, Level p_52937_, BlockPos p_52938_) {
      WorldBorder worldborder = p_52937_.getWorldBorder();

      for (int i = 0; i < 1000; i++) {
         BlockPos blockpos = p_52938_.offset(
            p_52937_.random.nextInt(16) - p_52937_.random.nextInt(16),
            p_52937_.random.nextInt(8) - p_52937_.random.nextInt(8),
            p_52937_.random.nextInt(16) - p_52937_.random.nextInt(16)
         );
         if (p_52937_.getBlockState(blockpos).isAir() && worldborder.isWithinBounds(blockpos) && !p_52937_.isOutsideBuildHeight(blockpos)) {
            if (p_52937_.isClientSide()) {
               for (int j = 0; j < 128; j++) {
                  double d0 = p_52937_.random.nextDouble();
                  float f = (p_52937_.random.nextFloat() - 0.5F) * 0.2F;
                  float f1 = (p_52937_.random.nextFloat() - 0.5F) * 0.2F;
                  float f2 = (p_52937_.random.nextFloat() - 0.5F) * 0.2F;
                  double d1 = Mth.lerp(d0, blockpos.getX(), p_52938_.getX()) + (p_52937_.random.nextDouble() - 0.5) + 0.5;
                  double d2 = Mth.lerp(d0, blockpos.getY(), p_52938_.getY()) + p_52937_.random.nextDouble() - 0.5;
                  double d3 = Mth.lerp(d0, blockpos.getZ(), p_52938_.getZ()) + (p_52937_.random.nextDouble() - 0.5) + 0.5;
                  p_52937_.addParticle(ParticleTypes.PORTAL, d1, d2, d3, f, f1, f2);
               }
            } else {
               p_52937_.setBlock(blockpos, p_52936_, 2);
               p_52937_.removeBlock(p_52938_, false);
            }

            return;
         }
      }
   }

   @Override
   protected int getDelayAfterPlace() {
      return 5;
   }

   @Override
   protected boolean isPathfindable(BlockState p_52913_, PathComputationType p_52916_) {
      return false;
   }

   @Override
   public int getDustColor(BlockState p_396408_, BlockGetter p_397945_, BlockPos p_397802_) {
      return -16777216;
   }
}
