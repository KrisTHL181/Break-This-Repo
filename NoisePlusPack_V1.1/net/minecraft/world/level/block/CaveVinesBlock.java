package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

public class CaveVinesBlock extends GrowingPlantHeadBlock implements CaveVines {
   public static final MapCodec<CaveVinesBlock> CODEC = simpleCodec(CaveVinesBlock::new);
   private static final float CHANCE_OF_BERRIES_ON_GROWTH = 0.11F;

   @Override
   public MapCodec<CaveVinesBlock> codec() {
      return CODEC;
   }

   public CaveVinesBlock(BlockBehaviour.Properties p_152959_) {
      super(p_152959_, Direction.DOWN, SHAPE, false, 0.1);
      this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0).setValue(BERRIES, false));
   }

   @Override
   protected int getBlocksToGrowWhenBonemealed(RandomSource p_220928_) {
      return 1;
   }

   @Override
   protected boolean canGrowInto(BlockState p_152998_) {
      return p_152998_.isAir();
   }

   @Override
   protected Block getBodyBlock() {
      return Blocks.CAVE_VINES_PLANT;
   }

   @Override
   protected BlockState updateBodyAfterConvertedFromHead(BlockState p_152987_, BlockState p_152988_) {
      return p_152988_.setValue(BERRIES, p_152987_.getValue(BERRIES));
   }

   @Override
   protected BlockState getGrowIntoState(BlockState p_220935_, RandomSource p_220936_) {
      return super.getGrowIntoState(p_220935_, p_220936_).setValue(BERRIES, p_220936_.nextFloat() < 0.11F);
   }

   @Override
   protected ItemStack getCloneItemStack(LevelReader p_310879_, BlockPos p_152967_, BlockState p_152968_, boolean p_377333_) {
      return new ItemStack(Items.GLOW_BERRIES);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_152980_, Level p_152981_, BlockPos p_152982_, Player p_152983_, BlockHitResult p_152985_) {
      return CaveVines.use(p_152983_, p_152980_, p_152981_, p_152982_);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_152993_) {
      super.createBlockStateDefinition(p_152993_);
      p_152993_.add(BERRIES);
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader p_256026_, BlockPos p_152971_, BlockState p_152972_) {
      return !p_152972_.getValue(BERRIES);
   }

   @Override
   public boolean isBonemealSuccess(Level p_220930_, RandomSource p_220931_, BlockPos p_220932_, BlockState p_220933_) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel p_220923_, RandomSource p_220924_, BlockPos p_220925_, BlockState p_220926_) {
      p_220923_.setBlock(p_220925_, p_220926_.setValue(BERRIES, true), 2);
   }
}
