package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartCommandBlock;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;

public class DetectorRailBlock extends BaseRailBlock {
   public static final MapCodec<DetectorRailBlock> CODEC = simpleCodec(DetectorRailBlock::new);
   public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE_STRAIGHT;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   private static final int PRESSED_CHECK_PERIOD = 20;

   @Override
   public MapCodec<DetectorRailBlock> codec() {
      return CODEC;
   }

   public DetectorRailBlock(BlockBehaviour.Properties p_52431_) {
      super(true, p_52431_);
      this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, false).setValue(SHAPE, RailShape.NORTH_SOUTH).setValue(WATERLOGGED, false));
   }

   @Override
   protected boolean isSignalSource(BlockState p_52489_) {
      return true;
   }

   @Override
   protected void entityInside(BlockState p_52458_, Level p_52459_, BlockPos p_52460_, Entity p_52461_, InsideBlockEffectApplier p_395259_, boolean p_432049_) {
      if (!p_52459_.isClientSide() && !p_52458_.getValue(POWERED)) {
         this.checkPressed(p_52459_, p_52460_, p_52458_);
      }
   }

   @Override
   protected void tick(BlockState p_221060_, ServerLevel p_221061_, BlockPos p_221062_, RandomSource p_221063_) {
      if (p_221060_.getValue(POWERED)) {
         this.checkPressed(p_221061_, p_221062_, p_221060_);
      }
   }

   @Override
   protected int getSignal(BlockState p_52449_, BlockGetter p_52450_, BlockPos p_52451_, Direction p_52452_) {
      return p_52449_.getValue(POWERED) ? 15 : 0;
   }

   @Override
   protected int getDirectSignal(BlockState p_52478_, BlockGetter p_52479_, BlockPos p_52480_, Direction p_52481_) {
      if (!p_52478_.getValue(POWERED)) {
         return 0;
      } else {
         return p_52481_ == Direction.UP ? 15 : 0;
      }
   }

   private void checkPressed(Level p_52433_, BlockPos p_52434_, BlockState p_52435_) {
      if (this.canSurvive(p_52435_, p_52433_, p_52434_)) {
         boolean flag = p_52435_.getValue(POWERED);
         boolean flag1 = false;
         List<AbstractMinecart> list = this.getInteractingMinecartOfType(p_52433_, p_52434_, AbstractMinecart.class, p_153125_ -> true);
         if (!list.isEmpty()) {
            flag1 = true;
         }

         if (flag1 && !flag) {
            BlockState blockstate = p_52435_.setValue(POWERED, true);
            p_52433_.setBlock(p_52434_, blockstate, 3);
            this.updatePowerToConnected(p_52433_, p_52434_, blockstate, true);
            p_52433_.updateNeighborsAt(p_52434_, this);
            p_52433_.updateNeighborsAt(p_52434_.below(), this);
            p_52433_.setBlocksDirty(p_52434_, p_52435_, blockstate);
         }

         if (!flag1 && flag) {
            BlockState blockstate1 = p_52435_.setValue(POWERED, false);
            p_52433_.setBlock(p_52434_, blockstate1, 3);
            this.updatePowerToConnected(p_52433_, p_52434_, blockstate1, false);
            p_52433_.updateNeighborsAt(p_52434_, this);
            p_52433_.updateNeighborsAt(p_52434_.below(), this);
            p_52433_.setBlocksDirty(p_52434_, p_52435_, blockstate1);
         }

         if (flag1) {
            p_52433_.scheduleTick(p_52434_, this, 20);
         }

         p_52433_.updateNeighbourForOutputSignal(p_52434_, this);
      }
   }

   protected void updatePowerToConnected(Level p_52473_, BlockPos p_52474_, BlockState p_52475_, boolean p_52476_) {
      RailState railstate = new RailState(p_52473_, p_52474_, p_52475_);

      for (BlockPos blockpos : railstate.getConnections()) {
         BlockState blockstate = p_52473_.getBlockState(blockpos);
         p_52473_.neighborChanged(blockstate, blockpos, blockstate.getBlock(), null, false);
      }
   }

   @Override
   protected void onPlace(BlockState p_52483_, Level p_52484_, BlockPos p_52485_, BlockState p_52486_, boolean p_52487_) {
      if (!p_52486_.is(p_52483_.getBlock())) {
         BlockState blockstate = this.updateState(p_52483_, p_52484_, p_52485_, p_52487_);
         this.checkPressed(p_52484_, p_52485_, blockstate);
      }
   }

   @Override
   public Property<RailShape> getShapeProperty() {
      return SHAPE;
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState p_52442_) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState p_52454_, Level p_52455_, BlockPos p_52456_, Direction p_425283_) {
      if (p_52454_.getValue(POWERED)) {
         List<MinecartCommandBlock> list = this.getInteractingMinecartOfType(p_52455_, p_52456_, MinecartCommandBlock.class, p_153123_ -> true);
         if (!list.isEmpty()) {
            return list.get(0).getCommandBlock().getSuccessCount();
         }

         List<AbstractMinecart> list1 = this.getInteractingMinecartOfType(p_52455_, p_52456_, AbstractMinecart.class, EntitySelector.CONTAINER_ENTITY_SELECTOR);
         if (!list1.isEmpty()) {
            return AbstractContainerMenu.getRedstoneSignalFromContainer((Container)list1.get(0));
         }
      }

      return 0;
   }

   private <T extends AbstractMinecart> List<T> getInteractingMinecartOfType(Level p_52437_, BlockPos p_52438_, Class<T> p_52439_, Predicate<Entity> p_52440_) {
      return p_52437_.getEntitiesOfClass(p_52439_, this.getSearchBB(p_52438_), p_52440_);
   }

   private AABB getSearchBB(BlockPos p_52471_) {
      double d0 = 0.2;
      return new AABB(
         p_52471_.getX() + 0.2, p_52471_.getY(), p_52471_.getZ() + 0.2, p_52471_.getX() + 1 - 0.2, p_52471_.getY() + 1 - 0.2, p_52471_.getZ() + 1 - 0.2
      );
   }

   @Override
   protected BlockState rotate(BlockState p_52466_, Rotation p_52467_) {
      RailShape railshape = p_52466_.getValue(SHAPE);
      RailShape railshape1 = this.rotate(railshape, p_52467_);
      return p_52466_.setValue(SHAPE, railshape1);
   }

   @Override
   protected BlockState mirror(BlockState p_52463_, Mirror p_52464_) {
      RailShape railshape = p_52463_.getValue(SHAPE);
      RailShape railshape1 = this.mirror(railshape, p_52464_);
      return p_52463_.setValue(SHAPE, railshape1);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_52469_) {
      p_52469_.add(SHAPE, POWERED, WATERLOGGED);
   }
}
