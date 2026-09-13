package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class AbstractCandleBlock extends Block {
   public static final int LIGHT_PER_CANDLE = 3;
   public static final BooleanProperty LIT = BlockStateProperties.LIT;

   @Override
   protected abstract MapCodec<? extends AbstractCandleBlock> codec();

   protected AbstractCandleBlock(BlockBehaviour.Properties p_151898_) {
      super(p_151898_);
   }

   protected abstract Iterable<Vec3> getParticleOffsets(BlockState var1);

   public static boolean isLit(BlockState p_151934_) {
      return p_151934_.hasProperty(LIT) && (p_151934_.is(BlockTags.CANDLES) || p_151934_.is(BlockTags.CANDLE_CAKES)) && p_151934_.getValue(LIT);
   }

   @Override
   protected void onProjectileHit(Level p_151905_, BlockState p_151906_, BlockHitResult p_151907_, Projectile p_151908_) {
      if (!p_151905_.isClientSide() && p_151908_.isOnFire() && this.canBeLit(p_151906_)) {
         setLit(p_151905_, p_151906_, p_151907_.getBlockPos(), true);
      }
   }

   protected boolean canBeLit(BlockState p_151935_) {
      return !p_151935_.getValue(LIT);
   }

   @Override
   public void animateTick(BlockState p_220697_, Level p_220698_, BlockPos p_220699_, RandomSource p_220700_) {
      if (p_220697_.getValue(LIT)) {
         this.getParticleOffsets(p_220697_)
            .forEach(p_220695_ -> addParticlesAndSound(p_220698_, p_220695_.add(p_220699_.getX(), p_220699_.getY(), p_220699_.getZ()), p_220700_));
      }
   }

   private static void addParticlesAndSound(Level p_220688_, Vec3 p_220689_, RandomSource p_220690_) {
      float f = p_220690_.nextFloat();
      if (f < 0.3F) {
         p_220688_.addParticle(ParticleTypes.SMOKE, p_220689_.x, p_220689_.y, p_220689_.z, 0.0, 0.0, 0.0);
         if (f < 0.17F) {
            p_220688_.playLocalSound(
               p_220689_.x + 0.5,
               p_220689_.y + 0.5,
               p_220689_.z + 0.5,
               SoundEvents.CANDLE_AMBIENT,
               SoundSource.BLOCKS,
               1.0F + p_220690_.nextFloat(),
               p_220690_.nextFloat() * 0.7F + 0.3F,
               false
            );
         }
      }

      p_220688_.addParticle(ParticleTypes.SMALL_FLAME, p_220689_.x, p_220689_.y, p_220689_.z, 0.0, 0.0, 0.0);
   }

   public static void extinguish(@Nullable Player p_151900_, BlockState p_151901_, LevelAccessor p_151902_, BlockPos p_151903_) {
      setLit(p_151902_, p_151901_, p_151903_, false);
      if (p_151901_.getBlock() instanceof AbstractCandleBlock) {
         ((AbstractCandleBlock)p_151901_.getBlock())
            .getParticleOffsets(p_151901_)
            .forEach(
               p_151926_ -> p_151902_.addParticle(
                  ParticleTypes.SMOKE, p_151903_.getX() + p_151926_.x(), p_151903_.getY() + p_151926_.y(), p_151903_.getZ() + p_151926_.z(), 0.0, 0.1F, 0.0
               )
            );
      }

      p_151902_.playSound(null, p_151903_, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
      p_151902_.gameEvent(p_151900_, GameEvent.BLOCK_CHANGE, p_151903_);
   }

   private static void setLit(LevelAccessor p_151919_, BlockState p_151920_, BlockPos p_151921_, boolean p_151922_) {
      p_151919_.setBlock(p_151921_, p_151920_.setValue(LIT, p_151922_), 11);
   }

   @Override
   protected void onExplosionHit(
      BlockState p_310999_, ServerLevel p_368647_, BlockPos p_311846_, Explosion p_310799_, BiConsumer<ItemStack, BlockPos> p_310677_
   ) {
      if (p_310799_.canTriggerBlocks() && p_310999_.getValue(LIT)) {
         extinguish(null, p_310999_, p_368647_, p_311846_);
      }

      super.onExplosionHit(p_310999_, p_368647_, p_311846_, p_310799_, p_310677_);
   }
}
