package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlastFurnaceBlock extends AbstractFurnaceBlock {
   public static final MapCodec<BlastFurnaceBlock> CODEC = simpleCodec(BlastFurnaceBlock::new);

   @Override
   public MapCodec<BlastFurnaceBlock> codec() {
      return CODEC;
   }

   protected BlastFurnaceBlock(BlockBehaviour.Properties p_49773_) {
      super(p_49773_);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos p_152386_, BlockState p_152387_) {
      return new BlastFurnaceBlockEntity(p_152386_, p_152387_);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level p_152382_, BlockState p_152383_, BlockEntityType<T> p_152384_) {
      return createFurnaceTicker(p_152382_, p_152384_, BlockEntityType.BLAST_FURNACE);
   }

   @Override
   protected void openContainer(Level p_49777_, BlockPos p_49778_, Player p_49779_) {
      BlockEntity blockentity = p_49777_.getBlockEntity(p_49778_);
      if (blockentity instanceof BlastFurnaceBlockEntity) {
         p_49779_.openMenu((MenuProvider)blockentity);
         p_49779_.awardStat(Stats.INTERACT_WITH_BLAST_FURNACE);
      }
   }

   @Override
   public void animateTick(BlockState p_220818_, Level p_220819_, BlockPos p_220820_, RandomSource p_220821_) {
      if (p_220818_.getValue(LIT)) {
         double d0 = p_220820_.getX() + 0.5;
         double d1 = p_220820_.getY();
         double d2 = p_220820_.getZ() + 0.5;
         if (p_220821_.nextDouble() < 0.1) {
            p_220819_.playLocalSound(d0, d1, d2, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         }

         Direction direction = p_220818_.getValue(FACING);
         Direction.Axis direction$axis = direction.getAxis();
         double d3 = 0.52;
         double d4 = p_220821_.nextDouble() * 0.6 - 0.3;
         double d5 = direction$axis == Direction.Axis.X ? direction.getStepX() * 0.52 : d4;
         double d6 = p_220821_.nextDouble() * 9.0 / 16.0;
         double d7 = direction$axis == Direction.Axis.Z ? direction.getStepZ() * 0.52 : d4;
         p_220819_.addParticle(ParticleTypes.SMOKE, d0 + d5, d1 + d6, d2 + d7, 0.0, 0.0, 0.0);
      }
   }
}
