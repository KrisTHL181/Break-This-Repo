package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class TestInstanceBlock extends BaseEntityBlock implements GameMasterBlock {
   public static final MapCodec<TestInstanceBlock> CODEC = simpleCodec(TestInstanceBlock::new);

   public TestInstanceBlock(BlockBehaviour.Properties p_391747_) {
      super(p_391747_);
   }

   @Override
   public @Nullable BlockEntity newBlockEntity(BlockPos p_392576_, BlockState p_397359_) {
      return new TestInstanceBlockEntity(p_392576_, p_397359_);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_391894_, Level p_393283_, BlockPos p_395877_, Player p_396004_, BlockHitResult p_397949_) {
      if (p_393283_.getBlockEntity(p_395877_) instanceof TestInstanceBlockEntity testinstanceblockentity) {
         if (!p_396004_.canUseGameMasterBlocks()) {
            return InteractionResult.PASS;
         }

         if (p_396004_.level().isClientSide()) {
            p_396004_.openTestInstanceBlock(testinstanceblockentity);
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   protected MapCodec<TestInstanceBlock> codec() {
      return CODEC;
   }
}
