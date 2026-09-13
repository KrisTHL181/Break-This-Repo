package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TestBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.TestBlockMode;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class TestBlock extends BaseEntityBlock implements GameMasterBlock {
   public static final MapCodec<TestBlock> CODEC = simpleCodec(TestBlock::new);
   public static final EnumProperty<TestBlockMode> MODE = BlockStateProperties.TEST_BLOCK_MODE;

   public TestBlock(BlockBehaviour.Properties p_395905_) {
      super(p_395905_);
   }

   @Override
   public @Nullable BlockEntity newBlockEntity(BlockPos p_395540_, BlockState p_395414_) {
      return new TestBlockEntity(p_395540_, p_395414_);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_393199_) {
      BlockItemStateProperties blockitemstateproperties = p_393199_.getItemInHand().get(DataComponents.BLOCK_STATE);
      BlockState blockstate = this.defaultBlockState();
      if (blockitemstateproperties != null) {
         TestBlockMode testblockmode = blockitemstateproperties.get(MODE);
         if (testblockmode != null) {
            blockstate = blockstate.setValue(MODE, testblockmode);
         }
      }

      return blockstate;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_395195_) {
      p_395195_.add(MODE);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_394844_, Level p_395112_, BlockPos p_395555_, Player p_391422_, BlockHitResult p_393840_) {
      if (p_395112_.getBlockEntity(p_395555_) instanceof TestBlockEntity testblockentity) {
         if (!p_391422_.canUseGameMasterBlocks()) {
            return InteractionResult.PASS;
         }

         if (p_395112_.isClientSide()) {
            p_391422_.openTestBlock(testblockentity);
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   protected void tick(BlockState p_396546_, ServerLevel p_395415_, BlockPos p_392034_, RandomSource p_394237_) {
      TestBlockEntity testblockentity = getServerTestBlockEntity(p_395415_, p_392034_);
      if (testblockentity != null) {
         testblockentity.reset();
      }
   }

   @Override
   protected void neighborChanged(
      BlockState p_391609_, Level p_396822_, BlockPos p_397767_, Block p_396884_, @Nullable Orientation p_395993_, boolean p_396803_
   ) {
      TestBlockEntity testblockentity = getServerTestBlockEntity(p_396822_, p_397767_);
      if (testblockentity != null && testblockentity.getMode() != TestBlockMode.START) {
         boolean flag = p_396822_.hasNeighborSignal(p_397767_);
         boolean flag1 = testblockentity.isPowered();
         if (flag && !flag1) {
            testblockentity.setPowered(true);
            testblockentity.trigger();
         } else if (!flag && flag1) {
            testblockentity.setPowered(false);
         }
      }
   }

   private static @Nullable TestBlockEntity getServerTestBlockEntity(Level p_393300_, BlockPos p_397106_) {
      return p_393300_ instanceof ServerLevel serverlevel && serverlevel.getBlockEntity(p_397106_) instanceof TestBlockEntity testblockentity
         ? testblockentity
         : null;
   }

   @Override
   public int getSignal(BlockState p_394389_, BlockGetter p_396658_, BlockPos p_391377_, Direction p_392211_) {
      if (p_394389_.getValue(MODE) != TestBlockMode.START) {
         return 0;
      } else if (p_396658_.getBlockEntity(p_391377_) instanceof TestBlockEntity testblockentity) {
         return testblockentity.isPowered() ? 15 : 0;
      } else {
         return 0;
      }
   }

   @Override
   protected ItemStack getCloneItemStack(LevelReader p_392000_, BlockPos p_392081_, BlockState p_397644_, boolean p_392867_) {
      ItemStack itemstack = super.getCloneItemStack(p_392000_, p_392081_, p_397644_, p_392867_);
      return setModeOnStack(itemstack, p_397644_.getValue(MODE));
   }

   public static ItemStack setModeOnStack(ItemStack p_397023_, TestBlockMode p_395078_) {
      p_397023_.set(DataComponents.BLOCK_STATE, p_397023_.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).with(MODE, p_395078_));
      return p_397023_;
   }

   @Override
   protected MapCodec<TestBlock> codec() {
      return CODEC;
   }
}
